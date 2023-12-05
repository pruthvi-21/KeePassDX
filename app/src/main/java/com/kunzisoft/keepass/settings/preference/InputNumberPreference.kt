package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.kunzisoft.keepass.R

open class InputNumberPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.dialogPreferenceStyle,
    defStyleRes: Int = defStyleAttr
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    override fun getDialogLayoutResource(): Int {
        return R.layout.pref_dialog_input_numbers
    }

    override fun setSummary(summary: CharSequence?) {
        if (summary == INFINITE_VALUE_STRING) {
            super.setSummary("∞")
        } else {
            super.setSummary(summary)
        }
    }

    companion object {
        const val INFINITE_VALUE_STRING = "-1"
    }
}
