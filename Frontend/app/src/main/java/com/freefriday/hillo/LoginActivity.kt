package com.freefriday.hillo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.kakao.auth.ISessionCallback
import com.kakao.auth.Session
import com.kakao.friends.AppFriendContext
import com.kakao.friends.AppFriendOrder
import com.kakao.friends.response.AppFriendsResponse
import com.kakao.kakaotalk.callback.TalkResponseCallback
import com.kakao.kakaotalk.v2.KakaoTalkService
import com.kakao.network.ErrorResult
import com.kakao.network.callback.ResponseCallback
import com.kakao.usermgmt.UserManagement
import com.kakao.usermgmt.callback.MeV2ResponseCallback
import com.kakao.usermgmt.response.MeV2Response
import com.kakao.util.exception.KakaoException

class LoginActivity : AppCompatActivity() {
    val ResponseCallback = object: MeV2ResponseCallback() {

        override fun onSuccess(result: MeV2Response?) {
            Log.i("DEBUGMSG","response success")
            Log.i("DEBUGMSG", "id:" +result?.id.toString())
            Log.i("DEBUGMSG","name: "+result?.kakaoAccount?.profile?.nickname)
        }

        override fun onSessionClosed(errorResult: ErrorResult?) {
            Log.i("DEBUGMSG","Session closing")
        }

    }
    val sessionCallback = object : ISessionCallback {
        override fun onSessionOpenFailed(exception: KakaoException?) {
            Log.i("DEBUGMSG", "Login Failed")
            Log.i("DEBUGMSG", "onSessionOpenFailed: "+exception)
        }

        override fun onSessionOpened() {
            Log.i("DEBUGMSG", "Login Success")
            getFriendlist()
        }

    }
    val getFriendlist = {
        val context = AppFriendContext(AppFriendOrder.NICKNAME, 0, 100, "asc")
        val friendresponse = object : TalkResponseCallback<AppFriendsResponse>(){
            override fun onSuccess(result: AppFriendsResponse?) {
                Log.i("DEBUGMSG", "get friend success")
                result?.friends!!.forEach{
                    Log.i("DEBUGMSG", it.profileNickname)
                    insertFriend(applicationContext, Friend(it.id, it.profileNickname, it.profileThumbnailImage), {})
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Session.getCurrentSession().addCallback(sessionCallback)
        if(Session.getCurrentSession().checkAndImplicitOpen()){
            finish()
        }

        val btn_load = findViewById<Button>(R.id.btn_load)
        btn_load.setOnClickListener {
            UserManagement.getInstance().me(ResponseCallback)

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Session.getCurrentSession().removeCallback(sessionCallback)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i("DEBUGMSG", "Result Returned")
        if(Session.getCurrentSession().handleActivityResult(requestCode,resultCode,data)){
            return
        }
        super.onActivityResult(requestCode, resultCode, data)

    }
}