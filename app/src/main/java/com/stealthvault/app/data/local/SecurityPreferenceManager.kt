package com.stealthvault.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        try {
            androidx.security.crypto.EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fails if debug keystore signature changes between re-installs. Fix: clear data and try again.
            context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE).edit().clear().commit()
            androidx.security.crypto.EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    companion object {
        private const val KEY_PIN = "master_pin"
        private const val KEY_DECOY_PIN = "decoy_pin"
        private const val KEY_IS_SETUP_COMPLETE = "is_setup_complete"
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
}
