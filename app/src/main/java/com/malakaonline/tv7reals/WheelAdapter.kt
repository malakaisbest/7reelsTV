package com.malakaonline.tv7reals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class WheelAdapter(
    private var items: List<MovieItem>,
    private val onItemFocused: (MovieItem, Int) -> Unit,
    private val onItemClick: (MovieItem) -> Unit
) : RecyclerView.Adapter<WheelAdapter.WheelViewHolder>() {

    private var activePosition: Int = 0

    fun updateItems(newItems: List<MovieItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setActivePosition(pos: Int) {
        val prev = activePosition
        activePosition = pos
        notifyItemChanged(prev)
        notifyItemChanged(activePosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WheelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wheel_card, parent, false)
        return WheelViewHolder(view)
    }

    override fun onBindViewHolder(holder: WheelViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position == activePosition)
    }

    override fun getItemCount(): Int = items.size

    inner class WheelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val poster: ImageView = itemView.findViewById(R.id.wheelPoster)
        private val title: TextView = itemView.findViewById(R.id.wheelTitle)
        private val year: TextView = itemView.findViewById(R.id.wheelYear)
        private val rating: TextView = itemView.findViewById(R.id.wheelRating)

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
                itemView.scaleX = 1.05f
                itemView.scaleY = 1.05f
            } else {
                itemView.alpha = 0.75f
                itemView.scaleX = 1.0f
                itemView.scaleY = 1.0f
            }

            itemView.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.08f).scaleY(1.08f).alpha(1.0f).setDuration(150).start()
                    v.elevation = 6f
                    onItemFocused(item, bindingAdapterPosition)
                } else {
                    v.animate().scaleX(if (bindingAdapterPosition == activePosition) 1.05f else 1.0f)
                        .scaleY(if (bindingAdapterPosition == activePosition) 1.05f else 1.0f)
                        .alpha(if (bindingAdapterPosition == activePosition) 1.0f else 0.75f)
                        .setDuration(150).start()
                    v.elevation = 0f
                }
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
