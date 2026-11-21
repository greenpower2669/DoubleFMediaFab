package com.doublefmediafab

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

data class GalleryEntry(val file: MediaFile, val info: String, val partner: MediaFile? = null)

class GalleryAdapter(
    private val onOpen: (GalleryEntry) -> Unit,
    private val onInspect: (GalleryEntry) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.Holder>() {
    private val entries = mutableListOf<GalleryEntry>()
    fun submit(newEntries: List<GalleryEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }
    override fun getItemCount() = entries.size

    class Holder(val tile: LinearLayout, val picture: ImageView, val title: TextView, val info: TextView) :
        RecyclerView.ViewHolder(tile)

    private fun dp(ctx: Context, value: Int) = (ctx.resources.displayMetrics.density * value).toInt()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val ctx = parent.context
        val tile = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 4), dp(ctx, 5), dp(ctx, 4), dp(ctx, 8))
            setBackgroundColor(Color.rgb(22, 34, 52))
        }
        val image = ImageView(ctx).apply {
            adjustViewBounds = false
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(34, 47, 63))
            contentDescription = "Aperçu du média"
        }
        tile.addView(image, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 108)))
        val title = TextView(ctx).apply {
            textSize = 12f; setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        }
        tile.addView(title)
        val info = TextView(ctx).apply {
            textSize = 11f; setTextColor(Color.rgb(165, 216, 227))
            maxLines = 2; gravity = Gravity.START
        }
        tile.addView(info)
        return Holder(tile, image, title, info)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = entries[position]
        val f = entry.file
        holder.title.text = (if (f.isVideo) "▶ " else "▣ ") + f.name
        holder.info.text = entry.info
        Glide.with(holder.picture)
            .load(f.uri)
            .centerCrop()
            .override(256, 256)
            .into(holder.picture)
        holder.tile.contentDescription = "${f.name}, ${entry.info}"
        holder.tile.setOnClickListener { onOpen(entry) }
        holder.tile.setOnLongClickListener { onInspect(entry); true }
    }
    override fun onViewRecycled(holder: Holder) {
        Glide.with(holder.picture).clear(holder.picture)
    }
}
