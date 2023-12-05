package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.settings.preferencedialogfragment.adapter.ListRadioItemAdapter

class DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat
    : DatabaseSavePreferenceDialogFragmentCompat(),
    ListRadioItemAdapter.RadioItemSelectedCallback<EncryptionAlgorithm> {

    private var mRecyclerView: RecyclerView? = null
    private var mEncryptionAlgorithmAdapter: ListRadioItemAdapter<EncryptionAlgorithm>? = null
    private var algorithmSelected: EncryptionAlgorithm? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        setExplanationText(R.string.encryption_explanation)

        mRecyclerView = view.findViewById(R.id.pref_dialog_list)
        mRecyclerView?.layoutManager = LinearLayoutManager(context)

        activity?.let { activity ->
            mEncryptionAlgorithmAdapter = ListRadioItemAdapter(activity)
            mEncryptionAlgorithmAdapter?.setRadioItemSelectedCallback(this)
            mRecyclerView?.adapter = mEncryptionAlgorithmAdapter
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        database?.let {
            algorithmSelected = database.encryptionAlgorithm
            mEncryptionAlgorithmAdapter?.setItems(
                database.availableEncryptionAlgorithms,
                algorithmSelected
            )
        }
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        if (positiveResult) {
            database?.let {
                if (algorithmSelected != null) {
                    val newAlgorithm = algorithmSelected
                    val oldAlgorithm = database.encryptionAlgorithm
                    database.encryptionAlgorithm = newAlgorithm

                    if (oldAlgorithm != null && newAlgorithm != null)
                        saveEncryption(oldAlgorithm, newAlgorithm)
                }
            }
        }
    }

    override fun onItemSelected(item: EncryptionAlgorithm) {
        this.algorithmSelected = item
    }

    companion object {

        fun newInstance(key: String): DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat {
            val fragment = DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
