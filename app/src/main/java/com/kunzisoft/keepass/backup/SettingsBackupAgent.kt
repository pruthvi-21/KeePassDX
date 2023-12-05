package com.kunzisoft.keepass.backup

import android.app.backup.BackupAgentHelper
import android.app.backup.SharedPreferencesBackupHelper

class SettingsBackupAgent : BackupAgentHelper() {

    override fun onCreate() {
        val defaultPrefs = this.packageName + "_preferences"
        val prefHelper = SharedPreferencesBackupHelper(this, defaultPrefs)
        addHelper(PREFS_BACKUP_KEY, prefHelper)
    }

    companion object {
        private const val PREFS_BACKUP_KEY = "prefs"
    }
}
