package com.example.magictime

import android.content.Context
import com.google.gson.Gson

class PreferenceManager(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("MagicTimePrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val KEY_ACTIVE_SESSION = "active_session"

    fun saveActiveSession(settings: AppSettings) {
        val json = gson.toJson(settings)
        sharedPreferences.edit().putString(KEY_ACTIVE_SESSION, json).apply()
    }

    fun getActiveSession(): AppSettings {
        val json = sharedPreferences.getString(KEY_ACTIVE_SESSION, null)
        return if (json != null) {
            gson.fromJson(json, AppSettings::class.java)
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
}