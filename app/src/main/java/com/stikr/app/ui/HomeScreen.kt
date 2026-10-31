package com.stikr.app.ui

import android.net.Uri
import androidx.compose.ui.res.painterResource
import com.stikr.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import com.stikr.app.ui.neo.*
import com.stikr.app.ui.theme.NeoDimens
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    packs: List<StickerPack>,
    onOpenPack: (String) -> Unit,
    onCreatePack: (String) -> Unit,
    onMakeSticker: () -> Unit,
    onStartBatch: (String, String, List<Uri>, Boolean) -> Unit,
    onShowTour: () -> Unit,
    onSettings: () -> Unit = {},
    onExportSingles: (String) -> Unit = {},
    onDeleteSingle: (String, String) -> Unit = { _, _ -> },
) {
    val palette=Neo.palette
    var showCreateBatch by rememberSaveable { mutableStateOf(false) }
    var pendingTitle by rememberSaveable { mutableStateOf("") }
    var pendingAuthor by rememberSaveable { mutableStateOf(PackRepository.DEFAULT_AUTHOR) }
    var pendingRemoveBackground by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Pair<StickerPack,String>?>(null) }
    var exportPack by remember { mutableStateOf<StickerPack?>(null) }
    var deleteSingle by remember { mutableStateOf<Pair<StickerPack,String>?>(null) }
    var fabMenu by remember { mutableStateOf(false) }
    val state=rememberLazyGridState()
    val floating by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo.isNotEmpty() && state.layoutInfo.visibleItemsInfo.none { it.key=="create" } && state.firstVisibleItemIndex>1 } }
    val singles=packs.filter { it.isSingles }.flatMap { pack -> pack.stickers.filterNot { it in pack.starterStickers }.map { pack to it } }
    val normal=packs.filterNot { it.isSingles }
    val root=File(LocalContext.current.filesDir,"packs")
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris -> if(uris.isNotEmpty()) onStartBatch(pendingTitle,pendingAuthor,uris,pendingRemoveBackground) }
    val bars=WindowInsets.systemBars.asPaddingValues()
    Box(Modifier.fillMaxSize().collectionBackground()) {
        LazyVerticalGrid(columns=GridCells.Fixed(2),state=state,modifier=Modifier.fillMaxSize().blur(if(exportPack!=null || deleteSingle!=null) 6.dp else 0.dp),contentPadding=PaddingValues(20.dp,bars.calculateTopPadding()+18.dp,20.dp,bars.calculateBottomPadding()+90.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item(span={GridItemSpan(maxLineSpan)},key="brand") { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Image(painterResource(R.drawable.stikr_ghost),"stikr",Modifier.size(36.dp).clip(CircleShape))
                Spacer(Modifier.weight(1f))
                SoundToggle()
            } }
            item(span={GridItemSpan(maxLineSpan)},key="intro") { Column(Modifier.padding(top=14.dp,bottom=10.dp)) { Text("Your little collections.",fontSize=30.sp,lineHeight=34.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-.8).sp,color=palette.ink); Text("Photos, words, and a little personality.",style=MaterialTheme.typography.bodyMedium,color=palette.muted,modifier=Modifier.padding(top=10.dp)) } }
            item(span={GridItemSpan(maxLineSpan)},key="create") { Row(Modifier.padding(bottom=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                CreationControl("Create a\nsticker pack","Select photos together",true,Modifier.weight(1f)) { showCreateBatch=true }
                CreationControl("Create a\nsticker","Edit one photo",false,Modifier.weight(1f),onMakeSticker)
            } }
            item(span={GridItemSpan(maxLineSpan)},key="your-packs") { Row(Modifier.padding(bottom=8.dp),verticalAlignment=Alignment.CenterVertically) { Text("Your packs",fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=palette.ink,modifier=Modifier.weight(1f)); Text("${normal.size} packs",style=MaterialTheme.typography.bodySmall,color=palette.muted) } }
            item(span={GridItemSpan(maxLineSpan)},key="singles") { Column {
                Row { Text("Single stickers",style=MaterialTheme.typography.titleSmall,color=palette.ink,modifier=Modifier.weight(1f)); Text("${singles.size} saved",style=MaterialTheme.typography.bodySmall,color=palette.muted) }
                LazyRow(Modifier.padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) { items(singles,key={it.first.id+it.second}) { (pack,name) ->
                    Box { Surface(Modifier.size(83.dp,86.dp).combinedClickable(onClick={},onLongClick={ selected=pack to name },onLongClickLabel="Sticker actions"),shape=RoundedCornerShape(14.dp),color=palette.card,border=BorderStroke(1.dp,Color(0xFF58435F))) { StickerImage(root.resolve(pack.id).resolve(name),"Single sticker",Modifier.padding(5.dp).fillMaxSize()) }
                        DropdownMenu(expanded=selected?.first?.id==pack.id && selected?.second==name,onDismissRequest={selected=null}) {
                            DropdownMenuItem(text={Text("Add to WhatsApp")},onClick={selected=null;exportPack=pack})
                            DropdownMenuItem(text={Text("Delete")},onClick={selected=null;deleteSingle=pack to name})
                        }
                    }
                } }
                Text(if(singles.isEmpty()) "Your single stickers will collect here." else "Long-press to add to WhatsApp or delete",fontSize=11.sp,color=palette.muted,modifier=Modifier.padding(top=10.dp))
                HorizontalDivider(Modifier.padding(top=22.dp,bottom=18.dp),color=Color(0xFF544058))
                Text("Sticker packs",style=MaterialTheme.typography.titleSmall,color=palette.ink,modifier=Modifier.padding(bottom=2.dp))
            } }
            items(normal,key={it.id}) { pack -> PackCollectionCard(pack,onClick={onOpenPack(pack.id)}) }
            if(normal.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) { Text("Create your first pack above.",color=palette.muted,modifier=Modifier.padding(vertical=30.dp)) }
        }
        if(floating) Box(Modifier.align(Alignment.BottomEnd).padding(end=20.dp,bottom=bars.calculateBottomPadding()+20.dp)) {
            FloatingActionButton(onClick={fabMenu=!fabMenu},containerColor=palette.butter,contentColor=palette.onAccent) { Icon(Icons.Default.Add,"Create a pack or single sticker") }
            DropdownMenu(expanded=fabMenu,onDismissRequest={fabMenu=false}) {
                DropdownMenuItem(text={Text("Create a sticker pack")},onClick={fabMenu=false;showCreateBatch=true})
                DropdownMenuItem(text={Text("Create a sticker")},onClick={fabMenu=false;onMakeSticker()})
            }
        }
    }
    exportPack?.let { pack -> Dialog(onDismissRequest={exportPack=null}) { Surface(shape=RoundedCornerShape(23.dp),color=palette.card) { Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        WhatsAppMark(Modifier.padding(top=8.dp,bottom=18.dp).size(34.dp))
        Text("Take your singles along.",style=MaterialTheme.typography.headlineSmall,color=palette.ink)
        Text("WhatsApp will import all ${pack.stickerCount} stickers in your Stikr pack.",style=MaterialTheme.typography.bodyMedium,color=palette.muted,modifier=Modifier.padding(vertical=16.dp))
        WhatsAppPackAction(pack.stickerCount,false) { exportPack=null;onExportSingles(pack.id) }
        TextButton(onClick={exportPack=null}) { Text("Close") }
    } } } }
    deleteSingle?.let { (pack,name) -> ConfirmDialog(kicker="",title="Delete this sticker?",body="It will be removed from your singles. Copies already in WhatsApp will stay.",confirmLabel="Delete",dismissLabel="Keep sticker",onDismiss={deleteSingle=null},onConfirm={deleteSingle=null;onDeleteSingle(pack.id,name)}) }
    if(showCreateBatch) CreateBatchDialog(onDismiss={showCreateBatch=false},onConfirm={title,author,removeBackground -> showCreateBatch=false;pendingTitle=title;pendingAuthor=author;pendingRemoveBackground=removeBackground;picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
}

@Composable
private fun CreationControl(title:String,subtitle:String,primary:Boolean,modifier:Modifier,onClick:()->Unit) {
    val p=Neo.palette
    Surface(onClick={ com.stikr.app.audio.AppSounds.click(); onClick() },modifier=modifier.heightIn(min=144.dp),shape=RoundedCornerShape(15.dp),color=if(primary)p.butter else p.card,contentColor=if(primary)p.onAccent else p.ink) { Column(Modifier.padding(15.dp)) {
        Icon(if(primary) Icons.Default.Add else Icons.Default.Edit,null,Modifier.size(24.dp))
        Text(title,fontSize=18.sp,lineHeight=21.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=10.dp,bottom=7.dp))
        Text(subtitle,fontSize=11.sp,color=if(primary)Color(0xFF4B592B) else p.muted)
    } }
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
