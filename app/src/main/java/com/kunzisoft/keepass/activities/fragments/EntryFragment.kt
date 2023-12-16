package com.kunzisoft.keepass.activities.fragments

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.ImageViewerActivity
import com.kunzisoft.keepass.activities.PdfViewerActivity
import com.kunzisoft.keepass.activities.TextFileViewerActivity
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.getLocalizedName
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.utils.ColorUtils
import com.kunzisoft.keepass.utils.TimeUtil.getDateTimeString
import com.kunzisoft.keepass.utils.UuidUtil
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.view.DividerItemDecorator
import com.kunzisoft.keepass.view.TemplateView
import com.kunzisoft.keepass.view.TextFieldView
import com.kunzisoft.keepass.view.showByFading
import com.kunzisoft.keepass.viewmodels.EntryViewModel
import com.libx.ui.views.CollapsibleLinearLayout

class EntryFragment : DatabaseFragment() {

    private lateinit var rootView: View
    private lateinit var mainSection: View

    private lateinit var templateView: TemplateView

    private lateinit var attachmentsContainerView: View
    private lateinit var attachmentsListView: RecyclerView
    private var attachmentsAdapter: EntryAttachmentsItemsAdapter? = null

    private var mClipboardHelper: ClipboardHelper? = null

    private val mEntryViewModel: EntryViewModel by activityViewModels()

    private var uuidReferenceView: TextFieldView? = null
    private var createdDateView: TextFieldView? = null
    private var modifiedDateView: TextFieldView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_entry, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        context?.let { context ->
            mClipboardHelper = ClipboardHelper(context)
        }

        rootView = view
        // Hide only the first time
        if (savedInstanceState == null) {
            view.isVisible = false
        }

        mainSection = view.findViewById(R.id.entry_section_main)

        templateView = view.findViewById(R.id.entry_template)
        loadTemplateSettings()

        attachmentsContainerView = view.findViewById(R.id.entry_attachments_container)
        attachmentsListView = view.findViewById(R.id.entry_attachments_list)
        attachmentsListView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        attachmentsListView.addItemDecoration(
            DividerItemDecorator(
                ResourcesCompat.getDrawable(resources, R.drawable.divider, requireContext().theme)
            )
        )

        setupMetadata(view)

        val colorSecondary =
            ColorUtils.getColorFromAttr(requireContext(), R.attr.colorSecondary, Color.BLUE)

        mEntryViewModel.onOtpElementUpdated.observe(viewLifecycleOwner) { otpElement ->
            val progressView = templateView.otpView?.progressView

            if (otpElement == null) {
                progressView?.visibility = View.GONE
            } else when (otpElement.type) {
                // Only add token if HOTP
                OtpType.HOTP -> {
                    progressView?.visibility = View.GONE
                }
                // Refresh view if TOTP
                OtpType.TOTP -> {
                    progressView?.apply {
                        max = otpElement.period
                        setProgressCompat(otpElement.secondsRemaining, true)
                        if (otpElement.secondsRemaining.toFloat() / max > 0.3) setIndicatorColor(
                            colorSecondary
                        )
                        else setIndicatorColor(Color.RED)

                        visibility = View.VISIBLE
                    }
                }
            }
        }

        mEntryViewModel.entryInfoHistory.observe(viewLifecycleOwner) { entryInfoHistory ->
            if (entryInfoHistory != null) {
                templateView.setTemplate(entryInfoHistory.template)
                assignEntryInfo(entryInfoHistory.entryInfo)
                // Smooth appearing
                rootView.showByFading()
                resetAppTimeoutWhenViewFocusedOrChanged(rootView)
            }
        }

