package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.google.android.material.slider.Slider
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.view.PassKeyView
import com.kunzisoft.keepass.viewmodels.KeyGeneratorViewModel

class PasswordGeneratorFragment : DatabaseFragment(), OnCheckedChangeListener {

    private lateinit var passKeyView: PassKeyView

    private lateinit var sliderLength: Slider

    private lateinit var uppercaseCompound: CompoundButton
    private lateinit var lowercaseCompound: CompoundButton
    private lateinit var digitsCompound: CompoundButton
    private lateinit var specialsCompound: CompoundButton
    private lateinit var considerCharsEditText: EditText
    private lateinit var ignoreCharsEditText: EditText
    private lateinit var atLeastOneCompound: CompoundButton
    private lateinit var excludeAmbiguousCompound: CompoundButton

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

        uppercaseCompound = view.findViewById(R.id.upperCase_filter)
        lowercaseCompound = view.findViewById(R.id.lowerCase_filter)
        digitsCompound = view.findViewById(R.id.digits_filter)
        specialsCompound = view.findViewById(R.id.special_filter)
        considerCharsEditText = view.findViewById(R.id.consider_chars_filter)
        ignoreCharsEditText = view.findViewById(R.id.ignore_chars_filter)
        atLeastOneCompound = view.findViewById(R.id.atLeastOne_filter)
        excludeAmbiguousCompound = view.findViewById(R.id.excludeAmbiguous_filter)

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

        loadSettings()

        uppercaseCompound.setOnCheckedChangeListener(this)
        lowercaseCompound.setOnCheckedChangeListener(this)
        digitsCompound.setOnCheckedChangeListener(this)
        specialsCompound.setOnCheckedChangeListener(this)
        atLeastOneCompound.setOnCheckedChangeListener(this)
        excludeAmbiguousCompound.setOnCheckedChangeListener(this)

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
        if (uppercaseCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_uppercase))
        if (lowercaseCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_lowercase))
        if (digitsCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_digits))
        if (specialsCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_special))
        if (atLeastOneCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_atLeastOne))
        if (excludeAmbiguousCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_excludeAmbiguous))
        return optionsSet
    }

    private fun setOptions(options: Set<String>) {
        uppercaseCompound.isChecked = false
        lowercaseCompound.isChecked = false
        digitsCompound.isChecked = false
        specialsCompound.isChecked = false
        atLeastOneCompound.isChecked = false
        excludeAmbiguousCompound.isChecked = false
        for (option in options) {
            when (option) {
                getString(R.string.value_password_uppercase) -> uppercaseCompound.isChecked = true
                getString(R.string.value_password_lowercase) -> lowercaseCompound.isChecked = true
                getString(R.string.value_password_digits) -> digitsCompound.isChecked = true
                getString(R.string.value_password_special) -> specialsCompound.isChecked = true
                getString(R.string.value_password_atLeastOne) -> atLeastOneCompound.isChecked = true
                getString(R.string.value_password_excludeAmbiguous) -> excludeAmbiguousCompound.isChecked =
                    true
            }
        }
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
                uppercaseCompound.isChecked,
                lowercaseCompound.isChecked,
                digitsCompound.isChecked,
                specialsCompound.isChecked,
                getConsiderChars(),
                getIgnoreChars(),
                atLeastOneCompound.isChecked,
                excludeAmbiguousCompound.isChecked
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

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        generatePassword()
    }

    companion object {
        private const val TAG = "PasswordGeneratorFrgmt"
    }
}
