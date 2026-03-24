package com.stealthvault.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.sqlcipher.database.SQLiteDatabase

@HiltAndroidApp
class StealthVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize SQLCipher for encrypted database support
        SQLiteDatabase.loadLibs(this)
    }
}
