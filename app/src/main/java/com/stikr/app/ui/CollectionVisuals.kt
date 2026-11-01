package com.stikr.app.ui

import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.stikr.app.data.StickerPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/** The collection's ambient surface also works behind dialogs and the pack screen. */
fun Modifier.collectionBackground(): Modifier = background(
    Brush.linearGradient(listOf(Color(0xFF141715), Color(0xFF0C0E0D), Color(0xFF12140F))),
).drawWithCache {
    val forest = Brush.radialGradient(listOf(Color(0x33374032), Color.Transparent), Offset(size.width * .5f, 0f), size.height * .72f)
    onDrawBehind { drawRect(forest) }
}

private fun coverFile(pack: StickerPack, root: File): File? =
    (pack.thumbnailImage ?: pack.stickers.firstOrNull())?.let { if (File(it).isAbsolute) File(it) else root.resolve(pack.id).resolve(it) }

/** Shared by home, pack details and the confirmed-success turntable. */
@Composable
fun PackCollectionCard(pack: StickerPack, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val root = File(LocalContext.current.filesDir, "packs")
    val file = coverFile(pack, root)
    val bitmap by produceState<android.graphics.Bitmap?>(null, file, pack.contentVersion) {
        value = withContext(Dispatchers.IO) { file?.let { runCatching { BitmapFactory.decodeFile(it.path, BitmapFactory.Options().apply { inSampleSize = 2 }) }.getOrNull() } }
    }
    val tint = remember(bitmap) {
        bitmap?.let { image ->
            var r = 0L; var g = 0L; var b = 0L; var n = 0
            for (y in 0 until image.height step 8) for (x in 0 until image.width step 8) {
                val pixel = image.getPixel(x,y)
                if (android.graphics.Color.alpha(pixel) > 128) { r += android.graphics.Color.red(pixel); g += android.graphics.Color.green(pixel); b += android.graphics.Color.blue(pixel); n++ }
            }
            if (n > 0) lerp(Color(android.graphics.Color.rgb((r/n).toInt(),(g/n).toInt(),(b/n).toInt())), Color(0xFFF4E4D2), .45f) else Color(0xFFE5D6C8)
        } ?: Color(0xFFE5D6C8)
    }
    val ink = Color(0xFF211B21)
    val date = remember(pack.createdAt) { SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(Date(pack.createdAt)).uppercase(Locale.ENGLISH) }
    val unique = pack.displayId.ifBlank { pack.id.filter { it.isLetterOrDigit() }.takeLast(5).uppercase(Locale.ENGLISH) }
    Box(modifier.fillMaxWidth().background(tint).then(if (onClick != null) Modifier.clickable(onClickLabel = "Open ${pack.name}", onClick = onClick) else Modifier)) {
        Column(Modifier.padding(8.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(4f/5f).background(tint.copy(alpha=.6f)).semantics { contentDescription = "${pack.name} thumbnail" }) {
                bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Row(Modifier.fillMaxWidth().padding(top=10.dp), horizontalArrangement=Arrangement.SpaceBetween) {
                Text("CHECKPOINT $date · #$unique", fontSize=8.sp, lineHeight=11.sp, color=ink, modifier=Modifier.weight(1f))
                Text("${pack.stickerCount}/30", fontSize=10.sp, color=ink, modifier=Modifier.padding(start=3.dp))
            }
            Text(pack.name, fontSize=22.sp, lineHeight=23.sp, letterSpacing=(-.7).sp, fontWeight=FontWeight.SemiBold, color=ink, maxLines=2, overflow=TextOverflow.Ellipsis, modifier=Modifier.heightIn(min=50.dp).padding(top=6.dp))
            Row(Modifier.fillMaxWidth().padding(top=7.dp,bottom=3.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                Text("STIKR / ${pack.serialNumber.coerceAtLeast(1).toString().padStart(3,'0')}", fontSize=8.sp, letterSpacing=1.sp, color=ink)
                Box(Modifier.size(27.dp).border(1.dp,ink), contentAlignment=Alignment.Center) { WhatsAppMark(Modifier.size(18.dp),ink) }
            }
        }
        Column(Modifier.width(94.dp).background(tint, object: Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = Outline.Generic(Path().apply { moveTo(0f,0f); lineTo(size.width,0f); lineTo(size.width,size.height*.76f); lineTo(size.width*.88f,size.height); lineTo(0f,size.height); close() })
        }).padding(start=8.dp,top=10.dp,end=12.dp,bottom=7.dp)) {
            Canvas(Modifier.width(62.dp).height(6.dp)) {
                val progress = (pack.stickerCount / 30f).coerceIn(0f,1f)
                for (x in 0..20) { val px=x*4.dp.toPx(); if(px+3.dp.toPx() <= size.width) drawLine(ink.copy(alpha=if(px < size.width*progress) 1f else .3f),Offset(px,size.height),Offset(px+3.dp.toPx(),0f),2.dp.toPx()) }
            }
            Text("LVL ${pack.serialNumber.coerceAtLeast(1).toString().padStart(3,'0')}",color=ink,fontSize=9.sp,lineHeight=13.sp,fontWeight=FontWeight.Medium)
        }
    }
}

@Composable
fun WhatsAppMark(modifier: Modifier = Modifier, color: Color = Color(0xFFD5F277)) {
    val path = remember { PathParser().parsePathString("M20.5 3.5A11.9 11.9 0 0 0 1.7 17.8L0 24l6.3-1.7A12 12 0 0 0 24 11.9a11.8 11.8 0 0 0-3.5-8.4ZM12 21.9a10 10 0 0 1-5.1-1.4l-.4-.2-3.7 1 1-3.6-.3-.4A10 10 0 1 1 12 21.9Zm5.5-7.5c-.3-.1-1.8-.9-2-.9-.3-.1-.5-.1-.7.2l-.9 1.1c-.2.2-.3.2-.6.1a8.2 8.2 0 0 1-4.1-3.6c-.3-.5.3-.5.8-1.4.1-.2.1-.4 0-.6L9.1 7c-.2-.5-.5-.4-.7-.4h-.6c-.2 0-.6.1-.8.4a3.3 3.3 0 0 0-1 2.4c0 1.4 1 2.8 1.2 3 .1.2 2.1 3.3 5.1 4.6 1.9.8 2.7.9 3.6.8.6-.1 1.8-.7 2-1.4.3-.7.3-1.3.2-1.4-.1-.2-.3-.3-.6-.4Z").toPath() }
    Canvas(modifier) { withTransform({ scale(size.width/24f,size.height/24f,Offset.Zero) }) { drawPath(path,color) } }
}

internal fun slideConfirmed(position: Float, travel: Float): Boolean = travel > 0f && position / travel >= .95f

/** A drag-only primary action; TalkBack exposes an explicit confirmation action. */
@Composable
fun SlideExport(enabled: Boolean = true, busy: Boolean = false, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    var width by remember { mutableFloatStateOf(0f) }
    var position by remember { mutableFloatStateOf(0f) }
    val knob = with(LocalDensity.current) { 52.dp.toPx() }
    val inset = with(LocalDensity.current) { 5.dp.toPx() }
    val travel = (width-knob-inset*2).coerceAtLeast(0f)
    val active = enabled && !busy
    val latestConfirm by rememberUpdatedState(onConfirm)
    LaunchedEffect(active) { if (!active) position=0f }
    Box(modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xFF232B20)).onSizeChanged { width=it.width.toFloat() }
        .semantics(mergeDescendants=true) {
            contentDescription = if(busy) "Preparing pack for WhatsApp" else "Slide to add to WhatsApp"
            if(!active) disabled()
            customActions = if(active) listOf(CustomAccessibilityAction("Confirm add to WhatsApp") { latestConfirm(); true }) else emptyList()
        }.pointerInput(active,travel) {
            if(active) detectHorizontalDragGestures(
                onDragStart={ position=0f; com.stikr.app.audio.AppSounds.slide() },
                onHorizontalDrag={ change, amount -> change.consume(); position=(position+amount).coerceIn(0f,travel) },
                onDragCancel={ position=0f },
                onDragEnd={ val confirmed=slideConfirmed(position,travel); position=0f; if(confirmed) latestConfirm() },
            )
        }, contentAlignment=Alignment.Center) {
        // Pure drawing beneath the content: never adds a second input or semantics layer.
        Canvas(Modifier.matchParentSize()) {
            if (active && position > 0f && travel > 0f) {
                val progress = (position / travel).coerceIn(0f, 1f)
                val front = (inset + position + knob * .55f).coerceAtMost(size.width)
                val amplitude = if (reducedMotion) 0f else 12.dp.toPx() * sin(progress * PI).toFloat()
                val liquid = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(front, 0f)
                    cubicTo(front - amplitude, size.height * .22f,
                        front + amplitude, size.height * .34f, front + amplitude * .4f, size.height * .5f)
                    cubicTo(front - amplitude, size.height * .67f,
                        front + amplitude, size.height * .85f, front, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(liquid, Brush.horizontalGradient(listOf(Color(0xFF3D4E2F), Color(0xFF75885A))))
                if (!reducedMotion) {
                    repeat(3) { index ->
                        val radius = (2f + index) * density * sin(progress * PI).toFloat()
                        drawCircle(Color(0xFFA9BD83).copy(alpha = .18f + progress * .16f),
                            radius, Offset(front + (12f + index * 9f) * density,
                                size.height * (.25f + index * .24f)))
                    }
                }
            }
        }
        Text(if(busy) "Preparing pack…" else "Slide to add to WhatsApp",fontSize=12.sp,color=Color(0xFFE1EBCD).copy(alpha=if(active) 1f else .5f),modifier=Modifier.padding(start=34.dp,end=28.dp))
        WhatsAppMark(Modifier.align(Alignment.CenterEnd).padding(end=17.dp).size(24.dp))
        Box(Modifier.align(Alignment.CenterStart).offset { IntOffset((inset+position).roundToInt(),0) }.size(52.dp).background(if(active) Color(0xFFD5F277) else Color(0xFF8A877A),RoundedCornerShape(26.dp)),contentAlignment=Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward,null,tint=Color(0xFF263113))
        }
    }
}

