package com.kunzisoft.keepass.activities.stylish

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil

object Stylish {
    enum class AppColorPalette(
        val value: String,
        val themeLightId: Int,
        val themeDarkId: Int,
        val themeBlackId: Int
    ) {
        GREEN(
            "green",
            R.style.Theme_Keepass_Green_Light,
            R.style.Theme_Keepass_Green_Dark,
            R.style.Theme_Keepass_Green_Black
        ),
        RED(
            "red",
            R.style.Theme_Keepass_Red_Light,
            R.style.Theme_Keepass_Red_Dark,
            R.style.Theme_Keepass_Red_Black
        ),
        YELLOW(
            "yellow",
            R.style.Theme_Keepass_Yellow_Light,
            R.style.Theme_Keepass_Yellow_Dark,
            R.style.Theme_Keepass_Yellow_Black
        ),
        BLUE(
            "blue",
            R.style.Theme_Keepass_Blue_Light,
            R.style.Theme_Keepass_Blue_Dark,
            R.style.Theme_Keepass_Blue_Black
        ),
        DYNAMIC(
            "dynamic",
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