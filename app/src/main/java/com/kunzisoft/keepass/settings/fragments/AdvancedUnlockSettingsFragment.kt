package com.kunzisoft.keepass.settings.fragments

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.UnavailableFeatureDialogFragment
import com.kunzisoft.keepass.biometric.AdvancedUnlockManager
import com.kunzisoft.keepass.settings.preferencedialogfragment.DurationDialogFragmentCompat
import com.kunzisoft.keepass.utils.UriUtil.openUrl

class AdvancedUnlockSettingsFragment : PreferenceFragmentCompat() {

    private var warningAlertDialog: AlertDialog? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_advanced_unlock, rootKey)

        activity?.let { activity ->

            val biometricUnlockEnablePreference: TwoStatePreference? =
                findPreference(getString(R.string.biometric_unlock_enable_key))
            val deviceCredentialUnlockEnablePreference: TwoStatePreference? =
                findPreference(getString(R.string.device_credential_unlock_enable_key))
            val autoOpenPromptPreference: TwoStatePreference? =
                findPreference(getString(R.string.biometric_auto_open_prompt_key))
            val tempAdvancedUnlockPreference: TwoStatePreference? =
                findPreference(getString(R.string.temp_advanced_unlock_enable_key))

            val biometricUnlockSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AdvancedUnlockManager.biometricUnlockSupported(activity)
            } else false
            biometricUnlockEnablePreference?.apply {
                // False if under Marshmallow
                if (!biometricUnlockSupported) {
                    isChecked = false
                    setOnPreferenceClickListener { preference ->
                        (preference as TwoStatePreference).isChecked = false
                        UnavailableFeatureDialogFragment.getInstance(Build.VERSION_CODES.M)
                            .show(parentFragmentManager, "unavailableFeatureDialog")
                        false
                    }
                } else {
                    setOnPreferenceClickListener {
                        val biometricChecked = biometricUnlockEnablePreference.isChecked
                        val deviceCredentialChecked =
                            deviceCredentialUnlockEnablePreference?.isChecked ?: false
                        if (!biometricChecked) {
                            biometricUnlockEnablePreference.isChecked = true
                            warningMessage(activity, keystoreWarning = false, deleteKeys = true) {
                                biometricUnlockEnablePreference.isChecked = false
                                autoOpenPromptPreference?.isEnabled = deviceCredentialChecked
                                tempAdvancedUnlockPreference?.isEnabled = deviceCredentialChecked
                            }
                        } else {
                            if (deviceCredentialChecked) {
                                biometricUnlockEnablePreference.isChecked = false
                                warningMessage(
                                    activity,
                                    keystoreWarning = true,
                                    deleteKeys = true
                                ) {
                                    biometricUnlockEnablePreference.isChecked = true
                                    deviceCredentialUnlockEnablePreference?.isChecked = false
                                }
                            } else {
                                biometricUnlockEnablePreference.isChecked = false
                                warningMessage(
                                    activity,
                                    keystoreWarning = true,
                                    deleteKeys = false
                                ) {
                                    biometricUnlockEnablePreference.isChecked = true
                                    autoOpenPromptPreference?.isEnabled = true
                                    tempAdvancedUnlockPreference?.isEnabled = true
                                }
                            }
                        }
                        true
                    }
                }
            }

            val deviceCredentialUnlockSupported =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AdvancedUnlockManager.deviceCredentialUnlockSupported(activity)
                } else false
            deviceCredentialUnlockEnablePreference?.apply {
                // Biometric unlock already checked
                if (biometricUnlockEnablePreference?.isChecked == true)
                    isChecked = false
                if (!deviceCredentialUnlockSupported) {
                    isChecked = false
                    setOnPreferenceClickListener { preference ->
                        (preference as TwoStatePreference).isChecked = false
                        UnavailableFeatureDialogFragment.getInstance(Build.VERSION_CODES.M)
                            .show(parentFragmentManager, "unavailableFeatureDialog")
                        false
                    }
                } else {
                    setOnPreferenceClickListener {
                        val deviceCredentialChecked =
                            deviceCredentialUnlockEnablePreference.isChecked
                        val biometricChecked = biometricUnlockEnablePreference?.isChecked ?: false
                        if (!deviceCredentialChecked) {
                            deviceCredentialUnlockEnablePreference.isChecked = true
                            warningMessage(activity, keystoreWarning = false, deleteKeys = true) {
                                deviceCredentialUnlockEnablePreference.isChecked = false
                                autoOpenPromptPreference?.isEnabled = biometricChecked
                                tempAdvancedUnlockPreference?.isEnabled = biometricChecked
                            }
                        } else {
                            if (biometricChecked) {
                                deviceCredentialUnlockEnablePreference.isChecked = false
                                warningMessage(
                                    activity,
                                    keystoreWarning = true,
                                    deleteKeys = true
                                ) {
                                    deviceCredentialUnlockEnablePreference.isChecked = true
                                    biometricUnlockEnablePreference?.isChecked = false
                                }
                            } else {
                                deviceCredentialUnlockEnablePreference.isChecked = false
                                warningMessage(
                                    activity,
                                    keystoreWarning = true,
                                    deleteKeys = false
                                ) {
                                    deviceCredentialUnlockEnablePreference.isChecked = true
                                    autoOpenPromptPreference?.isEnabled = true
                                    tempAdvancedUnlockPreference?.isEnabled = true
                                }
                            }
                        }
                        true
                    }
                }
            }

            autoOpenPromptPreference?.isEnabled = biometricUnlockEnablePreference?.isChecked == true
                    || deviceCredentialUnlockEnablePreference?.isChecked == true
            tempAdvancedUnlockPreference?.isEnabled =
                biometricUnlockEnablePreference?.isChecked == true
                        || deviceCredentialUnlockEnablePreference?.isChecked == true

            tempAdvancedUnlockPreference?.setOnPreferenceClickListener {
                tempAdvancedUnlockPreference.isChecked = !tempAdvancedUnlockPreference.isChecked
                warningMessage(activity, keystoreWarning = false, deleteKeys = true) {
                    tempAdvancedUnlockPreference.isChecked = !tempAdvancedUnlockPreference.isChecked
                }
                true
            }

            val deleteKeysFingerprints: Preference? =
                findPreference(getString(R.string.biometric_delete_all_key_key))
            if (biometricUnlockSupported || deviceCredentialUnlockSupported) {
                deleteKeysFingerprints?.setOnPreferenceClickListener {
                    warningMessage(activity, keystoreWarning = false, deleteKeys = true)
                    false
                }
            } else {
                deleteKeysFingerprints?.isEnabled = false
            }
        }

        findPreference<Preference>(getString(R.string.advanced_unlock_explanation_key))?.setOnPreferenceClickListener {
            context?.openUrl(R.string.advanced_unlock_explanation_url)
            false
        }
    }

    private fun warningMessage(
        activity: FragmentActivity,
        keystoreWarning: Boolean,
        deleteKeys: Boolean,
        validate: (() -> Unit)? = null
    ) {
        var message = ""
        if (keystoreWarning) {
            message += resources.getString(R.string.advanced_unlock_prompt_store_credential_message)
            message += "\n\n" + resources.getString(R.string.advanced_unlock_keystore_warning)
        }
        if (keystoreWarning && deleteKeys) {
            message += "\n\n"
        }
        if (deleteKeys) {
            message += resources.getString(R.string.advanced_unlock_delete_all_key_warning)
        }
        warningAlertDialog = AlertDialog.Builder(activity)
            .setView(R.layout.alert_dialog_layout)
            .setMessage(message)
            .setTitle(R.string.warning)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(
                resources.getString(android.R.string.ok)
            ) { _, _ ->
                validate?.invoke()
                warningAlertDialog?.setOnDismissListener(null)
                if (deleteKeys && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AdvancedUnlockManager.deleteAllEntryKeysInKeystoreForBiometric(activity)
                }
            }
            .setNegativeButton(
                resources.getString(android.R.string.cancel)
            ) { _, _ -> }
            .create()
        warningAlertDialog?.show()
    }

    override fun onPause() {
        warningAlertDialog?.dismiss()
        super.onPause()
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        var otherDialogFragment = false

        var dialogFragment: DialogFragment? = null
        // Main Preferences
        when (preference.key) {
            getString(R.string.temp_advanced_unlock_timeout_key) -> {
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
