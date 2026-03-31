package com.example.magictime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.magictime.databinding.ActivityConfigBinding
import com.google.android.gms.ads.MobileAds

class ConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigBinding
    private val prefs by lazy { getSharedPreferences("MagicPrefs", Context.MODE_PRIVATE) }
    private var offsetMinutes = 0
    private var delaySeconds = 5
    private var revealDelaySeconds = 3
    private var revealDurationSeconds = 7
    private lateinit var pickImage: ActivityResultLauncher<String>
    private var isUsingGalleryMode = false
    private var customImageUriString: String? = null
    private var currentFloatDelay = 0
    private lateinit var prefManager: PreferenceManager
    private lateinit var appSettings: AppSettings

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            customImageUriString = it.toString()
            isUsingGalleryMode = true
            binding.switchImageSource.isChecked = true

            refreshPreviewImage()
            updateDynamicPreviewScale(binding.seekBarFloatSize.progress)

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefManager = PreferenceManager(this)
        appSettings = prefManager.getActiveSession()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, imeInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        MobileAds.initialize(this) {

        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.tvToggleBox1.setOnClickListener { handleAccordion(1) }
        binding.tvToggleBox2.setOnClickListener { handleAccordion(2) }
        binding.tvToggleBox3.setOnClickListener { handleAccordion(3) }
        binding.tvToggleBox4.setOnClickListener { handleAccordion(4) }

        pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                prefs.edit().putString("WALLPAPER_URI", it.toString()).apply()
                try {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    binding.imgPreview.setImageURI(it)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }

        val speedAdapter = android.widget.ArrayAdapter.createFromResource(
            this,
            R.array.speed_options,
            R.layout.spinner_item
        )

        speedAdapter.setDropDownViewResource(R.layout.spinner_item)
        binding.spinnerTimeSpeed.adapter = speedAdapter

        binding.spinnerTimeSpeed.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedSpeed: Float = when (position) {
                    0 -> 1.35f; 1 -> 1.2f; 2 -> 1.0f; 3 -> 0.85f; 4 -> 0.7f; 5 -> 0.45f; 6 -> 0.2f
                    else -> 1.0f
                }
                prefs.edit().putFloat("TIME_SPEED", selectedSpeed).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val stackAdapter = android.widget.ArrayAdapter.createFromResource(
            this,
            R.array.stack_options,
            R.layout.spinner_item
        )
        stackAdapter.setDropDownViewResource(R.layout.spinner_item)
        binding.spinnerStackSystem.adapter = stackAdapter

        binding.spinnerStackSystem.setSelection(prefs.getInt("SELECTED_STACK", 0))

        binding.spinnerStackSystem.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("SELECTED_STACK", position).apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        loadSavedSettings()
        updateUIText()

        binding.rgTimeFormat.setOnCheckedChangeListener { _, checkedId ->
            prefs.edit().putBoolean("IS_24H", (checkedId == R.id.rbFormat24)).apply()
        }

        binding.rgDateLanguage.setOnCheckedChangeListener { _, checkedId ->
            prefs.edit().putString("DATE_LANGUAGE", if (checkedId == R.id.rbLangEN) "en" else "id").apply()
        }



        setupLivePreview(binding.etCustomCarrier, "Operator Text")
        setupLivePreview(binding.etCustomMarquee, "Running Text")

        binding.switchShowCarrier.setOnCheckedChangeListener { _, isChecked ->
            binding.etCustomCarrier.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.switchShowMarquee.setOnCheckedChangeListener { _, isChecked ->
            binding.etCustomMarquee.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.switchShowWifi.setOnCheckedChangeListener { _, isChecked ->
            binding.switchSignal1.isEnabled = !isChecked
            binding.switchSignal2.isEnabled = !isChecked

            binding.switchSignal1.alpha = if (isChecked) 0.5f else 1.0f
            binding.switchSignal2.alpha = if (isChecked) 0.5f else 1.0f
        }

        binding.switchEnablePin.setOnCheckedChangeListener { _, isCheckedStatus ->
            binding.etCustomPin.isEnabled = isCheckedStatus
            binding.etCustomPin.alpha = if (isCheckedStatus) 1.0f else 0.5f

            if (!isCheckedStatus) {
                prefs.edit().remove("FORCED_FLOAT_CARD").apply()
            }
            refreshPreviewImage()
            updateDynamicPreviewScale(binding.seekBarFloatSize.progress)
        }

        setupLivePreview(binding.etCustomPin, "PIN Predict")
        setupLivePreview(binding.etRevealText, "Secret Message")

        setupLivePreview(binding.etRevealText, "Secret Message")

        binding.btnRevealDelayPlus.setOnClickListener { revealDelaySeconds++; updateUIText() }
        binding.btnRevealDelayMinus.setOnClickListener { if (revealDelaySeconds > 0) revealDelaySeconds--; updateUIText() }
        binding.btnRevealDurationPlus.setOnClickListener { revealDurationSeconds++; updateUIText() }
        binding.btnRevealDurationMinus.setOnClickListener { if (revealDurationSeconds > 1) revealDurationSeconds--; updateUIText() }

        binding.rgTimeTrigger.setOnCheckedChangeListener { _, _ -> updateTriggerInfo() }

        binding.btnOffsetPlus.setOnClickListener { offsetMinutes++; updateUIText() }
        binding.btnOffsetMinus.setOnClickListener { offsetMinutes--; updateUIText() }
        binding.btnDelayPlus.setOnClickListener { delaySeconds++; updateUIText() }
        binding.btnDelayMinus.setOnClickListener { if (delaySeconds > 0) delaySeconds--; updateUIText() }

        binding.switchShakeTrigger.isChecked = prefs.getBoolean("USE_SHAKE_TRIGGER", false)
        binding.switchShakeTrigger.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("USE_SHAKE_TRIGGER", isChecked).apply()
        }

        binding.switchRedCardBack.setOnCheckedChangeListener { _, _ ->
            refreshPreviewImage()
        }

        binding.btnSetFloatCard.setOnClickListener {
            showCardSelectorDialog()
        }

        binding.switchImageSource.setOnCheckedChangeListener { _, isChecked ->
            isUsingGalleryMode = isChecked

            if (isChecked) {
                binding.btnInsertFloatImage.visibility = View.VISIBLE
                binding.btnSetFloatCard.visibility = View.GONE
            } else {
                binding.btnInsertFloatImage.visibility = View.GONE
                binding.btnSetFloatCard.visibility = View.VISIBLE
            }

            refreshPreviewImage()
            updateDynamicPreviewScale(binding.seekBarFloatSize.progress)
        }

        binding.btnInsertFloatImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.btnMinusFloatDelay.setOnClickListener {
            if (currentFloatDelay > 0) {
                currentFloatDelay--
                binding.tvFloatDelayValue.text = "${currentFloatDelay}"
            }
        }

        binding.btnPlusFloatDelay.setOnClickListener {
            if (currentFloatDelay < 30) {
                currentFloatDelay++
                binding.tvFloatDelayValue.text = "${currentFloatDelay}"
            }
        }

        binding.seekBarFloatSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val finalProgress = if (progress < 10) 10 else progress

                if (progress < 10 && seekBar != null) {
                    seekBar.progress = 10
                }

                binding.tvFloatSizeLabel.text = "Object Scale: $finalProgress%"

                updateDynamicPreviewScale(finalProgress)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        loadFloatSettings()

        binding.btnPickWallpaper.setOnClickListener { pickImage.launch("image/*") }

        binding.btnSaveAdvanced.setOnClickListener {
            saveFloatSettings()
            saveSettings()

            appSettings.currentStatusMode = "CUSTOM"
            prefManager.saveActiveSession(appSettings)

            android.widget.Toast.makeText(this, "Advanced Settings Saved!", android.widget.Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        saveFloatSettings()
    }

    private fun setupLivePreview(editText: EditText, label: String) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (editText.hasFocus()) {
                    binding.tvPreviewContent.text = s.toString()
                    saveSettings()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.cardTypingPreview.visibility = View.VISIBLE
                binding.tvPreviewLabel.text = "Editing: $label"
                binding.tvPreviewContent.text = editText.text.toString()
            } else {
                binding.cardTypingPreview.visibility = View.GONE
            }
        }

        editText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                v.clearFocus()

                binding.cardTypingPreview.visibility = View.GONE

                binding.root.requestFocus()

                true
            } else {
                false
            }
        }
    }

    private fun updateUIText() {
        val sign = if (offsetMinutes > 0) "+" else ""
        binding.tvOffsetValue.text = "$sign$offsetMinutes"
        binding.tvDelayValue.text = delaySeconds.toString()
        binding.tvRevealDurationValue.text = revealDurationSeconds.toString()
        binding.tvRevealDelayValue.text = revealDelaySeconds.toString()
        updateTriggerInfo()
    }

    private fun updateTriggerInfo() {
        binding.tvTriggerInfo.text = if (binding.rbVolume.isChecked) {
            "Info: Time uses Volume Down, so SECRET MESSAGE uses DOUBLE TAP."
        } else {
            "Info: Time uses Double Tap, so SECRET MESSAGE uses VOLUME DOWN."
        }
    }

    private fun loadSavedSettings() {
        binding.switchTimeTravel.setOnCheckedChangeListener(null)

        binding.switchTimeTravel.isChecked = appSettings.activeRoutines.contains("TIMEJUMP")
        offsetMinutes = appSettings.timeJumpOffset
        delaySeconds = (appSettings.globalDelay / 1000).toInt()

        appSettings.wallpaperPath?.let {
            try { binding.imgPreview.setImageURI(Uri.parse(it)) } catch (e: Exception) {}
        }

        binding.etCustomCarrier.setText(appSettings.operatorText)
        binding.etCustomMarquee.setText(appSettings.marqueeText)
        binding.switchShowCarrier.isChecked = appSettings.showOperator
        binding.switchShowMarquee.isChecked = appSettings.showRunningText
        binding.etCustomCarrier.visibility = if (appSettings.showOperator) View.VISIBLE else View.GONE
        binding.etCustomMarquee.visibility = if (appSettings.showRunningText) View.VISIBLE else View.GONE
        binding.rbFormat24.isChecked = appSettings.is24HourFormat
        binding.rbFormat12.isChecked = !appSettings.is24HourFormat
        binding.rbLangEN.isChecked = (appSettings.dateLanguage == "en")
        binding.rbLangID.isChecked = !binding.rbLangEN.isChecked

        val position = when (appSettings.timeFlowSpeed) { 1.35f->0; 1.2f->1; 1.0f->2; 0.85f->3; 0.7f->4; 0.45f->5; 0.2f->6; else->2 }
        binding.spinnerTimeSpeed.setSelection(position)

        val stackPos = when (appSettings.stackSystem) { "Mnemonica" -> 1; "Aronson" -> 2; else -> 0 }
        binding.spinnerStackSystem.setSelection(stackPos)

        binding.switchEnablePin.isChecked = appSettings.isPinEnabled
        binding.etCustomPin.isEnabled = appSettings.isPinEnabled
        binding.etCustomPin.alpha = if (appSettings.isPinEnabled) 1.0f else 0.5f
        binding.switchEnableReveal.isChecked = appSettings.activeRoutines.contains("PREDICTION")
        binding.rbPredLangEN.isChecked = (appSettings.predictionLanguage == "en")
        binding.rbPredLangID.isChecked = !binding.rbPredLangEN.isChecked

        when(appSettings.predictionTarget) {
            "CARRIER" -> binding.rbTargetCarrier.isChecked = true
            "MARQUEE" -> binding.rbTargetMarquee.isChecked = true
            else -> binding.rbTargetBoth.isChecked = true
        }

        val isProfileMode = (appSettings.currentStatusMode == "PRESET") || (appSettings.currentStatusMode == "LOADED")

        binding.rbVolume.isChecked = if (isProfileMode) appSettings.isVolumeTriggerForTime else prefs.getBoolean("TRIGGER_VOLUME", true)
        binding.rbDoubleTap.isChecked = !binding.rbVolume.isChecked

        if (isProfileMode) {
            val netMode = appSettings.networkMode.uppercase()
            val isWifi = netMode.contains("WIFI")
            val use5g = netMode.contains("5G")
            val sim1 = netMode.contains("SIM1") || netMode.contains("DUAL") || (!isWifi)
            val sim2 = netMode.contains("SIM2") || netMode.contains("DUAL")

            binding.switchShowWifi.isChecked = isWifi
            binding.switch5G.isChecked = use5g
            binding.switchSignal1.isChecked = sim1
            binding.switchSignal2.isChecked = sim2

            binding.switchSignal1.isEnabled = !isWifi
            binding.switchSignal2.isEnabled = !isWifi
            binding.switchSignal1.alpha = if (isWifi) 0.5f else 1.0f
            binding.switchSignal2.alpha = if (isWifi) 0.5f else 1.0f

            revealDurationSeconds = (appSettings.predictionDuration / 1000L).toInt()
        } else {
            val isWifiOn = prefs.getBoolean("SHOW_WIFI", true)
            binding.switchShowWifi.isChecked = isWifiOn
            binding.switchSignal1.isEnabled = !isWifiOn
            binding.switchSignal2.isEnabled = !isWifiOn
            binding.switchSignal1.alpha = if (isWifiOn) 0.5f else 1.0f
            binding.switchSignal2.alpha = if (isWifiOn) 0.5f else 1.0f
            binding.switch5G.isChecked = prefs.getBoolean("USE_5G", false)
            binding.switchSignal1.isChecked = prefs.getBoolean("SIM1_4G", true)
            binding.switchSignal2.isChecked = prefs.getBoolean("SIM2_4G", false)

            revealDurationSeconds = prefs.getInt("REVEAL_DURATION", 7)
        }

        binding.etCustomPin.setText(prefs.getString("CUSTOM_PIN", "123456"))
        binding.etRevealText.setText(prefs.getString("REVEAL_TEXT", ""))
        revealDelaySeconds = prefs.getInt("REVEAL_DELAY", 3)

        updateUIText()
    }

    private fun saveSettings() {
        val routines = appSettings.activeRoutines.toMutableSet()
        if (binding.switchTimeTravel.isChecked) routines.add("TIMEJUMP") else routines.remove("TIMEJUMP")
        if (binding.switchEnableReveal.isChecked) routines.add("PREDICTION") else routines.remove("PREDICTION")
        appSettings.activeRoutines = routines

        appSettings.timeJumpOffset = offsetMinutes
        appSettings.globalDelay = delaySeconds * 1000L
        appSettings.operatorText = binding.etCustomCarrier.text.toString()
        appSettings.marqueeText = binding.etCustomMarquee.text.toString()
        appSettings.showOperator = binding.switchShowCarrier.isChecked
        appSettings.showRunningText = binding.switchShowMarquee.isChecked
        appSettings.is24HourFormat = binding.rbFormat24.isChecked
        appSettings.isPinEnabled = binding.switchEnablePin.isChecked
        appSettings.dateLanguage = if (binding.rbLangEN.isChecked) "en" else "id"
        appSettings.predictionLanguage = if (binding.rbPredLangEN.isChecked) "en" else "id"
        appSettings.predictionTarget = when(binding.rgRevealTarget.checkedRadioButtonId) {
            R.id.rbTargetCarrier -> "CARRIER"; R.id.rbTargetMarquee -> "MARQUEE"; else -> "BOTH"
        }

        appSettings.timeFlowSpeed = when (binding.spinnerTimeSpeed.selectedItemPosition) {
            0 -> 1.35f; 1 -> 1.2f; 2 -> 1.0f; 3 -> 0.85f; 4 -> 0.7f; 5 -> 0.45f; 6 -> 0.2f; else -> 1.0f
        }
        appSettings.stackSystem = when(binding.spinnerStackSystem.selectedItemPosition) {
            1 -> "Mnemonica"; 2 -> "Aronson"; else -> "Bart Harding"
        }

        appSettings.isVolumeTriggerForTime = binding.rbVolume.isChecked

        val editor = prefs.edit()
        editor.putBoolean("TRIGGER_VOLUME", binding.rbVolume.isChecked)
        editor.putBoolean("SHOW_WIFI", binding.switchShowWifi.isChecked)
        editor.putBoolean("SIM1_4G", binding.switchSignal1.isChecked)
        editor.putBoolean("SIM2_4G", binding.switchSignal2.isChecked)
        editor.putBoolean("USE_5G", binding.switch5G.isChecked)
        val pinInput = binding.etCustomPin.text.toString()
        editor.putString("CUSTOM_PIN", if (pinInput.isNotEmpty()) pinInput else "123456")
        editor.putInt("REVEAL_DURATION", revealDurationSeconds)
        editor.putString("REVEAL_TEXT", binding.etRevealText.text.toString())
        editor.putInt("REVEAL_DELAY", revealDelaySeconds)
        editor.apply()
    }

    private fun loadFloatSettings() {
        val fPrefs = getSharedPreferences("MagicTimePrefs", MODE_PRIVATE)

        binding.switchFloatEffect.isChecked = appSettings.activeRoutines.contains("FLOAT")
        binding.switchShakeTrigger.isChecked = appSettings.isShakeTriggerEnabled
        binding.switchRedCardBack.isChecked = appSettings.useRedCardBack
        binding.seekBarFloatSize.progress = (appSettings.objectScale * 100).toInt()

        customImageUriString = appSettings.floatTargetCardPath

        currentFloatDelay = fPrefs.getInt("FLOAT_DELAY", 0)
        binding.tvFloatDelayValue.text = "${currentFloatDelay}"
        isUsingGalleryMode = fPrefs.getBoolean("IS_GALLERY_MODE", false)
        binding.switchImageSource.isChecked = isUsingGalleryMode

        refreshPreviewImage()
        updateDynamicPreviewScale(binding.seekBarFloatSize.progress)

        binding.switchLivePreview.isChecked = false
        binding.framePhonePreview.visibility = View.GONE

        binding.switchLivePreview.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.framePhonePreview.visibility = View.VISIBLE
                refreshPreviewImage()
                updateDynamicPreviewScale(binding.seekBarFloatSize.progress)
            } else {
                binding.framePhonePreview.visibility = View.GONE
            }
        }

        val speedMode = fPrefs.getString("FLOAT_SPEED_MODE", "MEDIUM")
        when (speedMode) {
            "SLOW" -> binding.rbSpeedSlow.isChecked = true
            "FAST" -> binding.rbSpeedFast.isChecked = true
            else -> binding.rbSpeedMedium.isChecked = true
        }
    }

    private fun saveFloatSettings() {
        val routines = appSettings.activeRoutines.toMutableSet()
        if (binding.switchFloatEffect.isChecked) routines.add("FLOAT") else routines.remove("FLOAT")
        appSettings.activeRoutines = routines

        appSettings.isShakeTriggerEnabled = binding.switchShakeTrigger.isChecked
        appSettings.useRedCardBack = binding.switchRedCardBack.isChecked
        appSettings.objectScale = binding.seekBarFloatSize.progress / 100f
        appSettings.floatTargetCardPath = customImageUriString

        val editor = getSharedPreferences("MagicTimePrefs", MODE_PRIVATE).edit()
        editor.putInt("FLOAT_DELAY", currentFloatDelay)
        editor.putBoolean("IS_GALLERY_MODE", isUsingGalleryMode)

        val speedMode = when (binding.rgFloatSpeed.checkedRadioButtonId) {
            R.id.rbSpeedSlow -> "SLOW"
            R.id.rbSpeedFast -> "FAST"
            else -> "MEDIUM"
        }
        editor.putString("FLOAT_SPEED_MODE", speedMode)
        editor.remove("FLOAT_STOCK_INDEX")
        editor.apply()
    }

    private fun handleAccordion(selectedBox: Int) {
        val layouts = arrayOf(binding.layoutHiddenBox1, binding.layoutHiddenBox2, binding.layoutHiddenBox3, binding.layoutHiddenBox4)
        val buttons = arrayOf(binding.tvToggleBox1, binding.tvToggleBox2, binding.tvToggleBox3, binding.tvToggleBox4)

        val targetPos = selectedBox - 1
        val isTargetAlreadyOpen = layouts[targetPos].visibility == View.VISIBLE

        android.transition.TransitionManager.beginDelayedTransition(
            binding.mainScrollView as android.view.ViewGroup,
            android.transition.AutoTransition()
        )

        for (i in layouts.indices) {
            layouts[i].visibility = View.GONE
            buttons[i].text = "See More ▾"
        }

        if (!isTargetAlreadyOpen) {
            layouts[targetPos].visibility = View.VISIBLE
            buttons[targetPos].text = "Hide Settings ▴"
        }
    }

    private fun refreshPreviewImage() {
        var imageSet = false

        if (isUsingGalleryMode && customImageUriString != null) {
            try {
                binding.imgDynamicFloatPreview.setImageURI(Uri.parse(customImageUriString))
                imageSet = true
            } catch (e: Exception) { e.printStackTrace() }
        }

        if (!imageSet && binding.switchEnablePin.isChecked) {
            val forcedCard = prefs.getString("FORCED_FLOAT_CARD", null)
            if (forcedCard != null) {
                val resId = resources.getIdentifier(forcedCard, "drawable", packageName)
                if (resId != 0) {
                    binding.imgDynamicFloatPreview.setImageResource(resId)
                    imageSet = true
                }
            }
        }

        if (!imageSet) {
            try {
                if (binding.switchRedCardBack.isChecked) {
                    binding.imgDynamicFloatPreview.setImageResource(R.drawable.back_card_red)
                } else {
                    binding.imgDynamicFloatPreview.setImageResource(R.drawable.back_card_blue)
                }
            } catch (e: Exception) {}
        }
    }

    private fun updateDynamicPreviewScale(progress: Int) {
        val frameWidth = binding.framePhonePreview.width

        if (frameWidth > 0) {
            applyPreviewSize(progress, frameWidth.toFloat())
        } else {
            binding.framePhonePreview.viewTreeObserver.addOnGlobalLayoutListener(
                object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        binding.framePhonePreview.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        applyPreviewSize(progress, binding.framePhonePreview.width.toFloat())
                    }
                }
            )
        }
    }

    private fun applyPreviewSize(progress: Int, frameWidth: Float) {
        val safeProgress = if (progress < 10) 10 else progress
        val targetWidth = (safeProgress / 100f) * frameWidth
        val targetHeight = targetWidth * 1.41f

        val params = binding.imgDynamicFloatPreview.layoutParams
        params.width = targetWidth.toInt()
        params.height = targetHeight.toInt()
        binding.imgDynamicFloatPreview.layoutParams = params

        binding.imgDynamicFloatPreview.requestLayout()
    }

    private fun showCardSelectorDialog() {
        val scrollView = android.widget.ScrollView(this)
        val gridLayout = android.widget.GridLayout(this).apply {
            columnCount = 4
            setPadding(32, 32, 32, 32)
        }
        scrollView.addView(gridLayout)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Choose a Prediction Card")
            .setView(scrollView)
            .setNegativeButton("Cancel", null)
            .create()

        val suits = listOf("c", "d", "h", "s")

        for (suit in suits) {
            for (value in 1..13) {
                val cardName = "$suit$value"
                val resId = resources.getIdentifier(cardName, "drawable", packageName)

                if (resId != 0) {
                    val imageView = android.widget.ImageView(this).apply {
                        setImageResource(resId)
                        adjustViewBounds = true

                        layoutParams = android.widget.GridLayout.LayoutParams().apply {
                            width = 0
                            height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                            columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                            setMargins(16, 16, 16, 16)
                        }

                        setOnClickListener {
                            prefs.edit().putString("FORCED_FLOAT_CARD", cardName).apply()

                            isUsingGalleryMode = false
                            binding.switchImageSource.isChecked = false

                            refreshPreviewImage()
                            updateDynamicPreviewScale(binding.seekBarFloatSize.progress)

                            dialog.dismiss()
                        }
                    }
                    gridLayout.addView(imageView)
                }
            }
        }

        dialog.show()
    }

    private fun showSecretManualDialog() {
        val manualHtml = """
            <font color="#5800D1"><b><u>[1] FEATURES & SETTINGS (DASHBOARD)</u></b></font><br>
            <b><u>A. TIME TRAVEL</u></b><br>
            &#8226; <i>Enable Time Travel:</i> Master switch to activate the clock manipulation illusion.<br>
            &#8226; <i>Trigger Method:</i> Choose whether to initiate the jump using Volume Down or Double Tap.<br>
            &#8226; <i>Delay Start:</i> How many seconds the app waits before visually changing the time after the trigger is pressed.<br>
            &#8226; <i>Time Flow Speed:</i> Controls the animation speed of the numbers rolling.<br>
            &#8226; <i>Time Format:</i> Toggle between 24-Hour and 12-Hour (AM/PM) display.<br><br>
            
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━</font><br><br>
            
            <b><u>B. PIN & SECRET MESSAGE</u></b><br>
            &#8226; <i>Enable PIN:</i> Activates the fake lockscreen keypad. (Note: If disabled, AR Float will only display the card back).<br>
            &#8226; <i>Custom PIN:</i> Set your unlock code (Default: 123456). Entering this normally simply unlocks the screen.<br>
            &#8226; <i>Prediction Language:</i> Choose English or Indonesian for the predicted card text.<br>
            &#8226; <i>Card Stack System:</i> Choose the memorized deck system (Bart Harding, Mnemonica, or Aronson) for your prediction routines.<br>
            &#8226; <i>Enable Secret Message:</i> Master switch for the text reveal prediction.<br>
            &#8226; <i>Message Trigger:</i> Automatically bound to the opposite of your Time Jump trigger.<br>
            &#8226; <i>Change Target:</i> Inject the prediction into the Carrier (Top Left), Marquee (Bottom Running Text), or Both.<br>
            &#8226; <i>Reveal Delay & Duration:</i> Set the wait time before the text changes, and how many seconds it stays visible.<br><br>
            
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━</font><br><br>
            
            <b><u>C. AR FLOAT OBJECT</u></b><br>
            &#8226; <i>Enable Float Effect:</i> Activates the invisible floating layer revealed by swiping.<br>
            &#8226; <i>Use Shake Trigger:</i> Replaces the default Volume Up trigger with a physical device shake. A successful shake secretly provides a double haptic feedback (2x vibration) before the delay starts. Volume Up remains completely silent.<br>
            &#8226; <i>Card Backs:</i> Defaults to Blue. Toggle 'Use Red Card Back' to change it.<br>
            &#8226; <i>Image Source:</i> Toggle between the built-in playing cards or pick a custom image from your gallery.<br>
            &#8226; <i>Object Scale & Preview:</i> Adjust the slider to resize the object. The live preview frame shows exact proportions.<br>
            &#8226; <i>Float Delay:</i> Add a delay before the object becomes fully visible/interactable after the swipe.<br>
            &#8226; <i>Speed Mode:</i> Adjust how fast the object moves dynamically across the screen.<br><br>
            
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━</font><br><br>
            
            <b><u>D. GENERAL SETTINGS</u></b><br>
            &#8226; <i>Wallpaper:</i> Pick a background image. Defaults to a pitch-black screen if left empty.<br>
            &#8226; <i>Date Language:</i> Switch the lockscreen date format (English/Indonesian).<br>
            &#8226; <i>Status Bar (Wi-Fi):</i> Turning ON the 'Show Wi-Fi' toggle automatically hides the cellular data (4G/5G) indicators for realism.<br>
            &#8226; <i>Cellular Network:</i> Toggle 'Show 4G/5G' on SIM 1 or 2. If OFF, only signal bars are shown. Toggle 'Enable 5G Icon' to fake a 5G connection instead of 4G.<br>
            &#8226; <i>Custom Text:</i> 'Operator Text' modifies the Carrier name. 'Running Text' modifies the Marquee text.<br><br>

            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>

            <font color="#5800D1"><b><u>[2] HARDWARE CONTROLS</u></b></font><br>
            &#8226; <b>Hold 'OK' Button (PIN View):</b> Silently activates Secret Mode, allowing you to input hidden PIN commands.<br>
            &#8226; <b>Physical Lock Button (Lockscreen):</b> Instantly cuts off the running text if you need to stop it cleanly mid-performance.<br>
            &#8226; <b>Physical Lock Button (PIN View):</b> Acts as a 'Back' button to return to the lockscreen.<br><br>
            
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>

            <font color="#5800D1"><b><u>[3] SECRET PIN INPUTS (THE HIDDEN CORE)</u></b></font><br>
            These commands are NOT in the dashboard settings. To execute them, you MUST <b>HOLD the 'OK' button</b> to activate Secret Mode. The system will silently listen for your input without giving visual feedback.<br><br>
            
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━</font><br><br>
            
            <b><u>A. TIME JUMP INJECTION (DYNAMIC OFFSET)</u></b><br>
            <i>Function: Silently injects a custom time offset (in minutes) on the fly before triggering the Time Travel illusion.</i><br>
            &#8226; <b>Formula:</b> Hold OK &#8594; 000 &#8594; <b>OK</b> &#8594; [Minutes] &#8594; <b>OK</b><br>
            &#8226; <i>Future (+8 mins):</i> Enter 8<br>
            &#8226; <i>Past (-8 mins):</i> Enter 08<br><br>
            
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━</font><br><br>
            
            <b><u>B. SET AR FLOAT CARD (FORCED PREDICTION)</u></b><br>
            <i>Function: Silently forces the AR Floating Object to display a specific playing card, overriding the gallery/dashboard settings.</i><br>
            &#8226; <b>Formula:</b> Hold OK &#8594; [Card Value] &#8594; <b>OK</b> &#8594; [Card Suit] &#8594; <b>OK</b><br>
            &#8226; <i>Values:</i> Ace=1, Jack=11, Queen=12, King=13 (Numbers 2-10 are normal)<br>
            &#8226; <i>Suits:</i> 1=Diamonds, 2=Clubs, 3=Hearts, 4=Spades<br>
            &#8226; <i>Example (Queen of Hearts):</i> Hold OK &#8594; 12 &#8594; OK &#8594; 3 &#8594; OK<br><br>
            
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━</font><br><br>

            <b><u>C. CARD STACK SYSTEM (MARQUEE TEXT REVEAL & BACK CARD FORCING)</u></b><br>
            <i>Function: Injects specific stack information into the running text or carrier as a prediction. The stack used will depend on your selection in the dashboard (Bart Harding, Mnemonica, or Aronson).</i><br>
            &#8226; <b>Show Full Stack & Force Back Card:</b> Hold OK &#8594; 0 &#8594; OK &#8594; 0 &#8594; OK<br>
            <i>(Note: This command has a dual effect. Not only does it reveal the stack in the text, but it also temporarily forces the AR Float to show a facedown 'Back Card' without saving it to memory. The color of the back card will follow your dashboard settings.)</i><br>
            &#8226; <b>Show Card at Position (1-52):</b> Hold OK &#8594; 0 &#8594; OK &#8594; [Position] &#8594; OK<br>
            &#8226; <b>Find Position of a Card:</b> Hold OK &#8594; 0[Value] &#8594; OK &#8594; [Suit] &#8594; OK <i>(e.g., 08 &#8594; OK &#8594; 3 &#8594; OK)</i><br>
            &#8226; <b>Find Cards Before & After:</b> Hold OK &#8594; 00[Value] &#8594; OK &#8594; [Suit] &#8594; OK <i>(e.g., 008 &#8594; OK &#8594; 3 &#8594; OK)</i><br><br>

            <font color="#777777"><i>*Bart Harding Stack Reference:*</i></font><br>
            <font size="2">10c, 7h, 4s, Ad, Jd, 6c, 7c, 9s, 6d, Ac, Jc, 8h, 5s, 2d, Qd, 3h, Kh, 10s, 7d, 2c, Qc, 9h, 6s, 3d, Kd, 4h, As, Js, 8d, 3c, Kc, 10h, 7s, 4d, 8c, 5h, 2s, Qs, 9d, 4c, Ah, Jh, 8s, 5d, 9c, 6h, 3s, Ks, 10d, 5c, 2h, Qh</font><br><br>

            <font color="#777777"><i>*Mnemonica Stack Reference:*</i></font><br>
            <font size="2">4c, 2h, 7d, 3c, 4h, 6d, As, 5h, 9s, 2s, Qh, 3d, Qc, 8h, 6s, 5s, 9h, Kc, 2d, Jh, 3s, 8s, 6h, 10c, 5d, Kd, 2c, 3h, 8d, 5c, Ks, Jd, 8c, 10s, Kh, Jc, 7s, 10h, Ad, 4s, 7h, 4d, Ac, 9c, Js, Qd, 7c, Qs, 10d, 6c, Ah, 9d</font><br><br>
            
            <font color="#777777"><i>*Aronson Stack Reference:*</i></font><br>
            <font size="2">Js, Kc, 5c, 2h, 9s, As, 3h, 6c, 8d, Ac, 10s, 5h, 2d, Kd, 7d, 8c, 3s, Ad, 7s, 5s, Qd, Ah, 8s, 3d, 7h, Qh, 5d, 7c, 4h, Kh, 4d, 10d, Jc, Jh, 10c, Jd, 4s, 10h, 6h, 3c, 2s, 9h, Ks, 6s, 4c, 8h, 9c, Qs, 6d, Qc, 2c, 9d</font><br><br>
            
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>

            <font color="#5800D1"><b><u>[4] PRO PERFORMANCE TIPS & ROUTINES</u></b></font><br>
            &#8226; <font color="#D12012"><b><u>WARNING:</u></b></font> Never swipe down from the top edge during a performance. This exposes the real Android status bar, ruining the illusion.<br>
            &#8226; <b><u>Seamless Entry (Biometrics):</u></b> Enable Fingerprint or Face Unlock on your real phone. Unlocking the device biometrically will bypass the real PIN screen and land directly on the Fake Lockscreen. This looks incredibly natural.<br>
            &#8226; <b><u>UI Consistency:</u></b> Always match the dashboard settings (Wi-Fi, 4G/5G, icons) to your phone's actual current state. This ensures zero visual discrepancies when the phone is eventually unlocked.<br>
            &#8226; <b><u>Mastering Delays:</u></b> Utilize the delay features for all effects. This creates a time gap between your secret physical trigger and the visual magic, destroying any audience suspicion.<br>
            &#8226; <b><u>AR Float 'Materialization':</u></b> Hide a physical card underneath the phone. Slide the AR card out to create a visual "out of phone" effect. Use the scale setting beforehand to match the AR object exactly to the real-world item's size.<br>
            &#8226; <b><u>AR Float Vanish:</u></b> Do not drag the AR object too close to the screen boundaries until you are ready. Overlapping the edge automatically triggers the side-vanish animation.<br>
            &#8226; <b><u>Time Travel Routine:</u></b> Highly effective when combined with a Stripper Deck to create an impossible, back-to-back timeline and location effect.<br>
            &#8226; <b><u>Marquee Peek:</u></b> Use the running text as a covert peek device for mentalism routines without arousing suspicion.<br>
            &#8226; <b><u>Notifications & DND:</u></b> Always activate Do Not Disturb (DND) or disable pop-ups. A real notification popping up will instantly break the illusion.<br>
            &#8226; <b><u>Handling & Misdirection:</u></b> Treat the phone casually. Practice Secret PIN inputs blindly to maintain eye contact-this is your best misdirection.<br>
            &#8226; <b><u>Functional Shortcuts:</u></b> The Camera, Phone, and Emergency Call buttons are fully operational. Use them casually to validate the lockscreen, but do not over-prove.<br>
            &#8226; <b><u>Safe Navigation:</u></b> Always use the physical lock button to back out of the PIN view to the lockscreen. This safely prevents the real Android navigation bar from accidentally appearing.<br><br>
            
            <br><br>
            <center>
            <font size="2" color="#5800D1"><b>APP & ROUTINE ENGINEERED BY ARDAN DESTA VAUNENDRA</b></font><br>
            <font size="2" color="#777777"><i>Developed for professional magic performances. Handle with absolute secrecy.</i></font>
            </center>
        """.trimIndent()

        val textView = android.widget.TextView(this).apply {
            text = androidx.core.text.HtmlCompat.fromHtml(manualHtml, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY)
            setPadding(60, 40, 60, 20)
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#1A1A1A"))
            setLineSpacing(0f, 1.2f)
        }

        val scrollView = android.widget.ScrollView(this).apply {
            addView(textView)
        }

        android.app.AlertDialog.Builder(this)
            .setTitle(androidx.core.text.HtmlCompat.fromHtml("<font color='#1D1D1F'><b>Secret Manual</b></font>", androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .show()
    }
}