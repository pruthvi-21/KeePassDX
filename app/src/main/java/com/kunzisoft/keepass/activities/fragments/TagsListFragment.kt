package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.TagsListAdapter
import com.kunzisoft.keepass.database.ContextualDatabase

class TagsListFragment : DatabaseFragment() {

    private var recyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tags_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.tags_list)
        recyclerView!!.layoutManager = LinearLayoutManager(context)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        database?.let {
            val tagsList = ArrayList<TagsListAdapter.TagsListItemModel>()
            database.tagPool.toList().forEach { tag ->
                val entries = database.getEntriesByTag(tag)
                val groups = database.getGroupsByTag(tag)

                tagsList.add(
                    TagsListAdapter.TagsListItemModel(
                        tag,
                        entries.count() + groups.count()
                    )
                )
            }

            val adapter = TagsListAdapter(requireContext(), tagsList)
            recyclerView?.adapter = adapter
        }
    }
}