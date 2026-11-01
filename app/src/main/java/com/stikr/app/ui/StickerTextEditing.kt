package com.stikr.app.ui

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.stikr.app.render.StickerRenderer.Style
import com.stikr.app.ui.neo.*
import kotlin.math.*

internal fun Style.textLayer(index: Int): Style = if (index == 0) copy(extraCaptions = emptyList()) else extraCaptions.getOrNull(index - 1) ?: copy(extraCaptions = emptyList())
internal fun Style.withTextLayer(index: Int, layer: Style): Style = if (index == 0) layer.copy(extraCaptions = extraCaptions)
    else copy(extraCaptions = extraCaptions.toMutableList().apply { if (index - 1 in indices) this[index - 1] = layer.copy(extraCaptions = emptyList()) })

/** Centroid-relative transforms keep the text under the two fingers while rotating/scaling. */
@Composable
internal fun Modifier.stickerTextGestures(style: Style, selected: Int, enabled: Boolean, onStyle: (Style) -> Unit): Modifier {
    val current by rememberUpdatedState(style)
    val change by rememberUpdatedState(onStyle)
    return if (!enabled) this else pointerInput(selected) {
        detectTransformGestures { centroid, pan, zoom, rotation ->
            val parent = current
            val layer = parent.textLayer(selected)
            if (layer.caption.isNotBlank()) {
                val oldSize = layer.captionSize.coerceAtLeast(8)
                val newSize = (oldSize * zoom).roundToInt().coerceIn(8, 320)
                val scale = newSize.toFloat() / oldSize
                val oldX = (layer.captionX ?: .5f) * size.width
                val oldY = (layer.captionY ?: if (layer.captionPosition == com.stikr.app.render.StickerRenderer.CaptionPosition.TOP) .12f else .88f) * size.height
                val v = Offset(oldX - centroid.x, oldY - centroid.y) * scale
                val radians = rotation * PI.toFloat() / 180f
                val point = Offset(v.x*cos(radians)-v.y*sin(radians), v.x*sin(radians)+v.y*cos(radians)) + centroid + pan
                change(parent.withTextLayer(selected, layer.copy(
                    captionX = (point.x/size.width).coerceIn(0f,1f), captionY = (point.y/size.height).coerceIn(0f,1f),
                    captionSize = newSize, captionRotation = layer.captionRotation + rotation,
                )))
            }
        }
    }
}

