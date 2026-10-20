package com.stikr.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stikr.app.data.BatchPlanner
import com.stikr.app.data.ImageLoader
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import com.stikr.app.data.TourPrefs
import com.stikr.app.editor.SubjectCutout
import com.stikr.app.export.PackExporter
import com.stikr.app.render.StickerCodec
import com.stikr.app.render.StickerRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
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

    private val packsRoot = File(app.filesDir, "packs")
    private val repo = PackRepository(packsRoot)

    // Test seams: the batch engine's photo loading and cut-out, swappable in
    // unit tests. Production uses the real photo picker and ML Kit.
    internal var imageLoader: (Uri) -> Bitmap? = { uri -> ImageLoader.load(getApplication(), uri) }
    internal var subjectCutout: suspend (Bitmap) -> Bitmap? = { SubjectCutout.cut(it) }

    private val tourPrefs = TourPrefs(app)

    private val _packs = MutableStateFlow<List<StickerPack>>(emptyList())
    val packs: StateFlow<List<StickerPack>> = _packs.asStateFlow()

    private val _editor = MutableStateFlow<EditorState?>(null)
    val editor: StateFlow<EditorState?> = _editor.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _showTour = MutableStateFlow(!tourPrefs.done)
    val showTour: StateFlow<Boolean> = _showTour.asStateFlow()

    private val _batch = MutableStateFlow<BatchState?>(null)
    val batch: StateFlow<BatchState?> = _batch.asStateFlow()

    /** The pack being prepared for WhatsApp, or null. One export runs at a time. */
    private val _exportingPackId = MutableStateFlow<String?>(null)
    val exportingPackId: StateFlow<String?> = _exportingPackId.asStateFlow()

    private val previewRequests = Channel<Unit>(Channel.CONFLATED)
    private var imageRequest = 0L

    @Volatile
    private var batchCancelled = false
    private var batchJob: Job? = null

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
        viewModelScope.launch { loadPacks() }
    }

    /**
     * Reloads the pack list and waits for it, so a callback that navigates to a
     * pack (right after creating one or saving into it) finds it in the list.
     */
    private suspend fun loadPacks() {
        _packs.value = withContext(Dispatchers.IO) { repo.listPacks() }
    }

    fun createPack(name: String, onCreated: (StickerPack) -> Unit) {
        viewModelScope.launch {
            val pack = storage { repo.createPack(name) }
            if (pack == null) {
                notify("Couldn't create the pack")
                return@launch
            }
            loadPacks()
            onCreated(pack)
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

    // ---------------------------------------------------------------- tour

    fun completeTour() {
        tourPrefs.done = true
        _showTour.value = false
    }

    fun replayTour() {
        _showTour.value = true
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
            var saved = false
            try {
                // A full pack is refused before anything is encoded; the photo stays.
                val pack = storage { repo.getPack(state.packId) }
                if (pack != null && pack.stickerCount >= PackRepository.MAX_WHATSAPP_STICKERS) {
                    _editor.value = _editor.value?.copy(saving = false)
                    notify("That pack is full \u2014 save into a new one")
                    return@launch
                }
                // A debounced preview may still show the previous style. Save the current one.
                val bytes = encodeSticker(subject, state.style)
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
                saved = true
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // A failure must not leave the editor stuck on "saving".
                if (!saved) {
                    _editor.value = _editor.value?.copy(saving = false)
                    notify("Couldn't save the sticker")
                }
            }
        }
    }

    /** Saves the editor's sticker into a fresh pack named after the current one. */
    fun saveStickerToNewPack(onSaved: (StickerPack) -> Unit) {
        val state = _editor.value ?: return
        val subject = state.subject ?: return
        if (!state.ready) return
        _editor.value = state.copy(saving = true)
        viewModelScope.launch {
            // A pack that was created but never filled must not survive a failure.
            var halfMade: StickerPack? = null
            try {
                val current = storage { repo.getPack(state.packId) }
                val name = BatchPlanner.nextPackName(current?.name ?: "My stickers")
                val author = current?.author ?: PackRepository.DEFAULT_AUTHOR
                val pack = storage { repo.createPack(name, author) }
                if (pack == null) {
                    _editor.value = _editor.value?.copy(saving = false)
                    notify("Couldn't save the sticker")
                    return@launch
                }
                halfMade = pack
                // A debounced preview may still show the previous style. Save the current one.
                val bytes = encodeSticker(subject, state.style)
                val updated = storage { repo.addSticker(pack.id, bytes) }
                if (updated == null) {
                    // Don't leave an empty pack behind, and keep the photo to retry with.
                    deletePackFolder(pack.id)
                    _editor.value = _editor.value?.copy(saving = false)
                    refresh()
                    notify("Couldn't save the sticker")
                    return@launch
                }
                halfMade = null // the sticker is in; a later failure must not delete the pack
                closeEditor()
                loadPacks()
                notify("Saved to \u201C${updated.name}\u201D")
                onSaved(updated)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Nothing may be left behind: no empty pack, no stuck spinner.
                halfMade?.let { pack -> deletePackFolder(pack.id) }
                _editor.value = _editor.value?.copy(saving = false)
                refresh()
                notify("Couldn't save the sticker")
            }
        }
    }

    // --------------------------------------------------------------- batch

    fun startBatch(title: String, author: String, uris: List<Uri>) {
        if (uris.isEmpty() || _batch.value != null || batchJob?.isActive == true) return
        val safeTitle = title.trim().ifEmpty { "My stickers" }
        val safeAuthor = author.trim().ifEmpty { PackRepository.DEFAULT_AUTHOR }
        _batch.value = BatchState(
            title = safeTitle,
            author = safeAuthor,
            total = uris.size,
            expectedPacks = BatchPlanner.chunkSizes(uris.size).size,
        )
        batchCancelled = false
        batchJob = viewModelScope.launch {
            try {
                runBatch(uris, safeTitle, safeAuthor)
            } finally {
                batchJob = null
            }
        }
    }

    fun cancelBatch() {
        if (_batch.value?.phase == BatchPhase.PROCESSING) batchCancelled = true
    }

    /** Closes the done screen. A running batch is stopped with [cancelBatch], never dismissed. */
    fun dismissBatch() {
        if (_batch.value?.phase == BatchPhase.DONE) _batch.value = null
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

    /**
     * Produces the sticker subject: cut-out + trimmed, or the photo itself.
     * The cutter is injectable so the batch engine can be tested without ML Kit.
     */
    private suspend fun process(
        source: Bitmap,
        cutout: Boolean,
        cutter: suspend (Bitmap) -> Bitmap? = { SubjectCutout.cut(it) },
    ): Pair<Bitmap, Boolean> = withContext(Dispatchers.Default) {
        if (!cutout) return@withContext source to false
        val cut = cutter(source)
        if (cut == null) {
            source to true
        } else {
            val trimmed = StickerRenderer.trimTransparent(cut)
            if (trimmed !== cut) cut.recycle()
            trimmed to false
        }
    }

    /**
     * Renders the subject as a 512x512 sticker and encodes it as a WebP.
     * A recycled subject or a failed encode throws; callers keep the editor.
     */
    private suspend fun encodeSticker(
        subject: Bitmap,
        style: StickerRenderer.Style,
    ): ByteArray = withContext(Dispatchers.Default) {
        val sticker = StickerRenderer.render(subject, style)
        try {
            StickerCodec.encodeWebp(sticker)
        } finally {
            sticker.recycle()
        }
    }

    /**
     * Removes a pack from storage; false when it is still on disk afterwards.
     * The folder decides the outcome, because a delete can fail silently.
     */
    private suspend fun deletePackFolder(id: String): Boolean {
        if (!File(packsRoot, id).exists()) return true
        try {
            storage { repo.deletePack(id) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The folder check below decides whether the pack is really gone.
        }
        return !File(packsRoot, id).exists()
    }

    /**
     * Turns every readable photo into a sticker, creating 30-sticker packs as
     * it goes. A photo that fails in any way (unreadable, cut-out, disk write)
     * is counted as failed and the run continues. The batch always leaves
     * PROCESSING: the finally block publishes DONE, or clears it when stopped.
     */
    private suspend fun runBatch(uris: List<Uri>, title: String, author: String) {
        val createdIds = mutableListOf<String>()
        val expectedPacks = BatchPlanner.chunkSizes(uris.size).size
        var packId: String? = null
        var processed = 0
        var saved = 0
        var fellBack = 0
        var failed = 0

        try {
            for (uri in uris) {
                if (batchCancelled) break

                try {
                    val source = withContext(Dispatchers.IO) { imageLoader(uri) }
                    if (source == null) {
                        failed++
                    } else {
                        try {
                            val (subject, photoFellBack) =
                                process(source, cutout = true, cutter = subjectCutout)
                            try {
                                val preview = withContext(Dispatchers.Default) {
                                    StickerRenderer.render(subject, StickerRenderer.Style())
                                }
                                try {
                                    val bytes =
                                        withContext(Dispatchers.Default) { StickerCodec.encodeWebp(preview) }
                                    // A new pack starts when there is none or the current one is full.
                                    val needsPack = packId == null ||
                                        saved % PackRepository.MAX_WHATSAPP_STICKERS == 0
                                    val target = if (needsPack) {
                                        val pack = storage {
                                            repo.createPack(
                                                name = BatchPlanner.packName(
                                                    title,
                                                    saved / PackRepository.MAX_WHATSAPP_STICKERS,
                                                    expectedPacks,
                                                ),
                                                author = author,
                                            )
                                        }
                                        if (pack == null) {
                                            null
                                        } else {
                                            packId = pack.id
                                            createdIds += pack.id
                                            pack.id
                                        }
                                    } else {
                                        packId
                                    }
                                    if (target == null) {
                                        failed++
                                    } else {
                                        val updated = storage { repo.addSticker(target, bytes) }
                                        if (updated == null) {
                                            failed++
                                        } else {
                                            saved++
                                            if (photoFellBack) fellBack++
                                        }
                                    }
                                } finally {
                                    preview.recycle()
                                }
                            } finally {
                                if (subject !== source) subject.recycle()
                            }
                        } finally {
                            source.recycle()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failed++
                }

                processed++
                publishBatch(processed, saved, fellBack, failed)
            }
        } finally {
            withContext(NonCancellable) {
                finishBatch(createdIds, title, expectedPacks, processed, saved, fellBack, failed)
            }
        }
    }

    /**
     * Terminal step of a run: deletes the packs when stopped, otherwise drops
     * packs that never got a sticker and publishes the DONE snapshot.
     */
    private suspend fun finishBatch(
        createdIds: MutableList<String>,
        title: String,
        expectedPacks: Int,
        processed: Int,
        saved: Int,
        fellBack: Int,
        failed: Int,
    ) {
        try {
            if (batchCancelled) {
                cancelFinishedBatch(createdIds)
                return
            }

            // A failed write right after a chunk was created leaves an empty pack;
            // drop it so the next chunk's name stays the one people see. A pack
            // that cannot be removed stays in the list, so the user can see it.
            val empty = withContext(Dispatchers.IO) {
                createdIds.filter { id -> (repo.getPack(id)?.stickerCount ?: 0) == 0 }
            }.toSet()
            val stuck = empty.filter { id -> !deletePackFolder(id) }.toSet()
            createdIds.retainAll { id -> id !in empty || id in stuck }

            if (batchCancelled) {
                cancelFinishedBatch(createdIds)
                return
            }

            if (createdIds.size == 1 && expectedPacks > 1) {
                storage { repo.renamePack(createdIds.first(), title) }
            }
            val created = withContext(Dispatchers.IO) {
                val packs = repo.listPacks().associateBy { it.id }
                createdIds.mapNotNull { packs[it] }
            }

            if (batchCancelled) {
                cancelFinishedBatch(createdIds)
                return
            }

            _batch.value = _batch.value?.copy(
                phase = BatchPhase.DONE,
                processed = processed,
                saved = saved,
                fellBack = fellBack,
                failed = failed,
                created = created,
            )
            refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Even a failing close-out must not leave the progress screen spinning.
            if (batchCancelled) {
                val stillOnDisk = deleteCreatedPacks(createdIds)
                _batch.value = null
                notify(cancelNotice(stillOnDisk))
            } else {
                _batch.value = _batch.value?.copy(
                    phase = BatchPhase.DONE,
                    processed = processed,
                    saved = saved,
                    fellBack = fellBack,
                    failed = failed,
                )
            }
            refresh()
        }
    }

    private suspend fun cancelFinishedBatch(createdIds: List<String>) {
        val stillOnDisk = deleteCreatedPacks(createdIds)
        _batch.value = null
        refresh()
        notify(cancelNotice(stillOnDisk))
    }

    /** Deletes every pack the run created; returns how many are still on disk. */
    private suspend fun deleteCreatedPacks(ids: List<String>): Int =
        ids.count { id -> !deletePackFolder(id) }

    /** The cancel notice only claims nothing was saved when that is true. */
    private fun cancelNotice(stillOnDisk: Int): String =
        if (stillOnDisk == 0) {
            "Batch stopped \u2014 nothing was saved"
        } else {
            "Batch stopped \u2014 $stillOnDisk pack${if (stillOnDisk == 1) "" else "s"} couldn't be deleted"
        }

    /**
     * Publishes the counters. The pack list is re-synced when the run ends, not
     * per photo: a long run would list every pack hundreds of times.
     */
    private fun publishBatch(processed: Int, saved: Int, fellBack: Int, failed: Int) {
        _batch.value = _batch.value?.copy(
            processed = processed,
            saved = saved,
            fellBack = fellBack,
            failed = failed,
        )
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
