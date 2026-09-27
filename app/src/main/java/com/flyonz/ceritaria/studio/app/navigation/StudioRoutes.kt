package com.flyonz.ceritaria.studio.app.navigation

object StudioRoutes {
    const val SERIES_DETAIL = "series/{seriesId}"
    const val SERIES_EPISODES = "series/{seriesId}/episodes"
    const val SERIES_REORDER = "series/{seriesId}/reorder"
    const val SERIES_EDITOR_NEW = "series-editor/new"
    const val SERIES_EDITOR_EDIT = "series-editor/{seriesId}"

    const val EPISODE_DETAIL = "episode/{episodeId}"
    const val EPISODE_EDITOR_NEW = "episode-editor/create"
    const val EPISODE_EDITOR_SERIES_NEW = "episode-editor/create/{seriesId}"
    const val EPISODE_EDITOR_EDIT = "episode-editor/edit/{episodeId}"

    fun seriesDetail(id: String) = "series/$id"
    fun seriesEpisodes(id: String) = "series/$id/episodes"
    fun seriesReorder(id: String) = "series/$id/reorder"
    fun seriesEditor(id: String) = "series-editor/$id"

    fun episodeDetail(id: String) = "episode/$id"
    fun episodeEditor(id: String) = "episode-editor/edit/$id"
    fun episodeEditorForSeries(seriesId: String) = "episode-editor/create/$seriesId"
}
