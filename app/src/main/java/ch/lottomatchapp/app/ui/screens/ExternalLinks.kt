package ch.lottomatchapp.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import ch.lottomatchapp.app.R

internal const val PRIVACY_POLICY_URL = "https://github.com/aegeriappstudio/lottomatchapp/wiki/Datenschutz-Lottomatch-App"
internal const val TERMS_URL = "https://github.com/aegeriappstudio/lottomatchapp/wiki/Nutzungsbedingungen-Lottomatch-App"

internal fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

internal fun sendFeedbackEmail(context: Context) {
    val feedbackEmail = context.getString(R.string.feedback_email_address)
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(feedbackEmail))
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_email_subject))
    }
    val chooser = Intent.createChooser(intent, context.getString(R.string.feedback_email_chooser))
    context.startActivity(chooser)
}
