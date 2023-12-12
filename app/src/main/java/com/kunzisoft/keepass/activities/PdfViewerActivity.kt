package com.kunzisoft.keepass.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.fragments.EntryFragment.Companion.EXTRA_ACTION_DOWNLOAD
import com.kunzisoft.keepass.activities.fragments.EntryFragment.Companion.EXTRA_ATTACHMENT_TAG
import com.kunzisoft.keepass.activities.fragments.EntryFragment.Companion.EXTRA_PREVIEW_FAILED_TAG
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import java.io.IOException

class PdfViewerActivity : DatabaseLockActivity() {

    private val pdfView: PDFView by lazy { findViewById(R.id.pdfView) }

    private var attachment: Attachment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        attachment = intent.getParcelableExtraCompat<Attachment>(EXTRA_ATTACHMENT_TAG)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val titleView = findViewById<TextView>(R.id.title_view)
        val subTitleView = findViewById<TextView>(R.id.subtitle_view)
        setSupportActionBar(toolbar)

        if (attachment == null) {
            setResult(
                Activity.RESULT_CANCELED,
                Intent().putExtra(EXTRA_PREVIEW_FAILED_TAG, true)
            )
            finish()
        }

        titleView.text = attachment!!.name
        subTitleView.text = Formatter.formatFileSize(this, attachment!!.binaryData.getSize())
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)

        if (attachment == null) return
        try {
            if (mDatabase?.binaryCache != null) {
                val unGzipInputStream = attachment!!.binaryData.getUnGzipInputDataStream(
                    mDatabase!!.binaryCache
                )

                pdfView
                    .fromStream(unGzipInputStream)
                    .enableSwipe(true)
                    .enableDoubletap(true)
                    .onError {
                        setResult(
                            Activity.RESULT_CANCELED,
                            Intent().putExtra(EXTRA_PREVIEW_FAILED_TAG, true)
                        )
                        finish()
                    }
                    .scrollHandle(DefaultScrollHandle(this))
                    .spacing(3)
                    .load()
            }
        } catch (e: IOException) {
            Log.e(TAG, "catch: exception occurred", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.attachment_preview, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onDatabaseBackPressed()
            R.id.menu_save_attachment -> {
                setResult(
                    Activity.RESULT_OK, Intent()
                        .putExtra(EXTRA_ACTION_DOWNLOAD, true)
                        .putExtra(EXTRA_ATTACHMENT_TAG, attachment)
                )
                finish()
            }
        }
        return true
    }

    override fun viewToInvalidateTimeout(): View? {
        // Null to manually manage events
        return null
    }

    companion object {
        private const val TAG = "PdfViewerActivity"
    }
}