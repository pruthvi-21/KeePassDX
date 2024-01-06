package com.kunzisoft.keepass.settings.fragments

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.preference.PreferenceFragmentCompat
import com.kunzisoft.keepass.R

abstract class NestedSettingsFragment : PreferenceFragmentCompat() {

    enum class Screen(@StringRes val titleRes: Int) {
        APPLICATION(R.string.menu_app_settings),
        FORM_FILLING(R.string.menu_form_filling_settings),
        ADVANCED_UNLOCK(R.string.menu_advanced_unlock_settings),
        APPEARANCE(R.string.menu_appearance_settings),
        DATABASE(R.string.menu_database_settings),
        DATABASE_SECURITY(R.string.menu_security_settings),
        DATABASE_MASTER_KEY(R.string.menu_master_key_settings);
    }

    fun getScreen(): Screen {
        return Screen.values()[requireArguments().getInt(TAG_KEY)]
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        onCreateScreenPreference(
            getScreen(),
            savedInstanceState,
            rootKey
        )
    }

    abstract fun onCreateScreenPreference(
        screen: Screen,
        savedInstanceState: Bundle?,
        rootKey: String?
    )

    companion object {

        private const val TAG_KEY = "NESTED_KEY"

        fun newInstance(key: Screen)
                : PreferenceFragmentCompat {

            return when (key) {
                Screen.APPLICATION -> ApplicationSettingsFragment()
                Screen.FORM_FILLING -> FormFillingSettingsFragment()
                Screen.ADVANCED_UNLOCK -> AdvancedUnlockSettingsFragment()
                Screen.APPEARANCE -> AppearanceSettingsFragment()

                else -> {
                    val fragment = NestedDatabaseSettingsFragment()

                    // supply arguments to bundle.
                    val args = Bundle()
                    args.putInt(TAG_KEY, key.ordinal)
                    fragment.arguments = args
                    return fragment
                }
            }
        }
    }
}
