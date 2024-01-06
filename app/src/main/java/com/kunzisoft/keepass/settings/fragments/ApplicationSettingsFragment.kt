package com.kunzisoft.keepass.settings.fragments

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.settings.SettingsActivity
import com.kunzisoft.keepass.settings.preferencedialogfragment.DurationDialogFragmentCompat
import com.kunzisoft.keepass.utils.UriUtil.releaseAllUnnecessaryPermissionUris
import org.joda.time.DateTime
import java.util.Properties

class ApplicationSettingsFragment : PreferenceFragmentCompat() {

    private var mExternalFileHelper: ExternalFileHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activity = activity as? SettingsActivity

        activity?.let {

            mExternalFileHelper = ExternalFileHelper(this)
            mExternalFileHelper?.buildOpenDocument { selectedFileUri ->
                // Import app settings result
                try {
                    selectedFileUri?.let { uri ->
                        val appProperties = Properties()
                        activity.contentResolver?.openInputStream(uri)?.use { inputStream ->
                            appProperties.load(inputStream)
                        }
                        PreferencesUtil.setAppProperties(activity, appProperties)

                        // Restart the current activity
                        (activity as? SettingsActivity)?.reloadActivity()
                        Toast.makeText(
                            activity,
                            R.string.success_import_app_properties,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        activity,
                        R.string.error_import_app_properties,
                        Toast.LENGTH_LONG
                    )
                        .show()
                    Log.e(TAG, "Unable to import app settings", e)
                }
            }
            mExternalFileHelper?.buildCreateDocument { createdFileUri ->
                // Export app settings result
                try {
                    createdFileUri?.let { uri ->
                        activity.contentResolver?.openOutputStream(uri)?.use { outputStream ->
                            PreferencesUtil
                                .getAppProperties(activity)
                                .store(outputStream, getString(R.string.description_app_properties))
                        }
                        Toast.makeText(
                            activity,
                            R.string.success_export_app_properties,
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        activity,
                        R.string.error_export_app_properties,
                        Toast.LENGTH_LONG
                    )
                        .show()
                    Log.e(DatabaseLockActivity.TAG, "Unable to export app settings", e)
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_application, rootKey)

        activity?.let { activity ->
            findPreference<Preference>(getString(R.string.remember_database_locations_key))?.setOnPreferenceChangeListener { _, newValue ->
                if (!(newValue as Boolean)) {
                    FileDatabaseHistoryAction.getInstance(activity.applicationContext).deleteAll {
                        activity.releaseAllUnnecessaryPermissionUris()
                    }
                }
                true
            }

            findPreference<Preference>(getString(R.string.remember_keyfile_locations_key))?.setOnPreferenceChangeListener { _, newValue ->
                if (!(newValue as Boolean)) {
                    FileDatabaseHistoryAction.getInstance(activity.applicationContext)
                        .deleteAllKeyFiles {
                            activity.releaseAllUnnecessaryPermissionUris()
                        }
                }
                true
            }

            findPreference<Preference>(getString(R.string.import_app_properties_key))?.setOnPreferenceClickListener { _ ->
                (activity as? SettingsActivity?)?.apply {
                    importAppProperties()
                }
                true
            }

            findPreference<Preference>(getString(R.string.export_app_properties_key))?.setOnPreferenceClickListener { _ ->
                (activity as? SettingsActivity?)?.apply {
                    exportAppProperties()
                }
                true
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {

        var otherDialogFragment = false

        var dialogFragment: DialogFragment? = null
        // Main Preferences
        when (preference.key) {
            getString(R.string.app_timeout_key) -> {
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

    fun importAppProperties() {
        mExternalFileHelper?.openDocument()
    }

    fun exportAppProperties() {
        mExternalFileHelper?.createDocument(
            getString(
                R.string.app_properties_file_name,
                DateTime.now().toLocalDateTime().toString("yyyy-MM-dd'_'HH-mm")
            )
        )
    }

    companion object {
        private val TAG = ApplicationSettingsFragment::class.java.simpleName
        private const val TAG_PREF_FRAGMENT = "TAG_PREF_FRAGMENT"
    }
}
