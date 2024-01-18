package com.kunzisoft.keepass.view

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.View.OnClickListener
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.isVisible
import com.google.android.material.navigation.NavigationView
import com.libx.ui.views.CollapsibleLinearLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.UriUtil
import kotlin.math.min


class NavigationDatabaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : NavigationView(context, attrs, defStyle) {

    private var databaseNavContainerView: View? = null
    private var databaseNavIconView: ImageView? = null
    private var databaseNavModifiedView: ImageView? = null
    private var databaseNavColorView: ImageView? = null
    private var databaseNavNameView: TextView? = null
    private var databaseNavPathView: TextView? = null
    private var databaseNavVersionView: TextView? = null

    var navItemGroups: CheckBox? = null
        private set
    var navItemEntries: CheckBox? = null
        private set
    var navItemLock: View? = null
        private set
    var navItemSettings: View? = null
        private set
    var navItemAbout: View? = null
        private set
    var navItemSaveDb: View? = null
        private set
    var navItemMergeDb: View? = null
        private set
    var navItemReloadDb: View? = null
        private set
    var navItemMergeFrom: View? = null
        private set
    var navItemSaveCopyTo: View? = null
        private set
    var navItemMore: CheckBox? = null
        private set

    private var hiddenLayout: CollapsibleLinearLayout? = null

    var onNavItemClickedListener: OnNavItemClickedListener? = null

    init {
        inflateHeaderView(R.layout.nav_header_database)
        databaseNavIconView = databaseNavContainerView?.findViewById(R.id.nav_database_icon)
        databaseNavModifiedView = databaseNavContainerView?.findViewById(R.id.nav_database_modified)
        databaseNavColorView = databaseNavContainerView?.findViewById(R.id.nav_database_color)
        databaseNavNameView = databaseNavContainerView?.findViewById(R.id.nav_database_name)
        databaseNavPathView = databaseNavContainerView?.findViewById(R.id.nav_database_path)
        databaseNavVersionView = databaseNavContainerView?.findViewById(R.id.nav_database_version)

        navItemGroups = databaseNavContainerView?.findViewById(R.id.nav_item_groups)
        navItemEntries = databaseNavContainerView?.findViewById(R.id.nav_item_entries)
        navItemLock = databaseNavContainerView?.findViewById(R.id.nav_item_lock)
        navItemSettings = databaseNavContainerView?.findViewById(R.id.nav_item_settings)
        navItemAbout = databaseNavContainerView?.findViewById(R.id.nav_item_about)
        navItemSaveDb = databaseNavContainerView?.findViewById(R.id.nav_item_save_database)
        navItemMergeDb = databaseNavContainerView?.findViewById(R.id.nav_item_merge_database)
        navItemReloadDb = databaseNavContainerView?.findViewById(R.id.nav_item_reload_database)
        navItemMergeFrom = databaseNavContainerView?.findViewById(R.id.nav_item_merge_from)
        navItemSaveCopyTo = databaseNavContainerView?.findViewById(R.id.nav_item_save_copy_to)
        navItemMore = databaseNavContainerView?.findViewById(R.id.nav_item_more)
        hiddenLayout = databaseNavContainerView?.findViewById(R.id.more_layout)

        navItemMore?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                hiddenLayout?.expand(150)
            } else {
                hiddenLayout?.collapse(150)
            }
        }

        val listener = OnClickListener {
            onNavItemClickedListener?.onNavItemClicked(it.id)
        }
        navItemGroups?.setOnClickListener(listener)
        navItemEntries?.setOnClickListener(listener)
        navItemLock?.setOnClickListener(listener)
        navItemSettings?.setOnClickListener(listener)
        navItemAbout?.setOnClickListener(listener)
        navItemSaveDb?.setOnClickListener(listener)
        navItemMergeDb?.setOnClickListener(listener)
        navItemReloadDb?.setOnClickListener(listener)
        navItemMergeFrom?.setOnClickListener(listener)
        navItemSaveCopyTo?.setOnClickListener(listener)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)

        val newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
            getNavigationViewSize(),
            widthMode
        )
        super.onMeasure(newWidthMeasureSpec, heightMeasureSpec)
    }

    @SuppressLint("PrivateResource")
    fun getNavigationViewSize(): Int {
        val maxWidth = resources.getDimension(R.dimen.m3_comp_navigation_drawer_container_width)

        val typedValue = TypedValue()
        var padding = 0
        if (context.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            padding = TypedValue.complexToDimensionPixelSize(
                typedValue.data,
                context.resources.displayMetrics
            )
        }

        return min(
            resources.displayMetrics.widthPixels - (padding * 1.5).toInt(),
            maxWidth.toInt()
        )
    }

    override fun inflateHeaderView(res: Int): View {
        val headerView = super.inflateHeaderView(res)
        databaseNavContainerView = headerView
        return headerView
    }

    fun setDatabaseName(name: String) {
        databaseNavNameView?.text = name
    }

    fun setDatabasePath(path: String?) {
        databaseNavPathView?.isVisible = path != null
        if (path != null) {
            databaseNavPathView?.text = UriUtil.getPathFromUri(context, Uri.parse(path))
        }
    }

    fun setDatabaseVersion(version: String) {
        databaseNavVersionView?.text = version
    }

    fun setDatabaseModifiedSinceLastLoading(modified: Boolean) {
        databaseNavModifiedView?.isVisible = modified
    }

    fun setDatabaseColor(color: Int?) {
        databaseNavColorView?.isVisible = color != null
        if (color != null) {
            databaseNavColorView?.drawable?.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
        }
    }

    fun collapseView() {
        navItemMore?.isChecked = false
    }

    fun interface OnNavItemClickedListener {
        fun onNavItemClicked(@IdRes itemId: Int)
    }
}