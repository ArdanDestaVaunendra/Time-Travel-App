package com.example.magictime

import android.content.Context
import com.google.gson.Gson

class LayoutConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("LayoutEditorPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_ACTIVE_SLOT = "layout_active_slot"
        private const val SLOT_PREFIX = "layout_slot_"
    }

    fun save(slot: Int, config: LayoutConfig) {
        prefs.edit().putString("$SLOT_PREFIX$slot", gson.toJson(config)).apply()
        setActiveSlot(slot)
    }

    fun load(slot: Int): LayoutConfig? {
        val raw = prefs.getString("$SLOT_PREFIX$slot", null) ?: return null
        return runCatching { gson.fromJson(raw, LayoutConfig::class.java) }.getOrNull()
    }

    fun setActiveSlot(slot: Int) {
        prefs.edit().putInt(KEY_ACTIVE_SLOT, slot).apply()
    }

    fun getActiveSlot(): Int = prefs.getInt(KEY_ACTIVE_SLOT, -1)

    fun clearActiveSlot() {
        prefs.edit().remove(KEY_ACTIVE_SLOT).apply()
    }
}
