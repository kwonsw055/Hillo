package com.freefriday.hillo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.browser.browseractions.BrowserActionsIntent
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.kakao.auth.*
import com.kakao.network.ErrorResult
import com.kakao.usermgmt.UserManagement
import com.kakao.usermgmt.callback.MeV2ResponseCallback
import com.kakao.usermgmt.response.MeV2Response
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
import java.lang.StringBuilder

val timeN = 48
val baseURL = "http://10.0.2.2:5000"
var myid:Long? = null
val rvadapter: RVAdapter by lazy{ RVAdapter(null)}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn_rec = findViewById<Button>(R.id.btn_rec)
        val btn_my = findViewById<Button>(R.id.btn_my)
        val btn_frn = findViewById<Button>(R.id.btn_frn)
        val main_frame = findViewById<FrameLayout>(R.id.main_frame)
        val frag = main_frame.findViewById<View>(R.id.rec_frag)

        //rvadapter = RVAdapter(null)
        //val recycler = LayoutInflater.from(applicationContext).inflate(R.layout.fragment_rec, main_frame) as RecyclerView
        val changeFrag = {
            frag : Fragment->
            val transaction = supportFragmentManager?.beginTransaction()
            transaction.replace(R.id.rec_frag, frag)
            transaction.commit()
        }
        btn_rec.setOnClickListener {
            changeFrag(RecFrag())
        }
        btn_my.setOnClickListener {
            changeFrag(TimeFrag())
        }
        //btn_frn.setOnClickListener { startActivity(Intent(applicationContext, addtable::class.java)) }
        val inst = object: KakaoAdapter(){
            override fun getApplicationConfig(): IApplicationConfig {
                return IApplicationConfig {
                    applicationContext
                }
            }
        }
        try{
            KakaoSDK.init(inst)
        }catch (e:KakaoSDK.AlreadyInitializedException){

        }
        UserManagement.getInstance().me(ResponseClass())

    }

}

inline fun <reified T> getJsonParse(response: Response<String>) = GsonBuilder().create().fromJson(response.body(), T::class.java)

class CallBackClass:Callback<String>{
    lateinit var afterSuccess: (Response<String>)->Unit
    var afterFailure:(Throwable)->Unit = {}
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
        return "$h:${"%02d".format(m)} $pm"
    }
    override fun toString(): String {
        return "$day ${timeformat(start)}~${timeformat(end)}"
    }
}

data class FreetimeJson(
    @SerializedName("fid") val fid:Long,
    @SerializedName("times") val times:Array<Timepair>
){
    override fun toString(): String {
        val str = StringBuilder()
        str.append("fid=$fid\n")
        times.forEach {
            str.append(it.toString())
            str.append("\n")
        }
        return str.toString()
    }
    fun toFreetime():Array<Freetime>{
        val res = mutableListOf<Freetime>()
        times.forEach {
            res.add(Freetime(fid, it))
        }
        return res.toTypedArray()
    }
}

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
    fun toFreetime():Array<Freetime>{
        val res = mutableListOf<Freetime>()
        result.forEach {
            it.toFreetime().forEach { t->res.add(t) }
        }
        return res.toTypedArray()
    }
}

data class Freetime(
    val id: Long,
    val time: Timepair
)

class ResponseClass: MeV2ResponseCallback() {
    override fun onSuccess(result: MeV2Response?) {

        Log.i("DEBUGMSG","response success")
        Log.i("DEBUGMSG", "id:" +result?.id.toString())
        Log.i("DEBUGMSG","name: "+result?.kakaoAccount?.profile?.nickname)

        myid = result?.id
        val doparse : (Response<String>)->Unit = {
            val parsed = getJsonParse<FreetimeArray>(it)
            rvadapter.data = parsed.toFreetime()
            rvadapter.notifyDataSetChanged()
        }
        RetrofitObj.getinst().gettest(result?.id,result?.kakaoAccount?.profile?.nickname).enqueue(CallBackClass(
        doparse))
        RetrofitObj.getinst().getfreetime(myid).enqueue(CallBackClass(doparse))
    }

    override fun onSessionClosed(errorResult: ErrorResult?) {
        Log.i("DEBUGMSG","Session closing")
    }
}

class RetrofitObj{
    companion object{
        private var rInstance:Rinter? = null
        fun getinst():Rinter{
            if(rInstance==null){
                synchronized(this){
                    rInstance = Retrofit.Builder().baseUrl(baseURL).addConverterFactory(ScalarsConverterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create()).build().create(Rinter::class.java)
                }
            }
            return rInstance!!
        }
    }
}

interface Rinter{
    @POST("test-q")
    fun gettest(@Query("id") id: Long?, @Query("name") name:String?): Call<String>
    @POST("test-sett")
    fun settime(@Body body:TimeList):Call<String>
    @GET("test-getft")
    fun getfreetime(@Query("id") id:Long?): Call<String>
}

data class TimeList(
    @SerializedName("id") val id:Long,
    @SerializedName("day") val day:List<Int>,
    @SerializedName("start") val start:List<Int>,
    @SerializedName("end") val end:List<Int>)