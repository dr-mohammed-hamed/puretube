package com.dr.tech.puretube.features

import com.dr.tech.puretube.core.data.repository.SubscriptionRepository
import com.dr.tech.puretube.core.database.dao.SubscriptionDao
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import com.dr.tech.puretube.core.extractor.portability.ImportExportService
import com.dr.tech.puretube.features.subscriptions.SubscriptionsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SubscriptionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeSubDao : SubscriptionDao {
        val subsFlow = MutableStateFlow<List<SubscriptionEntity>>(emptyList())
        override fun getAllFlow(): Flow<List<SubscriptionEntity>> = subsFlow
        override suspend fun getAll(): List<SubscriptionEntity> = subsFlow.value
        override fun isSubscribed(channelId: String): Flow<Boolean> =
            MutableStateFlow(subsFlow.value.any { it.channelId == channelId })
        override suspend fun insert(entity: SubscriptionEntity) {
            val list = subsFlow.value.toMutableList()
            list.removeAll { it.channelId == entity.channelId }
            list.add(entity)
            subsFlow.value = list
        }
        override suspend fun insertAll(entities: List<SubscriptionEntity>) {
            val list = subsFlow.value.toMutableList()
            entities.forEach { entity ->
                list.removeAll { it.channelId == entity.channelId }
                list.add(entity)
            }
            subsFlow.value = list
        }
        override suspend fun deleteById(channelId: String): Int {
            val list = subsFlow.value.toMutableList()
            val removed = list.removeAll { it.channelId == channelId }
            subsFlow.value = list
            return if (removed) 1 else 0
        }
        override suspend fun getCount(): Int = subsFlow.value.size
    }

    private lateinit var subDao: FakeSubDao
    private lateinit var subRepo: SubscriptionRepository
    private lateinit var viewModel: SubscriptionsViewModel

    @Before
    fun setUp() {
        subDao = FakeSubDao()
        val dispatcher = mainDispatcherRule.testDispatcher
        subRepo = SubscriptionRepository(subDao, ExtractorThrottler(4), ImportExportService(), ioDispatcher = dispatcher)
        viewModel = SubscriptionsViewModel(subRepo, ioDispatcher = dispatcher)
    }

    @Test
    fun initialState_isEmpty() {
        assertTrue(viewModel.uiState.value.subscriptions.isEmpty())
    }

    @Test
    fun activateStarterPack_populatesSubscriptions() = runTest {
        viewModel.activateStarterPack()
        assertTrue(viewModel.uiState.value.subscriptions.isNotEmpty())
        assertNotNull(viewModel.uiState.value.userNoticeMessage)
    }

    @Test
    fun unsubscribeFlow_requestAndConfirm_removesChannel() = runTest {
        val channel = SubscriptionEntity("c_del", "Delete Channel", "@del", null, "10", true, 1000L)
        subDao.insert(channel)
        assertEquals(1, viewModel.uiState.value.subscriptions.size)

        viewModel.requestUnsubscribe(channel)
        assertEquals(channel, viewModel.uiState.value.channelPendingUnsubscribe)

        viewModel.confirmUnsubscribe()
        assertNull(viewModel.uiState.value.channelPendingUnsubscribe)
        assertTrue(viewModel.uiState.value.subscriptions.isEmpty())
    }

    @Test
    fun dismissUnsubscribeDialog_clearsPending() {
        val channel = SubscriptionEntity("c_keep", "Keep Channel", "@keep", null, "10", true, 1000L)
        viewModel.requestUnsubscribe(channel)
        assertNotNull(viewModel.uiState.value.channelPendingUnsubscribe)

        viewModel.dismissUnsubscribeDialog()
        assertNull(viewModel.uiState.value.channelPendingUnsubscribe)
    }
}
