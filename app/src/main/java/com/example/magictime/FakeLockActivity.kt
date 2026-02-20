package com.example.magictime

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.magictime.databinding.ActivityFakeLockBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class FakeLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFakeLockBinding
    private val prefs by lazy { getSharedPreferences("MagicPrefs", Context.MODE_PRIVATE) }
    private var isMagicActivated = false
    private var currentDisplayOffset = 0L
    private var delayStartMs = 0L
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector

    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level: Int = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

                if (level != -1 && scale != -1) {
                    val batteryPct = (level * 100 / scale.toFloat()).toInt()

                    binding.tvBatteryLevel.text = "$batteryPct"
                    binding.tvBatteryLevel.background.setLevel(batteryPct * 100)

                    // Menentukan warna icon baterai
                    val bgColor = when {
                        batteryPct <= 5 -> android.graphics.Color.RED
                        batteryPct <= 15 -> android.graphics.Color.YELLOW
                        else -> android.graphics.Color.WHITE
                    }
                    binding.tvBatteryLevel.background.setTint(bgColor)

                    val textColor = if (batteryPct <= 5) {
                        android.graphics.Color.WHITE
                    } else {
                        android.graphics.Color.BLACK
                    }
                    binding.tvBatteryLevel.setTextColor(textColor)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityFakeLockBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        binding.tvTicker.isSelected = true
        binding.tvMarqueeBottom.isSelected = true

        try { hideSystemUI() } catch(e: Exception){}

        loadSettings()
        loadWallpaper()
        setupInteractions()
        setupShortcuts()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
        startClockLoop()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
        handler.removeCallbacksAndMessages(null)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupShortcuts() {
        binding.bgCamera.setOnClickListener(null)
        binding.bgPhone.setOnClickListener(null)

        val openCameraAction = {
            try {
                val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE)
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    startActivity(Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                } catch (e: Exception) {}
            }
        }
        applySwipeEffect(binding.bgCamera, openCameraAction)
        applySwipeEffect(binding.ivCamera, openCameraAction)

        val openPhoneAction = {
            try {
                val intent = Intent(Intent.ACTION_DIAL)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        applySwipeEffect(binding.bgPhone, openPhoneAction)
        applySwipeEffect(binding.ivPhone, openPhoneAction)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun applySwipeEffect(view: View, onTrigger: () -> Unit) {
        view.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0f
            var startY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - startX
                        val dy = event.rawY - startY
                        v.translationX = dx
                        v.translationY = dy
                        v.alpha = 0.7f
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val distance = Math.hypot((event.rawX - startX).toDouble(), (event.rawY - startY).toDouble())

                        if (distance > 150) {
                            onTrigger()
                            v.translationX = 0f
                            v.translationY = 0f
                            v.scaleX = 1.0f
                            v.scaleY = 1.0f
                            v.alpha = 1.0f
                        } else {
                            v.animate()
                                .translationX(0f).translationY(0f)
                                .scaleX(1.0f).scaleY(1.0f)
                                .alpha(1.0f)
                                .setDuration(300)
                                .setInterpolator(android.view.animation.OvershootInterpolator())
                                .start()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupInteractions() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float): Boolean {
                if (e1 != null && (e1.y - e2.y > 100) && Math.abs(vY) > 100) {
                    performUnlock()
                    return true
                }
                return false
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!prefs.getBoolean("TRIGGER_VOLUME", true)) {
                    triggerMagic()
                    return true
                }
                return super.onDoubleTap(e)
            }
        })
        binding.root.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event); true }
    }

    private fun loadSettings() {
        currentDisplayOffset = prefs.getInt("OFFSET_SEC", 0).toLong()
        delayStartMs = prefs.getInt("DELAY_MS", 0).toLong()

        val showCarrier = prefs.getBoolean("SHOW_CARRIER", true)
        binding.tvTicker.visibility = if(showCarrier) View.VISIBLE else View.INVISIBLE
        binding.tvTicker.text = prefs.getString("CUSTOM_CARRIER", "TELKOMSEL · Emergency call only")

        val showMarquee = prefs.getBoolean("SHOW_MARQUEE", true)
        binding.tvMarqueeBottom.visibility = if(showMarquee) View.VISIBLE else View.INVISIBLE
        binding.tvMarqueeBottom.text = prefs.getString("CUSTOM_MARQUEE", "custom text here")

        binding.ivWifi.visibility = if (prefs.getBoolean("SHOW_WIFI", true)) View.VISIBLE else View.GONE

        val plainColor = android.graphics.Color.parseColor("#4BFFFFFF")

        if (prefs.getBoolean("SIM1_4G", true)) {
            binding.iv4g1.visibility = View.VISIBLE; binding.ivSignal1.clearColorFilter()
        } else {
            binding.iv4g1.visibility = View.GONE; binding.ivSignal1.setColorFilter(plainColor)
        }

        if (prefs.getBoolean("SIM2_4G", false)) {
            binding.iv4g2.visibility = View.VISIBLE; binding.ivSignal2.clearColorFilter()
        } else {
            binding.iv4g2.visibility = View.GONE; binding.ivSignal2.setColorFilter(plainColor)
        }
    }

    private fun loadWallpaper() {
        val uriString = prefs.getString("WALLPAPER_URI", null)
        if (uriString != null) {
            try { binding.imgBackground.setImageURI(Uri.parse(uriString)) } catch (e: Exception) {
                binding.imgBackground.setBackgroundColor(android.graphics.Color.BLACK)
            }
        } else { binding.imgBackground.setBackgroundColor(android.graphics.Color.BLACK) }
    }

    private fun startClockLoop() {
        val runnable = object : Runnable {
            override fun run() {
                updateTimeUI()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun updateTimeUI() {
        val now = System.currentTimeMillis() + (currentDisplayOffset * 1000)
        val is24Hour = prefs.getBoolean("IS_24H", true)
        val timePattern = if (is24Hour) "HH:mm" else "h:mm"
        val bigFormat = SimpleDateFormat(timePattern, Locale.getDefault())
        try { binding.tvBigClock.text = bigFormat.format(Date(now)) } catch (e: Exception){}

        val langCode = prefs.getString("DATE_LANGUAGE", "id") ?: "id"
        val datePattern: String
        val locale: Locale

        if (langCode == "en") {
            datePattern = "EEE, MMM dd"
            locale = Locale.ENGLISH
        } else {
            datePattern = "EEE, dd MMM"
            locale = Locale("id", "ID")
        }

        val dateFormat = SimpleDateFormat(datePattern, locale)
        try { binding.tvDate.text = dateFormat.format(Date(now)) } catch (e: Exception){}
    }

    private fun triggerMagic() {
        if (isMagicActivated) return
        isMagicActivated = true
        lifecycleScope.launch {
            if (delayStartMs > 0) delay(delayStartMs)
            val totalMinutesToTravel = abs(currentDisplayOffset / 60).toInt()
            val stepDirection = if (currentDisplayOffset > 0) -1 else 1
            for (i in 1..totalMinutesToTravel) {
                currentDisplayOffset += (60 * stepDirection)
                updateTimeUI()
                val remainingMinutes = abs(currentDisplayOffset / 60)
                if (remainingMinutes < 3) delay(1000) else delay(500)
            }
            currentDisplayOffset = 0
            updateTimeUI()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (prefs.getBoolean("TRIGGER_VOLUME", true) && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            triggerMagic()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun performUnlock() {
        finishAndRemoveTask()
        overridePendingTransition(0, R.anim.slide_out_up)
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
