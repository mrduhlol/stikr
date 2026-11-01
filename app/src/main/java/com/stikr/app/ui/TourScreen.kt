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
    val titles = listOf("Your photos.\nYour personality.", "A little\nmore you.", "Made for\nthe chat.")
    val bodies = listOf(
        "Turn little moments into stickers.\nMake one, or build a whole collection.",
        "Shape the corners. Find your font.\nAdd words, color, and a fine outline.",
        "Your stickers, together in a pack.\nSlide to add them to WhatsApp.",
    )
    BackHandler { }
    Column(Modifier.fillMaxSize().collectionBackground().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.stikr_ghost), "stikr", Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)))
            Spacer(Modifier.weight(1f))
            Text("0${page + 1} / 03", style = MaterialTheme.typography.labelMedium, color = p.muted)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            TourArtwork(page, Modifier.fillMaxWidth().heightIn(min = 230.dp).padding(vertical = 22.dp))
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
            TextButton(onClick = { if (page == 2) page-- else onDone() }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (page == 2) "Back" else "Skip", color = p.muted)
            }
            NeoButton(if (page == 2) "Get started" else "Continue",
                onClick = { if (page == 2) onDone() else page++ },
                modifier = Modifier.weight(1f), trailingIcon = Icons.AutoMirrored.Filled.ArrowForward)
        }
    }
}

/** Product imagery from the approved preview, with the actual pack's sharp frame language. */
@Composable
private fun TourArtwork(page: Int, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (page == 1) {
            Column(Modifier.rotate(3f).size(218.dp, 185.dp).clip(RoundedCornerShape(15.dp))
                .background(Color(0xFF20271C)).border(1.dp, Color(0xFF48523B), RoundedCornerShape(15.dp)),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("hello.", fontSize = 55.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, color = Color(0xFFE0DECD))
                Text("Aa     ●     ⌜", fontSize = 20.sp, color = Color(0xFFB2BAA6), modifier = Modifier.padding(top = 18.dp))
            }
        } else if (page == 0) {
            Image(painterResource(R.drawable.tour_sample_photo), "Sample photo sticker",
                Modifier.rotate(-9f).size(151.dp, 190.dp).clip(RoundedCornerShape(15.dp)), contentScale = ContentScale.Crop)
            Text("hello.", fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 32.sp,
                color = Color(0xFF292B25), modifier = Modifier.align(Alignment.BottomCenter).offset(x = 48.dp, y = (-8).dp).rotate(7f)
                    .background(Color(0xFFD7D2C4), RoundedCornerShape(9.dp)).padding(horizontal = 15.dp, vertical = 9.dp))
        } else {
            Column(Modifier.rotate(-5f).width(157.dp).background(Color(0xFFB4B9A1)).padding(8.dp)) {
                Box {
                    Image(painterResource(R.drawable.tour_sample_photo), "Sample pack cover", Modifier.fillMaxWidth().height(154.dp), contentScale = ContentScale.Crop)
                    Text("▰▰▰▰▰\nLVL 001", color = Color(0xFF1C2019), fontSize = 8.sp,
                        modifier = Modifier.background(Color(0xFFB4B9A1)).padding(5.dp))
                }
                Text("NOV 2026 · #48217       12/30", fontSize = 7.sp, color = Color(0xFF1C2019), modifier = Modifier.padding(top = 8.dp))
                Text("Night ghosts", fontSize = 20.sp, color = Color(0xFF1C2019))
            }
            Surface(Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 10.dp).size(49.dp),
                shape = RoundedCornerShape(25.dp), color = Color(0xFF232B1C), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF545F41))) {
                Box(contentAlignment = Alignment.Center) { WhatsAppMark(Modifier.size(25.dp), Color(0xFFCCDBA8)) }
            }
        }
    }
}
