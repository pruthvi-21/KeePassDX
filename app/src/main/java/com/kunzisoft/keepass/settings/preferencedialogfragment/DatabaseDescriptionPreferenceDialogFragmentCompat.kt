package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import com.kunzisoft.keepass.database.ContextualDatabase

class DatabaseDescriptionPreferenceDialogFragmentCompat :
    DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        inputText = database?.description ?: ""
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        database?.let {
            if (positiveResult) {
                val newDescription = inputText
                val oldDescription = database.description
                database.description = newDescription
                saveDescription(oldDescription, newDescription)
            }
        }
    }

    companion object {

        fun newInstance(key: String): DatabaseDescriptionPreferenceDialogFragmentCompat {
            val fragment = DatabaseDescriptionPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
