package com.dinaraparanid.prima.mvvmp.old_shit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.view.dialogs.PrimaReleaseDialogFragment
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.web.github.GitHubFetcher
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.main_menu.about_app.AboutAppFragment]
 */

class AboutAppViewModel(private val activity: WeakReference<Activity>) : BasePresenter() {
    /**
     * Sends intent to open
     * developer's profile on Github
     */

    @JvmName("sendGithubIntent")
    internal fun sendGithubIntent() = activity.unchecked.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/dinaraparanid")
        )
    )

    /**
     * Sends intent to open
     * developer's profile on Twitter
     */

    @JvmName("sendTwitterIntent")
    @Deprecated("Don't use Twitter anymore")
    internal fun sendTwitterIntent() = activity.unchecked.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://twitter.com/paranid5")
        )
    )

    /**
     * Sends intent to open
     * developer's profile on Telegram
     */

    @JvmName("sendTelegramIntent")
    internal fun sendTelegramIntent() = activity.unchecked.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://t.me/Paranid5")
        )
    )

    /**
     * Sends intent to
     * developer's email
     */

    @JvmName("sendEmailIntent")
    internal fun sendEmailIntent() = activity.unchecked.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND)
                .setType("plain/text")
                .putExtra(Intent.EXTRA_EMAIL, arrayOf("dinaraparanid@gmail.com")),
            activity.unchecked.resources.getString(R.string.send_email)
        )
    )

    @JvmName("sendVKIntent")
    internal fun sendVKIntent() = activity.unchecked.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://vk.com/paranid5")
        )
    )

    @JvmName("showCurrentVersionInfo")
    internal fun showCurrentVersionInfo() = activity.unchecked.run {
        GitHubFetcher().fetchLatestRelease().observe(this as MainActivity) { release ->
            PrimaReleaseDialogFragment(release, this, PrimaReleaseDialogFragment.Target.CURRENT).show()
        }
    }
}