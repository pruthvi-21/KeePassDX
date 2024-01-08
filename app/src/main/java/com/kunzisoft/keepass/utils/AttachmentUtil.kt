package com.kunzisoft.keepass.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.utils.AttachmentUtil.ATTACHMENTS_CACHE_DIR
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object AttachmentUtil {
    const val ATTACHMENTS_CACHE_DIR = "attachments"
    const val FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider"

    fun previewAttachment(
        context: Context,
        database: ContextualDatabase,
        attachment: Attachment
    ) {
        try {
            val tempFile = File(context.getAttachmentsCacheDir(), attachment.name)

            val unGzipInputStream = attachment.binaryData
                .getUnGzipInputDataStream(database.binaryCache)

            FileOutputStream(tempFile).use { outStream ->
                unGzipInputStream.copyTo(outStream)
            }

            val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, tempFile)

            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(tempFile.absolutePath)
            val mimeType =
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, R.string.no_compatible_app_found, Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("TAG", "catch: exception occurred", e)
        }
    }
}

fun Context.getAttachmentsCacheDir(createIfNotExists: Boolean = true): File {
    val attachmentsDir = File(cacheDir, ATTACHMENTS_CACHE_DIR)
    if (!attachmentsDir.exists() && createIfNotExists) {
        attachmentsDir.mkdirs()
    }
    return attachmentsDir
}

fun Context.deleteAttachmentsFromCache() {
    val attachmentsDir = getAttachmentsCacheDir(false)
    val attachmentFiles = attachmentsDir.listFiles()

    if (attachmentsDir.exists() && attachmentFiles != null) {
        attachmentsDir.deleteRecursively()
    }
}