//Fragment for showing my time table
package com.freefriday.hillo

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

//string formater for start/end time pickers
val timeformater = {
        i: Int ->
    val h = i/2
    val m = i%2*30
    (if(h>12){h-12}else{h}).toString()+":"+"%02d".format(m)+(if(h>=12){" PM"}else{" AM"})
}

class TempTimeFrag: TimeFrag(){
    override val appcontext: Context = joinappContext
    override val timervadapter : TimetableRVAdapter by lazy{ TimetableRVAdapter(null, false)}
    override fun afteraddtext(time: TimeTable) {}//don't add to database
    override fun postTime(timelist: TimeList){
        RetrofitObj.getinst().settemptime(session, timelist).enqueue(CallBackClass{
            Log.i("DEBUGMSG", it.toString())
            Toast.makeText(appcontext, "Temp Timetable upload success", Toast.LENGTH_SHORT).show()
        }.addAfterFailure {
            Toast.makeText(appcontext, "Temp Timetable upload failure", Toast.LENGTH_SHORT).show()
            Toast.makeText(appcontext, it.errorBody()?.string(), Toast.LENGTH_SHORT).show()
        })
    }
}

open class TimeFrag : Fragment() {

    //numberpickers for time input
    lateinit var num_day : NumberPicker
    lateinit var num_start : NumberPicker
    lateinit var num_end : NumberPicker

    //recycler view for showing time list
    lateinit var rec_time : RecyclerView

    //button for submitting
    lateinit var btn_submit : Button
    open val appcontext = appContext

    //list of time
    val timelist= mutableListOf<TimeTable>()

    //Recycler View Adapter used for time table fragment
    open val timervadapter : TimetableRVAdapter by lazy{ TimetableRVAdapter(null, true)}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //Initiate Views
        val inflated = inflater.inflate(R.layout.fragment_addtable, container, false)

        //initialize number pickers
        num_day = inflated.findViewById(R.id.num_day)
        num_start = inflated.findViewById(R.id.num_start)
        num_end = inflated.findViewById(R.id.num_end)

        //Initialize buttons
        btn_submit = inflated.findViewById(R.id.btn_submit)

        //Application Context
        val applicationContext = inflater.context

        //disables keyboard input for numberpicker
        fun disableInput(num : NumberPicker){
            val txt = num.getChildAt(0) as EditText
            txt.isFocusable = false
            txt.inputType = InputType.TYPE_NULL
        }

        //set day picker
        num_day.minValue = 0 //MON
        num_day.maxValue = 6 //SUN
        num_day.setFormatter { day2kor(int2date(it)!!) }
        num_day.value = 0
        disableInput(num_day)
        //for initial value showing
        (NumberPicker::class.java.getDeclaredField("mInputText").apply { isAccessible = true }.get(num_day) as EditText).filters = emptyArray()

        //set start time picker
        num_start.minValue = 0
        num_start.maxValue = 47
        num_start.wrapSelectorWheel = false
        num_start.setFormatter(timeformater)
        num_start.value = 18
        disableInput(num_start)
        //for initial value showing
        (NumberPicker::class.java.getDeclaredField("mInputText").apply { isAccessible = true }.get(num_start) as EditText).filters = emptyArray()

        //set end time picker
        num_end.minValue = 0
        num_end.maxValue = 47
        num_end.wrapSelectorWheel = false
        num_end.setFormatter(timeformater)
        num_end.value = 20
        disableInput(num_end)
        //for initial value showing
        (NumberPicker::class.java.getDeclaredField("mInputText").apply { isAccessible = true }.get(num_end) as EditText).filters = emptyArray()

        //set recycler view
        rec_time = inflated.findViewById(R.id.rec_time)
        rec_time.adapter = timervadapter
        rec_time.layoutManager = LinearLayoutManager(inflater.context)
        rec_time.setHasFixedSize(true)
        timervadapter.data = timelist

        //Get all time from time table DB
        getallTable(applicationContext, timelist,{
            timelist.sortBy { it.day.num*2400+it.start }
            activity?.runOnUiThread {timervadapter.notifyDataSetChanged()}
        })

