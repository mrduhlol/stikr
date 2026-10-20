package com.stikr.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import com.stikr.app.export.PackExporter
import com.stikr.app.ui.neo.Neo
import com.stikr.app.ui.neo.NeoButton
import com.stikr.app.ui.neo.NeoCard
import com.stikr.app.ui.neo.NeoDialog
import com.stikr.app.ui.neo.NeoEmptyState
import com.stikr.app.ui.neo.NeoHaptic
import com.stikr.app.ui.neo.NeoIconButton
import com.stikr.app.ui.neo.NeoOutlinedButton
import com.stikr.app.ui.neo.NeoProgressBar
import com.stikr.app.ui.neo.NeoSectionHeader
import com.stikr.app.ui.neo.NeoTone
import com.stikr.app.ui.neo.neoFrame
import com.stikr.app.ui.theme.NeoDimens
import com.stikr.app.ui.theme.NeoShapes
import java.io.File

@Composable
fun PackScreen(
    pack: StickerPack,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onAddSticker: () -> Unit,
    onDeleteSticker: (String) -> Unit,
    onShareSticker: (String) -> Unit,
    onExport: () -> Unit,
    exporting: Boolean = false,
) {
    val palette = Neo.palette
    val context = LocalContext.current
    val root = remember(context) { File(context.filesDir, "packs") }
    val dir = remember(root, pack.id) { root.resolve(pack.id) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var openSticker by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmSticker by rememberSaveable { mutableStateOf<String?>(null) }
    val whatsAppInstalled = remember { PackExporter.isWhatsAppInstalled(context) }
    val count = pack.stickerCount
    val full = count >= PackRepository.MAX_WHATSAPP_STICKERS
    val bars = WindowInsets.systemBars.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize().background(palette.paper)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = NeoDimens.gutter,
                end = NeoDimens.gutter + NeoDimens.shadowSmall,
                top = bars.calculateTopPadding() + 8.dp,
                // Room for the pinned export band, so the last row scrolls clear of it.
                bottom = bars.calculateBottomPadding() + 136.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(NeoDimens.gapSmall + NeoDimens.shadowSmall),
            verticalArrangement = Arrangement.spacedBy(NeoDimens.gapSmall + NeoDimens.shadowSmall),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "top", contentType = "top") {
                TopBar(
                    pack = pack,
                    onBack = onBack,
                    onRename = { showRename = true },
                    onDelete = { showDelete = true },
                )
            }

            if (count == 0) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "empty", contentType = "empty") {
                    NeoEmptyState(
                        title = "No stickers yet",
                        body = "Pick a photo — stikr cuts the subject out right on your phone.",
                        action = "Add first sticker",
                        onAction = onAddSticker,
                        modifier = Modifier.padding(top = 8.dp),
                        art = { EmptyArt() },
                    )
                }
                return@LazyVerticalGrid
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "header", contentType = "header") {
                NeoSectionHeader(
                    title = "Stickers",
                    meta = "$count/${PackRepository.MAX_WHATSAPP_STICKERS}",
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            itemsIndexed(pack.stickers, key = { _, name -> name }, contentType = { _, _ -> "sticker" }) { index, fileName ->
                StickerTile(
                    file = dir.resolve(fileName),
                    number = index + 1,
                    onClick = { openSticker = fileName },
                )
            }
            if (!full) {
                item(key = "add", contentType = "add") {
                    AddTile(onClick = onAddSticker)
                }
            }
        }

        ExportBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            count = count,
            whatsAppInstalled = whatsAppInstalled,
            exporting = exporting,
            onExport = onExport,
        )
    }

    openSticker?.let { fileName ->
        val number = pack.stickers.indexOf(fileName) + 1
        if (number == 0) {
            openSticker = null
        } else {
            StickerSheet(
                file = dir.resolve(fileName),
                number = number,
                onDismiss = { openSticker = null },
                onShare = {
                    openSticker = null
                    onShareSticker(fileName)
                },
                onDelete = {
                    openSticker = null
                    confirmSticker = fileName
                },
            )
        }
    }

    confirmSticker?.let { fileName ->
        ConfirmDialog(
            kicker = "Can’t be undone",
            title = "Delete sticker?",
            body = "It’s removed from “${pack.name}”. Copies already imported into WhatsApp stay.",
            confirmLabel = "Delete",
            dismissLabel = "Keep",
            onDismiss = { confirmSticker = null },
            onConfirm = {
                confirmSticker = null
                onDeleteSticker(fileName)
            },
        )
    }

    if (showRename) {
        NameDialog(
            title = "Rename pack",
            kicker = "Pack name",
            confirmLabel = "Save",
            initial = pack.name,
            onDismiss = { showRename = false },
            onConfirm = { name ->
                showRename = false
                onRename(name)
            },
        )
    }

    if (showDelete) {
        ConfirmDialog(
            kicker = "Can’t be undone",
            title = "Delete this pack?",
            body = "“${pack.name}” and its $count " +
                "${if (count == 1) "sticker" else "stickers"} will be removed from stikr. " +
                "Copies already imported into WhatsApp are not affected.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                onDelete()
            },
        )
    }
}

@Composable
private fun TopBar(
    pack: StickerPack,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = Neo.palette
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeoIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = pack.name.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = if (pack.stickerCount == 1) "1 STICKER" else "${pack.stickerCount} STICKERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                )
            }
            NeoIconButton(
                onClick = onRename,
                icon = Icons.Default.Edit,
                contentDescription = "Rename pack",
            )
            Spacer(Modifier.width(NeoDimens.gapSmall))
            NeoIconButton(
                onClick = onDelete,
                icon = Icons.Default.Delete,
                contentDescription = "Delete pack",
                fill = palette.pink,
            )
        }
    }
}

