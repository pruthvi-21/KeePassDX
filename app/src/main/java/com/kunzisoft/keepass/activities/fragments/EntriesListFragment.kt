package com.kunzisoft.keepass.activities.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.libx.ui.views.SpinnerX
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.EntriesListAdapter
import com.kunzisoft.keepass.adapters.NodesAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.Tags
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.KeyboardUtil.hideKeyboard
import com.kunzisoft.keepass.view.EntriesSortView
import java.util.LinkedList

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

    private val listActionNodes = LinkedList<Entry>()

    var nodeActionSelectionMode = false
        private set

    var nodeActionPasteMode: GroupFragment.PasteMode = GroupFragment.PasteMode.UNDEFINED
        private set

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
        if (node !is Entry) return
        if (nodeActionSelectionMode) {
            if (listActionNodes.contains(node)) {
                // Remove selected item if already selected
                listActionNodes.remove(node)
            } else {
                // Add selected item if not already selected
                listActionNodes.add(node)
            }
            nodeClickListener?.onNodeSelected(database, listActionNodes)
            entriesAdapter?.setActionNodes(listActionNodes)
            entriesAdapter?.notifyNodeChanged(node)
        } else {
            nodeClickListener?.onNodeClick(database, node)
        }
    }

    override fun onNodeLongClick(database: ContextualDatabase, node: Node): Boolean {
        if (node !is Entry) return false
        if (nodeActionPasteMode == GroupFragment.PasteMode.UNDEFINED) {
            // Select the first item after a long click
            if (!listActionNodes.contains(node))
                listActionNodes.add(node)

            nodeClickListener?.onNodeSelected(database, listActionNodes)

            entriesAdapter?.setActionNodes(listActionNodes)
            entriesAdapter?.notifyNodeChanged(node)
            activity?.hideKeyboard()
        }
        return true
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        tagsLabelView?.text = tagsList[position]

        val tag = if (position == 0) "" else tagsList[position]
        entriesAdapter?.notifyCustomTagSelected(tag)
        updateList(false)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    fun actionNodesCallback(
        database: ContextualDatabase,
        nodes: List<Node>,
        menuListener: GroupFragment.NodesActionMenuListener?,
        onDestroyActionMode: (mode: ActionMode?) -> Unit
    ): ActionMode.Callback {

        return object : ActionMode.Callback {

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                nodeActionSelectionMode = false
                nodeActionPasteMode = GroupFragment.PasteMode.UNDEFINED
                sortView?.isEnabled = false
                tagsSpinner?.isEnabled = false
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menu?.clear()
                if (nodeActionPasteMode != GroupFragment.PasteMode.UNDEFINED) {
                    mode?.menuInflater?.inflate(R.menu.node_paste_menu, menu)
                } else {
                    nodeActionSelectionMode = true
                    mode?.menuInflater?.inflate(R.menu.node_menu, menu)

                    // Open and Edit for a single item
                    if (nodes.size == 1) {
                        // Edition
                        if (database.isReadOnly) {
                            menu?.removeItem(R.id.menu_edit)
                        }
                    } else {
                        menu?.removeItem(R.id.menu_open)
                        menu?.removeItem(R.id.menu_edit)
                    }

                    // Deletion
                    if (database.isReadOnly) {
                        menu?.removeItem(R.id.menu_delete)
                    }

                    menu?.removeItem(R.id.menu_move)
                    menu?.removeItem(R.id.menu_copy)
                }

                // Add the number of items selected in title
                mode?.title = nodes.size.toString()
                return true
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                if (menuListener == null)
                    return false
                return when (item?.itemId) {
                    R.id.menu_open -> menuListener.onOpenMenuClick(database, nodes[0])
                    R.id.menu_edit -> menuListener.onEditMenuClick(database, nodes[0])
                    R.id.menu_delete -> menuListener.onDeleteMenuClick(database, nodes)
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                listActionNodes.clear()
                entriesAdapter?.unselectActionNodes()
                nodeActionPasteMode = GroupFragment.PasteMode.UNDEFINED
                nodeActionSelectionMode = false
                sortView?.isEnabled = true
                tagsSpinner?.isEnabled = true
                onDestroyActionMode(mode)
            }
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