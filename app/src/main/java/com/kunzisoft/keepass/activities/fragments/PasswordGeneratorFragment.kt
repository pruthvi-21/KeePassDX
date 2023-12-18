package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.view.PassKeyView
import com.kunzisoft.keepass.viewmodels.KeyGeneratorViewModel

class PasswordGeneratorFragment : DatabaseFragment() {

    private lateinit var passKeyView: PassKeyView

    private lateinit var sliderLength: Slider

    private lateinit var uppercaseSwitch: MaterialSwitch
    private lateinit var lowercaseSwitch: MaterialSwitch
    private lateinit var digitsSwitch: MaterialSwitch
    private lateinit var specialsSwitch: MaterialSwitch
    private lateinit var considerCharsEditText: EditText
    private lateinit var ignoreCharsEditText: EditText
    private lateinit var atLeastOneSwitch: MaterialSwitch
    private lateinit var excludeAmbiguousSwitch: MaterialSwitch

    private var minLengthSlider: Int = 0
    private var maxLengthSlider: Int = 0

    private val mKeyGeneratorViewModel: KeyGeneratorViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_generate_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        passKeyView = view.findViewById(R.id.password_view)
        val passwordCopyView: View? = view.findViewById(R.id.password_copy_button)

        sliderLength = view.findViewById(R.id.slider_length)

        uppercaseSwitch = view.findViewById(R.id.upperCase_filter)
        lowercaseSwitch = view.findViewById(R.id.lowerCase_filter)
        digitsSwitch = view.findViewById(R.id.digits_filter)
        specialsSwitch = view.findViewById(R.id.special_filter)
        considerCharsEditText = view.findViewById(R.id.consider_chars_filter)
        ignoreCharsEditText = view.findViewById(R.id.ignore_chars_filter)
        atLeastOneSwitch = view.findViewById(R.id.atLeastOne_filter)
        excludeAmbiguousSwitch = view.findViewById(R.id.excludeAmbiguous_filter)

        context?.let { context ->
            passwordCopyView?.visibility = if (PreferencesUtil.allowCopyProtectedFields(context))
                View.VISIBLE else View.GONE
            val clipboardHelper = ClipboardHelper(context)
            passwordCopyView?.setOnClickListener {
                clipboardHelper.timeoutCopyToClipboard(
                    getString(R.string.password),
                    passKeyView.passwordString,
                    true
                )
            }
        }

        minLengthSlider = resources.getInteger(R.integer.password_generator_length_min)
        maxLengthSlider = resources.getInteger(R.integer.password_generator_length_max)

        uppercaseSwitch.setOnCheckedChangeListener { _, _ ->
            updateSwitchState(uppercaseSwitch, lowercaseSwitch, digitsSwitch, specialsSwitch)
        }
        lowercaseSwitch.setOnCheckedChangeListener { _, _ ->
            updateSwitchState(lowercaseSwitch, uppercaseSwitch, digitsSwitch, specialsSwitch)
        }
        digitsSwitch.setOnCheckedChangeListener { _, _ ->
            updateSwitchState(digitsSwitch, uppercaseSwitch, lowercaseSwitch, specialsSwitch)
        }
        specialsSwitch.setOnCheckedChangeListener { _, _ ->
            updateSwitchState(specialsSwitch, uppercaseSwitch, lowercaseSwitch, digitsSwitch)
        }

        atLeastOneSwitch.setOnCheckedChangeListener { _, _ -> generatePassword() }
        excludeAmbiguousSwitch.setOnCheckedChangeListener { _, _ -> generatePassword() }

        considerCharsEditText.doOnTextChanged { _, _, _, _ ->
            generatePassword()
        }
        ignoreCharsEditText.doOnTextChanged { _, _, _, _ ->
            generatePassword()
        }

