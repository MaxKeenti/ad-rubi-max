package com.example.bachewatch.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decode → scale to ≤1600 px long edge → JPEG q80 (~300–500 KB). Re-encoding
 * a Bitmap drops EXIF, so capture-time and orientation tags are copied from
 * the original onto the output: capture time is the forensic backstop
 * (CONTEXT.md), and orientation keeps the photo upright since BitmapFactory
 * does not auto-rotate.
 */
@Singleton
class FotoCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun comprimir(fotoUri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        val original = resolver.openInputStream(fotoUri).use { BitmapFactory.decodeStream(it) }
            ?: error("No se pudo leer la foto")

        val tagsPreservados = resolver.openInputStream(fotoUri).use { input ->
            requireNotNull(input) { "No se pudo leer la foto" }
            val exif = ExifInterface(input)
            TAGS_A_PRESERVAR.associateWith { exif.getAttribute(it) }
        }

        val escalada = escalarLongEdge(original, MAX_LADO)

        // ExifInterface writes to a file path, not a stream — compress to a
        // cacheDir temp, stamp the tags back, read the bytes, then delete.
        val tmp = File.createTempFile("comp_", ".jpg", context.cacheDir)
        try {
            FileOutputStream(tmp).use { escalada.compress(Bitmap.CompressFormat.JPEG, CALIDAD, it) }
            ExifInterface(tmp.absolutePath).apply {
                tagsPreservados.forEach { (tag, valor) -> if (valor != null) setAttribute(tag, valor) }
                saveAttributes()
            }
            tmp.readBytes()
        } finally {
            tmp.delete()
        }
    }

    private fun escalarLongEdge(bitmap: Bitmap, maxLado: Int): Bitmap {
        val ladoMayor = maxOf(bitmap.width, bitmap.height)
        if (ladoMayor <= maxLado) return bitmap
        val factor = maxLado.toFloat() / ladoMayor
        val ancho = (bitmap.width * factor).toInt().coerceAtLeast(1)
        val alto = (bitmap.height * factor).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, ancho, alto, true)
    }

    private companion object {
        const val MAX_LADO = 1600
        const val CALIDAD = 80
        val TAGS_A_PRESERVAR = listOf(
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_OFFSET_TIME,
            ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
            ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
            ExifInterface.TAG_ORIENTATION,
        )
    }
}
