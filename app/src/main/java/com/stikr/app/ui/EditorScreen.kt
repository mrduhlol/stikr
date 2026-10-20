package com.stikr.app.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stikr.app.data.PackRepository
import com.stikr.app.render.StickerRenderer
import com.stikr.app.ui.neo.Neo
import com.stikr.app.ui.neo.NeoButton
import com.stikr.app.ui.neo.NeoCard
import com.stikr.app.ui.neo.NeoHaptic
import com.stikr.app.ui.neo.NeoIconButton
import com.stikr.app.ui.neo.NeoOutlinedButton
import com.stikr.app.ui.neo.NeoProgressBar
import com.stikr.app.ui.neo.NeoSegmented
import com.stikr.app.ui.neo.NeoSlider
import com.stikr.app.ui.neo.NeoStepLabel
import com.stikr.app.ui.neo.NeoSwatch
import com.stikr.app.ui.neo.NeoSwitch
import com.stikr.app.ui.neo.NeoTag
import com.stikr.app.ui.neo.NeoTextField
import com.stikr.app.ui.neo.neoFrame
import com.stikr.app.ui.theme.NeoDimens
import com.stikr.app.ui.theme.NeoShapes
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    state: EditorState,
    packName: String,
    onPick: (Uri) -> Unit,
    onCutout: (Boolean) -> Unit,
    onStyle: (StickerRenderer.Style) -> Unit,
    onSave: () -> Unit,
    packFull: Boolean,
    onSaveNew: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = Neo.palette
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPick)
    }
    var pickerOpened by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val pickPhoto = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    // A picked photo is work in progress: leaving asks first.
    val leave = {
        if (state.source != null && !state.saving) confirmDiscard = true else onBack()
    }

    LaunchedEffect(Unit) {
        if (state.source == null && !pickerOpened) {
            pickerOpened = true
            pickPhoto()
        }
    }
    BackHandler(onBack = leave)

    Scaffold(
        containerColor = palette.paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomBar(
                state = state,
                packName = packName,
                onSave = onSave,
                onSaveNew = onSaveNew,
                packFull = packFull,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = NeoDimens.gutter, end = NeoDimens.gutter + NeoDimens.shadow, top = 8.dp),
        ) {
            TopBar(packName = packName, onBack = leave)
            Spacer(Modifier.height(18.dp))

            PreviewCard(state = state, onPickPhoto = pickPhoto)
            Spacer(Modifier.height(24.dp))

            CutoutCard(state = state, onCutout = onCutout)
            Spacer(Modifier.height(NeoDimens.gap + NeoDimens.shadow / 2))

            OutlineCard(state = state, onStyle = onStyle)
            Spacer(Modifier.height(NeoDimens.gap + NeoDimens.shadow / 2))

            CaptionCard(state = state, onStyle = onStyle)
            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            kicker = "Not saved yet",
            title = "Discard this sticker?",
            body = "The photo and your edits are dropped. Nothing has been added to " +
                (if (packName.isNotEmpty()) "“$packName”." else "the pack."),
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            onDismiss = { confirmDiscard = false },
            onConfirm = {
                confirmDiscard = false
                onBack()
            },
        )
    }
}

