//Fragment for recommendation
package com.freefriday.hillo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecFrag : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val constlayout = inflater.inflate(R.layout.fragment_rec, container, false) as ConstraintLayout
        val recycler = constlayout.findViewById<RecyclerView>(R.id.rv_view)
        recycler.setHasFixedSize(true)
        recycler.layoutManager = LinearLayoutManager(inflater.context)
        recycler.adapter = recrvadapter
        return constlayout
    }
}