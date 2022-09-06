/*
 * Animation Garden App
 * Copyright (C) 2022  Him188
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.him188.animationgarden.app.app

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import me.him188.animationgarden.api.AnimationGardenClient
import me.him188.animationgarden.api.impl.model.KeyedMutableListFlow
import me.him188.animationgarden.api.impl.model.KeyedMutableListFlowImpl
import me.him188.animationgarden.api.impl.model.mutate
import me.him188.animationgarden.api.model.SearchQuery
import me.him188.animationgarden.api.model.SearchSession
import me.him188.animationgarden.api.model.Topic
import me.him188.animationgarden.api.tags.Episode
import me.him188.animationgarden.app.ui.OrganizedViewState
import me.him188.animationgarden.app.ui.updateStarredAnime
import java.io.File

@Stable
class ApplicationState(
    initialClient: AnimationGardenClient,
    workingDir: File,
) {
    @Stable
    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
        })


    @Stable
    val searchQuery: MutableState<SearchQuery> = mutableStateOf(SearchQuery())

    @Stable
    val topicsFlow: KeyedMutableListFlow<String, Topic> = KeyedMutableListFlowImpl { it.id }

    @Stable
    val client: MutableState<AnimationGardenClient> = mutableStateOf(initialClient)

    @Stable
    val session: MutableState<SearchSession> by lazy {
        mutableStateOf(client.value.startSearchSession(SearchQuery()))
    }


    // derived
    @Stable
    val topicsFlowState: State<StateFlow<List<Topic>>> = derivedStateOf { topicsFlow.asFlow() }

    @Stable
    val starredAnimeFlowState: State<StateFlow<List<StarredAnime>>> = derivedStateOf { data.starredAnime.asFlow() }


    @Stable
    val organizedViewState: OrganizedViewState = OrganizedViewState()

    private val dataFile = workingDir.resolve("data").apply { mkdirs() }.resolve("app.yml")

    @Stable
    val appDataSaver: AppDataSaver = AppDataSaver(dataFile).apply {
        reload()
    }

    @Stable
    val data: AppData
        get() = appDataSaver.data

    @Stable
    val fetcher: Fetcher =
        Fetcher(
            CoroutineScope(applicationScope.coroutineContext + SupervisorJob(applicationScope.coroutineContext.job)),
            onFetchSucceed = {
                // update starred anime on success
                searchQuery.value.keywords?.let { query ->
                    organizedViewState.let {
                        data.starredAnime.updateStarredAnime(query, currentOrganizedViewState = it)
                    }
                }
            }
        )

    fun updateSearchQuery(searchQuery: SearchQuery) {
        this.searchQuery.value = searchQuery
        fetcher.hasMorePages.value = true
        fetcher.fetchingState.value = FetchingState.Idle
        topicsFlow.value = listOf()
        session.value = client.value.startSearchSession(searchQuery)
        launchFetchNextPage(!searchQuery.keywords.isNullOrBlank())
    }

    fun launchFetchNextPage(
        continuous: Boolean,
    ) {
        fetcher.launchFetchNextPage(continuous, session.value, topicsFlow)
    }


    @Composable
    fun isEpisodeWatched(episode: Episode): Boolean {
        val starredAnime by remember {
            data.starredAnime.asFlow()
                .map { list -> list.find { it.searchQuery == searchQuery.value.keywords } }
        }.collectAsState(null)

        return starredAnime?.watchedEpisodes?.contains(episode) == true
    }

    fun onEpisodeDownloaded(episode: Episode) {
        data.starredAnime.mutate { list ->
            list.map { anime ->
                if (anime.searchQuery == searchQuery.value.keywords) {
                    anime.run { copy(watchedEpisodes = watchedEpisodes + episode) }
                } else {
                    anime
                }
            }
        }
    }
}

fun ApplicationState.doSearch(keywords: String?) {
    updateSearchQuery(SearchQuery(keywords?.trim(), null, null))
}


@Composable
fun ApplicationState.rememberCurrentStarredAnimeState(): State<StarredAnime?> {
    val currentStarredAnimeList by starredAnimeFlowState.value.collectAsState()
    val starredAnimeState = remember {
        derivedStateOf {
            currentStarredAnimeList.find { it.searchQuery == searchQuery.value.keywords }
        }
    }
    val currentStarredAnime by starredAnimeState

    val currentTopics by remember { topicsFlow.asFlow() }.collectAsState()
    val currentSearchQuery by searchQuery
    LaunchedEffect(currentStarredAnime, currentTopics, currentSearchQuery) {
        organizedViewState.apply {
            selectedAlliance.value = currentStarredAnime?.preferredAlliance
            selectedResolution.value = currentStarredAnime?.preferredResolution
            selectedSubtitleLanguage.value = currentStarredAnime?.preferredSubtitleLanguage

            setTopics(currentTopics, currentSearchQuery.keywords)
        }
    }
    return starredAnimeState
}