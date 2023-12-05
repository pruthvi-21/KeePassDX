package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase

class DatabaseRoundsPreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        explanationText = getString(R.string.rounds_explanation)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        inputText = database?.numberKeyEncryptionRounds?.toString() ?: MIN_ITERATIONS.toString()
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        if (positiveResult) {
            database?.let {
                var rounds: Long = try {
                    inputText.toLong()
                } catch (e: NumberFormatException) {
                    MIN_ITERATIONS
                }
                if (rounds < MIN_ITERATIONS) {
                    rounds = MIN_ITERATIONS
                }
                // TODO Max iterations

                val oldRounds = database.numberKeyEncryptionRounds
                try {
                    database.numberKeyEncryptionRounds = rounds
                } catch (e: NumberFormatException) {
                    Toast.makeText(context, R.string.error_rounds_too_large, Toast.LENGTH_LONG)
                        .show()
                    database.numberKeyEncryptionRounds = Long.MAX_VALUE
                }

                saveIterations(oldRounds, rounds)
            }
        }
    }

    companion object {

        const val MIN_ITERATIONS = 1L

        fun newInstance(key: String): DatabaseRoundsPreferenceDialogFragmentCompat {
            val fragment = DatabaseRoundsPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
