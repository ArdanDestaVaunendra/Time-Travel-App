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
        binding.etCustomPin.setText(prefs.getString("CUSTOM_PIN", "123456"))

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
        prefs.edit().putString("CUSTOM_PIN", if (pinInput.isNotEmpty()) pinInput else "123456").apply()

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
}