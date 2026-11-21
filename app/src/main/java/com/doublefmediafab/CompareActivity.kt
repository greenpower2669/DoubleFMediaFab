package com.doublefmediafab

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide

class CompareActivity : AppCompatActivity() {
    private val players = mutableListOf<ExoPlayer>()
    private val trashLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { outcome ->
        AppLog.write(this, "TRASH_RESULT", "resultCode=${outcome.resultCode}")
        if (outcome.resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val a = intent.getStringExtra("uriA") ?: run { finish(); return }
        val b = intent.getStringExtra("uriB") ?: run { finish(); return }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 8, 10, 8)
            setBackgroundColor(Color.rgb(16, 21, 34))
        }
        val heading = TextView(this).apply {
            text = "↔ Comparer les deux médias"
            textSize = 21f
            setTextColor(Color.WHITE)
            setPadding(8, 12, 8, 22)
        }
        root.addView(heading)
        val columns = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        columns.addView(pane(a, intent.getStringExtra("titleA").orEmpty(),
            intent.getBooleanExtra("videoA", false)), LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        columns.addView(pane(b, intent.getStringExtra("titleB").orEmpty(),
            intent.getBooleanExtra("videoB", false)), LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        root.addView(columns, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val footer = TextView(this).apply {
            text = "La suppression nécessite ta confirmation Android. Un seul fichier est envoyé à la corbeille."
            setTextColor(Color.rgb(184, 218, 230))
            textSize = 13f
            setPadding(12, 12, 12, 12)
        }
        root.addView(footer)
        setContentView(root)
        AppLog.write(this, "COMPARE_OPEN", "a=$a b=$b")
    }

    private fun pane(uriText: String, title: String, isVideo: Boolean): LinearLayout {
        val uri = Uri.parse(uriText)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 0, 4, 0)
        }
        val name = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 12f
            maxLines = 3
        }
        column.addView(name)
        if (isVideo) {
            val exo = ExoPlayer.Builder(this).build()
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.prepare()
            val view = PlayerView(this).apply {
                player = exo
                useController = true
            }
            column.addView(view, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            players.add(exo)
        } else {
            val image = ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(Color.BLACK)
            }
            column.addView(image, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            Glide.with(this).load(uri).fitCenter().into(image)
        }
        val see = Button(this).apply {
            text = "Agrandir"
            isAllCaps = false
            setOnClickListener {
                startActivity(Intent(this@CompareActivity, ViewerActivity::class.java).apply {
                    putExtra("uri", uriText)
                    putExtra("video", isVideo)
                    putExtra("title", title)
                })
            }
        }
        column.addView(see)
        val delete = Button(this).apply {
            text = "Corbeille"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(149, 59, 61)))
            setOnClickListener { confirmTrash(uri, title) }
        }
        column.addView(delete)
        return column
    }

    private fun confirmTrash(uri: Uri, name: String) {
        AlertDialog.Builder(this)
            .setTitle("Placer dans la corbeille ?")
            .setMessage("Fichier : $name\n\nAndroid demandera une confirmation. La corbeille permet une restauration pendant la durée définie par le système.")
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Continuer") { _, _ ->
                try {
                    val request = MediaStore.createTrashRequest(contentResolver, listOf(uri), true)
                    trashLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    AppLog.write(this, "TRASH_REQUEST", uri.toString())
                } catch (e: Exception) {
                    AppLog.write(this, "TRASH_ERROR", "${e.javaClass.simpleName}: ${e.message}")
                    AlertDialog.Builder(this).setMessage("Impossible de demander la mise à la corbeille : ${e.message}")
                        .setPositiveButton("OK", null).show()
                }
            }.show()
    }

    override fun onStop() {
        players.forEach { it.pause() }
        super.onStop()
    }

    override fun onDestroy() {
        players.forEach { it.release() }
        players.clear()
        super.onDestroy()
    }

    companion object {
        fun open(context: Context, a: MediaFile, b: MediaFile) {
            context.startActivity(Intent(context, CompareActivity::class.java).apply {
                putExtra("uriA", a.uri.toString())
                putExtra("uriB", b.uri.toString())
                putExtra("titleA", a.name)
                putExtra("titleB", b.name)
                putExtra("videoA", a.isVideo)
                putExtra("videoB", b.isVideo)
            })
        }
    }
}
