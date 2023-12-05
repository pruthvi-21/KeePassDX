package com.kunzisoft.keepass.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.AboutActivity
import com.kunzisoft.keepass.settings.SettingsActivity

object MenuUtil {

    fun defaultMenuInflater(context: Context, inflater: MenuInflater, menu: Menu) {
        inflater.inflate(R.menu.about, menu)
    }

    /*
     * @param checkLock Check the time lock before launch settings in LockingActivity
     */
    fun onDefaultMenuOptionsItemSelected(activity: Activity,
                                         item: MenuItem,
                                         timeoutEnable: Boolean = false) {
        when (item.itemId) {
            R.id.menu_app_settings -> {
                // To avoid flickering when launch settings in a LockingActivity
                SettingsActivity.launch(activity, timeoutEnable)
            }
            R.id.menu_about -> {
                val intent = Intent(activity, AboutActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }
}
