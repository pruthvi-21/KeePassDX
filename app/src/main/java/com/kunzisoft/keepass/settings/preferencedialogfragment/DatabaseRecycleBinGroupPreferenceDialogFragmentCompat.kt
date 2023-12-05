package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.settings.preferencedialogfragment.adapter.ListRadioItemAdapter

class DatabaseRecycleBinGroupPreferenceDialogFragmentCompat
    : DatabaseSavePreferenceDialogFragmentCompat(),
    ListRadioItemAdapter.RadioItemSelectedCallback<Group> {

    private var mGroupsAdapter: ListRadioItemAdapter<Group>? = null
    private var mGroupRecycleBin: Group? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        val recyclerView = view.findViewById<RecyclerView>(R.id.pref_dialog_list)
        recyclerView.layoutManager = LinearLayoutManager(context)

        activity?.let { activity ->
            mGroupsAdapter = ListRadioItemAdapter(activity)
            mGroupsAdapter?.setRadioItemSelectedCallback(this)
            recyclerView.adapter = mGroupsAdapter
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        database?.let {
            mGroupRecycleBin = database.recycleBin
            mGroupsAdapter?.setItems(database.getAllGroupsWithoutRoot(), mGroupRecycleBin)
        }
    }

    override fun onItemSelected(item: Group) {
        mGroupRecycleBin = item
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        if (positiveResult) {
            database?.let {
                if (database.allowConfigurableRecycleBin) {
                    val oldGroup = database.recycleBin
                    val newGroup = mGroupRecycleBin
                    database.setRecycleBin(newGroup)
                    saveRecycleBin(oldGroup, newGroup)
                }
            }
        }
    }

    companion object {

        fun newInstance(
            key: String
        ): DatabaseRecycleBinGroupPreferenceDialogFragmentCompat {
            val fragment = DatabaseRecycleBinGroupPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
