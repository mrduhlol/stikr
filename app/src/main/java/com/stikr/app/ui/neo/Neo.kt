package com.stikr.app.ui.neo

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stikr.app.R
import com.stikr.app.ui.theme.Anton
import com.stikr.app.ui.theme.LocalNeoPalette
import com.stikr.app.ui.theme.NeoDimens
import com.stikr.app.ui.theme.NeoMotion
import com.stikr.app.ui.theme.NeoPalette
import com.stikr.app.ui.theme.NeoShapes
import kotlin.math.roundToInt

/** Access to the theme's palette from any composable. */
object Neo {
    val palette: NeoPalette
        @Composable get() = LocalNeoPalette.current
}

private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)

/** A framed surface: ink border + hard shadow. Clickable cards press in. */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    fill: Color = Neo.palette.card,
    shape: Shape = NeoShapes.card,
    shadow: Dp = NeoDimens.shadow,
    borderWidth: Dp = NeoDimens.border,
    padding: PaddingValues = PaddingValues(0.dp),
    clipContent: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = Neo.palette
    var press: State<Float>? = null
    var click: Modifier = Modifier
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        press = rememberPressProgress(interaction)
        click = Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClickLabel = onClickLabel,
            role = Role.Button,
            onClick = onClick,
        )
    }
    Column(
        modifier = modifier
            .neoFrame(
                fill = fill,
                borderColor = palette.edgeFor(fill),
                shadowColor = palette.shadow,
                shape = shape,
                borderWidth = borderWidth,
                shadowOffset = shadow,
                press = press,
                clipContent = clipContent,
            )
            .then(click)
            .padding(padding),
        content = content,
    )
}

/**
 * The action button: flat fill, thick border, and it presses *into* its
 * shadow. Primary actions default to cherry. Disabled buttons lose their
 * shadow and go flat grey, so "can't press" reads before reading the label.
 */
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = Neo.palette.cherry,
    contentColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    shadow: Dp = NeoDimens.shadow,
    shape: Shape = NeoShapes.control,
    contentPadding: PaddingValues = ButtonPadding,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    haptic: NeoHaptic = NeoHaptic.Tap,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
) {
    val palette = Neo.palette
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressProgress(interaction, enabled)
    val focused by interaction.collectIsFocusedAsState()
    val face = if (enabled) fill else palette.creamDim
    val label = when {
        !enabled -> palette.muted
        contentColor != Color.Unspecified -> contentColor
        else -> palette.contentFor(fill)
    }

    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .neoFrame(
                fill = face,
                borderColor = when {
                    focused -> palette.purple
                    enabled -> palette.edgeFor(fill)
                    else -> palette.ink.copy(alpha = 0.35f)
                },
                shadowColor = palette.shadow,
                shape = shape,
                shadowOffset = if (enabled) shadow else 0.dp,
                press = press,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics(haptic)
                    onClick()
                },
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = label, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text.uppercase(),
            style = textStyle,
            color = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(10.dp))
            Icon(trailingIcon, contentDescription = null, tint = label, modifier = Modifier.size(20.dp))
        }
    }
}

/** Secondary action: the same button on a card fill. */
@Composable
fun NeoOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fill: Color = Neo.palette.card,
    shadow: Dp = NeoDimens.shadow,
    contentPadding: PaddingValues = ButtonPadding,
    icon: ImageVector? = null,
    haptic: NeoHaptic = NeoHaptic.Tap,
) {
    NeoButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        fill = fill,
        enabled = enabled,
        shadow = shadow,
        contentPadding = contentPadding,
        icon = icon,
        haptic = haptic,
    )
}

/** A quiet text action: underlined mono, still a full 48dp target. */
@Composable
fun NeoTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Neo.palette.ink,
) {
    val haptics = rememberHaptics()
    Box(
        modifier = modifier
            .heightIn(min = NeoDimens.touch)
            .clickable(role = Role.Button) {
                haptics(NeoHaptic.Tap)
                onClick()
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(textDecoration = TextDecoration.Underline),
            color = color,
        )
    }
}

