package com.freefriday.hillo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.kakao.auth.ISessionCallback
import com.kakao.auth.Session
import com.kakao.friends.AppFriendContext
import com.kakao.friends.AppFriendOrder
import com.kakao.friends.response.AppFriendsResponse
import com.kakao.kakaotalk.callback.TalkResponseCallback
import com.kakao.kakaotalk.v2.KakaoTalkService
import com.kakao.network.ErrorResult
import com.kakao.usermgmt.UserManagement
import com.kakao.usermgmt.callback.MeV2ResponseCallback
import com.kakao.usermgmt.response.MeV2Response
import com.kakao.util.exception.KakaoException

class LoginActivity : AppCompatActivity() {
    val sessionCallback = object : ISessionCallback {
        override fun onSessionOpenFailed(exception: KakaoException?) {
            if(debug_log) {
                Log.i("DEBUGMSG", "Login Failed")
                Log.i("DEBUGMSG", "onSessionOpenFailed: " + exception)
            }
        }

        override fun onSessionOpened() {
            if(debug_log) Log.i("DEBUGMSG", "Login Success")
            UserManagement.getInstance().me(KakaoResponseClass())
            getFriendlist()
        }
    }
    val getFriendlist = {
        val context = AppFriendContext(AppFriendOrder.NICKNAME, 0, 100, "asc")
        val friendresponse = object : TalkResponseCallback<AppFriendsResponse>(){
            override fun onSuccess(result: AppFriendsResponse?) {
                if(debug_log) Log.i("DEBUGMSG", "get friend success")
                result?.friends!!.forEach{
                    if(debug_log) Log.i("DEBUGMSG", it.profileNickname)
                    insertFriend(applicationContext, Friend(it.id, it.profileNickname, it.profileThumbnailImage, it.uuid), {})
                }
            }

            override fun onNotKakaoTalkUser() {
            }

            override fun onSessionClosed(errorResult: ErrorResult?) {
                if(debug_log) Log.i("DEBUGMSG", "onSessionClosed: "+errorResult!!.errorMessage)
            }

            override fun onFailure(errorResult: ErrorResult?) {
                if(debug_log) Log.i("DEBUGMSG", "onFailure: "+errorResult!!.errorMessage)
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
    }

    override fun onDestroy() {
        super.onDestroy()
        Session.getCurrentSession().removeCallback(sessionCallback)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(debug_log) Log.i("DEBUGMSG", "Result Returned")
        if(Session.getCurrentSession().handleActivityResult(requestCode,resultCode,data)){
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}