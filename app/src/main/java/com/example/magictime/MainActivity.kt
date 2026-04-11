package com.example.magictime

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.magictime.databinding.ActivityMainBinding
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentToast: Toast? = null
    private lateinit var prefManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        setupMenuUI()
        updateStatusBanner()

        binding.btnEditLayout.setOnClickListener {
            startActivity(Intent(this, EditLayoutActivity::class.java))
        }

        setupDndButton()

        binding.btnStartMagic.setOnClickListener {
            killAllToasts()

            val intent = Intent(this, FakeLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            startActivity(intent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }

            finish()
        }


    }

    override fun onResume() {
        super.onResume()
        updateStatusBanner()
        updateDndButtonState()
    }

    private fun setupMenuUI() {
        binding.menuPreset.root.setOnClickListener {
            startActivity(Intent(this, PresetActivity::class.java))
        }
        binding.menuPreset.root.findViewById<TextView>(R.id.menuTitle).text = "Preset Routine"
        binding.menuPreset.root.findViewById<TextView>(R.id.menuSubtitle).text = "Quick toggle effects & target cards"
        binding.menuPreset.root.findViewById<ImageView>(R.id.menuIcon).setImageResource(R.drawable.ic_menu_preset)

        binding.menuPreset.root.findViewById<ImageView>(R.id.btnHelpInfo).setOnClickListener {
            showMenuInfoDialog("Preset Routine", "Quickly toggle magic effects (Time Travel, AR Float, Prediction) on or off instantly without adjusting complex delays. Perfect for rapid setup right before a performance.")
        }

        binding.menuPersonalize.root.setOnClickListener {
            startActivity(Intent(this, PersonalizeActivity::class.java))
        }
        binding.menuPersonalize.root.findViewById<TextView>(R.id.menuTitle).text = "Personalize"
        binding.menuPersonalize.root.findViewById<TextView>(R.id.menuSubtitle).text = "Wallpaper, text & lockscreen UI"
        binding.menuPersonalize.root.findViewById<ImageView>(R.id.menuIcon).setImageResource(R.drawable.ic_menu_personalize)

        binding.menuPersonalize.root.findViewById<ImageView>(R.id.btnHelpInfo).setOnClickListener {
            showMenuInfoDialog("Personalize", "Customize the external appearance of your lockscreen. Change the wallpaper, carrier/marquee text, and PIN. Modifying these settings will NOT overwrite or reset your currently loaded profile.")
        }

        binding.menuAdvanced.root.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }
        binding.menuAdvanced.root.findViewById<TextView>(R.id.menuTitle).text = "Advanced Settings"
        binding.menuAdvanced.root.findViewById<TextView>(R.id.menuSubtitle).text = "Fine-tune delays, stacks & triggers"
        binding.menuAdvanced.root.findViewById<ImageView>(R.id.menuIcon).setImageResource(R.drawable.ic_menu_advanced)

        binding.menuAdvanced.root.findViewById<ImageView>(R.id.btnHelpInfo).setOnClickListener {
            showMenuInfoDialog("Advanced Settings", "The core engine of the app. Fine-tune your routines with absolute precision: adjust trigger delays, resize the AR Float object, and select your preferred Stack system (Mnemonica or Bart Harding).")
        }

        binding.menuLoad.root.setOnClickListener {
            startActivity(Intent(this, LoadSettingActivity::class.java))
        }
        binding.menuLoad.root.findViewById<TextView>(R.id.menuTitle).text = "Load Setting"
        binding.menuLoad.root.findViewById<TextView>(R.id.menuSubtitle).text = "Access your 5 saved profiles"
        binding.menuLoad.root.findViewById<ImageView>(R.id.menuIcon).setImageResource(R.drawable.ic_menu_load)

        binding.menuLoad.root.findViewById<ImageView>(R.id.btnHelpInfo).setOnClickListener {
            showMenuInfoDialog("Load Setting", "A slot-based memory system. Save your current configurations into one of 5 dedicated slots (e.g., 'Stage' or 'Street'). Instantly LOAD a profile whenever needed to retrieve all your settings in a second.")
        }

        binding.menuTutorial.root.setOnClickListener {
            startActivity(Intent(this, TutorialActivity::class.java))
        }
        binding.menuTutorial.root.findViewById<TextView>(R.id.menuTitle).text = "Tutorial & Grimoire"
        binding.menuTutorial.root.findViewById<TextView>(R.id.menuSubtitle).text = "Read manual & watch handlings"
        binding.menuTutorial.root.findViewById<ImageView>(R.id.menuIcon).setImageResource(R.drawable.ic_menu_tutorial)

        binding.menuTutorial.root.findViewById<ImageView>(R.id.btnHelpInfo).setOnClickListener {
            showMenuInfoDialog("Tutorial & Grimoire", "Your secret grimoire. Access the written Secret Manual for hidden PIN inputs, or watch linked video tutorials covering handling, misdirection, and Black Art concepts.")
        }
    }

    private fun updateStatusBanner() {
        val session = prefManager.getActiveSession()

        val cardBanner = binding.cardStatusBanner
        val tvStatus = binding.tvStatusText
        val imgIcon = binding.imgStatusIcon

        when (session.currentStatusMode) {
            "PRESET" -> {
                cardBanner.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                tvStatus.text = "Mode: Quick Preset"
                imgIcon.setColorFilter(Color.parseColor("#4CAF50"))
            }
            "LOADED" -> {
                cardBanner.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                tvStatus.setTextColor(Color.parseColor("#1565C0"))
                tvStatus.text = "Loaded: Profile Active"
                imgIcon.setColorFilter(Color.parseColor("#2196F3"))
            }
            "CUSTOM" -> {
                cardBanner.setCardBackgroundColor(Color.parseColor("#F1EEFF"))
                tvStatus.setTextColor(Color.parseColor("#3B3E9C"))
                tvStatus.text = "Mode: Custom / Modified"
                imgIcon.setColorFilter(Color.parseColor("#3B3E9C"))
            }
            else -> {
                tvStatus.text = "Ready to Perform"
            }
        }
    }

    private fun setupDndButton() {
        binding.btnUseDnd.setOnClickListener {
            if (!DndManager.hasAccess(this)) {
                DndManager.openAccessSettings(this)
                showToast("Grant Do Not Disturb access first, then tap again.")
                return@setOnClickListener
            }

            val next = !DndManager.isAutoEnabled(this)
            DndManager.setAutoEnabled(this, next)
            updateDndButtonState()

            showToast(if (next) "Zero Notif Mode enabled" else "Zero Notif Mode disabled")
        }

        updateDndButtonState()
    }

    private fun updateDndButtonState() {
        val enabled = DndManager.isAutoEnabled(this)
        val hasAccess = DndManager.hasAccess(this)

        if (!hasAccess) {
            binding.btnUseDnd.text = "Zero Notif Mode: Permission Required"
            binding.btnUseDnd.alpha = 0.9f
            return
        }

        val base = if (enabled) "Zero Notif Mode: ON" else "Zero Notif Mode: OFF"
        val spannable = SpannableString(base)

        val stateText = if (enabled) "ON" else "OFF"
        val start = base.lastIndexOf(stateText)
        val end = start + stateText.length

        val stateColor = if (enabled) {
            android.graphics.Color.parseColor("#2E7D32")
        } else {
            android.graphics.Color.parseColor("#9E9E9E")
        }

        spannable.setSpan(
            ForegroundColorSpan(stateColor),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.btnUseDnd.text = spannable
        binding.btnUseDnd.alpha = 1f
    }

    private fun showMenuInfoDialog(title: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun killAllToasts() {
        currentToast?.cancel()
    }

    private fun showToast(message: String) {
        currentToast?.cancel()
        currentToast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        currentToast?.show()
    }
}