package com.example.magictime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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

        // Load Status Bar Icons Toggles
        binding.switchShowWifi.isChecked = prefs.getBoolean("SHOW_WIFI", true)
        binding.switchSignal1.isChecked = prefs.getBoolean("SIM1_4G", true)
        binding.switchSignal2.isChecked = prefs.getBoolean("SIM2_4G", false)

        val is24H = prefs.getBoolean("IS_24H", true)
        if (is24H) {
            binding.rbFormat24.isChecked = true
        } else {
            binding.rbFormat12.isChecked = true
        }

        val savedLang = prefs.getString("DATE_LANGUAGE", "id") // Default "id"
        if (savedLang == "en") {
            binding.rbLangEN.isChecked = true
        } else {
            binding.rbLangID.isChecked = true
        }
    }


    private fun saveSettings() {
        prefs.edit().apply {
            putInt("OFFSET_SEC", offsetMinutes * 60)
            putInt("DELAY_MS", delaySeconds * 1000)
            putBoolean("TRIGGER_VOLUME", binding.rbVolume.isChecked)

            putString("CUSTOM_CARRIER", binding.etCustomCarrier.text.toString())
            putString("CUSTOM_MARQUEE", binding.etCustomMarquee.text.toString())

            putBoolean("SHOW_CARRIER", binding.switchShowCarrier.isChecked)
            putBoolean("SHOW_MARQUEE", binding.switchShowMarquee.isChecked)

            // Save Status Bar Icons Toggles
            putBoolean("SHOW_WIFI", binding.switchShowWifi.isChecked)
            putBoolean("SIM1_4G", binding.switchSignal1.isChecked)
            putBoolean("SIM2_4G", binding.switchSignal2.isChecked)

            putBoolean("IS_24H", binding.rbFormat24.isChecked)
        }.apply()
    }
}