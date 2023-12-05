package com.kunzisoft.keepass.database.action

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.hardware.HardwareKey

class UpdateCompressionBinariesDatabaseRunnable (
    context: Context,
    database: ContextualDatabase,
    private val oldCompressionAlgorithm: CompressionAlgorithm,
    private val newCompressionAlgorithm: CompressionAlgorithm,
    saveDatabase: Boolean,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : SaveDatabaseRunnable(
    context,
    database,
    saveDatabase,
    null,
    challengeResponseRetriever
) {

    override fun onStartRun() {
        // Set new compression
        if (database.allowDataCompression) {
            try {
                database.apply {
                    updateDataBinaryCompression(oldCompressionAlgorithm, newCompressionAlgorithm)
                    compressionAlgorithm = newCompressionAlgorithm
                }
            } catch (e: Exception) {
                setError(e)
            }
        }

        super.onStartRun()
    }

    override fun onFinishRun() {
        super.onFinishRun()

        if (database.allowDataCompression) {
            if (!result.isSuccess) {
                try {
                    database.apply {
                        compressionAlgorithm = oldCompressionAlgorithm
                        updateDataBinaryCompression(newCompressionAlgorithm, oldCompressionAlgorithm)
                    }
                } catch (e: Exception) {
                    setError(e)
                }
            }
        }
    }
}