/** A square framed icon button — back, rename, delete. Always 48dp. */
@Composable
fun NeoIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    fill: Color = Neo.palette.card,
    tint: Color = Color.Unspecified,
    size: Dp = NeoDimens.touch,
    iconSize: Dp = 22.dp,
    shape: Shape = NeoShapes.control,
    shadow: Dp = NeoDimens.shadowSmall,
    enabled: Boolean = true,
    haptic: NeoHaptic = NeoHaptic.Tap,
) {
    val palette = Neo.palette
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressProgress(interaction, enabled)
    val iconTint = if (tint != Color.Unspecified) tint else palette.contentFor(fill)

    Box(
        modifier = modifier
            .size(size)
            .neoFrame(
                fill = fill,
                borderColor = palette.edgeFor(fill),
                shadowColor = palette.shadow,
                shape = shape,
                shadowOffset = shadow,
                press = press,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics(haptic)
                    onClick()
                },
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
    }
}

/**
 * A rigid segmented control: equal cells sharing ink rules, the selected one
 * inverted to solid ink.
 */
@Composable
fun NeoSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Neo.palette
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .neoFrame(
                fill = palette.card,
                borderColor = palette.ink,
                shadowColor = palette.shadow,
                shape = NeoShapes.control,
                shadowOffset = NeoDimens.shadowSmall,
                clipContent = true,
            )
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, option ->
            if (index > 0) {
                Box(
                    Modifier
                        .width(NeoDimens.border)
                        .fillMaxHeight()
                        .background(palette.ink),
                )
            }
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = NeoDimens.touch)
                    .then(if (selected) Modifier.background(palette.ink) else Modifier)
                    .selectable(selected = selected, role = Role.RadioButton) {
                        if (!selected) haptics(NeoHaptic.Tap)
                        onSelect(index)
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) palette.paper else palette.ink,
                )
            }
        }
    }
}

/**
 * A square-thumbed toggle with its state spelled out ("ON"/"OFF"), so it
 * never relies on colour alone.
 */
@Composable
fun NeoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val palette = Neo.palette
    val haptics = rememberHaptics()
    val position by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(NeoMotion.release),
        label = "neo-switch",
    )
    val trackWidth = 72.dp
    val thumb = 26.dp
    val inset = 5.dp
    val travel = trackWidth - thumb - inset * 2
    val track = if (checked) palette.cherry else palette.creamDim

    Box(
        modifier = modifier
            .heightIn(min = NeoDimens.touch)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = {
                    haptics(NeoHaptic.Tap)
                    onCheckedChange(it)
                },
            )
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .alpha(if (enabled) 1f else 0.45f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = trackWidth, height = thumb + inset * 2)
                .neoFrame(
                    fill = track,
                    borderColor = palette.ink,
                    shadowColor = palette.shadow,
                    shape = NeoShapes.control,
                    shadowOffset = NeoDimens.shadowSmall,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = if (checked) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall,
                color = if (checked) palette.onCherry else palette.ink,
                modifier = Modifier
                    .align(if (checked) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 9.dp),
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(((inset + travel * position).toPx()).roundToInt(), 0) }
                    .size(thumb)
                    .neoFrame(
                        fill = palette.card,
                        borderColor = palette.ink,
                        shadowColor = palette.shadow,
                        shape = NeoShapes.tag,
                        shadowOffset = 0.dp,
                    ),
            )
        }
    }
}

/** A colour swatch: the selected one sits sunk into its shadow, checked. */
@Composable
fun NeoSwatch(
    fill: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Neo.palette
    val haptics = rememberHaptics()
    val sink = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(NeoMotion.release),
        label = "neo-swatch",
    )
    Column(
        modifier = modifier
            .widthIn(min = NeoDimens.touch)
            .selectable(selected = selected, role = Role.RadioButton) {
                if (!selected) haptics(NeoHaptic.Tap)
                onClick()
            }
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(40.dp)
                .neoFrame(
                    fill = fill,
                    borderColor = palette.ink,
                    shadowColor = palette.shadow,
                    shape = NeoShapes.control,
                    shadowOffset = NeoDimens.shadowSmall,
                    press = sink,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = if (fill.luminance() > 0.4f) palette.onAccent else Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) palette.ink else palette.muted,
        )
    }
}

