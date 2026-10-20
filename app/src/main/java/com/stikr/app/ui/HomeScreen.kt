package com.stikr.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import com.stikr.app.ui.neo.Neo
import com.stikr.app.ui.neo.NeoButton
import com.stikr.app.ui.neo.NeoCard
import com.stikr.app.ui.neo.NeoDialog
import com.stikr.app.ui.neo.NeoHaptic
import com.stikr.app.ui.neo.NeoIconButton
import com.stikr.app.ui.neo.NeoOutlinedButton
import com.stikr.app.ui.neo.NeoSectionHeader
import com.stikr.app.ui.neo.NeoTag
import com.stikr.app.ui.neo.NeoTextButton
import com.stikr.app.ui.neo.NeoTextField
import com.stikr.app.ui.neo.NeoWordmark
import com.stikr.app.ui.neo.StickerHeadline
import com.stikr.app.ui.neo.neoFrame
import com.stikr.app.ui.neo.rememberHaptics
import com.stikr.app.ui.theme.NeoDimens
import com.stikr.app.ui.theme.NeoShapes
import java.io.File

@Composable
fun HomeScreen(
    packs: List<StickerPack>,
    onOpenPack: (String) -> Unit,
    onCreatePack: (String) -> Unit,
    onMakeSticker: () -> Unit,
    onStartBatch: (String, String, List<Uri>) -> Unit,
    onShowTour: () -> Unit,
) {
    val palette = Neo.palette
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showCreateBatch by rememberSaveable { mutableStateOf(false) }
    // The picker answers after the dialog is gone, so the names it needs are
    // kept here, not in the dialog's own state.
    var pendingTitle by rememberSaveable { mutableStateOf("") }
    var pendingAuthor by rememberSaveable { mutableStateOf(PackRepository.DEFAULT_AUTHOR) }
    val bars = WindowInsets.systemBars.asPaddingValues()

    // As many photos as the user likes: the picker decides, not us.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) onStartBatch(pendingTitle, pendingAuthor, uris)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = NeoDimens.gutter,
                end = NeoDimens.gutter + NeoDimens.shadow,
                top = bars.calculateTopPadding() + 8.dp,
                bottom = bars.calculateBottomPadding() + if (packs.isEmpty()) 32.dp else 112.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(NeoDimens.gap + NeoDimens.shadow / 2),
            verticalArrangement = Arrangement.spacedBy(NeoDimens.gap + NeoDimens.shadow / 2),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "wordmark", contentType = "wordmark") {
                NeoWordmark(
                    trailing = {
                        NeoTag("on-device", fill = palette.green, rotation = 3f)
                        Spacer(Modifier.width(NeoDimens.gapSmall))
                        // The tour, on demand: the same one a first run shows.
                        NeoIconButton(
                            onClick = onShowTour,
                            icon = Icons.Default.Info,
                            contentDescription = "About stikr",
                        )
                    },
                )
            }

            if (packs.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "hero", contentType = "hero") {
                    EmptyHero(
                        onMakeSticker = onMakeSticker,
                        onNewBatch = { showCreateBatch = true },
                        onCreatePack = { showCreate = true },
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }, key = "header", contentType = "header") {
                    NeoSectionHeader(
                        title = "Your packs",
                        meta = if (packs.size == 1) "1 pack" else "${packs.size} packs",
                        modifier = Modifier.padding(top = 8.dp),
                        trailing = {
                            NeoOutlinedButton(
                                text = "New batch",
                                onClick = { showCreateBatch = true },
                                shadow = NeoDimens.shadowSmall,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            )
                        },
                    )
                }
                itemsIndexed(packs, key = { _, pack -> pack.id }, contentType = { _, _ -> "pack" }) { index, pack ->
                    PackCard(
                        pack = pack,
                        fill = packFill(index),
                        showNew = index == 0 && packs.size > 1,
                        onClick = { onOpenPack(pack.id) },
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "new-pack", contentType = "new-pack") {
                    NewPackRow(onClick = { showCreate = true })
                }
            }
        }

        if (packs.isNotEmpty()) {
            NeoButton(
                text = "Make a sticker",
                onClick = onMakeSticker,
                icon = Icons.Default.Add,
                haptic = NeoHaptic.Confirm,
                shadow = NeoDimens.shadowLarge - 2.dp,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = NeoDimens.gutter, end = NeoDimens.gutter + NeoDimens.shadow, bottom = 20.dp)
                    .fillMaxWidth(),
            )
        }
    }

    if (showCreate) {
        NameDialog(
            title = "New pack",
            kicker = "Name it",
            confirmLabel = "Create",
            initial = "",
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                onCreatePack(name)
            },
        )
    }

    if (showCreateBatch) {
        CreateBatchDialog(
            onDismiss = { showCreateBatch = false },
            onConfirm = { title, author ->
                // The picker's answer lands after this dialog is gone, so the
                // names wait here for it.
                showCreateBatch = false
                pendingTitle = title
                pendingAuthor = author
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }
}

/** Packs cycle through the pastel card colours. */
@Composable
private fun packFill(index: Int): Color {
    val palette = Neo.palette
    return when (index % 4) {
        0 -> palette.cyan
        1 -> palette.green
        2 -> palette.butter
        else -> palette.pink
    }
}

@Composable
private fun EmptyHero(onMakeSticker: () -> Unit, onNewBatch: () -> Unit, onCreatePack: () -> Unit) {
    val palette = Neo.palette
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(12.dp))
        StickerHeadline(
            text = "Photos in, stickers out.",
            accentWord = "stickers",
            fontSize = 58.sp,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Pick a photo. stikr cuts out the subject on your phone, gives it a white " +
                "sticker outline and packs it for WhatsApp. Nothing is uploaded, ever.",
            style = MaterialTheme.typography.bodyLarge,
            color = palette.ink,
        )
        Spacer(Modifier.height(26.dp))
        StepsStrip()
        Spacer(Modifier.height(30.dp))
        NeoButton(
            text = "Make a sticker",
            onClick = onMakeSticker,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            haptic = NeoHaptic.Confirm,
            shadow = NeoDimens.shadowLarge - 2.dp,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        NeoOutlinedButton(
            text = "New batch",
            onClick = onNewBatch,
            shadow = NeoDimens.shadowSmall,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        NeoTextButton(
            text = "or start an empty pack",
            onClick = onCreatePack,
            color = palette.muted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** How it works, as a rigid three-cell strip sharing ink rules. */
@Composable
private fun StepsStrip() {
    val palette = Neo.palette
    val steps = remember {
        listOf(
            Triple("01", "🖼️", "Pick a photo"),
            Triple("02", "✂️", "Cut it out"),
            Triple("03", "💬", "Send to WhatsApp"),
        )
    }
    val fills = listOf(palette.cyan, palette.pink, palette.green)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .neoFrame(
                fill = palette.card,
                borderColor = palette.onAccent,
                shadowColor = palette.shadow,
                shape = NeoShapes.card,
                clipContent = true,
            ),
    ) {
        steps.forEachIndexed { index, (number, emoji, label) ->
            if (index > 0) {
                Box(
                    Modifier
                        .width(NeoDimens.border)
                        .fillMaxHeight()
                        .background(palette.onAccent),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(fills[index])
                    .padding(horizontal = 12.dp, vertical = 14.dp)
                    .clearAndSetSemantics { contentDescription = "Step $number: $label" },
            ) {
                Text(number, style = MaterialTheme.typography.labelMedium, color = palette.onAccent)
                Spacer(Modifier.height(10.dp))
                Text(emoji, fontSize = 26.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.onAccent,
                )
            }
        }
    }
}

@Composable
private fun PackCard(
    pack: StickerPack,
    fill: Color,
    showNew: Boolean,
    onClick: () -> Unit,
) {
    val palette = Neo.palette
    val context = LocalContext.current
    val root = remember(context) { File(context.filesDir, "packs") }
    val count = pack.stickerCount
    val missing = (PackRepository.MIN_WHATSAPP_STICKERS - count).coerceAtLeast(0)
    val countLabel = if (count == 1) "1 sticker" else "$count stickers"
    val full = count >= PackRepository.MAX_WHATSAPP_STICKERS

    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        fill = fill,
        padding = PaddingValues(12.dp),
        onClick = onClick,
        onClickLabel = "Open pack",
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = pack.name.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = palette.onAccent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showNew) {
                NeoTag("new", fill = palette.card, rotation = 6f)
            }
            if (full) {
                // A full pack can't take another sticker: the editor offers a
                // new one instead.
                if (showNew) Spacer(Modifier.width(6.dp))
                NeoTag("full", fill = palette.card, rotation = -6f)
            }
        }
        Text(
            text = countLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = palette.onAccent,
        )
        Spacer(Modifier.height(10.dp))
        Mosaic(
            files = remember(pack.id, pack.stickers) {
                pack.stickers.take(4).map { root.resolve(pack.id).resolve(it) }
            },
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (missing == 0) "WHATSAPP READY" else "$missing MORE FOR WHATSAPP",
                style = MaterialTheme.typography.labelSmall,
                color = palette.onAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = palette.onAccent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** The first four stickers of a pack in a ruled 2x2 sheet. */
@Composable
private fun Mosaic(files: List<File>) {
    val palette = Neo.palette
    val rule = palette.onAccent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .neoFrame(
                // A paper well, not white, so white sticker outlines stay visible.
                fill = palette.creamDim,
                borderColor = rule,
                shadowColor = palette.shadow,
                shape = NeoShapes.control,
                shadowOffset = 0.dp,
                clipContent = true,
            )
            .padding(NeoDimens.border),
    ) {
        for (row in 0..1) {
            if (row == 1) Box(Modifier.fillMaxWidth().height(NeoDimens.hairline).background(rule))
            Row(modifier = Modifier.weight(1f)) {
                for (column in 0..1) {
                    if (column == 1) Box(Modifier.width(NeoDimens.hairline).fillMaxHeight().background(rule))
                    val file = files.getOrNull(row * 2 + column)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (file != null) {
                            StickerImage(file = file, contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

/** A dashed full-width slot that reads as "a pack goes here". */
@Composable
private fun NewPackRow(onClick: () -> Unit) {
    val palette = Neo.palette
    val haptics = rememberHaptics()
    val ink = palette.ink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .drawWithCache {
                val width = NeoDimens.border.toPx()
                val dash = Stroke(
                    width = width,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx())),
                )
                val radius = CornerRadius(6.dp.toPx())
                onDrawBehind {
                    drawRoundRect(
                        color = ink,
                        topLeft = Offset(width / 2f, width / 2f),
                        size = Size(size.width - width, size.height - width),
                        cornerRadius = radius,
                        style = dash,
                    )
                }
            }
            .clickable(role = Role.Button, onClickLabel = "Create a new pack") {
                haptics(NeoHaptic.Tap)
                onClick()
            }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = palette.ink, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text("NEW PACK", style = MaterialTheme.typography.labelLarge, color = palette.ink)
    }
}

/** One dialog for both "New pack" and "Rename pack". */
@Composable
fun NameDialog(
    title: String,
    confirmLabel: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    kicker: String = "Pack name",
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    NeoDialog(
        onDismiss = onDismiss,
        title = title,
        kicker = kicker,
        body = {
            NeoTextField(
                value = name,
                onValueChange = { name = it.take(MAX_PACK_NAME) },
                label = "Name",
                placeholder = "Untitled pack",
                focusRequester = focus,
            )
        },
        actions = {
            NeoOutlinedButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shadow = NeoDimens.shadowSmall,
                contentPadding = PaddingValues(vertical = 14.dp),
            )
            NeoButton(
                text = confirmLabel,
                onClick = { onConfirm(name.trim().ifEmpty { "Untitled pack" }) },
                modifier = Modifier.weight(1f),
                shadow = NeoDimens.shadowSmall,
                haptic = NeoHaptic.Confirm,
                contentPadding = PaddingValues(vertical = 14.dp),
            )
        },
    )
}

/** WhatsApp shows at most 128 characters of a pack name. */
private const val MAX_PACK_NAME = 128
