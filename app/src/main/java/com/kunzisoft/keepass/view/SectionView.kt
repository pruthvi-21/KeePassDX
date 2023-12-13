package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.IdRes
import androidx.core.view.isVisible

class SectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    init {
        orientation = VERTICAL
    }

    fun removeViewById(@IdRes viewId: Int, onFinish: ((View) -> Unit)? = null) {
        findViewById<View?>(viewId)?.let { viewToRemove ->
            viewToRemove.collapse(true) {
                removeView(viewToRemove)
                onFinish?.invoke(viewToRemove)
                // Hide section if needed
                try {
                    if (childCount == 0) {
                        collapse(true)
                    }
                } catch (e: Exception) {
                    isVisible = false
                }
            }
        }
    }

    override fun removeAllViews() {
        super.removeAllViews()
        isVisible = false
    }
}
