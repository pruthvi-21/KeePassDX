package com.kunzisoft.keepass.tests.utils

import com.kunzisoft.keepass.utils.UuidUtil
import junit.framework.TestCase
import java.util.*

class UUIDTest : TestCase() {

    fun testUUID() {
        val randomUUID = UUID.randomUUID()
        val hexStringUUID = com.kunzisoft.keepass.utils.UuidUtil.toHexString(randomUUID)
        val retrievedUUID = com.kunzisoft.keepass.utils.UuidUtil.fromHexString(hexStringUUID)
        assertEquals(randomUUID, retrievedUUID)
    }
}