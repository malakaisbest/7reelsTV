package com.malakaonline.tv7reals

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val PREFS_NAME = "7reels_prefs"
    private const val KEY_APP_MODE = "app_mode"

    const val MODE_WEBVIEW = "webview"
    const val MODE_NATIVE = "native"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns null if no mode has been selected yet (first-time launch).
     */
    fun getAppMode(context: Context): String? {
        return getPrefs(context).getString(KEY_APP_MODE, null)
    }

    fun setAppMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_APP_MODE, mode).apply()
    }

    fun clearAppMode(context: Context) {
        getPrefs(context).edit().remove(KEY_APP_MODE).apply()
    }
}
