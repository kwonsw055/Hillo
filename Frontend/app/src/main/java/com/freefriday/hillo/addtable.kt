package com.freefriday.hillo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class addtable : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addtable)

        val text_time = findViewById<EditText>(R.id.text_time)
        val btn_submit = findViewById<Button>(R.id.btn_submit)
        val text_list = findViewById<TextView>(R.id.text_list)
        val timelist= mutableListOf<TimeTable>()

        getallTable(applicationContext, timelist,{
            runOnUiThread{text_list.text = timelist.print()}
        })

        fun addtext(day:String, start:String, end:String){
            val convday = str2date(kor2day(day).toUpperCase())
            val convstart = start.toInt()
            val convend = end.toInt()
            val time = TimeTable(null, convday, convstart, convend)
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