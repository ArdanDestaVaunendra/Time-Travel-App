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

class LoadSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoadSettingBinding
    private lateinit var prefManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        binding.btnBack.setOnClickListener { finish() }

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

        tvSlotNumber.text = "SLOT $index"

        val currentSlotName = prefManager.getSlotName(index)
        tvSlotName.text = currentSlotName

        if (currentSlotName == "Empty Slot") {
            btnLoad.isEnabled = false
            imgIcon.setColorFilter(android.graphics.Color.parseColor("#C7C7CC"))
        } else {
            btnLoad.isEnabled = true
            imgIcon.setColorFilter(android.graphics.Color.parseColor("#5800D1"))
        }

        btnSave.setOnClickListener {
            showSaveDialog(index, tvSlotName, imgIcon, btnLoad)
        }

        btnLoad.setOnClickListener {
            val savedSettings = prefManager.loadFromSlot(index)
            if (savedSettings != null) {
                savedSettings.currentStatusMode = "LOADED"
                prefManager.saveActiveSession(savedSettings)

                Toast.makeText(this, "Profile Loaded: $currentSlotName", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showSaveDialog(index: Int, tvSlotName: TextView, imgIcon: ImageView, btnLoad: MaterialButton) {
        val editText = EditText(this)
        editText.hint = "e.g., Stage Routine, Street Magic..."
        editText.setPadding(50, 40, 50, 40)

        AlertDialog.Builder(this)
            .setTitle("Save to Slot $index")
            .setMessage("Enter a name for this profile:")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val inputName = editText.text.toString()
                val profileName = if (inputName.isNotEmpty()) inputName else "Profile $index"

                val activeSettings = prefManager.getActiveSession()
                prefManager.saveToSlot(index, activeSettings, profileName)

                tvSlotName.text = profileName
                btnLoad.isEnabled = true
                imgIcon.setColorFilter(android.graphics.Color.parseColor("#5800D1"))

                Toast.makeText(this, "Saved to Slot $index", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}