package it.elledue.control.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/** Owned image bytes in Room: backup/restore is atomic with the economic archive. */
object LocalImages {
    const val MAX_IMAGE_BYTES = 180 * 1024
    const val MAX_SIDE = 1200
    fun validate(value: String?) {
        if (value == null) return
        require(value.length <= MAX_IMAGE_BYTES * 4 / 3 + 8) { "Fotografia troppo grande." }
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        require(bytes.size in 1..MAX_IMAGE_BYTES)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth in 1..MAX_SIDE && bounds.outHeight in 1..MAX_SIDE && bounds.outMimeType == "image/jpeg") { "Fotografia non valida." }
        requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) { "Fotografia corrotta." }.recycle()
    }
    fun import(context: Context, uri: Uri): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Immagine non leggibile." }
        var sample = 1
        while (bounds.outWidth / sample > MAX_SIDE * 2 || bounds.outHeight / sample > MAX_SIDE * 2) sample *= 2
        val source = context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: error("Immagine non leggibile.")
        val orientation = runCatching { context.contentResolver.openInputStream(uri)!!.use { android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) } }.getOrDefault(1)
        val matrix = android.graphics.Matrix().apply {
            when (orientation) {
                2 -> setScale(-1f, 1f); 3 -> setRotate(180f); 4 -> setScale(1f, -1f)
                5 -> { setRotate(90f); postScale(-1f, 1f) }; 6 -> setRotate(90f)
                7 -> { setRotate(90f); postScale(1f, -1f) }; 8 -> setRotate(270f)
            }
        }
        val oriented = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (oriented !== source) source.recycle()
        val ratio = minOf(1.0, MAX_SIDE.toDouble() / maxOf(oriented.width, oriented.height))
        val bitmap = Bitmap.createScaledBitmap(oriented, (oriented.width * ratio).toInt().coerceAtLeast(1), (oriented.height * ratio).toInt().coerceAtLeast(1), true)
        if (bitmap !== oriented) oriented.recycle()
        try {
            var quality = 88; var bytes: ByteArray
            do { bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray(); quality -= 8 } while (bytes.size > MAX_IMAGE_BYTES && quality >= 16)
            require(bytes.size <= MAX_IMAGE_BYTES) { "Immagine troppo complessa: seleziona una fotografia più piccola." }
            return Base64.encodeToString(bytes, Base64.NO_WRAP).also(::validate)
        } finally { bitmap.recycle() }
    }
}
