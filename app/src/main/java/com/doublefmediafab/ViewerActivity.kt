package com.doublefmediafab

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class ViewerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uriText = intent.getStringExtra("uri") ?: run { finish(); return }
        val uri = android.net.Uri.parse(uriText)
        val isVideo = intent.getBooleanExtra("video", false)
        val title = intent.getStringExtra("title") ?: "Média"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(7, 11, 21))
        }
        val heading = TextView(this).apply {
            text = "‹ Retour        " + title
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(20, 25, 12, 24)
            setOnClickListener { finish() }
            maxLines = 2
        }
        root.addView(heading)
        if (isVideo) {
            val video = PlayerView(this).apply {
                useController = true
                setBackgroundColor(Color.BLACK)
            }
            root.addView(video, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            val exo = ExoPlayer.Builder(this).build()
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.prepare()
            exo.playWhenReady = true
            video.player = exo
            player = exo
        } else {
            val image = ImageView(this).apply {
                setBackgroundColor(Color.BLACK)
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = title
            }
            root.addView(image, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            Glide.with(this).load(uri).fitCenter().into(image)
        }
        val footer = TextView(this).apply {
            text = "Photo : pincement pour zoomer via la galerie système à venir · Retour pour explorer"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(12, 14, 12, 16)
        }
        root.addView(footer)
        setContentView(root)
        AppLog.write(this, "VIEWER_OPEN", "video=$isVideo uri=$uriText")
    }

    override fun onStop() {
        player?.pause()
        super.onStop()
    }
    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
    companion object {
        fun open(context: Context, file: MediaFile) {
            context.startActivity(Intent(context, ViewerActivity::class.java).apply {
                putExtra("uri", file.uri.toString())
                putExtra("title", file.name)
                putExtra("video", file.isVideo)
            })
        }
    }
}
