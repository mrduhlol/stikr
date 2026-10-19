package com.stikr.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime

/** Loads a sticker file off disk at the displayed size, with no I/O in composition. */
@Composable
fun StickerImage(file: File?, contentDescription: String?, modifier: Modifier = Modifier) {
    var targetSize by remember(file) { mutableIntStateOf(0) }
    val bitmap by produceState<Bitmap?>(initialValue = null, file, targetSize) {
        value = null
        if (file != null && targetSize > 0) {
            value = withContext(Dispatchers.IO) { StickerThumbnails.load(file, targetSize) }
        }
    }
    Box(modifier.onSizeChanged { targetSize = maxOf(it.width, it.height) }) {
        bitmap?.let { loaded ->
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/** Shared across lists and navigation. Eviction leaves displayed bitmaps alive. */
internal object StickerThumbnails {
    private data class Key(
        val path: String,
        val modified: FileTime,
        val length: Long,
        val fileKey: Any?,
        val sample: Int,
    )

    private val cache = object : LruCache<Key, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: Key, value: Bitmap): Int = value.allocationByteCount
    }

    // Caller runs on Dispatchers.IO, including the metadata check on cache hits.
    fun load(file: File, targetSize: Int): Bitmap? = try {
        val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
        val sample = sampleSize(targetSize)
        val key = Key(file.absolutePath, attributes.lastModifiedTime(), attributes.size(), attributes.fileKey(), sample)
        cache.get(key) ?: BitmapFactory.decodeFile(
            file.path,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )?.let { decoded ->
            synchronized(cache) {
                val existing = cache.get(key)
                if (existing != null) {
                    decoded.recycle()
                    existing
                } else {
                    cache.put(key, decoded)
                    decoded
                }
            }
        }
    } catch (_: Exception) {
        null
    }

    // Stored stickers are 512px. Keep at least as many pixels as the display needs.
    internal fun sampleSize(targetSize: Int): Int {
        var sample = 1
        while (512 / (sample * 2) >= targetSize.coerceAtLeast(1)) sample *= 2
        return sample
    }
}
