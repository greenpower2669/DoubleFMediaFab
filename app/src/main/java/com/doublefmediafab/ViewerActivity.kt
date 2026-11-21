package com.doublefmediafab

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlin.math.abs

/** Navigation plein écran : glissement, zoom tactile, playlist et diaporama. */
class ViewerActivity : AppCompatActivity() {
    private val items = ArrayList<MediaFile>()
    private var position = 0
    private var slideshow = false
    private var toolsVisible = false
    private var changing = false
    private var pendingTrash: Int? = null
    private var player: ExoPlayer? = null
    private var photo: ZoomImageView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val photoAdvance = Runnable { if (slideshow) move(1) }
    private lateinit var stage: FrameLayout
    private lateinit var top: LinearLayout
    private lateinit var bottom: LinearLayout
    private lateinit var heading: TextView
    private lateinit var slideButton: Button
    private lateinit var pauseButton: Button
    private lateinit var zoomIn: Button
    private lateinit var zoomOut: Button
    private lateinit var strip: RecyclerView
    private lateinit var adapter: ThumbAdapter

    private val trashLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val n = pendingTrash
        pendingTrash = null
        AppLog.write(this, "TRASH_VIEWER_RESULT", "result=${result.resultCode}")
        if (result.resultCode == RESULT_OK && n != null && n in items.indices) {
            items.removeAt(n)
            setResult(RESULT_OK)
            if (items.isEmpty()) finish() else {
                position = n.coerceAtMost(items.lastIndex)
                adapter.notifyDataSetChanged()
                display(false)
            }
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val uri = intent.getStringExtra("uri") ?: run { finish(); return }
        if (sharedQueue.any { it.uri.toString() == uri }) items.addAll(sharedQueue)
        else items.add(MediaFile(Uri.parse(uri), intent.getStringExtra("title") ?: "Média",
            0L, 0L, "", if (intent.getBooleanExtra("video", false)) 1 else 0, ""))
        val selected = state?.getString("selected") ?: uri
        position = items.indexOfFirst { it.uri.toString() == selected }.coerceAtLeast(0)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        layout()
        display(false)
        Toast.makeText(this, "Glisser : suivant/précédent · Pincer : zoom · Double-tap : outils",
            Toast.LENGTH_LONG).show()
        AppLog.write(this, "VIEWER_OPEN", "items=${items.size}")
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun tool(label: String, click: () -> Unit) = Button(this).apply {
        text = label; textSize = 13f; isAllCaps = false
        setTextColor(Color.WHITE)
        backgroundTintList = ColorStateList.valueOf(Color.rgb(35, 70, 91))
        setOnClickListener { click() }
    }

    private fun layout() {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        stage = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(stage, FrameLayout.LayoutParams(-1, -1))
        top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.rgb(13, 23, 36))
        }
        top.addView(tool("‹ Retour") { finish() })
        heading = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 14f; maxLines = 2
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        top.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(top, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))

        bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(13, 23, 36))
            setPadding(dp(3), dp(2), dp(3), dp(8))
        }
        val buttons = LinearLayout(this)
        buttons.addView(tool("◀") { move(-1) })
        buttons.addView(tool("▶") { move(1) })
        zoomOut = tool("− Zoom") { photo?.zoomOut() }
        zoomIn = tool("+ Zoom") { photo?.zoomIn() }
        buttons.addView(zoomOut); buttons.addView(zoomIn)
        pauseButton = tool("⏯ Vidéo") {
            player?.let { if (it.isPlaying) it.pause() else it.play() }
            updateTools()
        }
        buttons.addView(pauseButton)
        slideButton = tool("▶ Diaporama") { setSlide(!slideshow) }
        buttons.addView(slideButton)
        buttons.addView(tool("🗑 Corbeille") { confirmTrash() }.apply {
            backgroundTintList = ColorStateList.valueOf(Color.rgb(141, 52, 63))
        })
        val horizontal = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(buttons)
        }
        bottom.addView(horizontal)
        bottom.addView(TextView(this).apply {
            text = "Liste de lecture · toucher une miniature pour choisir"
            textSize = 12f; setTextColor(Color.rgb(172, 224, 235))
            setPadding(dp(8), 0, dp(3), dp(2))
        })
        adapter = ThumbAdapter()
        strip = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ViewerActivity,
                LinearLayoutManager.HORIZONTAL, false)
            adapter = this@ViewerActivity.adapter
        }
        bottom.addView(strip, LinearLayout.LayoutParams(-1, dp(78)))
        root.addView(bottom, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        top.visibility = View.GONE; bottom.visibility = View.GONE
        setContentView(root)
    }

    private fun toggleTools() {
        toolsVisible = !toolsVisible
        top.visibility = if (toolsVisible) View.VISIBLE else View.GONE
        bottom.visibility = if (toolsVisible) View.VISIBLE else View.GONE
        if (toolsVisible) strip.scrollToPosition(position)
        updateTools()
    }

    private fun updateTools() {
        if (items.isEmpty()) return
        val item = items[position]
        heading.text = "${position + 1}/${items.size} · ${item.name}"
        zoomOut.visibility = if (item.isVideo) View.GONE else View.VISIBLE
        zoomIn.visibility = if (item.isVideo) View.GONE else View.VISIBLE
        pauseButton.visibility = if (item.isVideo) View.VISIBLE else View.GONE
        pauseButton.text = if (player?.isPlaying == true) "⏸ Pause" else "▶ Vidéo"
        slideButton.text = if (slideshow) "⏸ Diaporama" else "▶ Diaporama"
        adapter.notifyDataSetChanged()
    }

    private fun move(by: Int) {
        if (items.size <= 1) return
        moveTo((position + by + items.size) % items.size)
    }
    private fun moveTo(next: Int) {
        if (changing || next !in items.indices || next == position) return
        changing = true
        handler.removeCallbacks(photoAdvance)
        val old = stage.getChildAt(0)
        if (old == null) {
            position = next; changing = false; display(false)
        } else old.animate().alpha(0f).scaleX(.95f).scaleY(.95f).setDuration(140)
            .withEndAction { position = next; changing = false; display(true) }.start()
    }

    private fun display(animated: Boolean) {
        handler.removeCallbacks(photoAdvance)
        player?.release(); player = null
        photo?.let { Glide.with(this).clear(it) }; photo = null
        stage.removeAllViews()
        if (items.isEmpty()) return
        val f = items[position]
        val view: View = if (f.isVideo) showVideo(f) else showPhoto(f)
        stage.addView(view, FrameLayout.LayoutParams(-1, -1))
        if (animated) {
            view.alpha = 0f; view.scaleX = 1.06f; view.scaleY = 1.06f
            view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(240).start()
        }
        updateTools()
        if (toolsVisible) strip.scrollToPosition(position)
        if (slideshow && !f.isVideo) handler.postDelayed(photoAdvance, 5000L)
        AppLog.write(this, "VIEWER_MEDIA", "${position + 1}/${items.size}: ${f.name}")
    }

    private fun showPhoto(f: MediaFile): ZoomImageView = ZoomImageView(this).also { v ->
        v.onDoubleTapTools = { toggleTools() }
        v.onSlide = { move(it) }
        v.contentDescription = f.name
        photo = v
        Glide.with(this).load(f.uri)
            .override(resources.displayMetrics.widthPixels * 2,
                resources.displayMetrics.heightPixels * 2)
            .dontTransform().into(v)
    }

    private fun showVideo(f: MediaFile): PlayerView {
        val exo = ExoPlayer.Builder(this).build()
        player = exo
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && slideshow) handler.post { move(1) }
                updateTools()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { updateTools() }
        })
        val gesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onDoubleTap(e: MotionEvent): Boolean { toggleTools(); return true }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent,
                                 velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x; val dy = e2.y - e1.y
                if (abs(dx) > dp(65) && abs(dx) > abs(dy) * 1.25f) {
                    move(if (dx < 0f) 1 else -1)
                    return true
                }
                return false
            }
        })
        val result = PlayerView(this).apply {
            useController = false
            player = exo
            setOnTouchListener { _, e -> gesture.onTouchEvent(e); true }
        }
        exo.setMediaItem(MediaItem.fromUri(f.uri))
        exo.prepare()
        exo.playWhenReady = true
        return result
    }

    private fun setSlide(active: Boolean) {
        slideshow = active
        handler.removeCallbacks(photoAdvance)
        if (active) {
            if (items[position].isVideo) {
                if (player?.playbackState == Player.STATE_ENDED) move(1)
                else player?.play()
            } else handler.postDelayed(photoAdvance, 5000L)
        }
        updateTools()
    }

    private fun confirmTrash() {
        setSlide(false)
        val f = items[position]
        AlertDialog.Builder(this).setTitle("Envoyer à la corbeille ?")
            .setMessage("${f.name}\n\nAndroid demandera une confirmation et permettra de restaurer le média.")
            .setNegativeButton("Conserver", null)
            .setPositiveButton("Continuer") { _, _ ->
                try {
                    pendingTrash = position
                    val pending = MediaStore.createTrashRequest(contentResolver, listOf(f.uri), true)
                    trashLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    AppLog.write(this, "VIEWER_TRASH_REQUEST", f.uri.toString())
                } catch (e: Exception) {
                    pendingTrash = null
                    AppLog.write(this, "VIEWER_TRASH_ERROR", "${e.javaClass.simpleName}: ${e.message}")
                    AlertDialog.Builder(this).setMessage("Corbeille indisponible : ${e.message}")
                        .setPositiveButton("OK", null).show()
                }
            }.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (items.isNotEmpty()) outState.putString("selected", items[position].uri.toString())
        super.onSaveInstanceState(outState)
    }
    override fun onStop() {
        handler.removeCallbacks(photoAdvance)
        slideshow = false
        player?.pause()
        super.onStop()
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release(); player = null
        photo?.let { Glide.with(this).clear(it) }
        super.onDestroy()
    }

    private inner class ThumbAdapter : RecyclerView.Adapter<ThumbAdapter.Holder>() {
        inner class Holder(val box: LinearLayout, val image: ImageView, val count: TextView) :
            RecyclerView.ViewHolder(box)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val box = LinearLayout(this@ViewerActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(3), dp(3), dp(3))
            }
            val image = ImageView(this@ViewerActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            box.addView(image, LinearLayout.LayoutParams(dp(69), dp(50)))
            val count = TextView(this@ViewerActivity).apply {
                textSize = 10f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            }
            box.addView(count)
            return Holder(box, image, count)
        }
        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(holder: Holder, p: Int) {
            val f = items[p]
            Glide.with(holder.image).load(f.uri).override(160, 120).centerCrop()
                .into(holder.image)
            holder.count.text = (if (f.isVideo) "▶ " else "") + (p + 1)
            holder.box.setBackgroundColor(
                if (p == position) Color.rgb(28, 123, 147) else Color.TRANSPARENT)
            holder.box.contentDescription = "Média ${p + 1} : ${f.name}"
            holder.box.setOnClickListener {
                val i = holder.bindingAdapterPosition
                if (i != RecyclerView.NO_POSITION) moveTo(i)
            }
        }
        override fun onViewRecycled(holder: Holder) { Glide.with(holder.image).clear(holder.image) }
    }

    companion object {
        // Référence volatile locale au processus : pas de transaction Binder volumineuse.
        private var sharedQueue: List<MediaFile> = emptyList()
        fun intent(context: Context, file: MediaFile, queue: List<MediaFile> = listOf(file)): Intent {
            sharedQueue = queue.toList()
            return Intent(context, ViewerActivity::class.java).apply {
                putExtra("uri", file.uri.toString())
                putExtra("title", file.name)
                putExtra("video", file.isVideo)
            }
        }
        fun open(context: Context, file: MediaFile, queue: List<MediaFile> = listOf(file)) {
            context.startActivity(intent(context, file, queue))
        }
    }
}
