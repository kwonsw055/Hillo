//Schema of Friend DB
package com.freefriday.hillo

import android.content.Context
import androidx.room.*

//Friend table name and attributes
const val FriendTableName = "friendtable"
const val FriendID = "id"
const val FriendNickname = "nickname"
const val FriendURL = "url"

//DB file name
const val FriendDBName = "friendtable.db"
const val FriendDBVersion = 1

//Entity for friend table
@Entity(tableName = FriendTableName)
class Friend(
    @PrimaryKey @ColumnInfo(name = FriendID) var id: Long,
    @ColumnInfo(name = FriendNickname)var nickname: String,
    @ColumnInfo(name = FriendURL)var url:String
)

//DAO for friend table
@Dao
interface FriendDAO{
    @Insert
    fun insertTable(friendtable: Friend)
    @Delete
    fun deleteTable(friendtable: Friend)
    @Query("select * from "+ FriendTableName)
    fun getallTable():List<Friend>
    @Query("delete from "+ FriendTableName)
    fun deleteAll()
    @Query("select * from $FriendTableName where $FriendID=:id")
    fun get(id:Long):List<Friend>
}

//Database for friend table
@Database(entities = [Friend::class], version= FriendDBVersion)
@TypeConverters(DateConverter::class)
abstract class FriendDB: RoomDatabase(){
    abstract fun FriendDAO(): FriendDAO
    companion object{
        var instance: FriendDB? = null
        fun getinst(context: Context):FriendDB?{
            if(instance==null){
                synchronized(FriendDB::class){
                    instance = Room.databaseBuilder(context, FriendDB::class.java,
                        FriendDBName).fallbackToDestructiveMigration().build()
                }
            }
            return instance
        }
    }
}

//Do select all query.
//Returns value to friendtablelist.
//friendtablelist will be cleared, then used.
fun getallFriends(context: Context, friendtablelist:MutableList<Friend>, afterexec:()->Unit){
    Thread{
        friendtablelist.clear()
        val queriedlist= FriendDB.getinst(context)?.FriendDAO()?.getallTable()
        queriedlist?.forEach {
            friendtablelist.add(it)
        }
        afterexec()
    }.start()
}

//Do select with id query.
fun getFriend(context: Context, id:Long, afterexec: (Friend?) -> Unit){
    Thread{
        val res = FriendDB.getinst(context)?.FriendDAO()?.get(id)
        afterexec(if(res?.size!!>0){res?.get(0)}else{null})
    }.start()
}

//Insert friend table.
fun insertFriend(context: Context, friendtable:Friend, afterexec:()->Unit){
    Thread{
        FriendDB.getinst(context)?.FriendDAO()?.insertTable(friendtable)
        afterexec()
    }.start()
}

//Delete friend table
fun deleteFriend(context: Context, friendtable: Friend, afterexec: () -> Unit){
    Thread{
        FriendDB.getinst(context)?.FriendDAO()?.deleteTable(friendtable)
        afterexec()
    }.start()
}

//Delete all friends
fun deleteAllFriends(context: Context, afterexec: () -> Unit){
    Thread{
        FriendDB.getinst(context)?.FriendDAO()?.deleteAll()
        afterexec()
    }.start()
}