package com.mgt.quiz_assistant.pages

import android.view.View
import com.mgt.quiz_assistant.MainPagerAdapter
import com.mgt.quiz_assistant.R
import kotlinx.android.synthetic.main.page_intro.view.*

class PageAreaHolder(itemView: View) : MainPagerAdapter.BindHolder(itemView) {
    override fun bind() {
        itemView.apply {
            imageView.setImageResource(R.drawable.choose_area)
            h1DescTextView.text = context.getString(R.string.choose_area_title)
            h2DescTextView.text = context.getString(R.string.choose_area_desc)
        }
    }
}