package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.SnapFileDatabaseInfo
import com.kunzisoft.keepass.utils.getParcelableCompat


class DatabaseChangedDialogFragment : DatabaseDialogFragment() {

    var actionDatabaseListener: ActionDatabaseChangedListener? = null

    override fun onPause() {
        super.onPause()
        actionDatabaseListener = null
        this.dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->

            val oldSnapFileDatabaseInfo: SnapFileDatabaseInfo? =
                arguments?.getParcelableCompat(OLD_FILE_DATABASE_INFO)
            val newSnapFileDatabaseInfo: SnapFileDatabaseInfo? =
                arguments?.getParcelableCompat(NEW_FILE_DATABASE_INFO)

            if (oldSnapFileDatabaseInfo != null && newSnapFileDatabaseInfo != null) {
                // Use the Builder class for convenient dialog construction
                val builder = AlertDialog.Builder(activity)
                builder.setView(R.layout.alert_dialog_layout)

                val stringBuilder = SpannableStringBuilder()
                if (newSnapFileDatabaseInfo.exists) {
                    stringBuilder.append(getString(R.string.warning_database_info_changed))
                    stringBuilder.append(
                        "\n\n" + oldSnapFileDatabaseInfo.toString(activity)
                                + "\n→\n" +
                                newSnapFileDatabaseInfo.toString(activity) + "\n\n"
                    )
                    stringBuilder.append(getString(R.string.warning_database_info_changed_options))
                } else {
                    stringBuilder.append(getString(R.string.warning_database_revoked))
                }
                builder.setMessage(stringBuilder)
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    actionDatabaseListener?.validateDatabaseChanged()
                }
                return builder.create()
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    interface ActionDatabaseChangedListener {
        fun validateDatabaseChanged()
    }

    companion object {

        const val DATABASE_CHANGED_DIALOG_TAG = "databaseChangedDialogFragment"
        private const val OLD_FILE_DATABASE_INFO = "OLD_FILE_DATABASE_INFO"
        private const val NEW_FILE_DATABASE_INFO = "NEW_FILE_DATABASE_INFO"

        fun getInstance(
            oldSnapFileDatabaseInfo: SnapFileDatabaseInfo,
            newSnapFileDatabaseInfo: SnapFileDatabaseInfo
        )
                : DatabaseChangedDialogFragment {
            val fragment = DatabaseChangedDialogFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(OLD_FILE_DATABASE_INFO, oldSnapFileDatabaseInfo)
                putParcelable(NEW_FILE_DATABASE_INFO, newSnapFileDatabaseInfo)
            }
            return fragment
        }
    }
}
