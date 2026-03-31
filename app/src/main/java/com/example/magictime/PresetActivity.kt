package com.example.magictime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.magictime.databinding.ActivityPresetBinding

class PresetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPresetBinding
    private lateinit var prefManager: PreferenceManager
    private lateinit var currentSettings: AppSettings
    private var tempFloatPath: String? = null
    private val pickGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            tempFloatPath = it.toString()
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                binding.imgPresetFloatPreview.setImageURI(it)
            } catch (e: Exception) { e.printStackTrace() }
        }
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
                try { binding.imgPresetFloatPreview.setImageURI(Uri.parse(tempFloatPath)) } catch (e: Exception) {}
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
                            columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
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

        prefManager.saveActiveSession(currentSettings)
    }

    private fun showCurrentEngineInfo() {
        val delaySec = currentSettings.globalDelay / 1000
        val timeJump = currentSettings.timeJumpOffset
        val revealSec = currentSettings.predictionDuration / 1000
        val stackSys = currentSettings.stackSystem

        val timeTrigger = if (currentSettings.isVolumeTriggerForTime) "Volume Down" else "Double Tap"
        val messageTrigger = if (currentSettings.isVolumeTriggerForTime) "Double Tap" else "Volume Down"
        val floatTrigger = if (currentSettings.isShakeTriggerEnabled) "Physical Shake" else "Volume Up"

        val infoMessage = """
        Active Engine Variables & Triggers:
        
        • Time Travel: $timeTrigger
        • Secret Message: $messageTrigger
        • AR Float Reveal: $floatTrigger
        
        • Global Delay: $delaySec Seconds
        • Time Jump Offset: +$timeJump Minutes
        • Reveal Duration: $revealSec Seconds
        • Prediction Stack: $stackSys
        
        To modify these values, go to Advanced Settings.
    """.trimIndent()

        android.app.AlertDialog.Builder(this)
            .setTitle("Active Engine Status")
            .setMessage(infoMessage)
            .setPositiveButton("Got it", null)
            .show()
    }
}