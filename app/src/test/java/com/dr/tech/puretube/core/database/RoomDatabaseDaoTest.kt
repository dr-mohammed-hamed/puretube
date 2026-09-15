package com.dr.tech.puretube.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dr.tech.puretube.core.database.dao.HistoryDao
import com.dr.tech.puretube.core.database.dao.SubscriptionDao
import com.dr.tech.puretube.core.database.dao.WatchLaterDao
import com.dr.tech.puretube.core.database.entity.HistoryEntity
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-backed in-memory database tests verifying SubscriptionDao,
 * WatchLaterDao, and HistoryDao.
 * Conforms to Constitution Principles I, III, IV, and VIII.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomDatabaseDaoTest {

    private lateinit var database: PureTubeDatabase
    private lateinit var subscriptionDao: SubscriptionDao
    private lateinit var watchLaterDao: WatchLaterDao
    private lateinit var historyDao: HistoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PureTubeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        subscriptionDao = database.subscriptionDao()
        watchLaterDao = database.watchLaterDao()
        historyDao = database.historyDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun subscriptionDao_crudOperations_workCorrectly() = runTest {
        assertEquals(0, subscriptionDao.getCount())

        val sub = SubscriptionEntity(
            channelId = "UC12345",
            channelName = "قناة إسلامية هادفة",
            channelHandle = "@Hadef",
            subscribedAtMs = 1700000000000L
        )
        subscriptionDao.insert(sub)
        assertEquals(1, subscriptionDao.getCount())

        val all = subscriptionDao.getAll()
        assertEquals(1, all.size)
        assertEquals("UC12345", all.first().channelId)

        assertTrue(subscriptionDao.isSubscribed("UC12345").first())
        assertFalse(subscriptionDao.isSubscribed("UC99999").first())

        val deleted = subscriptionDao.deleteById("UC12345")
        assertEquals(1, deleted)
        assertEquals(0, subscriptionDao.getCount())
    }

    @Test
    fun watchLaterDao_crudOperations_workCorrectly() = runTest {
        assertEquals(0, watchLaterDao.getCount())

        val item = WatchLaterEntity(
            videoId = "vid_abc_123",
            title = "درس تفسير نافع",
            channelId = "UC12345",
            channelName = "تفسير القرآن",
            durationMs = 1800000L, // 30 minutes in ms
            addedAtMs = 1700000000000L
        )
        watchLaterDao.insert(item)
        assertEquals(1, watchLaterDao.getCount())

        assertTrue(watchLaterDao.isInWatchLater("vid_abc_123").first())
        assertFalse(watchLaterDao.isInWatchLater("vid_other").first())

        val list = watchLaterDao.getAllFlow().first()
        assertEquals(1, list.size)
        assertEquals(1800000L, list.first().durationMs)

        val deleted = watchLaterDao.deleteById("vid_abc_123")
        assertEquals(1, deleted)
        assertEquals(0, watchLaterDao.getCount())
    }

    @Test
    fun historyDao_resumePositionAndCompletion_workCorrectly() = runTest {
        val history = HistoryEntity(
            videoId = "vid_lecture_01",
            title = "محاضرة علمية هادفة",
            channelId = "UC998877",
            channelName = "قناة العلم",
            durationMs = 3600000L, // 1 hour in ms
            lastPlaybackPositionMs = 600000L, // 10 minutes in ms
            lastPlayedAtMs = 1700000000000L,
            isCompleted = false
        )

        historyDao.upsert(history)

        val item = historyDao.getById("vid_lecture_01")
        assertNotNull(item)
        assertEquals(600000L, item?.lastPlaybackPositionMs)
        assertEquals(false, item?.isCompleted)

        // Update position to 25 minutes (1,500,000 ms)
        historyDao.updatePlaybackPosition("vid_lecture_01", 1500000L, 1700001000000L)
        val updatedPosition = historyDao.getPlaybackPosition("vid_lecture_01")
        assertEquals(1500000L, updatedPosition)

        // Mark completed -> isCompleted = true, position resets to 0
        historyDao.markCompleted("vid_lecture_01")
        val completedItem = historyDao.getById("vid_lecture_01")
        assertNotNull(completedItem)
        assertEquals(true, completedItem?.isCompleted)
        assertEquals(0L, completedItem?.lastPlaybackPositionMs)

        // Re-watching resets isCompleted to false
        historyDao.updatePlaybackPosition("vid_lecture_01", 300000L, 1700002000000L)
        val rewatchedItem = historyDao.getById("vid_lecture_01")
        assertEquals(false, rewatchedItem?.isCompleted)
        assertEquals(300000L, rewatchedItem?.lastPlaybackPositionMs)

        // Clear history
        historyDao.clearAll()
        val emptyList = historyDao.getRecentFlow().first()
        assertEquals(0, emptyList.size)
        assertNull(historyDao.getById("vid_lecture_01"))
    }
}
