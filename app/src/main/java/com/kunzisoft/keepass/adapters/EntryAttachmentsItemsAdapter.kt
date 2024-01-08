package com.kunzisoft.keepass.adapters

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.text.format.Formatter
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.helper.getLocalizedName
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.services.AttachmentFileNotificationService.Companion.FILE_PROGRESSION_MAX
import com.kunzisoft.keepass.utils.ColorUtils


class EntryAttachmentsItemsAdapter(context: Context) :
    AnimatedItemsAdapter<EntryAttachmentState, EntryAttachmentsItemsAdapter.EntryBinariesViewHolder>(
        context
    ) {

    var database: ContextualDatabase? = null
    var onItemClickListener: ((item: EntryAttachmentState, viewPreview: Boolean) -> Unit)? =
        null
    var onBinaryPreviewLoaded: ((item: EntryAttachmentState) -> Unit)? = null

    private val imageExtensionList = arrayOf(".jpg", "jpeg", ".png", ".bmp", ".webp", ".gif")
    private val textFileExtensionList = arrayOf(".txt")
    private val pdfExtension = ".pdf"

    private var mTitleColor: Int
    private var colorError = ColorUtils.getColorFromAttr(context, R.attr.colorError, Color.RED)

    init {
        // Get the primary text color of the theme
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val typedArray: TypedArray = context.obtainStyledAttributes(
            typedValue.data, intArrayOf(
                android.R.attr.textColor
            )
        )
        mTitleColor = typedArray.getColor(0, -1)
        typedArray.recycle()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryBinariesViewHolder {
        return EntryBinariesViewHolder(inflater.inflate(R.layout.item_attachment, parent, false))
    }

    override fun onBindViewHolder(holder: EntryBinariesViewHolder, position: Int) {
        val entryAttachmentState = itemsList[position]

        holder.itemView.visibility = View.VISIBLE

        holder.binaryFilePreviewIcon.apply {
            isVisible = true
            val fileName = entryAttachmentState.attachment.name

            if (imageExtensionList.any { fileName.endsWith(it, true) }) {
                setImageResource(R.drawable.ic_thumb_image_24dp)
            } else if (fileName.endsWith(pdfExtension)) {
                setImageResource(R.drawable.ic_thumb_pdf_24dp)
            } else if (textFileExtensionList.any { fileName.endsWith(it, true) }) {
                setImageResource(R.drawable.ic_thumb_txt_24dp)
            } else {
                setImageResource(R.drawable.ic_thumb_file_24dp)
            }
        }

        holder.binaryFileTitle.text = entryAttachmentState.attachment.name

        if (entryAttachmentState.attachment.binaryData.isCorrupted) {
            holder.binaryFilePreviewIcon.apply {
                setColorFilter(colorError)
                setImageResource(R.drawable.ic_attach_file_broken_white_24dp)
            }
            holder.binaryFileTitle.setTextColor(colorError)
        } else {
            holder.binaryFileTitle.setTextColor(mTitleColor)
        }

        val size = entryAttachmentState.attachment.binaryData.getSize()
        val fileSize = Formatter.formatFileSize(context, size)

        holder.binaryFileSize.text = if (entryAttachmentState.attachment.binaryData.isCompressed) {
            context.resources.getString(
                R.string.compression_text_placeholder,
                fileSize,
                CompressionAlgorithm.GZIP.getLocalizedName(context.resources)
            )
        } else {
            context.resources.getString(R.string.filesize_text_placeholder, fileSize)
        }

        holder.binaryFileProgress.max = FILE_PROGRESSION_MAX
        when (entryAttachmentState.streamDirection) {
            StreamDirection.UPLOAD -> {
                holder.binaryFileProgressIcon.isActivated = true
                when (entryAttachmentState.downloadState) {
                    AttachmentState.START,
                    AttachmentState.IN_PROGRESS -> {
                        holder.binaryFileProgressContainer.visibility = View.VISIBLE
                        holder.binaryFileProgress.apply {
                            visibility = View.VISIBLE
                            progress = entryAttachmentState.downloadProgression
                        }
                        holder.binaryFileDeleteButton.apply {
                            visibility = View.GONE
                            setOnClickListener(null)
                        }
                    }

                    AttachmentState.NULL,
                    AttachmentState.ERROR,
                    AttachmentState.CANCELED,
                    AttachmentState.COMPLETE -> {
                        holder.binaryFileProgressContainer.visibility = View.GONE
                        holder.binaryFileProgress.visibility = View.GONE
                        holder.binaryFileDeleteButton.apply {
                            visibility = View.VISIBLE
                            onBindDeleteButton(holder, this, entryAttachmentState, position)
                        }
                    }
                }
                holder.binaryFileInfo.setOnClickListener(null)
            }

            StreamDirection.DOWNLOAD -> {
                holder.binaryFileProgressIcon.isActivated = false
                holder.binaryFileProgressContainer.visibility = View.VISIBLE
                holder.binaryFileDeleteButton.visibility = View.GONE
                holder.binaryFileProgress.apply {
                    visibility = when (entryAttachmentState.downloadState) {
                        AttachmentState.NULL,
                        AttachmentState.COMPLETE,
                        AttachmentState.CANCELED,
                        AttachmentState.ERROR -> View.GONE

                        AttachmentState.START,
                        AttachmentState.IN_PROGRESS -> View.VISIBLE
                    }
                    setProgressCompat(entryAttachmentState.downloadProgression, true)
                }
                holder.binaryFileInfo.setOnClickListener {
                    onItemClickListener?.invoke(
                        entryAttachmentState,
                        !entryAttachmentState.attachment.binaryData.isCorrupted
                    )
                }

                holder.binaryFileProgressIcon.setOnClickListener {
                    onItemClickListener?.invoke(entryAttachmentState, false)
                }
            }
        }
    }

    class EntryBinariesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var binaryFileInfo: View = itemView.findViewById(R.id.item_attachment_info)
        var binaryFileTitle: TextView = itemView.findViewById(R.id.item_attachment_title)
        var binaryFileSize: TextView = itemView.findViewById(R.id.item_attachment_size)
        var binaryFileProgressContainer: View =
            itemView.findViewById(R.id.item_attachment_progress_container)
        var binaryFileProgressIcon: ImageView = itemView.findViewById(R.id.item_attachment_icon)
        var binaryFilePreviewIcon: ImageView = itemView.findViewById(R.id.item_attachment_logo)
        var binaryFileProgress: CircularProgressIndicator =
            itemView.findViewById(R.id.item_attachment_progress)
        var binaryFileDeleteButton: View = itemView.findViewById(R.id.item_attachment_delete_button)
    }
}
