package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R

class TextSelectFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle), GenericTextFieldView {

    private var spinnerId = View.generateViewId()

    private var valueSpinnerAdapter = ValueSpinnerAdapter(context)
    private var mDefaultPosition = 0

    private var spinner: AppCompatSpinner

    private val labelView =
        TextView(ContextThemeWrapper(context, R.style.Theme_KeePass_TextInput_Label)).apply {
            layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics
                    ).toInt()
                }
        }

    init {
        val view = inflate(context, R.layout.layout_select_field_spinner, null)
        spinner = view as AppCompatSpinner
        spinner.adapter = valueSpinnerAdapter

        orientation = VERTICAL

        buildViews()
        addView(labelView)
        addView(spinner)

        setPadding(
            0,
            (7f * resources.displayMetrics.density).toInt(),
            0,
            (3f * resources.displayMetrics.density).toInt()
        )
    }

    private fun buildViews() {
        spinner.id = spinnerId
    }

    override fun applyFontVisibility(fontInVisibility: Boolean) {
        valueSpinnerAdapter.fontVisibility = fontInVisibility
    }

    override var label: String
        get() {
            return labelView.text.toString()
        }
        set(value) {
            labelView.text = value

            spinnerId = "valueViewId $value".hashCode()
            buildViews()
        }

    fun setItems(items: List<String>) {
        valueSpinnerAdapter.setItems(items)
    }

    // To define default value and retrieve selected one
    override var value: String
        get() {
            var selectedItemString = spinner.selectedItem?.toString()
            if (selectedItemString.isNullOrEmpty()) {
                selectedItemString = valueSpinnerAdapter.getItem(0)
            }
            return selectedItemString
        }
        set(value) {
            spinner.setSelection(valueSpinnerAdapter.getPosition(value))
        }

    override var default: String
        get() = valueSpinnerAdapter.getItem(mDefaultPosition)
        set(value) {
            mDefaultPosition = valueSpinnerAdapter.getPosition(value)
            valueSpinnerAdapter.notifyDataSetChanged()
        }

    override fun setOnActionClickListener(
        onActionClickListener: OnClickListener?,
        @DrawableRes actionImageId: Int?
    ) {

    }

    override var isFieldVisible: Boolean
        get() {
            return isVisible
        }
        set(value) {
            isVisible = value
        }

    private class ValueSpinnerAdapter(context: Context) : BaseAdapter() {

        private val inflater: LayoutInflater = LayoutInflater.from(context)
        private var listItems: MutableList<String> = mutableListOf<String>().apply {
            // Don't know why but must be init with at least one item, else no view
            add("")
        }

        var fontVisibility: Boolean = false

        override fun getCount(): Int {
            return listItems.size
        }

        override fun getItem(position: Int): String {
            return listItems[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        fun getPosition(value: String): Int {
            val index = listItems.indexOf(value)
            if (index < 0)
                return 0
            return index
        }

        fun setItems(items: List<String>) {
            listItems.clear()
            listItems.addAll(items)
        }

        fun clear() {
            listItems.clear()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val value: String = getItem(position)

            val holder: ValueSelectorViewHolder
            var valueView = convertView
            if (valueView == null) {
                holder = ValueSelectorViewHolder()
                valueView = inflater.inflate(R.layout.item_select_field_value, parent, false)
                holder.valueText = valueView?.findViewById(R.id.spinner_value)
                if (fontVisibility)
                    holder.valueText?.applyFontVisibility()
                valueView?.tag = holder
            } else {
                holder = valueView.tag as ValueSelectorViewHolder
            }

            holder.valueText?.text = value

            return valueView!!
        }

        inner class ValueSelectorViewHolder {
            var valueText: TextView? = null
        }
    }
}