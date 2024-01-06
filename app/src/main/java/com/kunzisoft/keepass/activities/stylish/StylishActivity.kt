package com.kunzisoft.keepass.activities.stylish

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.settings.PreferencesUtil

/**
 * Stylish Hide Activity that apply a dynamic style and sets FLAG_SECURE to prevent screenshots / from
 * appearing in the recent app preview
 */
abstract class StylishActivity : AppCompatActivity() {

    /* (non-Javadoc) Workaround for HTC Linkify issues
     * @see android.app.Activity#startActivity(android.content.Intent)
     */

    private var currentlyAppliedThemeRes = R.style.Theme_Keepass_Dark

    override fun startActivity(intent: Intent) {
        try {
            intent.component?.let {
                if (it.shortClassName == ".HtcLinkifyDispatcherActivity")
                    intent.component = null
            }
            super.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            /* Catch the bad HTC implementation case */
            super.startActivity(Intent.createChooser(intent, null))
        }
    }

    open fun finishActivityIfReloadRequested(): Boolean {
        return false
    }

    open fun reloadActivity() {
        if (!finishActivityIfReloadRequested()) {
            startActivity(intent)
        }
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val colorPalette = PreferencesUtil.getAppColorPalette(this)
        currentlyAppliedThemeRes = when (Stylish.currentThemeMode(this)) {
            Stylish.ThemeMode.LIGHT -> colorPalette.themeLightId
            Stylish.ThemeMode.DARK -> colorPalette.themeDarkId
            Stylish.ThemeMode.BLACK -> colorPalette.themeBlackId
        }
        setTheme(currentlyAppliedThemeRes)

        PreferenceManager.getDefaultSharedPreferences(this).apply {
            registerOnSharedPreferenceChangeListener(onScreenshotModePrefListener)
            registerOnSharedPreferenceChangeListener(onThemeChangePrefListener)
        }

    }

    private val onScreenshotModePrefListener = OnSharedPreferenceChangeListener { _, key ->
        if (key != getString(R.string.enable_screenshot_mode_key)) return@OnSharedPreferenceChangeListener

        setScreenshotMode(PreferencesUtil.isScreenshotModeEnabled(this))
    }

    private val onThemeChangePrefListener = OnSharedPreferenceChangeListener { _, key ->
        if (this !is GroupActivity) return@OnSharedPreferenceChangeListener
        when (key) {
            getString(R.string.setting_icon_pack_choose_key),
            getString(R.string.setting_app_theme_key),
            getString(R.string.show_entry_colors_key),
            getString(R.string.list_item_subtitle_key),
            getString(R.string.list_groups_show_number_entries_key),
            getString(R.string.show_otp_token_key),
            getString(R.string.list_size_key),
            getString(R.string.monospace_font_fields_enable_key),
            getString(R.string.hide_expired_entries_key),

            getString(R.string.database_name_key),
            getString(R.string.database_description_key),
            getString(R.string.database_default_username_key),
            getString(R.string.database_custom_color_key),
            getString(R.string.database_data_compression_key),
            getString(R.string.database_data_remove_unlinked_attachments_key),
            getString(R.string.recycle_bin_enable_key),
            getString(R.string.recycle_bin_group_key),
            getString(R.string.templates_group_enable_key),
            getString(R.string.templates_group_uuid_key),
            getString(R.string.max_history_items_key),
            getString(R.string.max_history_size_key)

            -> {
                Log.d(this.javaClass.name, "Settings change detected, restarting activity")
                //delaying to just complete the preference animation (popup, switch, ...)
                Handler(Looper.getMainLooper()).postDelayed({ recreate() }, 300)
            }
        }
    }

    private fun setScreenshotMode(isEnabled: Boolean) {
        findViewById<View>(R.id.screenshot_mode_banner)?.visibility =
            if (isEnabled) VISIBLE else GONE

        // Several gingerbread devices have problems with FLAG_SECURE
        if (isEnabled) {
            window.clearFlags(FLAG_SECURE)
        } else {
            window.setFlags(FLAG_SECURE, FLAG_SECURE)
        }
    }

    override fun onResume() {
        super.onResume()

        val colorPalette = PreferencesUtil.getAppColorPalette(this)
        val newTheme = when (Stylish.currentThemeMode(this)) {
            Stylish.ThemeMode.LIGHT -> colorPalette.themeLightId
            Stylish.ThemeMode.DARK -> colorPalette.themeDarkId
            Stylish.ThemeMode.BLACK -> colorPalette.themeBlackId
        }
        if (newTheme != currentlyAppliedThemeRes) {
            Log.d(this.javaClass.name, "Theme change detected, restarting activity")
            recreate()
        }

        setScreenshotMode(PreferencesUtil.isScreenshotModeEnabled(this))
    }
}
