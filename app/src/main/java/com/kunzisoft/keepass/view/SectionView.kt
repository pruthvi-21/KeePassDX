package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import com.kunzisoft.keepass.R

class SectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val containerSectionView: LinearLayout

    init {
        inflate(context, R.layout.layout_section_view, this)
        containerSectionView = findViewById(R.id.container_view)
    }

    override fun addView(child: View?) {
        visibility = View.VISIBLE
        containerSectionView.addView(child)
    }

    fun removeViewById(@IdRes viewId: Int, onFinish: ((View) -> Unit)? = null) {
        containerSectionView.findViewById<View?>(viewId)?.let { viewToRemove ->
            viewToRemove.collapse(true) {
                containerSectionView.removeView(viewToRemove)
                onFinish?.invoke(viewToRemove)
                // Hide section if needed
                try {
                    if (containerSectionView.childCount == 0) {
                        collapse(true)
                    }
                } catch (e: Exception) {
                    visibility = View.GONE
                }
            }
        }
    }

    override fun removeAllViews() {
        containerSectionView.removeAllViews()
        visibility = View.GONE
    }
}
