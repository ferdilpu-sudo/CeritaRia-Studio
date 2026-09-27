package com.flyonz.ceritaria.studio.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.flyonz.ceritaria.studio.feature.episode.list.EpisodeListScreen
import com.flyonz.ceritaria.studio.feature.series.detail.SeriesDetailScreen
import com.flyonz.ceritaria.studio.feature.series.editor.SeriesEditorScreen
import com.flyonz.ceritaria.studio.feature.series.list.SeriesListScreen

fun NavGraphBuilder.seriesNavGraph(
    navController: NavHostController,
    contentPadding: PaddingValues,
) {
    composable(StudioDestination.Series.route) {
        SeriesListScreen(
            contentPadding = contentPadding,
            onSeriesClick = { navController.navigate(StudioRoutes.seriesDetail(it)) },
            onCreateSeries = { navController.navigate(StudioRoutes.SERIES_EDITOR_NEW) },
        )
    }
    composable(StudioRoutes.SERIES_DETAIL) {
        SeriesDetailScreen(
            contentPadding = contentPadding,
            onBack = navController::popBackStack,
            onEpisodesClick = { navController.navigate(StudioRoutes.seriesEpisodes(it)) },
            onEditClick = { navController.navigate(StudioRoutes.seriesEditor(it)) },
            onDeleted = {
                navController.popBackStack(StudioDestination.Series.route, inclusive = false)
            },
        )
    }
    composable(StudioRoutes.SERIES_EDITOR_NEW) {
        SeriesEditorScreen(
            contentPadding = contentPadding,
            onBack = navController::popBackStack,
            onSaved = { navigateToFreshSeriesDetail(navController, it) },
        )
    }
    composable(StudioRoutes.SERIES_EDITOR_EDIT) {
        SeriesEditorScreen(
            contentPadding = contentPadding,
            onBack = navController::popBackStack,
            onSaved = { navigateToFreshSeriesDetail(navController, it) },
        )
    }
    composable(StudioRoutes.SERIES_EPISODES) {
        EpisodeListScreen(
            contentPadding = contentPadding,
            onEpisodeClick = { navController.navigate(StudioRoutes.episodeDetail(it)) },
            onCreateEpisode = { seriesId ->
                val route = seriesId?.let(StudioRoutes::episodeEditorForSeries)
                    ?: StudioRoutes.EPISODE_EDITOR_NEW
                navController.navigate(route)
            },
        )
    }
}

private fun navigateToFreshSeriesDetail(
    navController: NavHostController,
    seriesId: String,
) {
    navController.navigate(StudioRoutes.seriesDetail(seriesId)) {
        popUpTo(StudioDestination.Series.route)
        launchSingleTop = true
    }
}
