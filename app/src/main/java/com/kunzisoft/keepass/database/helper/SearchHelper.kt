package com.kunzisoft.keepass.database.helper

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.timeout.TimeoutHelper

object SearchHelper {

    const val MAX_SEARCH_ENTRY = 1000

    /**
     * Method to show the number of search results with max results
     */
    fun showNumberOfSearchResults(number: Int): String {
        return if (number >= MAX_SEARCH_ENTRY) {
            (MAX_SEARCH_ENTRY -1).toString() + "+"
        } else {
            number.toString()
        }
    }

    /**
     * Utility method to perform actions if item is found or not after an auto search in [database]
     */
    fun checkAutoSearchInfo(context: Context,
                            database: ContextualDatabase?,
                            searchInfo: SearchInfo?,
                            onItemsFound: (openedDatabase: ContextualDatabase,
                                           items: List<EntryInfo>) -> Unit,
                            onItemNotFound: (openedDatabase: ContextualDatabase) -> Unit,
                            onDatabaseClosed: () -> Unit) {
        if (database == null || !database.loaded) {
            onDatabaseClosed.invoke()
        } else if (TimeoutHelper.checkTime(context)) {
            var searchWithoutUI = false
            if (searchInfo != null
                && !searchInfo.manualSelection
                && !searchInfo.containsOnlyNullValues()) {
                // If search provide results
                database.createVirtualGroupFromSearchInfo(
                        searchInfo.toString(),
                        MAX_SEARCH_ENTRY
                )?.let { searchGroup ->
                    if (searchGroup.numberOfChildEntries > 0) {
                        searchWithoutUI = true
                        onItemsFound.invoke(database,
                                searchGroup.getChildEntriesInfo(database))
                    }
                }
            }
            if (!searchWithoutUI) {
                onItemNotFound.invoke(database)
            }
        }
    }
}
