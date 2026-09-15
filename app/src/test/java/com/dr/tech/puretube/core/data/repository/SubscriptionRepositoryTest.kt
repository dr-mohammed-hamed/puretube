package com.dr.tech.puretube.core.data.repository

import com.dr.tech.puretube.core.database.dao.SubscriptionDao
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import com.dr.tech.puretube.core.extractor.pack.CuratedStarterPack
import com.dr.tech.puretube.core.extractor.portability.ImportExportService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SubscriptionRepository verifying auto-seeding starter pack logic (PM Decision 1),
 * reactive subscriptions flow, and mutations.
 */
class SubscriptionRepositoryTest {

    private class TestSubscriptionDao : SubscriptionDao {
        val list = mutableListOf<SubscriptionEntity>()

        override fun getAllFlow(): Flow<List<SubscriptionEntity>> = flowOf(list.toList())
        override suspend fun getAll(): List<SubscriptionEntity> = list.toList()
        override fun isSubscribed(channelId: String): Flow<Boolean> =
            flowOf(list.any { it.channelId == channelId })

        override suspend fun insert(entity: SubscriptionEntity) {
            list.removeAll { it.channelId == entity.channelId }
            list.add(entity)
        }

        override suspend fun insertAll(entities: List<SubscriptionEntity>) {
            entities.forEach { insert(it) }
        }

        override suspend fun deleteById(channelId: String): Int {
            val removed = list.removeAll { it.channelId == channelId }
            return if (removed) 1 else 0
        }

        override suspend fun getCount(): Int = list.size
    }

    private lateinit var dao: TestSubscriptionDao
    private lateinit var throttler: ExtractorThrottler
    private lateinit var importExportService: ImportExportService
    private lateinit var repository: SubscriptionRepository

    @Before
    fun setUp() {
        dao = TestSubscriptionDao()
        throttler = ExtractorThrottler(maxConcurrentCalls = 4)
        importExportService = ImportExportService()
        repository = SubscriptionRepository(dao, throttler, importExportService)
    }

    @Test
    fun checkAndSeedStarterPack_whenDbIsEmpty_seedsCuratedPack() = runTest {
        assertEquals(0, dao.getCount())

        val seeded = repository.checkAndSeedStarterPackIfEmpty()
        assertTrue(seeded)
        assertEquals(CuratedStarterPack.channels.size, dao.getCount())

        // Verify channels are present
        val all = repository.getAllSubscriptions()
        assertTrue(all.any { it.channelId == CuratedStarterPack.channels.first().channelId })
    }

    @Test
    fun checkAndSeedStarterPack_whenDbHasExistingSubscriptions_skipsSeeding() = runTest {
        // User already has an existing custom channel
        val customChannel = SubscriptionEntity(
            channelId = "UC_custom_channel",
            channelName = "قناتي المفضلة",
            subscribedAtMs = 1700000000000L
        )
        dao.insert(customChannel)
        assertEquals(1, dao.getCount())

        val seeded = repository.checkAndSeedStarterPackIfEmpty()
        assertFalse(seeded) // Must return false and NOT seed
        assertEquals(1, dao.getCount())
        assertEquals("UC_custom_channel", repository.getAllSubscriptions().first().channelId)
    }

    @Test
    fun subscribeAndUnsubscribe_updatesStateCorrectly() = runTest {
        val testChannel = SubscriptionEntity(
            channelId = "UC_test_123",
            channelName = "قناة اختبارية"
        )

        repository.subscribe(testChannel)
        assertTrue(repository.isSubscribed("UC_test_123").first())
        assertEquals(1, repository.getAllSubscriptions().size)

        repository.unsubscribe("UC_test_123")
        assertFalse(repository.isSubscribed("UC_test_123").first())
        assertEquals(0, repository.getAllSubscriptions().size)
    }

    @Test
    fun reactivateStarterPack_seedsAllCuratedChannels() = runTest {
        repository.reactivateStarterPack()
        assertEquals(CuratedStarterPack.channels.size, dao.getCount())
    }

    @Test
    fun subscriptionMutations_triggerOnSubscriptionsChangedCallback() = runTest {
        var callbackCount = 0
        repository.onSubscriptionsChanged = {
            callbackCount++
        }

        val testChannel = SubscriptionEntity(
            channelId = "UC_callback_test",
            channelName = "قناة اختبار الإشعار"
        )

        repository.subscribe(testChannel)
        assertEquals(1, callbackCount)

        repository.unsubscribe("UC_callback_test")
        assertEquals(2, callbackCount)

        repository.reactivateStarterPack()
        assertEquals(3, callbackCount)

        // Verify seeding on empty triggers callback
        dao.deleteById("UC_callback_test")
        dao.list.clear()
        repository.checkAndSeedStarterPackIfEmpty()
        assertEquals(4, callbackCount)
    }

    @Test
    fun subscriptionMutations_emitSubscriptionEventsSharedFlow() = runTest {
        var eventCount = 0
        val job = backgroundScope.launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            repository.subscriptionEvents.collect {
                eventCount++
            }
        }

        val testChannel = SubscriptionEntity(
            channelId = "UC_flow_test",
            channelName = "قناة تدفق الأحداث"
        )

        repository.subscribe(testChannel)
        assertEquals(1, eventCount)

        repository.unsubscribe("UC_flow_test")
        assertEquals(2, eventCount)

        job.cancel()
    }
}
