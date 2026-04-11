package com.example.magictime

import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.magictime.databinding.ActivityEditLayoutBinding
import android.net.Uri
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.activity.OnBackPressedCallback
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import kotlin.text.get
import kotlin.text.set
import kotlin.text.toInt
import kotlin.times

class EditLayoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditLayoutBinding
    private lateinit var store: LayoutConfigStore
    private var activeView: View? = null
    private lateinit var prefManager: PreferenceManager
    private val minScalePercent = 50
    private val maxScalePercent = 200
    private val colorPurple = android.graphics.Color.parseColor("#5800D1")
    private val colorGray = android.graphics.Color.parseColor("#9E9E9E")
    private var lastCommittedConfig: LayoutConfig? = null
    private var defaultConfig: LayoutConfig? = null
    private val eps = 0.5f
    private val holdHandler = Handler(Looper.getMainLooper())
    private var holdRunnable: Runnable? = null
    private val holdStartDelayMs = 2000L
    private val holdRepeatMs = 40L
    private val defaultPos = mutableMapOf<Int, PointF>()
    private val defaultScale = mutableMapOf<Int, Float>()
    private var isCardRaised = false
    private var raisedTranslationY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        enforceImmersiveMode()
        loadWallpaperPreview()
        setupScaleEditor()
        setupSaveLoadButtons()
        setupExitButton()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitDialog()
                }
            }
        )

        store = LayoutConfigStore(this)

        binding.canvasRoot.post {
            applyExactDefaultsFromFakeLayout()
            captureDefaultsIfNeeded()
            restoreFromActiveOrDefault()
            defaultConfig = buildConfigFromCanvas()
            lastCommittedConfig = buildConfigFromCanvas()
            setupDragAndSelect()
        }

        setupPanelToggle()
        setupCardLiftButtons()
        setupReset()
        setupCanvasTapToClear()
        setupDpadHold()
        updateControlState()
    }

    private fun editableViews(): List<View> = listOf(
        binding.tvClockEdit,
        binding.tvDateEdit,
        binding.tvOperatorEdit,
        binding.tvMarqueeEdit,
        binding.statusBarEdit,
        binding.ivLockEdit,
        binding.phoneButtonEdit,
        binding.cameraButtonEdit
    )

    private fun applyExactDefaultsFromFakeLayout() {
        val fakeRoot = layoutInflater.inflate(R.layout.activity_fake_lock, null, false)

        val wSpec = View.MeasureSpec.makeMeasureSpec(binding.canvasRoot.width, View.MeasureSpec.EXACTLY)
        val hSpec = View.MeasureSpec.makeMeasureSpec(binding.canvasRoot.height, View.MeasureSpec.EXACTLY)
        fakeRoot.measure(wSpec, hSpec)
        fakeRoot.layout(0, 0, binding.canvasRoot.width, binding.canvasRoot.height)

        val tvBigClock = fakeRoot.findViewById<View>(R.id.tvBigClock)
        val tvDate = fakeRoot.findViewById<View>(R.id.tvDate)
        val tvTicker = fakeRoot.findViewById<View>(R.id.tvTicker)
        val tvMarquee = fakeRoot.findViewById<View>(R.id.tvMarqueeBottom)
        val statusBar = fakeRoot.findViewById<View>(R.id.statusBarContainer)
        val ivLock = fakeRoot.findViewById<View>(R.id.ivLock)
        val bgPhone = fakeRoot.findViewById<View>(R.id.bgPhone)
        val bgCamera = fakeRoot.findViewById<View>(R.id.bgCamera)

        binding.tvClockEdit.x = tvBigClock.x
        binding.tvClockEdit.y = tvBigClock.y

        binding.tvDateEdit.x = tvDate.x
        binding.tvDateEdit.y = tvDate.y

        binding.tvOperatorEdit.x = tvTicker.x
        binding.tvOperatorEdit.y = tvTicker.y

        binding.tvMarqueeEdit.x = tvMarquee.x
        binding.tvMarqueeEdit.y = tvMarquee.y

        binding.statusBarEdit.x = statusBar.x
        binding.statusBarEdit.y = statusBar.y

        binding.ivLockEdit.x = ivLock.x
        binding.ivLockEdit.y = ivLock.y

        binding.phoneButtonEdit.x = bgPhone.x
        binding.phoneButtonEdit.y = bgPhone.y

        binding.cameraButtonEdit.x = bgCamera.x
        binding.cameraButtonEdit.y = bgCamera.y
    }

    private fun applyLockscreenLikeDefaults() {
        val density = resources.displayMetrics.density
        val canvasH = binding.canvasRoot.height.toFloat()
        val canvasW = binding.canvasRoot.width.toFloat()

        val bias = 0.12f
        val bottomMarginPx = -130f * density

        binding.tvClockEdit.x = (canvasW - binding.tvClockEdit.width) / 2f
        binding.tvClockEdit.y = (canvasH - binding.tvClockEdit.height - bottomMarginPx) * bias

        val dateTopOffset = -20f * density
        binding.tvDateEdit.x = (canvasW - binding.tvDateEdit.width) / 2f
        binding.tvDateEdit.y = binding.tvClockEdit.y + binding.tvClockEdit.height + dateTopOffset
    }

    private fun setupDragAndSelect() {
        val drag = LockDragController(
            canvas = binding.canvasRoot,
            onSelected = { setActiveView(it) },
            onSnapGuide = { showV, showH ->
                binding.vGuideVertical.visibility = if (showV) View.VISIBLE else View.GONE
                binding.vGuideHorizontal.visibility = if (showH) View.VISIBLE else View.GONE
            },
            onMoved = { moved ->
                if (activeView == moved) moveActiveIndicator(moved)
            }
        )

        drag.attach(binding.tvOperatorEdit, SnapArea.TOP)
        drag.attach(binding.statusBarEdit, SnapArea.TOP)
        drag.attach(binding.tvClockEdit, SnapArea.CENTER)
        drag.attach(binding.tvDateEdit, SnapArea.CENTER)
        drag.attach(binding.tvMarqueeEdit, SnapArea.CENTER)
        drag.attach(binding.ivLockEdit, SnapArea.CENTER)
        drag.attach(binding.phoneButtonEdit, SnapArea.CENTER)
        drag.attach(binding.cameraButtonEdit, SnapArea.CENTER)

        editableViews().forEach { v ->
            v.setOnClickListener { onEditableTapped(v) }
        }
    }

    private fun setActiveView(v: View) {
        activeView = v
        editableViews().forEach {
            it.alpha = if (it == v) 1f else 0.78f
        }

        moveActiveIndicator(v)
        updateControlState()

        val currentPercent = (v.scaleX * 100f).toInt().coerceIn(minScalePercent, maxScalePercent)
        binding.etScalePercent.setText(currentPercent.toString())
        binding.seekScale.progress = currentPercent - minScalePercent
    }

    private fun setupDpadHold() {
        bindNudgeHold(binding.btnUp, 0f, -1f)
        bindNudgeHold(binding.btnDown, 0f, 1f)
        bindNudgeHold(binding.btnLeft, -1f, 0f)
        bindNudgeHold(binding.btnRight, 1f, 0f)
    }

    private fun bindNudgeHold(button: View, dx: Float, dy: Float) {
        button.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    nudgeActive(dx, dy)
                    holdRunnable = object : Runnable {
                        override fun run() {
                            nudgeActive(dx, dy)
                            holdHandler.postDelayed(this, holdRepeatMs)
                        }
                    }
                    holdHandler.postDelayed(holdRunnable!!, holdStartDelayMs)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    holdRunnable?.let { holdHandler.removeCallbacks(it) }
                    holdRunnable = null
                    v.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun nudgeActive(dx: Float, dy: Float) {
        val v = activeView ?: return
        v.x = (v.x + dx).coerceIn(0f, (binding.canvasRoot.width - v.width).toFloat())
        v.y = (v.y + dy).coerceIn(0f, (binding.canvasRoot.height - v.height).toFloat())
        activeView?.let { moveActiveIndicator(it) }
    }

    private fun setupPanelToggle() {
        binding.btnHideControls.setOnClickListener {
            val visible = binding.cardControls.visibility == View.VISIBLE
            binding.cardControls.visibility = if (visible) View.GONE else View.VISIBLE
            binding.btnShowControlsFloating.visibility = if (visible) View.VISIBLE else View.GONE
        }
        binding.btnShowControlsFloating.setOnClickListener {
            binding.cardControls.visibility = View.VISIBLE
            binding.btnShowControlsFloating.visibility = View.GONE
        }
    }

    private fun setupReset() {
        binding.btnReset.setOnClickListener {
            applyDefaults()
            store.clearActiveSlot()
            toast("Reset to default")
        }
    }

    private fun captureDefaultsIfNeeded() {
        if (defaultPos.isNotEmpty() && defaultScale.isNotEmpty()) return
        editableViews().forEach { v ->
            defaultPos[v.id] = PointF(v.x, v.y)
            defaultScale[v.id] = v.scaleX
        }
    }

    private fun applyDefaults() {
        editableViews().forEach { v ->
            defaultPos[v.id]?.let { p ->
                v.x = p.x
                v.y = p.y
            }
            val s = defaultScale[v.id] ?: 1f
            v.scaleX = s
            v.scaleY = s
        }

        activeView?.let { v ->
            moveActiveIndicator(v)
            val percent = (v.scaleX * 100f).toInt().coerceIn(minScalePercent, maxScalePercent)
            binding.etScalePercent.setText(percent.toString())
            binding.seekScale.progress = percent - minScalePercent
        } ?: run {
            binding.etScalePercent.setText("100")
            binding.seekScale.progress = 100 - minScalePercent
        }
    }

    private fun restoreFromActiveOrDefault() {
        val slot = store.getActiveSlot()
        val config = if (slot in 1..3) store.load(slot) else null
        if (config != null) applyConfig(config) else applyDefaults()
    }

    private fun buildConfigFromCanvas(): LayoutConfig {
        return LayoutConfig(
            clock = Pos(binding.tvClockEdit.x, binding.tvClockEdit.y, binding.tvClockEdit.scaleX),
            date = Pos(binding.tvDateEdit.x, binding.tvDateEdit.y, binding.tvDateEdit.scaleX),
            operator = Pos(binding.tvOperatorEdit.x, binding.tvOperatorEdit.y, binding.tvOperatorEdit.scaleX),
            marquee = Pos(binding.tvMarqueeEdit.x, binding.tvMarqueeEdit.y, binding.tvMarqueeEdit.scaleX),
            statusBar = Pos(binding.statusBarEdit.x, binding.statusBarEdit.y, binding.statusBarEdit.scaleX),
            lockIcon = Pos(binding.ivLockEdit.x, binding.ivLockEdit.y, binding.ivLockEdit.scaleX),
            phoneButton = Pos(binding.phoneButtonEdit.x, binding.phoneButtonEdit.y, binding.phoneButtonEdit.scaleX),
            cameraButton = Pos(binding.cameraButtonEdit.x, binding.cameraButtonEdit.y, binding.cameraButtonEdit.scaleX)
        )
    }
    private fun applyConfig(c: LayoutConfig) {
        binding.tvClockEdit.x = c.clock.x
        binding.tvClockEdit.y = c.clock.y
        binding.tvClockEdit.scaleX = c.clock.scale
        binding.tvClockEdit.scaleY = c.clock.scale

        binding.tvDateEdit.x = c.date.x
        binding.tvDateEdit.y = c.date.y
        binding.tvDateEdit.scaleX = c.date.scale
        binding.tvDateEdit.scaleY = c.date.scale

        binding.tvOperatorEdit.x = c.operator.x
        binding.tvOperatorEdit.y = c.operator.y
        binding.tvOperatorEdit.scaleX = c.operator.scale
        binding.tvOperatorEdit.scaleY = c.operator.scale

        binding.tvMarqueeEdit.x = c.marquee.x
        binding.tvMarqueeEdit.y = c.marquee.y
        binding.tvMarqueeEdit.scaleX = c.marquee.scale
        binding.tvMarqueeEdit.scaleY = c.marquee.scale

        binding.statusBarEdit.x = c.statusBar.x
        binding.statusBarEdit.y = c.statusBar.y
        binding.statusBarEdit.scaleX = c.statusBar.scale
        binding.statusBarEdit.scaleY = c.statusBar.scale

        c.lockIcon?.let {
            binding.ivLockEdit.x = it.x
            binding.ivLockEdit.y = it.y
            binding.ivLockEdit.scaleX = it.scale
            binding.ivLockEdit.scaleY = it.scale
        }

        c.phoneButton?.let {
            binding.phoneButtonEdit.x = it.x
            binding.phoneButtonEdit.y = it.y
            binding.phoneButtonEdit.scaleX = it.scale
            binding.phoneButtonEdit.scaleY = it.scale
        }

        c.cameraButton?.let {
            binding.cameraButtonEdit.x = it.x
            binding.cameraButtonEdit.y = it.y
            binding.cameraButtonEdit.scaleX = it.scale
            binding.cameraButtonEdit.scaleY = it.scale
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enforceImmersiveMode()
    }

    private fun enforceImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun loadWallpaperPreview() {
        val appSettings = prefManager.getActiveSession()
        val uri = appSettings.wallpaperPath

        if (!uri.isNullOrBlank()) {
            val ok = runCatching {
                binding.imgWallpaperEdit.setImageURI(Uri.parse(uri))
            }.isSuccess

            if (!ok) {
                binding.imgWallpaperEdit.setImageResource(R.drawable.default_bg)
            }
        } else {
            binding.imgWallpaperEdit.setImageResource(R.drawable.default_bg)
        }
    }

    private fun setupScaleEditor() {
        binding.seekScale.progress = 50

        binding.seekScale.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val percent = progress + minScalePercent
                binding.etScalePercent.setText(percent.toString())
                if (fromUser) applyScaleToActive(percent)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.etScalePercent.setOnEditorActionListener { v, actionId, event ->
            val done = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                            event.action == android.view.KeyEvent.ACTION_DOWN)

            if (done) {
                applyScaleFromInput()
                v.clearFocus()
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun applyScaleFromInput() {
        val v = activeView ?: return
        val raw = binding.etScalePercent.text?.toString()?.toIntOrNull() ?: 100
        val clamped = raw.coerceIn(minScalePercent, maxScalePercent)
        binding.etScalePercent.setText(clamped.toString())
        binding.seekScale.progress = clamped - minScalePercent
        val scale = clamped / 100f
        v.scaleX = scale
        v.scaleY = scale
        moveActiveIndicator(v)
    }

    private fun applyScaleToActive(percent: Int) {
        val v = activeView ?: return
        val scale = percent / 100f
        v.scaleX = scale
        v.scaleY = scale
    }

    private fun setupSaveLoadButtons() {
        binding.btnSaveLayout.setOnClickListener { showSaveDialog() }
        binding.btnLoadLayout.setOnClickListener { showLoadDialog() }
    }

    private fun showSaveDialog() {
        data class Item(val slot: Int, val title: String, val filled: Boolean) {
            override fun toString(): String = title
        }

        val items = listOf(
            Item(1, "Slot 1 ${if (store.load(1) != null) "(Filled)" else "(Empty)"}", store.load(1) != null),
            Item(2, "Slot 2 ${if (store.load(2) != null) "(Filled)" else "(Empty)"}", store.load(2) != null),
            Item(3, "Slot 3 ${if (store.load(3) != null) "(Filled)" else "(Empty)"}", store.load(3) != null)
        )

        val adapter = object : ArrayAdapter<Item>(this, android.R.layout.simple_list_item_1, items) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as android.widget.TextView
                val filled = items[position].filled
                tv.setTextColor(if (filled) colorPurple else colorGray)
                tv.alpha = if (filled) 1f else 0.45f
                return tv
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Save Layout")
            .setAdapter(adapter) { _, which ->
                val slot = items[which].slot
                store.save(slot, buildConfigFromCanvas())
                lastCommittedConfig = buildConfigFromCanvas()
                toast("Saved to Slot $slot")
            }
            .show()
    }

    private fun showLoadDialog() {
        data class Item(val title: String, val enabled: Boolean) {
            override fun toString(): String = title
        }

        val i1 = store.load(1) != null
        val i2 = store.load(2) != null
        val i3 = store.load(3) != null

        val items = listOf(
            Item("Slot 1 ${if (i1) "(Filled)" else "(Empty)"}", i1),
            Item("Slot 2 ${if (i2) "(Filled)" else "(Empty)"}", i2),
            Item("Slot 3 ${if (i3) "(Filled)" else "(Empty)"}", i3)
        )

        val adapter = object : ArrayAdapter<Item>(this, android.R.layout.simple_list_item_1, items) {
            override fun isEnabled(position: Int) = items[position].enabled
            override fun areAllItemsEnabled() = false

            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val v = super.getView(position, convertView, parent) as android.widget.TextView
                val item = items[position]
                v.setTextColor(if (item.enabled) colorPurple else colorGray)
                v.alpha = if (item.enabled) 1f else 0.45f
                return v
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Load Layout")
            .setAdapter(adapter) { _, which ->
                val slot = which + 1
                store.load(slot)?.let {
                    applyConfig(it)
                    store.setActiveSlot(slot)
                    lastCommittedConfig = buildConfigFromCanvas()
                    toast("Loaded Slot $slot")
                }
            }
            .show()
    }

    private fun setupExitButton() {
        binding.btnExitEditor.setOnClickListener { showExitDialog() }
    }


    private fun showExitDialog() {
        val current = buildConfigFromCanvas()
        val committed = lastCommittedConfig
        val hasChanges = committed == null || !isSameConfig(current, committed)
        val isDefaultNow = defaultConfig?.let { isSameConfig(current, it) } == true

        when {
            !hasChanges -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Exit Editor")
                    .setMessage("No unsaved changes.")
                    .setPositiveButton("Exit") { _, _ -> finish() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            isDefaultNow -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Exit Editor")
                    .setMessage("Default layout is not saved.")
                    .setPositiveButton("Save") { _, _ -> showSaveDialogThenExit() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            else -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Exit Editor")
                    .setMessage("Save layout before exit?")
                    .setPositiveButton("Save") { _, _ -> showSaveDialogThenExit() }
                    .setNeutralButton("Don't Save") { _, _ -> finish() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun showSaveDialogThenExit() {
        val items = arrayOf("Slot 1", "Slot 2", "Slot 3")
        MaterialAlertDialogBuilder(this)
            .setTitle("Save then Exit")
            .setItems(items) { _, which ->
                store.save(which + 1, buildConfigFromCanvas())
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onEditableTapped(v: View) {
        if (activeView == v) {
            clearActiveView()
        } else {
            setActiveView(v)
        }
    }

    private fun clearActiveView() {
        activeView = null
        editableViews().forEach { it.alpha = 1f }
        binding.viewActiveIndicator.visibility = View.GONE
        updateControlState()
    }

    private fun moveActiveIndicator(target: View) {
        val indicator = binding.viewActiveIndicator
        val pad = (6 * resources.displayMetrics.density).toInt()

        val lp = indicator.layoutParams
        lp.width = target.width + (pad * 2)
        lp.height = target.height + (pad * 2)
        indicator.layoutParams = lp

        indicator.x = target.x - pad
        indicator.y = target.y - pad
        indicator.visibility = View.VISIBLE
    }


    private fun updateControlState() {
        val enabled = activeView != null

        binding.seekScale.isEnabled = enabled
        binding.etScalePercent.isEnabled = enabled

        val alpha = if (enabled) 1f else 0.45f
        binding.seekScale.alpha = alpha
        binding.etScalePercent.alpha = alpha
    }

    private fun setupCanvasTapToClear() {
        binding.canvasRoot.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val tappedOnEditable = editableViews().any { isTouchInsideView(event.x, event.y, it) }
                if (!tappedOnEditable) {
                    clearActiveView()
                }
            }
            false
        }
    }

    private fun isTouchInsideView(x: Float, y: Float, view: View): Boolean {
        return x >= view.x && x <= (view.x + view.width) &&
                y >= view.y && y <= (view.y + view.height)
    }

    private fun prepareAbsoluteEditingLayer() {
        editableViews().forEach { v ->
            val absX = v.x
            val absY = v.y

            val lp = v.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                ?: return@forEach

            lp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            lp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            lp.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            lp.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            lp.startToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            lp.endToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            lp.topToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            lp.bottomToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET

            v.layoutParams = lp
            v.x = absX
            v.y = absY
        }
    }

    private fun isSameConfig(a: LayoutConfig, b: LayoutConfig): Boolean {
        return samePos(a.clock, b.clock) &&
                samePos(a.date, b.date) &&
                samePos(a.operator, b.operator) &&
                samePos(a.marquee, b.marquee) &&
                samePos(a.statusBar, b.statusBar) &&
                samePosNullable(a.lockIcon, b.lockIcon) &&
                samePosNullable(a.phoneButton, b.phoneButton) &&
                samePosNullable(a.cameraButton, b.cameraButton)
    }

    private fun samePosNullable(a: Pos?, b: Pos?): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        return samePos(a, b)
    }

    private fun samePos(a: Pos, b: Pos): Boolean {
        return kotlin.math.abs(a.x - b.x) <= eps &&
                kotlin.math.abs(a.y - b.y) <= eps &&
                kotlin.math.abs(a.scale - b.scale) <= 0.01f
    }

    private fun setupCardLiftButtons() {
        binding.cardControls.post {
            val byCardHeight = binding.cardControls.height * 1.2f
            val byCanvas = binding.canvasRoot.height * 1.2f
            raisedTranslationY = -kotlin.math.min(byCardHeight, byCanvas)
        }

        binding.btnCardUp.setOnClickListener {
            if (isCardRaised) return@setOnClickListener
            isCardRaised = true
            binding.cardControls.animate()
                .translationY(raisedTranslationY)
                .setDuration(220)
                .start()
        }

        binding.btnCardDown.setOnClickListener {
            if (!isCardRaised) return@setOnClickListener
            isCardRaised = false
            binding.cardControls.animate()
                .translationY(0f)
                .setDuration(220)
                .start()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}