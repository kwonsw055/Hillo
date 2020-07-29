package com.freefriday.hillo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class JoinActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join)

        //Get session number from intent parameter
        val session = intent.data?.getQueryParameter("session")?.toLong()

        //Text View for showing status
        val text_status = findViewById<TextView>(R.id.text_status)

        //Button for joining session
        val btn_join = findViewById<Button>(R.id.btn_join)

        btn_join.setOnClickListener {
            RetrofitObj.getinst().joinsession(myid, session).enqueue(CallBackClass({
                text_status.text = "Connected"
            }))
        }
    }
}