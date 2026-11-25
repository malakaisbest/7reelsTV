package com.malakaonline.tv7reals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class CastAdapter(
    private var items: List<CastMember>
) : RecyclerView.Adapter<CastAdapter.CastViewHolder>() {

    fun updateItems(newItems: List<CastMember>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cast_card, parent, false)
        return CastViewHolder(view)
    }

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: ImageView = itemView.findViewById(R.id.castAvatar)
        private val name: TextView = itemView.findViewById(R.id.castName)
        private val character: TextView = itemView.findViewById(R.id.castCharacter)

        fun bind(item: CastMember) {
            name.text = item.name
            character.text = item.character

            avatar.load(item.getProfileUrl()) {
                crossfade(true)
            }
        }
    }
}
