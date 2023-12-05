package com.kunzisoft.keepass.settings

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.preferencedialogfragment.AutofillBlocklistAppIdPreferenceDialogFragmentCompat
import com.kunzisoft.keepass.settings.preferencedialogfragment.AutofillBlocklistWebDomainPreferenceDialogFragmentCompat

class AutofillSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences_autofill, rootKey)

        val autofillInlineSuggestionsPreference: TwoStatePreference? = findPreference(getString(R.string.autofill_inline_suggestions_key))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            autofillInlineSuggestionsPreference?.isVisible = false
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        var otherDialogFragment = false

        var dialogFragment: DialogFragment? = null

        when (preference.key) {
            getString(R.string.autofill_application_id_blocklist_key) -> {
                dialogFragment = AutofillBlocklistAppIdPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.autofill_web_domain_blocklist_key) -> {
                dialogFragment = AutofillBlocklistWebDomainPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            else -> otherDialogFragment = true
        }

        if (dialogFragment != null) {
            @Suppress("DEPRECATION")
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, TAG_AUTOFILL_PREF_FRAGMENT)
        }
        // Could not be handled here. Try with the super method.
        else if (otherDialogFragment) {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    companion object {

        private const val TAG_AUTOFILL_PREF_FRAGMENT = "TAG_AUTOFILL_PREF_FRAGMENT"
    }
}
