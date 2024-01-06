package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceDialogFragmentCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.Stylish
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.PrefColorPaletteItem

class AppColorPalettePreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {

    private lateinit var dynamicSwitch: CompoundButton
    private lateinit var colorItemGreen: PrefColorPaletteItem
    private lateinit var colorItemRed: PrefColorPaletteItem
    private lateinit var colorItemYellow: PrefColorPaletteItem
    private lateinit var colorItemBlue: PrefColorPaletteItem
    private lateinit var colorItemPurple: PrefColorPaletteItem
    private lateinit var colorItemLavender: PrefColorPaletteItem

    private var selectedColorPalette = Stylish.AppColorPalette.defaultPalette()

    private val onItemSelectedListener = PrefColorPaletteItem.OnItemSelectedListener {
        deselectAll()
        it.isSelected = true
        selectedColorPalette = it.colorPalette
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        dialog?.setTitle(R.string.app_color_palette_title)

        dynamicSwitch = view.findViewById(R.id.pref_palette_item_dynamic_switch) as CompoundButton
        colorItemGreen = view.findViewById(R.id.pref_palette_item_green) as PrefColorPaletteItem
        colorItemRed = view.findViewById(R.id.pref_palette_item_red) as PrefColorPaletteItem
        colorItemYellow = view.findViewById(R.id.pref_palette_item_yellow) as PrefColorPaletteItem
        colorItemBlue = view.findViewById(R.id.pref_palette_item_blue) as PrefColorPaletteItem
        colorItemPurple = view.findViewById(R.id.pref_palette_item_purple) as PrefColorPaletteItem
        colorItemLavender =
            view.findViewById(R.id.pref_palette_item_lavender) as PrefColorPaletteItem

        colorItemGreen.init(Stylish.AppColorPalette.GREEN, onItemSelectedListener)
        colorItemRed.init(Stylish.AppColorPalette.RED, onItemSelectedListener)
        colorItemYellow.init(Stylish.AppColorPalette.YELLOW, onItemSelectedListener)
        colorItemBlue.init(Stylish.AppColorPalette.BLUE, onItemSelectedListener)
        colorItemPurple.init(Stylish.AppColorPalette.PURPLE, onItemSelectedListener)
        colorItemLavender.init(Stylish.AppColorPalette.LAVENDER, onItemSelectedListener)

        colorItemGreen.darkView.setBackgroundColor(
            ResourcesCompat.getColor(resources, R.color.green_dark_primary, activity?.theme)
        )
        colorItemRed.darkView.setBackgroundColor(
            ResourcesCompat.getColor(resources, R.color.red_dark_primary, activity?.theme)
        )
        colorItemYellow.darkView.setBackgroundColor(
            ResourcesCompat.getColor(resources, R.color.yellow_dark_primary, activity?.theme)
        )
        colorItemBlue.darkView.setBackgroundColor(
            ResourcesCompat.getColor(resources, R.color.blue_dark_primary, activity?.theme)
        )
        colorItemPurple.darkView.setBackgroundColor(
            ResourcesCompat.getColor(resources, R.color.purple_dark_primary, activity?.theme)
        )
        colorItemLavender.darkView.setBackgroundColor(
            ResourcesCompat.getColor(resources, R.color.lavender_dark_primary, activity?.theme)
        )

        dynamicSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            deselectAll()
            buttonView.isChecked = isChecked
            if (!isChecked) onItemSelectedListener.onItemSelected(colorItemGreen)
            else selectedColorPalette = Stylish.AppColorPalette.DYNAMIC
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            dynamicSwitch.isVisible = false
            view.findViewById<View>(R.id.divider).isVisible = false
        }

        //initialState
        val appColor = PreferencesUtil.getAppColorPalette(requireContext())
        setColorPalette(appColor)
    }

    private fun deselectAll() {
        dynamicSwitch.isChecked = false
        colorItemGreen.isSelected = false
        colorItemRed.isSelected = false
        colorItemYellow.isSelected = false
        colorItemBlue.isSelected = false
        colorItemPurple.isSelected = false
        colorItemLavender.isSelected = false
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            PreferencesUtil.setAppColorPalette(requireContext(), selectedColorPalette)
            activity?.recreate()
        }
    }

    private fun setColorPalette(colorPalette: Stylish.AppColorPalette) {
        when (colorPalette) {
            Stylish.AppColorPalette.GREEN -> onItemSelectedListener.onItemSelected(colorItemGreen)
            Stylish.AppColorPalette.RED -> onItemSelectedListener.onItemSelected(colorItemRed)
            Stylish.AppColorPalette.YELLOW -> onItemSelectedListener.onItemSelected(colorItemYellow)
            Stylish.AppColorPalette.BLUE -> onItemSelectedListener.onItemSelected(colorItemBlue)
            Stylish.AppColorPalette.PURPLE -> onItemSelectedListener.onItemSelected(colorItemPurple)
            Stylish.AppColorPalette.LAVENDER ->
                onItemSelectedListener.onItemSelected(colorItemLavender)

            Stylish.AppColorPalette.DYNAMIC -> {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
                    onItemSelectedListener.onItemSelected(colorItemGreen)
                    //As SDK<31 doesn't support dynamic theming, change it to default palette
                    PreferencesUtil.setAppColorPalette(
                        requireContext(),
                        Stylish.AppColorPalette.defaultPalette()
                    )
                } else {
                    dynamicSwitch.isChecked = true
                }
            }
        }
    }

    companion object {
        fun newInstance(key: String): AppColorPalettePreferenceDialogFragmentCompat {
            val fragment = AppColorPalettePreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
