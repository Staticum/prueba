package com.staticum.urodiario.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object PhotoFiles {
    private fun photosDir(context: Context): File =
        File(context.filesDir, "record_photos").apply { mkdirs() }

    fun createNewPhotoUri(context: Context): Uri {
        val file = File(photosDir(context), "IMG_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun copyContentUriToAppStorage(context: Context, sourceUri: Uri): Uri {
        val file = File(photosDir(context), "IMG_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
