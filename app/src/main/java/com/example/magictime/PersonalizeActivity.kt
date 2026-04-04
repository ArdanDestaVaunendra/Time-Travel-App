package com.example.magictime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.magictime.databinding.ActivityPersonalizeBinding
import kotlin.toString

class PersonalizeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalizeBinding
    private lateinit var prefManager: PreferenceManager
    private lateinit var currentSettings: AppSettings
    private val prefs by lazy { getSharedPreferences("MagicPrefs", Context.MODE_PRIVATE) }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            currentSettings.wallpaperPath = it.toString()
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                binding.imgPreviewWallpaper.setImageURI(it)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalizeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.isFocusable = true
        binding.root.isFocusableInTouchMode = true

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, imeInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        prefManager = PreferenceManager(this)
        currentSettings = prefManager.getActiveSession()

        setupUIFromSettings()

        setupLivePreview(binding.etCustomCarrier, "Operator Text")
        setupLivePreview(binding.etCustomMarquee, "Running Text")
        setupLivePreview(binding.etCustomPin, "Lockscreen PIN")

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnPersonalizeInfo.setOnClickListener {
            showPersonalizeHelpDialog()
        }

        binding.btnPickWallpaper.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.switchShowCarrier.setOnCheckedChangeListener { _, isChecked ->
            binding.etCustomCarrier.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        binding.switchShowMarquee.setOnCheckedChangeListener { _, isChecked ->
            binding.etCustomMarquee.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        binding.switchEnablePin.setOnCheckedChangeListener { _, isChecked ->
            binding.etCustomPin.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.btnSavePersonalize.setOnClickListener {
            saveDataToSettings()
            Toast.makeText(this, "Personalize Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnPersonalizePreviewDone.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.root.windowToken, 0)

            if (binding.etCustomCarrier.hasFocus()) binding.etCustomCarrier.clearFocus()
            if (binding.etCustomMarquee.hasFocus()) binding.etCustomMarquee.clearFocus()
            if (binding.etCustomPin.hasFocus()) binding.etCustomPin.clearFocus()

            binding.cardTypingPreview.visibility = View.GONE
            binding.root.requestFocus()
        }
    }

    private fun setupUIFromSettings() {
        currentSettings.wallpaperPath?.let {
            try { binding.imgPreviewWallpaper.setImageURI(Uri.parse(it)) } catch (e: Exception) {}
        }

        binding.switchEnablePin.isChecked = currentSettings.isPinEnabled
        binding.switchShowCarrier.isChecked = currentSettings.showOperator
        binding.switchShowMarquee.isChecked = currentSettings.showRunningText

        binding.etCustomCarrier.setText(currentSettings.operatorText)
        binding.etCustomMarquee.setText(currentSettings.marqueeText)
        binding.etCustomPin.setText(prefs.getString("CUSTOM_PIN", Defaults.PIN))
        binding.etCustomCarrier.visibility = if (currentSettings.showOperator) View.VISIBLE else View.GONE
        binding.etCustomMarquee.visibility = if (currentSettings.showRunningText) View.VISIBLE else View.GONE
        binding.etCustomPin.visibility = if (currentSettings.isPinEnabled) View.VISIBLE else View.GONE
    }

    private fun saveDataToSettings() {
        currentSettings.isPinEnabled = binding.switchEnablePin.isChecked
        currentSettings.showOperator = binding.switchShowCarrier.isChecked
        currentSettings.showRunningText = binding.switchShowMarquee.isChecked

        currentSettings.operatorText = binding.etCustomCarrier.text.toString()
        currentSettings.marqueeText = binding.etCustomMarquee.text.toString()

        val pinInput = binding.etCustomPin.text.toString()
        prefs.edit()
            .putString("CUSTOM_PIN", if (pinInput.isNotEmpty()) pinInput else Defaults.PIN)
            .putString("CUSTOM_CARRIER", currentSettings.operatorText)
            .putString("CUSTOM_MARQUEE", currentSettings.marqueeText)
            .apply()

        prefManager.saveActiveSession(currentSettings)
    }

    private fun setupLivePreview(editText: EditText, label: String) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (editText.hasFocus()) {
                    binding.tvPreviewContent.text = s.toString()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.cardTypingPreview.visibility = View.VISIBLE
                binding.tvPreviewLabel.text = "Preview:"
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

    private fun showPersonalizeHelpDialog() {
        val helpHtml = """
        <center>
        <font color="#5800D1"><b><u>PERSONALIZE HELP</u></b></font><br>
        <font size="2" color="#777777"><i>Customize lockscreen appearance and behavior</i></font>
        </center><br><br>

        <font color="#5800D1"><b><u>WHAT IS PERSONALIZE?</u></b></font><br>
        Personalize is used to customize your lockscreen appearance without altering the core engine.<br>
        It focuses strictly on the visuals and elements displayed during your performance.<br><br>

        <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>

        <font color="#5800D1"><b><u>CUSTOM OPTIONS</u></b></font><br>
        &#8226; <b>Wallpaper:</b> Change the lockscreen background to match your routine.<br>
        &#8226; <b>Require PIN Before Unlock:</b> Enable or disable the lockscreen PIN requirement.<br>
        &#8226; <b>Show Operator Text:</b> Toggle the visibility of the custom operator name.<br>
        &#8226; <b>Show Running Text:</b> Toggle the visibility of the custom marquee text.<br>
        &#8226; <b>Custom PIN:</b> Set your secret unlock code.<br>
        &#8226; <b>Operator Text:</b> Input your custom operator text to fit the presentation.<br>
        &#8226; <b>Running Text:</b> Input the scrolling text for the bottom of the lockscreen.<br><br>

        <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>

        <font color="#5800D1"><b><u>SECRET PIN INPUTS (THE HIDDEN CORE)</u></b></font><br>
        These commands are NOT in the dashboard settings. To execute them, you MUST <b>HOLD the 'OK' button</b> to activate Secret Mode. The system will silently listen for your input without giving visual feedback.<br><br>

        <font color="#5800D1"><b><u>A. TIME JUMP INJECTION (DYNAMIC OFFSET)</u></b></font><br>
        <i>Function: Silently injects a custom time offset (in minutes) on the fly before triggering the Time Travel illusion.</i><br>
        &#8226; <b>Formula:</b> Hold OK &#8594; 000 &#8594; <b>OK</b> &#8594; [Minutes] &#8594; <b>OK</b><br>
        &#8226; <i>Future (+8 mins):</i> Enter 8<br>
        &#8226; <i>Past (-8 mins):</i> Enter 08<br><br>

        <font color="#5800D1"><b><u>B. SET AR FLOAT CARD (FORCED PREDICTION)</u></b></font><br>
        <i>Function: Silently forces the AR Floating Object to display a specific playing card, overriding the gallery/dashboard settings.</i><br>
        &#8226; <b>Formula:</b> Hold OK &#8594; [Card Value] &#8594; <b>OK</b> &#8594; [Card Suit] &#8594; <b>OK</b><br>
        &#8226; <i>Values:</i> Ace=1, Jack=11, Queen=12, King=13 (Numbers 2-10 are normal)<br>
        &#8226; <i>Suits:</i> 1=Diamonds, 2=Clubs, 3=Hearts, 4=Spades<br>
        &#8226; <i>Example (Queen of Hearts):</i> Hold OK &#8594; 12 &#8594; OK &#8594; 3 &#8594; OK<br><br>

        <font color="#5800D1"><b><u>C. CARD STACK SYSTEM (MARQUEE TEXT REVEAL & BACK CARD FORCING)</u></b></font><br>
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

        <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>

        <font color="#D12012"><b><u>NOTE</u></b></font><br>
        <font color="#777777"><i>&#8226; The <b>Save</b> button applies your personalization changes to the active session.<br>
        &#8226; These settings can be permanently saved to a slot via the <b>Load Setting</b> menu if needed.</i></font>
    """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage(android.text.Html.fromHtml(helpHtml, android.text.Html.FROM_HTML_MODE_LEGACY))
            .setPositiveButton("Got it", null)
            .show()
    }
}