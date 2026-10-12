package com.dr.tech.puretube.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * ترحيلات Room الصريحة (المبدأ III: لا فقدان بيانات في الإصدارات).
 * v1: subscriptions/watch_later/playback_history.
 * v2: + playlists/playlist_items(FK CASCADE)/search_history.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlists` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`createdAtMs` INTEGER NOT NULL, " +
                "`updatedAtMs` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playlists_createdAtMs` " +
                "ON `playlists` (`createdAtMs`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`playlistId` INTEGER NOT NULL, " +
                "`videoId` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`channelId` TEXT NOT NULL, " +
                "`channelName` TEXT NOT NULL, " +
                "`thumbnailUrl` TEXT, " +
                "`durationMs` INTEGER NOT NULL, " +
                "`addedAtMs` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playlist_items_playlistId` " +
                "ON `playlist_items` (`playlistId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playlist_items_videoId` " +
                "ON `playlist_items` (`videoId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_playlist_items_playlistId_videoId` " +
                "ON `playlist_items` (`playlistId`, `videoId`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `search_history` (" +
                "`query` TEXT NOT NULL, " +
                "`searchedAtMs` INTEGER NOT NULL, " +
                "PRIMARY KEY(`query`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_search_history_searchedAtMs` " +
                "ON `search_history` (`searchedAtMs`)"
        )
    }
}
