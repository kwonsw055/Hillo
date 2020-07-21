//Fragment for recommendation
package com.freefriday.hillo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import retrofit2.Response

class RecFrag : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //set recycler view
        val constlayout = inflater.inflate(R.layout.fragment_rec, container, false) as ConstraintLayout
        val recycler = constlayout.findViewById<RecyclerView>(R.id.rv_view)
        recycler.setHasFixedSize(true)
        recycler.layoutManager = LinearLayoutManager(inflater.context)
        recycler.adapter = recrvadapter

        //set swipe refresh
        val swipe = constlayout.findViewById<SwipeRefreshLayout>(R.id.swipe_rec)
        val doparseAndstop: (Response<String>)->Unit = {
            doparse(it)
            swipe.isRefreshing = false
        }
        swipe.setOnRefreshListener {
            RetrofitObj.getinst().getfreetime(myid).enqueue(CallBackClass(doparseAndstop))
        }
        return constlayout
    }
}