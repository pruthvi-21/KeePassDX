package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Menu
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.widget.PopupMenu
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.settings.PreferencesUtil

class SortView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private var sortNodeEnum: SortNodeEnum = SortNodeEnum.DB
    private var isAscending = true
    private var areGroupsBefore = true
    private var isRecycleBinAtBottom = true

    private val sortView: TextView
    private val sortOrderView: ImageView

    private val sortPopupMenu: PopupMenu
    private var listener: SortSelectionListener? = null

    private val sortNodeMenuIds = arrayOf(
        R.id.menu_sort_db,
        R.id.menu_sort_title,
        R.id.menu_sort_username,
        R.id.menu_sort_creation_time,
        R.id.menu_sort_last_modify_time,
        R.id.menu_sort_last_access_time
    )

    init {
        inflate(context, R.layout.view_sort_view, this)
        sortView = findViewById(R.id.sort_menu)
        sortOrderView = findViewById(R.id.sort_menu_order)

        sortPopupMenu = PopupMenu(context, sortView).apply {
            menuInflater.inflate(R.menu.sort, menu)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                menu.setGroupDividerEnabled(true)
            }

            setOnMenuItemClickListener {
                if (it.itemId in sortNodeMenuIds) {
                    it.isChecked = true
                    uncheckAllSortCheckboxesExcept(menu, it.itemId)
                    sortNodeEnum = retrieveSortEnumFromViewId(it.itemId)
                    sortView.text = it.title
                } else {
                    if (it.itemId == R.id.menu_sort_groups_before) {
                        areGroupsBefore = !areGroupsBefore
                        it.isChecked = areGroupsBefore
                    }
                    if (it.itemId == R.id.menu_sort_recycle_bin_bottom) {
                        isRecycleBinAtBottom = !isRecycleBinAtBottom
                        it.isChecked = isRecycleBinAtBottom
                    }
                }

                disableRecycleBinBottomOptionIfNaturalOrder()
                listener?.onSortSelected(
                    sortNodeEnum,
                    SortNodeEnum.SortNodeParameters(
                        isAscending,
                        areGroupsBefore,
                        isRecycleBinAtBottom
                    )
                )

                true
            }
        }

        //initial state
        setListOrder(PreferencesUtil.getListSort(context))
        arrangeInAscending(PreferencesUtil.getAscendingSort(context))
        setGroupsBefore(PreferencesUtil.getGroupsBeforeSort(context))
        setRecycleBinAtBottom(PreferencesUtil.getRecycleBinBottomSort(context))

        sortView.setOnClickListener {
            sortPopupMenu.show()
        }
        sortOrderView.setOnClickListener {
            arrangeInAscending(!isAscending)
            listener?.onSortSelected(
                sortNodeEnum,
                SortNodeEnum.SortNodeParameters(
                    isAscending,
                    groupsBefore = true,
                    recycleBinBottom = true
                )
            )
        }

        disableRecycleBinBottomOptionIfNaturalOrder()
    }

    private fun setGroupsBefore(groupsBefore: Boolean) {
        areGroupsBefore = groupsBefore
        sortPopupMenu.menu
            .findItem(R.id.menu_sort_groups_before).isChecked = groupsBefore
    }

    private fun setRecycleBinAtBottom(recycleBinAtBottom: Boolean) {
        isRecycleBinAtBottom = recycleBinAtBottom
        sortPopupMenu.menu
            .findItem(R.id.menu_sort_recycle_bin_bottom).isChecked = recycleBinAtBottom
    }

    fun setListOrder(sortOrder: SortNodeEnum) {
        sortNodeEnum = sortOrder
        val menuId = retrieveViewFromEnum(sortOrder)
        sortView.text = sortPopupMenu.menu.findItem(retrieveViewFromEnum(sortOrder)).title
        sortPopupMenu.menu.findItem(menuId).setChecked(true)
        uncheckAllSortCheckboxesExcept(sortPopupMenu.menu, menuId)
    }

    fun arrangeInAscending(isAscending: Boolean) {
        this.isAscending = isAscending
        sortOrderView.setImageResource(
            if (isAscending) R.drawable.ic_sort_order_ascending
            else R.drawable.ic_sort_order_descending
        )
    }

    private fun uncheckAllSortCheckboxesExcept(menu: Menu, id: Int) {
        sortNodeMenuIds.forEach {
            if (it != id) menu.findItem(it).isChecked = false
        }
    }

    @IdRes
    private fun retrieveViewFromEnum(sortNodeEnum: SortNodeEnum): Int {
        return when (sortNodeEnum) {
            SortNodeEnum.DB -> R.id.menu_sort_db
            SortNodeEnum.TITLE -> R.id.menu_sort_title
            SortNodeEnum.USERNAME -> R.id.menu_sort_username
            SortNodeEnum.CREATION_TIME -> R.id.menu_sort_creation_time
            SortNodeEnum.LAST_MODIFY_TIME -> R.id.menu_sort_last_modify_time
            SortNodeEnum.LAST_ACCESS_TIME -> R.id.menu_sort_last_access_time
        }
    }

    private fun retrieveSortEnumFromViewId(@IdRes checkedId: Int): SortNodeEnum {
        return when (checkedId) {
            R.id.menu_sort_db -> SortNodeEnum.DB
            R.id.menu_sort_title -> SortNodeEnum.TITLE
            R.id.menu_sort_username -> SortNodeEnum.USERNAME
            R.id.menu_sort_creation_time -> SortNodeEnum.CREATION_TIME
            R.id.menu_sort_last_modify_time -> SortNodeEnum.LAST_MODIFY_TIME
            R.id.menu_sort_last_access_time -> SortNodeEnum.LAST_ACCESS_TIME
            else -> SortNodeEnum.TITLE
        }
    }

    private fun disableRecycleBinBottomOptionIfNaturalOrder() {
        sortPopupMenu.menu.findItem(R.id.menu_sort_recycle_bin_bottom)?.isEnabled =
            sortNodeEnum != SortNodeEnum.DB
    }

    fun setOnSortSelectionListener(listener: SortSelectionListener?) {
        this.listener = listener
    }

    interface SortSelectionListener {
        fun onSortSelected(
            sortNodeEnum: SortNodeEnum,
            sortNodeParameters: SortNodeEnum.SortNodeParameters
        )
    }
}