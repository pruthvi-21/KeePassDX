package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.view.View
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase

class DatabaseParallelismPreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        setExplanationText(R.string.parallelism_explanation)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        inputText = database?.parallelism?.toString() ?: MIN_PARALLELISM.toString()
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        if (positiveResult) {
            database?.let {
                val parallelism: Long = try {
                    inputText.toLong()
                } catch (e: NumberFormatException) {
                    MIN_PARALLELISM
                }

                val oldParallelism = database.parallelism
                database.parallelism = parallelism

                saveParallelism(oldParallelism, parallelism)
            }
        }
    }

    companion object {

        const val MIN_PARALLELISM = 1L

        fun newInstance(key: String): DatabaseParallelismPreferenceDialogFragmentCompat {
            val fragment = DatabaseParallelismPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
