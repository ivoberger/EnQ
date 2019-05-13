/*
* Copyright 2019 Ivo Berger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ivoberger.enq.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.ivoberger.enq.MainDirections
import com.ivoberger.enq.R
import com.ivoberger.enq.model.ServerInfo
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.ui.MainActivity
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
class ServerDiscoveryDialog : DialogFragment() {

    private var isDiscovering = true
    private var triedLatestServer = false
    private lateinit var mDiscoveringViews: List<View?>
    private lateinit var mRetryViews: List<View?>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("Showing server discovery dialog")
        val dialog = context?.alertDialog {
            titleResource = R.string.tlt_server_discovery
            isCancelable = false
            setView(R.layout.dialog_progress_spinner)
            positiveButton(R.string.btn_retry) {}
        }
        dialog?.onShow {
            positiveButton.onClick { lifecycleScope.launch { attemptDiscovery() } }
            positiveButton.setTextColor(context.secondaryColor())

            mDiscoveringViews = listOf(findViewById(R.id.discovery_progress))
            mRetryViews = listOf(positiveButton)

            if (isDiscovering) lifecycleScope.launch { attemptDiscovery() }
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

    private suspend fun attemptDiscovery() {
        dialog?.apply {
            setTitle(R.string.tlt_server_discovery)
            mDiscoveringViews.forEach { it?.visibility = View.VISIBLE }
            mRetryViews.forEach { it?.visibility = View.GONE }
        }
        JMusicBot.discoverHost(if (!triedLatestServer) AppSettings.getLatestServer()?.baseUrl else null)
        JMusicBot.state.running?.join()
        // check if saved url works
        val versionInfo = try {
            JMusicBot.getVersionInfo()
        } catch (e: Exception) {
            Timber.w(e)
            if (!triedLatestServer) {
                JMusicBot.discoverHost()
                JMusicBot.state.running?.join()
                triedLatestServer = true
            }
            null
        }
        if (JMusicBot.state.hasServer) {
            (activity as MainActivity).navController.navigate(MainDirections.actionGlobalLoginDialog(true))
            Timber.d("Found server")
            activity?.lifecycleScope?.launch {
                try {
                    val serverInfo = ServerInfo(
                        JMusicBot.baseUrl!!, versionInfo
                            ?: JMusicBot.getVersionInfo()
                    )
                    AppSettings.addServer(serverInfo)
                    Timber.d("Added server $serverInfo")
                } catch (e: Exception) {
                    Timber.w(e)
                }
            }
            dismiss()
        } else showRetryOption()
    }
}