@Composable
internal fun StickerTextPanel(style: Style, enabled: Boolean, selected: Int, onSelected: (Int) -> Unit, onStyle: (Style) -> Unit) {
    val layer = style.textLayer(selected)
    val change: (Style) -> Unit = { onStyle(style.withTextLayer(selected, it)) }
    val palette = Neo.palette
    NeoCard(Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
        Text("Your text", style = MaterialTheme.typography.titleLarge, color = palette.ink)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeoOutlinedButton("Add text", {
                if (style.caption.isBlank() && style.extraCaptions.isEmpty()) {
                    onStyle(style.copy(caption="Your text",captionX=.5f,captionY=.5f,captionFont="condensed",captionSize=60));onSelected(0)
                } else if (style.extraCaptions.size < 19) {
                    onStyle(style.copy(extraCaptions=style.extraCaptions + Style(caption="Your text",captionX=.5f,captionY=.5f,captionFont="condensed",captionSize=60)))
                    onSelected(style.extraCaptions.size+1)
                }
            }, enabled=enabled && style.extraCaptions.size<19, modifier=Modifier.weight(1f))
            NeoOutlinedButton("Duplicate", {
                onStyle(style.copy(extraCaptions=style.extraCaptions + layer.copy(captionY=((layer.captionY?:.5f)+.05f).coerceAtMost(1f))))
                onSelected(style.extraCaptions.size+1)
            }, enabled=enabled && layer.caption.isNotBlank() && style.extraCaptions.size<19, modifier=Modifier.weight(1f))
        }
        if (style.extraCaptions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            (listOf(style) + style.extraCaptions).indices.chunked(4).forEach { row ->
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { row.forEach { i ->
                    NeoButton("Text ${i+1}", { onSelected(i) }, modifier=Modifier.weight(1f), fill=if(i==selected) palette.cherry else palette.card)
                } }
            }
        }
        Spacer(Modifier.height(14.dp))
        NeoTextField(layer.caption, { value -> change(layer.copy(caption=value.take(500),captionX=layer.captionX?:.5f,captionY=layer.captionY?:.78f)) }, label="Your text", placeholder="Say something", enabled=enabled)
        Spacer(Modifier.height(22.dp))
        Text("Text editing", style=MaterialTheme.typography.titleMedium,color=palette.ink)
        Spacer(Modifier.height(14.dp))
        TextStyleControls(layer.captionFont,layer.captionSize,layer.captionBold,layer.captionBackground,layer.captionTextColor?:layer.captionColor.fill,
            { change(layer.copy(captionFont=it)) },{ change(layer.copy(captionSize=it,captionX=layer.captionX?:.5f,captionY=layer.captionY?:.78f)) },
            { change(layer.copy(captionBold=it)) },{ change(layer.copy(captionBackground=it)) },{ change(layer.copy(captionTextColor=it)) },8f..320f)
        Spacer(Modifier.height(16.dp))
        listOf("Italic" to layer.captionItalic,"Underline" to layer.captionUnderline,"Strikethrough" to layer.captionStrike).forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Text(item.first,Modifier.weight(1f),color=palette.ink)
                NeoSwitch(item.second,{ checked -> change(when(index){0->layer.copy(captionItalic=checked);1->layer.copy(captionUnderline=checked);else->layer.copy(captionStrike=checked)}) },contentDescription=item.first+" text")
            }
        }
        CustomTextColor("Custom text colour",layer.captionTextColor?:layer.captionColor.fill) { change(layer.copy(captionTextColor=it)) }
        CustomTextColor("Custom background colour",layer.captionBackground) { change(layer.copy(captionBackground=it)) }
        Spacer(Modifier.height(16.dp))
        Text("Alignment",color=palette.muted)
        NeoSegmented(listOf("Left","Centre","Right"),listOf("left","center","right").indexOf(layer.captionAlign).coerceAtLeast(0),{ change(layer.copy(captionAlign=listOf("left","center","right")[it])) })
        Spacer(Modifier.height(16.dp))
        val angle = ((layer.captionRotation + 180f) % 360f + 360f) % 360f - 180f
        Text("Rotation · ${angle.roundToInt()}°",color=palette.ink)
        NeoSlider(angle,{change(layer.copy(captionRotation=it))},-180f..180f,"Text rotation")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            NeoOutlinedButton("Re-centre",{change(layer.copy(captionX=.5f,captionY=.5f))},modifier=Modifier.weight(1f))
            NeoOutlinedButton("Reset rotation",{change(layer.copy(captionRotation=0f))},modifier=Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        NeoOutlinedButton("Delete text",{
            if(selected==0) { onStyle(style.copy(caption="")); onSelected(0) }
            else {onStyle(style.copy(extraCaptions=style.extraCaptions.filterIndexed { i,_->i!=selected-1 }));onSelected(0)}
        },enabled=enabled)
        Spacer(Modifier.height(12.dp))
        Text("Drag on the canvas to move the selected text. Pinch to resize; twist with two fingers to rotate freely.",style=MaterialTheme.typography.bodySmall,color=palette.muted)
    }
}

@Composable
private fun CustomTextColor(label:String, color:Int, onColor:(Int)->Unit) {
    var value by remember(color) { mutableStateOf("#"+String.format("%06X", color and 0xFFFFFF)) }
    NeoTextField(value,{ text -> value=text.take(7); if(text.matches(Regex("#[0-9a-fA-F]{6}"))) onColor(android.graphics.Color.parseColor(text)) },label=label,placeholder="#RRGGBB")
}
