package com.stikr.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.stikr.app.ui.neo.NeoIconButton
import com.stikr.app.ui.neo.NeoSwitch
import com.stikr.app.ui.theme.NeoShapes
import java.io.File
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import com.stikr.app.ui.neo.Neo
import com.stikr.app.ui.neo.NeoButton
import com.stikr.app.ui.neo.NeoCard
import com.stikr.app.ui.neo.NeoDialog
import com.stikr.app.ui.neo.NeoOutlinedButton
import com.stikr.app.ui.neo.NeoProgressBar
import com.stikr.app.ui.neo.NeoTextField
import com.stikr.app.ui.neo.StickerHeadline
import com.stikr.app.ui.theme.NeoDimens

/**
 * The batch flow on screen: progress while photos are turned into stickers,
 * then the packs that came out. The run itself lives in [MainViewModel].
 */
@Composable
fun BatchScreen(
    state: BatchState,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    onOpenPack: (String) -> Unit,
    onEditSticker: (String, String) -> Unit = { _, _ -> },
    onShareSticker: (String, String) -> Unit = { _, _ -> },
    onDeleteSticker: (String, String) -> Unit = { _, _ -> },
    onExport: (String) -> Unit = {},
    exportingPackId: String? = null,
) {
    when (state.phase) {
        BatchPhase.PROCESSING -> ProcessingScreen(state = state, onCancel = onCancel)
        BatchPhase.DONE -> DoneScreen(state, onDone, onOpenPack, onEditSticker, onShareSticker, onDeleteSticker, onExport, exportingPackId)
    }
}

@Composable
private fun ProcessingScreen(state: BatchState, onCancel: () -> Unit) {
    val palette = Neo.palette
    var confirming by rememberSaveable { mutableStateOf(false) }

    // Back asks the same question the Stop button does. The run lives in the
    // view model, so leaving without confirming would strand it.
    BackHandler { confirming = true }

    Scaffold(
        containerColor = palette.paper,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NeoDimens.gutter, vertical = 14.dp),
            ) {
                NeoOutlinedButton(
                    text = "Stop",
                    onClick = { confirming = true },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 15.dp),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .collectionBackground()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NeoDimens.gutter),
        ) {
            Spacer(Modifier.height(26.dp))
            StickerHeadline(text = state.title, fontSize = 32.sp)
            Spacer(Modifier.height(30.dp))
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    NeoProgressBar(
                        progress = if (state.total > 0) {
                            (state.processed.toFloat() / state.total).coerceIn(0f, 1f)
                        } else {
                            0f
                        },
                        contentDescription = "${state.processed} of ${state.total} photos processed",
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "${state.processed} of ${state.total}",
                        style = MaterialTheme.typography.displayMedium,
                        color = palette.ink,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (state.removeBackground) "Cutting out the subject…" else "Making stickers…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.muted,
                    )
                }
            }
        }
    }

    if (confirming) {
        StopDialog(
            onDismiss = { confirming = false },
            onConfirm = {
                confirming = false
                onCancel()
            },
        )
    }
}

@Composable
private fun StopDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val palette = Neo.palette
    NeoDialog(
        onDismiss = onDismiss,
        title = "Stop the batch?",
        body = {
            Text(
                text = "Nothing made so far will be kept.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
            )
        },
        actions = {
            NeoOutlinedButton(
                text = "Keep going",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 14.dp),
            )
            NeoButton(
                text = "Stop",
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 14.dp),
            )
        },
    )
}

