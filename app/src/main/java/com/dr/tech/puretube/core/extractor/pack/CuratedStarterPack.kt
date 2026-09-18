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
            channelId = "@QuranRadio_ksa",
            channelName = "إذاعة القرآن الكريم",
            channelHandle = "@QuranRadio_ksa",
            avatarUrl = null,
            subscriberCountText = "تلاوات وبرامج قرآنية مباركة",
            isNotificationsEnabled = true,
            subscribedAtMs = 1700000000005L
        ),
        SubscriptionEntity(
            channelId = "@AljazeeraDocumentary",
            channelName = "الجزيرة الوثائقية",
            channelHandle = "@AljazeeraDocumentary",
            avatarUrl = null,
            subscriberCountText = "وثائقيات هادفة",
            isNotificationsEnabled = true,
            subscribedAtMs = 1700000000004L
        ),
        SubscriptionEntity(
            channelId = "@DroosOnline4u",
            channelName = "دروس أونلاين",
            channelHandle = "@DroosOnline4u",
            avatarUrl = null,
            subscriberCountText = "تطوير الذات والمهارات",
            isNotificationsEnabled = true,
            subscribedAtMs = 1700000000003L
        ),
        SubscriptionEntity(
            channelId = "@AliMuhammadAli",
            channelName = "علي محمد علي",
            channelHandle = "@AliMuhammadAli",
            avatarUrl = null,
            subscriberCountText = "قراءة الكتب والإنتاجية",
            isNotificationsEnabled = true,
            subscribedAtMs = 1700000000002L
        ),
        SubscriptionEntity(
            channelId = "@HsoubAcademy",
            channelName = "أكاديمية حسوب",
            channelHandle = "@HsoubAcademy",
            avatarUrl = null,
            subscriberCountText = "علوم الحاسوب والبرمجة",
            isNotificationsEnabled = true,
            subscribedAtMs = 1700000000001L
        ),
        SubscriptionEntity(
            channelId = "@Thmanyah",
            channelName = "ثمانية",
            channelHandle = "@Thmanyah",
            avatarUrl = null,
            subscriberCountText = "وثائقيات وبودكاست هادف",
            isNotificationsEnabled = true,
            subscribedAtMs = 1700000000000L
        )
    )
}
