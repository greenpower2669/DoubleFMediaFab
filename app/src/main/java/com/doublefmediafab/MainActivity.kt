package com.doublefmediafab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var files = emptyList<MediaFile>()
    private var groups = emptyList<MatchGroup>()
    private var working = false
    private var mode = "galerie"
    private var mediaFilter = 2
    private lateinit var status: TextView
    private lateinit var gallery: GalleryAdapter
    private lateinit var search: EditText
    private lateinit var grid: RecyclerView

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        AppLog.write(this, "PERMISSIONS", grants.toString())
        loadMedia()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.write(this, "APP_START", "sdk=${Build.VERSION.SDK_INT}")
        buildScreen()
        requestAccess()
    }

    private fun dp(x: Int) = (resources.displayMetrics.density * x).toInt()
    private fun makeButton(title: String, action: () -> Unit): Button =
        Button(this).apply {
            text = title
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(36, 75, 99)))
            setOnClickListener { action() }
        }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(5))
            setBackgroundColor(Color.rgb(16, 21, 34))
        }
        val heading = TextView(this).apply {
            text = "🔎 Double F Media Fab"
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(dp(4), dp(5), 0, dp(8))
        }
        root.addView(heading)
        val horizontal = HorizontalScrollView(this)
        horizontal.isHorizontalScrollBarEnabled = false
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(makeButton("🖼 Galerie") { mode = "galerie"; render() })
        buttons.addView(makeButton("🔍 Doublons") { mode = "doublons"; render() })
        buttons.addView(makeButton("⚡ Analyser") { analyze() })
        buttons.addView(makeButton("↻ Actualiser") { loadMedia() })
        buttons.addView(makeButton("📋 Journaux") { showLogs() })
        horizontal.addView(buttons)
        root.addView(horizontal)
        val filters = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        filters.addView(makeButton("Tout") { mediaFilter = 2; render() })
        filters.addView(makeButton("Photos") { mediaFilter = 0; render() })
        filters.addView(makeButton("Vidéos") { mediaFilter = 1; render() })
        root.addView(filters)
        status = TextView(this).apply {
            setTextColor(Color.rgb(181, 224, 231))
            textSize = 14f
            text = "Préparation de la médiathèque…"
            setPadding(dp(5), dp(4), 0, dp(6))
        }
        root.addView(status)
        search = EditText(this).apply {
            hint = "Rechercher un nom ou un dossier"
            textSize = 15f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        root.addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(49)))
        gallery = GalleryAdapter(
            onOpen = { entry ->
                if (entry.partner != null) {
                    CompareActivity.open(this, entry.file, entry.partner)
                } else {
                    ViewerActivity.open(this, entry.file)
                }
            },
            onInspect = { entry -> ViewerActivity.open(this, entry.file) }
        )
        grid = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            adapter = gallery
            setHasFixedSize(true)
            itemAnimator = null
        }
        root.addView(grid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun requestAccess() {
        val permissions = when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= 33 -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            permissionLauncher.launch(permissions)
        } else loadMedia()
    }

    private fun loadMedia() {
        if (working) return
        working = true
        status.text = "Lecture des photos et vidéos…"
        Thread {
            try {
                val found = MediaFiles.scan(this)
                runOnUiThread {
                    files = found; working = false
                    AppLog.write(this, "MEDIA_SCAN", "count=${files.size}")
                    render()
                }
            } catch (e: Exception) {
                AppLog.write(this, "SCAN_ERROR", "${e.javaClass.simpleName}: ${e.message}")
                runOnUiThread { working = false; status.text = "Accès médias : ${e.message}" }
            }
        }.start()
    }

    private fun analyze() {
        if (working) return
        if (files.isEmpty()) {
            AlertDialog.Builder(this).setMessage("Aucun média accessible. Vérifie les autorisations photos et vidéos.")
                .setPositiveButton("OK", null).show()
            return
        }
        working = true
        status.text = "Analyse lancée, calcul sur le téléphone…"
        Thread {
            try {
                val result = SimilarityEngine(this).analyze(files) { progress ->
                    runOnUiThread { status.text = progress }
                }
                runOnUiThread {
                    groups = result
                    mode = "doublons"
                    working = false
                    render()
                }
            } catch (e: Exception) {
                AppLog.write(this, "ANALYSE_FATAL", "${e.javaClass.simpleName}: ${e.message}\n${e.stackTraceToString()}")
                runOnUiThread {
                    working = false
                    status.text = "Analyse interrompue : consulte les journaux"
                    showLogs()
                }
            }
        }.start()
    }

    private fun render() {
        if (working) return
        val q = search.text?.toString()?.trim()?.lowercase(Locale.ROOT).orEmpty()
        fun accepted(file: MediaFile): Boolean =
            (mediaFilter == 2 || file.kind == mediaFilter) &&
                (q.isBlank() || file.name.lowercase(Locale.ROOT).contains(q) ||
                    file.folder.lowercase(Locale.ROOT).contains(q))
        val entries: List<GalleryEntry>
        if (mode == "doublons") {
            entries = groups.flatMap { group ->
                group.items.filter(::accepted).map { file ->
                    GalleryEntry(file, "${group.label} · ${group.score}% · ${formatSize(file.size)}",
                        group.items.firstOrNull { it.uri != file.uri })
                }
            }
            val total = groups.sumOf { it.savedBytes }
            status.text = "${groups.size} groupes · ${entries.size} médias affichés · gain potentiel ${formatSize(total)} (à vérifier)"
        } else {
            entries = files.filter(::accepted).map {
                GalleryEntry(it, "${formatSize(it.size)} · ${it.folder}")
            }
            status.text = "${entries.size} médias affichés / ${files.size} trouvés · appui long : plein écran"
        }
        gallery.submit(entries)
    }

    private fun showLogs() {
        val content = AppLog.contents(this)
        AlertDialog.Builder(this)
            .setTitle("Journaux de diagnostic")
            .setMessage(content.takeLast(12000))
            .setPositiveButton("Fermer", null)
            .setNeutralButton("Partager") { _, _ ->
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "DoubleFMediaFab – diagnostic")
                    putExtra(Intent.EXTRA_TEXT, content.takeLast(120000))
                }, "Partager les journaux"))
            }.show()
    }

    override fun onResume() {
        super.onResume()
        // MediaStore can change after the system trash confirmation.
        if (::gallery.isInitialized && files.isNotEmpty() && !working) {
            // Refresh is always available through the toolbar.
            render()
        }
    }

    companion object {
        fun formatSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes o"
            val units = arrayOf("Ko", "Mo", "Go", "To")
            var value = bytes.toDouble()
            var unit = -1
            do { value /= 1024.0; unit++ } while (value >= 1024 && unit < units.size - 1)
            return String.format(Locale.FRANCE, "%.1f %s", value, units[unit])
        }
    }
}
