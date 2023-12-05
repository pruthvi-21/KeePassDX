package com.kunzisoft.keepass.database.action

import android.content.Context
import android.net.Uri
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.getBinaryDir

class CreateDatabaseRunnable(
    context: Context,
    private val mDatabase: ContextualDatabase,
    private val databaseUri: Uri,
    private val databaseName: String,
    private val rootName: String,
    private val templateGroupName: String?,
    val mainCredential: MainCredential,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : SaveDatabaseRunnable(
    context,
    mDatabase,
    true,
    mainCredential,
    challengeResponseRetriever
) {
    override fun onStartRun() {
        try {
            // Create new database record
            mDatabase.apply {
                this.fileUri = databaseUri
                createData(databaseName, rootName, templateGroupName)
            }
        } catch (e: Exception) {
            mDatabase.clearAndClose(context.getBinaryDir())
            setError(e)
        }

        super.onStartRun()
    }

    override fun onFinishRun() {
        if (result.isSuccess) {
            mDatabase.loaded = true
        }
        super.onFinishRun()
    }
}
