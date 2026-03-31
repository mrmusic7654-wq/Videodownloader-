package com.example.videodownloader

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Create a simple layout
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
            val urlEditText = EditText(this)
            urlEditText.hint = "Enter URL"
            urlEditText.setText("https://www.google.com")
            
            // Status text
            val statusText = TextView(this)
            statusText.text = "Status: Ready"
            statusText.setPadding(0, 16, 0, 16)
            
            // WebView
            val webView = WebView(this)
            webView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
            
            // Add views
            layout.addView(title)
            layout.addView(urlEditText)
            layout.addView(statusText)
            layout.addView(webView)
            
            setContentView(layout)
            
            // Configure WebView with error handling
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    statusText.text = "✅ Page loaded: $url"
                }
                
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    statusText.text = "❌ Error: $description"
                }
            }
            
            // Load a URL
            webView.loadUrl("https://www.google.com")
            statusText.text = "Loading google.com..."
            
            // Add button to change URL
            val goButton = Button(this)
            goButton.text = "Go to URL"
            goButton.setOnClickListener {
                val url = urlEditText.text.toString()
                if (url.isNotEmpty()) {
                    val finalUrl = if (url.startsWith("http")) url else "https://$url"
                    statusText.text = "Loading: $finalUrl"
                    webView.loadUrl(finalUrl)
                }
            }
            layout.addView(goButton, 3) // Insert button before WebView
            
            Toast.makeText(this, "App started! Enter URL and tap Go", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            // Show error on screen
            val errorText = TextView(this)
            errorText.text = "Error: ${e.message}\n\n${e.stackTraceToString().take(300)}"
            errorText.setTextColor(android.graphics.Color.RED)
            setContentView(errorText)
        }
    }
}
