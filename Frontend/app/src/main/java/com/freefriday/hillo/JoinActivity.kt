package com.freefriday.hillo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class JoinActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join)
        val session = intent.data?.getQueryParameter("session")?.toLong()

        val text_status = findViewById<TextView>(R.id.text_status)
        val btn_join = findViewById<Button>(R.id.btn_join)
        btn_join.setOnClickListener {
            RetrofitObj.getinst().joinsession(myid, session).enqueue(CallBackClass({
                text_status.text = "Connected"
            }))
        }
    }
}