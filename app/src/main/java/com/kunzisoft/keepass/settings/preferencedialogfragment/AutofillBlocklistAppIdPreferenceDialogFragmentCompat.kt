package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil

class AutofillBlocklistAppIdPreferenceDialogFragmentCompat
    : AutofillBlocklistPreferenceDialogFragmentCompat() {

    override fun buildSearchInfoFromString(searchInfoString: String): SearchInfo? {
        val newSearchInfo = searchInfoString
                // remove chars not allowed in application ID
                .replace(Regex("[^a-zA-Z0-9_.]+"), "")
        return SearchInfo().apply { this.applicationId = newSearchInfo }
    }

    override fun getDefaultValues(): Set<String> {
        return PreferencesUtil.getDefaultApplicationIdBlocklist(this.resources)
    }

    companion object {
        fun newInstance(key: String): AutofillBlocklistAppIdPreferenceDialogFragmentCompat {
            val fragment = AutofillBlocklistAppIdPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
