package com.freefriday.hillo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
        val timelist:MutableList<String> = mutableListOf()
        fun addtext(str: String){
            timelist.add(str)
            text_list.text = timelist.print()
        }

        btn_submit.setOnClickListener {
            val reg = """(\w+) (\d+) (\d+)""".toRegex()
            var (day, start, end) = reg.find(text_time.text)!!.destructured
            day = kor2day(day)
            Toast.makeText(applicationContext, "$day/$start/$end",Toast.LENGTH_LONG).show()
            addtext("$day/$start/$end")
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
    fun MutableList<String>.print():String{
        val result = StringBuilder()
        for(str in this){
            result.append("-")
            result.append(str)
            result.append("\n")
        }
        return result.toString()
    }
}