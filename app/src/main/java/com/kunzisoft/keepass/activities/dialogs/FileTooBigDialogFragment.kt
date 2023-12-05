package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.getParcelableCompat

/**
 * Custom Dialog to confirm big file to upload
 */
class FileTooBigDialogFragment : DialogFragment() {

    private var mActionChooseListener: ActionChooseListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            mActionChooseListener = context as ActionChooseListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                context.toString()
                        + " must implement " + ActionChooseListener::class.java.name
            )
        }
    }

    override fun onDetach() {
        mActionChooseListener = null
        super.onDetach()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(activity)
            builder.setView(R.layout.alert_dialog_layout)
            builder.setMessage(SpannableStringBuilder().apply {
                append(getString(R.string.warning_file_too_big))
                append("\n\n")
                append(getString(R.string.warning_sure_add_file))
            })
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                mActionChooseListener?.onValidateUploadFileTooBig(
                    arguments?.getParcelableCompat(KEY_FILE_URI),
                    arguments?.getString(KEY_FILE_NAME)
                )
            }
            builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                dismiss()
            }
            // Create the AlertDialog object and return it
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    interface ActionChooseListener {
        fun onValidateUploadFileTooBig(attachmentToUploadUri: Uri?, fileName: String?)
    }

    companion object {
        const val MAX_WARNING_BINARY_FILE = 5242880

        private const val KEY_FILE_URI = "KEY_FILE_URI"
        private const val KEY_FILE_NAME = "KEY_FILE_NAME"

        fun build(
            attachmentToUploadUri: Uri,
            fileName: String
        ): FileTooBigDialogFragment {
            val fragment = FileTooBigDialogFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(KEY_FILE_URI, attachmentToUploadUri)
                putString(KEY_FILE_NAME, fileName)
            }
            return fragment
        }
    }
}
