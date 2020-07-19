//Schema of Time Table DB
package com.freefriday.hillo

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.google.gson.internal.bind.DateTypeAdapter
import java.io.File

//Time table name and attributes
const val TimeTableName = "timetable"
const val TimeTableDay = "day"
const val TimeTableStart = "start"
const val TimeTableEnd = "end"

//DB file name
const val TimeTableDBName = "timetable.db"
const val TimeTableDBVersion = 2

//Entity for time table
@Entity(tableName = TimeTableName)
class TimeTable(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = TimeTableDay)var day: Date,
    @ColumnInfo(name = TimeTableStart)var start:Int,
    @ColumnInfo(name = TimeTableEnd)var end:Int
)

//DAO for time table
@Dao
interface TimeTableDAO{
    @Insert
    fun insertTable(timetable: TimeTable)
    @Delete
    fun deleteTable(timetable: TimeTable)
    @Query("select * from "+ TimeTableName)
    fun getallTable():List<TimeTable>
    @Query("delete from "+ TimeTableName)
    fun deleteAll()
}

//Database for time table
@Database(entities = [TimeTable::class], version= TimeTableDBVersion)
@TypeConverters(DateConverter::class)
abstract class TimeTableDB:RoomDatabase(){
    abstract fun TimeTableDAO(): TimeTableDAO
    companion object{
        var instance: TimeTableDB? = null
        fun getinst(context: Context):TimeTableDB?{
            if(instance==null){
                synchronized(TimeTableDB::class){
                    instance = Room.databaseBuilder(context, TimeTableDB::class.java,
                        TimeTableDBName).fallbackToDestructiveMigration().build()
                }
            }
            return instance
        }
    }
}

//Date type for time table
enum class Date(val str: String, val num: Int){
    mon("MON", 0),
    tue("TUE", 1),
    wed("WED", 2),
    thu("THU", 3),
    fri("FRI", 4),
    sat("SAT", 5),
    sun("SUN", 6);
}

//Date type converter
class DateConverter{
    @TypeConverter
    fun date2str(date:Date)=date.str
    @TypeConverter
    fun strTodate(str:String)= str2date(str)
}

//Convert int to date type
fun int2date(i:Int):Date?= when(i){
        0-> Date.mon
        1-> Date.tue
        2-> Date.wed
        3-> Date.thu
        4-> Date.fri
        5-> Date.sat
        6-> Date.sun
        else->null
    }

//Convert string to date type
fun str2date(str:String):Date?=when(str){
    "MON"-> Date.mon
    "TUE"-> Date.tue
    "WED"-> Date.wed
    "THU"-> Date.thu
    "FRI"-> Date.fri
    "SAT"-> Date.sat
    "SUN"-> Date.sun
    else->null
}

//Do select all query.
//Returns value to timetablelist.
//timetablelist will be cleared, then used.
fun getallTable(context:Context,timetablelist:MutableList<TimeTable>, afterexec:()->Unit){
    Thread{
        timetablelist.clear()
        val queriedlist= TimeTableDB.getinst(context)?.TimeTableDAO()?.getallTable()
        queriedlist?.forEach {
            timetablelist.add(it)
        }
        afterexec()
    }.start()
}

//Insert time table.
fun insertTable(context:Context, timetable:TimeTable, afterexec:()->Unit){
    Thread{
        TimeTableDB.getinst(context)?.TimeTableDAO()?.insertTable(timetable)
        afterexec()
    }.start()
}

//Delete time table
fun deleteTable(context: Context, timetable: TimeTable, afterexec: () -> Unit){
    Thread{
        TimeTableDB.getinst(context)?.TimeTableDAO()?.deleteTable(timetable)
        afterexec()
    }.start()
}

//Delete all
fun deleteAll(context: Context, afterexec: () -> Unit){
    Thread{
        TimeTableDB.getinst(context)?.TimeTableDAO()?.deleteAll()
        afterexec()
    }.start()
}