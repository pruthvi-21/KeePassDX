package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.view.View
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.utils.DataByte

class DatabaseMemoryUsagePreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    private var dataByte = DataByte(MIN_MEMORY_USAGE, DataByte.ByteFormat.BYTE)

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        setExplanationText(R.string.memory_usage_explanation)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        database?.let {
            val memoryBytes = database.memoryUsage
            dataByte = DataByte(memoryBytes, DataByte.ByteFormat.BYTE)
                .toBetterByteFormat()
            inputText = dataByte.number.toString()
            setUnitText(dataByte.format.stringId)
        }
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        if (positiveResult) {
            database?.let {
                var newMemoryUsage: Long = try {
                    inputText.toLong()
                } catch (e: NumberFormatException) {
                    MIN_MEMORY_USAGE
                }
                if (newMemoryUsage < MIN_MEMORY_USAGE) {
                    newMemoryUsage = MIN_MEMORY_USAGE
                }
                // To transform in bytes
                dataByte.number = newMemoryUsage
                var numberOfBytes = dataByte.toBytes()
                if (numberOfBytes > Long.MAX_VALUE) {
                    numberOfBytes = Long.MAX_VALUE
                }

                val oldMemoryUsage = database.memoryUsage
                database.memoryUsage = numberOfBytes

                saveMemoryUsage(oldMemoryUsage, numberOfBytes)
            }
        }
    }

    companion object {

        const val MIN_MEMORY_USAGE = 1L

        fun newInstance(key: String): DatabaseMemoryUsagePreferenceDialogFragmentCompat {
            val fragment = DatabaseMemoryUsagePreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
