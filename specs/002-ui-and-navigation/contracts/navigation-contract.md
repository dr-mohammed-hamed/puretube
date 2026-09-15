# Contracts & Interface Specifications: UI & Navigation (002-ui-and-navigation)

**Feature**: UI & Navigation Shell  
**Created**: 2026-09-15  
**Status**: Completed  

---

## 1. Navigation Shell Contract

```kotlin
interface NavigationHost {
    val currentTab: StateFlow<PureNavTab>
    fun navigateTo(tab: PureNavTab)
}
```

- Exposed via `MainNavigationShell` Composable.
- All top-level destinations are declared in `PureNavTab`.
- Switching tabs hoists state and retains in-memory view hierarchy.

---

## 2. ViewModel Event Contracts

### Feed Contract
```kotlin
interface FeedScreenContract {
    val uiState: StateFlow<FeedUiState>
    fun refreshFeed(force: Boolean = true)
    fun addToWatchLater(video: FeedVideoItem)
    fun activateStarterPack()
}
```

### Subscriptions Contract
```kotlin
interface SubscriptionsScreenContract {
    val uiState: StateFlow<SubscriptionsUiState>
    fun addChannel(query: String)
    fun activateStarterPack()
    fun requestUnsubscribe(channel: SubscriptionEntity)
    fun confirmUnsubscribe()
    fun dismissUnsubscribeDialog()
    fun importNewPipeJson(json: String)
    fun importTakeoutCsv(csv: String)
    fun exportSubscriptions(): String?
    fun clearUserNotice()
}
```

### Watch Later Contract
```kotlin
interface WatchLaterScreenContract {
    val uiState: StateFlow<WatchLaterUiState>
    fun removeFromWatchLater(videoId: String)
    fun clearAll()
}
```

### Settings Contract
```kotlin
interface SettingsScreenContract {
    val uiState: StateFlow<SettingsUiState>
    fun setPreset(preset: ThemePreset)
}
```

---

## 3. UI Component Contracts

### PureVideoCard Contract
- **Inputs**:
  - `video: FeedVideoItem`
  - `onVideoClick: (String) -> Unit` (passes `videoId`)
  - `onSaveWatchLater: (FeedVideoItem) -> Unit`
  - `onChannelClick: (String) -> Unit`
- **Guarantees**:
  - Zero hardcoded colors; reads exclusively from `PureTheme.colors`.
  - Zero Unicode emojis; duration badge formatted cleanly as mm:ss or hh:mm:ss.
  - Image loaded via Coil with smooth placeholder and error fallback.
