package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.DialogPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.Stylish
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.ColorUtils

class AppColorPalettePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.dialogPreferenceStyle,
    defStyleRes: Int = defStyleAttr
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        setSummaryProvider {
            if (PreferencesUtil.getAppColorPalette(context) == Stylish.AppColorPalette.DYNAMIC)
                context.getString(R.string.color_palette_dynamic)
            else context.getString(R.string.icon_section_custom)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val summaryView = holder.findViewById(android.R.id.summary) as TextView?
        summaryView?.apply {
            val colorPrimary = ColorUtils.getColorFromAttr(context, R.attr.colorPrimary, -1)
            if (colorPrimary != -1) {
                setTextColor(colorPrimary)
            }
        }
    }

    override fun getDialogLayoutResource(): Int {
        return R.layout.pref_dialog_app_color_palette
    }
}
