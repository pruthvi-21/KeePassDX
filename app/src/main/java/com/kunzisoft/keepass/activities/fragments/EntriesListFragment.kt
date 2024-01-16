package com.kunzisoft.keepass.activities.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.libx.ui.views.SpinnerX
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.EntriesListAdapter
import com.kunzisoft.keepass.adapters.NodesAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.Tags
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.EntriesSortView

class EntriesListFragment : DatabaseFragment(),
    EntriesSortView.SortSelectionListener,
    AdapterView.OnItemSelectedListener,
    EntriesListAdapter.NodeClickCallback {

    private var tagsSpinner: SpinnerX? = null
    private var tagsLabelView: TextView? = null
    private var recyclerView: RecyclerView? = null
    private var sortView: EntriesSortView? = null

    private var entriesAdapter: EntriesListAdapter? = null
    private var tagsAdapter: ArrayAdapter<String>? = null
    private var tagsList = ArrayList<String>()

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

        tagsSpinner = view.findViewById(R.id.tags_spinner)
        tagsLabelView = view.findViewById(R.id.sort_tags_view)
        sortView = view.findViewById(R.id.sort_view)
        recyclerView = view.findViewById(R.id.tags_list)
        recyclerView!!.layoutManager = LinearLayoutManager(context)

        tagsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tagsList)
        tagsAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        sortView?.setOnSortSelectionListener(this)
        tagsSpinner?.onItemSelectedListener = this

        tagsLabelView?.setOnClickListener {
            tagsSpinner?.performClick()
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        context ?: return
        database ?: return

        entriesAdapter = EntriesListAdapter(requireContext(), database)
        entriesAdapter?.setOnNodeClickListener(this)

        recyclerView?.adapter = entriesAdapter
        tagsSpinner?.adapter = tagsAdapter
        updateList()
    }

    fun updateList(updateTags: Boolean = true) {
        val entries = mDatabase?.getEntries() ?: return
        entriesAdapter?.rebuildList(entries)

        if (updateTags) {
            tagsList.apply {
                clear()
                add("All")

                Tags().apply {
                    entries.forEach {
                        put(it.tags)
                    }
                    addAll(toList().sorted())
                }
            }
            tagsAdapter?.notifyDataSetChanged()
        }
    }


    override fun onNodeClick(database: ContextualDatabase, node: Node) {
        nodeClickListener?.onNodeClick(database, node)
    }

    override fun onNodeLongClick(database: ContextualDatabase, node: Node): Boolean {
        return true
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        tagsLabelView?.text = tagsList[position]

        val tag = if (position == 0) "" else tagsList[position]
        entriesAdapter?.notifyCustomTagSelected(tag)
        updateList(false)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

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
            entriesAdapter?.notifyChangeSort(sortNodeEnum, sortNodeParameters, showRecycleBinItems)
            updateList(false)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to sort the list", e)
        }
    }

    companion object {
        const val TAG = "EntriesListFragment"
    }
}