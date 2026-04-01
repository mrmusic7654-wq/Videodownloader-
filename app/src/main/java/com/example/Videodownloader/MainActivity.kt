package com.example.videodownloader

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create a simple layout with just text and a button
        val textView = TextView(this)
        textView.text = "Video Downloader\n\nApp is WORKING!\n\nTap the button below"
        textView.textSize = 20f
        textView.gravity = android.view.Gravity.CENTER
        textView.setPadding(32, 32, 32, 32)
        
        val button = Button(this)
        button.text = "Click Me"
        button.setOnClickListener {
            Toast.makeText(this, "App is working perfectly!", Toast.LENGTH_LONG).show()
        }
        
        // Create vertical layout
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.addView(textView)
        layout.addView(button)
        
        setContentView(layout)
    }
}
