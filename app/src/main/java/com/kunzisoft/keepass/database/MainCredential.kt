package com.kunzisoft.keepass.database

import android.content.ContentResolver
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.getUriInputStream
import com.kunzisoft.keepass.utils.readEnum
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.writeEnum

data class MainCredential(
    var password: String? = null,
    var keyFileUri: Uri? = null,
    var hardwareKey: HardwareKey? = null
) : Parcelable {

    constructor(parcel: Parcel) : this() {
        password = parcel.readString()
        keyFileUri = parcel.readParcelableCompat()
        hardwareKey = parcel.readEnum<HardwareKey>()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(password)
        parcel.writeParcelable(keyFileUri, flags)
        parcel.writeEnum(hardwareKey)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MainCredential

        if (password != other.password) return false
        if (keyFileUri != other.keyFileUri) return false
        if (hardwareKey != other.hardwareKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = password?.hashCode() ?: 0
        result = 31 * result + (keyFileUri?.hashCode() ?: 0)
        result = 31 * result + (hardwareKey?.hashCode() ?: 0)
        return result
    }

    fun toMasterCredential(contentResolver: ContentResolver): MasterCredential {
        return MasterCredential(
            this.password,
            this.keyFileUri?.let {
                getKeyFileData(contentResolver, it)
            },
            this.hardwareKey
        )
    }

    private fun getKeyFileData(
        contentResolver: ContentResolver,
        keyFileUri: Uri
    ): ByteArray? {
        contentResolver.getUriInputStream(keyFileUri)?.use { keyFileInputStream ->
            return keyFileInputStream.readBytes()
        }
        return null
    }

    companion object CREATOR : Parcelable.Creator<MainCredential> {
        override fun createFromParcel(parcel: Parcel): MainCredential {
            return MainCredential(parcel)
        }

        override fun newArray(size: Int): Array<MainCredential?> {
            return arrayOfNulls(size)
        }

        private val TAG = MainCredential::class.java.simpleName
    }
}
