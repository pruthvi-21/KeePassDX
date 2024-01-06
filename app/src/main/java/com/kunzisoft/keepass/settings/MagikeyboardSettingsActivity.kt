package com.kunzisoft.keepass.settings

import androidx.preference.PreferenceFragmentCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.fragments.MagikeyboardSettingsFragment

class MagikeyboardSettingsActivity : ExternalSettingsActivity() {

    override fun retrieveTitle(): Int {
        return R.string.keyboard_setting_label
    }

    override fun retrievePreferenceFragment(): PreferenceFragmentCompat {
        return MagikeyboardSettingsFragment()
    }

}