package com.example.escouter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.escouter.databinding.ActivityImageViewerBinding

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrl = intent.getStringExtra("image_url")

        if (imageUrl.isNullOrEmpty()) {
            finish()
            return
        }

        binding.imgFullScreen.load(imageUrl) {
            crossfade(true)
        }
    }
}