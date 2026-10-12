package com.dr.tech.puretube.features.search

import com.dr.tech.puretube.core.data.model.SearchFilter
import com.dr.tech.puretube.core.data.model.SearchResultItem
import com.dr.tech.puretube.core.data.repository.SearchRepository
import com.dr.tech.puretube.core.database.dao.SearchHistoryDao
import com.dr.tech.puretube.core.database.entity.SearchHistoryEntity
import com.dr.tech.puretube.features.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private class FakeSearchRepository : SearchRepository {
        val searchedQueries = mutableListOf<Pair<String, SearchFilter>>()
        var delayFor: (String) -> Long = { 0L }
        var nextToken: String? = "tok-1"

        private fun itemsFor(query: String, filter: SearchFilter): List<SearchResultItem> {
            val video = SearchResultItem.VideoItem(
                videoId = "vid-$query",
                title = "Title $query",
                channelId = "ch1",
                channelName = "Channel 1"
            )
            val channel = SearchResultItem.ChannelItem(
                channelId = "ch-$query",
                channelName = "Channel $query"
            )
            return when (filter) {
                SearchFilter.VIDEOS -> listOf(video)
                SearchFilter.CHANNELS -> listOf(channel)
                SearchFilter.ALL -> listOf(video, channel)
            }
        }

        override suspend fun search(
            query: String,
            filter: SearchFilter
        ): Result<Pair<List<SearchResultItem>, String?>> {
            searchedQueries.add(query to filter)
            val wait = delayFor(query)
            if (wait > 0) delay(wait)
            return Result.success(itemsFor(query, filter) to nextToken)
        }

        override suspend fun getNextPage(pageUrl: String): Result<Pair<List<SearchResultItem>, String?>> {
            val video = SearchResultItem.VideoItem(
                videoId = "vid-more",
                title = "More",
                channelId = "ch1",
                channelName = "Channel 1"
            )
            return Result.success(listOf(video) to null)
        }
    }

    private class FakeSearchHistoryDao : SearchHistoryDao {
        val itemsFlow = MutableStateFlow<List<SearchHistoryEntity>>(emptyList())
        val upserted = mutableListOf<String>()

        override fun getRecentFlow(limit: Int): Flow<List<SearchHistoryEntity>> = itemsFlow
        override suspend fun upsert(entity: SearchHistoryEntity) {
            upserted.add(entity.query)
            val list = itemsFlow.value.toMutableList()
            list.removeAll { it.query == entity.query }
            list.add(0, entity)
            itemsFlow.value = list.take(30)
        }

        override suspend fun delete(query: String): Int {
            val list = itemsFlow.value.toMutableList()
            val removed = list.removeAll { it.query == query }
            itemsFlow.value = list
            return if (removed) 1 else 0
        }

        override suspend fun clearAll(): Int {
            val count = itemsFlow.value.size
            itemsFlow.value = emptyList()
            return count
        }

        override suspend fun getCount(): Int = itemsFlow.value.size
    }

    private lateinit var fakeRepo: FakeSearchRepository
    private lateinit var fakeHistoryDao: FakeSearchHistoryDao
    private lateinit var viewModel: SearchViewModel

    private fun scheduler() = mainDispatcherRule.testDispatcher.scheduler

    @Before
    fun setUp() {
        fakeRepo = FakeSearchRepository()
        fakeHistoryDao = FakeSearchHistoryDao()
        viewModel = SearchViewModel(
            searchRepository = fakeRepo,
            searchHistoryDao = fakeHistoryDao,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            debounceMs = 500L
        )
    }

    @Test
    fun debounce_emitsOnlyLastQuery() = runTest {
        viewModel.onQueryChange("a")
        scheduler().advanceTimeBy(100L)
        viewModel.onQueryChange("ab")
        scheduler().advanceTimeBy(600L)
        scheduler().advanceUntilIdle()

        assertEquals(listOf("ab" to SearchFilter.ALL), fakeRepo.searchedQueries)
        val results = viewModel.uiState.value.results
        assertEquals(2, results.size)
    }

    @Test
    fun staleResponse_doesNotOverwriteNewer() = runTest {
        fakeRepo.delayFor = { query -> if (query == "slow") 1_000L else 0L }
        fakeRepo.nextToken = null

        viewModel.onSearchSubmit("slow")
        viewModel.onSearchSubmit("fast")
        scheduler().advanceUntilIdle()

        val results = viewModel.uiState.value.results
        assertEquals(2, results.size)
        val first = results.first() as SearchResultItem.VideoItem
        assertEquals("vid-fast", first.videoId)
    }

    @Test
    fun filterChange_resetsPageAndResearches() = runTest {
        fakeRepo.nextToken = "tok-1"
        viewModel.onSearchSubmit("q")
        scheduler().advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasMore)

        fakeRepo.nextToken = null
        viewModel.onFilterChange(SearchFilter.VIDEOS)
        scheduler().advanceUntilIdle()

        assertEquals(
            listOf("q" to SearchFilter.ALL, "q" to SearchFilter.VIDEOS),
            fakeRepo.searchedQueries
        )
        assertFalse(viewModel.uiState.value.hasMore)
        assertEquals(1, viewModel.uiState.value.results.size)
        assertEquals(SearchFilter.VIDEOS, viewModel.filter.value)
    }

    @Test
    fun successfulSearch_savesHistory() = runTest {
        fakeRepo.nextToken = null
        viewModel.onSearchSubmit("quran")
        scheduler().advanceUntilIdle()

        assertTrue(fakeHistoryDao.upserted.contains("quran"))
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun blankQuery_clearsResultsWithoutSearch() = runTest {
        viewModel.onSearchSubmit("q")
        scheduler().advanceUntilIdle()
        assertTrue(viewModel.uiState.value.results.isNotEmpty())

        viewModel.onQueryChange("   ")
        scheduler().advanceUntilIdle()

        assertTrue(viewModel.uiState.value.results.isEmpty())
        assertEquals(1, fakeRepo.searchedQueries.size)
    }
}
