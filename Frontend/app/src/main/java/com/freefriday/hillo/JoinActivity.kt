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

//Server Socket
val serverSocket : ServerSocket by lazy{ServerSocket(tcpPort)}

//Is network thread started?
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

        //Recycler View for result
        val rv_join = findViewById<RecyclerView>(R.id.rv_join)

        //Result data
        var rvdata = mutableListOf<TimeTable>()

        //RV Adapter for Recycler View
        val rvadapter = JoinRVAdapter(rvdata)

        //Initialize Recycler View
        rv_join.adapter = rvadapter
        rvadapter.data = rvdata
        rv_join.layoutManager = LinearLayoutManager(this)
        rv_join.setHasFixedSize(true)

        //Start socket Thread
        if(!netThreaded){
            Thread{
                //Keep listening
                while(true){
                    Log.i("DEBUGMSG", "socket init")
                    try{
                        //accepted socket
                        val socket = serverSocket.accept()

                        //input stream for socket
                        val inputstream = socket.getInputStream()

                        //buffered reader of input stream
                        val bufferedreader = BufferedReader(InputStreamReader(inputstream))

                        //result string
                        //used when session ends
                        var str = ""

                        //is str value being written?
                        var strWrite = false

                        //read value
                        while(inputstream.available()>0){

                            //read data
                            val data = bufferedreader.readLine()

                            Log.i("DEBUGMSG", "data = "+data.toString())

                            //check data type
                            try{
                                //Try parsing to int
                                //Check if data is joined user count
                                data.toInt()

                                Log.i("DEBUGMSG", "Is int")

                                //Change joincount text
                                this.runOnUiThread {
                                    text_joincount.text = getString(R.string.status_count, data.toInt())
                                }

                            }catch (e: Exception){
                                //If not int type, then data is the result for available times
                                Log.i("DEBUGMSG", "Not int")

                                str = data

                                strWrite = true

                                //Parse received data to TimeTable list
                                rvadapter.data = parseSocketResponse(data)

                                //Change UI to voting stage
                                this.runOnUiThread {
                                    rvadapter.notifyItemRangeChanged(0, rvadapter.itemCount-1)

                                    btn_end.visibility = View.GONE
                                    text_joincount.text = getString(R.string.status_vote)
                                    text_status.visibility = View.GONE
                                }
                            }
                        }
                        socket.close()
                        //If result was received, exit thread
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

            //Network thread has started
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

            //Get myid if null
            if(myid == null) UserManagement.getInstance().me(KakaoResponseClass())

            //Wait until myid is valid
            while(myid == null);

            //Check if session was parsed correctly
            if(session == null){
                //If not, try parsing again
                session = intent.data?.getQueryParameter("session")?.toLong()

                //If still null, ask user to try again
                if(session == null){
                    Toast.makeText(this, R.string.toast_join_error, Toast.LENGTH_SHORT).show()
                }
            }

            //Join session via http
            RetrofitObj.getinst().joinsession(myid, session).enqueue(CallBackClass({
                //Check if user is the leader
                if(it.body()=="Leader"){
                    //Set UI to leader stage
                    btn_join.visibility = View.INVISIBLE
                    btn_end.visibility = View.VISIBLE
                    text_status.text = getString(R.string.status_leader)
                }
                //If not, set to joined stage
                else{
                    text_status.text = getString(R.string.status_joined)
                }
            }).addAfterFailure {
                //If session is ended
                Toast.makeText(this, getString(R.string.toast_session_ended), Toast.LENGTH_SHORT).show()
                finish()
            }.addAfterNoConnection {
                //If server is down
                Toast.makeText(this, getString(R.string.toast_server_down), Toast.LENGTH_SHORT).show()
                finish()
            })
        }

        //End session when button clicked
        btn_end.setOnClickListener {
            RetrofitObj.getinst().endsession(session).enqueue(CallBackClass{})
        }
    }
}

//Convert received string to TimeTable list
fun parseSocketResponse(str: String): MutableList<TimeTable>{

    //Results
    var result = mutableListOf<TimeTable>()

    //Input example: [('MON', 1200, 1600), ('TUE', 1400, 2230)]
    var matches = """'(...)', (\d{4}), (\d{4})""".toRegex().find(str)

    while(matches != null){
        //Deconstruct matched groups
        var(day, start, end) = matches.destructured

        //Add to results and move on
        result.add(TimeTable(null, str2date(day)!!, start.toInt(), end.toInt()))
        matches = matches.next()
    }

    //Return results
    return result
}