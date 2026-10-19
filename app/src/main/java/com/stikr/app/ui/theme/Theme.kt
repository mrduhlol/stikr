package com.stikr.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stikr.app.R

/**
 * stikr's look: neo-brutalist sticker book. Cream paper, thick ink borders,
 * big hard offset shadows that elements press into, flat fills, Anton
 * headlines and uppercase Space Mono labels. No blur, no gradients.
 */
@Immutable
data class NeoPalette(
    /** Screen background: warm cream. */
    val paper: Color,
    /** Raised surfaces: cards, fields. */
    val card: Color,
    /** Text and the border of every non-pastel surface. */
    val ink: Color,
    /** The hard offset shadow under cards and buttons. */
    val shadow: Color,
    /** Secondary text. Kept at ≥4.5:1 on paper and card. */
    val muted: Color,
    /** Highlight fill: selected chips, the add tile, the FAB. */
    val butter: Color,
    val cyan: Color,
    val green: Color,
    val pink: Color,
    /** The one loud accent: red. Fill of every primary action. */
    val cherry: Color,
    /** Text and icons on [cherry]. */
    val onCherry: Color,
    /** Red used as text on paper — lifted in dark mode to stay legible. */
    val cherryInk: Color,
    /** The app mark's purple: logo tile and progress fills. */
    val purple: Color,
    val onPurple: Color,
    /** Subtle fills: checkerboard, tracks, empty slots, disabled controls. */
    val creamDim: Color,
    /**
     * Text/icon/border colour for pastel fills (butter, cyan, green, pink).
     * Those fills stay light in both themes, so their content stays dark ink.
     */
    val onAccent: Color,
) {
    /**
     * The border and content colour for a surface of [fill]: dark ink on
     * light fills, [ink] otherwise. In light mode both are the same ink; in
     * dark mode this keeps pastel cards outlined in black and dark cards
     * outlined in cream.
     */
    fun edgeFor(fill: Color): Color = when (fill) {
        cherry -> ink
        purple -> ink
        else -> if (fill.luminance() > 0.4f) onAccent else ink
    }

    /** Text colour for content sitting on [fill]. */
    fun contentFor(fill: Color): Color = when (fill) {
        cherry -> onCherry
        purple -> onPurple
        else -> if (fill.luminance() > 0.4f) onAccent else ink
    }
}

private val Ink = Color(0xFF191715)

private val LightPalette = NeoPalette(
    paper = Color(0xFFF1EDE7),
    card = Color(0xFFFFFFFF),
    ink = Ink,
    shadow = Ink,
    muted = Color(0xFF5E5A52),
    butter = Color(0xFFF8DFA0),
    cyan = Color(0xFF9EDDE6),
    green = Color(0xFF9CE4A4),
    pink = Color(0xFFF3CDDA),
    cherry = Color(0xFFAE403E),
    onCherry = Color(0xFFFFFFFF),
    cherryInk = Color(0xFFAE403E),
    purple = Color(0xFF723AF4),
    onPurple = Color(0xFFFFFFFF),
    creamDim = Color(0xFFE4DDD2),
    onAccent = Ink,
)

/**
 * Dark mode is the same print inverted: near-black paper, cream ink, and
 * cream shadows — a black shadow on black paper would vanish. Pastels keep
 * their brand hexes so they read as the same stickers.
 */
private val DarkPalette = NeoPalette(
    paper = Color(0xFF13110D),
    card = Color(0xFF221F19),
    ink = Color(0xFFF3EFE6),
    shadow = Color(0xFFF3EFE6),
    muted = Color(0xFFADA595),
    butter = Color(0xFFF8DFA0),
    cyan = Color(0xFF9EDDE6),
    green = Color(0xFF9CE4A4),
    pink = Color(0xFFF3CDDA),
    cherry = Color(0xFFAE403E),
    onCherry = Color(0xFFFFFFFF),
    cherryInk = Color(0xFFEE7B72),
    purple = Color(0xFF723AF4),
    onPurple = Color(0xFFFFFFFF),
    creamDim = Color(0xFF2F2B23),
    onAccent = Ink,
)

val LocalNeoPalette = staticCompositionLocalOf { LightPalette }

/** Anton: the heavy condensed display face used for every headline. */
val Anton = FontFamily(Font(R.font.anton_regular))

/** Space Mono Bold: uppercase labels, buttons, counters. */
val SpaceMono = FontFamily(Font(R.font.space_mono_bold, FontWeight.Bold))

private fun scheme(p: NeoPalette, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.cherry,
        onPrimary = p.onCherry,
        primaryContainer = p.butter,
        onPrimaryContainer = p.onAccent,
        secondary = p.green,
        onSecondary = p.onAccent,
        secondaryContainer = p.creamDim,
        onSecondaryContainer = p.ink,
        tertiary = p.purple,
        onTertiary = p.onPurple,
        background = p.paper,
        onBackground = p.ink,
        surface = p.card,
        onSurface = p.ink,
        surfaceVariant = p.creamDim,
        onSurfaceVariant = p.muted,
        surfaceContainer = p.card,
        surfaceContainerHigh = p.card,
        surfaceContainerHighest = p.creamDim,
        outline = p.ink,
        outlineVariant = p.ink.copy(alpha = 0.35f),
        error = p.cherry,
        onError = p.onCherry,
        errorContainer = p.pink,
        onErrorContainer = p.onAccent,
        inverseSurface = p.ink,
        inverseOnSurface = p.paper,
        scrim = Color.Black,
    )
}

private val NeoTypography = Typography(
    displayLarge = TextStyle(fontFamily = Anton, fontSize = 60.sp, lineHeight = 60.sp, letterSpacing = 0.sp),
    displayMedium = TextStyle(fontFamily = Anton, fontSize = 46.sp, lineHeight = 48.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = Anton, fontSize = 38.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = Anton, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = 0.2.sp),
    headlineMedium = TextStyle(fontFamily = Anton, fontSize = 28.sp, lineHeight = 31.sp, letterSpacing = 0.2.sp),
    headlineSmall = TextStyle(fontFamily = Anton, fontSize = 24.sp, lineHeight = 27.sp, letterSpacing = 0.3.sp),
    titleLarge = TextStyle(fontFamily = Anton, fontSize = 21.sp, lineHeight = 25.sp, letterSpacing = 0.3.sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.6.sp),
    labelMedium = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, lineHeight = 15.sp, letterSpacing = 0.8.sp),
    labelSmall = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 1.sp),
)

private val NeoMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

private val LightScheme = scheme(LightPalette, dark = false)
private val DarkScheme = scheme(DarkPalette, dark = true)

@Composable
fun StikrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalNeoPalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            shapes = NeoMaterialShapes,
            typography = NeoTypography,
            content = content,
        )
    }
}
