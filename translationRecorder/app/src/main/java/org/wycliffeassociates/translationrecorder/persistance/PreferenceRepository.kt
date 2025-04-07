package org.wycliffeassociates.translationrecorder.persistance

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class PreferenceRepository(private val context: Context) : IPreferenceRepository {
    /**
     * Returns an instance of the user preferences.
     * This is just the default shared preferences
     */
    private val defaultPrefs: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(context)

    override fun <T> getDefaultPref(key: String, type: Class<T>): T? {
        return getPref(key, type, defaultPrefs)
    }

    override fun <T> getDefaultPref(key: String, defaultValue: T, type: Class<T>): T {
        return getPref(key, defaultValue, type, defaultPrefs)
    }

    override fun <T> setDefaultPref(key: String, value: T?, type: Class<T>) {
        setPref(key, value, type, defaultPrefs)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T>getPref(
        key: String,
        type: Class<T>,
        sharedPreferences: SharedPreferences
    ): T {
        return when (type) {
            java.lang.String::class.java -> sharedPreferences.getString(key, null)
            java.lang.Integer::class.java -> sharedPreferences.getInt(key, -1)
            java.lang.Long::class.java -> sharedPreferences.getLong(key, -1L)
            java.lang.Float::class.java -> sharedPreferences.getFloat(key, -1F)
            java.lang.Boolean::class.java -> sharedPreferences.getBoolean(key, false)
            else -> null
        } as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T>getPref(
        key: String,
        value: T,
        type: Class<T>,
        sharedPreferences: SharedPreferences
    ): T {
        return when (type) {
            java.lang.String::class.java -> sharedPreferences.getString(key, value as String)
            java.lang.Integer::class.java -> sharedPreferences.getInt(key, value as Int)
            java.lang.Long::class.java -> sharedPreferences.getLong(key, value as Long)
            java.lang.Float::class.java -> sharedPreferences.getFloat(key, value as Float)
            java.lang.Boolean::class.java -> sharedPreferences.getBoolean(key, value as Boolean)
            else -> value as String
        } as T
    }

    private fun <T>setPref(
        key: String,
        value: T?,
        type: Class<T>,
        sharedPreferences: SharedPreferences
    ) {
        val editor = sharedPreferences.edit()
        if (value != null) {
            when (type) {
                java.lang.String::class.java -> editor.putString(key, value as String?)
                java.lang.Integer::class.java -> editor.putInt(key, value as Int)
                java.lang.Long::class.java -> editor.putLong(key, value as Long)
                java.lang.Float::class.java -> editor.putFloat(key, value as Float)
                java.lang.Boolean::class.java -> editor.putBoolean(key, value as Boolean)
                else -> editor.apply()
            }
        } else {
            editor.remove(key)
        }
        editor.apply()
    }
}