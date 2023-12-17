package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.ColorUtils

class AttachmentPreviewPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.switchPreferenceStyle,
    defStyleRes: Int = defStyleAttr
) : SwitchPreferenceCompat(context, attrs, defStyleAttr, defStyleRes) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val titleView = holder.itemView.findViewById<TextView>(android.R.id.title)
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_beta)?.apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
        titleView.setCompoundDrawables(null, null, drawable, null)
    }
}
