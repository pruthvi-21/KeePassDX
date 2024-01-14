package com.kunzisoft.keepass.activities.fragments

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.tabs.TabLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.EntryActivity
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.adapters.TagsAdapter
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
import com.kunzisoft.keepass.view.TemplateView
import com.kunzisoft.keepass.view.TextFieldView
import com.kunzisoft.keepass.view.showByFading
import com.kunzisoft.keepass.viewmodels.EntryViewModel
import com.kunzisoft.keepass.utils.AttachmentUtil
import java.io.IOException

class EntryFragment : DatabaseFragment() {

    private lateinit var rootView: View

    private var contentView: View? = null
    private lateinit var tagsListView: RecyclerView
    private lateinit var templateView: TemplateView
    private var attachmentsListView: RecyclerView? = null
    private var metadataView: View? = null
    private lateinit var uuidReferenceView: TextFieldView
    private lateinit var createdDateView: TextFieldView
    private lateinit var modifiedDateView: TextFieldView
    private lateinit var historyView: TextView

    private var attachmentsAdapter: EntryAttachmentsItemsAdapter? = null
    private var tagsAdapter: TagsAdapter? = null

    private var mClipboardHelper: ClipboardHelper? = null

    private val mEntryViewModel: EntryViewModel by activityViewModels()

    private var tabPosition = -1

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

        contentView = view.findViewById(R.id.content_view)
        tagsListView = view.findViewById(R.id.entry_tags_list_view)
        templateView = view.findViewById(R.id.entry_template)
        loadTemplateSettings()

        tagsAdapter = TagsAdapter(requireContext())
        tagsListView.adapter = tagsAdapter

        attachmentsListView = view.findViewById(R.id.entry_attachments_list)
        attachmentsListView?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        metadataView = view.findViewById(R.id.metadata_view)
        historyView = view.findViewById(R.id.history_count_view)
        setupMetadata(view)

        val tabPos = savedInstanceState?.getInt(EntryActivity.KEY_TAB_POSITION)
            ?: EntryActivity.DEFAULT_TAB_POSITION
        onTabSelected(tabPos)

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

        mEntryViewModel.entryHistory.observe(viewLifecycleOwner) {
            it?.size ?: return@observe

            val historyCount = it.size
            historyView.text = "$historyCount"
            historyView.isEnabled = historyCount > 0
            historyView.setOnClickListener {
                activity?.supportFragmentManager?.let { it1 ->
                    EntryHistoryBottomSheetFragment()
                        .show(it1, "EntryHistoryBottomSheetFragment")
                }
            }
        }
    }

    private fun setupMetadata(view: View) {
        uuidReferenceView = view.findViewById(R.id.uuid_view)
        createdDateView = view.findViewById(R.id.created_view)
        modifiedDateView = view.findViewById(R.id.modified_view)

        uuidReferenceView.apply {
            setLabel(R.string.entry_UUID)
            setProtection(false)
            setCopyButtonState(TextFieldView.ButtonState.GONE)
        }
        createdDateView.apply {
            setLabel(R.string.entry_created)
            setProtection(false)
            setCopyButtonState(TextFieldView.ButtonState.GONE)
        }
        modifiedDateView.apply {
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

        attachmentsListView?.adapter = attachmentsAdapter
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

        createdDateView.isVisible = if (createdTime != null) {
            createdDateView.value = createdTime
            true
        } else false

        modifiedDateView.isVisible = if (modifiedTime != null) {
            modifiedDateView.value = modifiedTime
            true
        } else false

        // Assign special data
        context?.let {
            com.kunzisoft.keepass.utils.UuidUtil.toHexString(entryInfo?.id)?.let {
                uuidReferenceView.value = it
            }
        }

        // Assign tags
        entryInfo?.let {
            val tags = it.tags
            tagsListView.visibility = if (tags.isEmpty()) View.GONE else View.VISIBLE
            tagsAdapter?.setTags(tags)
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
//        attachmentsListView.visibility = if (attachments.isEmpty()) View.GONE else View.VISIBLE
        attachmentsAdapter?.assignItems(attachments.map {
            EntryAttachmentState(it, StreamDirection.DOWNLOAD)
        })

        val isPreviewSettingEnabled = PreferencesUtil.isPreviewFilesEnabled(requireContext())
        attachmentsAdapter?.onItemClickListener = { item, viewPreview ->
            if (isPreviewSettingEnabled && viewPreview) {
                try {
                    mDatabase?.let {
                        AttachmentUtil.previewAttachment(requireContext(), it, item.attachment)
                    }
                } catch (e: IOException) {
                    Log.e("TAG", "catch: exception occurred", e)
                }
            } else {
                mEntryViewModel.onAttachmentSelected(item.attachment)
            }
        }
    }

    fun putAttachment(attachmentToDownload: EntryAttachmentState) {
        attachmentsAdapter?.putItem(attachmentToDownload)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EntryActivity.KEY_TAB_POSITION, tabPosition)
        super.onSaveInstanceState(outState)
    }

    companion object {
        fun getInstance(): EntryFragment {
            return EntryFragment().apply {
                arguments = Bundle()
            }
        }
    }

    private val fadeInSlideLeft by lazy {
        AnimationUtils.loadAnimation(context, R.anim.fade_in_slide_left)
    }

    private val fadeInSlideRight by lazy {
        AnimationUtils.loadAnimation(context, R.anim.fade_in_slide_right)
    }

    fun onTabSelected(tabAt: Int) {
        var enterAnim: Animation? = null

        if (tabPosition != -1) {
            enterAnim = if (tabPosition < tabAt) fadeInSlideLeft
            else fadeInSlideRight

            contentView?.clearAnimation()
            attachmentsListView?.clearAnimation()
            metadataView?.clearAnimation()
        }

        tabPosition = tabAt

        when (tabPosition) {
            0 -> {
                if (enterAnim != null) contentView?.startAnimation(enterAnim)
                contentView?.isVisible = true
                attachmentsListView?.isVisible = false
                metadataView?.isVisible = false
            }

            1 -> {
                if (enterAnim != null) attachmentsListView?.startAnimation(enterAnim)
                contentView?.isVisible = false
                attachmentsListView?.isVisible = true
                metadataView?.isVisible = false
            }

            2 -> {
                if (enterAnim != null) metadataView?.startAnimation(enterAnim)
                contentView?.isVisible = false
                attachmentsListView?.isVisible = false
                metadataView?.isVisible = true
            }
        }

    }
}
