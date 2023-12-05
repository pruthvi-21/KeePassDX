package com.kunzisoft.keepass.utils

import android.content.Context
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R

class DataByte(var number: Long, var format: ByteFormat) {

    fun toBetterByteFormat(): DataByte {
        return when (this.format) {
            ByteFormat.BYTE -> {
                when {
                    //this.number % GIBIBYTES == 0L -> {
                    //    DataByte((this.number / GIBIBYTES), ByteFormat.GIBIBYTE)
                    //}
                    this.number % MEBIBYTES == 0L -> {
                        DataByte((this.number / MEBIBYTES), ByteFormat.MEBIBYTE)
                    }
                    this.number % KIBIBYTES == 0L -> {
                        DataByte((this.number / KIBIBYTES), ByteFormat.KIBIBYTE)
                    }
                    else -> {
                        DataByte(this.number, ByteFormat.BYTE)
                    }
                }
            }
            else -> {
                DataByte(toBytes(), ByteFormat.BYTE).toBetterByteFormat()
            }
        }
    }

    /**
     * Number of bytes in current DataByte
     */
    fun toBytes(): Long {
        return when (this.format) {
            ByteFormat.BYTE -> this.number
            ByteFormat.KIBIBYTE -> this.number * KIBIBYTES
            ByteFormat.MEBIBYTE -> this.number * MEBIBYTES
            //ByteFormat.GIBIBYTE -> this.number * GIBIBYTES
        }
    }

    override fun toString(): String {
        return "$number ${format.name}"
    }

    fun toString(context: Context): String {
        return "$number ${context.getString(format.stringId)}"
    }

    enum class ByteFormat(@StringRes var stringId: Int) {
        BYTE(R.string.unit_byte),
        KIBIBYTE(R.string.unit_kibibyte),
        MEBIBYTE(R.string.unit_mebibyte)
        //GIBIBYTE(R.string.unit_gibibyte)
    }

    companion object {
        const val KIBIBYTES = 1024L
        const val MEBIBYTES = 1048576L
        const val GIBIBYTES = 1073741824L
    }
}