        mEntryViewModel.onAttachmentAction.observe(viewLifecycleOwner) { entryAttachmentState ->
            entryAttachmentState?.let {
                if (it.streamDirection != StreamDirection.UPLOAD) {
                    putAttachment(it)
                }
            }
        }
    }

    private fun setupMetadata(view: View) {
        uuidReferenceView = view.findViewById(R.id.uuid_view)
        createdDateView = view.findViewById(R.id.created_view)
        modifiedDateView = view.findViewById(R.id.modified_view)

        uuidReferenceView?.apply {
            setLabel(R.string.entry_UUID)
            setProtection(false)
            setCopyButtonState(TextFieldView.ButtonState.GONE)
        }
        createdDateView?.apply {
            setLabel(R.string.entry_created)
            setProtection(false)
            setCopyButtonState(TextFieldView.ButtonState.GONE)
        }
        modifiedDateView?.apply {
            setLabel(R.string.entry_modified)
            setProtection(false)
            setCopyButtonState(TextFieldView.ButtonState.GONE)
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        context?.let { context ->
            attachmentsAdapter = EntryAttachmentsItemsAdapter(context)
            attachmentsAdapter?.database = database
        }

        attachmentsListView.adapter = attachmentsAdapter
    }

    private fun loadTemplateSettings() {
        context?.let { context ->
            templateView.setFirstTimeAskAllowCopyProtectedFields(
                PreferencesUtil.isFirstTimeAskAllowCopyProtectedFields(
                    context
                )
            )
            templateView.setAllowCopyProtectedFields(
                PreferencesUtil.allowCopyProtectedFields(
                    context
                )
            )
        }
    }

    private fun assignEntryInfo(entryInfo: EntryInfo?) {
        // Set copy buttons
        templateView.apply {
            setOnAskCopySafeClickListener {
                showClipboardDialog()
            }

            setOnCopyActionClickListener { field ->
                mClipboardHelper?.timeoutCopyToClipboard(
                    TemplateField.getLocalizedName(context, field.name),
                    field.protectedValue.stringValue,
                    field.protectedValue.isProtected
                )
            }
        }

        // Populate entry views
        templateView.setEntryInfo(entryInfo)

        // OTP timer updated
        templateView.setOnOtpElementUpdated { otpElementUpdated ->
            mEntryViewModel.onOtpElementUpdated(otpElementUpdated)
        }

        // Manage attachments
        assignAttachments(entryInfo?.attachments ?: listOf())

        // Assign dates
        val createdTime = entryInfo?.creationTime?.getDateTimeString(resources)
        val modifiedTime = entryInfo?.lastModificationTime?.getDateTimeString(resources)

        createdDateView?.isVisible = if (createdTime != null) {
            createdDateView?.value = createdTime
            true
        } else false

        modifiedDateView?.isVisible = if (modifiedTime != null) {
            modifiedDateView?.value = modifiedTime
            true
        } else false

        // TODO Custom data
        // customDataView.text = entryInfo?.customData?.toString()

        // Assign special data
        context?.let {
            uuidReferenceView?.isVisible = PreferencesUtil.showUUID(it)
            UuidUtil.toHexString(entryInfo?.id)?.let {
                uuidReferenceView?.value = it
            }
        }
    }

    private fun showClipboardDialog() {
        context?.let {
            AlertDialog.Builder(it)
                .setView(R.layout.alert_dialog_layout)
                .setMessage(
                    getString(R.string.allow_copy_password_warning) +
                            "\n\n" +
                            getString(R.string.clipboard_warning)
                )
                .create().apply {
                    setButton(AlertDialog.BUTTON_POSITIVE, getText(R.string.enable)) { dialog, _ ->
                        PreferencesUtil.setAllowCopyPasswordAndProtectedFields(context, true)
                        finishDialog(dialog)
                    }
                    setButton(AlertDialog.BUTTON_NEGATIVE, getText(R.string.disable)) { dialog, _ ->
                        PreferencesUtil.setAllowCopyPasswordAndProtectedFields(context, false)
                        finishDialog(dialog)
                    }
                    show()
                }
        }
    }

    private fun finishDialog(dialog: DialogInterface) {
        dialog.dismiss()
        loadTemplateSettings()
        templateView.reload()
    }

    /* -------------
     * Attachments
     * -------------
     */

    private fun assignAttachments(attachments: List<Attachment>) {
        attachmentsContainerView.visibility = if (attachments.isEmpty()) View.GONE else View.VISIBLE
        attachmentsContainerView.apply {
            val attachmentsHeaderView = findViewById<View>(R.id.entry_show_attachments)
            val attachmentsContentView =
                findViewById<CollapsibleLinearLayout>(R.id.entry_attachments_content)
            val arrow = findViewById<ImageView>(R.id.attachments_arrow)
            attachmentsHeaderView.setOnClickListener {
                if (attachmentsContentView.isExpanded) {
                    attachmentsContentView.collapse()
                    arrow.animate()
                        .rotation(0f)
                        .setDuration(200)
                        .start()
                } else {
                    attachmentsContentView.expand()
                    arrow.animate()
                        .rotation(90f)
                        .setDuration(200)
                        .start()
                }
            }
        }
        attachmentsAdapter?.assignItems(attachments.map {
            EntryAttachmentState(it, StreamDirection.DOWNLOAD)
        })

        attachmentsAdapter?.onItemClickListener = { item, fileType, viewPreview ->
            if (viewPreview) {
                when (fileType) {
                    EntryAttachmentsItemsAdapter.FileType.PDF -> {
                        pdfAndImagePreviewLauncher.launch(
                            Intent(requireContext(), PdfViewerActivity::class.java).apply {
                                putExtra(EXTRA_ATTACHMENT_TAG, item.attachment)
                            }
                        )
                    }

                    EntryAttachmentsItemsAdapter.FileType.IMAGE -> {
                        pdfAndImagePreviewLauncher.launch(
                            Intent(requireContext(), ImageViewerActivity::class.java).apply {
                                putExtra(EXTRA_ATTACHMENT_TAG, item.attachment)
                            }
                        )
                    }

                    EntryAttachmentsItemsAdapter.FileType.TEXT -> {
                        pdfAndImagePreviewLauncher.launch(
                            Intent(requireContext(), TextFileViewerActivity::class.java).apply {
                                putExtra(EXTRA_ATTACHMENT_TAG, item.attachment)
                            }
                        )
                    }

                    else -> {
                        mEntryViewModel.onAttachmentSelected(item.attachment)
                    }
                }
            } else {
                mEntryViewModel.onAttachmentSelected(item.attachment)
            }
        }
    }

    fun putAttachment(attachmentToDownload: EntryAttachmentState) {
        attachmentsAdapter?.putItem(attachmentToDownload)
    }

    //launcher for pdf preview
    val pdfAndImagePreviewLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    result.data?.let {
                        val attachment =
                            it.getParcelableExtraCompat<Attachment>(EXTRA_ATTACHMENT_TAG)
                        val value = it.getBooleanExtra(EXTRA_ACTION_DOWNLOAD, false)
                        if (value && attachment != null) {
                            mEntryViewModel.onAttachmentSelected(attachment)
                        }
                    }
                }

                Activity.RESULT_CANCELED -> {
                    result.data?.let {
                        if (it.getBooleanExtra(EXTRA_PREVIEW_FAILED_TAG, false)) {
                            Toast.makeText(context, "Unable to load", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    companion object {

        const val EXTRA_ACTION_DOWNLOAD = "extra_action_download"
        const val EXTRA_ATTACHMENT_TAG = "extra_attachment_tag"
        const val EXTRA_PREVIEW_FAILED_TAG = "extra_preview_failed_tag"

        fun getInstance(): EntryFragment {
            return EntryFragment().apply {
                arguments = Bundle()
            }
        }
    }
}
