package com.stikr.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The neo-brutalist measurements. Every frame in the app is drawn from
 * these, so the grid stays rigid: one border weight, three shadow depths.
 */
object NeoDimens {
    /** Ink border on every framed surface. */
    val border = 3.dp

    /** Thin rules: dividers, tags, the checkerboard frame. */
    val hairline = 2.dp

    /** Hard shadow depths. Pressing an element sinks it by its depth. */
    val shadowSmall = 3.dp
    val shadow = 5.dp
    val shadowLarge = 8.dp

    /** Minimum touch target, and the size of every icon button. */
    val touch = 48.dp

    /** Screen side margins and the gaps of the grid. */
    val gutter = 20.dp
    val gap = 16.dp
    val gapSmall = 12.dp
}

/** Shapes are allocated once here, never per recomposition. */
object NeoShapes {
    val card = RoundedCornerShape(6.dp)
    val control = RoundedCornerShape(4.dp)
    val tag = RoundedCornerShape(2.dp)
    val tile = RoundedCornerShape(6.dp)
}

/** Durations: short and snappy, brutalism doesn't ease around. */
object NeoMotion {
    const val press = 60
    const val release = 110
    const val screen = 200
}
