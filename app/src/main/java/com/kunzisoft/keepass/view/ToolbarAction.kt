package com.kunzisoft.keepass.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.SupportMenuInflater
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.ColorUtils

class ToolbarAction @JvmOverloads constructor(context: Context,
                                              attrs: AttributeSet? = null,
                                              defStyle: Int = R.attr.toolbarActionStyle)
    : MaterialToolbar(context, attrs, defStyle) {

    private var mActionModeCallback: ActionMode.Callback? = null
    private val actionMode = NodeActionMode(this)
    private var isOpen = false

    init {
        ContextCompat.getDrawable(context, R.drawable.ic_close_white_24dp)?.let { closeDrawable ->
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)
            @ColorInt val colorControl = typedValue.data
            closeDrawable.colorFilter = PorterDuffColorFilter(colorControl, PorterDuff.Mode.SRC_ATOP)
            navigationIcon = closeDrawable
        }
        setTitleTextColor(ColorUtils.getColorFromAttr(context, R.attr.colorOnSurface, Color.WHITE))
    }

    fun startSupportActionMode(actionModeCallback: ActionMode.Callback): ActionMode {
        mActionModeCallback?.onDestroyActionMode(actionMode)
        mActionModeCallback = actionModeCallback
        mActionModeCallback?.onCreateActionMode(actionMode, menu)
        mActionModeCallback?.onPrepareActionMode(actionMode, menu)

        setOnMenuItemClickListener {
            mActionModeCallback?.onActionItemClicked(actionMode, it) ?: false
        }
        setNavigationOnClickListener{
            actionMode.finish()
        }

        open()

        return actionMode
    }

    fun getSupportActionModeCallback(): ActionMode.Callback? {
        return mActionModeCallback
    }

    fun removeSupportActionModeCallback() {
        mActionModeCallback = null
    }

    override fun invalidateMenu() {
        super.invalidateMenu()
        open()
        mActionModeCallback?.onPrepareActionMode(actionMode, menu)
    }

    fun open() {
        if (!isOpen) {
            isOpen = true
            expand()
        }
    }

    fun close() {
        if (isOpen) {
            isOpen = false
            collapse()
        }
        mActionModeCallback?.onDestroyActionMode(actionMode)
    }

    private class NodeActionMode(var toolbarAction: ToolbarAction): ActionMode() {

        override fun finish() {
            menu.clear()
            toolbarAction.close()
            toolbarAction.removeSupportActionModeCallback()
        }

        override fun getMenu(): Menu {
            return toolbarAction.menu
        }

        override fun getCustomView(): View {
            return toolbarAction
        }

        override fun setCustomView(view: View?) {}

        @SuppressLint("RestrictedApi")
        override fun getMenuInflater(): MenuInflater {
            return SupportMenuInflater(toolbarAction.context)
        }

        override fun invalidate() {
            toolbarAction.invalidateMenu()
        }

        override fun getSubtitle(): CharSequence {
            return toolbarAction.subtitle
        }

        override fun setTitle(title: CharSequence?) {
            toolbarAction.title = title
        }

        override fun setTitle(resId: Int) {
            toolbarAction.setTitle(resId)
        }

        override fun getTitle(): CharSequence {
            return toolbarAction.title
        }

        override fun setSubtitle(subtitle: CharSequence?) {
            toolbarAction.subtitle = subtitle
        }

        override fun setSubtitle(resId: Int) {
            toolbarAction.setSubtitle(resId)
        }
    }
}