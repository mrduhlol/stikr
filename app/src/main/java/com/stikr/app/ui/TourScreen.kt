package com.stikr.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stikr.app.ui.neo.Neo
import com.stikr.app.ui.neo.NeoButton
import com.stikr.app.ui.neo.NeoCard
import com.stikr.app.ui.neo.NeoOutlinedButton
import com.stikr.app.ui.neo.StickerHeadline
import com.stikr.app.ui.neo.neoFrame
import com.stikr.app.ui.theme.NeoDimens

/**
 * The first-run tour: one centred card over home, three pages with dots and a
 * way out on every page. Finishing or skipping both call [onDone]; back is
 * swallowed so an accidental press can't lose the tour.
 */
@Composable
fun TourOverlay(onDone: () -> Unit) {
    val palette = Neo.palette
    var pageIndex by rememberSaveable { mutableStateOf(0) }
    val page = TourPages[pageIndex]
    val badgeFills = listOf(palette.cyan, palette.butter, palette.green)
    val lastPage = pageIndex == TourPages.lastIndex

    BackHandler { }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper),
        contentAlignment = Alignment.Center,
    ) {
        NeoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NeoDimens.gutter),
            shadow = NeoDimens.shadowLarge,
            padding = PaddingValues(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TourBadge(
                    emoji = page.emoji,
                    fill = badgeFills[pageIndex % badgeFills.size],
                )
                Spacer(Modifier.height(26.dp))
                StickerHeadline(text = page.title, fontSize = 28.sp)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = page.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(TourPages.size) { dot ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (dot == pageIndex) palette.ink
                                    else palette.ink.copy(alpha = 0.25f),
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NeoOutlinedButton(
                        text = "Skip",
                        onClick = onDone,
                        modifier = Modifier.weight(1f),
                    )
                    NeoButton(
                        text = if (lastPage) "Get started" else "Next",
                        onClick = {
                            if (lastPage) onDone() else pageIndex += 1
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * The page's sticker: a tilted, framed disc with the emoji on it. The kit has
 * no emoji badge of its own, so the badge is drawn with the same frame every
 * other surface uses.
 */
@Composable
private fun TourBadge(emoji: String, fill: Color) {
    val palette = Neo.palette
    Box(
        modifier = Modifier
            .rotate(-5f)
            .size(84.dp)
            .neoFrame(
                fill = fill,
                borderColor = palette.ink,
                shadowColor = palette.shadow,
                shape = CircleShape,
                borderWidth = NeoDimens.border,
                shadowOffset = NeoDimens.shadowSmall,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 36.sp)
    }
}

private data class TourPage(
    val emoji: String,
    val title: String,
    val body: String,
)

private val TourPages = listOf(
    TourPage(
        emoji = "\u2728",
        title = "Photos in, stickers out",
        body = "stikr turns a photo into a sticker on your phone \u2014 cut out, outlined, " +
            "captioned. Nothing is uploaded. Ever.",
    ),
    TourPage(
        emoji = "\u2702\uFE0F",
        title = "Pick, cut, dress up",
        body = "Select photos together, then tap any sticker in its pack to edit. Keep the full photo " +
            "or turn on auto cut-out, and add an outline or a caption.",
    ),
    TourPage(
        emoji = "\uD83D\uDCE6",
        title = "Packs, ready for WhatsApp",
        body = "Stickers live in packs of up to 30 and import straight into WhatsApp. " +
            "Got a pile of photos? Start a batch \u2014 many photos in, packs out.",
    ),
)
