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

class SortView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private var sortNodeEnum: SortNodeEnum = SortNodeEnum.DB
    private var isAscending = true
    private var areGroupsBefore = true
    private var isRecycleBinAtBottom = true

    private val popupView: View
    private val sortView: TextView
    private val sortOrderView: ImageView
    private val sortOptionsGroup: RadioGroup
    private val additionalOptionsView: LinearLayout
    private val groupsBeforeView: CheckBox
    private val recycleBinAtBottomView: CheckBox

    private var listener: SortSelectionListener? = null

    init {
        inflate(context, R.layout.view_sort_view, this)
        sortView = findViewById(R.id.sort_menu)
        sortOrderView = findViewById(R.id.sort_menu_order)

        val layoutInflater = LayoutInflater.from(context)

        popupView = layoutInflater.inflate(R.layout.popup_sort_options, null)
        sortOptionsGroup = popupView.findViewById(R.id.sort_options_group)
        additionalOptionsView = popupView.findViewById(R.id.additional_options)

        groupsBeforeView = layoutInflater.inflate(R.layout.popup_item_checkbox, null) as CheckBox
        recycleBinAtBottomView =
            layoutInflater.inflate(R.layout.popup_item_checkbox, null) as CheckBox

        groupsBeforeView.setText(R.string.sort_groups_before)
        recycleBinAtBottomView.setText(R.string.sort_recycle_bin_bottom)

        additionalOptionsView.addView(groupsBeforeView)
        additionalOptionsView.addView(recycleBinAtBottomView)

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

        if (additionalOptionsView.childCount <= 1) additionalOptionsView.isVisible = false
    }

    private fun informListeners() {
        listener?.onSortSelected(
            sortNodeEnum,
            SortNodeEnum.SortNodeParameters(isAscending, areGroupsBefore, isRecycleBinAtBottom)
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

        groupsBeforeView.setOnCheckedChangeListener { buttonView, isChecked ->
            areGroupsBefore = isChecked
            informListeners()
            popupWindow.dismiss()
        }

        recycleBinAtBottomView.setOnCheckedChangeListener { buttonView, isChecked ->
            isRecycleBinAtBottom = isChecked
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

    fun setGroupsBefore(groupsBefore: Boolean) {
        areGroupsBefore = groupsBefore
        groupsBeforeView.isChecked = groupsBefore
    }

    fun setRecycleBinAtBottom(recycleBinAtBottom: Boolean) {
        isRecycleBinAtBottom = recycleBinAtBottom
        recycleBinAtBottomView.isChecked = recycleBinAtBottom
    }

    fun showMoreOptions(show: Boolean) {
        additionalOptionsView.isVisible = show
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
            sortNodeParameters: SortNodeEnum.SortNodeParameters
        )
    }
}