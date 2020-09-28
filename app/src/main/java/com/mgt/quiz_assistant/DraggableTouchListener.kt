package com.mgt.quiz_assistant

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import kotlin.math.abs

class DraggableTouchListener(private val rootView:View): View.OnTouchListener {
    private var lastDownTime: Long = 0
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val params = rootView.layoutParams as WindowManager.LayoutParams

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
                if (System.currentTimeMillis() - lastDownTime < ViewConfiguration.getLongPressTimeout()
                    && abs(initialTouchX - event.rawX) < 20 && abs(initialTouchY - event.rawY) < 20
                ) {
                    v.performClick()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                //Calculate the X and Y coordinates of the view.
                params.x = initialX - (event.rawX - initialTouchX).toInt()
                params.y = initialY - (event.rawY - initialTouchY).toInt()

                //Update the layout with new X & Y coordinate
                Utils.getWindowManager().updateViewLayout(rootView, params)
                return true
            }
        }
        return false
    }
}