package com.kunzisoft.keepass.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import java.io.File


object UriUtil {

    fun Uri.getDocumentFile(context: Context): DocumentFile? {
        return try {
            when {
                this.withFileScheme() -> {
                    this.path?.let {
                        File(it).let { file ->
                            return DocumentFile.fromFile(file)
                        }
                    }
                }

                this.withContentScheme() -> {
                    DocumentFile.fromSingleUri(context, this)
                }

                else -> {
                    Log.e("FileData", "Content scheme not known")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to get document file", e)
            null
        }
    }

    private fun persistUriPermission(
        contentResolver: ContentResolver?,
        uri: Uri,
        release: Boolean,
        readOnly: Boolean
    ) {
        try {
            // try to persist read and write permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                contentResolver?.apply {
                    var readPermissionAllowed = false
                    var writePermissionAllowed = false
                    // Check current permissions allowed
                    persistedUriPermissions.find { uriPermission ->
                        uriPermission.uri == uri
                    }?.let { uriPermission ->
                        Log.d(TAG, "Check URI permission : $uriPermission")
                        if (uriPermission.isReadPermission) {
                            readPermissionAllowed = true
                        }
                        if (uriPermission.isWritePermission) {
                            writePermissionAllowed = true
                        }
                    }

                    // Release permission
                    if (release) {
                        if (writePermissionAllowed) {
                            Log.d(TAG, "Release write permission : $uri")
                            val removeFlags: Int = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            releasePersistableUriPermission(uri, removeFlags)
                        }
                        if (readPermissionAllowed) {
                            Log.d(TAG, "Release read permission $uri")
                            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            releasePersistableUriPermission(uri, takeFlags)
                        }
                    }

                    // Take missing permission
                    if (!readPermissionAllowed) {
                        Log.d(TAG, "Take read permission $uri")
                        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        takePersistableUriPermission(uri, takeFlags)
                    }
                    if (readOnly) {
                        if (writePermissionAllowed) {
                            Log.d(TAG, "Release write permission $uri")
                            val removeFlags: Int = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            releasePersistableUriPermission(uri, removeFlags)
                        }
                    } else {
                        if (!writePermissionAllowed) {
                            Log.d(TAG, "Take write permission $uri")
                            val takeFlags: Int = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            takePersistableUriPermission(uri, takeFlags)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (release)
                Log.e(TAG, "Unable to release persistable URI permission", e)
            else
                Log.e(TAG, "Unable to take persistable URI permission", e)
        }
    }

    fun ContentResolver.takeUriPermission(uri: Uri?, readOnly: Boolean = false) {
        uri?.let {
            persistUriPermission(this, it, false, readOnly)
        }
    }

    fun ContentResolver.releaseUriPermission(uri: Uri?) {
        uri?.let {
            persistUriPermission(this, it, release = true, readOnly = false)
        }
    }

    fun Context.releaseAllUnnecessaryPermissionUris() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            applicationContext?.let { appContext ->
                val fileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(appContext)
                fileDatabaseHistoryAction.getDatabaseFileList { databaseFileList ->
                    val listToNotRemove = mutableListOf<Uri>()
                    databaseFileList.forEach {
                        it.databaseUri?.let { databaseUri ->
                            listToNotRemove.add(databaseUri)
                        }
                        it.keyFileUri?.let { keyFileUri ->
                            listToNotRemove.add(keyFileUri)
                        }
                    }
                    // Remove URI permission for not database files
                    val resolver = appContext.contentResolver
                    resolver.persistedUriPermissions.forEach { uriPermission ->
                        val uri = uriPermission.uri
                        if (!listToNotRemove.contains(uri))
                            resolver.releaseUriPermission(uri)
                    }
                }
            }
        }
    }

    fun Intent.getUri(key: String): Uri? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                val clipData = this.clipData
                if (clipData != null) {
                    if (clipData.description.label == key) {
                        if (clipData.itemCount == 1) {
                            val clipItem = clipData.getItemAt(0)
                            if (clipItem != null) {
                                return clipItem.uri
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return this.getParcelableExtraCompat(key)
        }
        return null
    }

    fun Context.openUrl(url: String?) {
        try {
            if (!url.isNullOrEmpty()) {
                // Default http:// if no protocol specified
                val newUrl = if (!url.contains("://")) {
                    "http://$url"
                } else {
                    url
                }
                this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(newUrl)))
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.no_url_handler, Toast.LENGTH_LONG).show()
        }
    }

    fun Context.openUrl(resId: Int) {
        this.openUrl(this.getString(resId))
    }

    fun Context.isContributingUser(): Boolean {
//        return (Education.isEducationScreenReclickedPerformed(this)
//                || isExternalAppInstalled(this.getString(R.string.keepro_app_id), false)
//        )
        return true
    }

    fun Context.isExternalAppInstalled(packageName: String, showError: Boolean = true): Boolean {
        try {
            this.applicationContext.packageManager.getPackageInfoCompat(
                packageName,
                PackageManager.GET_ACTIVITIES
            )
            return true
        } catch (e: Exception) {
            if (showError)
                Log.e(TAG, "App not accessible", e)
        }
        return false
    }

    fun Context.openExternalApp(packageName: String, sourcesURL: String? = null) {
        var launchIntent: Intent? = null
        try {
            launchIntent = this.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (ignored: Exception) {
        }
        try {
            if (launchIntent == null) {
                this.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setData(Uri.parse(this.getString(R.string.play_store_url, packageName)))
                )
            } else {
                this.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "App cannot be open", e)
        }
    }

    fun getPathFromUri(context: Context, uri: Uri): String? {
        var filePath: String? = null
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if ("com.android.externalstorage.documents" == uri.authority) {
                // External Storage Provider
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    filePath =
                        Environment.getExternalStorageDirectory().toString() + "/" + Uri.decode(
                            split[1]
                        )
                }
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // MediaStore (and general)
            filePath = getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            // File
            filePath = uri.path
        }
        return filePath
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        val contentResolver = context.contentResolver
        val projection = arrayOf("_data")
        try {
            contentResolver.query(uri, projection, selection, selectionArgs, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow("_data")
                    return cursor.getString(columnIndex)
                }
            }
        } catch (e: java.lang.Exception) {
            Log.e("UriUtils", "Error getting data column for URI $uri", e)
        }
        return null
    }

    private const val TAG = "UriUtil"
}
