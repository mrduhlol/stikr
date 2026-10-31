package com.stikr.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Shared spacing and controls; collectible cards define their own sharp silhouette. */
object NeoDimens {
    /** Quiet surface outlines. */
    val border = 1.dp

    /** Thin rules: dividers, tags, the checkerboard frame. */
    val hairline = 1.dp

    /** Legacy frame API remains source compatible without offset shadows. */
    val shadowSmall = 0.dp
    val shadow = 0.dp
    val shadowLarge = 0.dp

    /** Minimum touch target, and the size of every icon button. */
    val touch = 48.dp

    /** Screen side margins and the gaps of the grid. */
    val gutter = 20.dp
    val gap = 16.dp
    val gapSmall = 12.dp
}

/** Shapes are allocated once here, never per recomposition. */
object NeoShapes {
    val card = RoundedCornerShape(14.dp)
    val control = RoundedCornerShape(12.dp)
    val tag = RoundedCornerShape(2.dp)
    val tile = RoundedCornerShape(14.dp)
}

/** Durations: short and snappy, brutalism doesn't ease around. */
object NeoMotion {
    const val press = 60
    const val release = 110
    const val screen = 200
}
