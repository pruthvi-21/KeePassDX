package com.kunzisoft.keepass.settings.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel

class MainPreferenceFragment : PreferenceFragmentCompat() {

    private var mCallback: Callback? = null

    private val mDatabaseViewModel: DatabaseViewModel by activityViewModels()
    private var mDatabaseLoaded: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is Callback) {
            mCallback = context
        } else {
            throw IllegalStateException("Owner must implement " + Callback::class.java.name)
        }
    }

    override fun onDetach() {
        mCallback = null
        super.onDetach()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mDatabaseViewModel.database.observe(viewLifecycleOwner) { database ->
            mDatabaseLoaded = database?.loaded == true
            checkDatabaseLoaded()
        }
        super.onViewCreated(view, savedInstanceState)
    }

    private fun checkDatabaseLoaded() {
        findPreference<Preference>(getString(R.string.settings_database_key))
            ?.isEnabled = mDatabaseLoaded

        findPreference<PreferenceCategory>(getString(R.string.settings_database_category_key))
            ?.isVisible = mDatabaseLoaded
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // add listeners for non-default actions
        findPreference<Preference>(getString(R.string.settings_app_key))?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.APPLICATION)
                false
            }
        }

        findPreference<Preference>(getString(R.string.settings_form_filling_key))?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.FORM_FILLING)
                false
            }
        }

        findPreference<Preference>(getString(R.string.settings_advanced_unlock_key))?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.ADVANCED_UNLOCK)
                false
            }
        }

        findPreference<Preference>(getString(R.string.settings_appearance_key))?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.APPEARANCE)
                false
            }
        }

        findPreference<Preference>(getString(R.string.settings_database_key))?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.DATABASE)
                false
            }
        }

        findPreference<Preference>(getString(R.string.settings_database_security_key))?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.DATABASE_SECURITY)
                false
            }
        }

        findPreference<Preference>(getString(R.string.settings_database_credentials_key))?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.DATABASE_MASTER_KEY)
                false
            }
        }

        checkDatabaseLoaded()
    }

    interface Callback {
        fun onNestedPreferenceSelected(key: NestedSettingsFragment.Screen)
    }
}