package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.util.AttributeSet
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.DataByte

open class InputSizePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.dialogPreferenceStyle,
    defStyleRes: Int = defStyleAttr
) : InputNumberPreference(context, attrs, defStyleAttr, defStyleRes) {

    override fun setSummary(summary: CharSequence?) {
        var summaryString = summary
        try {
            val memorySize = summary.toString().toLong()
            summaryString = if (memorySize >= 0) {
                // To convert bytes to mebibytes
                DataByte(memorySize, DataByte.ByteFormat.BYTE)
                    .toBetterByteFormat().toString(context)
            } else {
                memorySize.toString()
            }
        } catch (e: Exception) {
        } finally {
            super.setSummary(summaryString)
        }
    }
}
