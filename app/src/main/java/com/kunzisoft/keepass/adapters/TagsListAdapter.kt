package com.kunzisoft.keepass.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.setTextSize


class TagsListAdapter(
    private val context: Context,
    private val tags: List<TagsListItemModel>
) : RecyclerView.Adapter<TagsListAdapter.TagsListViewHolder>() {

    private var mTextSizeUnit: Int = TypedValue.COMPLEX_UNIT_PX
    private var mPrefSizeMultiplier: Float = 0F
    private var mTextDefaultDimension: Float = 0F
    private var mSubTextDefaultDimension: Float = 0F
    private var mIconDefaultDimension: Float = 0F

    var callback: TagItemClickListener? = null

    init {
        this.mPrefSizeMultiplier = PreferencesUtil.getListTextSize(context)
        this.mIconDefaultDimension = context.resources.getDimension(R.dimen.list_icon_size_default)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagsListViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_list_nodes_group, parent, false)
        return TagsListViewHolder(view).apply {
            mTextDefaultDimension = label.textSize
            mSubTextDefaultDimension = subText.textSize
        }
    }

    override fun onBindViewHolder(holder: TagsListViewHolder, position: Int) {
        val tagItem = tags[position]

        holder.label.apply {
            text = tagItem.label
            setTextSize(mTextSizeUnit, mTextDefaultDimension, mPrefSizeMultiplier)
        }
        holder.subText.apply {
            text = when (tagItem.count) {
                1 -> context.getString(R.string.tags_count_one)
                else -> context.getString(R.string.tags_count_multiple, tagItem.count)
            }
            setTextSize(mTextSizeUnit, mSubTextDefaultDimension, mPrefSizeMultiplier)
        }

        holder.icon.apply {
            setImageResource(R.drawable.ic_tag_white_24dp)
            imageTintList =
                ColorStateList.valueOf(MaterialColors.getColor(this, R.attr.colorSecondary))

            layoutParams?.apply {
                height = (mIconDefaultDimension * mPrefSizeMultiplier).toInt()
                width = (mIconDefaultDimension * mPrefSizeMultiplier).toInt()
            }
        }

        // Assign click
        holder.container.setOnClickListener {
            callback?.onTagClick(tagItem.label)
        }
    }

    override fun getItemCount() = tags.size

    interface TagItemClickListener {
        fun onTagClick(tag: String)
    }

    class TagsListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var container: View = itemView.findViewById(R.id.node_container)
        var icon: ImageView = itemView.findViewById(R.id.node_icon)
        var label: TextView = itemView.findViewById(R.id.node_text)
        var subText: TextView = itemView.findViewById(R.id.node_subtext)
    }

    data class TagsListItemModel(
        val label: String,
        val count: Int
    )

    companion object {
        private val TAG = TagsListAdapter::class.java.name
    }
}
