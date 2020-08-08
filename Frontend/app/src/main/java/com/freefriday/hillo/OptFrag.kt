package com.freefriday.hillo

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

val defStarttime = 18//09:00AM
val defEndtime = 44//10:00PM

class OptFrag : Fragment(){

    var starttime = defStarttime
    var endtime = defEndtime

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val inflated = inflater.inflate(R.layout.fragment_options, container, false)

        //Shared Preferences
        val sp = this.activity?.getPreferences(Context.MODE_PRIVATE)

        //start time set
        val btn_start_up = inflated.findViewById<Button>(R.id.btn_opt_start_up)
        val btn_start_down = inflated.findViewById<Button>(R.id.btn_opt_start_down)
        val text_starttime = inflated.findViewById<TextView>(R.id.text_opt_starttime)
        starttime = sp?.getInt(getString(R.string.pref_starttime), defStarttime) ?: defStarttime
        text_starttime.text = timeformater(starttime)

        //end time set
        val btn_end_up = inflated.findViewById<Button>(R.id.btn_opt_end_up)
        val btn_end_down = inflated.findViewById<Button>(R.id.btn_opt_end_down)
        val text_endtime = inflated.findViewById<TextView>(R.id.text_opt_endtime)
        endtime = sp?.getInt(getString(R.string.pref_endtime), defEndtime) ?: defEndtime
        text_endtime.text = timeformater(endtime)

        btn_start_up.setOnClickListener {
            starttime++
            if(starttime>=endtime)starttime=endtime-1
            text_starttime.text = timeformater(starttime)
        }

        btn_start_down.setOnClickListener {
            starttime--
            if(starttime<0)starttime=0
            text_starttime.text = timeformater(starttime)
        }

        btn_end_up.setOnClickListener {
            endtime++
            if(endtime>47)endtime=47
            text_endtime.text = timeformater(endtime)
        }

        btn_end_down.setOnClickListener {
            endtime--
            if(endtime<=starttime)endtime=starttime+1
            text_endtime.text = timeformater(endtime)
        }

        return inflated
    }

    override fun onDestroy() {
        with(this.activity?.getPreferences(Context.MODE_PRIVATE)?.edit()){
            if(this != null){
                putInt(getString(R.string.pref_starttime), starttime)
                putInt(getString(R.string.pref_endtime), endtime)
                apply()
            }
        }
        super.onDestroy()
    }
}