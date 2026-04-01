package com.example.magictime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.magictime.databinding.ActivityPresetBinding
import kotlin.div
import kotlin.text.toInt
import kotlin.times

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

        prefManager = PreferenceManager(this)
        currentSettings = prefManager.getActiveSession()
        tempFloatPath = currentSettings.floatTargetCardPath

        loadActiveRoutines()
        updatePreviewImage()

        binding.switchARFloat.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutFloatOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
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
            // Default Card Back
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
        if (binding.switchPrediction.isChecked) newRoutines.add("PREDICTION")
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

        val infoMessageHtml = """
    <center>
    <font color="#5800D1"><b><u>PRESET MODE: BALANCED DEFAULTS</u></b></font><br>
    <font size="2" color="#777777"><i>Standalone Profile Active</i></font>
    </center><br><br>
    
    <b><u>ACTIVE ROUTINES</u></b><br>
    &#8226; <i>TIMEJUMP:</i> ${if (isTimeJumpOn) "<b><font color=\"#2E7D32\">ON</font></b>" else "<b><font color=\"#D12012\">OFF</font></b>"}<br>
    &#8226; <i>PREDICTION:</i> ${if (isPredictionOn) "<b><font color=\"#2E7D32\">ON</font></b>" else "<b><font color=\"#D12012\">OFF</font></b>"}<br>
    &#8226; <i>FLOAT:</i> ${if (isFloatOn) "<b><font color=\"#2E7D32\">ON</font></b>" else "<b><font color=\"#D12012\">OFF</font></b>"}<br><br>

    <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>

    <b><u>TRIGGER MAPPING</u></b><br>
    &#8226; <i>Time Travel Trigger:</i> <b>$timeTrigger</b><br>
    &#8226; <i>Secret Message Trigger:</i> <b>$messageTrigger</b><br>
    &#8226; <i>AR Float Trigger:</i> <b>$floatTrigger</b><br><br>

    <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>

    <b><u>ENGINE DEFAULTS</u></b><br>
    &#8226; <i>Global Delay:</i> ${PresetDefaults.GLOBAL_DELAY_MS / 1000} Seconds<br>
    &#8226; <i>Time Jump Offset:</i> +${PresetDefaults.TIME_JUMP_MIN} Minutes<br>
    &#8226; <i>Time Flow Speed:</i> x${PresetDefaults.TIME_FLOW_SPEED}<br>
    &#8226; <i>Reveal Duration:</i> ${PresetDefaults.PREDICTION_DURATION_MS / 1000} Seconds<br>
    &#8226; <i>Float Delay:</i> ${PresetDefaults.FLOAT_DELAY_SEC} Seconds<br>
    &#8226; <i>Float Scale:</i> ${(PresetDefaults.FLOAT_SCALE * 100).toInt()}%<br>
    &#8226; <i>Prediction Stack:</i> ${PresetDefaults.STACK_SYSTEM}<br>
    &#8226; <i>Prediction Target:</i> ${PresetDefaults.PREDICTION_TARGET}<br>
    &#8226; <i>Prediction Language:</i> ${PresetDefaults.PREDICTION_LANGUAGE.uppercase()}<br>
    &#8226; <i>Date Language:</i> ${PresetDefaults.DATE_LANGUAGE.uppercase()}<br>
    &#8226; <i>Time Format:</i> ${if (PresetDefaults.IS_24H) "24-Hour" else "12-Hour"}<br><br>

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
}
