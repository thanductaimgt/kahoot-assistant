package com.mgt.quiz_assistant

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var gAnim: ValueAnimator
    private lateinit var kAnim: ValueAnimator
    private lateinit var textAnim: ValueAnimator
    private lateinit var titleAnim: ValueAnimator
    private var statusBarHeight = 0
    private lateinit var myDialog: MyDialog

    private var isInit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        if (!hasPermissions(this, PERMISSIONS)) {
//            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
//        }

        if (!Settings.canDrawOverlays(this)) {
            init()
        } else {
            requestScreenshotPermission()
        }
    }

    private fun init() {
        setTheme(R.style.AppTheme)
        requestFullScreen()
        setStatusBarMode(false)
        val rootView = View.inflate(this, R.layout.activity_main, null)
        makeRoomForStatusBar(this, rootView, MAKE_ROOM_TYPE_PADDING)
        setContentView(rootView)

        viewPager.adapter = MainPagerAdapter()
        circleIndicator.setViewPager(viewPager)

        myDialog = MyDialog()

        startAnim()

        isInit = true
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
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
                } else {
                    requestScreenshotPermission()
                }
            }
            SCREENSHOT_PERMISSION -> {
                if (RESULT_OK == resultCode) {
                    screenshotIntent = data
                    startService(Intent(this, ControlService::class.java))
                    finish()
                } else if (Activity.RESULT_CANCELED == resultCode) {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
                    if (!isInit) {
                        finish()
                    }
                }
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

    fun showDialog() {
        myDialog.show(supportFragmentManager,
            title = getString(R.string.request_draw_permission_title),
            description = getString(R.string.request_draw_permission_desc),
            button1Action = {
                if (!Settings.canDrawOverlays(this)) {
                    requestDrawOverlayPermission()
                } else {
                    requestScreenshotPermission()
                }
            })
    }

    private fun requestDrawOverlayPermission() {
        val intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        startActivityForResult(intent, DRAW_OVERLAY_PERMISSION)
    }

    private fun requestFullScreen() {
//        window.addFlags(Window.FEATURE_ACTION_BAR_OVERLAY)
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    private fun setStatusBarMode(isLight: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = if (isLight) {
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }

    @IntDef(MAKE_ROOM_TYPE_PADDING, MAKE_ROOM_TYPE_MARGIN)
    @Retention(AnnotationRetention.SOURCE)
    annotation class MakeRoomType

    private fun makeRoomForStatusBar(
        context: Context,
        targetView: View,
        @MakeRoomType makeRoomType: Int = MAKE_ROOM_TYPE_PADDING
    ) {
        val resourceId: Int =
            context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)

            makeRoomForStatusBarInternal(targetView, statusBarHeight, makeRoomType)
        } else {
            var isInvoked = false

            ViewCompat.setOnApplyWindowInsetsListener(targetView) { _, insets ->
                insets.also {
                    if (!isInvoked) {
                        isInvoked = true
                        statusBarHeight = insets.systemWindowInsetTop

                        makeRoomForStatusBarInternal(targetView, statusBarHeight, makeRoomType)
                    }
                }
            }
        }
//        val rectangle = Rect()
//        activity.window.decorView.getWindowVisibleDisplayFrame(rectangle)
//        val statusBarHeight: Int = rectangle.top
////        val contentViewTop: Int = activity.window.findViewById<View>(Window.ID_ANDROID_CONTENT).top
////        val titleBarHeight = contentViewTop - statusBarHeight
//        rootView.updatePadding(top = statusBarHeight)
    }

    private fun makeRoomForStatusBarInternal(
        targetView: View,
        statusBarHeight: Int,
        @MakeRoomType makeRoomType: Int = MAKE_ROOM_TYPE_PADDING
    ) {
        if (makeRoomType == MAKE_ROOM_TYPE_PADDING) {
            targetView.updatePadding(top = targetView.paddingTop + statusBarHeight)
        } else {
            targetView.updateLayoutParams {
                this as ViewGroup.MarginLayoutParams
                updateMargins(top = targetView.marginTop + statusBarHeight)
            }
        }
    }

    private fun startAnim() {
        appNameTextView.alpha = 0f
        googleIcon.scaleX = 0f
        kahootIcon.scaleX = 0f
        googleIcon.translationY = 0f
        kahootIcon.translationY = 0f

        textAnim = ValueAnimator().apply {
            duration = 500
            setFloatValues(0f, 1f)
            interpolator = LinearInterpolator()
            addUpdateListener {
                val value = animatedValue as Float
                googleIcon.translationY = -value * 20
                kahootIcon.translationY = googleIcon.translationY
                appNameTextView.translationY = -kahootIcon.translationY
                appNameTextView.alpha = value
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {

                }

                override fun onAnimationEnd(animation: Animator?) {
                    Handler(Looper.getMainLooper()).apply {
                        postDelayed({
                            kAnim.removeAllListeners()
                            gAnim.removeAllListeners()
                            textAnim.removeAllListeners()
                            kAnim.reverse()
                            gAnim.reverse()
                            textAnim.reverse()
                        }, 1000)
                        postDelayed({
                            titleAnim.start()
                        }, 1200)
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {

                }

                override fun onAnimationStart(animation: Animator?) {

                }
            })
        }
        gAnim = ValueAnimator().apply {
            duration = 600
            setFloatValues(0f, 1f)
            interpolator = BounceInterpolator()
            addUpdateListener {
                val value = animatedValue as Float
                googleIcon.scaleX = value
                googleIcon.scaleY = value
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {

                }

                override fun onAnimationEnd(animation: Animator?) {
                    textAnim.start()
                }

                override fun onAnimationCancel(animation: Animator?) {

                }

                override fun onAnimationStart(animation: Animator?) {

                }
            })
        }
        kAnim = ValueAnimator().apply {
            duration = 800
            setFloatValues(0f, 1f)
            interpolator = OvershootInterpolator()
            addUpdateListener {
                val value = animatedValue as Float
                kahootIcon.alpha = value
                kahootIcon.scaleX = value
                kahootIcon.scaleY = value
                kahootIcon.rotation = value * 10
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {

                }

                override fun onAnimationEnd(animation: Animator?) {
                    gAnim.start()
                }

                override fun onAnimationCancel(animation: Animator?) {

                }

                override fun onAnimationStart(animation: Animator?) {

                }
            })
        }
        titleAnim = ValueAnimator().apply {
            duration = 1000
            setFloatValues(0f, 1f)
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                titleTextView.alpha = animatedFraction
                appIcon.alpha = animatedFraction
                viewPager.alpha = animatedFraction
                circleIndicator.alpha = animatedFraction
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {

                }

                override fun onAnimationEnd(animation: Animator?) {

                }

                override fun onAnimationCancel(animation: Animator?) {

                }

                override fun onAnimationStart(animation: Animator?) {

                }
            })
        }
        kAnim.start()
    }

    companion object {
        const val MAKE_ROOM_TYPE_PADDING = 0
        const val MAKE_ROOM_TYPE_MARGIN = 1

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