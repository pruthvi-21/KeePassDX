package com.kunzisoft.keepass.activities.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.IconPickerAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.icon.IconImageDraw
import com.kunzisoft.keepass.viewmodels.IconPickerViewModel
import com.libx.ui.helpers.AutoSpanGridLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class IconFragment<T : IconImageDraw> : DatabaseFragment(),
    IconPickerAdapter.IconPickerListener<T> {

    protected lateinit var iconsGridView: RecyclerView
    protected lateinit var iconPickerAdapter: IconPickerAdapter<T>
    protected var iconActionSelectionMode = false

    protected val iconPickerViewModel: IconPickerViewModel by activityViewModels()

    abstract fun retrieveMainLayoutId(): Int

    abstract fun defineIconList(database: ContextualDatabase?)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(retrieveMainLayoutId(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the textColor to tint the icon
        val ta = context?.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        val tintColor = ta?.getColor(0, Color.BLACK) ?: Color.BLACK
        ta?.recycle()

        iconsGridView = view.findViewById(R.id.icons_grid_view)
        iconPickerAdapter = IconPickerAdapter(requireContext(), tintColor)
        iconPickerAdapter.iconPickerListener = this
        iconsGridView.adapter = iconPickerAdapter

        iconsGridView.layoutManager =
            AutoSpanGridLayout(context, resources.getDimension(R.dimen.item_icon_picker_width))

        resetAppTimeoutWhenViewFocusedOrChanged(view)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        iconPickerAdapter.iconDrawableFactory = database?.iconDrawableFactory

        CoroutineScope(Dispatchers.IO).launch {
            val populateList = launch {
                iconPickerAdapter.clear()
                defineIconList(database)
            }
            withContext(Dispatchers.Main) {
                populateList.join()
                iconPickerAdapter.notifyDataSetChanged()
            }
        }
    }

    fun onIconDeleteClicked() {
        iconActionSelectionMode = false
    }
}