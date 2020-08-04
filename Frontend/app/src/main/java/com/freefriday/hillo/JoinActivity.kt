package com.freefriday.hillo

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
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

//Max nubmer of stages
const val maxStage = 4

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

        //Vote data
        var votedata = mutableListOf<Int>()

        //RV Adapter for Recycler View
        val rvadapter = JoinRVAdapter(rvdata, votedata, session, this)

        //Initialize Recycler View
        rv_join.adapter = rvadapter
        rvadapter.data = rvdata
        rv_join.layoutManager = LinearLayoutManager(this)
        rv_join.setHasFixedSize(true)

        var nowport = 7000

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
        if(myid == null) UserManagement.getInstance().me(KakaoResponseClass().addAfterSessionClosed {
            //If not login info, finish
            Toast.makeText(this, getString(R.string.toast_no_user), Toast.LENGTH_LONG).show()
            finish()
        })

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

        //Check if user info exists
        RetrofitObj.getinst().checkuser(myid).enqueue(CallBackClass{
            //If user exists

            //Join session via http
            RetrofitObj.getinst().joinsession(myid, session).enqueue(CallBackClass{
                //Check if user is the leader

                //Port number received from http response
                nowport = it.body()?.toInt() ?: 65365

                //If nowport is negative, user is leader
                if(nowport<0){
                    //So, set UI to leader stage
                    btn_join.visibility = View.INVISIBLE
                    btn_end.visibility = View.VISIBLE
                    text_status.text = getString(R.string.status_leader)
                    nowport = -nowport
                }
                //If not, set to joined stage
                else{
                    btn_end.visibility = View.INVISIBLE
                    btn_join.visibility = View.INVISIBLE
                    text_status.text = getString(R.string.status_joined)
                }
                Log.i("DEBUGMSG", "nowport="+nowport)

                //Start socket
                Thread{
                    //current stage of listening
                    //0=Participants count
                    //1=Parse time result
                    //2=Voting count
                    //3=Parse vote result
                    var stage = 0

                    //Number of users in session
                    var userCount = 0

                    //Keep listening
                    Log.i("DEBUGMSG", "socket init")
                    try{
                        //accepted socket
                        val socket = Socket(baseIP, nowport)

                        Log.i("DEBUGMSG", "socket connected")

                        //input stream for socket
                        val inputstream = socket.getInputStream()

                        //buffered reader of input stream
                        val bufferedreader = BufferedReader(InputStreamReader(inputstream))

                        while(true){
                            //read value
                            if(bufferedreader.ready()){

                                //read data
                                val data = bufferedreader.readLine()

                                Log.i("DEBUGMSG", "data = "+data.toString())

                                //check data type
                                try{
                                    //Try parsing to int
                                    //Check if data is joined user count
                                    data.toInt()
                                    if(stage == 0) userCount = data.toInt()

                                    Log.i("DEBUGMSG", "Is int")

                                    //If already parsed result, goto stage 2
                                    if(stage == 1)stage = 2

                                    when(stage){
                                        0->//Change joincount text
                                            this.runOnUiThread {
                                                text_joincount.text = getString(R.string.status_count, userCount)
                                            }

                                        2->//Change votecount text
                                            this.runOnUiThread {
                                                text_joincount.text = getString(R.string.status_vote_count, data.toInt(), userCount)
                                            }
                                    }
                                }catch (e: Exception){
                                    //If not int type, then data is the result for available times
                                    Log.i("DEBUGMSG", "Not int")

                                    //If first time parsing, goto stage 1
                                    if(stage == 0)stage = 1

                                    //If parsing voting result, goto stage 3
                                    if(stage == 2)stage = 3

                                    when(stage){
                                        1-> {
                                            //Parse received data to TimeTable list
                                            rvadapter.data = parseSocketResponse(data)

                                            //Change UI to voting stage
                                            this.runOnUiThread {
                                                rvadapter.notifyItemRangeChanged(
                                                    0,
                                                    rvadapter.itemCount - 1
                                                )

                                                btn_end.visibility = View.GONE
                                                text_joincount.text = getString(R.string.status_vote)
                                                text_status.visibility = View.GONE
                                            }
                                        }
                                        3->{
                                            //Parse voting result to list
                                            rvadapter.vote = parseVoteResponse(data)

                                            //Sort by vote count
                                            rvadapter.data!!.sortByDescending {rvadapter.vote[rvadapter.data!!.indexOf(it)]}

                                            rvadapter.vote.sortByDescending{it}

                                            //Notify Recycler View Adapter
                                            this.runOnUiThread {
                                                rvadapter.notifyDataSetChanged()
                                                text_joincount.text = getString(R.string.status_vote_end)
                                                rv_join.smoothScrollToPosition(0)
                                            }

                                            stage = 4
                                        }
                                    }
                                }//End of try-catch

                            }//End of Buffer ready

                            //If stage is max, exit
                            if(stage == maxStage){
                                Log.i("DEBUGMSG", "exiting, stage = "+stage)
                                break
                            }
                        }//End of while(true)

                        socket.close()
                    }
                    catch (e:SocketTimeoutException){
                        Log.i("DEBUGMSG", "socket timed out")
                    }
                    catch (e: SocketException){
                        Log.i("DEBUGMSG", "socket closed")
                    }
                }.start()

            }.addAfterFailure {
                //If session is ended
                Toast.makeText(this, getString(R.string.toast_session_ended), Toast.LENGTH_SHORT).show()
                finish()
            }.addAfterNoConnection {
                //If server is down
                Toast.makeText(this, getString(R.string.toast_server_down), Toast.LENGTH_SHORT).show()
                finish()
            })

        }.addAfterFailure {
            //If user info not exists
            Toast.makeText(this, getString(R.string.toast_no_user), Toast.LENGTH_LONG).show()
            finish()
        })

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
    var matches = """'(...)', (\d+), (\d+)""".toRegex().find(str)

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

//Convert received string to Int list
fun parseVoteResponse(str: String): MutableList<Int>{

    //Results
    var result = mutableListOf<Int>()

    //Input example: [0, 1, 3, 5]
    var matches = """(\d+)""".toRegex().find(str)

    while(matches != null){
        //Deconstruct matched groups
        val (x) = matches.destructured

        //Add to results and move on
        result.add(x.toInt())
        matches = matches.next()
    }

    //Return results
    return result
}