package com.example.kahoot_assistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        if (!hasPermissions(this, PERMISSIONS)) {
//            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
//        }

        if (!Settings.canDrawOverlays(this)) {
            requestDrawOverlayPermission()
        } else {
            requestScreenshotPermission()
        }
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == PERMISSION_ALL) {
//            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
//                startService(Intent(this, ControlService::class.java))
//            } else {
//                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
//            }
////            finish()
//        }
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            DRAW_OVERLAY_PERMISSION -> {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "draw permissions denied", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    requestScreenshotPermission()
                }
            }
            SCREENSHOT_PERMISSION -> {
                if (RESULT_OK == resultCode) {
                    screenshotIntent = data
                    startService(Intent(this, ControlService::class.java))
                } else if (Activity.RESULT_CANCELED == resultCode) {
                    Toast.makeText(this, "screenshot permission denied", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }
    }

//    fun hasPermissions(context: Context, permissions: Array<String>): Boolean = permissions.all {
//        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
//    }

    private fun requestScreenshotPermission() {
        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            SCREENSHOT_PERMISSION
        )
    }

    private fun requestDrawOverlayPermission() {
        val intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        startActivityForResult(intent, DRAW_OVERLAY_PERMISSION)
    }

    companion object {
        const val SCREENSHOT_PERMISSION = 0
        const val DRAW_OVERLAY_PERMISSION = 1
//        val PERMISSIONS = arrayOf(
//            android.Manifest.permission.SYSTEM_ALERT_WINDOW
//        )

        private var mediaProjection: MediaProjection? = null
        private var screenshotIntent: Intent? = null

        fun getMediaProjection(context: Context): MediaProjection {
            val mediaProjectionManager =
                context.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            if (null != mediaProjection) {
                mediaProjection!!.stop()
                mediaProjection = null
            }
            mediaProjection = mediaProjectionManager.getMediaProjection(
                RESULT_OK,
                screenshotIntent!!.clone() as Intent
            )
            return mediaProjection!!
        }
    }
}