package com.example.videodownloader.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DownloadReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        when (action) {
            "DOWNLOAD_COMPLETE" -> {
                val fileName = intent.getStringExtra("file_name") ?: "video"
                Toast.makeText(context, "$fileName downloaded successfully!", Toast.LENGTH_LONG).show()
            }
            "DOWNLOAD_FAILED" -> {
                val error = intent.getStringExtra("error") ?: "Unknown error"
                Toast.makeText(context, "Download failed: $error", Toast.LENGTH_LONG).show()
            }
        }
    }
}
