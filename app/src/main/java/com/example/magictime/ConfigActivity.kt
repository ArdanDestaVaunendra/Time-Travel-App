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

        pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                prefs.edit().putString("WALLPAPER_URI", it.toString()).apply()
                binding.imgPreview.setImageURI(it)
                try {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        binding.spinnerTimeSpeed.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedSpeed: Float = when (position) {
                    0 -> 1.35f
                    1 -> 1.2f
                    2 -> 1.0f
                    3 -> 0.85f
                    4 -> 0.7f
                    5 -> 0.45f
                    6 -> 0.2f
                    else -> 1.0f
                }
                prefs.edit().putFloat("TIME_SPEED", selectedSpeed).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        loadSavedSettings()
        updateUIText()

        binding.rgTimeFormat.setOnCheckedChangeListener { _, checkedId ->
            val is24Hour = (checkedId == R.id.rbFormat24)
            prefs.edit().putBoolean("IS_24H", is24Hour).apply()
        }

        binding.rgDateLanguage.setOnCheckedChangeListener { _, checkedId ->
            val langCode = if (checkedId == R.id.rbLangEN) "en" else "id"
            prefs.edit().putString("DATE_LANGUAGE", langCode).apply()
        }

        setupLivePreview(binding.etCustomCarrier, "Operator Text")
        setupLivePreview(binding.etCustomMarquee, "Running Text")

        binding.switchShowCarrier.setOnCheckedChangeListener { _, isChecked ->
            binding.etCustomCarrier.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.switchShowMarquee.setOnCheckedChangeListener { _, isChecked ->
            binding.etCustomMarquee.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.switchEnablePin.setOnCheckedChangeListener { _, isChecked ->
            binding.etCustomPin.isEnabled = isChecked
            binding.etCustomPin.alpha = if (isChecked) 1.0f else 0.5f
        }

        setupLivePreview(binding.etRevealText, "Secret Message")

        binding.btnRevealDelayPlus.setOnClickListener {
            revealDelaySeconds++
            updateUIText()
        }
        binding.btnRevealDelayMinus.setOnClickListener {
            if (revealDelaySeconds > 0) revealDelaySeconds--
            updateUIText()
        }

        binding.rgTimeTrigger.setOnCheckedChangeListener { _, _ ->
            updateTriggerInfo()
        }

        binding.btnRevealDurationPlus.setOnClickListener {
            revealDurationSeconds++
            updateUIText()
        }
        binding.btnRevealDurationMinus.setOnClickListener {
            if (revealDurationSeconds > 1) revealDurationSeconds--
            updateUIText()
        }

        binding.tvToggleAdvanced.setOnClickListener {
            android.transition.TransitionManager.beginDelayedTransition(
                binding.mainScrollView as android.view.ViewGroup,
                android.transition.AutoTransition()
            )

            if (binding.layoutAdvancedOptions.visibility == View.VISIBLE) {
                binding.layoutAdvancedOptions.visibility = View.GONE
                binding.tvToggleAdvanced.text = "See All Custom ▾"
            } else {
                binding.layoutAdvancedOptions.visibility = View.VISIBLE
                binding.tvToggleAdvanced.text = "Hide Custom ▴"
            }
        }

        binding.btnOffsetPlus.setOnClickListener { offsetMinutes++; updateUIText() }
        binding.btnOffsetMinus.setOnClickListener { offsetMinutes--; updateUIText() }
        binding.btnDelayPlus.setOnClickListener { delaySeconds++; updateUIText() }
        binding.btnDelayMinus.setOnClickListener { if (delaySeconds > 0) delaySeconds--; updateUIText() }

        binding.btnPickWallpaper.setOnClickListener { pickImage.launch("image/*") }

        binding.btnStart.setOnClickListener {
            saveSettings()
            startActivity(Intent(this, FakeLockActivity::class.java))
            finish()
        }
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
                if (!binding.etCustomCarrier.hasFocus() && !binding.etCustomMarquee.hasFocus()) {
                    binding.cardTypingPreview.visibility = View.GONE
                }
            }
        }

        editText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
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
        val isVolumeForTime = binding.rbVolume.isChecked
        binding.tvTriggerInfo.text = if (isVolumeForTime) {
            "Info: Time uses Volume Down, so SECRET MESSAGE uses DOUBLE TAP."
        } else {
            "Info: Time uses Double Tap, so SECRET MESSAGE uses VOLUME DOWN."
        }
    }

    private fun loadSavedSettings() {
        offsetMinutes = prefs.getInt("OFFSET_SEC", 0) / 60
        delaySeconds = prefs.getInt("DELAY_MS", 5000) / 1000

        if (prefs.getBoolean("TRIGGER_VOLUME", true)) {
            binding.rbVolume.isChecked = true
        } else {
            binding.rbDoubleTap.isChecked = true
        }

        val uriString = prefs.getString("WALLPAPER_URI", null)
        if (uriString != null) {
            try { binding.imgPreview.setImageURI(Uri.parse(uriString)) } catch (e: Exception) {}
        }

        binding.etCustomCarrier.setText(prefs.getString("CUSTOM_CARRIER", "TELKOMSEL · Emergency call only"))
        binding.etCustomMarquee.setText(prefs.getString("CUSTOM_MARQUEE", "Ardan Desta Vaunendra - ardandestavaunedra@gmail.com"))

        val showCarrier = prefs.getBoolean("SHOW_CARRIER", true)
        val showMarquee = prefs.getBoolean("SHOW_MARQUEE", true)
        binding.switchShowCarrier.isChecked = showCarrier
        binding.switchShowMarquee.isChecked = showMarquee

        binding.etCustomCarrier.visibility = if (showCarrier) View.VISIBLE else View.GONE
        binding.etCustomMarquee.visibility = if (showMarquee) View.VISIBLE else View.GONE

        binding.switchShowWifi.isChecked = prefs.getBoolean("SHOW_WIFI", true)
        binding.switchSignal1.isChecked = prefs.getBoolean("SIM1_4G", true)
        binding.switchSignal2.isChecked = prefs.getBoolean("SIM2_4G", false)

        val is24H = prefs.getBoolean("IS_24H", true)
        if (is24H) {
            binding.rbFormat24.isChecked = true
        } else {
            binding.rbFormat12.isChecked = true
        }

        val savedLang = prefs.getString("DATE_LANGUAGE", "id")
        if (savedLang == "en") {
            binding.rbLangEN.isChecked = true
        } else {
            binding.rbLangID.isChecked = true
        }

        val savedSpeed = prefs.getFloat("TIME_SPEED", 1.0f)
        val position = when (savedSpeed) {
            1.35f -> 0
            1.2f -> 1
            1.0f -> 2
            0.85f -> 3
            0.7f -> 4
            0.45f -> 5
            0.2f -> 6
            else -> 2
        }
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

        val target = prefs.getString("REVEAL_TARGET", "BOTH")
        when(target) {
            "CARRIER" -> binding.rbTargetCarrier.isChecked = true
            "MARQUEE" -> binding.rbTargetMarquee.isChecked = true
            else -> binding.rbTargetBoth.isChecked = true
        }

        val savedPredLang = prefs.getString("PREDICTION_LANG", "id")
        if (savedPredLang == "en") {
            binding.rbPredLangEN.isChecked = true
        } else {
            binding.rbPredLangID.isChecked = true
        }
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

        editor.putBoolean("IS_24H", binding.rbFormat24.isChecked)

        editor.putBoolean("ENABLE_PIN", binding.switchEnablePin.isChecked)

        val pinInput = binding.etCustomPin.text.toString()
        val pinToSave = if (pinInput.isNotEmpty()) pinInput else "123456"
        editor.putString("CUSTOM_PIN", pinToSave)
        val predLang = if (binding.rbPredLangEN.isChecked) "en" else "id"
        editor.putString("PREDICTION_LANG", predLang)

        editor.putInt("REVEAL_DURATION", revealDurationSeconds)

        editor.putBoolean("ENABLE_REVEAL", binding.switchEnableReveal.isChecked)
        editor.putString("REVEAL_TEXT", binding.etRevealText.text.toString())
        editor.putInt("REVEAL_DELAY", revealDelaySeconds)

        val targetId = binding.rgRevealTarget.checkedRadioButtonId
        val targetString = when(targetId) {
            R.id.rbTargetCarrier -> "CARRIER"
            R.id.rbTargetMarquee -> "MARQUEE"
            else -> "BOTH"
        }
        editor.putString("REVEAL_TARGET", targetString)

        editor.apply()
    }
}