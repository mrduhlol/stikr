package com.stikr.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stikr.app.data.ImageLoader
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import com.stikr.app.editor.SubjectCutout
import com.stikr.app.export.PackExporter
import com.stikr.app.render.StickerCodec
import com.stikr.app.render.StickerRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Everything the editor screen needs to draw itself. */
data class EditorState(
    val packId: String,
    /** The photo as picked. */
    val source: Bitmap? = null,
    /** The photo after cut-out + trim, what the sticker is made of. */
    val subject: Bitmap? = null,
    /** The last rendered 512x512 sticker. */
    val preview: Bitmap? = null,
    val loading: Boolean = false,
    val status: String? = null,
    val saving: Boolean = false,
    val style: StickerRenderer.Style = StickerRenderer.Style(),
    val cutoutEnabled: Boolean = true,
    val cutoutFailed: Boolean = false,
) {
    val ready: Boolean get() = preview != null && !loading && !saving
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PackRepository(File(app.filesDir, "packs"))

    private val _packs = MutableStateFlow<List<StickerPack>>(emptyList())
    val packs: StateFlow<List<StickerPack>> = _packs.asStateFlow()

    private val _editor = MutableStateFlow<EditorState?>(null)
    val editor: StateFlow<EditorState?> = _editor.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** The pack being prepared for WhatsApp, or null. One export runs at a time. */
    private val _exportingPackId = MutableStateFlow<String?>(null)
    val exportingPackId: StateFlow<String?> = _exportingPackId.asStateFlow()

    private val previewRequests = Channel<Unit>(Channel.CONFLATED)
    private var imageRequest = 0L

    init {
        refresh()
        viewModelScope.launch {
            val renderer = StickerRenderer.PreviewCache()
            try {
                for (request in previewRequests) {
                    // One worker and one pending request, even during a fast slider drag.
                    delay(32)
                    previewRequests.tryReceive()
                    val state = _editor.value
                    val subject = state?.subject
                    if (state == null || subject == null || state.loading || state.saving) {
                        renderer.clear()
                        continue
                    }
                    val version = imageRequest
                    val preview = withContext(Dispatchers.Default) { renderer.render(subject, state.style) }
                    val current = _editor.value
                    if (imageRequest == version && current?.subject === subject && current.style == state.style) {
                        _editor.value = current.copy(preview = preview)
                    } else {
                        preview.recycle()
                    }
                }
            } finally {
                renderer.clear()
            }
        }
    }

    // --------------------------------------------------------------- packs

    fun refresh() {
        viewModelScope.launch {
            _packs.value = withContext(Dispatchers.IO) { repo.listPacks() }
        }
    }

    fun createPack(name: String, onCreated: (StickerPack) -> Unit) {
        viewModelScope.launch {
            val pack = storage { repo.createPack(name) }
            refresh()
            if (pack != null) onCreated(pack) else notify("Couldn't create the pack")
        }
    }

    fun renamePack(packId: String, name: String) {
        viewModelScope.launch {
            if (storage { repo.renamePack(packId, name) } == null) notify("Couldn't rename the pack")
            refresh()
        }
    }

    fun deletePack(packId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.deletePack(packId) }
            refresh()
            onDeleted()
        }
    }

    fun deleteSticker(packId: String, fileName: String) {
        viewModelScope.launch {
            val updated = storage { repo.deleteSticker(packId, fileName) }
            refresh()
            notify(if (updated != null) "Sticker deleted" else "Couldn't delete the sticker")
        }
    }

    fun notify(text: String) {
        _message.value = text
    }

    fun consumeMessage() {
        _message.value = null
    }

    // --------------------------------------------------------------- editor

    fun startEditor(packId: String) {
        imageRequest++
        _editor.value = EditorState(packId = packId)
        renderPreview()
    }

    fun closeEditor() {
        imageRequest++
        _editor.value = null
        renderPreview()
    }

    fun loadImage(uri: Uri) {
        val state = _editor.value ?: return
        val request = ++imageRequest
        _editor.value = state.copy(loading = true, status = "Loading photo\u2026")
        viewModelScope.launch {
            val source = withContext(Dispatchers.IO) { ImageLoader.load(getApplication(), uri) }
            if (request != imageRequest) {
                source?.recycle()
                return@launch
            }
            if (source == null) {
                _editor.value = _editor.value?.copy(loading = false, status = null)
                // Style edits made while loading were skipped by the preview worker.
                renderPreview()
                notify("Couldn't read that photo")
                return@launch
            }
            _editor.value = _editor.value?.copy(
                source = source,
                loading = true,
                status = if (state.cutoutEnabled) "Cutting out the subject\u2026" else null,
            )
            val (subject, failed) = process(source, state.cutoutEnabled)
            if (request != imageRequest) {
                if (subject !== source) subject.recycle()
                return@launch
            }
            _editor.value = _editor.value?.copy(
                subject = subject,
                cutoutFailed = failed,
                loading = false,
                status = null,
            )
            renderPreview()
            if (failed) notify("Auto cut-out isn't available \u2014 using the full photo")
        }
    }

    fun setCutout(enabled: Boolean) {
        val state = _editor.value ?: return
        val source = state.source
        if (source == null || (enabled == state.cutoutEnabled && state.subject != null)) {
            _editor.value = state.copy(cutoutEnabled = enabled)
            return
        }
        val request = ++imageRequest
        _editor.value = state.copy(
            cutoutEnabled = enabled,
            loading = true,
            status = if (enabled) "Cutting out the subject\u2026" else null,
        )
        viewModelScope.launch {
            val (subject, failed) = process(source, enabled)
            if (request != imageRequest) {
                if (subject !== source) subject.recycle()
                return@launch
            }
            _editor.value = _editor.value?.copy(
                subject = subject,
                cutoutFailed = failed,
                loading = false,
                status = null,
            )
            renderPreview()
        }
    }

    fun setStyle(style: StickerRenderer.Style) {
        val state = _editor.value ?: return
        if (state.style == style || state.saving) return
        _editor.value = state.copy(style = style)
        renderPreview()
    }

    fun saveSticker(onSaved: () -> Unit) {
        val state = _editor.value ?: return
        val subject = state.subject ?: return
        if (!state.ready) return
        _editor.value = state.copy(saving = true)
        viewModelScope.launch {
            // A debounced preview may still show the previous style. Save the current one.
            val bytes = withContext(Dispatchers.Default) {
                val sticker = StickerRenderer.render(subject, state.style)
                try {
                    StickerCodec.encodeWebp(sticker)
                } finally {
                    sticker.recycle()
                }
            }
            val updated = storage { repo.addSticker(state.packId, bytes) }
            if (updated == null) {
                // Keep the photo and style so the user can retry.
                _editor.value = _editor.value?.copy(saving = false)
                notify("Couldn't save the sticker")
                return@launch
            }
            closeEditor()
            refresh()
            notify("Sticker saved")
            onSaved()
        }
    }

    // --------------------------------------------------------------- export

    fun exportToWhatsApp(packId: String) {
        val pack = _packs.value.firstOrNull { it.id == packId } ?: return
        if (pack.stickerCount < PackRepository.MIN_WHATSAPP_STICKERS) {
            notify("WhatsApp needs at least ${PackRepository.MIN_WHATSAPP_STICKERS} stickers in a pack")
            return
        }
        if (_exportingPackId.value != null) return
        _exportingPackId.value = packId
        viewModelScope.launch {
            try {
                val prepared = try {
                    PackExporter.ensureTray(getApplication(), repo, pack)
                } catch (_: IOException) {
                    notify("Couldn't prepare the pack for WhatsApp")
                    return@launch
                }
                val launched = PackExporter.launchWhatsApp(getApplication(), prepared)
                refresh()
                if (!launched) notify("WhatsApp isn't installed \u2014 share a single sticker instead")
            } finally {
                _exportingPackId.value = null
            }
        }
    }

    fun shareSticker(packId: String, fileName: String) {
        val pack = _packs.value.firstOrNull { it.id == packId } ?: return
        viewModelScope.launch {
            PackExporter.shareSticker(getApplication(), repo, pack, fileName)
        }
    }

    // ------------------------------------------------------------- internals

    /** Produces the sticker subject: cut-out + trimmed, or the photo itself. */
    private suspend fun process(source: Bitmap, cutout: Boolean): Pair<Bitmap, Boolean> =
        withContext(Dispatchers.Default) {
            if (!cutout) return@withContext source to false
            val cut = SubjectCutout.cut(source)
            if (cut == null) {
                source to true
            } else {
                val trimmed = StickerRenderer.trimTransparent(cut)
                if (trimmed !== cut) cut.recycle()
                trimmed to false
            }
        }

    /** Runs a repository write off the main thread; null when storage fails (e.g. a full disk). */
    private suspend fun <T> storage(block: () -> T): T? = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (_: IOException) {
            null
        }
    }

    private fun renderPreview() {
        previewRequests.trySend(Unit)
    }
}
