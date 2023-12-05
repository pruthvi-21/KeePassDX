package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.preferencedialogfragment.adapter.AutofillBlocklistAdapter
import com.kunzisoft.keepass.utils.getParcelableArrayCompat
import java.util.TreeSet

abstract class AutofillBlocklistPreferenceDialogFragmentCompat
    : InputPreferenceDialogFragmentCompat(),
    AutofillBlocklistAdapter.ItemDeletedCallback<SearchInfo> {

    private var persistedItems = TreeSet<SearchInfo> { o1, o2 ->
        o1.toString().compareTo(o2.toString())
    }

    private var filterAdapter: AutofillBlocklistAdapter<SearchInfo>? = null

    abstract fun buildSearchInfoFromString(searchInfoString: String): SearchInfo?

    abstract fun getDefaultValues(): Set<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // To get items for saved instance state
        savedInstanceState?.getParcelableArrayCompat<SearchInfo>(ITEMS_KEY)?.let {
            it.forEach { itemSaved ->
                persistedItems.add(itemSaved)
            }
        } ?: run {
            // Or from preference
            preference.getPersistedStringSet(getDefaultValues()).forEach { searchInfoString ->
                addSearchInfo(searchInfoString)
            }
        }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        setOnInputTextEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (inputText.isEmpty()) {
                        onDialogClosed(true)
                        dialog?.dismiss()
                        true
                    } else {
                        addItemFromInputText()
                        false
                    }
                }

                else -> false
            }
        }

        val addItemButton = view.findViewById<View>(R.id.add_item_button)
        addItemButton?.setOnClickListener {
            addItemFromInputText()
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.pref_dialog_list)
        recyclerView.layoutManager = LinearLayoutManager(context)

        activity?.let { activity ->
            filterAdapter = AutofillBlocklistAdapter(activity)
            filterAdapter?.setItemDeletedCallback(this)
            recyclerView.adapter = filterAdapter
            filterAdapter?.replaceItems(persistedItems.toList())
        }
    }

    private fun addSearchInfo(searchInfoString: String): Boolean {
        val itemToAdd = buildSearchInfoFromString(searchInfoString)
        return if (itemToAdd != null && !itemToAdd.containsOnlyNullValues()) {
            persistedItems.add(itemToAdd)
            true
        } else {
            false
        }
    }

    private fun addItemFromInputText() {
        if (addSearchInfo(inputText)) {
            inputText = ""
        } else {
            setInputTextError(getString(R.string.error_string_type))
        }
        filterAdapter?.replaceItems(persistedItems.toList())
    }

    override fun onItemDeleted(item: SearchInfo) {
        persistedItems.remove(item)
        filterAdapter?.replaceItems(persistedItems.toList())
    }

    private fun getStringItems(): Set<String> {
        val setItems = HashSet<String>()
        persistedItems.forEach {
            it.getName(resources).let { item ->
                setItems.add(item)
            }
        }
        return setItems
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArray(ITEMS_KEY, persistedItems.toTypedArray())
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            preference.persistStringSet(getStringItems())
        }
    }

    companion object {
        private const val ITEMS_KEY = "ITEMS_KEY"
    }
}
