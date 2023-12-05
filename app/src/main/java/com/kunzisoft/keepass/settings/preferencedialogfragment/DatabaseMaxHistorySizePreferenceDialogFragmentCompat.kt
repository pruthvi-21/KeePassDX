package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.view.View
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.utils.DataByte

class DatabaseMaxHistorySizePreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    private var dataByte = DataByte(2L, DataByte.ByteFormat.MEBIBYTE)

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        setExplanationText(R.string.max_history_size_summary)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        database?.historyMaxSize?.let { maxItemsDatabase ->
            dataByte = DataByte(maxItemsDatabase, DataByte.ByteFormat.BYTE)
                .toBetterByteFormat()
            inputText = dataByte.number.toString()
            if (dataByte.number >= 0) {
                setUnitText(dataByte.format.stringId)
            } else {
                unitText = null
            }

            setSwitchAction({ isChecked ->
                if (!isChecked) {
                    dataByte = INFINITE_MAX_HISTORY_SIZE_DATA_BYTE
                    inputText = INFINITE_MAX_HISTORY_SIZE.toString()
                    unitText = null
                } else {
                    dataByte = DEFAULT_MAX_HISTORY_SIZE_DATA_BYTE
                    inputText = dataByte.number.toString()
                    setUnitText(dataByte.format.stringId)
                }
                showInputText(isChecked)
            }, maxItemsDatabase > INFINITE_MAX_HISTORY_SIZE)
        }
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        if (positiveResult) {
            database?.let {
                val maxHistorySize: Long = try {
                    inputText.toLong()
                } catch (e: NumberFormatException) {
                    DEFAULT_MAX_HISTORY_SIZE_DATA_BYTE.toBytes()
                }
                val numberOfBytes = if (maxHistorySize >= 0) {
                    val dataByteConversion = DataByte(maxHistorySize, dataByte.format)
                    var bytes = dataByteConversion.toBytes()
                    if (bytes > Long.MAX_VALUE) {
                        bytes = Long.MAX_VALUE
                    }
                    bytes
                } else {
                    INFINITE_MAX_HISTORY_SIZE
                }

                val oldMaxHistorySize = database.historyMaxSize
                database.historyMaxSize = numberOfBytes

                saveMaxHistorySize(oldMaxHistorySize, numberOfBytes)
            }
        }
    }

    companion object {

        const val INFINITE_MAX_HISTORY_SIZE = -1L

        private val INFINITE_MAX_HISTORY_SIZE_DATA_BYTE = DataByte(INFINITE_MAX_HISTORY_SIZE, DataByte.ByteFormat.MEBIBYTE)
        private val DEFAULT_MAX_HISTORY_SIZE_DATA_BYTE = DataByte(6L, DataByte.ByteFormat.MEBIBYTE)

        fun newInstance(key: String): DatabaseMaxHistorySizePreferenceDialogFragmentCompat {
            val fragment = DatabaseMaxHistorySizePreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
