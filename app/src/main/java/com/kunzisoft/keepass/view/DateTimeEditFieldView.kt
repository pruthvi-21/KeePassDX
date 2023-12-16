package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.utils.TimeUtil.getDateTimeString

class DateTimeEditFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), GenericDateTimeFieldView {

    private var entryExpiresLabelView: TextInputLayout
    private var entryExpiresTextView: TextView
    private var entryExpiresCheckBox: CompoundButton

    private var mDateTime: DateInstant = DateInstant.IN_ONE_MONTH_DATE_TIME

    private var mDefault: DateInstant = DateInstant.NEVER_EXPIRES

    var setOnDateClickListener: ((DateInstant) -> Unit)? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_edit_date_time, this)

        entryExpiresLabelView = findViewById(R.id.expiration_label)
        entryExpiresTextView = findViewById(R.id.expiration_text)
        entryExpiresCheckBox = findViewById(R.id.expiration_checkbox)

        entryExpiresLabelView.setEndIconOnClickListener {
            setOnDateClickListener?.invoke(dateTime)
        }
        entryExpiresCheckBox.setOnCheckedChangeListener { _, isChecked ->
            defineDateTimeAfterCheckBoxChange(isChecked)
        }
    }

    private fun defineDateTimeAfterCheckBoxChange(isChecked: Boolean) {
        dateTime = if (isChecked) {
            when (mDateTime.type) {
                DateInstant.Type.DATE_TIME -> DateInstant.IN_ONE_MONTH_DATE_TIME
                DateInstant.Type.DATE -> DateInstant.IN_ONE_MONTH_DATE
                DateInstant.Type.TIME -> DateInstant.IN_ONE_HOUR_TIME
            }
        } else mDefault
    }

    override var label: String
        get() {
            return entryExpiresCheckBox.hint.toString()
        }
        set(value) {
            entryExpiresLabelView.hint = value
            entryExpiresCheckBox.text = value
        }

    var type: DateInstant.Type
        get() {
            return mDateTime.type
        }
        set(value) {
            mDateTime.type = value
        }

    override var activation: Boolean
        get() {
            return entryExpiresCheckBox.isChecked
        }
        set(value) {
            entryExpiresCheckBox.isChecked = value
            defineDateTimeAfterCheckBoxChange(value)
        }

    /**
     * Warning dateTime.type is ignore, use type instead
     */
    override var dateTime: DateInstant
        get() {
            return if (activation) mDateTime
            else mDefault
        }
        set(value) {
            mDateTime = DateInstant(value.date, mDateTime.type)
            entryExpiresTextView.text = if (entryExpiresCheckBox.isChecked) {
                entryExpiresTextView.alpha = 1f
                entryExpiresLabelView.isEnabled = true
                mDateTime.getDateTimeString(resources)
            } else {
                entryExpiresTextView.alpha = 0.5f
                entryExpiresLabelView.isEnabled = false
                resources.getString(R.string.never_expires)
            }
        }

    override var value: String
        get() {
            return if (activation) dateTime.toString() else ""
        }
        set(value) {
            mDateTime = try {
                DateInstant(value)
            } catch (e: Exception) {
                mDefault
            }
        }

    override var default: String
        get() = mDefault.toString()
        set(value) {
            mDefault = try {
                DateInstant(value)
            } catch (e: Exception) {
                mDefault
            }
        }

    override var isFieldVisible: Boolean
        get() {
            return isVisible
        }
        set(value) {
            isVisible = value
        }
}
