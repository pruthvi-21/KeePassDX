package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.icons.IconPackChooser

class IconPackListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.dialogPreferenceStyle,
    defStyleRes: Int = defStyleAttr
) : ListPreference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        val entries = ArrayList<String>()
        val values = ArrayList<String>()
        for (iconPack in IconPackChooser.getIconPackList(context)) {
            iconPack.id?.let { iconPackId ->
                entries.add(iconPack.name)
                values.add(iconPackId)
            }
        }

        setEntries(entries.toTypedArray())
        entryValues = values.toTypedArray()
        IconPackChooser.getSelectedIconPack(context)?.let { selectedIconPack ->
            setDefaultValue(selectedIconPack.id)
        }
    }
}
