package com.kunzisoft.keepass.activities

import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.getPackageInfoCompat


class AboutActivity : StylishActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = " "
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_toolbar_back_white_24dp)

        val appName = getString(R.string.app_name)
        findViewById<TextView>(R.id.activity_about_app_name).text = appName

        var version: String
        try {
            version = packageManager.getPackageInfoCompat(packageName).versionName
        } catch (e: NameNotFoundException) {
            Log.w(javaClass.simpleName, "Unable to get the app or the build version", e)
            version = "Unable to get the app version"
        }

        version = getString(R.string.version_label, version)
        val versionTextView = findViewById<TextView>(R.id.activity_about_version)
        versionTextView.text = version

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.about_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            android.R.id.home -> finish()

            R.id.menu_app_info -> {
                val uri = Uri.fromParts("package", packageName, null)
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = uri
                }

                // Check if the intent can be resolved to avoid crashes on some devices
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
