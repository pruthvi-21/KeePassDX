package com.kunzisoft.keepass.view

import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.helpers.setOpenDocumentClickListener
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.model.CredentialStorage
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.KeyboardUtil.showKeyboard

class MainCredentialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private var hardwareKeyContainerView: View

    private var checkboxPasswordView: CompoundButton
    private var passwordTextView: EditText
    private var checkboxKeyFileView: CompoundButton
    private var keyFileSelectionView: KeyFileSelectionView
    private var checkboxHardwareView: CompoundButton
    private var hardwareKeySelectionView: HardwareKeySelectionView

    var onPasswordChecked: (CompoundButton.OnCheckedChangeListener)? = null
    var onKeyFileChecked: (CompoundButton.OnCheckedChangeListener)? = null
    var onHardwareKeyChecked: (CompoundButton.OnCheckedChangeListener)? = null
    var onValidateListener: (() -> Unit)? = null

    private var mCredentialStorage: CredentialStorage = CredentialStorage.PASSWORD

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_main_credentials, this)

        hardwareKeyContainerView = findViewById(R.id.container_hardware_key)

        checkboxPasswordView = findViewById(R.id.password_checkbox)
        passwordTextView = findViewById(R.id.password_text_view)
        checkboxKeyFileView = findViewById(R.id.keyfile_checkbox)
        keyFileSelectionView = findViewById(R.id.keyfile_selection)
        checkboxHardwareView = findViewById(R.id.hardware_key_checkbox)
        hardwareKeySelectionView = findViewById(R.id.hardware_key_selection)

        val onEditorActionListener = object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    validateCredential()
                    return true
                }
                return false
            }
        }

        passwordTextView.setOnEditorActionListener(onEditorActionListener)
        passwordTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (editable.toString().isNotEmpty() && !checkboxPasswordView.isChecked)
                    checkboxPasswordView.isChecked = true
            }
        })
        passwordTextView.setOnKeyListener { _, _, keyEvent ->
            var handled = false
            if (keyEvent.action == KeyEvent.ACTION_DOWN
                && keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                validateCredential()
                handled = true
            }
            handled
        }

        checkboxPasswordView.setOnCheckedChangeListener { view, checked ->
            onPasswordChecked?.onCheckedChanged(view, checked)
        }
        checkboxKeyFileView.setOnCheckedChangeListener { view, checked ->
            if (checked) {
                if (keyFileSelectionView.uri == null) {
                    checkboxKeyFileView.isChecked = false
                }
            } else {
                keyFileSelectionView.uri = null
            }
            onKeyFileChecked?.onCheckedChanged(view, checked)
        }
        keyFileSelectionView.fileSelectListener = {
            checkboxKeyFileView.isChecked = it
        }
        showHardwareKeyLayout(PreferencesUtil.allowHardwareKey(context))
    }

    fun validateCredential() {
        onValidateListener?.invoke()
    }

    fun populatePasswordTextView(text: String?) {
        if (text == null || text.isEmpty()) {
            passwordTextView.setText("")
            if (checkboxPasswordView.isChecked)
                checkboxPasswordView.isChecked = false
        } else {
            passwordTextView.setText(text)
            if (checkboxPasswordView.isChecked)
                checkboxPasswordView.isChecked = true
        }
    }

    fun populateKeyFileView(uri: Uri?) {
        if (uri == null || uri.toString().isEmpty()) {
            keyFileSelectionView.uri = null
            if (checkboxKeyFileView.isChecked)
                checkboxKeyFileView.isChecked = false
        } else {
            keyFileSelectionView.uri = uri
            if (!checkboxKeyFileView.isChecked)
                checkboxKeyFileView.isChecked = true
        }
    }

    fun populateHardwareKeyView(hardwareKey: HardwareKey?) {
        if (hardwareKey == null) {
            hardwareKeySelectionView.hardwareKey = null
            if (checkboxHardwareView.isChecked)
                checkboxHardwareView.isChecked = false
        } else {
            hardwareKeySelectionView.hardwareKey = hardwareKey
            if (!checkboxHardwareView.isChecked)
                checkboxHardwareView.isChecked = true
        }
    }

    fun setOpenKeyfileClickListener(externalFileHelper: ExternalFileHelper?) {
        keyFileSelectionView.keyFileSelectionButton.setOpenDocumentClickListener(externalFileHelper)
    }

    fun isFill(): Boolean {
        return checkboxPasswordView.isChecked
                || (checkboxKeyFileView.isChecked && keyFileSelectionView.uri != null)
                || (checkboxHardwareView.isChecked && hardwareKeySelectionView.hardwareKey != null)
    }

    fun getMainCredential(): MainCredential {
        return MainCredential().apply {
            this.password = if (checkboxPasswordView.isChecked)
                passwordTextView.text?.toString() else null
            this.keyFileUri = if (checkboxKeyFileView.isChecked)
                keyFileSelectionView.uri else null
            this.hardwareKey =
                if (checkboxHardwareView.isChecked && PreferencesUtil.allowHardwareKey(context))
                    hardwareKeySelectionView.hardwareKey else null
        }
    }

    fun changeConditionToStoreCredential(credentialStorage: CredentialStorage) {
        this.mCredentialStorage = credentialStorage
    }

    fun conditionToStoreCredential(): Boolean {
        // TODO HARDWARE_KEY
        return when (mCredentialStorage) {
            CredentialStorage.PASSWORD -> checkboxPasswordView.isChecked
            CredentialStorage.KEY_FILE -> false
            CredentialStorage.HARDWARE_KEY -> false
        }
    }

    /**
     * Return content of the store credential view allowed,
     * String? for password
     *
     */
    fun retrieveCredentialForStorage(listener: CredentialStorageListener): ByteArray? {
        return when (mCredentialStorage) {
            CredentialStorage.PASSWORD -> listener.passwordToStore(passwordTextView.text?.toString())
            CredentialStorage.KEY_FILE -> listener.keyfileToStore(keyFileSelectionView.uri)
            CredentialStorage.HARDWARE_KEY -> listener.hardwareKeyToStore()
        }
    }

    interface CredentialStorageListener {
        fun passwordToStore(password: String?): ByteArray?
        fun keyfileToStore(keyfile: Uri?): ByteArray?
        fun hardwareKeyToStore(): ByteArray?
    }

    fun requestPasswordFocus() {
        passwordTextView.requestFocusFromTouch()
    }

    // Auto select the password field and open keyboard
    fun focusPasswordFieldAndOpenKeyboard() {
        passwordTextView.postDelayed({
            passwordTextView.requestFocusFromTouch()
            passwordTextView.showKeyboard()
        }, 100)
    }

    fun showHardwareKeyLayout(show: Boolean) {
        if (!show) {
            hardwareKeyContainerView.isVisible = false
        } else {
            hardwareKeyContainerView.isVisible = true
            checkboxHardwareView.setOnCheckedChangeListener { view, checked ->
                if (checked) {
                    if (hardwareKeySelectionView.hardwareKey == null) {
                        checkboxHardwareView.isChecked = false
                    }
                }
                onHardwareKeyChecked?.onCheckedChanged(view, checked)
            }

            hardwareKeySelectionView.selectionListener = { _ ->
                checkboxHardwareView.isChecked = true
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val saveState = SavedState(superState)
        saveState.mCredentialStorage = this.mCredentialStorage
        return saveState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        this.mCredentialStorage = state.mCredentialStorage ?: CredentialStorage.DEFAULT
    }

    internal class SavedState : BaseSavedState {
        var mCredentialStorage: CredentialStorage? = null

        constructor(superState: Parcelable?) : super(superState) {}

        private constructor(parcel: Parcel) : super(parcel) {
            mCredentialStorage = CredentialStorage.getFromOrdinal(parcel.readInt())
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(mCredentialStorage?.ordinal ?: 0)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}