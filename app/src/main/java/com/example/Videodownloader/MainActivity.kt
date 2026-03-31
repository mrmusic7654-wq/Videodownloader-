package com.example.videodownloader

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var goButton: Button
    private lateinit var backButton: Button
    private lateinit var forwardButton: Button
    private lateinit var refreshButton: Button
    private lateinit var downloadButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var bottomSheet: LinearLayout
    
    private var currentPageUrl = ""
    private var detectedVideoUrls = mutableListOf<String>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Request permissions
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
        
        initViews()
        setupWebView()
        setupControls()
    }
    
    private fun initViews() {
        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        goButton = findViewById(R.id.goButton)
        backButton = findViewById(R.id.backButton)
        forwardButton = findViewById(R.id.forwardButton)
        refreshButton = findViewById(R.id.refreshButton)
        downloadButton = findViewById(R.id.downloadButton)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        bottomSheet = findViewById(R.id.bottomSheet)
        
        // Initially hide bottom sheet
        bottomSheet.visibility = View.GONE
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadsImagesAutomatically = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                currentPageUrl = url ?: ""
                urlBar.setText(url)
                detectedVideoUrls.clear()
                hideBottomSheet()
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                
                // Inject script to detect videos
                injectVideoDetector()
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.startsWith("http")) {
                    view?.loadUrl(url)
                }
                return true
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
            }
            
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                // Check for video URLs in console
                val msg = consoleMessage.message()
                if (msg.contains(".mp4") || msg.contains(".webm") || msg.contains(".m3u8")) {
                    extractVideoUrl(msg)
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }
        
        // Add JavaScript interface
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onVideoDetected(url: String) {
                runOnUiThread {
                    extractVideoUrl(url)
                }
            }
            
            @JavascriptInterface
            fun log(message: String) {
                android.util.Log.d("WebView", message)
                if (message.contains(".mp4") || message.contains(".webm")) {
                    extractVideoUrl(message)
                }
            }
        }, "VideoDownloader")
        
        // Load default page
        webView.loadUrl("https://www.google.com")
    }
    
    private fun injectVideoDetector() {
        val script = """
            (function() {
                console.log("Video detector injected");
                
                // Detect video elements
                function detectVideos() {
                    var videos = document.querySelectorAll('video');
                    var sources = document.querySelectorAll('source');
                    var iframes = document.querySelectorAll('iframe');
                    
                    videos.forEach(function(video) {
                        var src = video.src || video.currentSrc;
                        if (src && (src.includes('.mp4') || src.includes('.webm'))) {
                            console.log("Video detected: " + src);
                            VideoDownloader.onVideoDetected(src);
                        }
                    });
                    
                    sources.forEach(function(source) {
                        var src = source.src;
                        if (src && (src.includes('.mp4') || src.includes('.webm'))) {
                            console.log("Source detected: " + src);
                            VideoDownloader.onVideoDetected(src);
                        }
                    });
                    
                    iframes.forEach(function(iframe) {
                        var src = iframe.src;
                        if (src && (src.includes('youtube') || src.includes('vimeo'))) {
                            console.log("Embed detected: " + src);
                        }
                    });
                }
                
                // Detect network requests for video files
                var originalOpen = XMLHttpRequest.prototype.open;
                XMLHttpRequest.prototype.open = function() {
                    var url = arguments[1];
                    if (url && (url.includes('.mp4') || url.includes('.webm') || url.includes('videoplayback'))) {
                        console.log("XHR Video: " + url);
                        VideoDownloader.onVideoDetected(url);
                    }
                    return originalOpen.apply(this, arguments);
                };
                
                // Detect video elements added dynamically
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        mutation.addedNodes.forEach(function(node) {
                            if (node.nodeName === 'VIDEO' || node.nodeName === 'SOURCE') {
                                detectVideos();
                            }
                        });
                    });
                });
                observer.observe(document.body, { childList: true, subtree: true });
                
                // Run initial detection
                setTimeout(detectVideos, 2000);
                setInterval(detectVideos, 5000);
                
                console.log("Video detector running");
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(script, null)
    }
    
    private fun extractVideoUrl(url: String) {
        // Clean and validate URL
        var cleanUrl = url.trim()
        if (cleanUrl.startsWith("//")) {
            cleanUrl = "https:$cleanUrl"
        }
        
        if (cleanUrl.contains(".mp4") || cleanUrl.contains(".webm") || cleanUrl.contains("videoplayback")) {
            if (!detectedVideoUrls.contains(cleanUrl)) {
                detectedVideoUrls.add(cleanUrl)
                showDownloadPrompt(cleanUrl)
            }
        }
    }
    
    private fun showDownloadPrompt(videoUrl: String) {
        // Show bottom sheet with download option
        bottomSheet.visibility = View.VISIBLE
        
        val titleView = bottomSheet.findViewById<TextView>(R.id.videoTitle)
        val sizeView = bottomSheet.findViewById<TextView>(R.id.videoSize)
        val downloadBtn = bottomSheet.findViewById<Button>(R.id.downloadBtn)
        val closeBtn = bottomSheet.findViewById<ImageButton>(R.id.closeSheet)
        
        titleView?.text = "Video Detected!"
        sizeView?.text = "Click download to save"
        
        downloadBtn?.setOnClickListener {
            startDownload(videoUrl)
            bottomSheet.visibility = View.GONE
        }
        
        closeBtn?.setOnClickListener {
            bottomSheet.visibility = View.GONE
        }
        
        // Auto-hide after 10 seconds
        android.os.Handler(mainLooper).postDelayed({
            if (bottomSheet.visibility == View.VISIBLE) {
                bottomSheet.visibility = View.GONE
            }
        }, 10000)
    }
    
    private fun hideBottomSheet() {
        bottomSheet.visibility = View.GONE
    }
    
    private fun startDownload(videoUrl: String) {
        Toast.makeText(this, "Downloading video...", Toast.LENGTH_SHORT).show()
        
        Thread {
            try {
                val fileName = "video_${System.currentTimeMillis()}.mp4"
                val connection = URL(videoUrl).openConnection()
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.connect()
                
                val inputStream = connection.getInputStream()
                val data = inputStream.readBytes()
                inputStream.close()
                
                val saved = saveVideoToStorage(fileName, data)
                
                runOnUiThread {
                    if (saved) {
                        Toast.makeText(this, "✅ Video saved to Downloads", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "❌ Download failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    private fun saveVideoToStorage(fileName: String, data: ByteArray): Boolean {
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
    
    private fun setupControls() {
        goButton.setOnClickListener {
            val url = urlBar.text.toString()
            if (url.isNotEmpty()) {
                val finalUrl = if (url.startsWith("http")) url else "https://$url"
                webView.loadUrl(finalUrl)
            }
        }
        
        backButton.setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        
        forwardButton.setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        
        refreshButton.setOnClickListener {
            webView.reload()
        }
        
        downloadButton.setOnClickListener {
            if (detectedVideoUrls.isNotEmpty()) {
                showVideoListDialog()
            } else {
                Toast.makeText(this, "No videos detected on this page", Toast.LENGTH_SHORT).show()
            }
        }
        
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
        
        urlBar.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                goButton.performClick()
                true
            } else false
        }
    }
    
    private fun showVideoListDialog() {
        val items = detectedVideoUrls.mapIndexed { index, url ->
            "Video ${index + 1}: ${url.takeLast(30)}"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Select Video to Download")
            .setItems(items) { _, which ->
                startDownload(detectedVideoUrls[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
