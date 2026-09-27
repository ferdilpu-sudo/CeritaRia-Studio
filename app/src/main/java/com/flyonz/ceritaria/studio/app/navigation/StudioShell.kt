package com.flyonz.ceritaria.studio.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flyonz.ceritaria.studio.feature.episode.detail.EpisodeDetailScreen
import com.flyonz.ceritaria.studio.feature.episode.list.EpisodeListScreen
import com.flyonz.ceritaria.studio.feature.home.HomeScreen
import com.flyonz.ceritaria.studio.feature.placeholder.PlaceholderScreen
import com.flyonz.ceritaria.studio.feature.series.detail.SeriesDetailScreen
import com.flyonz.ceritaria.studio.feature.series.list.SeriesListScreen

private const val SERIES_DETAIL_ROUTE = "series/{seriesId}"
private const val EPISODE_DETAIL_ROUTE = "episode/{episodeId}"

@Composable
fun StudioShell(
    userEmail: String?,
    onSignOut: () -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = StudioDestination.topLevel.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    StudioDestination.topLevel.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { DestinationIcon(destination) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = StudioDestination.Home.route,
        ) {
            composable(StudioDestination.Home.route) {
                HomeScreen(contentPadding = padding, userEmail = userEmail, onSignOut = onSignOut)
            }
            composable(StudioDestination.Series.route) {
                SeriesListScreen(
                    contentPadding = padding,
                    onSeriesClick = { id -> navController.navigate("series/" + id) },
                )
            }
            composable(SERIES_DETAIL_ROUTE) {
                SeriesDetailScreen(contentPadding = padding, onBack = navController::popBackStack)
            }
            composable(StudioDestination.Episodes.route) {
                EpisodeListScreen(
                    contentPadding = padding,
                    onEpisodeClick = { id -> navController.navigate("episode/" + id) },
                )
            }
            composable(EPISODE_DETAIL_ROUTE) {
                EpisodeDetailScreen(contentPadding = padding, onBack = navController::popBackStack)
            }
            composable(StudioDestination.Analytics.route) {
                PlaceholderScreen(contentPadding = padding, titleRes = StudioDestination.Analytics.labelRes)
            }
        }
    }
}

@Composable
private fun DestinationIcon(destination: StudioDestination) {
    val image = when (destination) {
        StudioDestination.Home -> Icons.Outlined.Home
        StudioDestination.Series -> Icons.Outlined.VideoLibrary
        StudioDestination.Episodes -> Icons.Outlined.Movie
        StudioDestination.Analytics -> Icons.Outlined.Analytics
    }
    Icon(imageVector = image, contentDescription = null)
}
