package com.mgt.quiz_assistant.pages

import android.view.View
import com.mgt.quiz_assistant.MainPagerAdapter
import com.mgt.quiz_assistant.R
import kotlinx.android.synthetic.main.page_intro.view.*

class PageResultHolder(itemView: View) : MainPagerAdapter.BindHolder(itemView) {
    override fun bind() {
        itemView.apply {
            imageView.setImageResource(R.drawable.result)
            h1DescTextView.text = context.getString(R.string.result_title)
            h2DescTextView.text = context.getString(R.string.result_desc)
        }
    }
}