package com.example.videodownloader

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var urlEditText: EditText
    private lateinit var goButton: Button
    private lateinit var statusText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Create layout programmatically
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(16, 16, 16, 16)
            
            // Title
            val title = TextView(this)
            title.text = "🎬 Video Downloader"
            title.textSize = 24f
            title.gravity = android.view.Gravity.CENTER
            title.setPadding(0, 0, 0, 16)
            
            // URL input
            urlEditText = EditText(this)
            urlEditText.hint = "Enter URL (YouTube, Facebook, etc.)"
            urlEditText.setText("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            
            // Go button
            goButton = Button(this)
            goButton.text = "🌐 Load Website"
            
            // Status text
            statusText = TextView(this)
            statusText.text = "Status: Ready"
            statusText.setPadding(0, 16, 0, 16)
            
            // WebView
            webView = WebView(this)
            webView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
            )
            
            // Add all views
            layout.addView(title)
            layout.addView(urlEditText)
            layout.addView(goButton)
            layout.addView(statusText)
            layout.addView(webView)
            
            setContentView(layout)
            
            // Request storage permission for Android 9 and below
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        100
                    )
                }
            }
            
            setupWebView()
            setupButtons()
            
            statusText.text = "Status: Ready - Enter URL and tap Load"
            
        } catch (e: Exception) {
            statusText.text = "Error: ${e.message}"
            e.printStackTrace()
        }
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                statusText.text = "Status: Page loaded - Playing videos will be detected"
                injectVideoDetector()
            }
            
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                statusText.text = "Status: Error - $description"
            }
        }
        
        // Add JavaScript interface for video detection
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onVideoDetected(url: String) {
                runOnUiThread {
                    statusText.text = "Status: Video detected! Click Download"
                    showDownloadDialog(url)
                }
            }
        }, "VideoDownloader")
    }
    
    private fun injectVideoDetector() {
        val script = """
            (function() {
                function detectVideos() {
                    var videos = document.querySelectorAll('video');
                    videos.forEach(function(video) {
                        var src = video.src || video.currentSrc;
                        if (src && (src.includes('.mp4') || src.includes('.webm') || src.includes('blob:'))) {
                            VideoDownloader.onVideoDetected(src);
                        }
                    });
                }
                detectVideos();
                setInterval(detectVideos, 3000);
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(script, null)
    }
    
    private fun setupButtons() {
        goButton.setOnClickListener {
            var url = urlEditText.text.toString()
            if (url.isNotEmpty()) {
                if (!url.startsWith("http")) {
                    url = "https://$url"
                }
                statusText.text = "Status: Loading $url"
                webView.loadUrl(url)
            } else {
                statusText.text = "Status: Please enter a URL"
            }
        }
    }
    
    private var currentVideoUrl = ""
    
    private fun showDownloadDialog(videoUrl: String) {
        currentVideoUrl = videoUrl
        
        AlertDialog.Builder(this)
            .setTitle("🎬 Video Detected!")
            .setMessage("Download this video to your device?")
            .setPositiveButton("Download") { _, _ ->
                startDownload(videoUrl)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun startDownload(videoUrl: String) {
        statusText.text = "Status: Downloading..."
        
        Thread {
            try {
                val fileName = "video_${System.currentTimeMillis()}.mp4"
                val connection = URL(videoUrl).openConnection()
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connect()
                
                val data = connection.getInputStream().readBytes()
                val saved = saveVideoToStorage(fileName, data)
                
                runOnUiThread {
                    if (saved) {
                        statusText.text = "Status: ✅ Download complete! Saved to Downloads"
                        Toast.makeText(this, "Video saved to Downloads", Toast.LENGTH_LONG).show()
                    } else {
                        statusText.text = "Status: ❌ Download failed"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Status: Error - ${e.message}"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    private fun saveVideoToStorage(fileName: String, data: ByteArray): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    resolver.openOutputStream(it)?.use { out -> out.write(data) }
                    true
                } ?: false
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { out -> out.write(data) }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
