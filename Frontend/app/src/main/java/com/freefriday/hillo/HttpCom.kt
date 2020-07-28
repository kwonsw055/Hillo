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
    var afterFailure:(Throwable)->Unit = {}

    //Constructors
    constructor(success: (Response<String>)->Unit){
        afterSuccess = success
    }
    constructor(success: (Response<String>)->Unit, failure: (Throwable)->Unit){
        CallBackClass(success)
        afterFailure = failure
    }

    override fun onFailure(call: Call<String>, t: Throwable) {
        Log.i("DEBUGMSG", "mytest failed: "+t.message)
    }

    override fun onResponse(call: Call<String>, response: Response<String>) {
        if(response.isSuccessful){
            Log.i("DEBUGMSG", "rsp="+response.body())
            afterSuccess(response)
        }
        else{
            Log.i("DEBUGMSG", "response failed: "+response.code()+": "+response.errorBody())
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

    //Get user free time recommendations
    @GET("test-getft")
    fun getfreetime(@Query("id") id:Long?): Call<String>

    //Make Session
    @GET("test-make")
    fun makesession(@Query("id") id:Long?): Call<String>

    //Join Session
    @POST("test-join")
    fun joinsession(@Query("id") id:Long?, @Query("session") session:Long?): Call<String>
}
