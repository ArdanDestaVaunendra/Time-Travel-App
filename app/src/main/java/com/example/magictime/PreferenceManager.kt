package com.example.magictime

import android.content.Context
import com.google.gson.Gson

class PreferenceManager(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("MagicTimePrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val KEY_ACTIVE_SESSION = "active_session"
    private val KEY_ACTIVE_SLOT_INDEX = "active_slot_index"

    fun saveActiveSession(settings: AppSettings) {
        val json = gson.toJson(settings)
        sharedPreferences.edit().putString(KEY_ACTIVE_SESSION, json).apply()
    }

    fun getActiveSession(): AppSettings {
        val json = sharedPreferences.getString(KEY_ACTIVE_SESSION, null)
        return if (json != null) {
            gson.fromJson(json, AppSettings::class.java)
            val session = if (json != null) gson.fromJson(json, AppSettings::class.java) else AppSettings()
            if (session.revealText.isBlank()) session.revealText = Defaults.REVEAL_TEXT
            return session
        } else {
            AppSettings()
        }
    }


    fun saveToSlot(slotIndex: Int, settings: AppSettings, slotName: String) {
        val json = gson.toJson(settings)
        sharedPreferences.edit()
            .putString("slot_$slotIndex", json)
            .putString("slot_name_$slotIndex", slotName)
            .apply()
    }

    fun loadFromSlot(slotIndex: Int): AppSettings? {
        val json = sharedPreferences.getString("slot_$slotIndex", null)
        return json?.let { gson.fromJson(it, AppSettings::class.java) }
    }

    fun getSlotName(slotIndex: Int): String {
        return sharedPreferences.getString("slot_name_$slotIndex", "Empty Slot") ?: "Empty Slot"
    }

    fun setActiveSlotIndex(slotIndex: Int) {
        sharedPreferences.edit().putInt(KEY_ACTIVE_SLOT_INDEX, slotIndex).apply()
    }

    fun getActiveSlotIndex(): Int {
        return sharedPreferences.getInt(KEY_ACTIVE_SLOT_INDEX, -1)
    }

    fun clearActiveSlotIndex() {
        sharedPreferences.edit().remove(KEY_ACTIVE_SLOT_INDEX).apply()
    }

    fun clearSlot(slotIndex: Int) {
        sharedPreferences.edit()
            .remove("slot_$slotIndex")
            .remove("slot_name_$slotIndex")
            .apply()

        if (getActiveSlotIndex() == slotIndex) {
            clearActiveSlotIndex()
        }
    }
}