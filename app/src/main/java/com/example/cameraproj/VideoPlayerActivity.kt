package com.example.cameraproj

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cameraproj.databinding.ActivityGalleryBinding
import com.example.cameraproj.databinding.ActivityVideoPlayerBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import java.io.File

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var exoPlayer: ExoPlayer

    private val videoPlayerBindingBinding : ActivityVideoPlayerBinding by lazy {
        ActivityVideoPlayerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(videoPlayerBindingBinding.root)

        // Получаем путь к видео из Intent
        val videoPath = intent.getStringExtra("video_path") ?: return
        // Инициализируем ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build()
        val playerView = videoPlayerBindingBinding.playerView
        playerView.player = exoPlayer

        // Устанавливаем видео
        val mediaItem = MediaItem.fromUri(Uri.fromFile(File(videoPath)))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Освобождаем ресурсы ExoPlayer
    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }
}
