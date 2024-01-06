package com.kunzisoft.keepass.settings

import androidx.preference.PreferenceFragmentCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.fragments.AutofillSettingsFragment

class AutofillSettingsActivity : ExternalSettingsActivity() {

    override fun retrieveTitle(): Int {
        return R.string.autofill_preference_title
    }

    override fun retrievePreferenceFragment(): PreferenceFragmentCompat {
        return AutofillSettingsFragment()
    }
}
