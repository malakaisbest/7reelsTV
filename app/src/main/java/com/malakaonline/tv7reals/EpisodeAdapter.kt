package com.malakaonline.tv7reals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class EpisodeAdapter(
    private var episodes: List<EpisodeItem>,
    private val onEpisodeSelected: (EpisodeItem) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder>() {

    private var activeEpisodeNum: Int = 1

    fun updateEpisodes(newEpisodes: List<EpisodeItem>, selectedEpisode: Int = 1) {
        episodes = newEpisodes
        activeEpisodeNum = selectedEpisode
        notifyDataSetChanged()
    }

    fun setActiveEpisode(epNum: Int) {
        activeEpisodeNum = epNum
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_episode_card, parent, false)
        return EpisodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(episodes[position])
    }

    override fun getItemCount(): Int = episodes.size

    inner class EpisodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val still: ImageView = itemView.findViewById(R.id.epStill)
        private val number: TextView = itemView.findViewById(R.id.epNumber)
        private val title: TextView = itemView.findViewById(R.id.epTitle)

        fun bind(item: EpisodeItem) {
            number.text = "EP ${item.episodeNumber}"
            title.text = item.name

            val isActive = item.episodeNumber == activeEpisodeNum
            if (isActive) {
                itemView.alpha = 1.0f
                itemView.scaleX = 1.04f
                itemView.scaleY = 1.04f
            } else {
                itemView.alpha = 0.85f
                itemView.scaleX = 1.0f
                itemView.scaleY = 1.0f
            }

            still.load(item.getStillUrl()) {
                crossfade(true)
            }

            itemView.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.08f).scaleY(1.08f).alpha(1.0f).setDuration(120).start()
                    v.elevation = 8f
                } else {
                    v.animate().scaleX(if (item.episodeNumber == activeEpisodeNum) 1.04f else 1.0f)
                        .scaleY(if (item.episodeNumber == activeEpisodeNum) 1.04f else 1.0f)
                        .alpha(if (item.episodeNumber == activeEpisodeNum) 1.0f else 0.85f)
                        .setDuration(120).start()
                    v.elevation = 0f
                }
            }

            itemView.setOnClickListener {
                setActiveEpisode(item.episodeNumber)
                onEpisodeSelected(item)
            }
        }
    }
}
