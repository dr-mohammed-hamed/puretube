package com.dr.tech.puretube.core.extractor.pack

import com.dr.tech.puretube.core.database.entity.SubscriptionEntity

/**
 * Curated Starter Pack of verified, high-quality, and wholesome channels.
 * Automatically seeded on initial launch if the user's subscription list is empty,
 * preventing blank-slate deprivation while keeping content 100% algorithm-free
 * (Constitution Principle I & SPECIFICATION.md Section 2.1).
 */
object CuratedStarterPack {

    val channels: List<SubscriptionEntity> = listOf(
        SubscriptionEntity(
            channelId = "UCq4qX1L1N1yS1rL8B4L0j8g",
            channelName = "تلاوات القرآن الكريم المختارة",
            channelHandle = "@QuranRecitationsPure",
            avatarUrl = null,
            subscriberCountText = "مجموعة مباركة",
            isNotificationsEnabled = true,
            subscribedAtMs = 1700000000005L
        ),
        SubscriptionEntity(
            channelId = "UCfiwzLy-8yKzIbsmZTzxDgw",
            channelName = "الجزيرة الوثائقية",
            channelHandle = "@AljazeeraDocumentary",
            avatarUrl = null,
            subscriberCountText = "وثائقيات هادفة",
            isNotificationsEnabled = true,
            subscribedAtMs = 1700000000004L
        ),
        SubscriptionEntity(
            channelId = "UCrZdmsZhnvSmsMZZzT89aKg",
            channelName = "دروس أونلاين",
            channelHandle = "@DroosOnline4u",
            avatarUrl = null,
            subscriberCountText = "تطوير الذات والمهارات",
            isNotificationsEnabled = true,
            subscribedAtMs = 1700000000003L
        ),
        SubscriptionEntity(
            channelId = "UCAuXQ_S4U36y8gGj9Q9_hPQ",
            channelName = "علي محمد علي",
            channelHandle = "@AliMuhammadAli",
            avatarUrl = null,
            subscriberCountText = "قراءة الكتب والإنتاجية",
            isNotificationsEnabled = true,
            subscribedAtMs = 1700000000002L
        ),
        SubscriptionEntity(
            channelId = "UCe9W2Cwbq0L1V9yGZ7LhX9Q",
            channelName = "أكاديمية حسوب",
            channelHandle = "@HsoubAcademy",
            avatarUrl = null,
            subscriberCountText = "علوم الحاسوب والبرمجة",
            isNotificationsEnabled = true,
            subscribedAtMs = 1700000000001L
        ),
        SubscriptionEntity(
            channelId = "UC_x5XG1OV2P6uZZ5FSM9Ttw",
            channelName = "TED بالعربي",
            channelHandle = "@TEDArabic",
            avatarUrl = null,
            subscriberCountText = "أفكار تستحق الانتشار",
            isNotificationsEnabled = true,
            subscribedAtMs = 1700000000000L
        )
    )
}