        //number picker value to integer time value
        fun picker2time(p: NumberPicker):Int=(p.value/2)*100+(p.value%2)*30

        //check if overlapping time exists
        fun findoverlap(t: TimeTable):Boolean
            =timelist.any {
            (it.day == t.day)&&!((it.end<t.start)||(t.end<it.start))
        }

        //submit button
        btn_submit.setOnClickListener func@{
            val day = int2date(num_day.value)
            val start = picker2time(num_start)
            val end = picker2time(num_end)

            //wrong time range
            if(start>=end){
                Toast.makeText(applicationContext, getString(R.string.toast_start_after_end), Toast.LENGTH_SHORT).show()
                return@func
            }

            //overlapping time exists
            if(findoverlap(TimeTable(null, day!!, start, end))){
                Toast.makeText(applicationContext, getString(R.string.toast_overlap), Toast.LENGTH_SHORT).show()
                return@func
            }

            //if not, add time
            addtext(TimeTable(null, day!!, start, end))
        }

        //return value of onCreateView
        return inflated
    }

    //function for adding time
    open fun addtext(time:TimeTable){
        timelist.add(time)
        timelist.sortBy { it.day.num*2400+it.start }
        val pos = timelist.indexOf(time)
        afteraddtext(time)
        activity?.runOnUiThread {
            timervadapter.notifyItemInserted(pos)
            timervadapter.notifyItemRangeChanged(pos, timervadapter.itemCount)
        }
    }

    open fun afteraddtext(time:TimeTable)=insertTable(activity?.applicationContext!!, time, {})

    //On end, post time table.
    override fun onStop() {
        super.onStop()
        myid?.let {
            val id = it

            fun int2time(time:Int):Int{
                return (time/2)*100+(time%2)*30
            }

            val starttime = this.activity?.getPreferences(Context.MODE_PRIVATE)?.getInt(getString(R.string.pref_starttime), defStarttime) ?: defStarttime
            val endtime = this.activity?.getPreferences(Context.MODE_PRIVATE)?.getInt(getString(R.string.pref_endtime), defEndtime) ?: defEndtime
            val day = mutableListOf<Int>()
            val start = mutableListOf<Int>()
            val end = mutableListOf<Int>()

            //Parse each components
            getInverseTime(int2time(starttime), int2time(endtime)).forEach {
                day.add(it.day.num)
                start.add(it.start)
                end.add(it.end)
            }

            //Post free time
            postTime(TimeList(id, day, start, end))
        }
    }

    open fun postTime(timelist: TimeList){
        RetrofitObj.getinst().settime(timelist).enqueue(CallBackClass{
            Log.i("DEBUGMSG", it.toString())
            Toast.makeText(appcontext, "Timetable upload success", Toast.LENGTH_SHORT).show()
        }.addAfterFailure {
            Toast.makeText(appcontext, "Timetable upload failure", Toast.LENGTH_SHORT).show()
            Toast.makeText(appcontext, it.errorBody()?.string(), Toast.LENGTH_SHORT).show()
        })
    }

    //Inverts timelist to freetime
    fun getInverseTime(starttime:Int, endtime:Int):MutableList<TimeTable>{
        val inverted = mutableListOf<TimeTable>()
        val grouped = timelist.groupBy { it.day }

        for(day in 0 .. 6){
            val times = grouped[int2date(day)]?.sortedBy { it.start }
            var itr = starttime
            times?.let {
                for(time in times){
                    if(itr>endtime) break
                    if(itr<time.start && time.start<endtime){
                        inverted.add(TimeTable(null, int2date(day)!!, itr, time.start))
                        itr = time.end
                    }
                }
            }
            if(itr<endtime)inverted.add(TimeTable(null, int2date(day)!!, itr, endtime))
        }
        return inverted
    }

    companion object {
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

        //Converts day to Korean string
        fun day2kor(day: Date): String =
            when (day) {
                Date.mon -> "월"
                Date.tue -> "화"
                Date.wed -> "수"
                Date.thu -> "목"
                Date.fri -> "금"
                Date.sat -> "토"
                Date.sun -> "일"
            }
    }
}