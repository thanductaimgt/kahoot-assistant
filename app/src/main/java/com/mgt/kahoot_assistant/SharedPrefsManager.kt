package com.mgt.kahoot_assistant

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect

object SharedPrefsManager {
    private lateinit var sharedPrefs: SharedPreferences

    fun getSharedPrefs(): SharedPreferences {
        if (!this::sharedPrefs.isInitialized) {
            sharedPrefs = MyApp.context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        }
        return sharedPrefs
    }

    fun getCaptureRect(): Rect {
        val formattedRect = getSharedPrefs().getString(CAPTURE_RECT, null)
        val rect = Rect()
        if (formattedRect == null) {
            val height = 300
            rect.left = 50
            rect.right = Utils.getScreenWidth() - rect.left
            rect.top = Utils.getScreenHeight() / 2 - height/2
            rect.bottom = rect.top + height
        } else {
            val pos = formattedRect.split(',').map { it.toInt() }
            rect.left = pos[0]
            rect.top = pos[1]
            rect.right = pos[2]
            rect.bottom = pos[3]
        }
        return rect
    }

    fun setCaptureRect(rect: Rect) {
        getSharedPrefs().edit().apply {
            putString(CAPTURE_RECT, "${rect.left},${rect.top},${rect.right},${rect.bottom}")
            apply()
        }
    }

    private const val NAME = "K_ASS"
    private const val CAPTURE_RECT = "CAPTURE_RECT"
}