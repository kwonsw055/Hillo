//Recycler View adapters
package com.freefriday.hillo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView



class FreetimeRVAdapter(var data:MutableList<Freetime>?) : RecyclerView.Adapter<FreetimeRVAdapter.RVHolder>() {
    class RVHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text_name = itemView.findViewById<TextView>(R.id.recycler_name)
        val text_time = itemView.findViewById<TextView>(R.id.recycler_time)
        val btn_start = itemView.findViewById<Button>(R.id.recycler_recstart)
        val btn_del = itemView.findViewById<Button>(R.id.recycler_recdel)
        fun setText(name: String, time: Timepair){
            text_name.text = name
            text_time.text = time.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RVHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_rec_content, parent, false)
        return RVHolder(view)
    }

    override fun getItemCount(): Int = data?.size ?: 0

    override fun onBindViewHolder(holder: RVHolder, position: Int) {
        holder.setText(data!![position].id.toString(), data!![position].time)
        holder.btn_del.setOnClickListener { deleteData(position) }
    }

    fun deleteData(pos: Int){
        data?.removeAt(pos)
        this.notifyItemRemoved(pos)
        this.notifyItemRangeChanged(pos, itemCount)
    }
}

class TimetableRVAdapter(var data:MutableList<TimeTable>?) : RecyclerView.Adapter<TimetableRVAdapter.RVHolder>(){
    var context: Context? = null
    class RVHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text_time = itemView.findViewById<TextView>(R.id.recycler_timetable)
        val btn_remove = itemView.findViewById<Button>(R.id.recycler_timedel)
        fun setText(time:TimeTable){
            text_time.text = time.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RVHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_time_content, parent, false)
        if(context == null)context = parent.context
        return RVHolder(view)
    }

    override fun getItemCount(): Int = data?.size ?: 0

    override fun onBindViewHolder(holder: RVHolder, position: Int) {
        holder.setText(data!![position])
        holder.btn_remove.setOnClickListener { deleteData(position) }
    }

    fun deleteData(pos: Int){
        deleteTable(context!!, data!![pos],{})
        data?.removeAt(pos)
        this.notifyItemRemoved(pos)
        this.notifyItemRangeChanged(pos, itemCount)
    }
}