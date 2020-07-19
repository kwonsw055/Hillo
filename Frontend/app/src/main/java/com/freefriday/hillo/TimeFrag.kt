//Fragment for showing my time table
package com.freefriday.hillo

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class TimeFrag : Fragment() {
    //text field for inputting time
    //lateinit var text_time : EditText
    lateinit var num_day : NumberPicker
    lateinit var num_start : NumberPicker
    lateinit var num_end : NumberPicker
    //text view for showing time list
    lateinit var text_list : TextView
    //button for submitting
    lateinit var btn_submit : Button
    //list of time
    val timelist= mutableListOf<TimeTable>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Initiate Views
        val inflated = inflater.inflate(R.layout.fragment_addtable, container, false)
        //text_time = inflated.findViewById<EditText>(R.id.text_time)
        num_day = inflated.findViewById(R.id.num_day)
        num_start = inflated.findViewById(R.id.num_start)
        num_end = inflated.findViewById(R.id.num_end)

        text_list = inflated.findViewById(R.id.text_list)
        btn_submit = inflated.findViewById(R.id.btn_submit)
        val applicationContext = inflater.context

        fun disableInput(num : NumberPicker){
            val txt = num.getChildAt(0) as EditText
            txt.isFocusable = false
            txt.inputType = InputType.TYPE_NULL
        }

        num_day.minValue = 0
        num_day.maxValue = 6
        num_day.setFormatter { day2kor(int2date(it)!!) }
        num_day.value = 0
        disableInput(num_day)
        (NumberPicker::class.java.getDeclaredField("mInputText").apply { isAccessible = true }.get(num_day) as EditText).filters = emptyArray()

        val timeformater = {
            i: Int ->
            val h = i/2
            val m = i%2*30
            (if(h>12){h-12}else{h}).toString()+":"+"%02d".format(m)+(if(h>=12){" PM"}else{" AM"})
        }

        num_start.minValue = 0
        num_start.maxValue = 47
        num_start.wrapSelectorWheel = false
        num_start.setFormatter(timeformater)
        disableInput(num_start)
        (NumberPicker::class.java.getDeclaredField("mInputText").apply { isAccessible = true }.get(num_start) as EditText).filters = emptyArray()

        num_end.minValue = 0
        num_end.maxValue = 47
        num_end.wrapSelectorWheel = false
        num_end.setFormatter(timeformater)

        disableInput(num_end)
        (NumberPicker::class.java.getDeclaredField("mInputText").apply { isAccessible = true }.get(num_end) as EditText).filters = emptyArray()

        //Get all time from time table DB
        getallTable(applicationContext, timelist,{
            activity?.runOnUiThread { text_list.text = timelist.print() }
        })

        //function for adding time
        fun addtext(time:TimeTable){
            timelist.add(time)
            insertTable(applicationContext, time, {})
            activity?.runOnUiThread { text_list.text = timelist.print() }
        }
        fun addtext(day:String, start:String, end:String){
            val convday = str2date(kor2day(day).toUpperCase())
            val convstart = start.toInt()
            val convend = end.toInt()
            val time = TimeTable(null, convday!!, convstart, convend)
            addtext(time)
        }


        fun picker2time(p: NumberPicker):Int=(p.value/2)*100+(p.value%2)*30


        fun findoverlap(t: TimeTable):Boolean
            =timelist.any {
            (it.day == t.day)&&!((it.end<t.start)||(t.end<it.start))
        }

        btn_submit.setOnClickListener func@{
            val day = int2date(num_day.value)
            val start = picker2time(num_start)
            val end = picker2time(num_end)

            if(start>=end){
                Toast.makeText(applicationContext, getString(R.string.toast_start_after_end), Toast.LENGTH_SHORT).show()
                return@func
            }
            if(findoverlap(TimeTable(null, day!!, start, end))){
                Toast.makeText(applicationContext, getString(R.string.toast_overlap), Toast.LENGTH_SHORT).show()
                return@func
            }
            addtext(TimeTable(null, day!!, start, end))
        }
        //set button to submit text
        /*
        btn_submit.setOnClickListener {
            if(text_time.text.toString() == "delete"){ //delete
                deleteAll(applicationContext, {
                    timelist.clear()
                    activity?.runOnUiThread { text_list.text = timelist.print() }
                })
            }else{ //do parsing
                val reg = """(\w+) (\d+) (\d+)""".toRegex()
                var (day, start, end) = reg.find(text_time.text)!!.destructured
                addtext(day, start, end)
            }
            activity?.runOnUiThread { text_list.text = null }
        }

         */

        //return value of onCreateView
        return inflated
    }

    //On end, post time table.
    override fun onStop() {
        super.onStop()
        myid?.let {
            val id = it
            val day = mutableListOf<Int>()
            val start = mutableListOf<Int>()
            val end = mutableListOf<Int>()

            //Parse each components
            getInverseTime().forEach {
                day.add(it.day.num)
                start.add(it.start)
                end.add(it.end)
            }

            //Post free time
            RetrofitObj.getinst().settime(TimeList(id, day, start, end)).enqueue(CallBackClass{
                Log.i("DEBUGMSG", it.toString())
            })
        }
    }

    //Inverts timelist to freetime
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

    //Converts Korean day string to English day string
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

    fun day2kor(day: Date):String=
        when(day){
            Date.mon->"월"
            Date.tue->"화"
            Date.wed->"수"
            Date.thu->"목"
            Date.fri->"금"
            Date.sat->"토"
            Date.sun->"일"
        }


    //print in style for TimeTable
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