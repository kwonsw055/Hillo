package com.freefriday.hillo

import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.w3c.dom.Text

class RVAdapter(var data:Array<Freetime>?) : RecyclerView.Adapter<RVHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RVHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_content, parent, false)
        return RVHolder(view)
    }

    override fun getItemCount(): Int = data?.size ?: 0

    override fun onBindViewHolder(holder: RVHolder, position: Int) {
        holder.setText(data!![position].id.toString(), data!![position].time)
    }

}

class RVHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val text_name = itemView.findViewById<TextView>(R.id.recycler_name)
    val text_time = itemView.findViewById<TextView>(R.id.recycler_time)
    fun setText(name: String, time: Timepair){
        text_name.text = name
        text_time.text = time.toString()
    }
}