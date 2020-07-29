package com.freefriday.hillo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

//Deprecatedaddtable
//Now using TimeFrag as fragment, instead of activity.
/*
class addtable : AppCompatActivity() {
    val timelist= mutableListOf<TimeTable>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_addtable)

        val text_time = findViewById<EditText>(R.id.text_time)
        val btn_submit = findViewById<Button>(R.id.btn_submit)
        val text_list = findViewById<TextView>(R.id.text_list)

        getallTable(applicationContext, timelist,{
            runOnUiThread{text_list.text = timelist.print()}
        })

        fun addtext(day:String, start:String, end:String){
            val convday = str2date(kor2day(day).toUpperCase())
            val convstart = start.toInt()
            val convend = end.toInt()
            val time = TimeTable(null, convday!!, convstart, convend)
            timelist.add(time)
            insertTable(applicationContext, time, {})
            text_list.text = timelist.print()
        }

        btn_submit.setOnClickListener {
            if(text_time.text.toString() == "delete"){
                deleteAll(applicationContext, {
                    timelist.clear()
                    runOnUiThread{text_list.text = timelist.print()}
                })
            }else{
                val reg = """(\w+) (\d+) (\d+)""".toRegex()
                var (day, start, end) = reg.find(text_time.text)!!.destructured
                addtext(day, start, end)
            }
            text_time.text = null
        }
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
            RetrofitObj.getinst().settime(tl).enqueue(CallBackClass())
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
 */