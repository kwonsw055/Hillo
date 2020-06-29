package com.freefriday.hillo

import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.text.AttributedCharacterIterator

class MainActivity : AppCompatActivity() {
    val timeN = 48
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
        var table_main = findViewById<TableLayout>(R.id.table_main)

        for (i in 0 until timeN){
            var temp_row = TableRow(applicationContext)

            for (j in 0 until 7){
                var temp_button = Button(applicationContext)
                temp_button.layoutParams = ViewGroup.LayoutParams(50,20)
                temp_button.text = int2date(j)?.str
                temp_row.addView(temp_button)
            }
            table_main.addView(temp_row)
        }
        */
        val frame = findViewById<LinearLayout>(R.id.main_frame)
        val btn_rec = findViewById<Button>(R.id.btn_rec)
        val framechild = LayoutInflater.from(applicationContext).inflate(R.layout.fragment_todo_list,frame)
        framechild.visibility = View.VISIBLE
        val temp = LayoutInflater.from(applicationContext).inflate(R.layout.recycler_content,frame)
        temp.visibility = View.VISIBLE
        /*
        btn_rec.setOnClickListener {
            if(framechild.visibility == View.VISIBLE){
                framechild.visibility = View.GONE
                temp.visibility = View.VISIBLE
            }
            else{
                framechild.visibility = View.VISIBLE
                temp.visibility = View.GONE
            }
        }

         */
    }
}

fun int2date(i:Int):date?=
    when(i){
        0-> date.mon
        1-> date.tue
        2-> date.wed
        3-> date.thu
        4-> date.fri
        5-> date.sat
        6-> date.sun
        else->null
    }


enum class date(val str: String){
    mon("Mon"), tue("Tue"), wed("Wed"), thu("Thu"), fri("Fri"), sat("Sat"), sun("Sun");
}