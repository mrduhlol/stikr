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

/** Plum surfaces, soft violet and olive accents, and readable native sans typography. */
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

private val Ink = Color(0xFF211B21)

private val LightPalette = NeoPalette(
    paper = Color(0xFFF1EDE7),
    card = Color(0xFFFFFFFF),
    ink = Ink,
    shadow = Ink,
    muted = Color(0xFF5E5A52),
    butter = Color(0xFFD5F277),
    cyan = Color(0xFF9EDDE6),
    green = Color(0xFF9CE4A4),
    pink = Color(0xFFF3CDDA),
    cherry = Color(0xFFD5F277),
    onCherry = Color(0xFF263113),
    cherryInk = Color(0xFFD5F277),
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
    paper = Color(0xFF201824),
    card = Color(0xFF302637),
    ink = Color(0xFFFFF9FF),
    shadow = Color(0xFFFFF9FF),
    muted = Color(0xFFC4B5CF),
    butter = Color(0xFFD5F277),
    cyan = Color(0xFF9EDDE6),
    green = Color(0xFF9CE4A4),
    pink = Color(0xFFF3CDDA),
    cherry = Color(0xFFD5F277),
    onCherry = Color(0xFF263113),
    cherryInk = Color(0xFFD5F277),
    purple = Color(0xFF723AF4),
    onPurple = Color(0xFFFFFFFF),
    creamDim = Color(0xFF392D42),
    onAccent = Ink,
)

private val AmoledPalette = DarkPalette.copy(
    paper = Color.Black,
    card = Color(0xFF0D0D0D),
    creamDim = Color(0xFF1B1B1B),
)

val LocalNeoPalette = staticCompositionLocalOf { DarkPalette }

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
        error = Color(0xFFEEA1AE),
        onError = Color(0xFF391923),
        errorContainer = p.pink,
        onErrorContainer = p.onAccent,
        inverseSurface = p.ink,
        inverseOnSurface = p.paper,
        scrim = Color.Black,
    )
}

private val NeoTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 60.sp, lineHeight = 60.sp, letterSpacing = 0.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 46.sp, lineHeight = 48.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = 0.2.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 31.sp, letterSpacing = 0.2.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 27.sp, letterSpacing = 0.3.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 25.sp, letterSpacing = 0.3.sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.6.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, lineHeight = 15.sp, letterSpacing = 0.8.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 1.sp),
)

private val NeoMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val LightScheme = scheme(LightPalette, dark = false)
private val DarkScheme = scheme(DarkPalette, dark = true)
private val AmoledScheme = scheme(AmoledPalette, dark = true)

@Composable
fun StikrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = DarkPalette
    CompositionLocalProvider(LocalNeoPalette provides palette) {
        MaterialTheme(
            colorScheme = DarkScheme,
            shapes = NeoMaterialShapes,
            typography = NeoTypography,
            content = content,
        )
    }
}
