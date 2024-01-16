package com.kunzisoft.keepass.view

import android.content.Context
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.settings.PreferencesUtil

class EntriesSortView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private var sortNodeEnum: SortNodeEnum = SortNodeEnum.DB
    private var isAscending = true
    private var showRecycleBinItems = false

    private val popupView: View
    private val sortView: TextView
    private val sortOrderView: ImageView
    private val sortOptionsGroup: RadioGroup
    private val additionalOptions: LinearLayout
    private val recycleBinCheckbox: CheckBox

    private var listener: SortSelectionListener? = null

    init {
        inflate(context, R.layout.view_sort_view, this)
        sortView = findViewById(R.id.sort_menu)
        sortOrderView = findViewById(R.id.sort_menu_order)

        val layoutInflater = LayoutInflater.from(context)
        popupView = layoutInflater.inflate(R.layout.popup_sort_options, null)
        sortOptionsGroup = popupView.findViewById(R.id.sort_options_group)
        additionalOptions = popupView.findViewById(R.id.additional_options)

        recycleBinCheckbox = layoutInflater.inflate(R.layout.popup_item_checkbox, null) as CheckBox
        recycleBinCheckbox.setText(R.string.sort_show_recycle_bin_items)

        additionalOptions.addView(recycleBinCheckbox)

        //initial state
        setListOrder(PreferencesUtil.getEntriesListSort(context))
        arrangeInAscending(PreferencesUtil.getAscendingSort(context))
        setShowRecycleBinItems(PreferencesUtil.getRecycleBinEntriesSort(context))

        sortView.setOnClickListener {
            val location = IntArray(2)
            sortView.getLocationOnScreen(location)
            val point = Point().apply {
                x = location[0]
                y = location[1] + 10
            }
            showPopup(context, point)
        }
        sortOrderView.setOnClickListener {
            arrangeInAscending(!isAscending)
            informListeners()
        }
    }

    private fun informListeners() {
        listener?.onSortSelected(
            sortNodeEnum,
            SortNodeEnum.SortNodeParameters(isAscending, false, true),
            showRecycleBinItems
        )
    }

    private fun showPopup(context: Context, p: Point) {
        val popupWindow = PopupWindow(context).apply {
            contentView = popupView
            width = LinearLayout.LayoutParams.WRAP_CONTENT
            height = LinearLayout.LayoutParams.WRAP_CONTENT
            isFocusable = true
            animationStyle = R.style.Theme_LibX_Popup_Animation
            setBackgroundDrawable(ColorDrawable())
        }

        sortOptionsGroup.setOnCheckedChangeListener { group, checkedId ->
            sortNodeEnum = retrieveSortEnumFromViewId(checkedId)
            sortView.text = group.findViewById<RadioButton>(checkedId).text
            informListeners()
            popupWindow.dismiss()
        }

        recycleBinCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
            showRecycleBinItems = isChecked
            informListeners()
            popupWindow.dismiss()
        }

        popupWindow.showAtLocation(popupView, Gravity.NO_GRAVITY, p.x, p.y)
    }

    fun setListOrder(sortOrder: SortNodeEnum) {
        sortNodeEnum = sortOrder
        val itemId = retrieveViewFromEnum(sortOrder)
        val radioButton = sortOptionsGroup.findViewById<RadioButton>(itemId)
        sortView.text = radioButton?.text
        sortOptionsGroup.check(itemId)
    }

    fun arrangeInAscending(isAscending: Boolean) {
        this.isAscending = isAscending
        sortOrderView.setImageResource(
            if (isAscending) R.drawable.ic_sort_order_ascending
            else R.drawable.ic_sort_order_descending
        )
    }

    fun setShowRecycleBinItems(showItems: Boolean) {
        this.showRecycleBinItems = showItems
        recycleBinCheckbox.isChecked = showItems
    }

    @IdRes
    private fun retrieveViewFromEnum(sortNodeEnum: SortNodeEnum): Int {
        return when (sortNodeEnum) {
            SortNodeEnum.DB -> R.id.sort_option_db
            SortNodeEnum.TITLE -> R.id.sort_option_title
            SortNodeEnum.USERNAME -> R.id.sort_option_username
            SortNodeEnum.CREATION_TIME -> R.id.sort_option_creation_time
            SortNodeEnum.LAST_MODIFY_TIME -> R.id.sort_option_last_modify_time
            SortNodeEnum.LAST_ACCESS_TIME -> R.id.sort_option_last_access_time
        }
    }

    private fun retrieveSortEnumFromViewId(@IdRes checkedId: Int): SortNodeEnum {
        return when (checkedId) {
            R.id.sort_option_db -> SortNodeEnum.DB
            R.id.sort_option_title -> SortNodeEnum.TITLE
            R.id.sort_option_username -> SortNodeEnum.USERNAME
            R.id.sort_option_creation_time -> SortNodeEnum.CREATION_TIME
            R.id.sort_option_last_modify_time -> SortNodeEnum.LAST_MODIFY_TIME
            R.id.sort_option_last_access_time -> SortNodeEnum.LAST_ACCESS_TIME
            else -> SortNodeEnum.TITLE
        }
    }

    fun setOnSortSelectionListener(listener: SortSelectionListener?) {
        this.listener = listener
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        sortView.isEnabled = enabled
        sortOrderView.isEnabled = enabled
    }

    interface SortSelectionListener {
        fun onSortSelected(
            sortNodeEnum: SortNodeEnum,
            sortNodeParameters: SortNodeEnum.SortNodeParameters,
            showRecycleBinItems: Boolean
        )
    }
}