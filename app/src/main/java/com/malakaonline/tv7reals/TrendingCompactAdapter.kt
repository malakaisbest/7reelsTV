package com.malakaonline.tv7reals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class TrendingCompactAdapter(
    private var items: List<MovieItem>,
    private val onItemFocused: (MovieItem) -> Unit,
    private val onItemClick: (MovieItem) -> Unit
) : RecyclerView.Adapter<TrendingCompactAdapter.TrendingViewHolder>() {

    fun updateItems(newItems: List<MovieItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trending_compact, parent, false)
        return TrendingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrendingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TrendingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val poster: ImageView = itemView.findViewById(R.id.trendPoster)
        private val title: TextView = itemView.findViewById(R.id.trendTitle)

        fun bind(item: MovieItem) {
            title.text = item.title
            poster.load(item.getPosterUrl()) {
                crossfade(true)
            }

            itemView.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(120).start()
                    v.elevation = 8f
                    onItemFocused(item)
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
