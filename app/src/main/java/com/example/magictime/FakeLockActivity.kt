package com.example.magictime

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RenderEffect
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.TranslateAnimation
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.magictime.databinding.ActivityFakeLockBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Suppress("DEPRECATION")
class FakeLockActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityFakeLockBinding
    private val prefs by lazy { getSharedPreferences("MagicPrefs", Context.MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector

    // === VARIABEL GENERAL & UI ===
    private var blurredWallpaperBitmap: Bitmap? = null
    private var screenHeight = 0
    private var screenWidth = 0
    private var touchStartX = 0f
    private var touchStartY = 0f
    private val swipeThreshold = 0.15f
    private var isSwipingForUnlock = false
    private var useIndonesianLanguage = true

    // === VARIABEL TIME TRAVEL ===
    private var isMagicActivated = false
    private var currentDisplayOffset = 0L
    private var delayStartMs = 0L
    private var timeSpeedMultiplier: Float = 1.0f
    private var baseRealTime: Long = 0L
    private var baseSyntheticTime: Long = 0L

    // === VARIABEL PIN & PREDICTION ===
    private var isPinEnabled = true
    private var correctPin = "888000"
    private var currentPinInput = ""
    private val maxPinLength = 6
    private var isSecretSetupMode = false
    private var secretSetupStep = 0
    private var secretCardValue = ""
    private var forceCardPrediction = ""
    private var isCardCodeEnabled = true

    // === VARIABEL SECRET REVEAL ===
    private var isRevealEnabled = false
    private var revealText = ""
    private var revealDelay = 3
    private var revealTarget = "BOTH"
    private var isRevealed = false
    private var revealDurationMs = 7000L

    // === VARIABEL AR FLOAT & SENSOR ===
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var floatActive = false
    private var floatScale = 100
    private var floatDelay = 0
    private var floatIsCustom = false
    private var floatUri: String? = null
    private var floatStockIndex = 0
    private var isBeingDragged = false
    private var velX = 0f
    private var velY = 0f
    private val friction = 0.80f
    private val sensitivity = 1.20f

    // === BATTERY RECEIVER ===
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
                        batteryPct <= 5 -> Color.RED
                        batteryPct <= 15 -> Color.YELLOW
                        else -> Color.WHITE
                    }
                    binding.tvBatteryLevel.background.setTint(bgColor)

                    val textColor = if (batteryPct <= 5) Color.WHITE else Color.BLACK
                    binding.tvBatteryLevel.setTextColor(textColor)
                }
            }
        }
    }

    // ==========================================
    // LIFECYCLE METHODS
    // ==========================================
    @RequiresApi(Build.VERSION_CODES.O)
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

        try { hideSystemUI() } catch (e: Exception) {}

        screenHeight = resources.displayMetrics.heightPixels
        screenWidth = resources.displayMetrics.widthPixels

        loadSettings()

        isPinEnabled = prefs.getBoolean("ENABLE_PIN", true)
        correctPin = prefs.getString("CUSTOM_PIN", "123456") ?: "123456"

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setupCardPhysicsAndTouch()
        loadWallpaper()
        setupInteractions()
        setupShortcuts()
        setupPinScreenInteractions()
        loadFloatConfigs()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
        startClockLoop()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
        sensorManager.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
    }

    // ==========================================
    // INITIALIZATION & SETTINGS
    // ==========================================
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

        // --- LOGIC WIFI & 4G/5G ---
        val isWifiOn = prefs.getBoolean("SHOW_WIFI", true)
        binding.ivWifi.visibility = if (isWifiOn) View.VISIBLE else View.GONE

        val plainColor = Color.parseColor("#4BFFFFFF")
        val use5G = prefs.getBoolean("USE_5G", false)
        val networkIconRes = if (use5G) R.drawable.ic_5g else R.drawable.ic_4g

        binding.iv4g1.setImageResource(networkIconRes)
        binding.iv4g2.setImageResource(networkIconRes)

        if (isWifiOn) {
            binding.iv4g1.visibility = View.GONE
            binding.ivSignal1.setColorFilter(plainColor)

            binding.iv4g2.visibility = View.GONE
            binding.ivSignal2.setColorFilter(plainColor)
        } else {
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

        isCardCodeEnabled = prefs.getBoolean("ENABLE_CARD_CODE", true) // Note: Flag is loaded but consider using it if needed in future logic
        revealDurationMs = prefs.getInt("REVEAL_DURATION", 7) * 1000L

        isRevealEnabled = prefs.getBoolean("ENABLE_REVEAL", false)
        revealText = prefs.getString("REVEAL_TEXT", "") ?: ""
        revealDelay = prefs.getInt("REVEAL_DELAY", 3)
        revealTarget = prefs.getString("REVEAL_TARGET", "BOTH") ?: "BOTH"

        val predLangCode = prefs.getString("PREDICTION_LANG", "id") ?: "id"
        useIndonesianLanguage = (predLangCode == "id")
    }

    private fun loadWallpaper() {
        val uriString = prefs.getString("WALLPAPER_URI", null)
        if (uriString != null) {
            try {
                binding.imgBackground.setImageURI(Uri.parse(uriString))
            } catch (e: Exception) {
                binding.imgBackground.setBackgroundColor(Color.BLACK)
            }
        } else {
            binding.imgBackground.setBackgroundColor(Color.BLACK)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sourceBitmap = if (uriString != null) {
                    MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(uriString))
                } else {
                    Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
                }

                val scaledBitmap = Bitmap.createScaledBitmap(sourceBitmap, sourceBitmap.width / 5, sourceBitmap.height / 5, false)
                blurredWallpaperBitmap = blurBitmap(this@FakeLockActivity, scaledBitmap, 20f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    // ==========================================
    // CLOCK & TIME LOGIC
    // ==========================================
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
        val now = System.currentTimeMillis() + (currentDisplayOffset * 1000)

        val is24Hour = prefs.getBoolean("IS_24H", true)
        val timePattern = if (is24Hour) "HH:mm" else "h:mm"
        val bigFormat = SimpleDateFormat(timePattern, Locale.getDefault())
        try {
            binding.tvBigClock.text = bigFormat.format(Date(now))
        } catch (e: Exception) {}

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
        } catch (e: Exception) {}
    }

    // ==========================================
    // MAGIC TRIGGERS & GESTURES
    // ==========================================
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
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (floatActive) {
                binding.imgFloatObject.removeCallbacks(null)
                binding.imgFloatObject.postDelayed({
                    showFloatingObject()
                }, floatDelay * 1000L)
            }
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (binding.imgFloatObject.visibility == View.VISIBLE) return true

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

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupInteractions() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (binding.imgFloatObject.visibility == View.VISIBLE) return true

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
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isSwipingForUnlock = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isSwipingForUnlock) return@setOnTouchListener false

                    val deltaX = touchStartX - event.rawX
                    val deltaY = touchStartY - event.rawY

                    val isSwipeDown = deltaY < 0 && Math.abs(deltaY) > Math.abs(deltaX)

                    if (!isSwipeDown) {
                        val distance = Math.hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()
                        val progress = (distance / (screenHeight / 1.5f)).coerceIn(0f, 1f)
                        applyInteractiveAnimation(progress)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isSwipingForUnlock) return@setOnTouchListener false
                    isSwipingForUnlock = false

                    val deltaX = touchStartX - event.rawX
                    val deltaY = touchStartY - event.rawY
                    val distance = Math.hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()

                    val isSwipeDown = deltaY < 0 && Math.abs(deltaY) > Math.abs(deltaX)

                    if (distance > screenHeight * swipeThreshold && !isSwipeDown) {
                        if (isPinEnabled) {
                            animateLockscreenToPinView()
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupShortcuts() {
        val openCameraAction = {
            try {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out)
            } catch (e: Exception) {
                try {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    }
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out)
                } catch (e: Exception) {}
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

                    handler.postDelayed({ binding.root.alpha = 1f }, 500)
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

                        if (Math.abs(validDx) >= triggerDistance) {
                            onTrigger()
                            v.translationX = 0f; v.scaleX = 1.0f; v.scaleY = 1.0f; v.alpha = 1.0f
                        } else {
                            v.animate()
                                .translationX(0f).translationY(0f)
                                .scaleX(1.0f).scaleY(1.0f).alpha(1.0f)
                                .setDuration(300)
                                .setInterpolator(OvershootInterpolator())
                                .start()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    // ==========================================
    // PIN & SECURITY
    // ==========================================
    private fun setupPinScreenInteractions() {
        for (i in 0..9) {
            val resId = resources.getIdentifier("btnPin$i", "id", packageName)
            if (resId != 0) {
                val btnLayout = findViewById<View>(resId)
                val tvNumber = btnLayout.findViewById<TextView>(R.id.tvPinNumber)
                val tvLetters = btnLayout.findViewById<TextView>(R.id.tvPinLetters)

                tvNumber.text = i.toString()
                tvLetters.text = when (i) {
                    2 -> "ABC"; 3 -> "DEF"; 4 -> "GHI"; 5 -> "JKL"; 6 -> "MNO"; 7 -> "PQRS"; 8 -> "TUV"; 9 -> "WXYZ"
                    else -> ""
                }

                btnLayout.setOnClickListener { onPinDigitEntered(i.toString()) }
            }
        }

        binding.ivLockIcon.setOnClickListener {
            vibratePattern(isDouble = false)
            hidePinScreen()
        }

        binding.btnOk.setOnLongClickListener {
            vibratePattern(isDouble = false)
            isSecretSetupMode = true
            secretSetupStep = 1
            currentPinInput = ""
            updatePinIndicators()
            true
        }

        binding.btnOk.setOnClickListener {
            if (isSecretSetupMode) {
                when (secretSetupStep) {
                    1 -> {
                        val input = currentPinInput.toIntOrNull() ?: 0
                        if (input in 1..13) {
                            secretCardValue = currentPinInput
                            vibratePattern(isDouble = false)
                            secretSetupStep = 2
                        } else {
                            isSecretSetupMode = false
                            secretSetupStep = 0
                        }
                        currentPinInput = ""
                        updatePinIndicators()
                    }
                    2 -> {
                        val input = currentPinInput.toIntOrNull() ?: 0
                        if (input in 1..4) {
                            generateCardPrediction(secretCardValue, input)
                            vibratePattern(isDouble = true)
                            val baseCustomText = prefs.getString("REVEAL_TEXT", "") ?: ""
                            revealText = if (baseCustomText.isNotEmpty()) {
                                "$baseCustomText $forceCardPrediction"
                            } else {
                                forceCardPrediction
                            }
                        }
                        isSecretSetupMode = false
                        secretSetupStep = 0
                        currentPinInput = ""
                        updatePinIndicators()
                    }
                }
            } else {
                validatePin()
            }
        }

        binding.btnDeletePin.setOnClickListener { onPinDelete() }

        binding.btnEmergencyPin.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun onPinDigitEntered(digit: String) {
        if (currentPinInput.length >= maxPinLength) return

        binding.tvPinError.visibility = View.GONE
        binding.tvPinError.clearAnimation()

        binding.tvEnterPinLabel.visibility = View.INVISIBLE
        binding.tvPinInfoLabel.visibility = View.INVISIBLE
        binding.pinIndicatorsLayout.visibility = View.VISIBLE

        currentPinInput += digit
        updatePinIndicators()

        if (currentPinInput.length == maxPinLength) {
            handler.postDelayed({ validatePin() }, 150)
        }
    }

    private fun updatePinIndicators() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4, binding.dot5, binding.dot6)
        for (i in dots.indices) {
            dots[i].setBackgroundResource(if (i < currentPinInput.length) R.drawable.bg_pin_dot_filled else R.drawable.bg_pin_dot_empty)
        }
    }

    private fun validatePin() {
        if (currentPinInput.length < maxPinLength) return

        if (currentPinInput == correctPin) {
            finishPinUnlockWithAnimation()
        } else {
            vibratePattern(isDouble = false)

            binding.pinIndicatorsLayout.visibility = View.INVISIBLE
            binding.tvPinInfoLabel.visibility = View.INVISIBLE

            binding.tvEnterPinLabel.translationY = 120f
            binding.tvEnterPinLabel.text = "Incorrect PIN"
            binding.tvEnterPinLabel.setTextColor(Color.WHITE)
            binding.tvEnterPinLabel.visibility = View.VISIBLE

            val shake = TranslateAnimation(-10f, 10f, 0f, 0f).apply {
                duration = 50; repeatCount = 5; repeatMode = android.view.animation.Animation.REVERSE
            }
            binding.tvEnterPinLabel.startAnimation(shake)

            currentPinInput = ""
            updatePinIndicators()

            handler.postDelayed({
                if (binding.tvEnterPinLabel.text == "Incorrect PIN") {
                    binding.tvEnterPinLabel.translationY = 0f
                    binding.tvEnterPinLabel.text = "Enter PIN"
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
        binding.tvEnterPinLabel.setTextColor(Color.WHITE)

        binding.tvEnterPinLabel.visibility = View.VISIBLE
        binding.tvPinInfoLabel.visibility = View.VISIBLE
        binding.pinIndicatorsLayout.visibility = View.INVISIBLE

        binding.pinScreenContainer.visibility = View.VISIBLE
        binding.pinScreenContainer.alpha = 0f
        binding.pinScreenContainer.translationY = 0f

        val startScale = 0.7f
        binding.bottomKeypadContainer.scaleX = startScale; binding.bottomKeypadContainer.scaleY = startScale; binding.bottomKeypadContainer.alpha = 0f
        binding.tvEnterPinLabel.scaleX = startScale; binding.tvEnterPinLabel.scaleY = startScale; binding.tvEnterPinLabel.alpha = 0f
        binding.tvPinInfoLabel.scaleX = startScale; binding.tvPinInfoLabel.scaleY = startScale; binding.tvPinInfoLabel.alpha = 0f

        binding.ivLockIcon.scaleX = startScale; binding.ivLockIcon.scaleY = startScale; binding.ivLockIcon.alpha = 0f

        blurredWallpaperBitmap?.let {
            binding.imgPinBackgroundBlurred.setImageBitmap(it)
            binding.imgPinBackgroundBlurred.setColorFilter(Color.parseColor("#99000000"), PorterDuff.Mode.SRC_OVER)
        }

        val duration = 350L
        val popInterpolator = OvershootInterpolator(1.2f)
        val fadeInterpolator = DecelerateInterpolator()

        binding.pinScreenContainer.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(fadeInterpolator)
            .start()

        fun popUpAnim(v: View, delay: Long) {
            v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(duration).setStartDelay(delay).setInterpolator(popInterpolator).start()
        }

        popUpAnim(binding.tvEnterPinLabel, 0)
        popUpAnim(binding.tvPinInfoLabel, 50)
        popUpAnim(binding.bottomKeypadContainer, 100)
        popUpAnim(binding.ivLockIcon, 150)
    }

    private fun hidePinScreen() {
        val duration = 250L
        val shrinkInterpolator = AnticipateInterpolator()

        fun shrinkOutAnim(v: View, delay: Long) {
            v.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .alpha(0f)
                .setStartDelay(delay)
                .setDuration(duration)
                .setInterpolator(shrinkInterpolator)
                .start()
        }

        shrinkOutAnim(binding.tvEnterPinLabel, 0)
        shrinkOutAnim(binding.tvPinInfoLabel, 20)
        shrinkOutAnim(binding.pinIndicatorsLayout, 40)
        shrinkOutAnim(binding.bottomKeypadContainer, 60)

        binding.ivLockIcon.animate()
            .scaleX(0f).scaleY(0f).alpha(0f)
            .setDuration(duration)
            .setInterpolator(shrinkInterpolator)
            .start()

        val lockscreenViews = listOf(
            binding.tvBigClock, binding.tvDate, binding.ivLock,
            binding.bgPhone, binding.ivPhone, binding.bgCamera, binding.ivCamera,
            binding.statusBarContainer, binding.tvTicker, binding.tvMarqueeBottom
        )
        lockscreenViews.forEach {
            it.visibility = View.VISIBLE
            it.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .translationY(0f)
                .translationX(0f)
                .setDuration(250L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        binding.pinScreenContainer.animate()
            .alpha(0f)
            .translationY(0f)
            .setStartDelay(100)
            .setDuration(200)
            .withEndAction {
                binding.pinScreenContainer.visibility = View.GONE
                currentPinInput = ""
                updatePinIndicators()

                binding.tvEnterPinLabel.scaleX = 1f
                binding.tvEnterPinLabel.scaleY = 1f
                binding.tvPinInfoLabel.scaleX = 1f
                binding.tvPinInfoLabel.scaleY = 1f
                binding.pinIndicatorsLayout.scaleX = 1f
                binding.pinIndicatorsLayout.scaleY = 1f
                binding.pinIndicatorsLayout.alpha = 1f
            }
            .start()
    }

    override fun onBackPressed() {
        if (binding.pinScreenContainer.visibility == View.VISIBLE) hidePinScreen() else super.onBackPressed()
    }

    private fun triggerSecretMessage() {
        if (!isRevealEnabled || isRevealed || revealText.isEmpty()) return
        isRevealed = true

        handler.postDelayed({
            val viewsToAnimate = mutableListOf<View>()
            when (revealTarget) {
                "CARRIER" -> viewsToAnimate.add(binding.tvTicker)
                "MARQUEE" -> viewsToAnimate.add(binding.tvMarqueeBottom)
                "BOTH" -> { viewsToAnimate.add(binding.tvTicker); viewsToAnimate.add(binding.tvMarqueeBottom) }
            }

            for (v in viewsToAnimate) {
                if (v !is TextView) continue

                v.animate().alpha(0f).setDuration(200).withEndAction {
                    v.animate().alpha(1f).setDuration(200).withEndAction {
                        v.animate().alpha(0f).setDuration(200).withEndAction {
                            v.animate().alpha(1f).setDuration(200).withEndAction {
                                v.animate().alpha(0f).setDuration(250).withEndAction {
                                    v.text = ""; v.alpha = 1f; v.setTextColor(Color.WHITE); v.isSelected = true
                                    var currentRevealText = ""

                                    val safeRevealText = revealText ?: ""

                                    safeRevealText.forEachIndexed { index, char ->
                                        v.postDelayed({
                                            currentRevealText += char
                                            v.text = currentRevealText

                                            if (index == safeRevealText.length - 1) {
                                                v.postDelayed({
                                                    val originalText = if (v.id == binding.tvTicker.id) {
                                                        prefs.getString("CUSTOM_CARRIER", "TELKOMSEL") ?: "TELKOMSEL"
                                                    } else {
                                                        prefs.getString("CUSTOM_MARQUEE", "Running Text") ?: "Running Text"
                                                    }

                                                    v.animate().alpha(0f).setDuration(150).withEndAction {
                                                        v.text = ""; v.alpha = 1f; v.setTextColor(Color.WHITE)
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
                                                }, revealDurationMs)
                                            }
                                        }, index * 40L)
                                    }
                                }.start()
                            }
                        }
                    }
                }
            }
            vibratePattern(isDouble = false)
        }, (revealDelay * 1000).toLong())
    }

    private fun finishPinUnlockWithAnimation() {
        val duration = 300L
        val interpolator = AccelerateInterpolator()

        binding.ivLockIcon.animate().alpha(0f).setDuration(duration).setInterpolator(interpolator).start()

        fun expandAndFadeOut(v: View) {
            v.animate().scaleX(1.5f).scaleY(1.5f).alpha(0f).setDuration(duration).setInterpolator(interpolator).start()
        }

        expandAndFadeOut(binding.tvEnterPinLabel); expandAndFadeOut(binding.tvPinInfoLabel)
        expandAndFadeOut(binding.pinIndicatorsLayout); expandAndFadeOut(binding.bottomKeypadContainer)

        val viewsToHide = listOf(
            binding.tvBigClock, binding.tvDate, binding.ivLock, binding.tvTicker,
            binding.tvMarqueeBottom, binding.statusBarContainer, binding.bgPhone,
            binding.ivPhone, binding.bgCamera, binding.ivCamera
        )
        viewsToHide.forEach { it.visibility = View.INVISIBLE }

        handler.postDelayed({
            finishAndRemoveTask()
            overridePendingTransition(0, android.R.anim.fade_out)
        }, duration)
    }

    // ==========================================
    // SENSOR & AR FLOAT LOGIC
    // ==========================================
    private fun showFloatingObject() {
        if (binding.imgFloatObject.visibility == View.VISIBLE) return

        loadFloatConfigs()
        vibratePattern(isDouble = false)

        binding.imgFloatObject.post {
            binding.imgFloatObject.x = (screenWidth / 2f) - (binding.imgFloatObject.width / 2f)
            binding.imgFloatObject.y = (screenHeight / 2f) - (binding.imgFloatObject.height / 2f)

            binding.imgFloatObject.alpha = 0f
            binding.imgFloatObject.visibility = View.VISIBLE
            binding.imgFloatObject.animate().alpha(1f).setDuration(1500).start()
        }
    }

    private fun loadFloatConfigs() {
        val fPrefs = getSharedPreferences("MagicTimePrefs", MODE_PRIVATE)
        floatActive = fPrefs.getBoolean("FLOAT_IS_ACTIVE", false)
        floatScale = try { fPrefs.getInt("FLOAT_SCALE", 100) } catch (e: Exception) { 100 }
        floatDelay = fPrefs.getInt("FLOAT_DELAY", 0)

        var isImageSet = false

        floatIsCustom = fPrefs.getBoolean("IS_GALLERY_MODE", false)
        floatUri = fPrefs.getString("FLOAT_CUSTOM_URI", null)

        if (floatIsCustom && floatUri != null) {
            try {
                binding.imgFloatObject.setImageURI(Uri.parse(floatUri))
                isImageSet = true
            } catch (t: Throwable) {
                t.printStackTrace()
                fPrefs.edit().putBoolean("IS_GALLERY_MODE", false).apply()
                fPrefs.edit().remove("FLOAT_CUSTOM_URI").apply()
                isImageSet = false
            }
        }

        if (!isImageSet) {
            try {
                val forcedCardName = prefs.getString("FORCED_FLOAT_CARD", null)
                if (forcedCardName != null) {
                    val resId = resources.getIdentifier(forcedCardName, "drawable", packageName)
                    if (resId != 0) {
                        binding.imgFloatObject.setImageResource(resId)
                        isImageSet = true
                    } else {
                        prefs.edit().remove("FORCED_FLOAT_CARD").apply()
                    }
                }
            } catch (t: Throwable) {
                prefs.edit().remove("FORCED_FLOAT_CARD").apply()
            }
        }

        if (!isImageSet) {
            try {
                binding.imgFloatObject.setImageResource(R.drawable.back_card)
            } catch (t: Throwable) { t.printStackTrace() }
        }

        try {
            val safeScreenWidth = if (screenWidth > 0) screenWidth.toFloat() else resources.displayMetrics.widthPixels.toFloat()
            val targetWidth = (floatScale / 100f) * safeScreenWidth
            val targetHeight = targetWidth * 1.41f

            val params = binding.imgFloatObject.layoutParams
            params.width = targetWidth.toInt()
            params.height = targetHeight.toInt()
            binding.imgFloatObject.layoutParams = params

            binding.imgFloatObject.scaleType = android.widget.ImageView.ScaleType.FIT_XY
            binding.imgFloatObject.requestLayout()
        } catch (t: Throwable) { t.printStackTrace() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCardPhysicsAndTouch() {
        binding.imgFloatObject.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isBeingDragged = true
                    velX = 0f; velY = 0f
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    v.x = event.rawX - v.width / 2
                    v.y = event.rawY - v.height / 2
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isBeingDragged = false

                    val isOverlapLeft = v.x < 0
                    val isOverlapRight = (v.x + v.width) > screenWidth
                    val isOverlapTop = v.y < 0
                    val isOverlapBottom = (v.y + v.height) > screenHeight

                    if (isOverlapLeft || isOverlapRight || isOverlapTop || isOverlapBottom) {
                        val destX = if (isOverlapLeft) -1500f else if (isOverlapRight) 1500f else 0f
                        val destY = if (isOverlapTop) -1500f else if (isOverlapBottom) 1500f else 0f

                        v.animate()
                            .translationXBy(destX).translationYBy(destY).alpha(0f)
                            .setDuration(180).setInterpolator(AccelerateInterpolator())
                            .withEndAction {
                                v.visibility = View.GONE
                                v.alpha = 1f
                                v.translationX = 0f; v.translationY = 0f
                                v.x = (screenWidth / 2 - v.width / 2).toFloat()
                                v.y = (screenHeight / 2 - v.height / 2).toFloat()
                            }.start()

                        vibratePattern(isDouble = false)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || isBeingDragged || binding.imgFloatObject.visibility != View.VISIBLE) return

        velX = (velX - (event.values[0] * sensitivity)) * friction
        velY = (velY + (event.values[1] * sensitivity)) * friction

        var newX = binding.imgFloatObject.x + velX
        var newY = binding.imgFloatObject.y + velY

        val safeScreenWidth = if (screenWidth > 0) screenWidth.toFloat() else resources.displayMetrics.widthPixels.toFloat()
        val safeScreenHeight = if (screenHeight > 0) screenHeight.toFloat() else resources.displayMetrics.heightPixels.toFloat()

        val objWidth = binding.imgFloatObject.width.toFloat()
        val objHeight = binding.imgFloatObject.height.toFloat()

        val minLimitX = if (safeScreenWidth > objWidth) 0f else safeScreenWidth - objWidth
        val maxLimitX = if (safeScreenWidth > objWidth) safeScreenWidth - objWidth else 0f
        val minLimitY = if (safeScreenHeight > objHeight) 0f else safeScreenHeight - objHeight
        val maxLimitY = if (safeScreenHeight > objHeight) safeScreenHeight - objHeight else 0f

        if (newX < minLimitX) { newX = minLimitX; velX = 0f }
        if (newX > maxLimitX) { newX = maxLimitX; velX = 0f }
        if (newY < minLimitY) { newY = minLimitY; velY = 0f }
        if (newY > maxLimitY) { newY = maxLimitY; velY = 0f }

        binding.imgFloatObject.x = newX
        binding.imgFloatObject.y = newY
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ==========================================
    // UTILS & ANIMATIONS
    // ==========================================
    private fun applyInteractiveAnimation(progress: Float) {
        val invertedAlpha = 1f - progress
        val translationUp = -300f * progress

        binding.tvBigClock.translationY = translationUp; binding.tvDate.translationY = translationUp; binding.ivLock.translationY = translationUp
        binding.tvBigClock.alpha = invertedAlpha; binding.tvDate.alpha = invertedAlpha; binding.ivLock.alpha = invertedAlpha

        binding.bgPhone.translationY = 200f * progress; binding.bgPhone.translationX = -150f * progress; binding.bgPhone.alpha = invertedAlpha
        binding.ivPhone.translationY = 200f * progress; binding.ivPhone.translationX = -150f * progress; binding.ivPhone.alpha = invertedAlpha
        binding.bgCamera.translationY = 200f * progress; binding.bgCamera.translationX = 150f * progress; binding.bgCamera.alpha = invertedAlpha
        binding.ivCamera.translationY = 200f * progress; binding.ivCamera.translationX = 150f * progress; binding.ivCamera.alpha = invertedAlpha

        binding.statusBarContainer.alpha = invertedAlpha
        binding.tvTicker.alpha = invertedAlpha
        binding.tvMarqueeBottom.alpha = invertedAlpha
    }

    private fun resetUnlockAnimation() {
        val duration = 300L
        val interpolator = OvershootInterpolator(1.2f)

        fun resetView(v: View) {
            v.animate().translationY(0f).translationX(0f).alpha(1f).setDuration(duration).setInterpolator(interpolator).start()
        }

        val views = listOf(binding.tvBigClock, binding.tvDate, binding.ivLock, binding.bgPhone, binding.ivPhone, binding.bgCamera, binding.ivCamera, binding.statusBarContainer, binding.tvTicker, binding.tvMarqueeBottom)
        views.forEach { resetView(it) }
    }

    private fun finishUnlockWithAnimation() {
        val duration = 250L
        val interpolator = AccelerateInterpolator()

        prefs.edit().remove("FORCED_FLOAT_CARD").apply()

        fun animateOut(v: View, transY: Float, transX: Float) {
            v.animate().translationY(transY).translationX(transX).alpha(0f).setDuration(duration).setInterpolator(interpolator).start()
        }

        animateOut(binding.tvBigClock, -500f, 0f); animateOut(binding.tvDate, -500f, 0f); animateOut(binding.ivLock, -500f, 0f)
        animateOut(binding.bgPhone, 400f, -300f); animateOut(binding.ivPhone, 400f, -300f)
        animateOut(binding.bgCamera, 400f, 300f); animateOut(binding.ivCamera, 400f, 300f)
        animateOut(binding.statusBarContainer, 0f, 0f); animateOut(binding.tvTicker, 0f, 0f); animateOut(binding.tvMarqueeBottom, 0f, 0f)

        handler.postDelayed({ finishAndRemoveTask(); overridePendingTransition(0, 0) }, duration + 50)
    }

    private fun animateLockscreenToPinView() {
        val duration = 250L
        val interpolator = AccelerateInterpolator()

        fun expandAndFade(v: View) {
            v.animate()
                .scaleX(1.5f)
                .scaleY(1.5f)
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .start()
        }

        val viewsToHide = listOf(
            binding.tvBigClock, binding.tvDate, binding.ivLock,
            binding.bgPhone, binding.ivPhone, binding.bgCamera, binding.ivCamera,
            binding.statusBarContainer, binding.tvTicker, binding.tvMarqueeBottom
        )

        viewsToHide.forEach { expandAndFade(it) }

        handler.postDelayed({
            performUnlock()
            viewsToHide.forEach { it.visibility = View.INVISIBLE }
        }, duration)
    }

    private fun generateCardPrediction(valueStr: String, suitCode: Int) {
        val valueInt = valueStr.toIntOrNull() ?: 1
        val suitLetter = when (suitCode) { 1 -> "d"; 2 -> "c"; 3 -> "h"; 4 -> "s"; else -> "s" }
        val drawableName = "$suitLetter$valueInt"
        prefs.edit().putString("FORCED_FLOAT_CARD", drawableName).apply()

        val valueEn = when (valueInt) { 1 -> "Ace"; 11 -> "Jack"; 12 -> "Queen"; 13 -> "King"; else -> valueStr }
        val valueId = when (valueInt) { 1 -> "As"; 11 -> "Jack"; 12 -> "Queen"; 13 -> "King"; else -> valueStr }
        val suitEn = when (suitCode) { 1 -> "Diamonds"; 2 -> "Clubs"; 3 -> "Hearts"; 4 -> "Spades"; else -> "" }
        val suitId = when (suitCode) { 1 -> "Wajik"; 2 -> "Keriting"; 3 -> "Hati"; 4 -> "Sekop"; else -> "" }

        forceCardPrediction = if (useIndonesianLanguage) "$valueId $suitId" else "$valueEn of $suitEn"
    }

    @SuppressLint("NewApi")
    private fun vibratePattern(isDouble: Boolean) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (isDouble) {
            val pattern = longArrayOf(0, 50, 100, 50)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                vibrator?.vibrate(pattern, -1)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator?.vibrate(50)
            }
        }
    }
}

private fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float): Bitmap {
    val clampedRadius = radius.coerceIn(0f, 25f)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val blurEffect = RenderEffect.createBlurEffect(clampedRadius, clampedRadius, Shader.TileMode.CLAMP)
            val paint = Paint().apply { isFilterBitmap = true }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            return result
        } catch (e: Exception) { e.printStackTrace() }
    }

    var rs: RenderScript? = null
    var inputAlloc: Allocation? = null
    var outputAlloc: Allocation? = null
    var script: ScriptIntrinsicBlur? = null
    return try {
        rs = RenderScript.create(context)
        inputAlloc = Allocation.createFromBitmap(rs, bitmap)
        outputAlloc = Allocation.createTyped(rs, inputAlloc.type)
        script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(clampedRadius)
        script.setInput(inputAlloc)
        script.forEach(outputAlloc)
        val outBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
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