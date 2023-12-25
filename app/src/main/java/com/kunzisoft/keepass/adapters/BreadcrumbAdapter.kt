package com.kunzisoft.keepass.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.ColorUtils
import com.kunzisoft.keepass.view.strikeOut

class BreadcrumbAdapter(val context: Context) :
    RecyclerView.Adapter<BreadcrumbAdapter.BreadcrumbGroupViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    var iconDrawableFactory: IconDrawableFactory? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    private var mNodeBreadcrumb: MutableList<Node?> = mutableListOf()
    var onItemClickListener: ((item: Node, position: Int) -> Unit)? = null
    var onLongItemClickListener: ((item: Node, position: Int) -> Unit)? = null

    private var mShowNumberEntries = false
    private var mIconColor: Int = 0

    private val colorPrimary =
        ColorUtils.getColorFromAttr(context, R.attr.colorPrimary, Color.WHITE)
    private val colorTextSecondary =
        ColorUtils.getColorFromAttr(context, android.R.attr.textColorSecondary, Color.WHITE)

    init {
        mShowNumberEntries = PreferencesUtil.showNumberEntries(context)

        // Retrieve the color to tint the icon
        val taIconColor = context.theme.obtainStyledAttributes(intArrayOf(R.attr.colorOnSurface))
        mIconColor = taIconColor.getColor(0, Color.WHITE)
        taIconColor.recycle()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setNode(node: Node?) {
        mNodeBreadcrumb.clear()
        node?.let {
            var currentNode = it
            mNodeBreadcrumb.add(0, currentNode)
            while (currentNode.containsParent()) {
                currentNode.parent?.let { parent ->
                    currentNode = parent
                    mNodeBreadcrumb.add(0, currentNode)
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreadcrumbGroupViewHolder {
        return BreadcrumbGroupViewHolder(
            inflater.inflate(R.layout.item_breadcrumb, parent, false)
        )
    }

    override fun onBindViewHolder(holder: BreadcrumbGroupViewHolder, position: Int) {
        val node = mNodeBreadcrumb[position]

        holder.groupNameView.apply {
            text = node?.title ?: ""
            strikeOut(node?.isCurrentlyExpires ?: false)
            setTextColor(
                if (position == mNodeBreadcrumb.size - 1 && position != 0)
                    colorPrimary else colorTextSecondary
            )
        }

        holder.groupHomeIcon.isVisible = position == 0
        holder.groupSeparator.isVisible = position != 0

        holder.itemView.apply {
            setOnClickListener {
                node?.let {
                    onItemClickListener?.invoke(it, position)
                }
            }
            setOnLongClickListener {
                node?.let {
                    onLongItemClickListener?.invoke(it, position)
                }
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return mNodeBreadcrumb.size
    }

    inner class BreadcrumbGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var groupHomeIcon: ImageView = itemView.findViewById(R.id.group_home_icon)
        var groupNameView: TextView = itemView.findViewById(R.id.group_name)
        var groupSeparator: ImageView = itemView.findViewById(R.id.group_separator)
    }
}