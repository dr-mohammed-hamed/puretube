package com.dr.tech.puretube.features

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dr.tech.puretube.core.data.repository.HistoryRepository
import com.dr.tech.puretube.core.data.repository.SubscriptionRepository
import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.database.dao.HistoryDao
import com.dr.tech.puretube.core.database.dao.SubscriptionDao
import com.dr.tech.puretube.core.database.dao.WatchLaterDao
import com.dr.tech.puretube.core.database.entity.HistoryEntity
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import com.dr.tech.puretube.core.designsystem.theme.ThemePreferences
import com.dr.tech.puretube.core.designsystem.theme.ThemePreset
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import com.dr.tech.puretube.core.extractor.portability.ImportExportService
import com.dr.tech.puretube.features.settings.SettingsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeSubDao : SubscriptionDao {
        override fun getAllFlow(): Flow<List<SubscriptionEntity>> = flowOf(emptyList())
        override suspend fun getAll(): List<SubscriptionEntity> = emptyList()
        override fun isSubscribed(channelId: String): Flow<Boolean> = flowOf(false)
        override suspend fun insert(entity: SubscriptionEntity) {}
        override suspend fun insertAll(entities: List<SubscriptionEntity>) {}
        override suspend fun deleteById(channelId: String): Int = 0
        override suspend fun getCount(): Int = 5
    }

    private class FakeWlDao : WatchLaterDao {
        override fun getAllFlow(): Flow<List<WatchLaterEntity>> = flowOf(emptyList())
        override fun isInWatchLater(videoId: String): Flow<Boolean> = flowOf(false)
        override suspend fun insert(entity: WatchLaterEntity) {}
        override suspend fun deleteById(videoId: String): Int = 0
        override suspend fun clearAll(): Int = 0
        override suspend fun getCount(): Int = 12
    }

    private class FakeHistDao : HistoryDao {
        override fun getRecentFlow(limit: Int): Flow<List<HistoryEntity>> = flowOf(emptyList())
        override suspend fun getById(videoId: String): HistoryEntity? = null
        override suspend fun getPlaybackPosition(videoId: String): Long? = null
        override suspend fun upsert(entity: HistoryEntity) {}
        override suspend fun updatePlaybackPosition(videoId: String, positionMs: Long, timestampMs: Long): Int = 1
        override suspend fun markCompleted(videoId: String): Int = 1
        override suspend fun deleteById(videoId: String): Int = 0
        override suspend fun clearAll(): Int = 0
        override suspend fun getCount(): Int = 8
    }

    private lateinit var context: Context
    private lateinit var themePreferences: ThemePreferences
    private lateinit var subRepo: SubscriptionRepository
    private lateinit var wlRepo: WatchLaterRepository
    private lateinit var histRepo: HistoryRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("puretube_theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        themePreferences = ThemePreferences(context)
        val dispatcher = mainDispatcherRule.testDispatcher
        subRepo = SubscriptionRepository(FakeSubDao(), ExtractorThrottler(4), ImportExportService(), ioDispatcher = dispatcher)
        wlRepo = WatchLaterRepository(FakeWlDao(), ioDispatcher = dispatcher)
        histRepo = HistoryRepository(FakeHistDao(), ioDispatcher = dispatcher)
        viewModel = SettingsViewModel(themePreferences, subRepo, wlRepo, histRepo, ioDispatcher = dispatcher)
    }

    @Test
    fun defaultPreset_isEmeraldNight() {
        assertEquals(ThemePreset.EMERALD_NIGHT, viewModel.uiState.value.currentPreset)
    }

    @Test
    fun selectThemePreset_updatesThemeAndState() {
        viewModel.selectThemePreset(ThemePreset.ROYAL_INDIGO)
        assertEquals(ThemePreset.ROYAL_INDIGO, viewModel.uiState.value.currentPreset)
        assertEquals(ThemePreset.ROYAL_INDIGO, themePreferences.currentPreset.value)
    }

    @Test
    fun refreshDatabaseStats_loadsCountsFromRepositories() = runTest {
        viewModel.refreshDatabaseStats()
        assertEquals(5, viewModel.uiState.value.subscriptionCount)
        assertEquals(12, viewModel.uiState.value.watchLaterCount)
        assertEquals(8, viewModel.uiState.value.historyCount)
    }
}
