package com.stikr.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.stikr.app.render.StickerTypography
import com.stikr.app.ui.neo.*
import kotlin.math.roundToInt

private val textColors = listOf(
    "Plum" to 0xFF211A2C.toInt(), "Ivory" to 0xFFF5F1DC.toInt(),
    "Violet" to 0xFFBDA1ED.toInt(), "Olive" to 0xFFD5F277.toInt(),
    "Pink" to 0xFFF29ABB.toInt(), "Black" to 0xFF000000.toInt(),
    "White" to 0xFFFFFFFF.toInt(), "Ink" to 0xFF191715.toInt(),
    "Butter" to 0xFFF8DFA0.toInt(), "Cherry" to 0xFFAE403E.toInt(),
)

@Composable
internal fun TextStyleControls(
    font: String, size: Int, bold: Boolean, background: Int, textColor: Int,
    onFont: (String) -> Unit, onSize: (Int) -> Unit, onBold: (Boolean) -> Unit,
    onBackground: (Int) -> Unit, onTextColor: (Int) -> Unit,
    sizeRange: ClosedFloatingPointRange<Float> = 16f..170f,
) {
    val palette = Neo.palette
    // Own the rhythm so host columns cannot add a second gap to every control.
    Column(Modifier.fillMaxWidth()) {
        Text("Font", color = palette.muted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        StickerTypography.fonts.chunked(3).forEach { choices ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { choice ->
                    NeoButton(
                        text = StickerTypography.label(choice), onClick = { onFont(choice) },
                        modifier = Modifier.weight(1f).semantics { selected = font == choice },
                        fill = if (font == choice) palette.cherry else palette.card,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        textStyle = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily(StickerTypography.typeface(choice))),
                    )
                }
                repeat(3 - choices.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Size · $size", Modifier.weight(1f), color = palette.ink)
            Text("Bold", Modifier.padding(end = 12.dp), color = palette.ink)
            NeoSwitch(bold, onBold, contentDescription = "Bold text")
        }
        NeoSlider(size.toFloat().coerceIn(sizeRange), { onSize(it.roundToInt()) }, sizeRange, "Text size")
        ColorChoices("Text color", textColor, onTextColor)
        Spacer(Modifier.height(16.dp))
        ColorChoices("Background", background, onBackground, transparent = true)
    }
}

@Composable
internal fun ColorChoices(label: String, selected: Int, onSelect: (Int) -> Unit, transparent: Boolean = false) {
    val palette = Neo.palette
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = palette.muted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        textColors.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (name, color) ->
                    NeoSwatch(Color(color), name, selected == color, { onSelect(color) }, Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (transparent) {
            NeoOutlinedButton(text = if (selected == 0) "No background · selected" else "No background", onClick = { onSelect(0) })
        }
    }
}
