package com.kunzisoft.keepass.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.igreenwood.loupe.Loupe
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.fragments.EntryFragment
import com.kunzisoft.keepass.activities.fragments.EntryFragment.Companion.EXTRA_ACTION_DOWNLOAD
import com.kunzisoft.keepass.activities.fragments.EntryFragment.Companion.EXTRA_ATTACHMENT_TAG
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.tasks.BinaryDatabaseManager
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import kotlin.math.max

class ImageViewerActivity : DatabaseLockActivity() {

    private var imageContainerView: ViewGroup? = null
    private lateinit var imageView: ImageView
    private lateinit var progressView: View
    private val titleView: TextView by lazy { findViewById(R.id.title_view) }
    private val subtitleView: TextView by lazy { findViewById(R.id.subtitle_view) }

    private var attachment: Attachment? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_viewer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setOnTouchListener { _, _ ->
            resetAppTimeout()
            false
        }

        imageContainerView = findViewById(R.id.image_viewer_container)
        imageView = findViewById(R.id.image_viewer_image)
        progressView = findViewById(R.id.image_viewer_progress)

        Loupe.create(imageView, imageContainerView!!) {
            onViewTouchedListener = View.OnTouchListener { _, _ ->
                // to reset timeout when Loupe image view touched
                resetAppTimeout()
                false
            }
            onViewTranslateListener = object : Loupe.OnViewTranslateListener {

                override fun onStart(view: ImageView) {
                    // called when the view starts moving
                }

                override fun onViewTranslate(view: ImageView, amount: Float) {
                    // called whenever the view position changed
                }

                override fun onRestore(view: ImageView) {
                    // called when the view drag gesture ended
                }

                override fun onDismiss(view: ImageView) {
                    // called when the view drag gesture ended
                    finish()
                }
            }
            useDragToDismiss = false
            useFlingToDismissGesture = false
        }
    }

    override fun viewToInvalidateTimeout(): View? {
        // Null to manually manage events
        return null
    }

    override fun finishActivityIfReloadRequested(): Boolean {
        return true
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)

        try {
            progressView.visibility = View.VISIBLE
            intent.getParcelableExtraCompat<Attachment>(EXTRA_ATTACHMENT_TAG)?.let { attachment ->
                this.attachment = attachment
                titleView.text = attachment.name

                val size = attachment.binaryData.getSize()
                subtitleView.text = Formatter.formatFileSize(this, size)

                // Approximately, to not OOM and allow a zoom
                val mImagePreviewMaxWidth = max(
                    resources.displayMetrics.widthPixels * 2,
                    resources.displayMetrics.heightPixels * 2
                )

                database?.let { database ->
                    BinaryDatabaseManager.loadBitmap(
                        database,
                        attachment.binaryData,
                        mImagePreviewMaxWidth
                    ) { bitmapLoaded ->
                        if (bitmapLoaded == null) {
                            finish()
                        } else {
                            progressView.visibility = View.GONE
                            imageView.setImageBitmap(bitmapLoaded)
                        }
                    }
                }
            } ?: run {
                setResult(
                    Activity.RESULT_CANCELED,
                    Intent().putExtra(EntryFragment.EXTRA_PREVIEW_FAILED_TAG, true)
                )
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to view the binary", e)
            finish()
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

    companion object {
        private val TAG = ImageViewerActivity::class.simpleName
    }
}