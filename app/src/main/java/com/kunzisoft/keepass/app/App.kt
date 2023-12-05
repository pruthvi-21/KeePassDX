package com.kunzisoft.keepass.app

import androidx.multidex.MultiDexApplication

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        PRNGFixes.apply()
    }
}
