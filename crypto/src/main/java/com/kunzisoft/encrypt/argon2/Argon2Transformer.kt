package com.kunzisoft.encrypt.argon2

import com.kunzisoft.encrypt.NativeLib

object Argon2Transformer {

    fun transformKey(type: Argon2Type,
                     password: ByteArray,
                     salt: ByteArray,
                     parallelism: Long,
                     memory: Long,
                     iterations: Long,
                     version: Int): ByteArray {

        NativeLib.init()
        val argon2Type = when (type) {
            Argon2Type.ARGON2_I -> com.kunzisoft.encrypt.argon2.NativeArgon2KeyTransformer.CType.ARGON2_I
            Argon2Type.ARGON2_D -> com.kunzisoft.encrypt.argon2.NativeArgon2KeyTransformer.CType.ARGON2_D
            Argon2Type.ARGON2_ID -> com.kunzisoft.encrypt.argon2.NativeArgon2KeyTransformer.CType.ARGON2_ID
        }

        return com.kunzisoft.encrypt.argon2.NativeArgon2KeyTransformer.nTransformKey(
            argon2Type.cValue,
            password,
            salt,
            parallelism.toInt(),
            memory.toInt(),
            iterations.toInt(),
            ByteArray(0),
            ByteArray(0),
            version
        )
    }
}