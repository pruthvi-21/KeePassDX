package com.kunzisoft.keepass.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
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
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import java.io.IOException

class PdfViewerActivity : DatabaseLockActivity() {

    private val pdfView: PDFView by lazy { findViewById(R.id.pdfView) }

    private var attachment: Attachment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        attachment = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ATTACHMENT, Attachment::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_ATTACHMENT)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val titleView = findViewById<TextView>(R.id.title_view)
        val subTitleView = findViewById<TextView>(R.id.subtitle_view)
        setSupportActionBar(toolbar)

        if (attachment == null) {
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
                    .onTap {
                        true
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
                        .putExtra(RESULT_DOWNLOAD, true)
                        .putExtra(EXTRA_ATTACHMENT, attachment)
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

        const val EXTRA_ATTACHMENT = "extra_attachment"
        const val RESULT_DOWNLOAD = "download_attachment"
    }
}