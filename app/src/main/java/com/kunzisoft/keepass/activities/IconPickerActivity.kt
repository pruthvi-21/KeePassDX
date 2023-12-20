package com.kunzisoft.keepass.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.IconEditDialogFragment
import com.kunzisoft.keepass.activities.fragments.IconPickerFragment
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.BinaryDatabaseManager
import com.kunzisoft.keepass.utils.UriUtil.getDocumentFile
import com.kunzisoft.keepass.utils.UriUtil.openUrl
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.view.asError
import com.kunzisoft.keepass.viewmodels.IconPickerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class IconPickerActivity : DatabaseLockActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var coordinatorLayout: CoordinatorLayout
    private var lockView: View? = null

    private var mIconImage: IconImage = IconImage()

    private val mainScope = CoroutineScope(Dispatchers.Main)

    private val iconPickerViewModel: IconPickerViewModel by viewModels()
    private var mCustomIconsSelectionMode = false
    private var mIconsSelected: List<IconImageCustom> = ArrayList()

    private var mExternalFileHelper: ExternalFileHelper? = null

    private var enableUpdateIcon = true
        set(value) {
            field = value

            invalidateOptionsMenu()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_icon_picker)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        updateIconsSelectedViews()

        coordinatorLayout = findViewById(R.id.icon_picker_coordinator)

        mExternalFileHelper = ExternalFileHelper(this)
        mExternalFileHelper?.buildOpenDocument { uri ->
            addCustomIcon(uri)
        }

        lockView = findViewById(R.id.lock_button)
        lockView?.setOnClickListener {
            lockAndExit()
        }

        intent?.getParcelableExtraCompat<IconImage>(EXTRA_ICON)?.let {
            mIconImage = it
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(
                    R.id.icon_picker_fragment, IconPickerFragment.getInstance(
                        // Default selection tab
                        if (mIconImage.custom.isUnknown)
                            IconPickerFragment.IconTab.STANDARD
                        else
                            IconPickerFragment.IconTab.CUSTOM
                    ), ICON_PICKER_FRAGMENT_TAG
                )
            }
        } else {
            mIconImage = savedInstanceState.getParcelableCompat(EXTRA_ICON) ?: mIconImage
        }

        iconPickerViewModel.standardIconPicked.observe(this) { iconStandard ->
            mIconImage.standard = iconStandard
            // Remove the custom icon if a standard one is selected
            mIconImage.custom = IconImageCustom()
            setResult()
            finish()
        }
        iconPickerViewModel.customIconPicked.observe(this) { iconCustom ->
            // Keep the standard icon if a custom one is selected
            mIconImage.custom = iconCustom
            setResult()
            finish()
        }
        iconPickerViewModel.customIconsSelected.observe(this) { iconsSelected ->
            mIconsSelected = iconsSelected
            updateIconsSelectedViews()
        }
        iconPickerViewModel.customIconAdded.observe(this) { iconCustomAdded ->
            if (iconCustomAdded.error && !iconCustomAdded.errorConsumed) {
                Snackbar.make(
                    coordinatorLayout,
                    iconCustomAdded.errorStringId,
                    Snackbar.LENGTH_LONG
                ).asError().show()
                iconCustomAdded.errorConsumed = true
            }
            enableUpdateIcon = true
        }
        iconPickerViewModel.customIconRemoved.observe(this) { iconCustomRemoved ->
            if (iconCustomRemoved.error && !iconCustomRemoved.errorConsumed) {
                Snackbar.make(
                    coordinatorLayout,
                    iconCustomRemoved.errorStringId,
                    Snackbar.LENGTH_LONG
                ).asError().show()
                iconCustomRemoved.errorConsumed = true
            }
            enableUpdateIcon = true
        }
        iconPickerViewModel.customIconUpdated.observe(this) { iconCustomUpdated ->
            if (iconCustomUpdated.error && !iconCustomUpdated.errorConsumed) {
                Snackbar.make(
                    coordinatorLayout,
                    iconCustomUpdated.errorStringId,
                    Snackbar.LENGTH_LONG
                ).asError().show()
                iconCustomUpdated.errorConsumed = true
            }
            iconCustomUpdated.iconCustom?.let {
                mDatabase?.updateCustomIcon(it)
            }
            iconPickerViewModel.deselectAllCustomIcons()
        }
    }

    override fun viewToInvalidateTimeout(): View? {
        return findViewById<ViewGroup>(R.id.icon_picker_container)
    }

    override fun finishActivityIfReloadRequested(): Boolean {
        return true
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)

        invalidateOptionsMenu()
    }

    private fun updateIconsSelectedViews() {
        toolbar.title = if (mIconsSelected.isEmpty()) {
            mCustomIconsSelectionMode = false
            getString(R.string.icon_picker_title)
        } else {
            mCustomIconsSelectionMode = true
            mIconsSelected.size.toString()
        }
        invalidateOptionsMenu()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(EXTRA_ICON, mIconImage)
    }

    override fun onResume() {
        super.onResume()
        lockView?.isVisible = PreferencesUtil.showLockDatabaseButton(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.icon, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_edit)?.apply {
            isEnabled = mIconsSelected.size == 1
            isVisible = isEnabled
        }
        menu?.findItem(R.id.menu_delete)?.apply {
            isEnabled = mCustomIconsSelectionMode
            isVisible = isEnabled
        }
        menu?.findItem(R.id.menu_add_icon)?.apply {
            isEnabled = mIconsSelected.isEmpty() && mDatabase?.allowCustomIcons == true
            isVisible = isEnabled
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (mCustomIconsSelectionMode) {
                    iconPickerViewModel.deselectAllCustomIcons()
                } else {
                    onDatabaseBackPressed()
                }
            }

            R.id.menu_edit -> {
                updateCustomIcon(mIconsSelected[0])
            }

            R.id.menu_delete -> {
                mIconsSelected.forEach { iconToRemove ->
                    removeCustomIcon(iconToRemove)
                }
            }

            R.id.menu_external_icon -> {
                this.openUrl(R.string.external_icon_url)
            }

            R.id.menu_add_icon -> {
                mExternalFileHelper?.openDocument(false)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun addCustomIcon(iconToUploadUri: Uri?) {
        enableUpdateIcon = false
        mainScope.launch {
            withContext(Dispatchers.IO) {
                // on Progress with thread
                val asyncResult: Deferred<IconPickerViewModel.IconCustomState?> = async {
                    val iconCustomState =
                        IconPickerViewModel.IconCustomState(null, true, R.string.error_upload_file)
                    iconToUploadUri?.getDocumentFile(this@IconPickerActivity)
                        ?.also { documentFile ->
                            if (documentFile.length() > MAX_ICON_SIZE) {
                                iconCustomState.errorStringId = R.string.error_file_to_big
                            } else {
                                mDatabase?.buildNewCustomIcon { customIcon, binary ->
                                    if (customIcon != null) {
                                        iconCustomState.iconCustom = customIcon
                                        mDatabase?.let { database ->
                                            BinaryDatabaseManager.resizeBitmapAndStoreDataInBinaryFile(
                                                contentResolver,
                                                database,
                                                iconToUploadUri,
                                                binary
                                            )
                                            when {
                                                binary == null -> {
                                                }

                                                binary.getSize() <= 0 -> {
                                                }

                                                database.isCustomIconBinaryDuplicate(binary) -> {
                                                    iconCustomState.errorStringId =
                                                        R.string.error_duplicate_file
                                                }

                                                else -> {
                                                    iconCustomState.error = false
                                                }
                                            }
                                        }
                                        if (iconCustomState.error) {
                                            mDatabase?.removeCustomIcon(customIcon)
                                        }
                                    }
                                }
                            }
                        }
                    iconCustomState
                }
                withContext(Dispatchers.Main) {
                    asyncResult.await()?.let { customIcon ->
                        iconPickerViewModel.addCustomIcon(customIcon)
                    }
                }
            }
        }
    }

    private fun updateCustomIcon(iconImageCustom: IconImageCustom) {
        IconEditDialogFragment.update(iconImageCustom)
            .show(supportFragmentManager, IconEditDialogFragment.TAG_UPDATE_ICON)
    }

    private fun removeCustomIcon(iconImageCustom: IconImageCustom) {
        enableUpdateIcon = false
        iconPickerViewModel.deselectAllCustomIcons()
        mDatabase?.removeCustomIcon(iconImageCustom)
        iconPickerViewModel.removeCustomIcon(
            IconPickerViewModel.IconCustomState(iconImageCustom, false, R.string.error_remove_file)
        )
    }

    private fun setResult() {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_ICON, mIconImage)
        })
    }

    override fun onDatabaseBackPressed() {
        if (mCustomIconsSelectionMode) {
            iconPickerViewModel.deselectAllCustomIcons()
            return
        }
        setResult()
        super.onDatabaseBackPressed()
    }

    companion object {

        private const val ICON_PICKER_FRAGMENT_TAG = "ICON_PICKER_FRAGMENT_TAG"
        private const val EXTRA_ICON = "EXTRA_ICON"
        private const val MAX_ICON_SIZE = 5242880

        fun registerIconSelectionForResult(
            context: FragmentActivity,
            listener: (icon: IconImage) -> Unit
        ): ActivityResultLauncher<Intent> {
            return context.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    listener.invoke(
                        result.data?.getParcelableExtraCompat(EXTRA_ICON) ?: IconImage()
                    )
                }
            }
        }

        fun launch(
            context: FragmentActivity,
            previousIcon: IconImage?,
            resultLauncher: ActivityResultLauncher<Intent>
        ) {
            // Create an instance to return the picker icon
            resultLauncher.launch(
                Intent(context, IconPickerActivity::class.java).apply {
                    if (previousIcon != null)
                        putExtra(EXTRA_ICON, previousIcon)
                }
            )
        }
    }
}
