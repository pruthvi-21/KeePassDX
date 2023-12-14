package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.text.InputFilter
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.isStandardPasswordName
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil

class TextEditFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle), GenericTextFieldView {

    private var labelViewId = ViewCompat.generateViewId()
    private var textInputLayoutId = ViewCompat.generateViewId()
    private var textInputEditTextId = ViewCompat.generateViewId()
    private var actionImageButtonId = ViewCompat.generateViewId()

    private var isColorizedPasswordActivated = PreferencesUtil.colorizePassword(context)

    private val textInputLayout = TextInputLayout(
        ContextThemeWrapper(context, R.style.Theme_Keepass_TextInputLayout)
    ).apply {
        layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
            weight = 1f
        }
    }

    private val textInputEditText = TextInputEditText(textInputLayout.context).apply {
        layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        inputType = EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO
        }
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        maxLines = 1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setHintTextColor(resources.getColorStateList(R.color.hint_color, null))
        }
    }

    private var actionImageButton = AppCompatImageButton(
        ContextThemeWrapper(context, R.style.Theme_Keepass_ImageButton_Simple),
        null,
        0
    ).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        visibility = View.GONE
        contentDescription = context.getString(R.string.menu_edit)
    }

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
        orientation = VERTICAL
        buildViews()

        textInputLayout.addView(textInputEditText)
        val linearLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            addView(textInputLayout)
            addView(actionImageButton)
            gravity = Gravity.CENTER
        }
        addView(labelView)
        addView(linearLayout)

        setPadding(
            0,
            (5f * resources.displayMetrics.density).toInt(),
            0,
            (5f * resources.displayMetrics.density).toInt()
        )
    }

    private fun buildViews() {
        labelView.id = labelViewId
        textInputLayout.id = textInputLayoutId
        textInputEditText.id = textInputEditTextId
        actionImageButton.id = actionImageButtonId
    }

    override fun applyFontVisibility(fontInVisibility: Boolean) {
        if (fontInVisibility)
            textInputEditText.applyFontVisibility()
    }

    fun getActionImageView(): View {
        return actionImageButton
    }

    private fun spannableValue(value: String?): Spannable? {
        if (value == null)
            return null
        return if (isColorizedPasswordActivated && TemplateField.isStandardPasswordName(
                context,
                label
            )
        )
            PasswordGenerator.getColorizedPassword(value)
        else
            SpannableString(value)
    }

    override var label: String
        get() {
            return textInputEditText.hint?.toString() ?: ""
        }
        set(value) {
            setHint(value)
            labelView.text = value
            // Define views Ids with label value
            labelViewId = "hintViewId $value".hashCode()
            textInputLayoutId = "labelViewId $value".hashCode()
            textInputEditTextId = "valueViewId $value".hashCode()
            actionImageButtonId = "actionImageButtonId $value".hashCode()
            buildViews()
        }

    private fun setHint(value: String) {
        textInputEditText.hint = when (value.lowercase()) {
            "username" -> "Username or Email"
            "password" -> "Your password"
            "url" -> "Website Url"
            else -> value
        }
    }

    override var value: String
        get() {
            return textInputEditText.text?.toString() ?: ""
        }
        set(value) {
            textInputEditText.setText(spannableValue(value))
        }

    override var default: String = ""

    fun setMaxChars(numberChars: Int) {
        when {
            numberChars <= 0 -> {
                textInputEditText.filters += InputFilter.LengthFilter(MAX_CHARS_LIMIT)
            }

            else -> {
                @Suppress("KotlinConstantConditions")
                val chars = if (numberChars > MAX_CHARS_LIMIT) MAX_CHARS_LIMIT else numberChars
                textInputEditText.filters += InputFilter.LengthFilter(chars)
            }
        }
    }

    fun setMaxLines(numberLines: Int) {
        when {
            numberLines == 1 -> {
                textInputEditText.inputType = textInputEditText.inputType or
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
                textInputEditText.maxLines = 1
            }

            numberLines <= 0 -> {
                textInputEditText.inputType = textInputEditText.inputType or
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                textInputEditText.maxLines = MAX_LINES_LIMIT
            }

            else -> {
                @Suppress("KotlinConstantConditions")
                val lines = if (numberLines > MAX_LINES_LIMIT) MAX_LINES_LIMIT else numberLines
                textInputEditText.inputType = textInputEditText.inputType or
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                textInputEditText.maxLines = lines
            }
        }
    }

    fun setProtection(protection: Boolean) {
        if (protection) {
            textInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            val visibilityTag = if (PreferencesUtil.hideProtectedValue(context))
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            else
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            textInputEditText.inputType = textInputEditText.inputType or visibilityTag
        }
    }

    override fun setOnActionClickListener(
        onActionClickListener: OnClickListener?,
        @DrawableRes actionImageId: Int?
    ) {
        actionImageId?.let {
            actionImageButton.setImageDrawable(ContextCompat.getDrawable(context, it))
        }
        actionImageButton.setOnClickListener(onActionClickListener)
        actionImageButton.visibility =
            if (onActionClickListener == null) View.GONE else View.VISIBLE
    }

    override var isFieldVisible: Boolean
        get() {
            return isVisible
        }
        set(value) {
            isVisible = value
        }

    companion object {
        const val MAX_CHARS_LIMIT = Integer.MAX_VALUE
        const val MAX_LINES_LIMIT = Integer.MAX_VALUE
    }
}
