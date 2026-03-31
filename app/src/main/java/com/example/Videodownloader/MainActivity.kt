package com.example.videodownloader

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val webView = WebView(this)
        setContentView(webView)
        
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Video Downloader</title>
                <style>
                    body {
                        font-family: system-ui;
                        background: linear-gradient(135deg, #667eea, #764ba2);
                        padding: 20px;
                        margin: 0;
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 20px;
                        padding: 20px;
                        text-align: center;
                    }
                    h1 {
                        color: #667eea;
                        margin-bottom: 20px;
                    }
                    input {
                        width: 100%;
                        padding: 12px;
                        margin: 10px 0;
                        border: 1px solid #ddd;
                        border-radius: 10px;
                        font-size: 16px;
                        box-sizing: border-box;
                    }
                    button {
                        width: 100%;
                        padding: 12px;
                        background: #667eea;
                        color: white;
                        border: none;
                        border-radius: 10px;
                        font-size: 16px;
                        font-weight: bold;
                        cursor: pointer;
                        margin-top: 10px;
                    }
                    button:hover {
                        background: #5a67d8;
                    }
                    .status {
                        margin-top: 15px;
                        padding: 10px;
                        background: #f0f0f0;
                        border-radius: 8px;
                        font-size: 14px;
                    }
                    .note {
                        margin-top: 15px;
                        font-size: 12px;
                        color: #666;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🎬 Video Downloader</h1>
                    <p>Paste any video URL below</p>
                    <input type="text" id="urlInput" placeholder="https://www.youtube.com/watch?v=...">
                    <button onclick="downloadVideo()">Download Video</button>
                    <div class="status" id="statusMsg">Ready</div>
                    <div class="note">💡 Supports YouTube, Facebook, Instagram, TikTok, and more</div>
                </div>
                
                <script>
                    function downloadVideo() {
                        var url = document.getElementById('urlInput').value;
                        var statusDiv = document.getElementById('statusMsg');
                        
                        if (!url) {
                            statusDiv.innerHTML = '❌ Please enter a URL';
                            return;
                        }
                        
                        statusDiv.innerHTML = '🔍 Processing...';
                        
                        // Extract video ID for YouTube
                        var videoId = '';
                        var ytMatch = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([^&?]+)/);
                        if (ytMatch) {
                            videoId = ytMatch[1];
                            var downloadUrl = 'https://www.y2mate.com/youtube/' + videoId;
                            statusDiv.innerHTML = '📥 Opening download page...';
                            window.open(downloadUrl, '_blank');
                        } else {
                            // For other sites, open in new tab
                            statusDiv.innerHTML = '📥 Opening URL...';
                            window.open(url, '_blank');
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }
}
