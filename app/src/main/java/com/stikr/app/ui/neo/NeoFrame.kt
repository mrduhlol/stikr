package com.stikr.app.ui.neo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.stikr.app.ui.theme.NeoDimens
import com.stikr.app.ui.theme.NeoMotion
import com.stikr.app.ui.theme.NeoShapes
import kotlinx.coroutines.launch

/** A press that never lets go: for "selected" swatches that sit sunk in. */
val Sunk: State<Float> = object : State<Float> {
    override val value: Float = 1f
}

/**
 * The one modifier the whole look rests on: a flat fill, an ink border drawn
 * inside the layout bounds, and a hard, un-blurred offset shadow behind it.
 *
 * [press] (0..1) sinks the face into its shadow. It is read only while
 * drawing, so a press animates without recomposing or relaying out anything;
 * the shadow stays put and the face (with its content) slides onto it.
 */
fun Modifier.neoFrame(
    fill: Color,
    borderColor: Color,
    shadowColor: Color,
    shape: Shape = NeoShapes.card,
    borderWidth: Dp = NeoDimens.border,
    shadowOffset: Dp = NeoDimens.shadow,
    press: State<Float>? = null,
    clipContent: Boolean = false,
): Modifier = this then NeoFrameElement(
    shape = shape,
    fill = fill,
    borderColor = borderColor,
    borderWidth = borderWidth,
    shadowOffset = shadowOffset,
    shadowColor = shadowColor,
    press = press,
    clipContent = clipContent,
)

/**
 * Press progress for [interaction], 0 at rest and 1 fully sunk. A quick tap
 * still travels all the way down before springing back, so it reads.
 */
@Composable
fun rememberPressProgress(interaction: InteractionSource, enabled: Boolean = true): State<Float> {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(interaction, enabled) {
        if (!enabled) {
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        var held = 0
        interaction.interactions.collect { event ->
            when (event) {
                is PressInteraction.Press -> {
                    held++
                    launch { progress.animateTo(1f, tween(NeoMotion.press, easing = LinearEasing)) }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    held = (held - 1).coerceAtLeast(0)
                    if (held == 0) {
                        launch {
                            if (progress.value < 1f) {
                                progress.animateTo(1f, tween(NeoMotion.press, easing = LinearEasing))
                            }
                            progress.animateTo(0f, tween(NeoMotion.release))
                        }
                    }
                }
            }
        }
    }
    return progress.asState()
}

private data class NeoFrameElement(
    val shape: Shape,
    val fill: Color,
    val borderColor: Color,
    val borderWidth: Dp,
    val shadowOffset: Dp,
    val shadowColor: Color,
    val press: State<Float>?,
    val clipContent: Boolean,
) : ModifierNodeElement<NeoFrameNode>() {
    override fun create() = NeoFrameNode(
        shape, fill, borderColor, borderWidth, shadowOffset, shadowColor, press, clipContent,
    )

    override fun update(node: NeoFrameNode) {
        val geometryChanged = node.shape != shape ||
            node.borderWidth != borderWidth ||
            node.clipContent != clipContent
        node.shape = shape
        node.fill = fill
        node.borderColor = borderColor
        node.borderWidth = borderWidth
        node.shadowOffset = shadowOffset
        node.shadowColor = shadowColor
        node.press = press
        node.clipContent = clipContent
        if (geometryChanged) node.dropCache()
        node.invalidateDraw()
    }
}

private class NeoFrameNode(
    var shape: Shape,
    var fill: Color,
    var borderColor: Color,
    var borderWidth: Dp,
    var shadowOffset: Dp,
    var shadowColor: Color,
    var press: State<Float>?,
    var clipContent: Boolean,
) : Modifier.Node(), DrawModifierNode {

    // The outline is rebuilt only when the size, direction or density changes.
    // It is inset by half the border so the stroke's outer edge lands exactly
    // on the layout bounds; fill, clip and shadow all use the same outline, so
    // no fill can peek past the border at a rounded corner.
    private var cachedSize = Size.Unspecified
    private var cachedDirection: LayoutDirection? = null
    private var cachedDensity = 0f
    private var outline: Outline? = null
    private var stroke: Stroke? = null
    private var clip: Path? = null

    fun dropCache() {
        outline = null
    }

    override fun ContentDrawScope.draw() {
        val borderPx = if (borderWidth > 0.dp) borderWidth.toPx() else 0f
        if (outline == null || cachedSize != size || cachedDirection != layoutDirection || cachedDensity != density) {
            val built = shape.createOutline(Size(size.width - borderPx, size.height - borderPx), layoutDirection, this)
            outline = built
            stroke = if (borderPx > 0f) Stroke(width = borderPx) else null
            clip = if (clipContent) Path().apply { addOutlineOf(built) } else null
            cachedSize = size
            cachedDirection = layoutDirection
            cachedDensity = density
        }
        val edge = outline ?: return
        val inset = borderPx / 2f
        val style = stroke
        val depth = shadowOffset.toPx()
        val sink = depth * (press?.value ?: 0f).coerceIn(0f, 1f)

        if (depth > 0f && sink < depth) {
            translate(depth + inset, depth + inset) {
                drawOutline(edge, color = shadowColor)
                if (style != null) drawOutline(edge, color = shadowColor, style = style)
            }
        }
        translate(sink, sink) {
            translate(inset, inset) { drawOutline(edge, color = fill) }
            val path = clip
            if (path != null) {
                translate(inset, inset) {
                    clipPath(path) {
                        translate(-inset, -inset) { this@draw.drawContent() }
                    }
                }
            } else {
                this@draw.drawContent()
            }
            if (style != null) {
                translate(inset, inset) { drawOutline(edge, color = borderColor, style = style) }
            }
        }
    }
}

private fun Path.addOutlineOf(outline: Outline) {
    when (outline) {
        is Outline.Rectangle -> addRect(outline.rect)
        is Outline.Rounded -> addRoundRect(outline.roundRect)
        is Outline.Generic -> addPath(outline.path)
    }
}
