package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R

/**
 * Custom Dialog that asks the user to download the pro version or make a donation.
 */
class UnderDevelopmentFeatureDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(activity)
            builder.setView(R.layout.alert_dialog_layout)

            val stringBuilder = SpannableStringBuilder()
            /*
            if (activity.isContributingUser()) {
                stringBuilder.append(HtmlCompat.fromHtml(getString(R.string.html_text_dev_feature_thanks), HtmlCompat.FROM_HTML_MODE_LEGACY)).append("\n\n")
                        .append(HtmlCompat.fromHtml(getString(R.string.html_rose), HtmlCompat.FROM_HTML_MODE_LEGACY)).append("\n\n")
                        .append(HtmlCompat.fromHtml(getString(R.string.html_text_dev_feature_work_hard), HtmlCompat.FROM_HTML_MODE_LEGACY)).append("\n")
                        .append(HtmlCompat.fromHtml(getString(R.string.html_text_dev_feature_upgrade), HtmlCompat.FROM_HTML_MODE_LEGACY)).append(" ")
                builder.setPositiveButton(android.R.string.ok) { _, _ -> dismiss() }
            } else {
             */
            stringBuilder.append(
                HtmlCompat.fromHtml(
                    getString(R.string.html_text_dev_feature),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            ).append("\n\n")
                .append(
                    HtmlCompat.fromHtml(
                        getString(R.string.html_text_dev_feature_contibute),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                ).append(" ")
                .append(
                    HtmlCompat.fromHtml(
                        getString(R.string.html_text_dev_feature_encourage),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                )
            //}
            builder.setMessage(stringBuilder)
            // Create the AlertDialog object and return it
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }
}
