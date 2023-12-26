package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.utils.getParcelableCompat

class PasswordEncodingDialogFragment : DialogFragment() {

    private var mListener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as Listener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                context.toString()
                        + " must implement " + Listener::class.java.name
            )
        }
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val databaseUri: Uri? = savedInstanceState?.getParcelableCompat(DATABASE_URI_KEY)
        val mainCredential: MainCredential =
            savedInstanceState?.getParcelableCompat(MAIN_CREDENTIAL) ?: MainCredential()

        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            builder.setView(R.layout.alert_dialog_layout)
            builder.setMessage(activity.getString(R.string.warning_password_encoding))
                .setTitle(R.string.warning)
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                mListener?.onPasswordEncodingValidateListener(
                    databaseUri,
                    mainCredential
                )
            }
            builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    interface Listener {
        fun onPasswordEncodingValidateListener(
            databaseUri: Uri?,
            mainCredential: MainCredential
        )
    }

    companion object {

        private const val DATABASE_URI_KEY = "DATABASE_URI_KEY"
        private const val MAIN_CREDENTIAL = "MAIN_CREDENTIAL"

        fun getInstance(
            databaseUri: Uri,
            mainCredential: MainCredential
        ): DialogFragment {
            val fragment = DialogFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(DATABASE_URI_KEY, databaseUri)
                putParcelable(MAIN_CREDENTIAL, mainCredential)
            }
            return fragment
        }
    }
}
