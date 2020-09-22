package com.mgt.kahoot_assistant.pages

import android.view.View
import com.mgt.kahoot_assistant.MainActivity
import com.mgt.kahoot_assistant.MainPagerAdapter
import kotlinx.android.synthetic.main.page_try.view.*

class PageTryHolder(itemView: View): MainPagerAdapter.BindHolder(itemView){
    override fun bind() {
        itemView.apply {
            startButton.setOnClickListener {
                (context as MainActivity).showDialog()
            }
        }
    }
}