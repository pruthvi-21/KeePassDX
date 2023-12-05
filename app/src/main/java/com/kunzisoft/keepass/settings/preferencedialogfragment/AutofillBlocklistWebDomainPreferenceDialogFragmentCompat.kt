package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.SearchInfo

class AutofillBlocklistWebDomainPreferenceDialogFragmentCompat
    : AutofillBlocklistPreferenceDialogFragmentCompat() {

    override fun buildSearchInfoFromString(searchInfoString: String): SearchInfo? {
        val newSearchInfo = searchInfoString
                // remove prefix https://
                .replace(Regex("^.*://"), "")
                // Remove suffix /login...
                .replace(Regex("/.*$"), "")
        return SearchInfo().apply { webDomain = newSearchInfo }
    }

    override fun getDefaultValues(): Set<String> {
        return context?.resources
                ?.getStringArray(R.array.autofill_web_domain_blocklist_default)
                ?.toMutableSet()
                ?: emptySet()
    }

    companion object {
        fun newInstance(key: String): AutofillBlocklistWebDomainPreferenceDialogFragmentCompat {
            val fragment = AutofillBlocklistWebDomainPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
