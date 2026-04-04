package com.example.magictime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.magictime.databinding.ActivityPresetBinding
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.toString

class PresetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPresetBinding
    private lateinit var prefManager: PreferenceManager
    private lateinit var currentSettings: AppSettings
    private var tempFloatPath: String? = null
    private val pickGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                tempFloatPath = it.toString()
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    binding.imgPresetFloatPreview.setImageURI(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    private object PresetDefaults {
        const val TRIGGER_TIME_VOLUME = false
        const val GLOBAL_DELAY_MS = 5000L
        const val TIME_JUMP_MIN = 8
        const val TIME_FLOW_SPEED = 0.85f
        const val IS_24H = true
        const val FLOAT_DELAY_SEC = 5
        const val FLOAT_SCALE = 0.91f
        const val SHAKE_TRIGGER = false
        const val PREDICTION_DURATION_MS = 7000L
        const val PREDICTION_LANGUAGE = "en"
        const val PREDICTION_TARGET = "BOTH"
        const val DATE_LANGUAGE = "en"
        const val STACK_SYSTEM = "Bart Harding"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPresetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, imeInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.root.isFocusable = true
        binding.root.isFocusableInTouchMode = true

        prefManager = PreferenceManager(this)
        currentSettings = prefManager.getActiveSession()
        tempFloatPath = currentSettings.floatTargetCardPath

        loadActiveRoutines()

        binding.etPresetRevealText.setText(currentSettings.revealText.ifBlank { Defaults.REVEAL_TEXT })
        setupPresetRevealPreview()

        updateRevealInputState(binding.switchPrediction.isChecked)

        updatePreviewImage()

        binding.switchARFloat.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutFloatOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.switchPrediction.setOnCheckedChangeListener { _, isChecked ->
            updateRevealInputState(isChecked)
            if (!isChecked) {
                hidePresetRevealEditorUi()
            }
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnPresetInfo.setOnClickListener {
            showCurrentEngineInfo()
        }

        binding.btnPresetPickGallery.setOnClickListener {
            pickGalleryLauncher.launch("image/*")
        }

        binding.btnPresetPickCard.setOnClickListener {
            showCardSelectorDialog()
        }

        binding.btnSavePreset.setOnClickListener {
            saveActiveRoutines()
            Toast.makeText(this, "Preset Applied with New Target!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadActiveRoutines() {
        val routines = currentSettings.activeRoutines
        binding.switchTimeJump.isChecked = routines.contains("TIMEJUMP")
        binding.switchPrediction.isChecked = routines.contains("PREDICTION")

        val isFloatActive = routines.contains("FLOAT")
        binding.switchARFloat.isChecked = isFloatActive
        binding.layoutFloatOptions.visibility = if (isFloatActive) View.VISIBLE else View.GONE
    }

    private fun updatePreviewImage() {
        if (tempFloatPath != null) {
            if (tempFloatPath!!.startsWith("content://")) {
                try {
                    binding.imgPresetFloatPreview.setImageURI(Uri.parse(tempFloatPath))
                } catch (e: Exception) {
                }
            } else {
                val resId = resources.getIdentifier(tempFloatPath, "drawable", packageName)
                if (resId != 0) {
                    binding.imgPresetFloatPreview.setImageResource(resId)
                }
            }
        } else {
            binding.imgPresetFloatPreview.setImageResource(R.drawable.back_card_blue)
        }
    }

    private fun showCardSelectorDialog() {
        val scrollView = android.widget.ScrollView(this)
        val gridLayout = android.widget.GridLayout(this).apply {
            columnCount = 4
            setPadding(32, 32, 32, 32)
        }
        scrollView.addView(gridLayout)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Force a Prediction Card")
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
                            columnSpec = android.widget.GridLayout.spec(
                                android.widget.GridLayout.UNDEFINED,
                                1f
                            )
                            setMargins(16, 16, 16, 16)
                        }

                        setOnClickListener {
                            tempFloatPath = cardName
                            updatePreviewImage()
                            dialog.dismiss()
                        }
                    }
                    gridLayout.addView(imageView)
                }
            }
        }
        dialog.show()
    }

    private fun saveActiveRoutines() {
        val newRoutines = mutableSetOf<String>()

        if (binding.switchTimeJump.isChecked) newRoutines.add("TIMEJUMP")

        if (binding.switchPrediction.isChecked) {
            newRoutines.add("PREDICTION")
            currentSettings.revealText = binding.etPresetRevealText.text
                ?.toString()
                ?.trim()
                .orEmpty()
                .ifBlank { Defaults.REVEAL_TEXT }
        } else {
            if (currentSettings.revealText.isBlank()) {
                currentSettings.revealText = Defaults.REVEAL_TEXT
            }
        }

        if (binding.switchARFloat.isChecked) {
            newRoutines.add("FLOAT")
            currentSettings.floatTargetCardPath = tempFloatPath
        }

        currentSettings.activeRoutines = newRoutines
        currentSettings.currentStatusMode = "PRESET"
        currentSettings.isVolumeTriggerForTime = false

        // --- BALANCED DEFAULTS PRESET ---
        currentSettings.is24HourFormat = PresetDefaults.IS_24H
        currentSettings.floatDelay = PresetDefaults.FLOAT_DELAY_SEC
        currentSettings.globalDelay = PresetDefaults.GLOBAL_DELAY_MS
        currentSettings.timeJumpOffset = PresetDefaults.TIME_JUMP_MIN
        currentSettings.timeFlowSpeed = PresetDefaults.TIME_FLOW_SPEED
        currentSettings.objectScale = PresetDefaults.FLOAT_SCALE
        currentSettings.predictionDuration = PresetDefaults.PREDICTION_DURATION_MS
        currentSettings.isShakeTriggerEnabled = PresetDefaults.SHAKE_TRIGGER
        currentSettings.predictionLanguage = PresetDefaults.PREDICTION_LANGUAGE
        currentSettings.predictionTarget = PresetDefaults.PREDICTION_TARGET
        currentSettings.dateLanguage = PresetDefaults.DATE_LANGUAGE
        currentSettings.stackSystem = PresetDefaults.STACK_SYSTEM
        // -------------------------------

        prefManager.saveActiveSession(currentSettings)
    }


    private fun showCurrentEngineInfo() {
        val timeTrigger = if (PresetDefaults.TRIGGER_TIME_VOLUME) "Volume Down" else "Double Tap"
        val messageTrigger = if (PresetDefaults.TRIGGER_TIME_VOLUME) "Double Tap" else "Volume Down"
        val floatTrigger = if (PresetDefaults.SHAKE_TRIGGER) "Physical Shake" else "Volume Up"

        val isTimeJumpOn = binding.switchTimeJump.isChecked
        val isPredictionOn = binding.switchPrediction.isChecked
        val isFloatOn = binding.switchARFloat.isChecked
        val activeRevealText = binding.etPresetRevealText.text
            ?.toString()
            ?.trim()
            .orEmpty()
            .ifBlank { currentSettings.revealText.ifBlank { Defaults.REVEAL_TEXT } }

        val activeRevealTextHtml = android.text.TextUtils.htmlEncode(activeRevealText)

        val activePin = getSharedPreferences("MagicPrefs", MODE_PRIVATE)
            .getString("CUSTOM_PIN", Defaults.PIN)
            ?.trim()
            .orEmpty()
            .ifBlank { Defaults.PIN }

        val activeOperatorText = currentSettings.operatorText
            .trim()
            .ifBlank { "-" }

        val activeMarqueeText = currentSettings.marqueeText
            .trim()
            .ifBlank { "-" }

        val activeOperatorTextHtml = android.text.TextUtils.htmlEncode(activeOperatorText)
        val activeMarqueeTextHtml = android.text.TextUtils.htmlEncode(activeMarqueeText)

        val activePinHtml = android.text.TextUtils.htmlEncode(activePin)

        val infoMessageHtml = """
            <center>
            <font color="#5800D1"><b><u>PRESET MODE: BALANCED DEFAULTS</u></b></font><br>
            <font size="2" color="#777777"><i>Standalone Profile Active</i></font>
            </center><br><br>
            
            <font color="#5800D1"><b><u>WHAT IS PRESET MODE?</u></b></font><br>
            Preset Mode is your "Plug & Play" setup designed for impromptu performances. It safely bypasses all complex configurations in the Advanced Settings and runs on thoroughly tested, balanced engine parameters.<br><br>
        
            <font color="#5800D1"><b><u>WHY & WHEN TO USE IT?</u></b></font><br>
            Use this mode when you need a quick, guaranteed-safe setup without worrying about conflicting variables or miscalculated delays. It is perfect for street magic or spontaneous situations where you don't have time to calibrate the engine.<br><br>
        
            <font color="#5800D1"><b><u>ROUTINE BEHAVIOR</u></b></font><br>
            When routines (Timejump, Prediction, or Float) are activated in this mode, their triggers and mechanics are strictly locked to the safest defaults. This allows you to focus 100% on your presentation and misdirection.<br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
            
            <font color="#5800D1"><b><u>ACTIVE ROUTINES</u></b></font><br>
            &#8226; <i>TIMEJUMP:</i> ${if (isTimeJumpOn) "<b><font color=\"#2E7D32\">ON</font></b>" else "<b><font color=\"#D12012\">OFF</font></b>"}<br>
            &#8226; <i>PREDICTION:</i> ${if (isPredictionOn) "<b><font color=\"#2E7D32\">ON</font></b>" else "<b><font color=\"#D12012\">OFF</font></b>"}<br>
            &#8226; <i>Prediction Text:</i> <b><font color="#007BFF">$activeRevealTextHtml</font></b><br>
            &#8226; <i>FLOAT:</i> ${if (isFloatOn) "<b><font color=\"#2E7D32\">ON</font></b>" else "<b><font color=\"#D12012\">OFF</font></b>"}<br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>TRIGGER MAPPING</u></b></font><br>
            &#8226; <i>Time Travel Trigger:</i> <b><font color="#007BFF">$timeTrigger</font></b><br>
            &#8226; <i>Secret Message Trigger:</i> <b><font color="#007BFF">$messageTrigger</font></b><br>
            &#8226; <i>AR Float Trigger:</i> <b><font color="#007BFF">$floatTrigger</font></b><br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>ENGINE DEFAULTS & SETTINGS</u></b></font><br>
            &#8226; <i>Global Delay:</i> <b><font color="#007BFF">${PresetDefaults.GLOBAL_DELAY_MS / 1000} Seconds</font></b><br>
            &#8226; <i>Time Jump Offset:</i> <b><font color="#007BFF">+${PresetDefaults.TIME_JUMP_MIN} Minutes</font></b><br>
            &#8226; <i>Time Flow Speed:</i> <b><font color="#007BFF">x${PresetDefaults.TIME_FLOW_SPEED}</font></b><br>
            &#8226; <i>Reveal Duration:</i> <b><font color="#007BFF">${PresetDefaults.PREDICTION_DURATION_MS / 1000} Seconds</font></b><br>
            &#8226; <i>Float Delay:</i> <b><font color="#007BFF">${PresetDefaults.FLOAT_DELAY_SEC} Seconds</font></b><br>
            &#8226; <i>Float Scale:</i> <b><font color="#007BFF">${(PresetDefaults.FLOAT_SCALE * 100).toInt()}%</font></b><br>
            &#8226; <i>Custom PIN:</i> <b><font color="#007BFF">$activePinHtml</font></b><br>
            &#8226; <i>Operator Visible:</i> ${if (currentSettings.showOperator) "<b><font color=\"#2E7D32\">YES</font></b>" else "<b><font color=\"#D12012\">NO</font></b>"}<br>
            &#8226; <i>Operator Text:</i> <b><font color="#007BFF">$activeOperatorTextHtml</font></b><br>
            &#8226; <i>Marquee Visible:</i> ${if (currentSettings.showRunningText) "<b><font color=\"#2E7D32\">YES</font></b>" else "<b><font color=\"#D12012\">NO</font></b>"}<br>
            &#8226; <i>Marquee Text:</i> <b><font color="#007BFF">$activeMarqueeTextHtml</font></b><br>
            &#8226; <i>Prediction Stack:</i> <b><font color="#007BFF">${PresetDefaults.STACK_SYSTEM}</font></b><br>
            &#8226; <i>Prediction Target:</i> <b><font color="#007BFF">${PresetDefaults.PREDICTION_TARGET}</font></b><br>
            &#8226; <i>Prediction Language:</i> <b><font color="#007BFF">${PresetDefaults.PREDICTION_LANGUAGE.uppercase()}</font></b><br>
            &#8226; <i>Date Language:</i> <b><font color="#007BFF">${PresetDefaults.DATE_LANGUAGE.uppercase()}</font></b><br>
            &#8226; <i>Time Format:</i> <b><font color="#007BFF">${if (PresetDefaults.IS_24H) "24-Hour" else "12-Hour"}</font></b><br><br>
            
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#D12012"><b><u>NOTE:</u></b></font><br>
            <font color="#777777"><i>Preset mode overwrites key engine variables with fixed defaults.<br>
            Use Advanced Settings if you want custom behavior.</i></font>
        """.trimIndent()

        android.app.AlertDialog.Builder(this)
            .setTitle("Preset Information")
            .setMessage(
                android.text.Html.fromHtml(
                    infoMessageHtml,
                    android.text.Html.FROM_HTML_MODE_LEGACY
                )
            )
            .setPositiveButton("Got it", null)
            .show()

    }

    private fun updateRevealInputState(isPredictionEnabled: Boolean) {
        binding.tilPresetRevealText.visibility = if (isPredictionEnabled) View.VISIBLE else View.GONE
        binding.etPresetRevealText.isEnabled = isPredictionEnabled
        binding.etPresetRevealText.alpha = if (isPredictionEnabled) 1f else 0.5f

        if (!isPredictionEnabled) {
            hidePresetRevealEditorUi()
        }
    }

    private fun setupPresetRevealPreview() {
        binding.etPresetRevealText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (binding.etPresetRevealText.hasFocus() && binding.switchPrediction.isChecked) {
                    binding.tvPresetPreviewContent.text = s?.toString().orEmpty()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etPresetRevealText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.switchPrediction.isChecked) {
                binding.cardPresetTypingPreview.visibility = View.VISIBLE
                binding.tvPresetPreviewContent.text = binding.etPresetRevealText.text?.toString().orEmpty()
            } else {
                hidePresetRevealEditorUi()
            }
        }

        binding.btnPresetRevealDone.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etPresetRevealText.windowToken, 0)

            binding.etPresetRevealText.clearFocus()
            hidePresetRevealEditorUi()
            binding.root.requestFocus()
        }

        binding.etPresetRevealText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                v.clearFocus()
                hidePresetRevealEditorUi()
                binding.root.requestFocus()
                true
            } else {
                false
            }
        }
    }

    private fun hidePresetRevealEditorUi() {
        binding.cardPresetTypingPreview.visibility = View.GONE
    }
}
