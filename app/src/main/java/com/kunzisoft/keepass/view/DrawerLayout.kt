package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout


class DrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : DrawerLayout(context, attrs, defStyle) {

    private var containerView: View? = null
    private var navigationView: NavigationDatabaseView? = null

    init {
        addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val slideX = drawerView.width * slideOffset
                containerView?.translationX = slideX
            }

            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {
                navigationView?.collapseView()
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        if (isOpen && navigationView != null) {
            containerView?.translationX = navigationView!!.getNavigationViewSize().toFloat()
        }
    }

    fun setNavigationView(navView: NavigationDatabaseView?) {
        this.navigationView = navView
    }

    fun attachContainer(view: View?) {
        this.containerView = view
    }
}