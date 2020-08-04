//Data class for FreeTime
//Used for http response and post
package com.freefriday.hillo

import com.google.gson.annotations.SerializedName
import java.lang.StringBuilder

//data class for day-start time-end time
//Used for parsing Json response of 'get freetime recommendations'
data class Timepair(
    @SerializedName("day")
    val day: String,
    @SerializedName("start")
    val start: Int,
    @SerializedName("end")
    val end: Int
){
    fun timeformat(time: Int):String{
        val h = time/100
        val pm = if(h>=12){"PM"}else{"AM"}
        val m = time%100
        return "${if(h>12){h-12}else{h}}:${"%02d".format(m)} $pm"
    }
    override fun toString(): String {
        return "${TimeFrag.day2kor(str2date(day)!!)} ${timeformat(start)}~${timeformat(end)}"
    }
}

//data class for friend ID & available time list
//Used for parsing Json response of 'get freetime recommendations'
data class FreetimeJson(
    @SerializedName("fid") val fid:Long,
    @SerializedName("name") val name:String,
    @SerializedName("times") val times:Array<Timepair>
){
    override fun toString(): String {
        val str = StringBuilder()
        str.append("fid=$fid\n")
        str.append("name=$name\n")
        times.forEach {
            str.append(it.toString())
            str.append("\n")
        }
        return str.toString()
    }
    fun toFreetime():Array<Freetime>{
        val res = mutableListOf<Freetime>()
        times.forEach {
            res.add(Freetime(fid, name, it))
        }
        return res.toTypedArray()
    }
}

//data class for lists of freetimejson
//Used for parsing Json response of 'get freetime recommendations'
data class FreetimeArray(
    @SerializedName("result")
    val result: Array<FreetimeJson>
){
    override fun toString(): String {
        val str = StringBuilder()
        result.forEach {
            str.append(it.toString())
            str.append("\n")
        }
        return str.toString()
    }
    fun toFreetime():MutableList<Freetime>{
        val res = mutableListOf<Freetime>()
        result.forEach {
            it.toFreetime().forEach { t->res.add(t) }
        }
        return res
    }
}

//data class for friend ID & time pair
data class Freetime(
    val id: Long,
    val name: String,
    val time: Timepair
)

//data class for id-day-start time-end time
//Used for free time POST
data class TimeList(
    @SerializedName("id") val id:Long,
    @SerializedName("day") val day:List<Int>,
    @SerializedName("start") val start:List<Int>,
    @SerializedName("end") val end:List<Int>)