@Composable
private fun DoneScreen(
    state: BatchState,
    onDone: () -> Unit,
    onOpenPack: (String) -> Unit,
    onEditSticker: (String, String) -> Unit,
    onShareSticker: (String, String) -> Unit,
    onDeleteSticker: (String, String) -> Unit,
    onExport: (String) -> Unit,
    exportingPackId: String?,
) {
    val palette = Neo.palette
    val pager = rememberPagerState(pageCount = { state.created.size })
    val currentPack = state.created.getOrNull(pager.currentPage)

    BackHandler { onDone() }

    Scaffold(
        containerColor = palette.paper,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NeoDimens.gutter, vertical = 14.dp),
            ) {
                currentPack?.let { pack ->
                    WhatsAppPackAction(pack.stickerCount, exportingPackId != null) { onExport(pack.id) }
                    Spacer(Modifier.height(12.dp))
                }
                NeoButton(
                    text = "Done",
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 15.dp),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .collectionBackground()
                .padding(padding)
                .padding(horizontal = NeoDimens.gutter),
        ) {
            Spacer(Modifier.height(26.dp))
            // The kit sets every headline in caps (NeoSectionHeader, NeoEmptyState,
            // NeoDialog all do), so consumers of these strings have to match them
            // case-insensitively.
            Text(
                text = when (state.created.size) {
                    0 -> "No stickers were made"
                    1 -> "1 pack created"
                    else -> "${state.created.size} packs created"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink,
            )
            if (state.created.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = summaryLine(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                )
                Spacer(Modifier.height(22.dp))
                if (state.created.size > 1) {
                    Text(
                        text = "Pack ${pager.currentPage + 1} of ${state.created.size} · Swipe to browse",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.muted,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                HorizontalPager(
                    state = pager,
                    key = { state.created[it].id },
                    modifier = Modifier.weight(1f).testTag("batch-pager"),
                    pageSpacing = 16.dp,
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    BatchPackPage(state.created[page], onOpenPack, onEditSticker, onShareSticker, onDeleteSticker)
                }
            }
        }
    }
}

@Composable
private fun BatchPackPage(
    pack: StickerPack,
    onOpenPack: (String) -> Unit,
    onEditSticker: (String, String) -> Unit,
    onShareSticker: (String, String) -> Unit,
    onDeleteSticker: (String, String) -> Unit,
) {
    val palette = Neo.palette
    val context = LocalContext.current
    val dir = remember(context, pack.id) { File(context.filesDir, "packs/${pack.id}") }
    var selected by rememberSaveable(pack.id) { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable(pack.id) { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(end = NeoDimens.shadowSmall, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PackCollectionCard(pack, Modifier.width(220.dp), onClick = { onOpenPack(pack.id) })
            }
            NeoIconButton(
                onClick = { onOpenPack(pack.id) },
                icon = Icons.Default.Edit,
                contentDescription = "Edit pack ${pack.name}",
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            )
        }
        pack.stickers.chunked(3).forEachIndexed { row, names ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                names.forEachIndexed { column, name ->
                    val number = row * 3 + column + 1
                    NeoCard(
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                            .semantics { contentDescription = "Sticker $number" },
                        fill = palette.creamDim,
                        shape = NeoShapes.tile,
                        shadow = 0.dp,
                        padding = PaddingValues(10.dp),
                        onClick = { selected = name },
                        onClickLabel = "Edit, share or delete",
                    ) {
                        StickerImage(dir.resolve(name), null, Modifier.fillMaxSize())
                    }
                }
                repeat(3 - names.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    selected?.takeIf { it in pack.stickers }?.let { name ->
        NeoDialog(
            onDismiss = { selected = null },
            title = "Make it yours",
            body = {
                StickerImage(dir.resolve(name), "Selected sticker", Modifier.fillMaxWidth().height(200.dp))
                Spacer(Modifier.height(16.dp))
                NeoButton("Edit sticker", onClick = {
                    selected = null
                    onEditSticker(pack.id, name)
                }, modifier = Modifier.fillMaxWidth())
            },
            actions = {
                NeoOutlinedButton("Delete", onClick = { selected = null; deleting = name }, modifier = Modifier.weight(1f))
                NeoButton("Share", onClick = { selected = null; onShareSticker(pack.id, name) }, modifier = Modifier.weight(1f))
            },
        )
    }
    deleting?.takeIf { it in pack.stickers }?.let { name ->
        ConfirmDialog(
            kicker = "Can’t be undone",
            title = "Delete sticker?",
            body = "It’s removed from “${pack.name}”. Copies already imported into WhatsApp stay.",
            confirmLabel = "Delete",
            dismissLabel = "Keep",
            onDismiss = { deleting = null },
            onConfirm = { deleting = null; onDeleteSticker(pack.id, name) },
        )
    }
}

/** "28 of 100 photos became stickers · 3 kept as full photos instead · 2 couldn't be read". */
private fun summaryLine(state: BatchState): String {
    val fellBack = if (state.fellBack > 0) {
        " \u00B7 ${state.fellBack} kept as full photos instead"
    } else {
        ""
    }
    val failed = if (state.failed > 0) " \u00B7 ${state.failed} couldn't be read" else ""
    return "${state.saved} of ${state.total} photos became stickers$fellBack$failed"
}

/** Title and author for a new batch; "Pick photos" hands them to the picker. */
@Composable
fun CreateBatchDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, author: String, removeBackground: Boolean) -> Unit,
) {
    var removeBackground by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf(PackRepository.DEFAULT_AUTHOR) }

    NeoDialog(
        onDismiss = onDismiss,
        title = "A new collection.",
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Name your pack, or leave it blank for a surprise.", style = MaterialTheme.typography.bodyMedium, color = Neo.palette.muted)
                NeoTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Pack title",
                )
                NeoTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = "Author",
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Remove background", color = Neo.palette.ink)
                        Text("Keep the full photo by default", style = MaterialTheme.typography.bodySmall, color = Neo.palette.muted)
                    }
                    NeoSwitch(
                        checked = removeBackground,
                        onCheckedChange = { removeBackground = it },
                        contentDescription = "Remove background",
                    )
                }
            }
        },
        actions = {
            NeoOutlinedButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 14.dp),
            )
            NeoButton(
                text = "Pick photos",
                onClick = {
                    onConfirm(
                        title.trim(),
                        author.trim().ifEmpty { PackRepository.DEFAULT_AUTHOR },
                        removeBackground,
                    )
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 14.dp),
            )
        },
    )
}
