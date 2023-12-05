package com.kunzisoft.keepass.viewmodels

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import androidx.documentfile.provider.DocumentFile
import com.kunzisoft.keepass.utils.parseUri
import com.kunzisoft.keepass.utils.UriUtil.getDocumentFile
import com.kunzisoft.keepass.utils.UriUtil.takeUriPermission
import java.io.Serializable
import java.text.DateFormat
import java.util.*

class FileDatabaseInfo : Serializable {

    private var context: Context
    private var documentFile: DocumentFile? = null
    var fileUri: Uri?
        private set

    constructor(context: Context, fileUri: Uri) {
        this.context = context
        this.fileUri = fileUri
        init()
    }

    constructor(context: Context, filePath: String) {
        this.context = context
        this.fileUri = filePath.parseUri()
        init()
    }

    fun init() {
        // Check permission
        context.contentResolver.takeUriPermission(fileUri)
        documentFile = fileUri?.getDocumentFile(context)
    }

    var exists: Boolean = false
        get() {
            return documentFile?.exists() ?: field
        }
        private set

    fun getLastModification(): Long? {
        return documentFile?.lastModified()
    }

    fun getLastModificationString(): String? {
        return documentFile?.lastModified()?.let {
            if (it != 0L) {
                DateFormat.getDateTimeInstance()
                        .format(Date(it))
            } else {
                null
            }
        }
    }

    fun getSize(): Long? {
        return documentFile?.length()
    }

    fun getSizeString(): String? {
        return documentFile?.let {
            Formatter.formatFileSize(context, it.length())
        }
    }

    fun retrieveDatabaseAlias(alias: String): String? {
        return when {
            alias.isNotEmpty() -> alias
            else -> if (exists) documentFile?.name else fileUri?.path
        }
    }
}
