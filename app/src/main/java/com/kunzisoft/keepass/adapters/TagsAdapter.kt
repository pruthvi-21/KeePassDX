package com.kunzisoft.keepass.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Tags

class TagsAdapter(context: Context) : RecyclerView.Adapter<TagsAdapter.TagViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var mTags: Tags = Tags()
    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = inflater.inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val field = mTags.get(position)
        holder.name.text = field
        holder.bind(field, onItemClickListener)
    }

    override fun getItemCount(): Int {
        return mTags.size()
    }

    fun setTags(tags: Tags) {
        mTags.setTags(tags)
        notifyDataSetChanged()
    }

    fun clear() {
        mTags.clear()
    }

    interface OnItemClickListener {
        fun onItemClick(item: String)
    }

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var name: TextView = itemView.findViewById(R.id.tag_name)

        fun bind(item: String, listener: OnItemClickListener?) {
            itemView.setOnClickListener { listener?.onItemClick(item) }
        }
    }
}