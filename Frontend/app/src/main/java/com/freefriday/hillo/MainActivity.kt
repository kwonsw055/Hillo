package com.freefriday.hillo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
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