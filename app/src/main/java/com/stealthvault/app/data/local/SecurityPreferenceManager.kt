package com.stealthvault.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private lateinit var prefs: android.content.SharedPreferences

    /** Plain (unencrypted) preferences for non-sensitive settings like theme. */
    private val appPrefs: SharedPreferences =
        context.getSharedPreferences(APP_SETTINGS_PREFS, Context.MODE_PRIVATE)

    init {
        try {
            initPrefs()
        } catch (e: Throwable) {
            try {
                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                keyStore.deleteEntry("_androidx_security_master_key_")
            } catch (ignored: Throwable) {}

            context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE).edit().clear().commit()
            try {
                initPrefs()
            } catch (fatal: Throwable) {
                // Graceful degradation for devices with permanently broken Keystores
                prefs = context.getSharedPreferences("fallback_prefs", Context.MODE_PRIVATE)
            }
        }
    }

    private fun initPrefs() {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        prefs = androidx.security.crypto.EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_PIN = "master_pin"
        private const val KEY_DECOY_PIN = "decoy_pin"
        private const val KEY_IS_SETUP_COMPLETE = "is_setup_complete"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_INTRUDER_SELFIE = "intruder_selfie_enabled"
        private const val KEY_SHAKE_TO_HIDE = "shake_to_hide_enabled"
        private const val KEY_AUTO_LOCK_MINUTES = "auto_lock_minutes"

        /** Filename used for plain (non-sensitive) app settings. */
        const val APP_SETTINGS_PREFS = "app_settings"
    }

    var masterPin: String?
        get() = prefs.getString(KEY_PIN, null)
        set(value) = prefs.edit().putString(KEY_PIN, value).apply()

    var decoyPin: String?
        get() = prefs.getString(KEY_DECOY_PIN, null)
        set(value) = prefs.edit().putString(KEY_DECOY_PIN, value).apply()

    var isSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_IS_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_SETUP_COMPLETE, value).apply()

    /**
     * Theme mode using [AppCompatDelegate] night-mode constants.
     * Defaults to [AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM].
     */
    var themeMode: Int
        get() = appPrefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = appPrefs.edit().putInt(KEY_THEME_MODE, value).apply()

    /** Whether the intruder-selfie camera capture is enabled (default on). */
    var intruderSelfieEnabled: Boolean
        get() = appPrefs.getBoolean(KEY_INTRUDER_SELFIE, true)
        set(value) = appPrefs.edit().putBoolean(KEY_INTRUDER_SELFIE, value).apply()

    /** Whether shaking the device closes the vault immediately (default on). */
    var shakeToHideEnabled: Boolean
        get() = appPrefs.getBoolean(KEY_SHAKE_TO_HIDE, true)
        set(value) = appPrefs.edit().putBoolean(KEY_SHAKE_TO_HIDE, value).apply()

    /**
     * Minutes of inactivity after which the vault auto-locks.
     * 0 means never. Supported values: 0, 1, 5, 10.
     */
    var autoLockMinutes: Int
        get() = appPrefs.getInt(KEY_AUTO_LOCK_MINUTES, 0)
        set(value) = appPrefs.edit().putInt(KEY_AUTO_LOCK_MINUTES, value).apply()
}
