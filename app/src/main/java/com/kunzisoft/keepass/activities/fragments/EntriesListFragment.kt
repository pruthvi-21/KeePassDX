package com.kunzisoft.keepass.activities.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.EntriesListAdapter
import com.kunzisoft.keepass.adapters.NodesAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.EntriesSortView

class EntriesListFragment : DatabaseFragment(),
    EntriesSortView.SortSelectionListener {

    private var recyclerView: RecyclerView? = null
    private var sortView: EntriesSortView? = null

    private var adapter: EntriesListAdapter? = null
    private var entriesList: List<Entry>? = null

    private var nodeClickListener: GroupFragment.NodeClickListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            nodeClickListener = context as GroupFragment.NodeClickListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(
                context.toString()
                        + " must implement " + NodesAdapter.NodeClickCallback::class.java.name
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entries_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sortView = view.findViewById(R.id.sort_view)
        recyclerView = view.findViewById(R.id.tags_list)
        recyclerView!!.layoutManager = LinearLayoutManager(context)


    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        context?.let { context ->
            database?.let {
                entriesList = database.getEntries()

                entriesList?.let {
                    adapter = EntriesListAdapter(context, database).apply {
                        setOnNodeClickListener(object : EntriesListAdapter.NodeClickCallback {
                            override fun onNodeClick(database: ContextualDatabase, node: Node) {
                                nodeClickListener?.onNodeClick(database, node)
                            }

                            override fun onNodeLongClick(
                                database: ContextualDatabase,
                                node: Node
                            ): Boolean {
                                return true
                            }
                        })
                    }
                    recyclerView?.adapter = adapter
                    adapter?.rebuildList(it)
                }
            }
        }
        sortView?.setOnSortSelectionListener(this)
    }

    fun updateList() {
        mDatabase?.getEntries()?.let {
            adapter?.rebuildList(it)
        }
    }

    override fun onSortSelected(
        sortNodeEnum: SortNodeEnum,
        sortNodeParameters: SortNodeEnum.SortNodeParameters,
        showRecycleBinItems: Boolean
    ) {
        // Save setting
        context?.let {
            PreferencesUtil.saveNodeEntriesSort(
                it,
                sortNodeEnum,
                sortNodeParameters,
                showRecycleBinItems
            )
        }

        // Tell the adapter to refresh it's list
        try {
            adapter?.notifyChangeSort(sortNodeEnum, sortNodeParameters, showRecycleBinItems)
            updateList()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to sort the list", e)
        }
    }

    companion object {
        const val TAG = "EntriesListFragment"
    }
}