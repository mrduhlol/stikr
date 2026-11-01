package com.stikr.app.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stikr.app.data.ImageLoader
import com.stikr.app.render.*
import com.stikr.app.ui.neo.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Self-contained route. Save returns an encoded, portrait 1080 × 1350 PNG. */
@Composable
fun ThumbnailEditor(onSave: (ByteArray) -> Unit, onBack: () -> Unit, initialFile: File? = null, saving: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = Neo.palette
    var mode by rememberSaveable { mutableStateOf(-1) }
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var selectedUri by rememberSaveable { mutableStateOf<String?>(null) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var crop by rememberSaveable(stateSaver = listSaver<ThumbnailCrop, Float>(
        save = { listOf(it.zoom, it.x, it.y) }, restore = { ThumbnailCrop(it[0], it[1], it[2]) },
    )) { mutableStateOf(ThumbnailCrop()) }
    var words by rememberSaveable { mutableStateOf("Your little collection.") }
    var font by rememberSaveable { mutableStateOf("display") }
    var textSize by rememberSaveable { mutableStateOf(120) }
    var bold by rememberSaveable { mutableStateOf(false) }
    var italic by rememberSaveable { mutableStateOf(false) }
    var background by rememberSaveable { mutableStateOf(0xFFBDA1ED.toInt()) }
    var textColor by rememberSaveable { mutableStateOf(0xFF211A2C.toInt()) }
    var working by remember { mutableStateOf(false) }
    val busy = working || saving
    var fontsReady by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val textStyle = ThumbnailText(words, font, textSize, bold, italic, background, textColor)
    var loadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    fun load(uri: Uri, reset: Boolean = true) {
        selectedUri = uri.toString()
        if (reset) { mode = 0; crop = ThumbnailCrop() }
        loadJob?.cancel()
        loadJob = scope.launch {
            working = true
            error = null
            try {
                val loaded = withContext(Dispatchers.IO) { ImageLoader.load(context, uri, maxDimension = 1600) }
                if (loaded == null) error = "This photo could not be opened. Choose another photo."
                else source = loaded
            } finally { working = false }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let { uri -> load(uri) } }
    LaunchedEffect(initialFile) {
        withContext(Dispatchers.IO) { StickerTypography.initialize(context) }
        fontsReady = true
        if (selectedUri != null) load(Uri.parse(selectedUri), reset = false)
        else if (!initialized) {
            initialized = true
            initialFile?.takeIf { it.exists() }?.let { load(Uri.fromFile(it)) }
        }
    }
    DisposableEffect(source) {
        val owned = source
        onDispose { owned?.takeUnless { it.isRecycled }?.recycle() }
    }
    BackHandler { if (!busy) onBack() }
    Column(Modifier.fillMaxSize().collectionBackground().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            NeoIconButton(onClick = { if (!busy) onBack() }, icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            Text("Pack thumbnail", Modifier.padding(start = 14.dp), style = MaterialTheme.typography.headlineMedium, color = palette.ink)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("A little cover for your collection.", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            NeoSegmented(listOf("Photo", "Text"), mode, { if (!busy) { mode = it; error = null } }, Modifier.fillMaxWidth())
            if (mode == -1) {
                Text("Choose a photo to frame, or make a cover with words.", color = palette.ink, modifier = Modifier.padding(vertical = 24.dp))
            } else {
                Text(if (mode == 0) "Frame your thumbnail" else "Make it yours", color = palette.ink, style = MaterialTheme.typography.titleLarge)
                if (mode == 0) Text("Drag to position. Pinch or use Zoom to crop.", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                Box(Modifier.fillMaxWidth().aspectRatio(4f / 5f).background(palette.card), contentAlignment = Alignment.Center) {
                    val photo = source
                    if (mode == 0 && photo == null) {
                        Text(if (busy) "Opening photo…" else "Choose a photo below", color = palette.muted)
                    } else {
                        Canvas(Modifier.fillMaxSize().semantics { contentDescription = if (mode == 0) "Photo thumbnail crop preview" else "Text thumbnail preview" }.pointerInput(photo, mode, busy) {
                            if (mode == 0 && photo != null && !busy) detectTransformGestures { _, pan, zoom, _ ->
                                crop = ThumbnailRenderer.clamp(photo.width, photo.height,
                                    crop.copy(zoom = crop.zoom * zoom, x = crop.x + pan.x / size.width, y = crop.y + pan.y / size.height))
                            }
                        }) {
                            drawIntoCanvas { composeCanvas ->
                                val canvas = composeCanvas.nativeCanvas
                                canvas.save()
                                canvas.clipRect(0f, 0f, size.width, size.height)
                                canvas.scale(size.width / ThumbnailRenderer.WIDTH, size.height / ThumbnailRenderer.HEIGHT)
                                if (mode == 0 && photo != null) ThumbnailRenderer.drawPhoto(canvas, photo, crop)
                                else if (fontsReady) ThumbnailRenderer.drawText(canvas, textStyle)
                                canvas.restore()
                            }
                        }
                    }
                }
                Text("4:5 portrait · 1080 × 1350 px", color = palette.muted, style = MaterialTheme.typography.labelMedium)
                if (mode == 0) {
                    NeoSlider(crop.zoom, { zoom -> source?.let { crop = ThumbnailRenderer.clamp(it.width, it.height, crop.copy(zoom = zoom)) } }, 1f..4f, "Thumbnail zoom", enabled = source != null && !busy)
                    NeoOutlinedButton(text = if (source == null) "Choose photo" else "Change photo", onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy)
                } else {
                    NeoTextField(words, { words = it.take(500) }, label = "Cover text", placeholder = "Name your little world", singleLine = false, enabled = !busy)
                    TextStyleControls(font, textSize, bold, background, textColor, { font = it }, { textSize = it }, { bold = it }, { background = it }, { textColor = it }, 32f..400f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Italic", Modifier.weight(1f), color = palette.ink)
                        NeoSwitch(italic, { italic = it }, contentDescription = "Italic text")
                    }
                }
            }
            error?.let { Text(it, color = palette.ink, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            Spacer(Modifier.height(12.dp))
        }
        Box(Modifier.fillMaxWidth().padding(20.dp)) {
            NeoButton(text = if (busy) "Preparing thumbnail…" else "Use thumbnail", enabled = !busy && ((mode == 0 && source != null) || (mode == 1 && fontsReady && words.isNotBlank())), modifier = Modifier.fillMaxWidth(), onClick = {
                val photo = source
                val selectedMode = mode
                val selectedCrop = crop
                val selectedText = textStyle
                scope.launch {
                    working = true
                    error = null
                    try {
                        val bytes = withContext(Dispatchers.Default) {
                            val bitmap = if (selectedMode == 0) ThumbnailRenderer.photo(requireNotNull(photo), selectedCrop) else ThumbnailRenderer.text(selectedText)
                            try { ThumbnailRenderer.png(bitmap) } finally { bitmap.recycle() }
                        }
                        onSave(bytes)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        error = "Thumbnail could not be saved. Try again or choose another photo."
                    } finally { working = false }
                }
            })
        }
    }
}
