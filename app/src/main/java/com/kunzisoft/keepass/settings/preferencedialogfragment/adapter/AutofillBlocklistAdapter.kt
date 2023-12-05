package com.kunzisoft.keepass.settings.preferencedialogfragment.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.ObjectNameResource

import java.util.ArrayList

class AutofillBlocklistAdapter<T : ObjectNameResource>(private val context: Context)
    : RecyclerView.Adapter<AutofillBlocklistAdapter.BlocklistItemViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    val items: MutableList<T> = ArrayList()

    private var itemDeletedCallback: ItemDeletedCallback<T>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlocklistItemViewHolder {
        val view = inflater.inflate(R.layout.pref_dialog_list_removable_item, parent, false)
        return BlocklistItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: BlocklistItemViewHolder, position: Int) {
        val item = this.items[position]
        holder.textItem.text = item.getName(context.resources)
        holder.deleteButton.setOnClickListener(OnItemDeleteClickListener(item))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun replaceItems(items: List<T>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    private inner class OnItemDeleteClickListener(private val itemClicked: T) : View.OnClickListener {

        override fun onClick(view: View) {
            itemDeletedCallback?.onItemDeleted(itemClicked)
            notifyDataSetChanged()
        }
    }

    fun setItemDeletedCallback(itemDeletedCallback: ItemDeletedCallback<T>) {
        this.itemDeletedCallback = itemDeletedCallback
    }

    interface ItemDeletedCallback<T> {
        fun onItemDeleted(item: T)
    }

    class BlocklistItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textItem: TextView = itemView.findViewById(R.id.pref_dialog_list_text)
        var deleteButton: ImageView = itemView.findViewById(R.id.pref_dialog_list_delete_button)
    }
}
