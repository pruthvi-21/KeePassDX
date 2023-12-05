package com.kunzisoft.keepass.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Class to invoke action in a separate IO thread
 */
class IOActionTask<T>(
    private val action: () -> T,
    private val afterActionListener: ((T?) -> Unit)? = null
) {

    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun execute() {
        mainScope.launch {
            withContext(Dispatchers.IO) {
                val asyncResult: Deferred<T?> = async {
                    try {
                        action.invoke()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                withContext(Dispatchers.Main) {
                    afterActionListener?.invoke(asyncResult.await())
                }
            }
        }
    }
}