package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import com.kunzisoft.keepass.database.ContextualDatabase

class DatabaseDefaultUsernamePreferenceDialogFragmentCompat :
    DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        inputText = database?.defaultUsername ?: ""
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        database?.let {
            if (positiveResult) {
                val newDefaultUsername = inputText
                val oldDefaultUsername = database.defaultUsername
                database.defaultUsername = newDefaultUsername
                saveDefaultUsername(oldDefaultUsername, newDefaultUsername)
            }
        }
    }

    companion object {

        fun newInstance(key: String): DatabaseDefaultUsernamePreferenceDialogFragmentCompat {
            val fragment = DatabaseDefaultUsernamePreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
