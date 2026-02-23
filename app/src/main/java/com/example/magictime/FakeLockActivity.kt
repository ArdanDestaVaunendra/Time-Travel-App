package com.example.magictime

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
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
import kotlin.text.compareTo
import kotlin.times

class FakeLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFakeLockBinding
    private val prefs by lazy { getSharedPreferences("MagicPrefs", Context.MODE_PRIVATE) }
    private var isMagicActivated = false
    private var currentDisplayOffset = 0L
    private var delayStartMs = 0L
    private var timeSpeedMultiplier: Float = 1.0f
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector
    private var baseRealTime: Long = 0L
    private var baseSyntheticTime: Long = 0L
    private var currentPinInput = ""
    private val maxPinLength = 6
    private var correctPin = "888000"
    private var isPinEnabled = true
    private var blurredWallpaperBitmap: android.graphics.Bitmap? = null
    private var isRevealEnabled = false
    private var revealText = ""
    private var revealDelay = 3
    private var revealTarget = "BOTH"
    private var isRevealed = false
    private var touchStartY = 0f
    private var screenHeight = 0
    private val swipeThreshold = 0.15f
    private var isSwipingForUnlock = false

    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level: Int = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

                if (level != -1 && scale != -1) {
                    val batteryPct = (level * 100 / scale.toFloat()).toInt()

                    binding.tvBatteryLevel.text = "$batteryPct"
                    binding.tvBatteryLevel.background.setLevel(batteryPct * 100)

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityFakeLockBinding.inflate(layoutInflater)
            setContentView(binding.root)
            screenHeight = resources.displayMetrics.heightPixels
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        binding.tvTicker.isSelected = true
        binding.tvMarqueeBottom.isSelected = true

        try {
            hideSystemUI()
        } catch (e: Exception) {
        }

        loadSettings()

        isPinEnabled = prefs.getBoolean("ENABLE_PIN", true)
        correctPin = prefs.getString("CUSTOM_PIN", "123456") ?: "123456"

        loadWallpaper()
        setupInteractions()
        setupShortcuts()
        setupPinScreenInteractions()
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
                val intent =
                    Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    }
                startActivity(intent)

                overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out)
            } catch (e: Exception) {
                try {
                    val intent =
                        Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        }
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out)
                } catch (e: Exception) {
                }
            }
        }
        applySwipeEffect(binding.bgCamera, false, openCameraAction)
        applySwipeEffect(binding.ivCamera, false, openCameraAction)

        val openPhoneAction = {
            binding.root.animate().alpha(0f).setDuration(200).withEndAction {
                try {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    }
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_left, android.R.anim.fade_out)

                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.root.alpha = 1f
                    }, 500)

                } catch (e: Exception) {
                    binding.root.alpha = 1f
                    e.printStackTrace()
                }
            }.start()
        }

        applySwipeEffect(binding.bgPhone, true, openPhoneAction)
        applySwipeEffect(binding.ivPhone, true, openPhoneAction)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun applySwipeEffect(view: View, isSwipeRight: Boolean, onTrigger: () -> Unit) {
        view.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0f

            val maxDragDistance = 250f
            val triggerDistance = 150f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - startX

                        val validDx = if (isSwipeRight) {
                            if (dx > 0) Math.min(dx, maxDragDistance) else 0f
                        } else {
                            if (dx < 0) Math.max(dx, -maxDragDistance) else 0f
                        }

                        val progress = Math.abs(validDx) / maxDragDistance

                        v.translationX = validDx
                        v.translationY = 0f
                        v.alpha = 1f - (progress * 0.5f)

                        val dynamicScale = 1.1f + (progress * 0.15f)
                        v.scaleX = dynamicScale
                        v.scaleY = dynamicScale

                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val dx = event.rawX - startX

                        val validDx = if (isSwipeRight) {
                            if (dx > 0) Math.min(dx, maxDragDistance) else 0f
                        } else {
                            if (dx < 0) Math.max(dx, -maxDragDistance) else 0f
                        }

                        val distance = Math.abs(validDx)

                        if (distance >= triggerDistance) {
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
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (prefs.getBoolean("TRIGGER_VOLUME", true)) {
                    triggerSecretMessage()
                } else {
                    triggerMagic()
                }
                return true
            }
        })

        binding.root.setOnTouchListener { _, event ->
            if (gestureDetector.onTouchEvent(event)) return@setOnTouchListener true

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartY = event.rawY
                    isSwipingForUnlock = true
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!isSwipingForUnlock) return@setOnTouchListener false

                    val deltaY = touchStartY - event.rawY

                    if (deltaY > 0) {
                        val progress = (deltaY / (screenHeight / 1.5f)).coerceIn(0f, 1f)
                        applyInteractiveAnimation(progress)
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isSwipingForUnlock) return@setOnTouchListener false
                    isSwipingForUnlock = false

                    val deltaY = touchStartY - event.rawY

                    if (deltaY > screenHeight * swipeThreshold) {

                        if (isPinEnabled) {
                            performUnlock()

                            resetUnlockAnimation()
                        } else {
                            finishUnlockWithAnimation()
                        }

                    } else {
                        resetUnlockAnimation()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun loadSettings() {
        currentDisplayOffset = prefs.getInt("OFFSET_SEC", 0).toLong()
        delayStartMs = prefs.getInt("DELAY_MS", 0).toLong()
        timeSpeedMultiplier = prefs.getFloat("TIME_SPEED", 1.0f)

        baseRealTime = System.currentTimeMillis()
        baseSyntheticTime = System.currentTimeMillis() + (currentDisplayOffset * 1000)

        val showCarrier = prefs.getBoolean("SHOW_CARRIER", true)
        binding.tvTicker.visibility = if (showCarrier) View.VISIBLE else View.INVISIBLE
        binding.tvTicker.text = prefs.getString("CUSTOM_CARRIER", "TELKOMSEL · Emergency call only")

        val showMarquee = prefs.getBoolean("SHOW_MARQUEE", true)
        binding.tvMarqueeBottom.visibility = if (showMarquee) View.VISIBLE else View.INVISIBLE
        binding.tvMarqueeBottom.text = prefs.getString("CUSTOM_MARQUEE", "custom text here")

        binding.ivWifi.visibility =
            if (prefs.getBoolean("SHOW_WIFI", true)) View.VISIBLE else View.GONE

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

        isRevealEnabled = prefs.getBoolean("ENABLE_REVEAL", false)
        revealText = prefs.getString("REVEAL_TEXT", "") ?: ""
        revealDelay = prefs.getInt("REVEAL_DELAY", 3)
        revealTarget = prefs.getString("REVEAL_TARGET", "BOTH") ?: "BOTH"
    }

    private fun loadWallpaper() {
        val uriString = prefs.getString("WALLPAPER_URI", null)
        if (uriString != null) {
            try {
                binding.imgBackground.setImageURI(Uri.parse(uriString))
            } catch (e: Exception) {
                binding.imgBackground.setBackgroundColor(android.graphics.Color.BLACK)
            }
        } else {
            binding.imgBackground.setBackgroundColor(android.graphics.Color.BLACK)
        }

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sourceBitmap = if (uriString != null) {
                    android.provider.MediaStore.Images.Media.getBitmap(
                        contentResolver,
                        Uri.parse(uriString)
                    )
                } else {
                    android.graphics.Bitmap.createBitmap(
                        100,
                        100,
                        android.graphics.Bitmap.Config.ARGB_8888
                    ).apply { eraseColor(android.graphics.Color.BLACK) }
                }

                val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                    sourceBitmap,
                    sourceBitmap.width / 5,
                    sourceBitmap.height / 5,
                    false
                )
                blurredWallpaperBitmap = blurBitmap(this@FakeLockActivity, scaledBitmap, 20f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startClockLoop() {
        handler.removeCallbacksAndMessages(null)
        val runnable = object : Runnable {
            override fun run() {
                updateTimeUI()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun updateTimeUI() {
        val elapsedReal = System.currentTimeMillis() - baseRealTime
        val now = System.currentTimeMillis() + (currentDisplayOffset * 1000)

        val is24Hour = prefs.getBoolean("IS_24H", true)
        val timePattern = if (is24Hour) "HH:mm" else "h:mm"
        val bigFormat = SimpleDateFormat(timePattern, Locale.getDefault())
        try {
            binding.tvBigClock.text = bigFormat.format(Date(now))
        } catch (e: Exception) {
        }

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
        try {
            binding.tvDate.text = dateFormat.format(Date(now))
        } catch (e: Exception) {
        }
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

                val baseDelay = if (remainingMinutes < 3) 1000L else 500L
                val actualDelay = (baseDelay / timeSpeedMultiplier).toLong()

                delay(actualDelay)
            }
            currentDisplayOffset = 0
            updateTimeUI()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val isVolumeForTime = prefs.getBoolean("TRIGGER_VOLUME", true)

            if (isVolumeForTime) {
                triggerMagic()
            } else {
                triggerSecretMessage()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun setupPinScreenInteractions() {
        for (i in 0..9) {
            val resId = resources.getIdentifier("btnPin$i", "id", packageName)
            if (resId != 0) {
                val btnLayout = findViewById<android.view.View>(resId)

                val tvNumber = btnLayout.findViewById<android.widget.TextView>(R.id.tvPinNumber)
                val tvLetters = btnLayout.findViewById<android.widget.TextView>(R.id.tvPinLetters)

                tvNumber.text = i.toString()
                tvLetters.text = when (i) {
                    2 -> "ABC"; 3 -> "DEF"; 4 -> "GHI"; 5 -> "JKL"; 6 -> "MNO"; 7 -> "PQRS"; 8 -> "TUV"; 9 -> "WXYZ"
                    else -> ""
                }

                btnLayout.setOnClickListener {
                    onPinDigitEntered(i.toString())
                }
            }
        }

        binding.btnDeletePin.setOnClickListener { onPinDelete() }

        val btnOk = findViewById<android.widget.TextView>(R.id.btnOkPin)
        btnOk.setOnClickListener {
            validatePin()
        }

        binding.btnEmergencyPin.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyInteractiveAnimation(progress: Float) {
        val invertedAlpha = 1f - progress
        val translationUp = -300f * progress
        binding.tvBigClock.translationY = translationUp
        binding.tvDate.translationY = translationUp
        binding.ivLock.translationY = translationUp

        binding.tvBigClock.alpha = invertedAlpha
        binding.tvDate.alpha = invertedAlpha
        binding.ivLock.alpha = invertedAlpha

        binding.bgPhone.translationY = 200f * progress
        binding.bgPhone.translationX = -150f * progress
        binding.bgPhone.alpha = invertedAlpha

        binding.ivPhone.translationY = 200f * progress
        binding.ivPhone.translationX = -150f * progress
        binding.ivPhone.alpha = invertedAlpha

        binding.bgCamera.translationY = 200f * progress
        binding.bgCamera.translationX = 150f * progress
        binding.bgCamera.alpha = invertedAlpha

        binding.ivCamera.translationY = 200f * progress
        binding.ivCamera.translationX = 150f * progress
        binding.ivCamera.alpha = invertedAlpha

        binding.statusBarContainer.alpha = invertedAlpha
        binding.tvTicker.alpha = invertedAlpha
        binding.tvMarqueeBottom.alpha = invertedAlpha
    }

    private fun resetUnlockAnimation() {
        val duration = 300L
        val interpolator = android.view.animation.OvershootInterpolator(1.2f)

        fun resetView(v: View) {
            v.animate()
                .translationY(0f)
                .translationX(0f)
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .start()
        }

        resetView(binding.tvBigClock); resetView(binding.tvDate); resetView(binding.ivLock)
        resetView(binding.bgPhone); resetView(binding.ivPhone)
        resetView(binding.bgCamera); resetView(binding.ivCamera)
        resetView(binding.statusBarContainer); resetView(binding.tvTicker); resetView(binding.tvMarqueeBottom)
    }

    private fun finishUnlockWithAnimation() {
        val duration = 250L
        val interpolator = android.view.animation.AccelerateInterpolator()

        fun animateOut(v: View, transY: Float, transX: Float) {
            v.animate()
                .translationY(transY)
                .translationX(transX)
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .start()
        }

        animateOut(binding.tvBigClock, -500f, 0f)
        animateOut(binding.tvDate, -500f, 0f)
        animateOut(binding.ivLock, -500f, 0f)

        animateOut(binding.bgPhone, 400f, -300f)
        animateOut(binding.ivPhone, 400f, -300f)

        animateOut(binding.bgCamera, 400f, 300f)
        animateOut(binding.ivCamera, 400f, 300f)

        animateOut(binding.statusBarContainer, 0f, 0f)
        animateOut(binding.tvTicker, 0f, 0f)
        animateOut(binding.tvMarqueeBottom, 0f, 0f)

        Handler(Looper.getMainLooper()).postDelayed({
            finishAndRemoveTask()
            overridePendingTransition(0, 0)
        }, duration + 50)
    }

    private fun onPinDigitEntered(digit: String) {
        if (currentPinInput.length >= 6) return

        binding.tvPinError.visibility = View.GONE
        binding.tvPinError.clearAnimation()

        binding.tvEnterPinLabel.visibility = View.INVISIBLE
        binding.tvPinInfoLabel.visibility = View.INVISIBLE
        binding.pinIndicatorsLayout.visibility = View.VISIBLE

        currentPinInput += digit

        updatePinIndicators()

        if (currentPinInput.length == 6) {
            handler.postDelayed({
                validatePin()
            }, 150)
        }
    }

    private fun updatePinIndicators() {
        val dots = listOf(
            binding.dot1, binding.dot2, binding.dot3,
            binding.dot4, binding.dot5, binding.dot6
        )

        for (i in dots.indices) {
            if (i < currentPinInput.length) {
                dots[i].setBackgroundResource(R.drawable.bg_pin_dot_filled)
            } else {
                dots[i].setBackgroundResource(R.drawable.bg_pin_dot_empty)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun validatePin() {
        if (currentPinInput.length < 6) return

        if (currentPinInput == correctPin) {
            finishPinUnlockWithAnimation()
        } else {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator?.vibrate(200)
            }

            binding.pinIndicatorsLayout.visibility = View.INVISIBLE
            binding.tvPinInfoLabel.visibility = View.INVISIBLE

            binding.tvEnterPinLabel.translationY = 120f
            binding.tvEnterPinLabel.text = "Incorrect PIN"
            binding.tvEnterPinLabel.setTextColor(android.graphics.Color.WHITE)
            binding.tvEnterPinLabel.visibility = View.VISIBLE

            val shake = android.view.animation.TranslateAnimation(-10f, 10f, 0f, 0f).apply {
                duration = 50
                repeatCount = 5
                repeatMode = android.view.animation.Animation.REVERSE
            }
            binding.tvEnterPinLabel.startAnimation(shake)

            currentPinInput = ""
            updatePinIndicators()

            handler.postDelayed({
                if (binding.tvEnterPinLabel.text == "Incorrect PIN") {
                    binding.tvEnterPinLabel.translationY = 0f

                    binding.tvEnterPinLabel.text = "Enter PIN"
                    binding.tvEnterPinLabel.setTextColor(android.graphics.Color.WHITE)

                    binding.tvPinInfoLabel.visibility = View.VISIBLE
                }
            }, 2000)
        }
    }

    private fun onPinDelete() {
        if (currentPinInput.isNotEmpty()) {
            currentPinInput = currentPinInput.dropLast(1)
            updatePinIndicators()
        }

        if (currentPinInput.isEmpty()) {
            binding.tvEnterPinLabel.visibility = View.VISIBLE
            binding.tvPinInfoLabel.visibility = View.VISIBLE
            binding.pinIndicatorsLayout.visibility = View.INVISIBLE
        }
    }

    private fun performUnlock() {
        if (!isPinEnabled) {
            finishUnlockWithAnimation()
            return
        }

        currentPinInput = ""
        updatePinIndicators()

        binding.tvEnterPinLabel.translationY = 0f
        binding.tvEnterPinLabel.text = "Enter PIN"
        binding.tvEnterPinLabel.setTextColor(android.graphics.Color.WHITE)
        binding.tvEnterPinLabel.visibility = View.VISIBLE
        binding.tvPinInfoLabel.visibility = View.VISIBLE
        binding.pinIndicatorsLayout.visibility = View.INVISIBLE
        binding.pinScreenContainer.visibility = View.VISIBLE
        binding.pinScreenContainer.alpha = 0f
        binding.pinScreenContainer.translationY = 0f

        val startScale = 0.7f

        binding.bottomKeypadContainer.scaleX = startScale
        binding.bottomKeypadContainer.scaleY = startScale
        binding.bottomKeypadContainer.alpha = 0f

        binding.tvEnterPinLabel.scaleX = startScale
        binding.tvEnterPinLabel.scaleY = startScale
        binding.tvEnterPinLabel.alpha = 0f

        binding.tvPinInfoLabel.scaleX = startScale
        binding.tvPinInfoLabel.scaleY = startScale
        binding.tvPinInfoLabel.alpha = 0f

        binding.ivLockIcon.scaleX = 1f
        binding.ivLockIcon.scaleY = 1f
        binding.ivLockIcon.alpha = 0f

        blurredWallpaperBitmap?.let {
            binding.imgPinBackgroundBlurred.setImageBitmap(it)
            binding.imgPinBackgroundBlurred.setColorFilter(
                android.graphics.Color.parseColor("#99000000"),
                android.graphics.PorterDuff.Mode.SRC_OVER
            )
        }

        val duration = 350L
        val popInterpolator = android.view.animation.OvershootInterpolator(1.2f)
        val fadeInterpolator = android.view.animation.DecelerateInterpolator()

        binding.pinScreenContainer.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(fadeInterpolator)
            .start()

        binding.ivLockIcon.animate()
            .alpha(1f)
            .setStartDelay(0)
            .setDuration(duration)
            .setInterpolator(fadeInterpolator)
            .start()

        fun popUpAnim(v: View, delay: Long) {
            v.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(duration)
                .setStartDelay(delay)
                .setInterpolator(popInterpolator)
                .start()
        }

        popUpAnim(binding.tvEnterPinLabel, 0)
        popUpAnim(binding.tvPinInfoLabel, 50)
        popUpAnim(binding.bottomKeypadContainer, 100)
    }

    private fun hidePinScreen() {
        binding.pinScreenContainer.animate()
            .alpha(0f)
            .translationY(binding.root.height.toFloat())
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                binding.pinScreenContainer.visibility = View.GONE
                currentPinInput = ""
                updatePinIndicators()
            }
            .start()
    }

    override fun onBackPressed() {
        if (binding.pinScreenContainer.visibility == View.VISIBLE) {
            hidePinScreen()
        } else {
            super.onBackPressed()
        }
    }

    private fun triggerSecretMessage() {
        if (!isRevealEnabled || isRevealed || revealText.isEmpty()) return
        isRevealed = true

        Handler(Looper.getMainLooper()).postDelayed({

            val viewsToAnimate = mutableListOf<android.view.View>()
            when (revealTarget) {
                "CARRIER" -> viewsToAnimate.add(binding.tvTicker)
                "MARQUEE" -> viewsToAnimate.add(binding.tvMarqueeBottom)
                "BOTH" -> {
                    viewsToAnimate.add(binding.tvTicker)
                    viewsToAnimate.add(binding.tvMarqueeBottom)
                }
            }

            for (v in viewsToAnimate) {
                if (v !is android.widget.TextView) continue

                v.animate().alpha(0f).setDuration(200).withEndAction {
                    v.animate().alpha(1f).setDuration(200).withEndAction {
                        v.animate().alpha(0f).setDuration(200).withEndAction {
                            v.animate().alpha(1f).setDuration(200).withEndAction {

                                v.animate().alpha(0f).setDuration(250).withEndAction {
                                    v.text = ""
                                    v.alpha = 1f
                                    v.setTextColor(android.graphics.Color.WHITE)
                                    v.isSelected = true

                                    var currentRevealText = ""
                                    revealText.forEachIndexed { index, char ->
                                        v.postDelayed({
                                            currentRevealText += char
                                            v.text = currentRevealText

                                            if (index == revealText.length - 1) {
                                                v.postDelayed({

                                                    val originalText =
                                                        if (v.id == binding.tvTicker.id) {
                                                            prefs.getString(
                                                                "CUSTOM_CARRIER",
                                                                "TELKOMSEL"
                                                            )
                                                        } else {
                                                            prefs.getString(
                                                                "CUSTOM_MARQUEE",
                                                                "Running Text"
                                                            )
                                                        } ?: ""

                                                    v.animate().alpha(0f).setDuration(150)
                                                        .withEndAction {
                                                            v.text = ""
                                                            v.alpha = 1f
                                                            v.setTextColor(android.graphics.Color.WHITE)

                                                            var currentOriginalText = ""
                                                            originalText.forEachIndexed { i, c ->
                                                                v.postDelayed({
                                                                    currentOriginalText += c
                                                                    v.text = currentOriginalText

                                                                    if (i == originalText.length - 1) {
                                                                        isRevealed = false
                                                                        v.isSelected = true
                                                                    }
                                                                }, i * 40L)
                                                            }
                                                        }.start()

                                                }, 7000)
                                            }
                                        }, index * 40L)
                                    }
                                }.start()
                            }
                        }
                    }
                }
            }

            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            vibrator?.let { if (it.hasVibrator()) it.vibrate(50) }

        }, (revealDelay * 1000).toLong())
    }

    private fun finishPinUnlockWithAnimation() {
        val duration = 300L
        val interpolator = android.view.animation.AccelerateInterpolator()

        fun animateOut(v: View, transY: Float, transX: Float) {
            v.animate()
                .translationY(transY)
                .translationX(transX)
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .start()
        }

        binding.ivLockIcon.animate()
            .alpha(0f)
            .setDuration(duration)
            .start()

        animateOut(binding.tvEnterPinLabel, -400f, 0f)
        animateOut(binding.tvPinInfoLabel, -400f, 0f)
        animateOut(binding.pinIndicatorsLayout, -400f, 0f)
        animateOut(binding.bottomKeypadContainer, 600f, 0f)

        binding.tvBigClock.visibility = View.INVISIBLE
        binding.tvDate.visibility = View.INVISIBLE
        binding.ivLock.visibility = View.INVISIBLE
        binding.tvTicker.visibility = View.INVISIBLE
        binding.tvMarqueeBottom.visibility = View.INVISIBLE
        binding.statusBarContainer.visibility = View.INVISIBLE
        binding.bgPhone.visibility = View.INVISIBLE
        binding.ivPhone.visibility = View.INVISIBLE
        binding.bgCamera.visibility = View.INVISIBLE
        binding.ivCamera.visibility = View.INVISIBLE

        handler.postDelayed({
            finishAndRemoveTask()
            overridePendingTransition(0, android.R.anim.fade_out)
        }, duration)
    }
}

// kotlin

private fun blurBitmap(context: Context, bitmap: android.graphics.Bitmap, radius: Float): android.graphics.Bitmap {
    val clampedRadius = radius.coerceIn(0f, 25f)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val result = android.graphics.Bitmap.createBitmap(bitmap.width, bitmap.height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(result)
            val blurEffect = android.graphics.RenderEffect.createBlurEffect(clampedRadius, clampedRadius, android.graphics.Shader.TileMode.CLAMP)
            val paint = android.graphics.Paint().apply {
                this.isFilterBitmap = true
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var rs: android.renderscript.RenderScript? = null
    var inputAlloc: android.renderscript.Allocation? = null
    var outputAlloc: android.renderscript.Allocation? = null
    var script: android.renderscript.ScriptIntrinsicBlur? = null
    return try {
        rs = android.renderscript.RenderScript.create(context)
        inputAlloc = android.renderscript.Allocation.createFromBitmap(rs, bitmap)
        outputAlloc = android.renderscript.Allocation.createTyped(rs, inputAlloc.type)
        script = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs))
        script.setRadius(clampedRadius)
        script.setInput(inputAlloc)
        script.forEach(outputAlloc)
        val outBitmap = android.graphics.Bitmap.createBitmap(bitmap.width, bitmap.height, android.graphics.Bitmap.Config.ARGB_8888)
        outputAlloc.copyTo(outBitmap)
        outBitmap
    } catch (e: Exception) {
        e.printStackTrace()
        bitmap
    } finally {
        try { script?.destroy() } catch (_: Throwable) {}
        try { inputAlloc?.destroy() } catch (_: Throwable) {}
        try { outputAlloc?.destroy() } catch (_: Throwable) {}
        try { rs?.destroy() } catch (_: Throwable) {}
    }
}

