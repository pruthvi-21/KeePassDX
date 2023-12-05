package com.kunzisoft.keepass.database.action

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.exception.DatabaseException
import com.kunzisoft.keepass.database.exception.UnknownDatabaseLocationException
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.getBinaryDir
import com.kunzisoft.keepass.utils.getUriInputStream

class ReloadDatabaseRunnable(
    private val context: Context,
    private val mDatabase: ContextualDatabase,
    private val progressTaskUpdater: ProgressTaskUpdater?
) : ActionRunnable() {

    var afterReloadDatabase : ((Result) -> Unit)? = null

    private val binaryDir = context.getBinaryDir()

    override fun onStartRun() {
        // Clear before we load
        mDatabase.clearIndexesAndBinaries(binaryDir)
        mDatabase.wasReloaded = true
    }

    override fun onActionRun() {
        try {
            mDatabase.reloadData(
                context.contentResolver.getUriInputStream(mDatabase.fileUri)
                    ?: throw UnknownDatabaseLocationException(),
                { memoryWanted ->
                    BinaryData.canMemoryBeAllocatedInRAM(context, memoryWanted)
                },
                progressTaskUpdater)
        } catch (e: DatabaseException) {
            setError(e)
        }

        if (!result.isSuccess) {
            mDatabase.clearAndClose(binaryDir)
        }
    }

    override fun onFinishRun() {
        afterReloadDatabase?.invoke(result)
    }
}