/** Call only after WhatsApp returns Activity.RESULT_OK. */
@Composable
fun PackAddedCelebration(pack: StickerPack, onFinished: () -> Unit) {
    val context=LocalContext.current
    val reduced=remember { Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f }
    val pop=remember { Animatable(if(reduced) 1f else .75f) }
    val turn=remember { Animatable(0f) }
    val burst=remember { Animatable(0f) }
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(pack.id) {
        if(!reduced) { pop.animateTo(1f,tween(180)); turn.animateTo(360f,tween(1350,easing=LinearEasing)); burst.animateTo(1f,tween(1100)) }
        delay(if(reduced) 1600 else 700); finish()
    }
    Box(Modifier.fillMaxSize().background(Color(0xED0C0E0D)),contentAlignment=Alignment.Center) {
        Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(32.dp).widthIn(max=330.dp)) {
            Text("Pack added",style=MaterialTheme.typography.headlineMedium,color=Color(0xFFF3F7E9),modifier=Modifier.padding(bottom=28.dp).semantics { liveRegion=LiveRegionMode.Polite })
            val back=turn.value in 90f..270f
            Box(Modifier.fillMaxWidth().graphicsLayer { scaleX=pop.value; scaleY=pop.value; rotationY=turn.value; cameraDistance=12*density }) {
                PackCollectionCard(pack, Modifier.graphicsLayer { alpha=if(back) 0f else 1f })
                if(back) Box(Modifier.matchParentSize().graphicsLayer { rotationY=180f }.background(Color(0xFFE5D6C8)),contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("stikr",fontSize=48.sp,fontWeight=FontWeight.Bold,color=Color(0xFF252019)); Text("Little things. Big personality.",fontSize=12.sp,color=Color(0xFF252019)) }
                }
            }
        }
        if(!reduced && burst.value>0f) Canvas(Modifier.fillMaxSize()) {
            val colors=listOf(Color(0xFFD5F277),Color(0xFFBDA1ED),Color(0xFFF4D8AA),Color(0xFFF29ABB))
            repeat(32) { i -> val angle=i*2*PI/32; val distance=(90+(i%4)*24).dp.toPx()*burst.value; drawRect(colors[i%4].copy(alpha=1-burst.value),Offset(center.x+cos(angle).toFloat()*distance,center.y+sin(angle).toFloat()*distance+burst.value*70.dp.toPx()),Size(5.dp.toPx(),10.dp.toPx())) }
        }
    }
}
