package com.dr.tech.puretube.core.database.di

import androidx.room.Room
import com.dr.tech.puretube.core.database.MIGRATION_1_2
import com.dr.tech.puretube.core.database.PureTubeDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module providing the Room SQLite Database singleton and DAOs.
 */
val databaseModule = module {
    single {
        // ترحيل صريح v1->v2 يحفظ بيانات المستخدم (المبدأ III).
        // مُنع fallbackToDestructiveMigration في الإصدارات لمنع مسح البيانات.
        Room.databaseBuilder(
            androidContext(),
            PureTubeDatabase::class.java,
            "puretube.db"
        ).addMigrations(MIGRATION_1_2)
          .build()
    }

    single { get<PureTubeDatabase>().subscriptionDao() }
    single { get<PureTubeDatabase>().watchLaterDao() }
    single { get<PureTubeDatabase>().historyDao() }
    single { get<PureTubeDatabase>().playlistDao() }
    single { get<PureTubeDatabase>().searchHistoryDao() }
}
