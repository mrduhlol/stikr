package com.stikr.app.ui.neo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stikr.app.ui.theme.NeoDimens
import com.stikr.app.ui.theme.NeoShapes

/**
 * A brutalist progress bar: framed track, a hard-edged hatched fill. With
 * [progress] null it is indeterminate and a block shuttles across the track;
 * the animation is read only while drawing.
 */
@Composable
fun NeoProgressBar(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    fill: Color = Neo.palette.purple,
    height: Dp = 22.dp,
    contentDescription: String? = null,
) {
    val palette = Neo.palette
    val phase: State<Float>? = if (progress == null) {
        val transition = rememberInfiniteTransition(label = "neo-progress")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 1100, easing = LinearEasing)),
            label = "neo-progress-phase",
        )
    } else {
        null
    }
    val hatch = palette.contentFor(fill).copy(alpha = 0.32f)
    val edge = palette.ink

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
                progressBarRangeInfo = if (progress == null) {
                    ProgressBarRangeInfo.Indeterminate
                } else {
                    ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
                }
            }
            .neoFrame(
                fill = palette.card,
                borderColor = palette.ink,
                shadowColor = palette.shadow,
                shape = NeoShapes.control,
                shadowOffset = NeoDimens.shadowSmall,
                clipContent = true,
            )
            .drawWithCache {
                val stripe = 9.dp.toPx()
                val rule = NeoDimens.border.toPx()
                // One diagonal-stripe path across the whole track, built per size.
                val stripes = Path().apply {
                    var x = -size.height
                    while (x < size.width + size.height) {
                        moveTo(x, size.height)
                        lineTo(x + size.height, 0f)
                        lineTo(x + size.height + stripe / 2f, 0f)
                        lineTo(x + stripe / 2f, size.height)
                        close()
                        x += stripe * 1.6f
                    }
                }
                onDrawBehind {
                    val (start, end) = if (phase == null) {
                        0f to size.width * (progress ?: 0f).coerceIn(0f, 1f)
                    } else {
                        val block = size.width * 0.38f
                        // Offset so the very first frame already shows the block.
                        val x = (size.width + block) * ((phase.value + 0.45f) % 1f) - block
                        x to x + block
                    }
                    if (end <= start) return@onDrawBehind
                    drawRect(fill, topLeft = Offset(start, 0f), size = Size(end - start, size.height))
                    clipRect(left = start, right = end) {
                        translate(left = if (phase == null) 0f else start) { drawPath(stripes, hatch) }
                    }
                    // hard ink edges where the fill meets the empty track
                    if (start > 0f) drawRect(edge, Offset(start - rule / 2f, 0f), Size(rule, size.height))
                    if (end < size.width) drawRect(edge, Offset(end - rule / 2f, 0f), Size(rule, size.height))
                }
            },
    )
}

/**
 * A segmented meter: [value] of [target] cells filled. Used for "stickers
 * until WhatsApp will take this pack".
 */
@Composable
fun NeoMeter(
    value: Int,
    target: Int,
    modifier: Modifier = Modifier,
    fill: Color = Neo.palette.ink,
    empty: Color = Neo.palette.card,
    edge: Color = Neo.palette.ink,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(target) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .neoFrame(
                        fill = if (index < value) fill else empty,
                        borderColor = edge,
                        shadowColor = edge,
                        shape = NeoShapes.tag,
                        borderWidth = NeoDimens.hairline,
                        shadowOffset = 0.dp,
                    ),
            )
        }
    }
}
