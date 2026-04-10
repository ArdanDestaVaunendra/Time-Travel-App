package com.example.magictime

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

object DndManager {
    private const val PREFS = "MagicPrefs"
    private const val KEY_USE_AUTO_DND = "USE_AUTO_DND"
    private const val KEY_WAS_SET_BY_APP = "DND_WAS_SET_BY_APP"
    private const val KEY_PREV_FILTER = "DND_PREV_FILTER"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isAutoEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USE_AUTO_DND, false)

    fun setAutoEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_USE_AUTO_DND, enabled).apply()
        if (!enabled) restoreIfNeeded(context)
    }

    fun hasAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun openAccessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun applyIfEnabled(context: Context) {
        if (!isAutoEnabled(context) || !hasAccess(context)) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val p = prefs(context)

        if (!p.getBoolean(KEY_WAS_SET_BY_APP, false)) {
            p.edit()
                .putInt(KEY_PREV_FILTER, nm.currentInterruptionFilter)
                .putBoolean(KEY_WAS_SET_BY_APP, true)
                .apply()
        }

        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    fun restoreIfNeeded(context: Context) {
        if (!hasAccess(context)) return

        val p = prefs(context)
        if (!p.getBoolean(KEY_WAS_SET_BY_APP, false)) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val prev = p.getInt(KEY_PREV_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        nm.setInterruptionFilter(prev)

        p.edit()
            .putBoolean(KEY_WAS_SET_BY_APP, false)
            .remove(KEY_PREV_FILTER)
            .apply()
    }
}
