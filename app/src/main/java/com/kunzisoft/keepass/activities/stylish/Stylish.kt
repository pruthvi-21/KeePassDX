package com.kunzisoft.keepass.activities.stylish

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil

object Stylish {
    enum class AppColorPalette(
        val value: String,
        @StringRes
        val label: Int,
        val themeLightId: Int,
        val themeDarkId: Int,
        val themeBlackId: Int
    ) {
        GREEN(
            "green",
            R.string.theme_green_label,
            R.style.Theme_Keepass_Green_Light,
            R.style.Theme_Keepass_Green_Dark,
            R.style.Theme_Keepass_Green_Black
        ),
        RED(
            "red",
            R.string.theme_red_label,
            R.style.Theme_Keepass_Red_Light,
            R.style.Theme_Keepass_Red_Dark,
            R.style.Theme_Keepass_Red_Black
        ),
        YELLOW(
            "yellow",
            R.string.theme_yellow_label,
            R.style.Theme_Keepass_Yellow_Light,
            R.style.Theme_Keepass_Yellow_Dark,
            R.style.Theme_Keepass_Yellow_Black
        ),
        BLUE(
            "blue",
            R.string.theme_blue_label,
            R.style.Theme_Keepass_Blue_Light,
            R.style.Theme_Keepass_Blue_Dark,
            R.style.Theme_Keepass_Blue_Black
        ),
        PURPLE(
            "purple",
            R.string.theme_purple_label,
            R.style.Theme_Keepass_Purple_Light,
            R.style.Theme_Keepass_Purple_Dark,
            R.style.Theme_Keepass_Purple_Black
        ),
        LAVENDER(
            "lavender",
            R.string.theme_lavender_label,
            R.style.Theme_Keepass_Lavender_Light,
            R.style.Theme_Keepass_Lavender_Dark,
            R.style.Theme_Keepass_Lavender_Black
        ),
        DYNAMIC(
            "dynamic",
            R.string.theme_dynamic_label,
            R.style.Theme_Keepass_Light,
            R.style.Theme_Keepass_Dark,
            R.style.Theme_Keepass_Black
        );

        companion object {
            fun fromValue(value: String?): AppColorPalette? {
                return values().find { it.value == value }
            }

            fun defaultPalette(): AppColorPalette {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S)
                    return GREEN
                return DYNAMIC
            }
        }
    }

    enum class ThemeMode {
        LIGHT, DARK, BLACK;
    }

    fun isDarkTheme(context: Context): Boolean {
        return currentThemeMode(context) == ThemeMode.DARK ||
                currentThemeMode(context) == ThemeMode.BLACK
    }

    fun currentThemeMode(context: Context): ThemeMode {
        return when (PreferencesUtil.getAppTheme(context)) {
            context.getString(R.string.app_theme_light_value) -> ThemeMode.LIGHT
            context.getString(R.string.app_theme_dark_value) -> {
                if (PreferencesUtil.getUseBlacksEnabled(context)) ThemeMode.BLACK
                else ThemeMode.DARK
            }

            else -> {
                when (context.resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
                    Configuration.UI_MODE_NIGHT_YES -> {
                        if (PreferencesUtil.getUseBlacksEnabled(context)) ThemeMode.BLACK
                        else ThemeMode.DARK
                    }

                    else -> ThemeMode.LIGHT
                }
            }
        }
    }
}