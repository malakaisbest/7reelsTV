package com.malakaonline.tv7reals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ContinueWatchingAdapter(
    private var items: List<WatchHistoryItem>,
    private val onItemFocused: ((WatchHistoryItem) -> Unit)? = null,
    private val onItemClick: (WatchHistoryItem) -> Unit
) : RecyclerView.Adapter<ContinueWatchingAdapter.CwViewHolder>() {

    fun updateItems(newItems: List<WatchHistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CwViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_continue_watching, parent, false)
        return CwViewHolder(view)
    }

    override fun onBindViewHolder(holder: CwViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CwViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.cwThumbnail)
        private val episodeBadge: TextView = itemView.findViewById(R.id.cwEpisodeBadge)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.cwProgressBar)
        private val title: TextView = itemView.findViewById(R.id.cwTitle)
        private val timeInfo: TextView = itemView.findViewById(R.id.cwTimeInfo)

        fun bind(item: WatchHistoryItem) {
            title.text = item.title

            if (item.mediaType == "tv") {
                episodeBadge.text = "S${item.season}:E${item.episode}"
                episodeBadge.visibility = View.VISIBLE
            } else {
                episodeBadge.visibility = View.GONE
            }

            val progressPct = item.getProgressPercent()
            progressBar.progress = progressPct

            val posStr = item.getFormattedPosition()
            val remStr = item.getRemainingTime()
            timeInfo.text = if (remStr.isNotEmpty()) "$posStr • $remStr" else posStr

            val imgUrl = item.getBackdropUrl().ifEmpty { item.getPosterUrl() }
            thumbnail.load(imgUrl) {
                crossfade(true)
            }

            itemView.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120).start()
                    v.elevation = 8f
                    onItemFocused?.invoke(item)
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    v.elevation = 0f
                }
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
