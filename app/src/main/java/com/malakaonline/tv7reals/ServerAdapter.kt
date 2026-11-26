package com.malakaonline.tv7reals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ServerAdapter(
    private val servers: List<StreamServer>,
    private val onServerClick: (StreamServer) -> Unit
) : RecyclerView.Adapter<ServerAdapter.ServerViewHolder>() {

    private var activeServerId: String = servers.firstOrNull()?.id ?: "vidfast"

    fun setActiveServer(id: String) {
        activeServerId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_server_card, parent, false)
        return ServerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.bind(servers[position])
    }

    override fun getItemCount(): Int = servers.size

    inner class ServerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.serverTitle)
        private val desc: TextView = itemView.findViewById(R.id.serverDesc)

        fun bind(item: StreamServer) {
            title.text = item.name
            desc.text = item.description

            val isActive = item.id == activeServerId
            itemView.isSelected = isActive

            itemView.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120).start()
                    v.elevation = 8f
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    v.elevation = 0f
                }
            }

            itemView.setOnClickListener {
                setActiveServer(item.id)
                onServerClick(item)
            }
        }
    }
}
