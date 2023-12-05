package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase

class DatabaseRemoveUnlinkedDataPreferenceDialogFragmentCompat :
    DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        explanationText = SpannableStringBuilder().apply {
            append(getString(R.string.warning_remove_unlinked_attachment))
            append("\n\n")
            append(getString(R.string.warning_sure_remove_data))
        }.toString()
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        database?.let {
            if (positiveResult) {
                removeUnlinkedData()
            }
        }
    }

    companion object {

        fun newInstance(key: String): DatabaseRemoveUnlinkedDataPreferenceDialogFragmentCompat {
            val fragment = DatabaseRemoveUnlinkedDataPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
