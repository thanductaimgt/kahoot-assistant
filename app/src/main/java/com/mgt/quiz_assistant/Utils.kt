package com.mgt.quiz_assistant

import android.app.Service
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager

object Utils {
    private lateinit var windowManager: WindowManager

    fun getWindowManager(): WindowManager {
        if (!this::windowManager.isInitialized) {
            windowManager = MyApp.context.getSystemService(Service.WINDOW_SERVICE) as WindowManager
        }
        return windowManager
    }

    fun getScreenHeight(): Int {
        val displayMetrics = DisplayMetrics()
        getWindowManager().defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    fun getScreenWidth(): Int {
        val displayMetrics = DisplayMetrics()
        getWindowManager().defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    fun createWindowParams(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
            ,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
    }
}

fun WindowManager.removeViewSafe(view: View) {
    if (view.isAttachedToWindow) {
        removeView(view)
    }
}