package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.google.android.material.slider.Slider
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.password.PassphraseGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.view.PassKeyView
import com.kunzisoft.keepass.viewmodels.KeyGeneratorViewModel

class PassphraseGeneratorFragment : BaseViewStubFragment() {

    private lateinit var passKeyView: PassKeyView

    private lateinit var sliderWordCount: Slider
    private lateinit var charactersCountText: TextView
    private lateinit var wordSeparator: EditText
    private lateinit var wordCaseSpinner: Spinner

    private var minSliderWordCount: Int = 0
    private var maxSliderWordCount: Int = 0
    private var wordCaseAdapter: ArrayAdapter<String>? = null

    private val mKeyGeneratorViewModel: KeyGeneratorViewModel by activityViewModels()

    override fun onCreateViewAfterViewStubInflated(
        inflatedView: View,
        savedInstanceState: Bundle?
    ) {
        passKeyView = inflatedView.findViewById(R.id.passphrase_view)
        val passphraseCopyView: View? = inflatedView.findViewById(R.id.passphrase_copy_button)
        sliderWordCount = inflatedView.findViewById(R.id.slider_word_count)
        charactersCountText = inflatedView.findViewById(R.id.character_count)
        wordSeparator = inflatedView.findViewById(R.id.word_separator)
        wordCaseSpinner = inflatedView.findViewById(R.id.word_case)

        minSliderWordCount = resources.getInteger(R.integer.passphrase_generator_word_count_min)
        maxSliderWordCount = resources.getInteger(R.integer.passphrase_generator_word_count_max)

        context?.let { context ->
            passphraseCopyView?.visibility = if (PreferencesUtil.allowCopyProtectedFields(context))
                View.VISIBLE else View.GONE
            val clipboardHelper = ClipboardHelper(context)
            passphraseCopyView?.setOnClickListener {
                clipboardHelper.timeoutCopyToClipboard(
                    getString(R.string.passphrase),
                    passKeyView.passwordString,
                    true
                )
            }

            wordCaseAdapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                resources.getStringArray(R.array.word_case_array)
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            wordCaseSpinner.adapter = wordCaseAdapter
        }

        loadSettings()

        sliderWordCount.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                generatePassphrase()
            }
        })
        wordSeparator.doOnTextChanged { _, _, _, _ ->
            generatePassphrase()
        }
        wordCaseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                generatePassphrase()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        generatePassphrase()

        mKeyGeneratorViewModel.passphraseGeneratedValidated.observe(viewLifecycleOwner) {
            mKeyGeneratorViewModel.setKeyGenerated(passKeyView.passwordString)
        }

        mKeyGeneratorViewModel.requirePassphraseGeneration.observe(viewLifecycleOwner) {
            generatePassphrase()
        }

        resetAppTimeoutWhenViewFocusedOrChanged(inflatedView)
    }

    override fun getViewStubLayoutResource(): Int {
        return R.layout.fragment_generate_passphrase
    }

    private fun getWordCount(): Int {
        return try {
            sliderWordCount.value.toInt()
        } catch (numberException: NumberFormatException) {
            minSliderWordCount
        }
    }

    private fun setWordCount(wordCount: Int) {
        setSliderValue(wordCount)
    }

    private fun setSliderValue(value: Int) {
        when {
            value < minSliderWordCount -> {
                sliderWordCount.value = minSliderWordCount.toFloat()
            }

            value > maxSliderWordCount -> {
                sliderWordCount.value = maxSliderWordCount.toFloat()
            }

            else -> {
                sliderWordCount.value = value.toFloat()
            }
        }
    }

    private fun getWordSeparator(): String {
        return wordSeparator.text.toString().ifEmpty { " " }
    }

    private fun getWordCase(): PassphraseGenerator.WordCase {
        var wordCase = PassphraseGenerator.WordCase.LOWER_CASE
        try {
            wordCase =
                PassphraseGenerator.WordCase.getByOrdinal(wordCaseSpinner.selectedItemPosition)
        } catch (caseException: Exception) {
            Log.e(TAG, "Unable to retrieve the word case", caseException)
        }
        return wordCase
    }

    private fun setWordCase(wordCase: PassphraseGenerator.WordCase) {
        wordCaseSpinner.setSelection(wordCase.ordinal)
    }

    private fun getSeparator(): String {
        return wordSeparator.text?.toString() ?: ""
    }

    private fun setSeparator(separator: String) {
        wordSeparator.setText(separator)
    }

    private fun generatePassphrase() {
        var passphrase = ""
        try {
            passphrase = PassphraseGenerator().generatePassphrase(
                getWordCount(),
                getWordSeparator(),
                getWordCase()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unable to generate a passphrase", e)
        }
        passKeyView.passwordString = passphrase
        charactersCountText.text = getString(R.string.character_count, passphrase.length)
    }

    override fun onDestroy() {
        saveSettings()
        super.onDestroy()
    }

    private fun saveSettings() {
        context?.let { context ->
            PreferencesUtil.setDefaultPassphraseWordCount(context, getWordCount())
            PreferencesUtil.setDefaultPassphraseWordCase(context, getWordCase())
            PreferencesUtil.setDefaultPassphraseSeparator(context, getSeparator())
        }
    }

    private fun loadSettings() {
        context?.let { context ->
            setWordCount(PreferencesUtil.getDefaultPassphraseWordCount(context))
            setWordCase(PreferencesUtil.getDefaultPassphraseWordCase(context))
            setSeparator(PreferencesUtil.getDefaultPassphraseSeparator(context))
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        // Nothing here
    }

    companion object {
        private const val TAG = "PassphraseGnrtrFrgmt"
    }
}
