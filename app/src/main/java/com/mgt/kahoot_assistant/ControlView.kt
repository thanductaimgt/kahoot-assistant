package com.mgt.kahoot_assistant

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import android.view.animation.BounceInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.control_bubble.view.*
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class ControlView(context: Context, private val windowManager: WindowManager): ConstraintLayout(context) {
    private var onCloseListener:(()->Any?)?=null
    private var onSearchListener:(()->Any?)?=null

    init {
        LayoutInflater.from(context).inflate(R.layout.control_bubble, this)

        initView()
    }

    private fun initView() {
        //Set the close button.

        //Set the close button.
        closeButton.setOnClickListener {
            onCloseListener?.invoke()
        }

        //Drag and move chat head using user's touch action.
        searchImgView.apply {
            setOnTouchListener(DraggableTouchListener(this@ControlView))
            setOnClickListener { onSearchListener?.invoke() }
        }

        val anim = ValueAnimator().apply {
            setFloatValues(0f, 1f)
            duration = 800
            interpolator = BounceInterpolator()
        }
        anim.addUpdateListener {
            scaleX = it.animatedValue as Float
            scaleY = it.animatedValue as Float
        }
        anim.start()
    }

    fun setOnCloseListener(onClose:(()->Any?)?){
        onCloseListener = onClose
    }

    fun setOnSearchListener(onSearch:(()->Any?)?){
        onSearchListener = onSearch
    }
}