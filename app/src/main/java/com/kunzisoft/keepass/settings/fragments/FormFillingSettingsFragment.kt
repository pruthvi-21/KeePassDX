package com.kunzisoft.keepass.settings.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.services.ClipboardEntryNotificationService
import com.kunzisoft.keepass.settings.AutofillSettingsActivity
import com.kunzisoft.keepass.settings.MagikeyboardSettingsActivity
import com.kunzisoft.keepass.settings.preferencedialogfragment.DurationDialogFragmentCompat

class FormFillingSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_form_filling, rootKey)

        activity?.let { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val autoFillEnablePreference: TwoStatePreference? =
                    findPreference(getString(R.string.settings_autofill_enable_key))
                activity.getSystemService(AutofillManager::class.java)?.let { autofillManager ->
                    if (autofillManager.hasEnabledAutofillServices())
                        autoFillEnablePreference?.isChecked =
                            autofillManager.hasEnabledAutofillServices()

                    autoFillEnablePreference?.onPreferenceClickListener =
                        object : Preference.OnPreferenceClickListener {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            override fun onPreferenceClick(preference: Preference): Boolean {
                                if ((preference as TwoStatePreference).isChecked) {
                                    try {
                                        enableService()
                                    } catch (e: ActivityNotFoundException) {
                                        val error =
                                            getString(R.string.error_autofill_enable_service)
                                        preference.isChecked = false
                                        Log.d(javaClass.name, error, e)
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                    }

                                } else {
                                    disableService()
                                }
                                return false
                            }

                            @RequiresApi(api = Build.VERSION_CODES.O)
                            private fun disableService() {
                                if (autofillManager.hasEnabledAutofillServices()) {
                                    autofillManager.disableAutofillServices()
                                } else {
                                    Log.d(javaClass.name, "Autofill service already disabled.")
                                }
                            }

                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Throws(ActivityNotFoundException::class)
                            private fun enableService() {
                                if (!autofillManager.hasEnabledAutofillServices()) {
                                    val intent =
                                        Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                                    intent.data =
                                        Uri.parse("package:com.kunzisoft.keepass.autofill.KeeAutofillService")
                                    Log.d(javaClass.name, "Autofill enable service: intent=$intent")
                                    startActivity(intent)
                                } else {
                                    Log.d(javaClass.name, "Autofill service already enabled.")
                                }
                            }
                        }
                }
            } else {
                findPreference<Preference>(getString(R.string.autofill_key))?.isVisible = false
            }
        }

        findPreference<Preference>(getString(R.string.magic_keyboard_key))?.setOnPreferenceClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            false
        }

        findPreference<Preference>(getString(R.string.magic_keyboard_preference_key))?.setOnPreferenceClickListener {
            startActivity(Intent(context, MagikeyboardSettingsActivity::class.java))
            false
        }

        findPreference<Preference>(getString(R.string.settings_autofill_key))?.setOnPreferenceClickListener {
            startActivity(Intent(context, AutofillSettingsActivity::class.java))
            false
        }

        findPreference<Preference>(getString(R.string.clipboard_notifications_key))?.setOnPreferenceChangeListener { _, newValue ->
            if (!(newValue as Boolean)) {
                ClipboardEntryNotificationService.removeNotification(context)
            }
            true
        }

        val copyPasswordPreference: TwoStatePreference? =
            findPreference(getString(R.string.allow_copy_password_key))
        copyPasswordPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean && context != null) {
                val message = getString(R.string.allow_copy_password_warning) +
                        "\n\n" +
                        getString(R.string.clipboard_warning)
                AlertDialog.Builder(requireContext())
                    .setMessage(message)
                    .setView(R.layout.alert_dialog_layout)
                    .create()
                    .apply {
                        setButton(AlertDialog.BUTTON_POSITIVE, getText(R.string.enable))
                        { dialog, _ ->
                            dialog.dismiss()
                        }
                        setButton(AlertDialog.BUTTON_NEGATIVE, getText(R.string.disable))
                        { dialog, _ ->
                            copyPasswordPreference.isChecked = false
                            dialog.dismiss()
                        }
                        show()
                    }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.let { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                findPreference<TwoStatePreference?>(getString(R.string.settings_autofill_enable_key))?.let { autoFillEnablePreference ->
                    val autofillManager = activity.getSystemService(AutofillManager::class.java)
                    autoFillEnablePreference.isChecked = autofillManager != null
                            && autofillManager.hasEnabledAutofillServices()
                }
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {

        var otherDialogFragment = false
        var dialogFragment: DialogFragment? = null

        when (preference.key) {
            getString(R.string.clipboard_timeout_key) -> {
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
