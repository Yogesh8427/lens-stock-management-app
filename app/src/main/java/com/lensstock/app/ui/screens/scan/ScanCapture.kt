package com.lensstock.app.ui.screens.scan

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.lensstock.app.ocr.TextReader
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Takes a photo to a cache file, runs it through ML Kit, and returns the raw recognized text. */
suspend fun ImageCapture.captureAndReadText(context: Context): String {
    val photoFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    suspendCancellableCoroutine<Unit> { continuation ->
        takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = continuation.resume(Unit)
                override fun onError(exception: ImageCaptureException) = continuation.resumeWithException(exception)
            },
        )
    }

    return try {
        val image = InputImage.fromFilePath(context, Uri.fromFile(photoFile))
        TextReader.read(image)
    } finally {
        photoFile.delete()
    }
}

/** Same OCR pipeline as [captureAndReadText], but for a photo already picked from the gallery. */
suspend fun readTextFromUri(context: Context, uri: Uri): String {
    val image = InputImage.fromFilePath(context, uri)
    return TextReader.read(image)
}
