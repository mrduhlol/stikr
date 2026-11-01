package com.stikr.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stikr.app.R
import com.stikr.app.ui.neo.*

/** Full-screen first-run composition. Skip and finish share the persisted completion callback. */
@Composable
fun TourOverlay(onDone: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(0) }
    val p = Neo.palette
    val titles = listOf("Your reactions.\nYour collection.", "One edit.\nEvery sticker.", "A little slide.\nA big reaction.")
    val bodies = listOf(
        "Pick the photos that say it best. Make a sticker, or bring them together in a pack.",
        "Set the look for the whole pack. Keep the original edges, add words, or fine-tune one sticker at a time.",
        "Feel a gentle tick as you slide to WhatsApp. Save a local backup whenever you want to keep a copy.",
    )
    BackHandler { }
    Column(Modifier.fillMaxSize().collectionBackground().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.stikr_ghost), "stikr", Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)))
            Spacer(Modifier.weight(1f))
            Text("0${page + 1} / 03", style = MaterialTheme.typography.labelMedium, color = p.muted)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            TourArtwork(page, Modifier.fillMaxWidth().height(390.dp).padding(vertical = 12.dp))
            Text(titles[page], style = MaterialTheme.typography.headlineLarge, color = p.ink, modifier = Modifier.semantics { heading() })
            Text(bodies[page], style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.padding(top = 16.dp, bottom = 24.dp))
        }
        Row(Modifier.padding(top = 18.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { index ->
                Box(Modifier.width(if (index == page) 35.dp else 21.dp).height(3.dp)
                    .background(if (index == page) Color(0xFFBACB91) else Color(0xFF353A32), RoundedCornerShape(3.dp)))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = { if (page > 0) page-- else onDone() }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (page > 0) "Back" else "Skip", color = p.muted)
            }
            NeoButton(if (page == 2) "Get started" else "Continue",
                onClick = { if (page == 2) onDone() else page++ },
                modifier = Modifier.weight(1f), trailingIcon = Icons.AutoMirrored.Filled.ArrowForward)
        }
    }
}

/** The same collectible card and supplied reaction photos used in the collection. */
@Composable
private fun TourArtwork(page: Int, modifier: Modifier) {
    val p = Neo.palette
    val photos = listOf(R.drawable.tour_reaction_ha, R.drawable.tour_reaction_grin, R.drawable.tour_reaction_shock,
        R.drawable.tour_reaction_joker, R.drawable.tour_reaction_doge, R.drawable.tour_reaction_wallet)
    Box(modifier, contentAlignment = Alignment.Center) {
        when(page) {
            0 -> PackCollectionCard(
                com.stikr.app.data.StickerPack("tour", "Reactions", "Abhishek", 1793491200000,
                    (1..10).map { "$it.webp" }, serialNumber=1, displayId="86483904"),
                Modifier.width(166.dp).rotate(-6f), coverResource=R.drawable.tour_preview_cover,
            )
            1 -> Column(Modifier.rotate(-5f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                photos.chunked(3).forEach { row ->
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        row.forEach { image ->
                            Image(painterResource(image), "Sticker example", Modifier.size(76.dp).clip(RoundedCornerShape(10.dp))
                                .background(p.card).padding(6.dp), contentScale=ContentScale.Fit)
                        }
                    }
                }
                Text("All 10 · outline off by default",style=MaterialTheme.typography.labelMedium,color=p.ink,
                    modifier=Modifier.padding(top=12.dp).background(p.creamDim,RoundedCornerShape(8.dp)).padding(12.dp))
            }
            else -> Column(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("you know exactly what I mean.",color=p.ink,style=MaterialTheme.typography.bodySmall,
                    modifier=Modifier.align(Alignment.End).background(p.creamDim,RoundedCornerShape(12.dp)).padding(14.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.tour_reaction_ha),"Reaction in a chat mockup",Modifier.size(110.dp).rotate(-7f).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
                    Image(painterResource(R.drawable.tour_reaction_wallet),"Another reaction sticker",Modifier.size(85.dp).rotate(7f).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop)
                }
                Text("Your pack. Right there in the chat.",style=MaterialTheme.typography.bodySmall,color=p.muted)
                Row(Modifier.fillMaxWidth().background(p.creamDim,RoundedCornerShape(28.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward,null,tint=p.ink)
                    Text("Slide to add to WhatsApp",style=MaterialTheme.typography.labelSmall,color=p.ink,modifier=Modifier.weight(1f))
                    WhatsAppMark(Modifier.size(22.dp))
                }
            }
        }
    }
}
