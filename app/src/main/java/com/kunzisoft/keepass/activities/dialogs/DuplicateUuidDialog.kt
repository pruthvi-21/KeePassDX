package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R

class DuplicateUuidDialog : DialogFragment() {

    var positiveAction: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(activity).apply {
                val message = getString(R.string.contains_duplicate_uuid) +
                        "\n\n" + getString(R.string.contains_duplicate_uuid_procedure)
                setMessage(message)
                setView(R.layout.alert_dialog_layout)
                setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                    positiveAction?.invoke()
                    dismiss()
                }
                setNegativeButton(getString(android.R.string.cancel)) { _, _ -> dismiss() }
            }
            // Create the AlertDialog object and return it
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        this.dismiss()
    }
}
