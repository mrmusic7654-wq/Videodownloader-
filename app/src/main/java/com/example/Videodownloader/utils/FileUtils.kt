package com.example.videodownloader.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object FileUtils {
    
    fun getDownloadsFolder(): File? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, use MediaStore
            null
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
    }
    
    fun createVideoFile(context: Context, fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            val file = File(getDownloadsFolder(), fileName)
            Uri.fromFile(file)
        }
    }
    
    fun getFileNameFromUrl(url: String): String {
        val segments = url.split("/")
        val lastSegment = segments.lastOrNull() ?: "video"
        return if (lastSegment.contains(".")) {
            lastSegment
        } else {
            "video_${System.currentTimeMillis()}.mp4"
        }
    }
}
