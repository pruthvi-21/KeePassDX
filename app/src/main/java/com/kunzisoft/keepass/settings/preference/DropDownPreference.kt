package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceViewHolder
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.ColorUtils

open class DropDownPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.dialogPreferenceStyle,
    defStyleRes: Int = defStyleAttr
) : DropDownPreference(context, attrs, defStyleAttr, defStyleRes) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val summaryView = holder.findViewById(android.R.id.summary) as TextView?
        summaryView?.apply {
            val colorPrimary = ColorUtils.getColorFromAttr(context, R.attr.colorPrimary, -1)
            if (colorPrimary != -1 && summaryProvider is SimpleSummaryProvider) {
                setTextColor(colorPrimary)
            }
        }
    }

}
