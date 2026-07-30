package com.soren.airpodscompanion

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat

internal const val ICLOUD_FIND_URL = "https://www.icloud.com/find/"

object ICloudFindLauncher {
    private val findUri = Uri.parse(ICLOUD_FIND_URL)

    fun open(context: Context): Boolean {
        val browserPackage = CustomTabsClient.getPackageName(context, null) ?: return false
        val toolbarColor = ContextCompat.getColor(context, R.color.icloud_tab_toolbar)
        val navigationBarColor = ContextCompat.getColor(context, R.color.icloud_tab_navigation)
        val colorParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(toolbarColor)
            .setNavigationBarColor(navigationBarColor)
            .build()
        val customTab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(false)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_LIGHT)
            .setDefaultColorSchemeParams(colorParams)
            .build()

        customTab.intent.setPackage(browserPackage)
        return runCatching {
            customTab.launchUrl(context, findUri)
            true
        }.getOrDefault(false)
    }
}
