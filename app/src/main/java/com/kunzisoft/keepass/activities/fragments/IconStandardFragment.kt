package com.kunzisoft.keepass.activities.fragments

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.icon.IconImageStandard


class IconStandardFragment : IconFragment<IconImageStandard>() {

    override fun retrieveMainLayoutId(): Int {
        return R.layout.fragment_icon_grid
    }

    override fun defineIconList(database: ContextualDatabase?) {
        database?.doForEachStandardIcons { standardIcon ->
            iconPickerAdapter.addIcon(standardIcon, false)
        }
    }

    override fun onIconClickListener(icon: IconImageStandard) {
        iconPickerViewModel.pickStandardIcon(icon)
    }

    override fun onIconLongClickListener(icon: IconImageStandard) {}
}