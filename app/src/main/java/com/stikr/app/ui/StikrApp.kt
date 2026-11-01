package com.stikr.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.stikr.app.data.StickerPack
import java.io.File
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stikr.app.data.PackRepository
import com.stikr.app.ui.neo.Neo
import com.stikr.app.ui.neo.NeoSnackbar
import com.stikr.app.ui.theme.NeoMotion
import com.stikr.app.ui.theme.AppearanceMode

private enum class Screen { HOME, PACK, EDITOR, THUMBNAIL, SETTINGS }

@Composable
fun StikrApp(
    viewModel: MainViewModel = viewModel(),
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    onAppearanceChange: (AppearanceMode) -> Unit = {},
) {
    val context = LocalContext.current
    var pendingExportId by rememberSaveable { mutableStateOf<String?>(null) }
    var celebration by remember { mutableStateOf<StickerPack?>(null) }
    var thumbnailSaving by remember { mutableStateOf(false) }
    var singleEditing by rememberSaveable { mutableStateOf(false) }
    var showLocalBackup by rememberSaveable { mutableStateOf(false) }
    var restoreUri by rememberSaveable { mutableStateOf<String?>(null) }
    val backupBusy by viewModel.localBackupBusy.collectAsStateWithLifecycle()
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { viewModel.exportLocalBackup(it) }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        restoreUri = uri?.toString()
    }
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val exportingPackId by viewModel.exportingPackId.collectAsStateWithLifecycle()
    val batch by viewModel.batch.collectAsStateWithLifecycle()
    val showTour by viewModel.showTour.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val whatsappLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val validationError = result.data?.getStringExtra("validation_error")
        when {
            !validationError.isNullOrBlank() -> viewModel.notify("WhatsApp couldn't add this pack: $validationError")
            result.resultCode == Activity.RESULT_OK -> {
                com.stikr.app.audio.AppSounds.success()
                celebration = viewModel.packs.value.firstOrNull { it.id == pendingExportId }
                viewModel.notify("Pack added to WhatsApp")
            }
        }
        pendingExportId = null
    }

    var screen by rememberSaveable { mutableStateOf(Screen.HOME.name) }
    var packId by rememberSaveable { mutableStateOf<String?>(null) }
    // The view model closes the editor before it hands back the pack it just
    // made, so the editor's own dismissal must not send the user back to the
    // pack we came from. This flag is alive only while that hand-over is
    // pending.
    var savingToNewPack by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let { text ->
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    // A save into a new pack that failed keeps the editor open and stops the
    // spinner, and the new pack never arrives: let the editor go again.
    LaunchedEffect(editor) {
        val state = editor
        if (savingToNewPack && state != null && !state.saving) savingToNewPack = false
    }

    fun openPack(id: String) {
        packId = id
        screen = Screen.PACK.name
    }

    fun openEditor(id: String) {
        singleEditing = false
        packId = id
        savingToNewPack = false // a fresh editor session starts clean
        viewModel.startEditor(id)
        screen = Screen.EDITOR.name
    }

    fun leaveEditor() {
        savingToNewPack = false
        viewModel.closeEditor()
        screen = if (singleEditing) Screen.HOME.name else Screen.PACK.name
        singleEditing = false
    }

    fun makeSticker() {
        viewModel.startSingleSticker { id ->
            packId = id
            singleEditing = true
            savingToNewPack = false
            screen = Screen.EDITOR.name
        }
    }

    fun exportPack(id: String) {
        viewModel.exportToWhatsApp(id) { intent ->
            try {
                pendingExportId = id
                whatsappLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                pendingExportId = null
                viewModel.notify("WhatsApp isn't available on this device")
            } catch (_: SecurityException) {
                pendingExportId = null
                viewModel.notify("Couldn't open WhatsApp. Please update WhatsApp and try again")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Neo.palette.paper)) {
        val currentBatch = batch
        when {
            // Overlays take the whole screen and are never animated: the tour
            // first, then a batch run, then the ordinary navigation stack.
            showTour -> TourOverlay(onDone = viewModel::completeTour)

            currentBatch != null -> BatchScreen(
                // Use repository snapshots so failed deletions never disappear optimistically.
                state = if (currentBatch.phase == BatchPhase.DONE) currentBatch.copy(
                    created = currentBatch.created.map { created -> packs.firstOrNull { it.id == created.id } ?: created },
                ) else currentBatch,
                onEditSticker = { id, fileName ->
                    packId = id
                    savingToNewPack = false
                    viewModel.editSticker(id, fileName)
                    viewModel.dismissBatch()
                    screen = Screen.EDITOR.name
                },
                onShareSticker = viewModel::shareSticker,
                onDeleteSticker = viewModel::deleteSticker,
                onExport = ::exportPack,
                exportingPackId = exportingPackId,
                onCancel = viewModel::cancelBatch,
                onDone = viewModel::dismissBatch,
                onOpenPack = { id ->
                    viewModel.dismissBatch()
                    openPack(id)
                },
            )

            else -> AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    // Short, hard slides: deeper screens come in from the right.
                    val forward = Screen.valueOf(targetState).ordinal > Screen.valueOf(initialState).ordinal
                    val direction = if (forward) 1 else -1
                    (
                        slideInHorizontally(tween(NeoMotion.screen)) { width -> direction * width / 6 } +
                            fadeIn(tween(NeoMotion.screen / 2))
                        ) togetherWith fadeOut(tween(NeoMotion.screen / 2))
                },
                label = "screen",
            ) { target ->
                when (target) {
                    Screen.HOME.name -> HomeScreen(
                        packs = packs,
                        onBackup = { showLocalBackup = true },
                        onOpenPack = ::openPack,
                        onCreatePack = { name -> viewModel.createPack(name) { openPack(it.id) } },
                        onMakeSticker = ::makeSticker,
                        onExportSingles = ::exportPack,
                        onDeleteSingle = viewModel::deleteSticker,
                        onStartBatch = { title, author, uris, removeBackground ->
                            viewModel.startBatch(title, author, uris, removeBackground)
                        },
                        onShowTour = viewModel::replayTour,
                        onSettings = { screen = Screen.SETTINGS.name },
                    )

                    Screen.PACK.name -> {
                        val pack = packs.firstOrNull { it.id == packId }
                        if (pack == null) {
                            if (screen == Screen.PACK.name) LaunchedEffect(packId) { screen = Screen.HOME.name }
                        } else {
                            PackScreen(
                                pack = pack,
                                onBack = { screen = Screen.HOME.name },
                                onRename = { viewModel.renamePack(pack.id, it) },
                                onRenameAuthor = { viewModel.renamePackAuthor(pack.id, it) },
                                onThumbnail = { screen = Screen.THUMBNAIL.name },
                                onDelete = { viewModel.deletePack(pack.id) { screen = Screen.HOME.name } },
                                onAddSticker = { openEditor(pack.id) },
                                onImportPhotos = { viewModel.importPhotos(pack.id, it) },
                                onEditSticker = { fileName ->
                                    viewModel.editSticker(pack.id, fileName)
                                    screen = Screen.EDITOR.name
                                },
                                importState = importing?.takeIf { it.packId == pack.id },
                                onDeleteSticker = { viewModel.deleteSticker(pack.id, it) },
                                onShareSticker = { viewModel.shareSticker(pack.id, it) },
                                onExport = { exportPack(pack.id) },
                                exporting = exportingPackId == pack.id,
                            )
                        }
                    }

                    Screen.THUMBNAIL.name -> {
                        val pack = packs.firstOrNull { it.id == packId }
                        if (pack == null) LaunchedEffect(Unit) { screen = Screen.HOME.name }
                        else ThumbnailEditor(
                            saving = thumbnailSaving,
                            initialFile = (pack.thumbnailImage ?: pack.stickers.firstOrNull())?.let {
                                File(context.filesDir, "packs/${pack.id}/$it")
                            },
                            onSave = { bytes ->
                                thumbnailSaving = true
                                viewModel.setPackThumbnail(pack.id, bytes) { saved ->
                                    thumbnailSaving = false
                                    if (saved) screen = Screen.PACK.name
                                }
                            },
                            onBack = { screen = Screen.PACK.name },
                        )
                    }

                    Screen.SETTINGS.name -> SettingsScreen(
                        mode = appearanceMode,
                        onSelect = onAppearanceChange,
                        onBack = { screen = Screen.HOME.name },
                    )

                    Screen.EDITOR.name -> {
                        val state = editor
                        if (state == null) {
                            // Also hit while the editor animates out after closing.
                            // While a new pack is being made, the dismissal waits
                            // for it instead of jumping back to the old pack.
                            if (screen == Screen.EDITOR.name && !savingToNewPack) {
                                LaunchedEffect(Unit) { screen = Screen.PACK.name }
                            }
                        } else {
                            val pack = packs.firstOrNull { it.id == state.packId }
                            EditorScreen(
                                state = state,
                                packName = pack?.name.orEmpty(),
                                onPick = viewModel::loadImage,
                                onCutout = viewModel::setCutout,
                                onStyle = viewModel::setStyle,
                                onSave = { viewModel.saveSticker { leaveEditor() } },
                                onSaveNew = {
                                    savingToNewPack = true
                                    viewModel.saveStickerToNewPack { newPack ->
                                        savingToNewPack = false
                                        openPack(newPack.id)
                                    }
                                },
                                // A full pack can't take another sticker; the editor
                                // offers a fresh one instead.
                                packFull = state.editingFileName == null && (pack?.stickerCount ?: 0) >= PackRepository.MAX_WHATSAPP_STICKERS,
                                onBack = { leaveEditor() },
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
        ) { data ->
            NeoSnackbar(data)
        }
        if (showLocalBackup) LocalBackupPanel(
            busy = backupBusy,
            onExport = { saveBackup.launch("stikr-backup-${java.time.LocalDate.now()}.zip") },
            onRestore = { openBackup.launch(arrayOf("application/zip", "application/octet-stream")) },
            onDismiss = { showLocalBackup = false },
        )
        restoreUri?.let { selected -> ConfirmDialog(
            kicker = "", title = "Restore this backup?",
            body = "Missing packs will be restored. For matching packs, the newer version is kept; local data wins ties.",
            confirmLabel = "Restore", dismissLabel = "Cancel",
            onDismiss = { restoreUri = null },
            onConfirm = { restoreUri = null; viewModel.restoreLocalBackup(android.net.Uri.parse(selected)) },
        ) }
        celebration?.let { pack -> PackAddedCelebration(pack) { celebration = null } }
    }

    // The editor owns its own back press — it asks before dropping a picked
    // photo — so this one only handles the pack screen. Compose gives the
    // press to the most recently composed *enabled* handler, and this one is
    // composed after the content: keep it disabled while an overlay is
    // showing, so TourOverlay (swallow) and BatchScreen (stop confirm / done)
    // keep their own back behaviour.
    BackHandler(enabled = screen == Screen.PACK.name && !showTour && batch == null) {
        screen = Screen.HOME.name
    }
}
