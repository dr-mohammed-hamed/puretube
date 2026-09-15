package com.dr.tech.puretube.core.database.di

import androidx.room.Room
import com.dr.tech.puretube.core.database.PureTubeDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module providing the Room SQLite Database singleton and DAOs.
 */
val databaseModule = module {
    single {
        // In development (Phase 1-2), destructive fallback enables rapid prototyping.
        // For Phase 5 production shipping, explicit Room Migrations must be defined
        // to prevent accidental user data loss (Constitution Principle III).
        Room.databaseBuilder(
            androidContext(),
            PureTubeDatabase::class.java,
            "puretube.db"
        ).fallbackToDestructiveMigration(dropAllTables = true)
         .build()
    }

    single { get<PureTubeDatabase>().subscriptionDao() }
    single { get<PureTubeDatabase>().watchLaterDao() }
    single { get<PureTubeDatabase>().historyDao() }
}
