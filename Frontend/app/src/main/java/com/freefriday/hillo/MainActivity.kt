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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.kakao.auth.*
import com.kakao.friends.AppFriendContext
import com.kakao.friends.AppFriendOrder
import com.kakao.friends.response.AppFriendsResponse
import com.kakao.kakaolink.v2.KakaoLinkService
import com.kakao.kakaotalk.callback.TalkResponseCallback
import com.kakao.kakaotalk.v2.KakaoTalkService
import com.kakao.message.template.ButtonObject
import com.kakao.message.template.LinkObject
import com.kakao.message.template.TextTemplate
import com.kakao.network.ErrorResult
import com.kakao.usermgmt.UserManagement
import com.kakao.usermgmt.callback.MeV2ResponseCallback
import com.kakao.usermgmt.response.MeV2Response
import com.kakao.util.exception.KakaoException
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Response

//HTTP URL for server
val baseIP = "34.64.150.182"
val basePort = 5000
val tcpPort = 7000
val baseURL = "http://${baseIP}:${basePort}"

//User kakao id
var myid:Long? = null

//Recycler View Adapter used for recommendation fragment
val recrvadapter: FreetimeRVAdapter by lazy{ FreetimeRVAdapter(null)}

//Recycler View Adapter used for time table fragment
val timervadapter : TimetableRVAdapter by lazy{ TimetableRVAdapter(null)}

//lambda for parsing free time
val doparse : (Response<String>)->Unit = {
    val parsed = getJsonParse<FreetimeArray>(it)
    recrvadapter.data = parsed.toFreetime()
    Log.i("DEBUGMSG", "data:")
    Log.i("DEBUGMSG", recrvadapter.data.toString())
    recrvadapter.notifyDataSetChanged()
}

//Overlapping view for login button
var loginview : View? = null

//Layout of main constraint layout
lateinit var main : ConstraintLayout

//Layout inflater
lateinit var lInflater: LayoutInflater

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

        //Button for option list
        val btn_opt = findViewById<Button>(R.id.btn_opt)

        //Frame layout for main frame
        //Contains Fragment
        val main_frame = findViewById<FrameLayout>(R.id.main_frame)

        //initialize main constraint layout
        main = findViewById<ConstraintLayout>(R.id.cl)

        //initialize layoutinflater
        lInflater = layoutInflater

        //Text View for title string
        val title = findViewById<TextView>(R.id.title_text)

        //Lambda function for switching fragments
        //Not using backstack.
        val changeFrag = {
            frag : Fragment, titlestr:String->
            val transaction = supportFragmentManager?.beginTransaction()
            transaction.replace(R.id.rec_frag, frag)
            transaction.commit()
            title.text = titlestr
        }

        val recfrag: RecFrag by lazy{RecFrag()}
        //Set recommendation button to switch fragments
        btn_rec.setOnClickListener {
            changeFrag(recfrag, getString(R.string.title_rec))
        }

        val timefrag: TimeFrag by lazy { TimeFrag() }
        //Set my time table button to switch fragments
        btn_my.setOnClickListener {
            changeFrag(timefrag, getString(R.string.title_time))
        }

        val makeFrag: MakeFrag by lazy{MakeFrag()}
        //Set make appointment button to switch fragments
        btn_frn.setOnClickListener {
            changeFrag(makeFrag, getString(R.string.title_make))
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

        //Try getting my info
        UserManagement.getInstance().me(KakaoResponseClass().addAfterSessionClosed {
            //Show login button to re open session
            loginview = lInflater.inflate(R.layout.activity_login, main, false)
            loginview?.setOnTouchListener { v, event ->  true}
            main.addView(loginview)
        }.addAfterSuccess {
            RetrofitObj.getinst().gettest(it?.id, it?.kakaoAccount?.profile?.nickname).enqueue(CallBackClass{})
        })


        val getFriendlist = {
            val context = AppFriendContext(AppFriendOrder.NICKNAME, 0, 100, "asc")
            val friendresponse = object : TalkResponseCallback<AppFriendsResponse>(){
                override fun onSuccess(result: AppFriendsResponse?) {
                    Log.i("DEBUGMSG", "get friend success")
                    result?.friends!!.forEach{
                        Log.i("DEBUGMSG", it.profileNickname)
                        insertFriend(applicationContext, Friend(it.id, it.profileNickname, it.profileThumbnailImage), {})
                        RetrofitObj.getinst().setfriend(myid, it.id).enqueue(CallBackClass{})
                    }
                }

                override fun onNotKakaoTalkUser() {
                }

                override fun onSessionClosed(errorResult: ErrorResult?) {
                    Log.i("DEBUGMSG", "onSessionClosed: "+errorResult!!.errorMessage)
                }

                override fun onFailure(errorResult: ErrorResult?) {
                    Log.i("DEBUGMSG", "onFailure: "+errorResult!!.errorMessage)
                }
            }

            KakaoTalkService.getInstance().requestAppFriends(context, friendresponse)
        }

        val sessionCallback = object : ISessionCallback {
            override fun onSessionOpenFailed(exception: KakaoException?) {
                Log.i("DEBUGMSG", "Login Failed")
                Log.i("DEBUGMSG", "onSessionOpenFailed: "+exception)
            }

            override fun onSessionOpened() {
                Log.i("DEBUGMSG", "Login Success")
                UserManagement.getInstance().me(KakaoResponseClass())
                getFriendlist()
            }
        }

        Session.getCurrentSession().addCallback(sessionCallback)
    }

    override fun onBackPressed() {
        //When login button is shown, intercept backpress
        if(loginview == null) super.onBackPressed()
        else{
            main.removeView(loginview)
            loginview = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //When login is done
        Log.i("DEBUGMSG", "Result Returned")
        if(Session.getCurrentSession().handleActivityResult(requestCode,resultCode,data)){
            //Delete login button
            main.removeView(loginview)
            loginview = null
            //Retry getting my info
            UserManagement.getInstance().me(KakaoResponseClass().addAfterSessionClosed {
                //Show login button to re open session
                loginview = lInflater.inflate(R.layout.activity_login, main, false)
                loginview?.setOnTouchListener { v, event ->  true}
                main.addView(loginview)
            }.addAfterSuccess {
                RetrofitObj.getinst().gettest(it?.id, it?.kakaoAccount?.profile?.nickname).enqueue(CallBackClass{})
            })
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

//Class for retrieving my info
class KakaoResponseClass : MeV2ResponseCallback(){

    var afterSessionClosed : (ErrorResult?)->Unit = {}
    var afterSuccess : (MeV2Response?)->Unit = {}

    fun addAfterSessionClosed(after: (ErrorResult?)->Unit):KakaoResponseClass{
        afterSessionClosed = after
        return this
    }

    fun addAfterSuccess(after: (MeV2Response?)->Unit):KakaoResponseClass {
        afterSuccess = after
        return this
    }

    override fun onSuccess(result: MeV2Response?) {

        Log.i("DEBUGMSG","response success")
        Log.i("DEBUGMSG", "id:" +result?.id.toString())
        Log.i("DEBUGMSG","name: "+result?.kakaoAccount?.profile?.nickname)

        //initialize myid
        myid = result?.id

        afterSuccess(result)
    }

    override fun onSessionClosed(errorResult: ErrorResult?) {
        Log.i("DEBUGMSG","Session closed")

        afterSessionClosed(errorResult)
    }
}