/** The outline-thickness slider: a ruled ink track and a cherry block thumb. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription }
            .alpha(if (enabled) 1f else 0.45f),
        thumb = { NeoSliderThumb(interaction) },
        track = { state -> NeoSliderTrack(state) },
    )
}

@Composable
private fun NeoSliderThumb(interaction: MutableInteractionSource) {
    val palette = Neo.palette
    val press = rememberPressProgress(interaction)
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 36.dp)
            .neoFrame(
                fill = palette.cherry,
                borderColor = palette.ink,
                shadowColor = palette.shadow,
                shape = NeoShapes.tag,
                shadowOffset = NeoDimens.shadowSmall,
                press = press,
            ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NeoSliderTrack(state: SliderState) {
    val palette = Neo.palette
    val ink = palette.ink
    val tick = palette.ink.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .neoFrame(
                fill = palette.card,
                borderColor = palette.ink,
                shadowColor = palette.shadow,
                shape = NeoShapes.tag,
                shadowOffset = 0.dp,
                clipContent = true,
            )
            .drawBehind {
                val range = state.valueRange.endInclusive - state.valueRange.start
                val fraction = if (range > 0f) ((state.value - state.valueRange.start) / range).coerceIn(0f, 1f) else 0f
                val rule = 2.dp.toPx()
                for (i in 1..7) {
                    val x = size.width * i / 8f
                    drawRect(tick, Offset(x - rule / 2f, size.height * 0.3f), Size(rule, size.height * 0.4f))
                }
                drawRect(ink, size = Size(size.width * fraction, size.height))
            },
    )
}

/** A tiny, slightly-rotated label tag ("new", "whatsapp"). */
@Composable
fun NeoTag(
    text: String,
    modifier: Modifier = Modifier,
    fill: Color = Neo.palette.butter,
    rotation: Float = -4f,
) {
    val palette = Neo.palette
    Box(
        modifier = modifier
            .rotate(rotation)
            .neoFrame(
                fill = fill,
                borderColor = palette.edgeFor(fill),
                shadowColor = palette.shadow,
                shape = NeoShapes.tag,
                borderWidth = NeoDimens.hairline,
                shadowOffset = 2.dp,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = palette.contentFor(fill))
    }
}

/** A boxed mono counter: "05", "2/3". */
@Composable
fun NeoCount(text: String, modifier: Modifier = Modifier, fill: Color = Neo.palette.card) {
    val palette = Neo.palette
    Box(
        modifier = modifier
            .neoFrame(
                fill = fill,
                borderColor = palette.edgeFor(fill),
                shadowColor = palette.shadow,
                shape = NeoShapes.tag,
                borderWidth = NeoDimens.hairline,
                shadowOffset = 0.dp,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = palette.contentFor(fill))
    }
}

/** Section title: big Anton, a boxed count, and a thick ink rule under it. */
@Composable
fun NeoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = Neo.palette
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (meta != null) {
                NeoCount(meta, modifier = Modifier.padding(end = if (trailing != null) 10.dp else 0.dp))
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(NeoDimens.border)
                .background(palette.ink),
        )
    }
}

