package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.settings.preferencedialogfragment.adapter.ListRadioItemAdapter

class DatabaseTemplatesGroupPreferenceDialogFragmentCompat
    : DatabaseSavePreferenceDialogFragmentCompat(),
        ListRadioItemAdapter.RadioItemSelectedCallback<Group> {

    private var mGroupsAdapter: ListRadioItemAdapter<Group>? = null
    private var mGroupTemplates: Group? = null

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
            mGroupTemplates = database.templatesGroup
            mGroupsAdapter?.setItems(database.getAllGroupsWithoutRoot(), mGroupTemplates)
        }
    }

    override fun onItemSelected(item: Group) {
        mGroupTemplates = item
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        if (positiveResult) {
            database?.let {
                if (database.allowTemplatesGroup) {
                    val oldGroup = database.templatesGroup
                    val newGroup = mGroupTemplates
                    database.setTemplatesGroup(newGroup)
                    saveTemplatesGroup(oldGroup, newGroup)
                }
            }
        }
    }

    companion object {

        fun newInstance(
                key: String): DatabaseTemplatesGroupPreferenceDialogFragmentCompat {
            val fragment = DatabaseTemplatesGroupPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
