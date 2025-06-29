
package com.example.audiosplitter

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {
    companion object { const val REQUEST_CODE_CAPTURE = 1001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(Button(this).apply {
            text = "Start Audio Splitter"
            setOnClickListener {
                val mpManager = getSystemService(MediaProjectionManager::class.java)
                startActivityForResult(mpManager.createScreenCaptureIntent(), REQUEST_CODE_CAPTURE)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CAPTURE && resultCode == Activity.RESULT_OK) {
            data?.let {
                AudioCaptureService.start(this, it)
            }
        }
    }
}
