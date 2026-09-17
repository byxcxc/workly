package com.workly.app.ui.background

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What the app needs to draw a user picture as its background:
 * the decoded image, plus the accent colour taken from it.
 */
data class BackgroundRender(
    val image: ImageBitmap? = null,
    /** ARGB of the picture's dominant colour, or `null` when there is no picture. */
    val accentArgb: Long? = null,
)

/**
 * Decodes the chosen picture once per URI, off the main thread, and derives the
 * accent colour from the same bitmap so the image is only read once.
 */
@Composable
fun rememberBackgroundRender(uri: String?, resolver: ContentResolver): BackgroundRender {
    val state = produceState(initialValue = BackgroundRender(), uri, resolver) {
        value = if (uri.isNullOrBlank()) {
            BackgroundRender()
        } else {
            withContext(Dispatchers.IO) { loadBackground(uri, resolver) }
        }
    }
    return state.value
}

private fun loadBackground(uri: String, resolver: ContentResolver): BackgroundRender {
    val bitmap = runCatching { decodeDownsampled(uri, resolver) }.getOrNull() ?: return BackgroundRender()
    val accent = runCatching { dominantColorArgb(bitmap) }.getOrNull()
    return BackgroundRender(image = bitmap.asImageBitmap(), accentArgb = accent)
}

/**
 * Decodes the picture at roughly [MAX_EDGE] pixels on its longest side.
 *
 * A phone photo can be 12 megapixels; decoding it at full size to blur it and
 * sit behind the UI would cost tens of megabytes for no visible gain.
 */
private fun decodeDownsampled(uri: String, resolver: ContentResolver): Bitmap? {
    val parsed = uri.toUri()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(resolver, parsed)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.isMutableRequired = false
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_EDGE) {
                decoder.setTargetSampleSize(1 + longest / MAX_EDGE)
            }
        }
    }

    @Suppress("DEPRECATION")
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    @Suppress("DEPRECATION")
    resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    while (longest / (sample * 2) >= MAX_EDGE) sample *= 2

    @Suppress("DEPRECATION")
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    @Suppress("DEPRECATION")
    return resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, options) }
}

/**
 * The picture's dominant colour, found with a coarse 4-bit-per-channel histogram.
 *
 * Doing this by hand keeps the app free of a palette dependency, and the result is
 * only used to tint the UI, so an approximate answer is exactly what is wanted.
 */
internal fun dominantColorArgb(bitmap: Bitmap): Long? {
    val sample = bitmap.scale(HISTOGRAM_SIZE, HISTOGRAM_SIZE)
    val pixels = IntArray(HISTOGRAM_SIZE * HISTOGRAM_SIZE)
    sample.getPixels(pixels, 0, HISTOGRAM_SIZE, 0, 0, HISTOGRAM_SIZE, HISTOGRAM_SIZE)
    if (sample !== bitmap) sample.recycle()

    val counts = IntArray(BUCKETS)
    val redSums = LongArray(BUCKETS)
    val greenSums = LongArray(BUCKETS)
    val blueSums = LongArray(BUCKETS)

    pixels.forEach { pixel ->
        val alpha = pixel ushr 24 and 0xFF
        if (alpha < 128) return@forEach
        val r = pixel ushr 16 and 0xFF
        val g = pixel ushr 8 and 0xFF
        val b = pixel and 0xFF
        // Ignore near-black and near-white: they are rarely the colour people
        // think of when they look at a photo.
        val luma = (r * 299 + g * 587 + b * 114) / 1000
        if (luma < 28 || luma > 232) return@forEach
        val bucket = (r shr 4 shl 8) or (g shr 4 shl 4) or (b shr 4)
        counts[bucket]++
        redSums[bucket] += r.toLong()
        greenSums[bucket] += g.toLong()
        blueSums[bucket] += b.toLong()
    }

    var bestBucket = -1
    var bestCount = 0
    counts.forEachIndexed { index, count ->
        if (count > bestCount) {
            bestCount = count
            bestBucket = index
        }
    }
    if (bestBucket < 0) return null

    val count = counts[bestBucket]
    val r = (redSums[bestBucket] / count).toInt()
    val g = (greenSums[bestBucket] / count).toInt()
    val b = (blueSums[bestBucket] / count).toInt()
    return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

/** Longest edge the background is decoded at. */
private const val MAX_EDGE = 1080
private const val HISTOGRAM_SIZE = 48
/** 4 bits per channel. */
private const val BUCKETS = 16 * 16 * 16
