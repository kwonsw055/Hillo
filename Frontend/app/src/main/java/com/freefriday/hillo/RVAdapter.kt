//Recycler View adapters
package com.freefriday.hillo

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey

//Glide Module for getting image
@GlideModule
class GlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        builder.apply { RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).signature(
            ObjectKey(System.currentTimeMillis().toShort())
        ) }
    }
}

//RecyclerView Adapter for Freetime
class FreetimeRVAdapter(var data:MutableList<Freetime>?) : RecyclerView.Adapter<FreetimeRVAdapter.RVHolder>() {

    //Application context
    lateinit var context: Context

    //Cache for id-name pair
    val namecache:MutableMap<Long, String> = mutableMapOf()

    //Cache for id-url pair
    val urlcache:MutableMap<Long, String?> = mutableMapOf()

    //Class for Holder
    class RVHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        //Text View for friend name
        val text_name = itemView.findViewById<TextView>(R.id.recycler_name)

        //Text View for recommended time
        val text_time = itemView.findViewById<TextView>(R.id.recycler_time)

        //Button for making appointment
        val btn_start = itemView.findViewById<Button>(R.id.recycler_recstart)

        //Button for deleting recommendation
        val btn_del = itemView.findViewById<Button>(R.id.recycler_recdel)

        //Image View for friend profile image
        val img_rec = itemView.findViewById<ImageView>(R.id.recycler_image)

        //Set name and time
        fun setText(name: String?, time: Timepair){
            text_name.text = name
            text_time.text = time.toString()
        }

        //Set image
        fun setImg(url:String?){
            //Using Glide for url image view
            GlideApp.with(img_rec).load(url).error(R.drawable.ic_launcher_foreground).into(img_rec)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RVHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_rec_content, parent, false)
        context = parent.context
        return RVHolder(view)
    }

    override fun getItemCount(): Int = data?.size ?: 0

    override fun onBindViewHolder(holder: RVHolder, position: Int) {

        //If cache is missing id, add to cache
        if(!(namecache.containsKey(data!![position].id)&&urlcache.containsKey(data!![position].id))){
            //Get friend info
            getFriend(context, data!![position].id,{f:Friend?->
                (context as Activity).runOnUiThread{
                    if(data!!.size>position){
                        //Set cache
                        namecache[data!![position].id] = f?.nickname?:context.getString(R.string.name_unknown)
                        urlcache[data!![position].id] = f?.url

                        //Set holder text and image
                        holder.setText(namecache[data!![position].id], data!![position].time)
                        holder.setImg(urlcache[data!![position].id])
                    }
                }
            })
        }
        //If cache has data, use it
        else{
            //Set holder text and image
            holder.setText(namecache[data!![position].id], data!![position].time)
            holder.setImg(urlcache[data!![position].id])
        }

        holder.btn_del.setOnClickListener { deleteData(position) }
    }

    //Used for deleting an item
    fun deleteData(pos: Int){
        data?.removeAt(pos)
        this.notifyItemRemoved(pos)
        this.notifyItemRangeChanged(pos, itemCount)
    }
}

//Recycler View Adapter for Timetable
class TimetableRVAdapter(var data:MutableList<TimeTable>?) : RecyclerView.Adapter<TimetableRVAdapter.RVHolder>(){

    //Application Context
    var context: Context? = null

    //Class for Holder
    class RVHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        //Text View for showing time
        val text_time = itemView.findViewById<TextView>(R.id.recycler_timetable)

        //Button for removing item
        val btn_remove = itemView.findViewById<Button>(R.id.recycler_timedel)

        //Set time
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

    //Used for deleting an item
    fun deleteData(pos: Int){
        deleteTable(context!!, data!![pos],{})
        data?.removeAt(pos)
        this.notifyItemRemoved(pos)
        this.notifyItemRangeChanged(pos, itemCount)
    }
}