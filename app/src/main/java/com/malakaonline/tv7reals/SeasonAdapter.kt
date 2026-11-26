package com.malakaonline.tv7reals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SeasonAdapter(
    private var seasons: List<SeasonItem>,
    private val onSeasonSelected: (SeasonItem) -> Unit
) : RecyclerView.Adapter<SeasonAdapter.SeasonViewHolder>() {

    private var activeSeasonNum: Int = 1

    fun updateSeasons(newSeasons: List<SeasonItem>, selectedSeason: Int = 1) {
        seasons = newSeasons
        activeSeasonNum = selectedSeason
        notifyDataSetChanged()
    }

    fun setActiveSeason(seasonNum: Int) {
        activeSeasonNum = seasonNum
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_season_chip, parent, false)
        return SeasonViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeasonViewHolder, position: Int) {
        holder.bind(seasons[position])
    }

    override fun getItemCount(): Int = seasons.size

    inner class SeasonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textSeasonName: TextView = itemView.findViewById(R.id.textSeasonName)

        fun bind(item: SeasonItem) {
            textSeasonName.text = item.name
            val isActive = item.seasonNumber == activeSeasonNum

            if (isActive) {
                itemView.setBackgroundResource(R.drawable.bg_card_selector)
                itemView.isSelected = true
                textSeasonName.setTextColor(itemView.context.getColor(R.color.brand_emerald_light))
            } else {
                itemView.setBackgroundResource(R.drawable.bg_tv_button)
                itemView.isSelected = false
                textSeasonName.setTextColor(itemView.context.getColor(R.color.white))
            }

            itemView.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).start()
                    v.elevation = 8f
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    v.elevation = 0f
                }
            }

            itemView.setOnClickListener {
                setActiveSeason(item.seasonNumber)
                onSeasonSelected(item)
            }
        }
    }
}
