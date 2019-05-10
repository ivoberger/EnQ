package com.ivoberger.enq.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.utils.secondaryColor
import com.ivoberger.jmusicbot.JMusicBot
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.onShow
import splitties.alertdialog.appcompat.positiveButton
import splitties.alertdialog.appcompat.titleResource
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.views.onClick
import timber.log.Timber

@ExperimentalSplittiesApi
@PotentialFutureAndroidXLifecycleKtxApi
class ServerDiscoveryDialog(private var discovering: Boolean, private val onServerFound: () -> Unit) :
    DialogFragment() {
    private lateinit var mDiscoveringViews: List<View?>
    private lateinit var mRetryViews: List<View?>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("Showing login dialog")
        val dialog = context?.alertDialog {
            titleResource = R.string.tlt_server_discovery
            isCancelable = false
            setView(R.layout.dialog_progress_spinner)
            positiveButton(R.string.btn_retry) {}
        }
        dialog?.onShow {
            positiveButton.onClick { attemptDiscovery() }
            positiveButton.setTextColor(context.secondaryColor())

            mDiscoveringViews = listOf(findViewById(R.id.discovery_progress))
            mRetryViews = listOf(positiveButton)

            if (discovering) activity?.lifecycleScope?.launch { attemptDiscovery() }
            else showRetryOption()
        }

        return dialog ?: throw IllegalStateException("Context cannot be null")
    }

    private fun showRetryOption() {
        dialog?.apply {
            setTitle(R.string.tlt_no_server)
            mRetryViews.forEach { it?.visibility = View.VISIBLE }
            mDiscoveringViews.forEach { it?.visibility = View.GONE }
        }
    }

    private fun attemptDiscovery() = activity?.lifecycleScope?.launch {
        dialog?.apply {
            setTitle(R.string.tlt_server_discovery)
            mDiscoveringViews.forEach { it?.visibility = View.VISIBLE }
            mRetryViews.forEach { it?.visibility = View.GONE }
        }
        JMusicBot.discoverHost(AppSettings.getLatestServer()?.baseUrl)
        JMusicBot.state.running?.join()
        // check if saved url works
        try {
            JMusicBot.getVersionInfo()
        } catch (e: Exception) {
            Timber.w(e)
        }
        if (JMusicBot.state.hasServer) {
            dialog?.dismiss()
            onServerFound()
        } else showRetryOption()
    }
}