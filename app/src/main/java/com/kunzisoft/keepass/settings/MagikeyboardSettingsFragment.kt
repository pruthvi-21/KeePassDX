package com.kunzisoft.keepass.settings

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.preferencedialogfragment.DurationDialogFragmentCompat

class MagikeyboardSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences_keyboard, rootKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {

        var otherDialogFragment = false

        var dialogFragment: DialogFragment? = null
        // Main Preferences
        when (preference.key) {
            getString(R.string.keyboard_entry_timeout_key) -> {
                dialogFragment = DurationDialogFragmentCompat.newInstance(preference.key)
            }

            else -> otherDialogFragment = true
        }

        if (dialogFragment != null) {
            @Suppress("DEPRECATION")
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, TAG_PREF_FRAGMENT)
        }
        // Could not be handled here. Try with the super method.
        else if (otherDialogFragment) {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    companion object {
        private const val TAG_PREF_FRAGMENT = "TAG_PREF_FRAGMENT"
    }
}