/**
 * The WhatsApp action, pinned at the bottom of the pack screen: the import
 * button once the pack qualifies, a "more to go" hint while it doesn't, the
 * preparing spinner mid-export, and the no-WhatsApp fallback that points at
 * sharing stickers one by one.
 */
@Composable
private fun ExportBar(
    modifier: Modifier = Modifier,
    count: Int,
    whatsAppInstalled: Boolean,
    exporting: Boolean,
    onExport: () -> Unit,
) {
    val palette = Neo.palette
    val min = PackRepository.MIN_WHATSAPP_STICKERS
    val missing = (min - count).coerceAtLeast(0)
    val ready = whatsAppInstalled && missing == 0
    val nav = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.paper)
            .padding(bottom = nav),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(NeoDimens.border)
                .background(palette.ink),
        )
        Column(
            modifier = Modifier.padding(
                start = NeoDimens.gutter,
                end = NeoDimens.gutter + NeoDimens.shadowSmall,
                top = 12.dp,
                bottom = 14.dp,
            ),
        ) {
            if (!whatsAppInstalled) {
                Text(
                    text = "WhatsApp isn’t on this phone — tap any sticker above to share it as an image",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            } else if (!ready) {
                Text(
                    text = if (missing == 1) {
                        "1 more sticker — WhatsApp takes packs of $min or more"
                    } else {
                        "$missing more stickers — WhatsApp takes packs of $min or more"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            if (exporting) {
                // Same height as the button, so the band doesn't jump mid-export.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Preparing pack…", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(10.dp))
                        NeoProgressBar(contentDescription = "Preparing pack for WhatsApp")
                    }
                }
            } else {
                NeoButton(
                    text = "Add to WhatsApp",
                    onClick = onExport,
                    enabled = ready,
                    haptic = NeoHaptic.Confirm,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StickerTile(file: File, number: Int, onClick: () -> Unit) {
    val palette = Neo.palette
    Box {
        NeoCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .semantics { contentDescription = "Sticker $number" },
            // A paper well, not white, so white sticker outlines stay visible.
            fill = palette.creamDim,
            shape = NeoShapes.tile,
            shadow = NeoDimens.shadowSmall + 1.dp,
            padding = PaddingValues(10.dp),
            onClick = onClick,
            onClickLabel = "Share or delete",
        ) {
            StickerImage(
                file = file,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .padding(start = NeoDimens.border, top = NeoDimens.border)
                .background(palette.ink, NeoShapes.tag)
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            Text(
                text = number.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelSmall,
                color = palette.paper,
            )
        }
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    val palette = Neo.palette
    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        fill = palette.butter,
        shape = NeoShapes.tile,
        shadow = NeoDimens.shadowSmall + 1.dp,
        onClick = onClick,
        onClickLabel = "Add sticker",
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = palette.onAccent,
                    modifier = Modifier.size(34.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ADD",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.onAccent,
                )
            }
        }
    }
}

/** Three blank, tilted sticker slots waiting to be filled. */
@Composable
private fun EmptyArt() {
    val palette = Neo.palette
    val fills = listOf(palette.cyan, palette.butter, palette.pink)
    val tilts = listOf(-8f, 3f, 10f)
    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        fills.forEachIndexed { index, fill ->
            Box(
                modifier = Modifier
                    .padding(top = if (index == 1) 0.dp else 10.dp)
                    .size(72.dp)
                    .rotate(tilts[index])
                    .neoFrame(
                        fill = fill,
                        borderColor = palette.onAccent,
                        shadowColor = palette.shadow,
                        shape = NeoShapes.tile,
                        shadowOffset = NeoDimens.shadowSmall,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (index == 1) Text("+", fontSize = 34.sp, color = palette.onAccent)
            }
        }
    }
}

/** A sticker up close, with its two actions. */
@Composable
private fun StickerSheet(
    file: File,
    number: Int,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = Neo.palette
    NeoDialog(
        onDismiss = onDismiss,
        kicker = "Sticker ${number.toString().padStart(2, '0')}",
        title = "Share or delete",
        body = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .neoFrame(
                        fill = palette.creamDim,
                        borderColor = palette.ink,
                        shadowColor = palette.shadow,
                        shape = NeoShapes.control,
                        shadowOffset = 0.dp,
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                StickerImage(file = file, contentDescription = "Sticker $number", modifier = Modifier.fillMaxSize())
            }
        },
        actions = {
            NeoOutlinedButton(
                text = "Delete",
                onClick = onDelete,
                icon = Icons.Default.Delete,
                shadow = NeoDimens.shadowSmall,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.weight(1f),
            )
            NeoButton(
                text = "Share",
                onClick = onShare,
                icon = Icons.Default.Share,
                shadow = NeoDimens.shadowSmall,
                haptic = NeoHaptic.Confirm,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.weight(1f),
            )
        },
    )
}

/** Every destructive action goes through this: cherry band, safe choice first. */
@Composable
fun ConfirmDialog(
    kicker: String,
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val palette = Neo.palette
    NeoDialog(
        onDismiss = onDismiss,
        kicker = kicker,
        tone = NeoTone.Danger,
        title = title,
        body = {
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
        },
        actions = {
            NeoOutlinedButton(
                text = dismissLabel,
                onClick = onDismiss,
                shadow = NeoDimens.shadowSmall,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.weight(1f),
            )
            NeoButton(
                text = confirmLabel,
                onClick = onConfirm,
                shadow = NeoDimens.shadowSmall,
                haptic = NeoHaptic.Reject,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.weight(1f),
            )
        },
    )
}
