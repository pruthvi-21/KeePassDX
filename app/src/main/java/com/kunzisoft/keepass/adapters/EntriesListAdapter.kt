package com.kunzisoft.keepass.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeVersionedInterface
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.getLocalizedName
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.utils.ColorUtils.getColorFromAttr
import com.kunzisoft.keepass.utils.TimeUtil.getDateTimeString
import com.kunzisoft.keepass.view.setTextSize
import com.kunzisoft.keepass.view.strikeOut
import java.util.ArrayList
import java.util.LinkedList

class EntriesListAdapter(
    private val context: Context,
    private val database: ContextualDatabase
) : RecyclerView.Adapter<EntriesListAdapter.NodeViewHolder>() {

    private var mNodeComparator: Comparator<NodeVersionedInterface<Group>>? = null
    private val mNodeSortedListCallback: NodeSortedListCallback
    private val mNodeSortedList: SortedList<Entry>
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    private var mCalculateViewTypeTextSize = Array(2) { true } // number of view type
    private var mTextSizeUnit: Int = TypedValue.COMPLEX_UNIT_PX
    private var mPrefSizeMultiplier: Float = 0F
    private var mTextDefaultDimension: Float = 0F
    private var mSubTextDefaultDimension: Float = 0F
    private var mOtpTokenTextDefaultDimension: Float = 0F
    private var mIconDefaultDimension: Float = 0F

    private var mShowEntryColors: Boolean = true
    private var mShowNumberEntries: Boolean = true
    private var mShowOTP: Boolean = false
    private var mEntryFilters = arrayOf<Group.ChildFilter>()
    private var mShowRecycleBinItems: Boolean = false
    private var mTagFilter = ""

    private var mActionNodesList = LinkedList<Entry>()
    private var mNodeClickCallback: NodeClickCallback? = null
    private var mClipboardHelper = ClipboardHelper(context)

    private val LIST_ITEM_SUBTITLE_USERNAME_SETTING_VALUE =
        context.getString(R.string.list_item_subtitle_username_value)
    private val LIST_ITEM_SUBTITLE_UUID_SETTING_VALUE =
        context.getString(R.string.list_item_subtitle_uuid_value)
    private val LIST_ITEM_SUBTITLE_TAGS_SETTING_VALUE =
        context.getString(R.string.list_item_subtitle_tags_value)
    private val LIST_ITEM_SUBTITLE_DATE_CREATED_SETTING_VALUE =
        context.getString(R.string.list_item_subtitle_date_created_value)
    private val LIST_ITEM_SUBTITLE_DATE_MODIFIED_SETTING_VALUE =
        context.getString(R.string.list_item_subtitle_date_modified_value)
    private val LIST_ITEM_SUBTITLE_EXPIRY_SETTING_VALUE =
        context.getString(R.string.list_item_subtitle_expiry_value)
    private val LIST_ITEM_SUBTITLE_EXPIRY_BLANK_SETTING_VALUE =
        context.getString(R.string.list_item_subtitle_expiry_blank_value)
    private val LIST_ITEM_SUBTITLE_NONE_SETTING_VALUE =
        context.getString(R.string.list_item_subtitle_none_value)

    @ColorInt
    private val mColorBackground: Int

    @ColorInt
    private val mColorSecondary: Int

    @ColorInt
    private val mTextColor: Int

    @ColorInt
    private val mTextColorSecondary: Int

    @ColorInt
    private val mColorSurfaceBright: Int

    @ColorInt
    private val mColorOnSurface: Int

    /**
     * Determine if the adapter contains or not any element
     * @return true if the list is empty
     */
    val isEmpty: Boolean
        get() = mNodeSortedList.size() <= 0

    init {
        this.mIconDefaultDimension = context.resources.getDimension(R.dimen.list_icon_size_default)

        assignPreferences()

        this.mNodeSortedListCallback = NodeSortedListCallback()
        this.mNodeSortedList = SortedList(Entry::class.java, mNodeSortedListCallback)

        val nightMode =
            context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        mColorBackground = getColorFromAttr(context, R.attr.colorSurface, Color.BLACK)
        mColorSecondary = getColorFromAttr(context, R.attr.colorSecondary, Color.WHITE)
        mTextColor = getColorFromAttr(context, android.R.attr.textColor, Color.WHITE)
        mTextColorSecondary =
            getColorFromAttr(context, android.R.attr.textColorSecondary, Color.WHITE)

        //For selection
        mColorSurfaceBright =
            if (nightMode) getColorFromAttr(context, R.attr.colorSurfaceBright, Color.GRAY)
            else getColorFromAttr(context, R.attr.colorSurfaceDim, Color.GRAY)
        mColorOnSurface = getColorFromAttr(context, R.attr.colorOnSurface, Color.WHITE)
    }

    private fun assignPreferences() {
        this.mPrefSizeMultiplier = PreferencesUtil.getListTextSize(context)

        notifyChangeSort(
            PreferencesUtil.getEntriesListSort(context),
            SortNodeEnum.SortNodeParameters(
                PreferencesUtil.getAscendingEntriesSort(context),
                groupsBefore = false,
                recycleBinBottom = true
            ),
            PreferencesUtil.getRecycleBinEntriesSort(context)
        )

        this.mShowEntryColors = PreferencesUtil.showEntryColors(context)
        this.mShowNumberEntries = PreferencesUtil.showNumberEntries(context)
        this.mShowOTP = PreferencesUtil.showOTPToken(context)

        this.mEntryFilters = Group.ChildFilter.getDefaults(
            PreferencesUtil.showExpiredEntries(context)
        )

        // Reinit textSize for all view type
        mCalculateViewTypeTextSize.forEachIndexed { index, _ ->
            mCalculateViewTypeTextSize[index] = true
        }
    }

    fun rebuildList(list: List<Entry>) {
        assignPreferences()

        val recycleBinChildrenIdList = ArrayList<String?>()

        fun searchChild(node: Group?) {
            node ?: return
            val children = node.getChildren()

            children.forEach { node ->
                if (node is Entry) {
                    recycleBinChildrenIdList.add(node.nodeId.toVisualString())
                }

                if (node is Group) {
                    searchChild(node)
                }
            }
        }

        searchChild(database.recycleBin)

        mNodeSortedList.replaceAll(
            list
                .filter {
                    if (mTagFilter.isEmpty()) true
                    else it.tags.contains(mTagFilter)
                }
                .filter {
                    if (mShowRecycleBinItems) true
                    else !recycleBinChildrenIdList.contains(it.nodeId.toVisualString())
                })
    }

    private inner class NodeSortedListCallback : SortedListAdapterCallback<Entry>(this) {
        override fun compare(item1: Entry, item2: Entry): Int {
            return mNodeComparator!!.compare(item1, item2)
        }

        override fun areContentsTheSame(oldItem: Entry, newItem: Entry): Boolean {
            val typeContentTheSame = oldItem.getVisualTitle() == newItem.getVisualTitle()
                    && oldItem.username == newItem.username
                    && oldItem.backgroundColor == newItem.backgroundColor
                    && oldItem.foregroundColor == newItem.foregroundColor
                    && oldItem.getOtpElement() == newItem.getOtpElement()
                    && oldItem.containsAttachment() == newItem.containsAttachment()
            return typeContentTheSame
                    && oldItem.nodeId == newItem.nodeId
                    && oldItem.type == newItem.type
                    && oldItem.title == newItem.title
                    && oldItem.icon == newItem.icon
                    && oldItem.isCurrentlyExpires == newItem.isCurrentlyExpires
        }

        override fun areItemsTheSame(item1: Entry, item2: Entry): Boolean {
            return item1 == item2
        }
    }

    fun contains(node: Entry): Boolean {
        return mNodeSortedList.indexOf(node) != SortedList.INVALID_POSITION
    }

    fun indexOf(node: Entry): Int {
        return mNodeSortedList.indexOf(node)
    }

    fun notifyChangeSort(
        sortNodeEnum: SortNodeEnum,
        sortNodeParameters: SortNodeEnum.SortNodeParameters,
        showRecycleBinItems: Boolean
    ) {
        this.mNodeComparator = sortNodeEnum.getNodeComparator(database, sortNodeParameters)
        mShowRecycleBinItems = showRecycleBinItems
    }

    fun notifyCustomTagSelected(tag: String) {
        mTagFilter = tag
    }

    override fun getItemViewType(position: Int): Int {
        return mNodeSortedList.get(position).type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
        val view: View = mInflater.inflate(R.layout.item_list_nodes_entry, parent, false)

        val nodeViewHolder = NodeViewHolder(view)
        mTextDefaultDimension = nodeViewHolder.text.textSize
        mSubTextDefaultDimension = nodeViewHolder.subText.textSize
        mOtpTokenTextDefaultDimension =
            nodeViewHolder.otpToken?.textSize ?: mOtpTokenTextDefaultDimension
        return nodeViewHolder
    }

    override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
        val subNode = mNodeSortedList.get(position)

        // Node selection
        holder.container.apply {
            isSelected = mActionNodesList.contains(subNode)
        }
        holder.iconSelected.isVisible = holder.container.isSelected

        // Assign text
        holder.text.apply {
            text = subNode.title
            setTextSize(mTextSizeUnit, mTextDefaultDimension, mPrefSizeMultiplier)
            strikeOut(subNode.isCurrentlyExpires)
        }
        holder.subText.apply {
            setTextSize(mTextSizeUnit, mSubTextDefaultDimension, mPrefSizeMultiplier)
            strikeOut(subNode.isCurrentlyExpires)
        }

        // Assign icon colors
        var iconColor = if (holder.container.isSelected)
            mColorOnSurface
        else mColorSecondary

        val subtitleType = PreferencesUtil.getListItemSubtitle(context)

        // Specific elements for entry
        if (subNode.type == Type.ENTRY) {
            val entry = subNode as Entry
            database.startManageEntry(entry)

            holder.text.text = entry.getVisualTitle()
            holder.subText.apply {
                when (subtitleType) {
                    LIST_ITEM_SUBTITLE_USERNAME_SETTING_VALUE -> {
                        val username = entry.username
                        text = username
                        isVisible = username.isNotEmpty()
                    }

                    LIST_ITEM_SUBTITLE_UUID_SETTING_VALUE -> {
                        val nodeId = subNode.nodeId.toVisualString()
                        text = nodeId
                        isVisible = (nodeId != null) and (entry.getVisualTitle() != nodeId)
                    }

                    LIST_ITEM_SUBTITLE_TAGS_SETTING_VALUE -> {
                        val tagsList = database.getEntryById(subNode.nodeId)?.tags

                        text = tagsList?.toList()?.joinToString(", ")
                        isVisible = tagsList?.isNotEmpty() == true
                    }

                    else -> {
                        isVisible = false
                    }
                }
            }

            val otpElement = entry.getOtpElement()
            holder.otpContainer?.removeCallbacks(holder.otpRunnable)
            if (otpElement != null
                && mShowOTP
                && otpElement.token.isNotEmpty()
            ) {

                // Execute runnable to show progress
                holder.otpRunnable.action = {
                    populateOtpView(holder, otpElement)
                }
                if (otpElement.type == OtpType.TOTP) {
                    holder.otpRunnable.postDelayed()
                }
                populateOtpView(holder, otpElement)

                holder.otpContainer?.visibility = View.VISIBLE
            } else {
                holder.otpContainer?.visibility = View.GONE
            }
            holder.attachmentIcon?.visibility =
                if (entry.containsAttachment()) View.VISIBLE else View.GONE

            // Assign colors
            assignBackgroundColor(holder.container, entry)
            assignBackgroundColor(holder.otpContainer, entry)
            val foregroundColor = if (mShowEntryColors) entry.foregroundColor else null
            if (!holder.container.isSelected) {
                if (foregroundColor != null) {
                    holder.text.setTextColor(foregroundColor)
                    holder.subText.setTextColor(foregroundColor)
                    holder.otpToken?.setTextColor(foregroundColor)
                    holder.otpProgress?.setIndicatorColor(foregroundColor)
                    holder.attachmentIcon?.setColorFilter(foregroundColor)
                    iconColor = foregroundColor
                } else {
                    holder.text.setTextColor(mTextColor)
                    holder.subText.setTextColor(mTextColorSecondary)
                    holder.otpToken?.setTextColor(mTextColorSecondary)
                    holder.otpProgress?.setIndicatorColor(mTextColorSecondary)
                    holder.attachmentIcon?.setColorFilter(mTextColorSecondary)
                }
            } else {
                holder.text.setTextColor(mColorOnSurface)
                holder.subText.setTextColor(mColorOnSurface)
                holder.otpToken?.setTextColor(mColorOnSurface)
                holder.otpProgress?.setIndicatorColor(mColorOnSurface)
                holder.attachmentIcon?.setColorFilter(mColorOnSurface)
            }

            database.stopManageEntry(entry)
        }

        //set subtitle
        holder.subText.apply {
            when (subtitleType) {
                LIST_ITEM_SUBTITLE_DATE_CREATED_SETTING_VALUE -> {
                    text = subNode.creationTime.getDateTimeString(resources)
                    isVisible = true
                }

                LIST_ITEM_SUBTITLE_DATE_MODIFIED_SETTING_VALUE -> {
                    text = subNode.lastModificationTime.getDateTimeString(resources)
                    isVisible = true
                }

                LIST_ITEM_SUBTITLE_EXPIRY_SETTING_VALUE -> {
                    text = if (subNode.expires) subNode.expiryTime.getDateTimeString(resources)
                    else context.getString(R.string.never_expires)
                    isVisible = true
                }

                LIST_ITEM_SUBTITLE_EXPIRY_BLANK_SETTING_VALUE -> {
                    text = subNode.expiryTime.getDateTimeString(resources)
                    isVisible = subNode.expires
                }
            }
        }

        // Assign image
        holder.icon.apply {
            database.iconDrawableFactory.assignDatabaseIcon(this, subNode.icon, iconColor)
            // Relative size of the icon
            layoutParams?.apply {
                height = (mIconDefaultDimension * mPrefSizeMultiplier).toInt()
                width = (mIconDefaultDimension * mPrefSizeMultiplier).toInt()
            }
        }

        // Assign click
        holder.container.setOnClickListener {
            mNodeClickCallback?.onNodeClick(database, subNode)
        }
        holder.container.setOnLongClickListener {
            mNodeClickCallback?.onNodeLongClick(database, subNode) ?: false
        }
    }

    private fun populateOtpView(holder: NodeViewHolder?, otpElement: OtpElement?) {
        when (otpElement?.type) {
            OtpType.HOTP -> {
                holder?.otpProgress?.apply {
                    max = 100
                    setProgressCompat(100, true)
                }
            }

            OtpType.TOTP -> {
                holder?.otpProgress?.apply {
                    max = otpElement.period
                    setProgressCompat(otpElement.secondsRemaining, true)
                }
            }

            null -> {}
        }
        holder?.otpToken?.apply {
            text = otpElement?.tokenString
            setTextSize(mTextSizeUnit, mOtpTokenTextDefaultDimension, mPrefSizeMultiplier)
        }
        holder?.otpContainer?.setOnClickListener {
            otpElement?.token?.let { token ->
                try {
                    mClipboardHelper.copyToClipboard(
                        TemplateField.getLocalizedName(context, TemplateField.LABEL_TOKEN),
                        token,
                        true
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to copy the OTP token", e)
                }
            }
        }
    }

    private fun assignBackgroundColor(view: View?, entry: Entry?) {
        view?.let {
            ViewCompat.setBackgroundTintList(
                view,
                ColorStateList.valueOf(
                    if (!view.isSelected) {
                        (if (entry != null && mShowEntryColors) entry.backgroundColor else null)
                            ?: mColorBackground
                    } else {
                        mColorSurfaceBright
                    }
                )
            )
        }
    }

    class OtpRunnable(val view: View?) : Runnable {

        var action: (() -> Unit)? = null

        override fun run() {
            action?.invoke()
            postDelayed()
        }

        fun postDelayed() {
            view?.postDelayed(this, 1000)
        }
    }

    override fun getItemCount(): Int {
        return mNodeSortedList.size()
    }

    /**
     * Assign a listener when a node is clicked
     */
    fun setOnNodeClickListener(nodeClickCallback: NodeClickCallback?) {
        this.mNodeClickCallback = nodeClickCallback
    }

    /**
     * Callback listener to redefine to do an action when a node is click
     */
    interface NodeClickCallback {
        fun onNodeClick(database: ContextualDatabase, node: Node)
        fun onNodeLongClick(database: ContextualDatabase, node: Node): Boolean
    }

    class NodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var container: View = itemView.findViewById(R.id.node_container)
        var icon: ImageView = itemView.findViewById(R.id.node_icon)
        var iconSelected: ImageView = itemView.findViewById(R.id.node_selected_icon)
        var text: TextView = itemView.findViewById(R.id.node_text)
        var subText: TextView = itemView.findViewById(R.id.node_subtext)
        var otpContainer: ViewGroup? = itemView.findViewById(R.id.node_otp_container)
        var otpProgress: LinearProgressIndicator? = itemView.findViewById(R.id.node_otp_progress)
        var otpToken: TextView? = itemView.findViewById(R.id.node_otp_token)
        var otpRunnable: OtpRunnable = OtpRunnable(otpContainer)
        var attachmentIcon: ImageView? = itemView.findViewById(R.id.node_attachment_icon)
    }

    companion object {
        private val TAG = EntriesListAdapter::class.java.name
    }
}
