package com.stikr.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
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
    onImportPhotos: (List<Uri>) -> Unit = {},
    onEditSticker: (String) -> Unit = {},
    importState: PackImportState? = null,
    onDeleteSticker: (String) -> Unit,
    onShareSticker: (String) -> Unit,
    onExport: () -> Unit,
    exporting: Boolean = false,
    onRenameAuthor: (String) -> Unit = {},
    onThumbnail: () -> Unit = {},
    onEditPack: () -> Unit = {},
) {
    val palette = Neo.palette
    val context = LocalContext.current
    val root = remember(context) { File(context.filesDir, "packs") }
    val dir = remember(root, pack.id) { root.resolve(pack.id) }
    var showAuthor by rememberSaveable { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var openSticker by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmSticker by rememberSaveable { mutableStateOf<String?>(null) }
    val count = pack.stickerCount
    val full = count >= PackRepository.MAX_WHATSAPP_STICKERS
    val remaining = (PackRepository.MAX_WHATSAPP_STICKERS - count).coerceAtLeast(0)
    val importing = importState?.packId == pack.id
    val multiplePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = remaining.coerceAtLeast(2)),
    ) { uris -> if (uris.isNotEmpty()) onImportPhotos(uris.take(remaining)) }
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImportPhotos(listOf(uri))
    }
    val pickPhotos: () -> Unit = {
        if (!full && !importing) {
            val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            if (remaining == 1) singlePicker.launch(request) else multiplePicker.launch(request)
        }
    }
    val bars = WindowInsets.systemBars.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize().collectionBackground()) {
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
                    onThumbnail = onThumbnail,
                    onAuthor = { showAuthor = true },
                    onEditPack = onEditPack,
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "thumbnail") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("by ${pack.author}", color = palette.muted, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp))
                    PackCollectionCard(pack, Modifier.width(260.dp), onClick = onThumbnail)
                }
            }

            if (importing) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "importing") {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        Text("Adding photos · ${importState?.processed}/${importState?.total}", color = palette.ink)
                        Spacer(Modifier.height(8.dp))
                        NeoProgressBar(contentDescription = "Adding photos")
                    }
                }
            }
            if (count == 0) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "empty", contentType = "empty") {
                    NeoEmptyState(
                        title = "No stickers yet",
                        body = "Select up to 30 photos at once. Tap a sticker to edit it, add a caption, or remove its background.",
                        action = "Select photos",
                        onAction = pickPhotos,
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
                    AddTile(onClick = pickPhotos)
                }
            }
        }

        ExportBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            count = count,
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
                onEdit = {
                    openSticker = null
                    onEditSticker(fileName)
                },
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

    if (showAuthor) {
        NameDialog(title = "Rename author", confirmLabel = "Save", initial = pack.author,
            onDismiss = { showAuthor = false }, onConfirm = { showAuthor = false; onRenameAuthor(it) }, kicker = "Author")
    }

    if (showRename) {
        NameDialog(
            title = "Rename Title",
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
private fun TopBar(pack: StickerPack, onBack: () -> Unit, onRename: () -> Unit,
                    onDelete: () -> Unit, onThumbnail: () -> Unit, onAuthor: () -> Unit, onEditPack: () -> Unit) {
    val palette = Neo.palette
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = palette.ink) }
        Text("Pack details", style = MaterialTheme.typography.titleMedium, color = palette.ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        TextButton(onClick = onEditPack, enabled = pack.stickers.isNotEmpty()) { Icon(Icons.Default.Edit, null, Modifier.size(15.dp), tint = palette.ink); Text("Pack edit", color = palette.ink) }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Pack options", tint = palette.ink) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Delete pack") }, onClick = { menu = false; onDelete() })
                DropdownMenuItem(text = { Text("Add thumbnail") }, onClick = { menu = false; onThumbnail() })
                DropdownMenuItem(text = { Text("Rename Title") }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text("Rename author") }, onClick = { menu = false; onAuthor() })
            }
        }
    }
}

/**
 * The WhatsApp action, pinned at the bottom of the pack screen: the import
 * button once the pack qualifies, a "more to go" hint while it doesn't, the
 * preparing spinner mid-export. Package availability is checked when tapped.
 */
@Composable
private fun ExportBar(
    modifier: Modifier = Modifier,
    count: Int,
    exporting: Boolean,
    onExport: () -> Unit,
) {
    val palette = Neo.palette
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
                .height(1.dp)
                .background(palette.muted.copy(alpha = .25f)),
        )
        Column(
            modifier = Modifier.padding(
                start = NeoDimens.gutter,
                end = NeoDimens.gutter + NeoDimens.shadowSmall,
                top = 12.dp,
                bottom = 14.dp,
            ),
        ) {
            WhatsAppPackAction(count, exporting, onExport)
        }
    }
}

/** Eligibility is local; the view model checks current WhatsApp availability on tap. */
@Composable
internal fun WhatsAppPackAction(count: Int, exporting: Boolean, onExport: () -> Unit) {
    val palette = Neo.palette
    val min = PackRepository.MIN_WHATSAPP_STICKERS
    val missing = (min - count).coerceAtLeast(0)
    val ready = count in min..PackRepository.MAX_WHATSAPP_STICKERS
    if (!ready) {
        Text(
            text = if (count > PackRepository.MAX_WHATSAPP_STICKERS) {
                "WhatsApp takes packs of 3–30 stickers. Remove ${count - PackRepository.MAX_WHATSAPP_STICKERS} to continue."
            } else if (missing == 1) {
                "1 more sticker — WhatsApp takes packs of $min or more"
            } else {
                "$missing more stickers — WhatsApp takes packs of $min or more"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
            modifier = Modifier.padding(bottom = 10.dp),
        )
    }
    SlideExport(enabled = ready, busy = exporting, onConfirm = onExport)

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
            shadow = 0.dp,
            padding = PaddingValues(10.dp),
            onClick = onClick,
            onClickLabel = "Edit, share or delete",
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
                .background(palette.card, NeoShapes.tag)
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            Text(
                text = number.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted,
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
        fill = palette.cherry,
        shape = NeoShapes.tile,
        shadow = 0.dp,
        onClick = onClick,
        onClickLabel = "Add sticker",
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = palette.onCherry,
                    modifier = Modifier.size(34.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Add photos",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.onCherry,
                )
            }
        }
    }
}

/** Three blank, tilted sticker slots waiting to be filled. */
@Composable
private fun EmptyArt() {
    val palette = Neo.palette
    val fills = listOf(palette.card, palette.cherry, palette.creamDim)
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
                        borderColor = palette.edgeFor(fill),
                        shadowColor = palette.shadow,
                        shape = NeoShapes.tile,
                        shadowOffset = NeoDimens.shadowSmall,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (index == 1) Text("+", fontSize = 34.sp, color = palette.ink)
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = Neo.palette
    NeoDialog(
        onDismiss = onDismiss,
        kicker = "Sticker ${number.toString().padStart(2, '0')}",
        title = "Make it yours",
        body = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .neoFrame(
                        fill = palette.creamDim,
                        borderColor = palette.edgeFor(palette.creamDim),
                        shadowColor = palette.shadow,
                        shape = NeoShapes.control,
                        shadowOffset = 0.dp,
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                StickerImage(file = file, contentDescription = "Sticker $number", modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(16.dp))
            NeoButton(
                text = "Edit sticker",
                icon = Icons.Default.Edit,
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
            )
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
