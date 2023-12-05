package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R

@RequiresApi(api = Build.VERSION_CODES.M)
class AdvancedUnlockInfoView @JvmOverloads constructor(context: Context,
                                                       attrs: AttributeSet? = null,
                                                       defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private var biometricButtonView: Button? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_advanced_unlock, this)

        biometricButtonView = findViewById(R.id.biometric_button)
    }

    fun setIconViewClickListener(listener: OnClickListener?) {
        biometricButtonView?.setOnClickListener(listener)
    }

    var title: CharSequence
        get() {
            return biometricButtonView?.text?.toString() ?: ""
        }
        set(value) {
            biometricButtonView?.text = value
        }

    fun setTitle(@StringRes textId: Int) {
        title = context.getString(textId)
    }

    fun setMessage(text: CharSequence) {
        if (text.isNotEmpty())
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    fun setMessage(@StringRes textId: Int) {
        Toast.makeText(context, textId, Toast.LENGTH_LONG).show()
    }

}