@Composable
private fun TopBar(packName: String, onBack: () -> Unit) {
    val palette = Neo.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeoIconButton(
            onClick = onBack,
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
        )
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = "NEW STICKER",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = if (packName.isNotEmpty()) "INTO “${packName.uppercase()}”" else "PICK A PHOTO TO START",
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PreviewCard(state: EditorState, onPickPhoto: () -> Unit) {
    val palette = Neo.palette
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        shadow = NeoDimens.shadowLarge,
        padding = PaddingValues(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(NeoShapes.control),
            contentAlignment = Alignment.Center,
        ) {
            if (state.source != null || state.loading) {
                CheckerboardBackground(Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(palette.creamDim))
            }

            state.preview?.let { preview ->
                val image = remember(preview) { preview.asImageBitmap() }
                Image(
                    bitmap = image,
                    contentDescription = "Sticker preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            if (state.source == null && !state.loading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        text = "NO PHOTO YET",
                        style = MaterialTheme.typography.headlineLarge,
                        color = palette.ink,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Pets, friends, screenshots — anything with a clear subject.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.ink,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    NeoButton(
                        text = "Pick a photo",
                        onClick = onPickPhoto,
                        icon = Icons.Default.Add,
                        haptic = NeoHaptic.Confirm,
                    )
                }
            }

            if (state.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.paper.copy(alpha = 0.78f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .neoFrame(
                                fill = palette.card,
                                borderColor = palette.ink,
                                shadowColor = palette.shadow,
                                shape = NeoShapes.control,
                            )
                            .padding(18.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    ) {
                        Text(
                            text = (state.status ?: "Working…").uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.ink,
                        )
                        Spacer(Modifier.height(12.dp))
                        NeoProgressBar(contentDescription = state.status ?: "Working")
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "On this phone. Nothing is uploaded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted,
                        )
                    }
                }
            }
        }
        if (state.source != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "512 \u00D7 512 \u00B7 WEBP",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                )
                NeoButton(
                    text = "Change photo",
                    onClick = onPickPhoto,
                    enabled = !state.loading && !state.saving,
                    icon = Icons.Default.Refresh,
                    fill = palette.card,
                    shadow = NeoDimens.shadowSmall,
                    textStyle = MaterialTheme.typography.labelMedium,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.padding(end = NeoDimens.shadowSmall, bottom = NeoDimens.shadowSmall),
                )
            }
        }
    }
}

@Composable
private fun CutoutCard(state: EditorState, onCutout: (Boolean) -> Unit) {
    val palette = Neo.palette
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeoStepLabel(1, "Cut-out", modifier = Modifier.weight(1f))
            if (state.cutoutFailed) NeoTag("unavailable", fill = palette.pink, rotation = 2f)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Remove the background",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                )
                Text(
                    text = if (state.cutoutFailed) {
                        "Not available on this phone — using the full photo"
                    } else {
                        "Finds the subject, on device"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
            Spacer(Modifier.width(12.dp))
            NeoSwitch(
                checked = state.cutoutEnabled,
                onCheckedChange = onCutout,
                enabled = state.source != null && !state.loading,
                contentDescription = "Auto cut-out",
            )
        }
    }
}

