package com.kunzisoft.keepass.services

import android.content.Intent
import androidx.core.app.ServiceCompat
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.LockReceiver
import com.kunzisoft.keepass.utils.registerLockReceiver
import com.kunzisoft.keepass.utils.unregisterLockReceiver

abstract class LockNotificationService : NotificationService() {

    private var mLockReceiver: LockReceiver = LockReceiver {
        actionOnLock()
    }

    protected open fun actionOnLock() {
        // Stop the service in all cases
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        // Register a lock receiver to stop notification service when lock on keyboard is performed
        registerLockReceiver(mLockReceiver)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!TimeoutHelper.temporarilyDisableLock) {
            actionOnLock()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        unregisterLockReceiver(mLockReceiver)
        super.onDestroy()
    }
}