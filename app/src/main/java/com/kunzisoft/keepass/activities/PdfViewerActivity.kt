package com.kunzisoft.keepass.activities

import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import java.io.IOException

class PdfViewerActivity : DatabaseLockActivity() {

    private val mainContainer: ViewGroup by lazy { findViewById(R.id.container) }
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

        pdfView.minZoom = 1.5f
        pdfView.setNightMode(true)
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
                        Toast.makeText(this@PdfViewerActivity, "Tap", Toast.LENGTH_SHORT).show()
                        true
                    }
                    .scrollHandle(DefaultScrollHandle(this))
                    .spacing(3)
                    .load()
            }
        } catch (e: IOException) {
            Log.e("PDF-TEST", "catch: ", e)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onDatabaseBackPressed()
        }
        return true
    }

    override fun viewToInvalidateTimeout(): View {
        return mainContainer
    }

    companion object {
        const val EXTRA_ATTACHMENT = "extra_attachment"
    }
}