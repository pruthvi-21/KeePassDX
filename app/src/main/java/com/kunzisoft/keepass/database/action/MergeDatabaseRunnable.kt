package com.kunzisoft.keepass.database.action

import android.content.Context
import android.net.Uri
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.exception.DatabaseException
import com.kunzisoft.keepass.database.exception.UnknownDatabaseLocationException
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.getUriInputStream

class MergeDatabaseRunnable(
    context: Context,
    private val mDatabaseToMergeUri: Uri?,
    private val mDatabaseToMergeMainCredential: MainCredential?,
    private val mDatabaseToMergeChallengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    database: ContextualDatabase,
    saveDatabase: Boolean,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    private val progressTaskUpdater: ProgressTaskUpdater?,
) : SaveDatabaseRunnable(
    context,
    database,
    saveDatabase,
    null,
    challengeResponseRetriever
) {
    override fun onStartRun() {
        database.wasReloaded = true
        super.onStartRun()
    }

    override fun onActionRun() {
        try {
            val contentResolver = context.contentResolver
            database.mergeData(
                context.contentResolver.getUriInputStream(
                    mDatabaseToMergeUri ?: database.fileUri
                ) ?: throw UnknownDatabaseLocationException(),
                mDatabaseToMergeMainCredential?.toMasterCredential(contentResolver),
                mDatabaseToMergeChallengeResponseRetriever,
                { memoryWanted ->
                    BinaryData.canMemoryBeAllocatedInRAM(context, memoryWanted)
                },
                progressTaskUpdater
            )
        } catch (e: DatabaseException) {
            setError(e)
        }

        super.onActionRun()
    }
}
