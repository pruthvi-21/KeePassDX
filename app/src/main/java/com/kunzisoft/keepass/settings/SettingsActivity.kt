package com.kunzisoft.keepass.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.app.backup.BackupManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.SetMainCredentialDialogFragment
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.settings.fragments.MainPreferenceFragment
import com.kunzisoft.keepass.settings.fragments.NestedSettingsFragment
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import org.joda.time.DateTime
import java.util.Properties

open class SettingsActivity
    : DatabaseLockActivity(),
    MainPreferenceFragment.Callback,
    SetMainCredentialDialogFragment.AssignMainCredentialDialogListener {

    private var backupManager: BackupManager? = null

    private var coordinatorLayout: CoordinatorLayout? = null
    private var lockView: FloatingActionButton? = null

    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        coordinatorLayout = findViewById(R.id.toolbar_coordinator)
        lockView = findViewById(R.id.lock_button)
        lockView?.isVisible = false

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setTitle(R.string.settings)
        var fragment: Fragment = MainPreferenceFragment()

        val bundle = intent.extras
        if (bundle?.containsKey(SCREEN_ARG) == true) {
            val screen = NestedSettingsFragment.Screen.values()[bundle.getInt(SCREEN_ARG)]

            supportActionBar?.setTitle(screen.titleRes)
            fragment = NestedSettingsFragment.newInstance(screen)
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }

        backupManager = BackupManager(this)
    }

    override fun viewToInvalidateTimeout(): View? {
        return coordinatorLayout
    }

    override fun finishActivityIfDatabaseNotLoaded(): Boolean {
        return false
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        coordinatorLayout?.showActionErrorIfNeeded(result)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onDatabaseBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        backupManager?.dataChanged()
        super.onStop()
    }

    override fun onAssignKeyDialogPositiveClick(mainCredential: MainCredential) {
        assignPassword(mainCredential)
    }

    override fun onAssignKeyDialogNegativeClick(mainCredential: MainCredential) {}

    private fun startNestedActivity(key: NestedSettingsFragment.Screen) {
        startActivity(Intent(this, SettingsActivity::class.java).apply {
            putExtra(SCREEN_ARG, key.ordinal)
        })
    }

    override fun onNestedPreferenceSelected(key: NestedSettingsFragment.Screen) {
        if (mTimeoutEnable)
            checkTimeAndLockIfTimeoutOrResetTimeout {
                startNestedActivity(key)
            }
        else startNestedActivity(key)
    }

    companion object {
        private const val SCREEN_ARG = "screen_arg"

        fun launch(activity: Activity, timeoutEnable: Boolean) {
            val intent = Intent(activity, SettingsActivity::class.java)
            intent.putExtra(TIMEOUT_ENABLE_KEY, timeoutEnable)
            if (!timeoutEnable) {
                activity.startActivity(intent)
            } else if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                activity.startActivity(intent)
            }
        }
    }
}
