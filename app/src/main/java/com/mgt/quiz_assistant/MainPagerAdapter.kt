package com.mgt.quiz_assistant

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.mgt.quiz_assistant.pages.PageAreaHolder
import com.mgt.quiz_assistant.pages.PageResultHolder
import com.mgt.quiz_assistant.pages.PageSearchHolder
import com.mgt.quiz_assistant.pages.PageTryHolder

class MainPagerAdapter : RecyclerView.Adapter<MainPagerAdapter.BindHolder>() {
    abstract class BindHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindHolder {
        return when (viewType) {
            0 -> PageAreaHolder(View.inflate(parent.context, R.layout.page_intro, null))
            1 -> PageSearchHolder(View.inflate(parent.context, R.layout.page_intro, null))
            2 -> PageResultHolder(View.inflate(parent.context, R.layout.page_intro, null))
            else -> PageTryHolder(View.inflate(parent.context, R.layout.page_try, null))
        }.apply {
            itemView.layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun getItemCount(): Int {
        return 4
    }

    override fun onBindViewHolder(holder: BindHolder, position: Int) {
        holder.bind()
    }
}