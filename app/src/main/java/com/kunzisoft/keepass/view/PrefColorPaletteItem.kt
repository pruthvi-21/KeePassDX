package com.kunzisoft.keepass.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.Stylish

class PrefColorPaletteItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MaterialCardView(context, attrs) {

    var colorPalette = Stylish.AppColorPalette.DYNAMIC
    var onItemSelectedListener: OnItemSelectedListener? = null

    private val tickView by lazy {
        ImageView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
            setImageResource(R.drawable.ic_check_dual_color_24dp)
            isVisible = false
        }
    }

    private val lightView by lazy {
        View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                gravity = Gravity.END
                weight = 1f
                alpha = 0f
            }
        }
    }

    val darkView by lazy {
        View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                gravity = Gravity.END
                weight = 1f
            }
        }
    }

    init {
        val linearLayout = LinearLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            addView(lightView)
            addView(darkView)
        }
        addView(linearLayout)
        addView(tickView)

        setOnClickListener {
            onItemSelectedListener?.onItemSelected(this)
        }
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        tickView.isVisible = selected
    }

    fun init(
        colorPalette: Stylish.AppColorPalette,
        onItemSelectedListener: OnItemSelectedListener
    ) {
        this.colorPalette = colorPalette
        this.onItemSelectedListener = onItemSelectedListener
    }

    fun interface OnItemSelectedListener {
        fun onItemSelected(item: PrefColorPaletteItem)
    }
}