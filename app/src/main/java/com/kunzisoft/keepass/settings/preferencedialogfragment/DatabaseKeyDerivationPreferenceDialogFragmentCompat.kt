package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine
import com.kunzisoft.keepass.settings.preferencedialogfragment.adapter.ListRadioItemAdapter

class DatabaseKeyDerivationPreferenceDialogFragmentCompat
    : DatabaseSavePreferenceDialogFragmentCompat(),
    ListRadioItemAdapter.RadioItemSelectedCallback<KdfEngine> {

    private var kdfEngineSelected: KdfEngine? = null
    private var mKdfAdapter: ListRadioItemAdapter<KdfEngine>? = null
    private var roundPreference: Preference? = null
    private var memoryPreference: Preference? = null
    private var parallelismPreference: Preference? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        setExplanationText(R.string.kdf_explanation)

        val recyclerView = view.findViewById<RecyclerView>(R.id.pref_dialog_list)
        recyclerView.layoutManager = LinearLayoutManager(context)

        activity?.let { activity ->
            mKdfAdapter = ListRadioItemAdapter(activity)
            mKdfAdapter?.setRadioItemSelectedCallback(this)
            recyclerView.adapter = mKdfAdapter
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        database?.let {
            kdfEngineSelected = database.kdfEngine
            mKdfAdapter?.setItems(database.availableKdfEngines, kdfEngineSelected)
        }
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        if (positiveResult) {
            database?.let {
                if (database.allowKdfModification) {
                    val newKdfEngine = kdfEngineSelected
                    val oldKdfEngine = database.kdfEngine
                    if (newKdfEngine != null && oldKdfEngine != null) {
                        database.kdfEngine = newKdfEngine
                        saveKeyDerivation(oldKdfEngine, newKdfEngine)
                    }
                }
            }
        }
    }

    fun setRoundPreference(preference: Preference?) {
        this.roundPreference = preference
    }

    fun setMemoryPreference(preference: Preference?) {
        this.memoryPreference = preference
    }

    fun setParallelismPreference(preference: Preference?) {
        this.parallelismPreference = preference
    }

    override fun onItemSelected(item: KdfEngine) {
        kdfEngineSelected = item
    }

    companion object {

        fun newInstance(key: String): DatabaseKeyDerivationPreferenceDialogFragmentCompat {
            val fragment = DatabaseKeyDerivationPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
