package com.dr.tech.puretube.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** سجل بحث محلي (نص الاستعلام مفتاح، بحد 30 يُدار من DAO/Repository). */
@Entity(tableName = "search_history", indices = [Index(value = ["searchedAtMs"])])
data class SearchHistoryEntity(
    @PrimaryKey
    val query: String,
    val searchedAtMs: Long = System.currentTimeMillis()
)
