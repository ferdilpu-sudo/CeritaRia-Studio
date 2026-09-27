package com.flyonz.ceritaria.studio.app.navigation

import androidx.annotation.StringRes
import com.flyonz.ceritaria.studio.R

sealed class StudioDestination(
    val route: String,
    @StringRes val labelRes: Int,
) {
    data object Home : StudioDestination("home", R.string.home)
    data object Series : StudioDestination("series", R.string.series)
    data object Episodes : StudioDestination("episodes", R.string.episodes)
    data object Analytics : StudioDestination("analytics", R.string.analytics)

    companion object {
        val topLevel = listOf(Home, Series, Episodes, Analytics)
    }
}
