package com.malakaonline.tv7reals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class MovieAdapter(
    private var items: List<MovieItem>,
    private val onItemFocused: (MovieItem) -> Unit,
    private val onItemClick: (MovieItem) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    fun updateItems(newItems: List<MovieItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_card, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val poster: ImageView = itemView.findViewById(R.id.cardPoster)
        private val title: TextView = itemView.findViewById(R.id.cardTitle)
        private val year: TextView = itemView.findViewById(R.id.cardYear)
        private val rating: TextView = itemView.findViewById(R.id.cardRating)

        fun bind(item: MovieItem) {
            title.text = item.title

            val releaseYear = item.releaseDate.split("-").firstOrNull() ?: ""
            year.text = releaseYear

            if (item.voteAverage > 0) {
                rating.text = "★ ${String.format("%.1f", item.voteAverage)}"
                rating.visibility = View.VISIBLE
            } else {
                rating.visibility = View.GONE
            }

            poster.load(item.getPosterUrl()) {
                crossfade(true)
            }

            itemView.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start()
                    v.elevation = 8f
                    onItemFocused(item)
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    v.elevation = 0f
                }
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