@Composable
private fun OutlineCard(state: EditorState, onStyle: (StickerRenderer.Style) -> Unit) {
    val palette = Neo.palette
    val enabled = state.source != null
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeoStepLabel(2, "Outline", modifier = Modifier.weight(1f))
            Text(
                text = "${state.style.outline}".padStart(2, '0'),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink,
            )
            Text(
                text = " PX",
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted,
            )
        }
        Spacer(Modifier.height(6.dp))
        NeoSlider(
            value = state.style.outline.toFloat(),
            onValueChange = { value -> onStyle(state.style.copy(outline = value.roundToInt())) },
            valueRange = 0f..StickerRenderer.MAX_OUTLINE.toFloat(),
            enabled = enabled,
            contentDescription = "Outline thickness",
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "COLOUR",
                style = MaterialTheme.typography.labelMedium,
                color = palette.muted,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StickerRenderer.OutlineColor.entries.forEach { outline ->
                    NeoSwatch(
                        fill = Color(outline.color),
                        label = outline.label,
                        selected = state.style.outlineColor == outline,
                        onClick = { onStyle(state.style.copy(outlineColor = outline)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptionCard(state: EditorState, onStyle: (StickerRenderer.Style) -> Unit) {
    val palette = Neo.palette
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(18.dp),
    ) {
        NeoStepLabel(3, "Caption")
        Spacer(Modifier.height(14.dp))
        NeoTextField(
            value = state.style.caption,
            onValueChange = { value -> onStyle(state.style.copy(caption = value)) },
            label = "Text — optional",
            placeholder = "Say something",
            enabled = state.source != null,
        )

        if (state.style.caption.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "COLOUR",
                style = MaterialTheme.typography.labelMedium,
                color = palette.muted,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StickerRenderer.CaptionColor.entries.forEach { color ->
                    NeoSwatch(
                        fill = Color(color.fill),
                        label = color.label,
                        selected = state.style.captionColor == color,
                        onClick = { onStyle(state.style.copy(captionColor = color)) },
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "POSITION",
                style = MaterialTheme.typography.labelMedium,
                color = palette.muted,
            )
            Spacer(Modifier.height(10.dp))
            NeoSegmented(
                options = listOf("Top", "Bottom"),
                selectedIndex = state.style.captionPosition.ordinal,
                onSelect = { index ->
                    onStyle(state.style.copy(captionPosition = StickerRenderer.CaptionPosition.entries[index]))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The save bar: pinned above the navigation bar and the keyboard. */
@Composable
private fun BottomBar(
    state: EditorState,
    packName: String,
    onSave: () -> Unit,
    onSaveNew: () -> Unit,
    packFull: Boolean,
) {
    val palette = Neo.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.paper)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(NeoDimens.border)
                .background(palette.ink),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = NeoDimens.gutter, end = NeoDimens.gutter + NeoDimens.shadow, top = 14.dp, bottom = 14.dp + NeoDimens.shadow),
        ) {
            when {
                state.saving -> Column(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                    Text("SAVING STICKER…", style = MaterialTheme.typography.labelMedium, color = palette.ink)
                    Spacer(Modifier.height(10.dp))
                    NeoProgressBar(height = 28.dp, contentDescription = "Saving sticker")
                }

                // A full pack can't take another sticker, but the work on
                // screen shouldn't be lost: offer a fresh pack instead.
                packFull -> Column {
                    NeoButton(
                        text = "Pack is full — ${PackRepository.MAX_WHATSAPP_STICKERS} stickers",
                        onClick = {},
                        enabled = false,
                        shadow = NeoDimens.shadow,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 17.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    NeoOutlinedButton(
                        text = "Save into a new pack",
                        onClick = onSaveNew,
                        enabled = state.ready,
                        shadow = NeoDimens.shadowSmall,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 17.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> NeoButton(
                    text = when {
                        packName.isNotEmpty() -> "Add to “$packName”"
                        else -> "Add to pack"
                    },
                    onClick = onSave,
                    enabled = state.ready,
                    haptic = NeoHaptic.Confirm,
                    modifier = Modifier.fillMaxWidth(),
                    shadow = NeoDimens.shadow,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 17.dp),
                )
            }
        }
    }
}

private val StickerRenderer.OutlineColor.label: String
    get() = when (this) {
        StickerRenderer.OutlineColor.WHITE -> "White"
        StickerRenderer.OutlineColor.INK -> "Ink"
    }

private val StickerRenderer.CaptionColor.label: String
    get() = when (this) {
        StickerRenderer.CaptionColor.CREAM -> "Cream"
        StickerRenderer.CaptionColor.INK -> "Ink"
        StickerRenderer.CaptionColor.BUTTER -> "Butter"
        StickerRenderer.CaptionColor.CHERRY -> "Cherry"
    }

/** The see-through checkerboard behind the preview, so transparency reads clearly. */
@Composable
fun CheckerboardBackground(modifier: Modifier = Modifier) {
    val palette = Neo.palette
    val even = palette.card
    val odd = palette.creamDim

    Canvas(modifier = modifier) {
        drawRect(even)
        val cell = size.minDimension / 12f
        if (cell <= 0f) return@Canvas
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) cell else 0f
            while (x < size.width) {
                drawRect(color = odd, topLeft = Offset(x, y), size = Size(cell, cell))
                x += cell * 2
            }
            y += cell
            row++
        }
    }
}
