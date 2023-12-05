package com.kunzisoft.keepass.database.action

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.hardware.HardwareKey

class RemoveUnlinkedDataDatabaseRunnable (
    context: Context,
    database: ContextualDatabase,
    saveDatabase: Boolean,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : SaveDatabaseRunnable(
    context,
    database,
    saveDatabase,
    null,
    challengeResponseRetriever
) {

    override fun onActionRun() {
        try {
            database.removeUnlinkedAttachments()
        } catch (e: Exception) {
            setError(e)
        }

        super.onActionRun()
    }
}
