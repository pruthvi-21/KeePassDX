package com.kunzisoft.keepass.database.action.history

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.action.SaveDatabaseRunnable
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.hardware.HardwareKey

class DeleteEntryHistoryDatabaseRunnable (
    context: Context,
    database: ContextualDatabase,
    private val mainEntry: Entry,
    private val entryHistoryPosition: Int,
    saveDatabase: Boolean,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : SaveDatabaseRunnable(context, database, saveDatabase, null, challengeResponseRetriever) {

    override fun onStartRun() {
        try {
            database.removeEntryHistory(mainEntry, entryHistoryPosition)
        } catch (e: Exception) {
            setError(e)
        }

        super.onStartRun()
    }
}
