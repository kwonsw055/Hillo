//Class for Main Activity
//Will start up with recommendation fragment
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

//HTTP URL for server
val baseURL = "http://10.0.2.2:5000"
//User kakao id
var myid:Long? = null
//Recycler View Adapter used for recommendation fragment
val rvadapter: RVAdapter by lazy{ RVAdapter(null)}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Button for recommendation fragment
        val btn_rec = findViewById<Button>(R.id.btn_rec)
        //Button for my time table fragment
        val btn_my = findViewById<Button>(R.id.btn_my)
        //Button for friend list fragment
        val btn_frn = findViewById<Button>(R.id.btn_frn)
        //Frame layout for main frame
        //Contains Fragment
        val main_frame = findViewById<FrameLayout>(R.id.main_frame)

        //Lambda function for switching fragments
        //Not using backstack.
        val changeFrag = {
            frag : Fragment->
            val transaction = supportFragmentManager?.beginTransaction()
            transaction.replace(R.id.rec_frag, frag)
            transaction.commit()
        }

        //Set recommendation button to switch fragments
        btn_rec.setOnClickListener {
            changeFrag(RecFrag())
        }
        //Set my time table button to switch fragments
        btn_my.setOnClickListener {
            changeFrag(TimeFrag())
        }

        //Instantiated using KakaoAdapter
        //Used for initialization
        val inst = object: KakaoAdapter(){
            override fun getApplicationConfig(): IApplicationConfig {
                return IApplicationConfig {
                    applicationContext
                }
            }
        }

        //Initialize Kakao SDK
        //Using try-catch for re-initializing exceptions
        try{
            KakaoSDK.init(inst)
        }catch (e:KakaoSDK.AlreadyInitializedException){
        }

        //Instantiated for Kakao service response
        val responseclass = object: MeV2ResponseCallback() {
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

        //Get my info
        UserManagement.getInstance().me(responseclass)
    }

}