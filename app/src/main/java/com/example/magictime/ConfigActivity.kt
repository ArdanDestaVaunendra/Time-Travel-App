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
import android.widget.TextView
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

    // --- VARIABEL AR FLOAT ---
    private var isUsingGalleryMode = false
    private var customImageUriString: String? = null
    private var currentFloatDelay = 0

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, imeInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        MobileAds.initialize(this) {}

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

        // --- SETUP UI AR FLOAT ---
        binding.switchImageSource.setOnCheckedChangeListener { _, isChecked ->
            isUsingGalleryMode = isChecked
            binding.btnInsertFloatImage.visibility = if (isChecked) View.VISIBLE else View.GONE

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

        binding.btnStart.setOnClickListener {
            saveFloatSettings()
            saveSettings()
            val intent = Intent(this, FakeLockActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        saveFloatSettings()
    }

    // --- FUNGSI LIVE PREVIEW & TEXT ---
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

    // --- SAVE & LOAD SETTINGS ---
    private fun loadSavedSettings() {
        offsetMinutes = prefs.getInt("OFFSET_SEC", 0) / 60
        delaySeconds = prefs.getInt("DELAY_MS", 5000) / 1000

        binding.rbVolume.isChecked = prefs.getBoolean("TRIGGER_VOLUME", true)
        binding.rbDoubleTap.isChecked = !binding.rbVolume.isChecked

        prefs.getString("WALLPAPER_URI", null)?.let {
            try { binding.imgPreview.setImageURI(Uri.parse(it)) } catch (e: Exception) {}
        }

        binding.etCustomCarrier.setText(prefs.getString("CUSTOM_CARRIER", "TELKOMSEL · Emergency call only"))
        binding.etCustomMarquee.setText(prefs.getString("CUSTOM_MARQUEE", "Ardan Desta Vaunendra - ardandestavaunedra@gmail.com"))

        val showCarrier = prefs.getBoolean("SHOW_CARRIER", true)
        val showMarquee = prefs.getBoolean("SHOW_MARQUEE", true)
        binding.switchShowCarrier.isChecked = showCarrier
        binding.switchShowMarquee.isChecked = showMarquee
        binding.etCustomCarrier.visibility = if (showCarrier) View.VISIBLE else View.GONE
        binding.etCustomMarquee.visibility = if (showMarquee) View.VISIBLE else View.GONE

        val isWifiOn = prefs.getBoolean("SHOW_WIFI", true)
        binding.switchShowWifi.isChecked = isWifiOn

        binding.switchSignal1.isEnabled = !isWifiOn
        binding.switchSignal2.isEnabled = !isWifiOn
        binding.switchSignal1.alpha = if (isWifiOn) 0.5f else 1.0f
        binding.switchSignal2.alpha = if (isWifiOn) 0.5f else 1.0f

        binding.switch5G.isChecked = prefs.getBoolean("USE_5G", false)

        binding.switchSignal1.isChecked = prefs.getBoolean("SIM1_4G", true)
        binding.switchSignal2.isChecked = prefs.getBoolean("SIM2_4G", false)

        binding.rbFormat24.isChecked = prefs.getBoolean("IS_24H", true)
        binding.rbFormat12.isChecked = !binding.rbFormat24.isChecked

        binding.rbLangEN.isChecked = (prefs.getString("DATE_LANGUAGE", "id") == "en")
        binding.rbLangID.isChecked = !binding.rbLangEN.isChecked

        val savedSpeed = prefs.getFloat("TIME_SPEED", 1.0f)
        val position = when (savedSpeed) { 1.35f->0; 1.2f->1; 1.0f->2; 0.85f->3; 0.7f->4; 0.45f->5; 0.2f->6; else->2 }
        binding.spinnerTimeSpeed.setSelection(position)

        val isPinEnabled = prefs.getBoolean("ENABLE_PIN", true)
        binding.switchEnablePin.isChecked = isPinEnabled
        binding.etCustomPin.setText(prefs.getString("CUSTOM_PIN", "123456"))
        binding.etCustomPin.isEnabled = isPinEnabled
        binding.etCustomPin.alpha = if (isPinEnabled) 1.0f else 0.5f

        revealDurationSeconds = prefs.getInt("REVEAL_DURATION", 7)
        binding.switchEnableReveal.isChecked = prefs.getBoolean("ENABLE_REVEAL", false)
        binding.etRevealText.setText(prefs.getString("REVEAL_TEXT", ""))
        revealDelaySeconds = prefs.getInt("REVEAL_DELAY", 3)

        when(prefs.getString("REVEAL_TARGET", "BOTH")) {
            "CARRIER" -> binding.rbTargetCarrier.isChecked = true
            "MARQUEE" -> binding.rbTargetMarquee.isChecked = true
            else -> binding.rbTargetBoth.isChecked = true
        }

        binding.rbPredLangEN.isChecked = (prefs.getString("PREDICTION_LANG", "id") == "en")
        binding.rbPredLangID.isChecked = !binding.rbPredLangEN.isChecked

        updateUIText()
    }

    private fun saveSettings() {
        val editor = prefs.edit()
        editor.putInt("OFFSET_SEC", offsetMinutes * 60)
        editor.putInt("DELAY_MS", delaySeconds * 1000)
        editor.putBoolean("TRIGGER_VOLUME", binding.rbVolume.isChecked)
        editor.putString("CUSTOM_CARRIER", binding.etCustomCarrier.text.toString())
        editor.putString("CUSTOM_MARQUEE", binding.etCustomMarquee.text.toString())
        editor.putBoolean("SHOW_CARRIER", binding.switchShowCarrier.isChecked)
        editor.putBoolean("SHOW_MARQUEE", binding.switchShowMarquee.isChecked)
        editor.putBoolean("SHOW_WIFI", binding.switchShowWifi.isChecked)
        editor.putBoolean("SIM1_4G", binding.switchSignal1.isChecked)
        editor.putBoolean("SIM2_4G", binding.switchSignal2.isChecked)
        editor.putBoolean("USE_5G", binding.switch5G.isChecked)
        editor.putBoolean("IS_24H", binding.rbFormat24.isChecked)
        editor.putBoolean("ENABLE_PIN", binding.switchEnablePin.isChecked)

        val pinInput = binding.etCustomPin.text.toString()
        editor.putString("CUSTOM_PIN", if (pinInput.isNotEmpty()) pinInput else "123456")
        editor.putString("PREDICTION_LANG", if (binding.rbPredLangEN.isChecked) "en" else "id")
        editor.putInt("REVEAL_DURATION", revealDurationSeconds)
        editor.putBoolean("ENABLE_REVEAL", binding.switchEnableReveal.isChecked)
        editor.putString("REVEAL_TEXT", binding.etRevealText.text.toString())
        editor.putInt("REVEAL_DELAY", revealDelaySeconds)

        val targetString = when(binding.rgRevealTarget.checkedRadioButtonId) {
            R.id.rbTargetCarrier -> "CARRIER"; R.id.rbTargetMarquee -> "MARQUEE"; else -> "BOTH"
        }
        editor.putString("REVEAL_TARGET", targetString)
        editor.apply()
    }

    private fun loadFloatSettings() {
        val fPrefs = getSharedPreferences("MagicTimePrefs", MODE_PRIVATE)
        binding.switchFloatEffect.isChecked = fPrefs.getBoolean("FLOAT_IS_ACTIVE", false)
        binding.seekBarFloatSize.progress = fPrefs.getInt("FLOAT_SCALE", 75)

        currentFloatDelay = fPrefs.getInt("FLOAT_DELAY", 0)
        binding.tvFloatDelayValue.text = "${currentFloatDelay}"

        isUsingGalleryMode = fPrefs.getBoolean("IS_GALLERY_MODE", false)
        binding.switchImageSource.isChecked = isUsingGalleryMode

        customImageUriString = fPrefs.getString("FLOAT_CUSTOM_URI", null)

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
        val editor = getSharedPreferences("MagicTimePrefs", MODE_PRIVATE).edit()
        editor.putBoolean("FLOAT_IS_ACTIVE", binding.switchFloatEffect.isChecked)
        editor.putInt("FLOAT_SCALE", binding.seekBarFloatSize.progress)
        editor.putInt("FLOAT_DELAY", currentFloatDelay)

        editor.putBoolean("IS_GALLERY_MODE", isUsingGalleryMode)
        editor.putString("FLOAT_CUSTOM_URI", customImageUriString)

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
                binding.imgDynamicFloatPreview.setImageResource(R.drawable.back_card)
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
}