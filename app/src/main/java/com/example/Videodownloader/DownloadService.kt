package com.example.videodownloader

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class DownloadService : Service() {
    
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "download_channel"
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val url = it.getStringExtra("download_url") ?: return START_NOT_STICKY
            val fileName = it.getStringExtra("file_name") ?: "video_${System.currentTimeMillis()}.mp4"
            
            startForeground(NOTIFICATION_ID, createNotification("Starting download...", 0))
            downloadInBackground(url, fileName, startId)
        }
        return START_NOT_STICKY
    }
    
    private fun downloadInBackground(url: String, fileName: String, startId: Int) {
        Thread {
            try {
                val connection = URL(url).openConnection()
                connection.connect()
                
                val inputStream = connection.getInputStream()
                val totalBytes = connection.contentLength
                var downloadedBytes = 0
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                // Save to temporary file first
                val tempFile = File(cacheDir, fileName)
                FileOutputStream(tempFile).use { outputStream ->
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        if (totalBytes > 0) {
                            val progress = (downloadedBytes * 100 / totalBytes)
                            updateNotification("Downloading...", progress)
                        }
                    }
                }
                inputStream.close()
                
                // Move to permanent storage
                val saved = saveToStorage(tempFile, fileName)
                
                // Clean up temp file
                tempFile.delete()
                
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                if (saved) {
                    updateNotification("Download complete!", 100)
                    // Show completion notification
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = saved
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    
                    val completedNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Download Complete")
                        .setContentText(fileName)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build()
                    
                    notificationManager.notify(NOTIFICATION_ID + 1, completedNotification)
                } else {
                    updateNotification("Download failed!", 0)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                updateNotification("Error: ${e.message}", 0)
            } finally {
                stopForeground(true)
                stopSelf(startId)
            }
        }.start()
    }
    
    private fun saveToStorage(tempFile: File, fileName: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        tempFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                val destFile = File(downloadsDir, fileName)
                tempFile.copyTo(destFile, overwrite = true)
                
                // Scan file
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(destFile)
                sendBroadcast(mediaScanIntent)
                
                Uri.fromFile(destFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(message: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Video Downloader")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(message: String, progress: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Video Downloader")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(progress < 100)
            .build()
        
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
