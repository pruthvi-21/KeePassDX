package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import com.kunzisoft.keepass.database.ContextualDatabase

class DatabaseNamePreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        inputText = database?.name ?: ""
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        if (positiveResult) {
            database?.let {
                val newName = inputText
                val oldName = database.name
                database.name = newName
                saveName(oldName, newName)
            }
        }
    }

    companion object {

        fun newInstance(
            key: String
        ): DatabaseNamePreferenceDialogFragmentCompat {
            val fragment = DatabaseNamePreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
