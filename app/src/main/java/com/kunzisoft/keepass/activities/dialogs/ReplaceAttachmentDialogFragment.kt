package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.utils.getParcelableCompat

/**
 * Custom Dialog to confirm big file to upload
 */
class ReplaceFileDialogFragment : DatabaseDialogFragment() {

    private var mActionChooseListener: ActionChooseListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            mActionChooseListener = context as ActionChooseListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString()
                    + " must implement " + ActionChooseListener::class.java.name)
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
            builder.setMessage(SpannableStringBuilder().apply {
                append(getString(R.string.warning_replace_file))
                append("\n\n")
                append(getString(R.string.warning_sure_add_file))
            })
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                mActionChooseListener?.onValidateReplaceFile(
                        arguments?.getParcelableCompat(KEY_FILE_URI),
                        arguments?.getParcelableCompat(KEY_ENTRY_ATTACHMENT))
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
        fun onValidateReplaceFile(attachmentToUploadUri: Uri?, attachment: Attachment?)
    }

    companion object {
        private const val KEY_FILE_URI = "KEY_FILE_URI"
        private const val KEY_ENTRY_ATTACHMENT = "KEY_ENTRY_ATTACHMENT"

        fun build(attachmentToUploadUri: Uri,
                  attachment: Attachment): ReplaceFileDialogFragment {
            val fragment = ReplaceFileDialogFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(KEY_FILE_URI, attachmentToUploadUri)
                putParcelable(KEY_ENTRY_ATTACHMENT, attachment)
            }
            return fragment
        }
    }
}
