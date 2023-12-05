package com.kunzisoft.keepass.database.action.node

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.action.SaveDatabaseRunnable
import com.kunzisoft.keepass.hardware.HardwareKey

abstract class ActionNodeDatabaseRunnable(
    context: Context,
    database: ContextualDatabase,
    private val afterActionNodesFinish: AfterActionNodesFinish?,
    save: Boolean,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : SaveDatabaseRunnable(context, database, save, null, challengeResponseRetriever) {

    /**
     * Function do to a node action
     */
    abstract fun nodeAction()

    override fun onStartRun() {
        try {
            nodeAction()
        } catch (e: Exception) {
            setError(e)
        }
        super.onStartRun()
    }

    /**
     * Function do get the finish node action
     */
    abstract fun nodeFinish(): ActionNodesValues

    override fun onFinishRun() {
        super.onFinishRun()
        afterActionNodesFinish?.apply {
            onActionNodesFinish(result, nodeFinish())
        }
    }
}
