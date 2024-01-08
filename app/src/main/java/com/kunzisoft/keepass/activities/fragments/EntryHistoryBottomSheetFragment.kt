package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.EntryHistoryAdapter
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.viewmodels.EntryViewModel

class EntryHistoryBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var historyRecyclerView: RecyclerView
    private var historyAdapter: EntryHistoryAdapter? = null

    private val mEntryViewModel: EntryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_entry_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let { context ->
            historyAdapter = EntryHistoryAdapter(context)
        }

        historyRecyclerView = view.findViewById(R.id.entry_history_list)
        historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
            adapter = historyAdapter
        }

        mEntryViewModel.entryHistory.observe(viewLifecycleOwner) {
            assignHistory(it)
        }
    }

    private fun assignHistory(history: List<EntryInfo>?) {
        historyAdapter?.clear()
        history?.let {
            historyAdapter?.entryHistoryList?.addAll(history)
        }
        historyAdapter?.onItemClickListener = { item, position ->
            mEntryViewModel.onHistorySelected(item, position)
        }
        historyAdapter?.entryHistoryList?.isEmpty()?.apply {
            historyRecyclerView.isVisible = !this
        }
        historyAdapter?.notifyDataSetChanged()
    }
}