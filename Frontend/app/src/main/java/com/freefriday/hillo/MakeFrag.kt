package com.freefriday.hillo

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.annotations.SerializedName
import com.kakao.kakaolink.v2.KakaoLinkResponse
import com.kakao.kakaolink.v2.KakaoLinkService
import com.kakao.message.template.ButtonObject
import com.kakao.message.template.LinkObject
import com.kakao.message.template.TextTemplate
import com.kakao.network.ErrorResult
import com.kakao.network.callback.ResponseCallback
import retrofit2.Response

class MakeFrag : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val inflated = inflater.inflate(R.layout.fragment_make, container, false) as ConstraintLayout
        val btn_make = inflated.findViewById<FloatingActionButton>(R.id.btn_make)
        btn_make.setOnClickListener {
            RetrofitObj.getinst().makesession(myid).enqueue(CallBackClass({
                r: Response<String> ->
                val response = getJsonParse<sessionresponse>(r)
                sendMessage(response.session)
            }))
        }
        return inflated
    }
    fun sendMessage(session: Long?){
        if(session != null){
            val tempparm = TextTemplate.newBuilder(getString(R.string.msg_text),LinkObject.newBuilder().setAndroidExecutionParams("session=${session}").build()).build()
            activity.run {
                KakaoLinkService.getInstance().sendDefault(activity, tempparm, msgCallback())
            }
        }
    }
}

class msgCallback : ResponseCallback<KakaoLinkResponse>() {
    override fun onSuccess(result: KakaoLinkResponse?) {
        Log.i("DEBUGMSG", result.toString())
    }

    override fun onFailure(errorResult: ErrorResult?) {
        Log.i("DEBUGMSG", errorResult?.errorMessage)
    }
}

data class sessionresponse(
    @SerializedName("session")
    val session: Long?
)