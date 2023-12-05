package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine

open class InputKdfNumberPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.dialogPreferenceStyle,
    defStyleRes: Int = defStyleAttr
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    override fun getDialogLayoutResource(): Int {
        return R.layout.pref_dialog_input_numbers
    }

    override fun setSummary(summary: CharSequence?) {
        if (summary == UNKNOWN_VALUE_STRING) {
            isEnabled = false
            super.setSummary("")
        } else {
            isEnabled = true
            super.setSummary(summary)
        }
    }

    companion object {
        const val UNKNOWN_VALUE_STRING = KdfEngine.UNKNOWN_VALUE.toString()
    }
}
