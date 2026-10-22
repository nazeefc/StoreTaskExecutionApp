package com.example.storetaskexecutionapp.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.storetaskexecutionapp.domain.model.PhotoType
import java.io.File

class PhotoStore(private val context: Context) {
    private val appContext = context.applicationContext

    fun photosDir(): File = File(appContext.filesDir, "photos").apply { mkdirs() }

    fun createPhotoFile(taskId: String, type: PhotoType): File {
        val safeId = taskId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(photosDir(), "${safeId}_${type.name.lowercase()}.jpg")
    }

    fun uriFor(file: File): Uri = FileProvider.getUriForFile(
        appContext,
        "${appContext.packageName}.fileprovider",
        file
    )

    fun deleteIfExists(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }
}
