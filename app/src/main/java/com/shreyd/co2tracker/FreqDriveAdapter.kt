package com.shreyd.co2tracker

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class FreqDriveAdapter(val mList: List<FreqDrive>) : RecyclerView.Adapter<FreqDriveAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_freqdrives, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val itemsViewModel = mList[position]
        println(itemsViewModel.startLocS)

        holder.startLoc.text = "${itemsViewModel.startLocS}"

        holder.endLoc.text = "${itemsViewModel.endLocS}"


        holder.mcView.setOnClickListener {
            val ptIntent = Intent(holder.itemView.context, PublicTransport::class.java)
            ptIntent.putExtra("driveId", itemsViewModel.id)
            holder.itemView.context.startActivity(ptIntent)
        }

    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val startLoc: TextView = itemView.findViewById(R.id.startLocation)
        val endLoc: TextView = itemView.findViewById(R.id.endLocation)
        val mcView: MaterialCardView = itemView.findViewById(R.id.MCView)
    }
}