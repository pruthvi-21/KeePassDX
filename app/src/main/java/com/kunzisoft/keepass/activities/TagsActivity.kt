package com.kunzisoft.keepass.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.database.ContextualDatabase

class TagsActivity : DatabaseLockActivity() {

    private lateinit var rootLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        rootLayout = findViewById(R.id.tags_activity_container)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {

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
