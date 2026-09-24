package com.staticum.diariocalorico.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

object PhotoFiles {

    private fun photosDir(context: Context): File =
        File(context.getExternalFilesDir(null), "meal_photos").apply { mkdirs() }

    fun createPhotoUri(context: Context): Pair<File, Uri> {
        val file = File(photosDir(context), "photo_${Instant.now().toEpochMilli()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }

    fun copyFromUri(context: Context, source: Uri): File {
        val dest = File(photosDir(context), "photo_${Instant.now().toEpochMilli()}_${(0..9999).random()}.jpg")
        context.contentResolver.openInputStream(source)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        }
        return dest
    }

    /**
     * Reutilizar una foto ya usada en otra comida crea una copia independiente en vez de
     * apuntar al mismo archivo: así, si esa comida original se elimina, no se pierde la foto
     * de la comida nueva (los archivos de fotos no se borran al eliminar una comida, pero
     * mantenerlos independientes evita cualquier acoplamiento futuro).
     */
    fun copyFromFile(context: Context, source: File): File {
        val dest = File(photosDir(context), "photo_${Instant.now().toEpochMilli()}_${(0..9999).random()}.jpg")
        source.inputStream().use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        }
        return dest
    }

    /** Todas las fotos usadas alguna vez en comidas, más recientes primero. */
    fun listPreviousPhotos(context: Context): List<File> =
        photosDir(context).listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
}
