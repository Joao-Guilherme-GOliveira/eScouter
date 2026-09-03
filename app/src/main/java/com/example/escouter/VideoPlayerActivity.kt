package com.example.escouter

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.escouter.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoUrl = intent.getStringExtra("video_url")

        if (videoUrl.isNullOrEmpty()) {
            finish()
            return
        }

        player = ExoPlayer.Builder(this).build()

        binding.playerView.player = player

        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    override fun onStop() {
        super.onStop()

        player?.release()
        player = null
    }
}