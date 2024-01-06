package com.kunzisoft.keepass.settings.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.DialogFragment
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.icons.IconPackChooser
import com.kunzisoft.keepass.settings.preference.IconPackListPreference
import com.kunzisoft.keepass.settings.preferencedialogfragment.AppColorPalettePreferenceDialogFragmentCompat

class AppearanceSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey)

        activity?.let { activity ->
            findPreference<IconPackListPreference>(getString(R.string.setting_icon_pack_choose_key))?.setOnPreferenceChangeListener { _, newValue ->
                val iconPackId = newValue as String
                IconPackChooser.setSelectedIconPack(iconPackId)
                true
            }

            findPreference<DropDownPreference>(getString(R.string.setting_app_theme_key))?.setOnPreferenceChangeListener { _, _ ->
                //delaying just to finish the popup exit animation
                Handler(Looper.getMainLooper()).postDelayed({ activity.recreate() }, 250)
                true
            }

            findPreference<SwitchPreferenceCompat>(getString(R.string.setting_app_use_black_key))?.setOnPreferenceChangeListener { _, _ ->
                //delaying just to finish the switch animation
                Handler(Looper.getMainLooper()).postDelayed({ activity.recreate() }, 300)
                true
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {

        var otherDialogFragment = false
        var dialogFragment: DialogFragment? = null

        when (preference.key) {
            getString(R.string.setting_app_color_palette_key) -> {
                dialogFragment =
                    AppColorPalettePreferenceDialogFragmentCompat.newInstance(preference.key)
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
        private const val TAG_PREF_FRAGMENT = "AppearanceSettingsFragment"
    }
}