/** A mono step label: "01 — CUT-OUT". */
@Composable
fun NeoStepLabel(number: Int, title: String, modifier: Modifier = Modifier) {
    val palette = Neo.palette
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .background(palette.ink, NeoShapes.tag)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = number.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelMedium,
                color = palette.paper,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = palette.ink,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/**
 * The slogan, set brutalist: ink Anton words, with [accentWord] knocked out
 * of a tilted cherry slab that has its own border and hard shadow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StickerHeadline(
    text: String,
    modifier: Modifier = Modifier,
    accentWord: String? = null,
    fontSize: TextUnit = 56.sp,
    wordGap: Dp = 12.dp,
    lineGap: Dp = 6.dp,
) {
    val palette = Neo.palette
    val style = remember(fontSize) {
        TextStyle(fontFamily = Anton, fontSize = fontSize, lineHeight = fontSize * 1.02f, letterSpacing = 0.sp)
    }
    val words = remember(text) { text.split(" ").filter { it.isNotBlank() } }

    FlowRow(
        modifier = modifier.semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.spacedBy(wordGap),
        verticalArrangement = Arrangement.spacedBy(lineGap),
    ) {
        words.forEach { word ->
            val accent = accentWord != null &&
                word.trim(',', '.', '!', '?').equals(accentWord, ignoreCase = true)
            if (accent) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, end = NeoDimens.shadow, bottom = NeoDimens.shadow)
                        .rotate(-2.5f)
                        .neoFrame(
                            fill = palette.cherry,
                            borderColor = palette.ink,
                            shadowColor = palette.shadow,
                            shape = NeoShapes.tag,
                            shadowOffset = NeoDimens.shadow,
                        )
                        .padding(horizontal = 10.dp),
                ) {
                    Text(word.uppercase(), style = style, color = palette.onCherry)
                }
            } else {
                Text(word.uppercase(), style = style, color = palette.ink)
            }
        }
    }
}

/** The wordmark: the purple app mark as a tilted tile, then "stikr" in Anton. */
@Composable
fun NeoWordmark(modifier: Modifier = Modifier, trailing: (@Composable RowScope.() -> Unit)? = null) {
    val palette = Neo.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .rotate(-6f)
                .size(42.dp)
                .neoFrame(
                    fill = palette.purple,
                    borderColor = palette.ink,
                    shadowColor = palette.shadow,
                    shape = NeoShapes.control,
                    shadowOffset = NeoDimens.shadowSmall,
                    clipContent = true,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // The launcher foreground carries adaptive-icon padding; scale past it.
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.requiredSize(66.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = "stikr",
            style = TextStyle(fontFamily = Anton, fontSize = 34.sp, lineHeight = 36.sp),
            color = palette.ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

/** How loud a dialog is: neutral (butter band) or destructive (cherry band). */
enum class NeoTone { Neutral, Danger }

/**
 * A dialog dressed like everything else: framed card with a deep shadow, a
 * mono kicker band across the top, Anton title, actions in a rigid row.
 */
@Composable
fun NeoDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    tone: NeoTone = NeoTone.Neutral,
    body: (@Composable ColumnScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    val palette = Neo.palette
    val band = if (tone == NeoTone.Danger) palette.cherry else palette.butter
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp + NeoDimens.shadowLarge, top = 16.dp, bottom = 16.dp + NeoDimens.shadowLarge),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .neoFrame(
                        fill = palette.card,
                        borderColor = palette.ink,
                        shadowColor = palette.shadow,
                        shape = NeoShapes.card,
                        shadowOffset = NeoDimens.shadowLarge,
                        clipContent = true,
                    ),
            ) {
                if (kicker != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(band)
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                    ) {
                        Text(
                            text = kicker.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.contentFor(band),
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(NeoDimens.border)
                            .background(palette.ink),
                    )
                }
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp)) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (body != null) {
                        Spacer(Modifier.height(12.dp))
                        body()
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NeoDimens.gapSmall),
                        content = actions,
                    )
                }
            }
        }
    }
}

/**
 * Text input as a framed slab with its label above in mono. Focus lifts it
 * onto a purple shadow.
 */
@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    focusRequester: FocusRequester? = null,
) {
    val palette = Neo.palette
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val cursor = remember(palette.cherryInk) { SolidColor(palette.cherryInk) }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (enabled) palette.ink else palette.muted)

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = palette.muted,
            )
            Spacer(Modifier.height(8.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = textStyle,
            cursorBrush = cursor,
            interactionSource = interaction,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = imeAction,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .neoFrame(
                            fill = if (enabled) palette.card else palette.creamDim,
                            borderColor = if (enabled) palette.ink else palette.ink.copy(alpha = 0.35f),
                            shadowColor = palette.purple,
                            shape = NeoShapes.control,
                            shadowOffset = if (focused) NeoDimens.shadowSmall else 0.dp,
                        )
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = textStyle, color = palette.muted)
                    }
                    inner()
                }
            },
        )
    }
}

/** Snackbar as a solid ink slab on a cherry shadow. */
@Composable
fun NeoSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    val palette = Neo.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = NeoDimens.shadow, bottom = NeoDimens.shadow)
            .neoFrame(
                fill = palette.ink,
                borderColor = palette.ink,
                shadowColor = palette.cherry,
                shape = NeoShapes.control,
                shadowOffset = NeoDimens.shadow,
            )
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(palette.butter),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = data.visuals.message,
            style = MaterialTheme.typography.labelLarge,
            color = palette.paper,
        )
    }
}

/**
 * A big centred empty state: a framed block, one headline, one line of copy,
 * one action. [art] sits above the headline.
 */
@Composable
fun NeoEmptyState(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = Neo.palette.card,
    art: (@Composable BoxScope.() -> Unit)? = null,
) {
    val palette = Neo.palette
    NeoCard(
        modifier = modifier.fillMaxWidth(),
        fill = fill,
        padding = PaddingValues(22.dp),
    ) {
        if (art != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center, content = art)
            Spacer(Modifier.height(18.dp))
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            color = palette.contentFor(fill),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = if (fill == palette.card) palette.muted else palette.contentFor(fill),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        NeoButton(
            text = action,
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(),
            haptic = NeoHaptic.Confirm,
        )
    }
}
