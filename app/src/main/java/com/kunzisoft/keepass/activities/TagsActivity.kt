package com.kunzisoft.keepass.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.adapters.TagsListAdapter
import com.kunzisoft.keepass.database.ContextualDatabase

class TagsActivity : DatabaseLockActivity() {

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        rootLayout = findViewById(R.id.tags_activity_container)
        recyclerView = findViewById(R.id.tags_list)
        recyclerView.layoutManager = LinearLayoutManager(this@TagsActivity)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {

        database?.let {
            val tagsList = ArrayList<TagsListAdapter.TagsListItemModel>()
            database.tagPool.toList().forEach { tag ->
                val entries = database.getEntriesByTag(tag)
                val groups = database.getGroupsByTag(tag)

                tagsList.add(
                    TagsListAdapter.TagsListItemModel(
                        tag,
                        entries.count() + groups.count()
                    )
                )
            }

            val adapter = TagsListAdapter(this, tagsList)
            recyclerView.adapter = adapter
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onDatabaseBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun viewToInvalidateTimeout(): View {
        return rootLayout
    }

    companion object {
        fun launch(context: Context) {
            val intent = Intent(context, TagsActivity::class.java)
            context.startActivity(intent)
        }

        private val TAG = TagsActivity::class.java.name
    }
}
