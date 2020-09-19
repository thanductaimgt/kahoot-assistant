package com.example.kahoot_assistant

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import android.view.animation.BounceInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.control_bubble.view.*

@SuppressLint("ViewConstructor")
class ControlView(context: Context, private val windowManager: WindowManager): ConstraintLayout(context) {
    private var onCloseListener:(()->Any?)?=null
    private var onSearchListener:(()->Any?)?=null

    init {
        LayoutInflater.from(context).inflate(R.layout.control_bubble, this)

        initView()
    }

    private fun initView(){
        //Set the close button.

        //Set the close button.
        closeButton.setOnClickListener{
            onCloseListener?.invoke()
        }

        //Drag and move chat head using user's touch action.
        searchImgView.setOnTouchListener(object : OnTouchListener {
            private var lastDownTime:Long = 0
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = layoutParams as WindowManager.LayoutParams

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {

                        //remember the initial position.
                        initialX = params.x
                        initialY = params.y

                        //get the touch location
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastDownTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        //As we implemented on touch listener with ACTION_MOVE,
                        //we have to check if the previous action was ACTION_DOWN
                        //to identify if the user clicked the view or not.
                        if (System.currentTimeMillis() - lastDownTime < ViewConfiguration.getLongPressTimeout()) {
                            onSearchListener?.invoke()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX - (event.rawX - initialTouchX).toInt()
                        params.y = initialY - (event.rawY - initialTouchY).toInt()

                        //Update the layout with new X & Y coordinate
                        windowManager.updateViewLayout(this@ControlView, params)
                        return true
                    }
                }
                return false
            }
        })

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