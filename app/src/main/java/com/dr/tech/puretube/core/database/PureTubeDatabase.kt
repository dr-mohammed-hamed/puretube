package com.dr.tech.puretube.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dr.tech.puretube.core.database.dao.HistoryDao
import com.dr.tech.puretube.core.database.dao.SubscriptionDao
import com.dr.tech.puretube.core.database.dao.WatchLaterDao
import com.dr.tech.puretube.core.database.entity.HistoryEntity
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity

/**
 * PureTube Room SQLite Database.
 * Serves as the Single Source of Truth (SSOT) for local subscriptions, bookmarks, and history.
 * Conforms to Constitution Principles I, III, IV, and VIII.
 */
@Database(
    entities = [
        SubscriptionEntity::class,
        WatchLaterEntity::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PureTubeDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun watchLaterDao(): WatchLaterDao
    abstract fun historyDao(): HistoryDao
}
