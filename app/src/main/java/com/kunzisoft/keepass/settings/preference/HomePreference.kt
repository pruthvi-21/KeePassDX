package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.kunzisoft.keepass.R

open class HomePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.preferenceStyle,
    defStyleRes: Int = defStyleAttr
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private val iconDrawable: Drawable?
    private val iconBackgroundColor: Int

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.HomePreference)
        iconDrawable = typedArray.getDrawable(R.styleable.HomePreference_icon)
        iconBackgroundColor =
            typedArray.getColor(R.styleable.HomePreference_backgroundColor, Color.TRANSPARENT)
        typedArray.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val iconFrame = holder.findViewById(R.id.icon_frame) as ViewGroup?
        val customIconView = LayoutInflater.from(context)
            .inflate(R.layout.preference_home_icon, null)

        val iconView = customIconView.findViewById<ImageView>(R.id.icon)
        val iconBgView = customIconView.findViewById<ImageView>(R.id.icon_bg)
        iconBgView.imageTintList = ColorStateList.valueOf(iconBackgroundColor)
        iconView.setImageDrawable(iconDrawable)

        iconFrame?.apply {
            removeAllViews()
            addView(customIconView)
        }
    }

}