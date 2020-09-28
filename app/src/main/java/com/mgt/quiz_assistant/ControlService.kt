package com.mgt.quiz_assistant

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.android.synthetic.main.action_view.view.*
import kotlinx.android.synthetic.main.capture_rect.view.*
import kotlin.math.max
import kotlin.math.round


class ControlService : Service() {
    private lateinit var controlView: ControlView
    private lateinit var rectView: View
    private lateinit var actionView: View
    private lateinit var recognizer: TextRecognizer
    private var receiveFirstImage = false
    private lateinit var rect: Rect

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        initRectView()
        initControlView()
        initActionView()

        val windowManager = Utils.getWindowManager()
        windowManager.addView(controlView, controlView.layoutParams)
        windowManager.addView(actionView, actionView.layoutParams)

        recognizer = TextRecognition.getClient()
    }

    private fun initControlView() {
        val params = Utils.createWindowParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.END
        params.x = 0
        params.y = Utils.getScreenHeight() / 4

        controlView = ControlView(this, Utils.getWindowManager()).apply {
            setOnSearchListener {
                startCapture()
            }
            setOnCloseListener {
                stopSelf()
            }
            layoutParams = params
        }
    }

    private fun initRectView() {
        rectView = View.inflate(this, R.layout.capture_rect, null).apply {
            doneButton.setOnClickListener {
                rect.apply {
                    left = drawView.colorballs[0].x
                    top = drawView.colorballs[0].y
                    right = drawView.colorballs[2].x
                    bottom = drawView.colorballs[2].y
                }

                rect.sort()

                SharedPrefsManager.setCaptureRect(rect)
                Utils.getWindowManager().removeViewSafe(rectView)
                actionView.visibility = View.VISIBLE
                controlView.visibility = View.VISIBLE
            }
            layoutParams = Utils.createWindowParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            ).apply { flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN }

            rect = SharedPrefsManager.getCaptureRect()
            drawView.setRect(rect)
            drawView.setListener(object : DrawView.Listener {
                override fun onStartModify() {
                    doneLayout.visibility = View.GONE
                }

                override fun onStopModify() {
                    doneLayout.visibility = View.VISIBLE
                }
            })
        }
    }

    private fun initActionView() {
        val params = Utils.createWindowParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.END
        params.x = 0
        params.y = 0

        actionView = View.inflate(this, R.layout.action_view, null).apply {
            rectImgView.setOnClickListener {
                Utils.getWindowManager().addView(rectView, rectView.layoutParams)
                actionView.visibility = View.INVISIBLE
                controlView.visibility = View.INVISIBLE
            }
            rectImgView.setOnTouchListener(DraggableTouchListener(this))
            setOnTouchListener(DraggableTouchListener(this))
            layoutParams = params
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(CHANNEL_ID, CHANNEL_NAME)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true)
            .setContentTitle(getString(R.string.foreground_title))
            .setSmallIcon(R.drawable.noti_icon)
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)

        startForeground(1234, builder.build())
        return super.onStartCommand(intent, flags, startId)
    }

    private lateinit var projection: MediaProjection
    private lateinit var it: ImageTransmogrifier
    private lateinit var vDisplay: VirtualDisplay

    private fun startCapture() {
        receiveFirstImage = false

        projection = MainActivity.getMediaProjection(this)
        it = ImageTransmogrifier(this)
        val cb: MediaProjection.Callback = object : MediaProjection.Callback() {
            override fun onStop() {
                vDisplay.release()
            }
        }
        vDisplay = projection.createVirtualDisplay(
            "andshooter",
            it.width, it.height,
            resources.displayMetrics.densityDpi,
            VIRT_DISPLAY_FLAGS, it.surface, null, handler
        )
        projection.registerCallback(cb, handler)
    }

    val handler: Handler = Handler(Looper.getMainLooper())

    private fun stopCapture() {
        if (this::projection.isInitialized) {
            projection.stop()
            vDisplay.release()
        }
    }

    fun processImage(bitmap: Bitmap) {
        if (receiveFirstImage) return
        receiveFirstImage = true

        val scaleX = Utils.getScreenWidth() / bitmap.width.toFloat()
        val scaleY = Utils.getScreenHeight() / bitmap.height.toFloat()
        val scale = max(scaleX, scaleY)

        val scaledRect = Rect()
        scaledRect.left = round(rect.left / scale).toInt()
        scaledRect.top = round(rect.top / scale).toInt()
        scaledRect.right = round(rect.right / scale).toInt()
        scaledRect.bottom = round(rect.bottom / scale).toInt()

        val cropBitmap = Bitmap.createBitmap(
            bitmap,
            scaledRect.left,
            scaledRect.top,
            scaledRect.width(),
            scaledRect.height()
        )

        val cropImgView = ImageView(this)
        cropImgView.setImageBitmap(cropBitmap)
        cropImgView.foreground = ColorDrawable(Color.parseColor("#50000000"))
        cropImgView.setOnClickListener { Utils.getWindowManager().removeViewSafe(cropImgView) }
        val params = Utils.createWindowParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = rect.left
            y = rect.top
            width = rect.width()
            height = rect.height()
            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }
        Utils.getWindowManager().addView(cropImgView, params)
        Handler(Looper.getMainLooper()).postDelayed({
            if (cropImgView.isAttachedToWindow) {
                Utils.getWindowManager().removeViewSafe(cropImgView)
            }
        }, 1000)

        val image = InputImage.fromBitmap(cropBitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text =
                    visionText.textBlocks.fold("") { acc, textBlock -> acc + " " + textBlock.text }
                searchText(text.replace('\n', ' '))
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                if (e is MlKitException) {
                    Toast.makeText(this, R.string.not_enough_mem, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, R.string.text_recog_error, Toast.LENGTH_LONG).show()
                }
            }
        stopCapture()
    }

    private fun searchText(text: String) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(
                SearchManager.QUERY,
                text
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Utils.getWindowManager().removeViewSafe(controlView)
        Utils.getWindowManager().removeViewSafe(rectView)
        Utils.getWindowManager().removeViewSafe(actionView)
        stopCapture()
    }

    @Suppress("SameParameterValue")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    companion object {
        const val CHANNEL_ID = "1998"
        const val CHANNEL_NAME = "Control Channel"

        const val VIRT_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }
}
