package com.kunzisoft.keepass.database.action

import android.content.Context
import android.net.Uri
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.exception.DatabaseException
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.getUriOutputStream
import java.io.File

open class SaveDatabaseRunnable(
    protected var context: Context,
    protected var database: ContextualDatabase,
    private var saveDatabase: Boolean,
    private var mainCredential: MainCredential?, // If null, uses composite Key
    private var challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    private var databaseCopyUri: Uri? = null
) : ActionRunnable() {

    var afterSaveDatabase: ((Result) -> Unit)? = null

    override fun onStartRun() {}

    override fun onActionRun() {
        database.checkVersion()
        // Save database in all cases if it's a copy
        if ((databaseCopyUri != null || saveDatabase) && result.isSuccess) {
            try {
                val contentResolver = context.contentResolver
                // Build temp database file to avoid file corruption if error
                database.saveData(
                    cacheFile = File(context.cacheDir, databaseCopyUri.hashCode().toString()),
                    databaseOutputStream = contentResolver
                        .getUriOutputStream(databaseCopyUri ?: database.fileUri),
                    isNewLocation = databaseCopyUri == null,
                    mainCredential?.toMasterCredential(contentResolver),
                    challengeResponseRetriever)
            } catch (e: DatabaseException) {
                setError(e)
            }
        }
    }

    override fun onFinishRun() {
        // Need to call super.onFinishRun() in child class
        afterSaveDatabase?.invoke(result)
    }
}
