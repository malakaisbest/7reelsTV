package com.malakaonline.tv7reals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ColumnMovieAdapter(
    private var items: List<MovieItem>,
    private val onItemFocused: (MovieItem, Int) -> Unit,
    private val onItemClick: (MovieItem) -> Unit
) : RecyclerView.Adapter<ColumnMovieAdapter.ColumnViewHolder>() {

    private var activePosition: Int = -1

    fun updateItems(newItems: List<MovieItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setActivePosition(pos: Int) {
        val prev = activePosition
        activePosition = pos
        if (prev in items.indices) notifyItemChanged(prev)
        if (activePosition in items.indices) notifyItemChanged(activePosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_column_card, parent, false)
        return ColumnViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColumnViewHolder, position: Int) {
        holder.bind(items[position], position == activePosition)
    }

    override fun getItemCount(): Int = items.size

    inner class ColumnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val poster: ImageView = itemView.findViewById(R.id.colPoster)
        private val title: TextView = itemView.findViewById(R.id.colTitle)
        private val year: TextView = itemView.findViewById(R.id.colYear)
        private val rating: TextView = itemView.findViewById(R.id.colRating)

        fun bind(item: MovieItem, isCurrentActive: Boolean) {
            title.text = item.title
            val relYear = item.releaseDate.split("-").firstOrNull() ?: ""
            year.text = relYear

            if (item.voteAverage > 0) {
                rating.text = "★ ${String.format("%.1f", item.voteAverage)}"
                rating.visibility = View.VISIBLE
            } else {
                rating.visibility = View.GONE
            }

            poster.load(item.getPosterUrl()) {
                crossfade(true)
            }

            if (isCurrentActive) {
                itemView.alpha = 1.0f
                itemView.scaleX = 1.04f
                itemView.scaleY = 1.04f
            } else {
                itemView.alpha = 0.85f
                itemView.scaleX = 1.0f
                itemView.scaleY = 1.0f
            }

            itemView.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.08f).scaleY(1.08f).alpha(1.0f).setDuration(120).start()
                    v.elevation = 8f
                    onItemFocused(item, bindingAdapterPosition)
                } else {
                    v.animate().scaleX(if (bindingAdapterPosition == activePosition) 1.04f else 1.0f)
                        .scaleY(if (bindingAdapterPosition == activePosition) 1.04f else 1.0f)
                        .alpha(if (bindingAdapterPosition == activePosition) 1.0f else 0.85f)
                        .setDuration(120).start()
                    v.elevation = 0f
                }
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
