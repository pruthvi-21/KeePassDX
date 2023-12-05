package com.kunzisoft.keepass.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment


class AppearanceSettingsActivity : SettingsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mTimeoutEnable = false
        setTitle(NestedSettingsFragment.Screen.APPEARANCE)
    }

    override fun retrieveMainFragment(): Fragment {
        return NestedSettingsFragment.newInstance(NestedSettingsFragment.Screen.APPEARANCE)
    }
}