//Used for handling http response and request
package com.freefriday.hillo

import android.util.Log
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

//Macro for parsing Json using class T
inline fun <reified T> getJsonParse(response: Response<String>) = GsonBuilder().create().fromJson(response.body(), T::class.java)

//Callback Class for Http Response
class CallBackClass: Callback<String> {

    //Performed if response is successful
    lateinit var afterSuccess: (Response<String>)->Unit

    //Performed if response is a failure
    var afterFailure:(Response<String>)->Unit = {}

    //Performed if connection is a failure
    var afterNoConnection: (Throwable)->Unit = {}

    //Constructors
    constructor(success: (Response<String>)->Unit){
        afterSuccess = success
    }

    //Add after failure in builder style
    fun addAfterFailure(failure: (Response<String>)->Unit):CallBackClass{
        afterFailure = failure
        return this
    }

    //Add after no connection in builder style
    fun addAfterNoConnection(noConnection: (Throwable)->Unit): CallBackClass{
        afterNoConnection = noConnection
        return this
    }

    override fun onFailure(call: Call<String>, t: Throwable) {
        if(debug_log) Log.i("DEBUGMSG", "mytest failed: "+t.message)
        afterNoConnection(t)
    }

    override fun onResponse(call: Call<String>, response: Response<String>) {
        if(response.isSuccessful){
            if(debug_log) Log.i("DEBUGMSG", "rsp="+response.body())
            afterSuccess(response)
        }
        else{
            if(debug_log) Log.i("DEBUGMSG", "response failed: "+response.code()+": "+response.errorBody()?.string())
            afterFailure(response)
        }
    }
}

//Retrofit Singleton Object
//Use getinst() to get instance
class RetrofitObj{
    companion object{
        private var rInstance:Rinter? = null
        fun getinst():Rinter{
            if(rInstance==null){
                synchronized(this){
                    rInstance = Retrofit.Builder().baseUrl(baseURL).addConverterFactory(
                        ScalarsConverterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create()).build().create(Rinter::class.java)
                }
            }
            return rInstance!!
        }
    }
}

//Interface for Retrofit object building
interface Rinter{
    //Post user info
    @POST("test-q")
    fun gettest(@Query("id") id: Long?, @Query("name") name:String?): Call<String>

    //Post user free time
    @POST("test-sett")
    fun settime(@Body body:TimeList):Call<String>

    //Set friend relation
    @POST("test-setf")
    fun setfriend(@Query("id") id:Long?, @Query("fid") fid:Long?): Call<String>

    //Get user free time recommendations
    @GET("test-getft")
    fun getfreetime(@Query("id") id:Long?): Call<String>

    //Make Session
    @GET("test-make")
    fun makesession(@Query("id") id:Long?): Call<String>

    //Join Session
    @POST("test-join")
    fun joinsession(@Query("id") id:Long?, @Query("session") session:Long?): Call<String>

    //End session
    @POST("test-end")
    fun endsession(@Query("session") session:Long?): Call<String>

    //Check user
    @GET("test-check")
    fun checkuser(@Query("id") id:Long?): Call<String>

    //Vote
    @POST("test-vote")
    fun votesession(@Query("session") session:Long?, @Query("item") item:Int?): Call<String>

    //Post temp time
    @POST("test-settt")
    fun settemptime(@Query("session") session:Long?, @Body body:TimeList):Call<String>

    //Remove rec
    @POST("test-rmrec")
    fun removerec(@Query("id") id:Long?, @Query("fid") fid:Long?, @Query("time") time:Int?): Call<String>
}
