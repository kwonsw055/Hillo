package com.freefriday.hillo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
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
import retrofit2.http.POST
import retrofit2.http.Query

val timeN = 48
val baseURL = "http://10.0.2.2:5000"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn_my = findViewById<Button>(R.id.btn_my)
        val btn_frn = findViewById<Button>(R.id.btn_frn)
        btn_my.setOnClickListener {
            val startintent = Intent(applicationContext, LoginActivity::class.java)
            startActivity(startintent)
        }
        btn_frn.setOnClickListener { startActivity(Intent(applicationContext, addtable::class.java)) }
        val inst = object: KakaoAdapter(){
            override fun getApplicationConfig(): IApplicationConfig {
                return IApplicationConfig {
                    applicationContext
                }
            }
        }
        KakaoSDK.init(inst)
        UserManagement.getInstance().me(ResponseClass())
    }

}
class CallBackClass:Callback<String>{
    override fun onFailure(call: Call<String>, t: Throwable) {
        Log.i("DEBUGMSG", "mytest failed: "+t.message)
    }

    override fun onResponse(call: Call<String>, response: Response<String>) {
        if(response.isSuccessful){
            Log.i("DEBUGMSG", "rsp="+response.body())
        }
        else{
            Log.i("DEBUGMSG", "response failed: "+response.code()+": "+response.errorBody())
        }
    }
}

class ResponseClass: MeV2ResponseCallback() {
    override fun onSuccess(result: MeV2Response?) {

        Log.i("DEBUGMSG","response success")
        Log.i("DEBUGMSG", "id:" +result?.id.toString())
        Log.i("DEBUGMSG","name: "+result?.kakaoAccount?.profile?.nickname)

        val retrof = Retrofit.Builder().baseUrl(baseURL).addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create()).build()
        val rstarter = retrof.create(Rinter::class.java)
        rstarter.gettest(result?.id,result?.kakaoAccount?.profile?.nickname).enqueue(CallBackClass())
    }

    override fun onSessionClosed(errorResult: ErrorResult?) {
        Log.i("DEBUGMSG","Session closing")
    }
}

interface Rinter{
    @POST("test-q")
    fun gettest(@Query("id") id: Long?, @Query("name") name:String?): Call<String>
}

