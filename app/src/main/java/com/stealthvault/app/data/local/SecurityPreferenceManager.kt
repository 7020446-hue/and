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

    private lateinit var prefs: android.content.SharedPreferences

    init {
        try {
            initPrefs()
        } catch (e: Exception) {
            // Fails if debug keystore signature changes between re-installs. Fix: clear data and try again.
            try {
                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                keyStore.deleteEntry("_androidx_security_master_key_")
            } catch (ignored: Exception) {}

            context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE).edit().clear().commit()
            initPrefs()
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
