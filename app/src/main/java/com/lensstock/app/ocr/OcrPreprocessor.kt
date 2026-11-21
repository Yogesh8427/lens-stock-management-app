package com.lensstock.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import java.io.File

/**
 * Some packet photos print the power in thin, faint gray dot-matrix text over a busy colored
 * background — ML Kit's recognizer then detects no digits at all (not even garbled ones).
 * Converting to grayscale and stretching the contrast around the midpoint makes that faint
 * print stand out before OCR runs, without needing a sharper or closer photo.
 */
object OcrPreprocessor {
    fun prepare(context: Context, uri: Uri): InputImage {
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).rotationDegrees()
        } ?: 0
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: error("Couldn't decode image")
        return InputImage.fromBitmap(enhance(upscale(rotate(bitmap, orientation))), 0)
    }

    fun prepare(file: File): InputImage {
        val orientation = ExifInterface(file.absolutePath).rotationDegrees()
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: error("Couldn't decode image")
        return InputImage.fromBitmap(enhance(upscale(rotate(bitmap, orientation))), 0)
    }

    /** Low-res packet photos (e.g. WhatsApp-compressed, ~960x1280) leave dot-matrix print only a
     * few pixels tall — too small for reliable OCR. Upscaling first gives the recognizer more
     * pixels to work with before the contrast stretch sharpens the enhanced edges. */
    private fun upscale(bitmap: Bitmap, targetLongSide: Int = 2400): Bitmap {
        val longSide = maxOf(bitmap.width, bitmap.height)
        if (longSide >= targetLongSide) return bitmap
        val scale = targetLongSide.toFloat() / longSide
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun ExifInterface.rotationDegrees(): Int =
        when (getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Grayscale + a contrast stretch anchored at mid-gray, done via Skia's ColorMatrix (not a manual pixel loop). */
    private fun enhance(bitmap: Bitmap, contrast: Float = 2.4f): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val translate = (1 - contrast) * 128f
        val grayscale = ColorMatrix().apply { setSaturation(0f) }
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        grayscale.postConcat(contrastMatrix)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(grayscale) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }
}
