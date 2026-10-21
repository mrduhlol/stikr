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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import com.stikr.app.ui.neo.Neo
import com.stikr.app.ui.neo.NeoButton
import com.stikr.app.ui.neo.NeoDialog
import com.stikr.app.ui.neo.NeoHaptic
import com.stikr.app.ui.neo.NeoOutlinedButton
import com.stikr.app.ui.neo.NeoTextField
import com.stikr.app.ui.theme.NeoDimens
import java.io.File

@Composable
fun HomeScreen(
    packs: List<StickerPack>,
    onOpenPack: (String) -> Unit,
    onCreatePack: (String) -> Unit,
    onMakeSticker: () -> Unit,
    onStartBatch: (String, String, List<Uri>) -> Unit,
    onShowTour: () -> Unit,
    onSettings: () -> Unit = {},
) {
    val palette = Neo.palette
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showCreateBatch by rememberSaveable { mutableStateOf(false) }
    // The photo picker returns after the naming dialog has closed.
    var pendingTitle by rememberSaveable { mutableStateOf("") }
    var pendingAuthor by rememberSaveable { mutableStateOf(PackRepository.DEFAULT_AUTHOR) }
    val bars = WindowInsets.systemBars.asPaddingValues()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) onStartBatch(pendingTitle, pendingAuthor, uris)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(palette.paper),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = bars.calculateTopPadding() + 8.dp,
            bottom = bars.calculateBottomPadding() + 28.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "toolbar") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "stikr",
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.ink,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onShowTour) {
                    Icon(Icons.Default.Info, contentDescription = "App guide", tint = palette.muted)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = palette.ink)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "create") {
            Column(Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                Text(
                    "Make it yours.",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-1).sp,
                    ),
                    color = palette.ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your photos, ready for the conversation.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.muted,
                )
                Spacer(Modifier.height(24.dp))
                Surface(color = palette.card, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("Create a sticker pack", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Select your photos together. Open the pack to edit any sticker.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.muted,
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { showCreateBatch = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                "Select photos",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            )
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        TextButton(
                            colors = ButtonDefaults.textButtonColors(contentColor = palette.cherryInk),
                            onClick = onMakeSticker,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text("Edit a single photo", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "On your device · Up to 30 stickers per pack",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "library") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your packs", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                    Text(
                        if (packs.isEmpty()) "A home for your stickers" else
                            "${packs.size} ${if (packs.size == 1) "pack" else "packs"} · ${packs.sumOf { it.stickerCount }} stickers",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                }
                TextButton(
                    onClick = { showCreate = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = palette.cherryInk),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("New pack", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
        if (packs.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                Surface(color = palette.card, shape = RoundedCornerShape(16.dp)) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(52.dp).background(palette.creamDim, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = palette.muted)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Your first pack starts here", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                        Spacer(Modifier.height(6.dp))
                        Text("Choose photos above to get started.", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                    }
                }
            }
        } else {
            items(packs, key = { it.id }, contentType = { "pack" }) { pack ->
                PackCard(pack, onClick = { onOpenPack(pack.id) })
            }
        }
    }

    if (showCreate) {
        NameDialog(
            title = "New pack",
            kicker = "Pack details",
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
                showCreateBatch = false
                pendingTitle = title
                pendingAuthor = author
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }
}

@Composable
private fun PackCard(pack: StickerPack, onClick: () -> Unit) {
    val palette = Neo.palette
    val context = LocalContext.current
    val root = remember(context) { File(context.filesDir, "packs") }
    val count = pack.stickerCount
    val missing = (PackRepository.MIN_WHATSAPP_STICKERS - count).coerceAtLeast(0)
    val files = remember(pack.id, pack.stickers) {
        pack.stickers.take(4).map { root.resolve(pack.id).resolve(it) }
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.card,
    ) {
        Column(Modifier.padding(12.dp)) {
            Mosaic(files)
            Spacer(Modifier.height(12.dp))
            Text(
                pack.name,
                style = MaterialTheme.typography.titleSmall,
                color = palette.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$count / ${PackRepository.MAX_WHATSAPP_STICKERS} stickers",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (missing == 0) "Ready for WhatsApp" else "Add $missing to share",
                style = MaterialTheme.typography.bodySmall,
                color = if (missing == 0) palette.cherryInk else palette.muted,
            )
        }
    }
}

/** Quiet preview wells keep the focus on the user's actual stickers. */
@Composable
private fun Mosaic(files: List<File>) {
    val palette = Neo.palette
    Column(
        Modifier.fillMaxWidth().aspectRatio(1f)
            .background(palette.creamDim, RoundedCornerShape(10.dp)).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(2) { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(2) { column ->
                    Box(Modifier.weight(1f).fillMaxSize().padding(3.dp), contentAlignment = Alignment.Center) {
                        val file = files.getOrNull(row * 2 + column)
                        if (file != null) {
                            StickerImage(file = file, contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
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
