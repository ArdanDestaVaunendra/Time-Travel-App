package com.example.magictime

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.magictime.databinding.ActivityLoadSettingBinding
import com.google.android.material.button.MaterialButton
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton

class LoadSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoadSettingBinding
    private lateinit var prefManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnLoadSettingInfo.setOnClickListener {
            showLoadSettingHelpDialog()
        }

        setupSlot(1, binding.slot1.root)
        setupSlot(2, binding.slot2.root)
        setupSlot(3, binding.slot3.root)
        setupSlot(4, binding.slot4.root)
        setupSlot(5, binding.slot5.root)
    }

    private fun setupSlot(index: Int, slotView: android.view.View) {
        val tvSlotNumber = slotView.findViewById<TextView>(R.id.tvSlotNumber)
        val tvSlotName = slotView.findViewById<TextView>(R.id.tvSlotName)
        val imgIcon = slotView.findViewById<ImageView>(R.id.imgSlotIcon)
        val btnSave = slotView.findViewById<MaterialButton>(R.id.btnSaveSlot)
        val btnLoad = slotView.findViewById<MaterialButton>(R.id.btnLoadSlot)
        val slotForeground = slotView.findViewById<View>(R.id.slotForeground)
        val btnReset = slotView.findViewById<ImageButton>(R.id.btnResetSlot)

        tvSlotNumber.text = "SLOT $index"

        bindSlotState(index, tvSlotName, imgIcon, btnSave, btnLoad, slotForeground, btnReset)
    }

    private fun bindSlotState(
        index: Int,
        tvSlotName: TextView,
        imgIcon: ImageView,
        btnSave: MaterialButton,
        btnLoad: MaterialButton,
        slotForeground: View,
        btnReset: ImageButton
    ) {
        val slotName = prefManager.getSlotName(index)
        val slotSettings = prefManager.loadFromSlot(index)

        tvSlotName.text = slotName

        if (slotName == "Empty Slot" || slotSettings == null) {
            btnLoad.isEnabled = false
            btnSave.text = "SAVE"
            btnReset.visibility = View.GONE
            slotForeground.animate().translationX(0f).setDuration(120).start()
            slotForeground.setOnTouchListener(null)
            imgIcon.setColorFilter(Color.parseColor("#C7C7CC"))

            tvSlotName.setTextColor(Color.parseColor("#1D1D1F"))
            tvSlotName.isClickable = false
            tvSlotName.setOnClickListener(null)

            btnSave.setOnClickListener {
                showSaveDialog(index, tvSlotName, imgIcon, btnLoad, btnSave, slotForeground, btnReset)
            }

            btnLoad.isEnabled = false
            btnLoad.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#D1D1D6"))
            btnLoad.setTextColor(Color.parseColor("#ECECF1"))

            return
        }

        btnLoad.isEnabled = true
        imgIcon.setColorFilter(Color.parseColor("#5800D1"))

        tvSlotName.setTextColor(Color.parseColor("#5800D1"))
        tvSlotName.isClickable = true
        tvSlotName.setOnClickListener {
            showRenameDialog(index, slotSettings, slotName, tvSlotName, imgIcon, btnSave, btnLoad, slotForeground, btnReset)
        }

        btnSave.text = "VIEW"
        btnSave.setOnClickListener {
            showViewDialog(index, slotName, slotSettings)
        }

        btnReset.visibility = View.VISIBLE
        setupSwipeToRevealReset(slotForeground, btnReset, dp(72))

        btnReset.setOnClickListener {
            showResetDialog(index, tvSlotName, imgIcon, btnSave, btnLoad, slotForeground, btnReset)
        }

        btnLoad.setOnClickListener {
            val savedSettings = prefManager.loadFromSlot(index)
            if (savedSettings != null) {
                savedSettings.currentStatusMode = "LOADED"
                prefManager.saveActiveSession(savedSettings)
                prefManager.setActiveSlotIndex(index)

                Toast.makeText(this, "Profile Loaded: $slotName", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btnLoad.isEnabled = true
        btnLoad.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#5800D1"))
        btnLoad.setTextColor(Color.WHITE)
    }

    private fun showSaveDialog(
        index: Int,
        tvSlotName: TextView,
        imgIcon: ImageView,
        btnLoad: MaterialButton,
        btnSave: MaterialButton,
        slotForeground: View,
        btnReset: ImageButton
    ) {
        val editText = EditText(this)
        editText.hint = "e.g., Stage Routine, Street Magic..."
        editText.setPadding(50, 40, 50, 40)

        AlertDialog.Builder(this)
            .setTitle("Save to Slot $index")
            .setMessage("Enter a name for this profile:")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val inputName = editText.text.toString().trim()
                val profileName = if (inputName.isNotEmpty()) inputName else "Profile $index"

                val activeSettings = prefManager.getActiveSession()
                prefManager.saveToSlot(index, activeSettings, profileName)

                tvSlotName.text = profileName
                btnLoad.isEnabled = true
                imgIcon.setColorFilter(android.graphics.Color.parseColor("#5800D1"))

                bindSlotState(index, tvSlotName, imgIcon, btnSave, btnLoad, slotForeground, btnReset)

                Toast.makeText(this, "Saved to Slot $index", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog(
        index: Int,
        slotSettings: AppSettings,
        currentName: String,
        tvSlotName: TextView,
        imgIcon: ImageView,
        btnSave: MaterialButton,
        btnLoad: MaterialButton,
        slotForeground: View,
        btnReset: ImageButton
    ) {
        val editText = EditText(this).apply {
            setText(currentName)
            setSelection(text.length)
            hint = "Rename profile..."
            setSingleLine(true)
        }

        val container = android.widget.FrameLayout(this).apply {
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(
                editText,
                android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Rename Slot $index")
            .setMessage("Enter new profile name:")
            .setView(container)
            .setPositiveButton("Rename") { _, _ ->
                val inputName = editText.text.toString().trim()
                val newName = if (inputName.isNotEmpty()) inputName else currentName

                prefManager.saveToSlot(index, slotSettings, newName)
                tvSlotName.text = newName
                imgIcon.setColorFilter(Color.parseColor("#5800D1"))

                bindSlotState(index, tvSlotName, imgIcon, btnSave, btnLoad, slotForeground, btnReset)
                Toast.makeText(this, "Slot $index renamed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showViewDialog(index: Int, slotName: String, s: AppSettings) {
        val routines = if (s.activeRoutines.isEmpty()) "-" else s.activeRoutines.joinToString(", ")
        val trigger = if (s.isVolumeTriggerForTime) "Volume Down" else "Double Tap"
        val activePin = getSharedPreferences("MagicPrefs", MODE_PRIVATE)
            .getString("CUSTOM_PIN", Defaults.PIN)
            ?.trim()
            .orEmpty()
            .ifBlank { Defaults.PIN }

        val activePinHtml = android.text.TextUtils.htmlEncode(activePin)
        val revealTextHtml = android.text.TextUtils.htmlEncode(s.revealText.ifBlank { Defaults.REVEAL_TEXT })
        val netMode = s.networkMode.uppercase()
        val isWifi = netMode.contains("WIFI")
        val use5g = netMode.contains("5G")
        val sim1 = netMode.contains("SIM1") || netMode.contains("DUAL") || (!isWifi)
        val sim2 = netMode.contains("SIM2") || netMode.contains("DUAL")

        val msgHtml = """
            <center>
            <font color="#5800D1"><b><u>SLOT $index: ${slotName.uppercase()}</u></b></font><br>
            <font size="2" color="#777777"><i>Profile Details</i></font>
            </center><br><br>
        
            <font color="#5800D1"><b><u>OVERVIEW</u></b></font><br>
            &#8226; <i>Mode:</i> <b><font color="#007BFF">${s.currentStatusMode}</font></b><br>
            &#8226; <i>Routines:</i> <b><font color="#007BFF">$routines</font></b><br>
            &#8226; <i>Time Trigger:</i> <b><font color="#007BFF">$trigger</font></b><br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>TIME SETTINGS</u></b></font><br>
            &#8226; <i>Global Delay:</i> <b><font color="#007BFF">${s.globalDelay / 1000}s</font></b><br>
            &#8226; <i>Time Jump Offset:</i> <b><font color="#007BFF">+${s.timeJumpOffset} min</font></b><br>
            &#8226; <i>Time Speed:</i> <b><font color="#007BFF">x${s.timeFlowSpeed}</font></b><br>
            &#8226; <i>Time Format:</i> <b><font color="#007BFF">${if (s.is24HourFormat) "24H" else "12H"}</font></b><br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>PREDICTION SETTINGS</u></b></font><br>
            &#8226; <i>Prediction Text:</i> <b><font color="#007BFF">${if (s.revealText.isBlank()) "-" else s.revealText}</font></b><br>
            &#8226; <i>Prediction Delay:</i> <b><font color="#007BFF">${s.revealDelay}s</font></b><br>
            &#8226; <i>Prediction Duration:</i> <b><font color="#007BFF">${s.predictionDuration / 1000}s</font></b><br>
            &#8226; <i>Prediction Target:</i> <b><font color="#007BFF">${s.predictionTarget}</font></b><br>
            &#8226; <i>Prediction Language:</i> <b><font color="#007BFF">${s.predictionLanguage.uppercase()}</font></b><br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>LOCKSCREEN UI</u></b></font><br>
            &#8226; <i>PIN Enabled:</i> ${if (s.isPinEnabled) "<b><font color=\"#2E7D32\">YES</font></b>" else "<b><font color=\"#D12012\">NO</font></b>"}<br>
            &#8226; <i>PIN Value:</i> <b><font color="#007BFF">$activePinHtml</font></b><br>
            &#8226; <i>Operator Visible:</i> ${if (s.showOperator) "<b><font color=\"#2E7D32\">YES</font></b>" else "<b><font color=\"#D12012\">NO</font></b>"}<br>
            &#8226; <i>Operator Text:</i> <b><font color="#007BFF">${s.operatorText}</font></b><br>
            &#8226; <i>Marquee Visible:</i> ${if (s.showRunningText) "<b><font color=\"#2E7D32\">YES</font></b>" else "<b><font color=\"#D12012\">NO</font></b>"}<br>
            &#8226; <i>Marquee Text:</i> <b><font color="#007BFF">${s.marqueeText}</font></b><br><br>
            
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
            
            <font color="#5800D1"><b><u>NETWORK STATUS</u></b></font><br>
            &#8226; <i>Wi-Fi:</i> ${if (isWifi) "<b><font color=\"#2E7D32\">ON</font></b>" else "<b><font color=\"#D12012\">OFF</font></b>"}<br>
            &#8226; <i>5G Icon:</i> ${if (use5g) "<b><font color=\"#2E7D32\">ON</font></b>" else "<b><font color=\"#D12012\">OFF</font></b>"}<br>
            &#8226; <i>SIM1 Data:</i> ${if (sim1) "<b><font color=\"#2E7D32\">ON</font></b>" else "<b><font color=\"#D12012\">OFF</font></b>"}<br>
            &#8226; <i>SIM2 Data:</i> ${if (sim2) "<b><font color=\"#2E7D32\">ON</font></b>" else "<b><font color=\"#D12012\">OFF</font></b>"}<br>
            &#8226; <i>Raw Mode:</i> <b><font color="#007BFF">${s.networkMode}</font></b><br><br>
                        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>AR FLOAT</u></b></font><br>
            &#8226; <i>Float Delay:</i> <b><font color="#007BFF">${s.floatDelay}s</font></b><br>
            &#8226; <i>Float Scale:</i> <b><font color="#007BFF">${(s.objectScale * 100).toInt()}%</font></b><br>
            &#8226; <i>Shake Trigger:</i> ${if (s.isShakeTriggerEnabled) "<b><font color=\"#2E7D32\">ON</font></b>" else "<b><font color=\"#D12012\">OFF</font></b>"}
        """.trimIndent()

        android.app.AlertDialog.Builder(this)
            .setMessage(android.text.Html.fromHtml(msgHtml, android.text.Html.FROM_HTML_MODE_LEGACY))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupSwipeToRevealReset(slotForeground: View, btnReset: ImageButton, revealWidthPx: Int) {
        var downX = 0f
        var downY = 0f
        var startTx = 0f

        slotForeground.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    downY = ev.rawY
                    startTx = v.translationX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY

                    // prioritize horizontal swipe
                    if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                        val next = (startTx + dx).coerceIn(0f, revealWidthPx.toFloat())
                        v.translationX = next
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val open = v.translationX > revealWidthPx * 0.35f
                    v.animate()
                        .translationX(if (open) revealWidthPx.toFloat() else 0f)
                        .setDuration(140)
                        .start()
                    true
                }
                else -> false
            }
        }
    }

    private fun showResetDialog(
        index: Int,
        tvSlotName: TextView,
        imgIcon: ImageView,
        btnSave: MaterialButton,
        btnLoad: MaterialButton,
        slotForeground: View,
        btnReset: ImageButton
    ) {
        AlertDialog.Builder(this)
            .setTitle("Reset Slot $index")
            .setMessage("This will clear this profile slot and set it back to Empty Slot.")
            .setPositiveButton("Reset") { _, _ ->
                prefManager.clearSlot(index)

                slotForeground.translationX = 0f
                btnReset.visibility = View.GONE

                bindSlotState(index, tvSlotName, imgIcon, btnSave, btnLoad, slotForeground, btnReset)
                Toast.makeText(this, "Slot $index reset", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                slotForeground.animate().translationX(0f).setDuration(120).start()
            }
            .show()
    }

    private fun showLoadSettingHelpDialog() {
        val helpHtml = """
            <center>
            <font color="#5800D1"><b><u>LOAD SETTING HELP</u></b></font><br>
            <font size="2" color="#777777"><i>Save, view, load, rename, and reset profile slots</i></font>
            </center><br><br>
        
            <font color="#5800D1"><b><u>WHAT IS LOAD SETTING?</u></b></font><br>
            A feature to save your active configurations into slots, allowing you to reload them at any time.<br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>HOW TO USE</u></b></font><br>
            &#8226; Configure your settings first in other screens (Preset / Personalize / Advanced).<br>
            &#8226; Open the Load Setting menu.<br>
            &#8226; Select an empty slot, tap <b>SAVE</b>, and enter a profile name.<br>
            &#8226; Filled slots will activate the <b>VIEW</b> and <b>LOAD</b> buttons.<br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>BUTTON GUIDE</u></b></font><br>
            &#8226; <b>SAVE</b>: Saves current active settings into an empty slot.<br>
            &#8226; <b>VIEW</b>: Displays the detailed configuration inside the profile slot.<br>
            &#8226; <b>LOAD</b>: Applies the profile in the slot as your current active setting.<br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>RENAME SLOT</u></b></font><br>
            Tap the slot name (purple text) on a filled slot to rename the profile.<br><br>
        
            <font color="#5800D1"><b><u>RESET SLOT</u></b></font><br>
            Swipe the slot card to the right to reveal the reset icon, then tap it to clear the slot.<br>
            Swipe back to the left to cancel.<br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#D12012"><b><u>NOTE</u></b></font><br>
            <font color="#777777"><i>&#8226; Resetting a slot only clears that specific data; it does not affect your current active settings or other slots.<br>
            &#8226; Once you hit <b>LOAD</b>, that profile becomes the active session until changed again.</i></font>
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Load Setting Information")
            .setMessage(android.text.Html.fromHtml(helpHtml, android.text.Html.FROM_HTML_MODE_LEGACY))
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
