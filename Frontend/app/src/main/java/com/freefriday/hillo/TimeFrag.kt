package com.freefriday.hillo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class TimeFrag : Fragment() {
    lateinit var text_time : EditText
    lateinit var text_list : TextView
    lateinit var btn_submit : Button
    val timelist= mutableListOf<TimeTable>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val inflated = inflater.inflate(R.layout.fragment_addtable, container, false)
        text_time = inflated.findViewById<EditText>(R.id.text_time)
        text_list = inflated.findViewById(R.id.text_list)
        btn_submit = inflated.findViewById(R.id.btn_submit)
        val applicationContext = inflater.context
        getallTable(applicationContext, timelist,{
            activity?.runOnUiThread { text_list.text = timelist.print() }
        })

        fun addtext(day:String, start:String, end:String){
            val convday = str2date(kor2day(day).toUpperCase())
            val convstart = start.toInt()
            val convend = end.toInt()
            val time = TimeTable(null, convday!!, convstart, convend)
            timelist.add(time)
            insertTable(applicationContext, time, {})
            activity?.runOnUiThread { text_list.text = timelist.print() }
        }

        btn_submit.setOnClickListener {
            if(text_time.text.toString() == "delete"){
                deleteAll(applicationContext, {
                    timelist.clear()
                    activity?.runOnUiThread { text_list.text = timelist.print() }
                })
            }else{
                val reg = """(\w+) (\d+) (\d+)""".toRegex()
                var (day, start, end) = reg.find(text_time.text)!!.destructured
                addtext(day, start, end)
            }
            activity?.runOnUiThread { text_list.text = null }
        }
        return inflated
    }

    override fun onStop() {
        super.onStop()
        myid?.let {
            val id = it
            val day = mutableListOf<Int>()
            val start = mutableListOf<Int>()
            val end = mutableListOf<Int>()
            getInverseTime().forEach {
                day.add(it.day.num)
                start.add(it.start)
                end.add(it.end)
            }
            val tl = TimeList(id, day, start, end)
            RetrofitObj.getinst().settime(tl).enqueue(CallBackClass{
                Log.i("DEBUGMSG", it.toString())
            })
        }
    }

    fun getInverseTime():MutableList<TimeTable>{
        val inverted = mutableListOf<TimeTable>()
        val grouped = timelist.groupBy { it.day }

        for(day in 0 .. 6){
            val times = grouped[int2date(day)]?.sortedBy { it.start }
            var itr = 0
            times?.let {
                for(time in times){
                    inverted.add(TimeTable(null, int2date(day)!!, itr, time.start))
                    itr = time.end
                }
            }
            inverted.add(TimeTable(null, int2date(day)!!, itr, 2359))
        }
        return inverted
    }

    fun kor2day(str:String)= when(str){
        "월"->"MON"
        "화"->"TUE"
        "수"->"WED"
        "목"->"THU"
        "금"->"FRI"
        "토"->"SAT"
        "일"->"SUN"
        else->str
    }

    fun MutableList<TimeTable>.print():String{
        val result = StringBuilder()
        for(str in this){
            result.append("-")
            result.append("${str.day}/${str.start}/${str.end}")
            result.append("\n")
        }
        return result.toString()
    }
}