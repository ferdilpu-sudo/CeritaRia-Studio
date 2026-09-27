package com.flyonz.ceritaria.studio.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.flyonz.ceritaria.studio.feature.episode.detail.EpisodeDetailScreen
import com.flyonz.ceritaria.studio.feature.episode.editor.EpisodeEditorScreen
import com.flyonz.ceritaria.studio.feature.episode.list.EpisodeListScreen

fun NavGraphBuilder.episodeNavGraph(
    navController: NavHostController,
    contentPadding: PaddingValues,
) {
    composable(StudioDestination.Episodes.route) {
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
    composable(StudioRoutes.EPISODE_DETAIL) {
        EpisodeDetailScreen(
            contentPadding = contentPadding,
            onBack = navController::popBackStack,
            onEditClick = { navController.navigate(StudioRoutes.episodeEditor(it)) },
            onDeleted = { navController.popBackStack() },
        )
    }
    composable(StudioRoutes.EPISODE_EDITOR_NEW) {
        EpisodeEditorScreen(
            contentPadding = contentPadding,
            onBack = navController::popBackStack,
            onSaved = { finishEpisodeCreate(navController, it) },
        )
    }
    composable(StudioRoutes.EPISODE_EDITOR_SERIES_NEW) {
        EpisodeEditorScreen(
            contentPadding = contentPadding,
            onBack = navController::popBackStack,
            onSaved = { finishEpisodeCreate(navController, it) },
        )
    }
    composable(StudioRoutes.EPISODE_EDITOR_EDIT) {
        EpisodeEditorScreen(
            contentPadding = contentPadding,
            onBack = navController::popBackStack,
            onSaved = { finishEpisodeEdit(navController, it) },
        )
    }
}

private fun finishEpisodeCreate(
    navController: NavHostController,
    episodeId: String,
) {
    navController.popBackStack()
    navController.navigate(StudioRoutes.episodeDetail(episodeId))
}

private fun finishEpisodeEdit(
    navController: NavHostController,
    episodeId: String,
) {
    navController.popBackStack()
    navController.popBackStack()
    navController.navigate(StudioRoutes.episodeDetail(episodeId))
}