        sliderLength.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                generatePassword()
            }
        })

        loadSettings()

        // Pre-populate a password to possibly save the user a few clicks
        generatePassword()

        mKeyGeneratorViewModel.passwordGeneratedValidated.observe(viewLifecycleOwner) {
            mKeyGeneratorViewModel.setKeyGenerated(passKeyView.passwordString)
        }

        mKeyGeneratorViewModel.requirePasswordGeneration.observe(viewLifecycleOwner) {
            generatePassword()
        }

        resetAppTimeoutWhenViewFocusedOrChanged(view)
    }

    private fun updateSwitchState(
        switch: MaterialSwitch,
        vararg switches: MaterialSwitch
    ) {
        if (switch.isChecked) {
            switches.forEach {
                it.isClickable = true
                it.thumbIconDrawable = null
            }

            //just to set initial state
            if (switches.all { !it.isChecked }) {
                switch.setThumbIconResource(R.drawable.ic_lock_white_padding_24dp)
                switch.isClickable = false
            }
        } else {
            if (switches.all { it.isChecked }) {
                return
            }

            val activeSwitches = switches.filter { it.isChecked }
            if (activeSwitches.size == 1) {
                activeSwitches[0].apply {
                    setThumbIconResource(R.drawable.ic_lock_white_padding_24dp)
                    isClickable = false
                }
            }
        }
        generatePassword()
    }

    private fun getPasswordLength(): Int {
        return try {
            sliderLength.value.toInt()
        } catch (numberException: NumberFormatException) {
            minLengthSlider
        }
    }

    private fun setPasswordLength(passwordLength: Int) {
        setSliderValue(passwordLength)
    }

    private fun getOptions(): Set<String> {
        val optionsSet = mutableSetOf<String>()
        if (uppercaseSwitch.isChecked)
            optionsSet.add(getString(R.string.value_password_uppercase))
        if (lowercaseSwitch.isChecked)
            optionsSet.add(getString(R.string.value_password_lowercase))
        if (digitsSwitch.isChecked)
            optionsSet.add(getString(R.string.value_password_digits))
        if (specialsSwitch.isChecked)
            optionsSet.add(getString(R.string.value_password_special))
        if (atLeastOneSwitch.isChecked)
            optionsSet.add(getString(R.string.value_password_atLeastOne))
        if (excludeAmbiguousSwitch.isChecked)
            optionsSet.add(getString(R.string.value_password_excludeAmbiguous))
        return optionsSet
    }

    private fun setOptions(options: Set<String>) {
        uppercaseSwitch.isChecked = false
        lowercaseSwitch.isChecked = false
        digitsSwitch.isChecked = false
        specialsSwitch.isChecked = false
        atLeastOneSwitch.isChecked = false
        excludeAmbiguousSwitch.isChecked = false
        for (option in options) {
            when (option) {
                getString(R.string.value_password_uppercase) -> uppercaseSwitch.isChecked = true
                getString(R.string.value_password_lowercase) -> lowercaseSwitch.isChecked = true
                getString(R.string.value_password_digits) -> digitsSwitch.isChecked = true
                getString(R.string.value_password_special) -> specialsSwitch.isChecked = true
                getString(R.string.value_password_atLeastOne) -> atLeastOneSwitch.isChecked = true
                getString(R.string.value_password_excludeAmbiguous) -> excludeAmbiguousSwitch.isChecked =
                    true
            }
        }
        updateSwitchState(uppercaseSwitch, lowercaseSwitch, digitsSwitch, specialsSwitch)
    }

    private fun getConsiderChars(): String {
        return considerCharsEditText.text.toString()
    }

    private fun setConsiderChars(chars: String) {
        considerCharsEditText.setText(chars)
    }

    private fun getIgnoreChars(): String {
        return ignoreCharsEditText.text.toString()
    }

    private fun setIgnoreChars(chars: String) {
        ignoreCharsEditText.setText(chars)
    }

    private fun generatePassword() {
        var password = ""
        try {
            password = PasswordGenerator(resources).generatePassword(
                getPasswordLength(),
                uppercaseSwitch.isChecked,
                lowercaseSwitch.isChecked,
                digitsSwitch.isChecked,
                specialsSwitch.isChecked,
                getConsiderChars(),
                getIgnoreChars(),
                atLeastOneSwitch.isChecked,
                excludeAmbiguousSwitch.isChecked
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unable to generate a password", e)
        }
        passKeyView.passwordString = password
    }

    override fun onDestroy() {
        saveSettings()
        super.onDestroy()
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        // Nothing here
    }

    private fun saveSettings() {
        context?.let { context ->
            PreferencesUtil.setDefaultPasswordOptions(context, getOptions())
            PreferencesUtil.setDefaultPasswordLength(context, getPasswordLength())
            PreferencesUtil.setDefaultPasswordConsiderChars(context, getConsiderChars())
            PreferencesUtil.setDefaultPasswordIgnoreChars(context, getIgnoreChars())
        }
    }

    private fun loadSettings() {
        context?.let { context ->
            setOptions(PreferencesUtil.getDefaultPasswordOptions(context))
            setPasswordLength(PreferencesUtil.getDefaultPasswordLength(context))
            setConsiderChars(PreferencesUtil.getDefaultPasswordConsiderChars(context))
            setIgnoreChars(PreferencesUtil.getDefaultPasswordIgnoreChars(context))
        }
    }

    private fun setSliderValue(value: Int) {
        when {
            value < minLengthSlider -> {
                sliderLength.value = minLengthSlider.toFloat()
            }

            value > maxLengthSlider -> {
                sliderLength.value = maxLengthSlider.toFloat()
            }

            else -> {
                sliderLength.value = value.toFloat()
            }
        }
    }

    companion object {
        private const val TAG = "PasswordGeneratorFrgmt"
    }
}
