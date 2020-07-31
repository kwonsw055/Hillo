package com.freefriday.hillo

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kakao.auth.IApplicationConfig
import com.kakao.auth.KakaoAdapter
import com.kakao.auth.KakaoSDK
import com.kakao.usermgmt.UserManagement
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

val serverSocket : ServerSocket by lazy{ServerSocket(tcpPort)}
var netThreaded = false

class JoinActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join)

        //Get session number from intent parameter
        var session = intent.data?.getQueryParameter("session")?.toLong()
        Log.i("DEBUGMSG", "session="+session)

        //Text View for showing status
        val text_status = findViewById<TextView>(R.id.text_status)

        val text_joincount = findViewById<TextView>(R.id.text_joincount)

        //Button for joining session
        val btn_join = findViewById<Button>(R.id.btn_join)

        //Button for ending session
        val btn_end = findViewById<Button>(R.id.btn_end)

        //Recycler View for
        val rv_join = findViewById<RecyclerView>(R.id.rv_join)
        var rvdata = mutableListOf<TimeTable>()
        val rvadapter = JoinRVAdapter(rvdata)
        rv_join.adapter = rvadapter
        rvadapter.data = rvdata
        rv_join.layoutManager = LinearLayoutManager(this)
        rv_join.setHasFixedSize(true)

        if(!netThreaded){
            Thread{
                while(true){
                    Log.i("DEBUGMSG", "socket init")
                    try{
                        val socket = serverSocket.accept()
                        val inputstream = socket.getInputStream()
                        val bufferedreader = BufferedReader(InputStreamReader(inputstream))
                        var str = ""
                        var strWrite = false
                        while(inputstream.available()>0){
                            val data = bufferedreader.readLine()
                            Log.i("DEBUGMSG", "data = "+data.toString())
                            try{
                                data.toInt()
                                Log.i("DEBUGMSG", "Is int")
                                this.runOnUiThread {
                                    text_joincount.text = getString(R.string.status_count, data.toInt())
                                }
                            }catch (e: Exception){
                                Log.i("DEBUGMSG", "Not int")
                                str = data
                                strWrite = true
                                rvadapter.data = parseSocketResponse(data)
                                this.runOnUiThread {
                                    rvadapter.notifyItemRangeChanged(0, rvadapter.itemCount-1)
                                    btn_end.visibility = View.GONE
                                    text_joincount.text = getString(R.string.status_vote)
                                    text_status.visibility = View.GONE
                                }
                            }
                        }
                        socket.close()
                        if(strWrite){
                            Log.i("DEBUGMSG", "str = "+str)
                            break
                        }
                    }
                    catch (e:SocketTimeoutException){
                        Log.i("DEBUGMSG", "socket timed out")
                    }
                    catch (e: SocketException){
                        Log.i("DEBUGMSG", "socket closed")
                    }
                }
            }.start()
            netThreaded = true
        }
        if(netThreaded){
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
            }catch (e: KakaoSDK.AlreadyInitializedException){
            }


            if(myid == null) UserManagement.getInstance().me(KakaoResponseClass())
            while(myid == null);
            if(session == null){
                session = intent.data?.getQueryParameter("session")?.toLong()
                if(session == null){
                    Toast.makeText(this, R.string.toast_join_error, Toast.LENGTH_SHORT).show()
                }
            }
            RetrofitObj.getinst().joinsession(myid, session).enqueue(CallBackClass({
                if(it.body()=="Leader"){
                    btn_join.visibility = View.INVISIBLE
                    btn_end.visibility = View.VISIBLE
                    text_status.text = getString(R.string.status_leader)
                }
                else{
                    text_status.text = getString(R.string.status_joined)
                }
            }).addAfterFailure {
                Toast.makeText(this, getString(R.string.toast_session_ended), Toast.LENGTH_SHORT).show()
                finish()
            }.addAfterNoConnection {
                Toast.makeText(this, getString(R.string.toast_server_down), Toast.LENGTH_SHORT).show()
                finish()
            })
        }

        btn_end.setOnClickListener {
            RetrofitObj.getinst().endsession(session).enqueue(CallBackClass{})
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

fun parseSocketResponse(str: String): MutableList<TimeTable>{
    var result = mutableListOf<TimeTable>()
    var matches = """'(...)', (\d{4}), (\d{4})""".toRegex().find(str)
    while(matches != null){
        var(day, start, end) = matches.destructured
        result.add(TimeTable(null, str2date(day)!!, start.toInt(), end.toInt()))
        matches = matches.next()
    }
    return result
}