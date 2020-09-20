package com.example.kahoot_assistant

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.example.kahoot_assistant.DrawView.ColorBall
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.android.synthetic.main.action_view.view.*
import kotlinx.android.synthetic.main.capture_rect.view.*


class ControlService : Service() {
    private lateinit var controlView: ControlView
    private lateinit var rectView: View
    private lateinit var actionView: View
    private lateinit var windowManager: WindowManager
    private lateinit var recognizer: TextRecognizer
    private val rect = Rect()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        initRectView()
        initControlView()
        initActionView()

        windowManager.addView(controlView, controlView.layoutParams)
        windowManager.addView(actionView, actionView.layoutParams)

        recognizer = TextRecognition.getClient()
    }

    private fun initControlView() {
        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }

        params.gravity = Gravity.BOTTOM or Gravity.END
        params.x = 0
        params.y = getScreenHeight() / 4

        controlView = ControlView(this, windowManager).apply {
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
        val rectParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }

        rectView = View.inflate(this, R.layout.capture_rect, null).apply {
            drawView.points[0] = Point()
            drawView.points[1] = Point()
            drawView.points[2] = Point()
            drawView.points[3] = Point()
            // declare each ball with the ColorBall class
            for (pt in drawView.points) {
                drawView.colorballs.add(ColorBall(context, android.R.drawable.ic_menu_zoom, pt))
            }

            drawView.points[0].x = 50
            drawView.points[0].y = getScreenHeight() / 2 - 150
            drawView.points[1].x = getScreenWidth() - 100
            drawView.points[1].y = drawView.points[0].y
            drawView.points[2].x = drawView.points[1].x
            drawView.points[2].y = drawView.points[0].y + 300
            drawView.points[3].x = drawView.points[0].x
            drawView.points[3].y = drawView.points[2].y

            checkImgView.setOnClickListener {
                rect.apply {
                    left = drawView.colorballs[0].x
                    top = drawView.colorballs[0].y
                    right = drawView.colorballs[2].x
                    bottom = drawView.colorballs[2].y
                }
                windowManager.removeView(rectView)
            }
            layoutParams = rectParams
        }
    }

    private fun initActionView() {
        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }

        params.gravity = Gravity.BOTTOM or Gravity.END
        params.x = 0
        params.y = 0

        actionView = View.inflate(this, R.layout.action_view, null).apply {
            rectImgView.setOnClickListener {
                windowManager.addView(rectView, rectView.layoutParams)
            }
            layoutParams = params
        }
    }

    private fun getScreenHeight(): Int {
        val displayMetrics = DisplayMetrics()
        getWindowManager()!!.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    private fun getScreenWidth(): Int {
        val displayMetrics = DisplayMetrics()
        getWindowManager()!!.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(CHANNEL_ID, CHANNEL_NAME)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true)
            .setContentTitle("Kahoot Assistant is running")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)

        startForeground(1234, builder.build())
        return super.onStartCommand(intent, flags, startId)
    }

    private lateinit var projection: MediaProjection
    private lateinit var it: ImageTransmogrifier
    private lateinit var vdisplay: VirtualDisplay

    private fun startCapture() {
        receiveFirstImage = false

        projection = MainActivity.getMediaProjection(this)
        it = ImageTransmogrifier(this)
        val cb: MediaProjection.Callback = object : MediaProjection.Callback() {
            override fun onStop() {
                vdisplay.release()
            }
        }
        vdisplay = projection.createVirtualDisplay(
            "andshooter",
            it.getWidth(), it.getHeight(),
            resources.displayMetrics.densityDpi,
            VIRT_DISPLAY_FLAGS, it.getSurface(), null, handler
        )
        projection.registerCallback(cb, handler)
    }

    fun getWindowManager(): WindowManager? {
        return windowManager
    }

    val handler: Handler = Handler(Looper.getMainLooper())

    private fun stopCapture() {
        if (this::projection.isInitialized) {
            projection.stop()
            vdisplay.release()
        }
    }

    var receiveFirstImage = false

    fun processImage(bitmap: Bitmap) {
        if (receiveFirstImage) return
        receiveFirstImage = true

        val scale = getScreenHeight() / bitmap.height.toFloat()

        val scaledRect = Rect()
        scaledRect.left = (rect.left / scale).toInt()
        scaledRect.top = (rect.top / scale).toInt()
        scaledRect.right = (rect.right / scale).toInt()
        scaledRect.bottom = (rect.bottom / scale).toInt()

        val cropBitmap = Bitmap.createBitmap(
            bitmap,
            scaledRect.left,
            scaledRect.top,
            scaledRect.width(),
            scaledRect.height()
        )

        val testView = ImageView(this)
        testView.setImageBitmap(cropBitmap)
        testView.foreground = ColorDrawable(Color.parseColor("#50000000"))
        testView.setOnClickListener { windowManager.removeView(testView) }
        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }
        params.gravity = Gravity.TOP or Gravity.START
        params.x = rect.left
        params.y = rect.top
        params.width = rect.width()
        params.height = rect.height()
        windowManager.addView(testView, params)

        val image = InputImage.fromBitmap(cropBitmap, 0)
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text =
                    visionText.textBlocks.fold("") { acc, textBlock -> acc + " " + textBlock.text }
//                val longestBlock = visionText.textBlocks.maxByOrNull { it.text.length }
//                longestBlock?.let {
//                    searchText(it.text.replace('\n', ' '))
//                }
                searchText(text.replace('\n', ' '))
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, "Text Recog Failed", Toast.LENGTH_SHORT).show()
            }
        stopCapture()
    }

    private fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        val defaultBitmap = Bitmap.createBitmap(width, height, bitmap.config)
        val canvas = Canvas(defaultBitmap)
        canvas.drawBitmap(bitmap, Rect(x, y, width, height), Rect(0, 0, width, height), Paint())
        return bitmap
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
        windowManager.removeView(controlView)
        windowManager.removeView(rectView)
        stopCapture()
    }

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
        const val CHANNEL_ID = "1234"
        const val CHANNEL_NAME = "Control Channel"

        const val VIRT_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }
}
