# Quickstart & Verification Guide: UI & Navigation (002-ui-and-navigation)

**Feature**: UI & Navigation Shell  
**Created**: 2026-09-15  
**Status**: Ready  

---

## 1. Automated Verification Commands

Run the following commands from the root directory to verify compilation and testing:

```bash
# 1. Run all unit tests across data, repositories, and ViewModels
./gradlew test

# 2. Run debug assemble build to verify Compose compiler, KSP, and Room compilation
./gradlew assembleDebug

# 3. Check for any forbidden hardcoded hex colors in UI files (Strict Quality Gate)
# Should return zero matches in features/ and components/
git grep "Color(0x" app/src/main/java/com/dr/tech/puretube/features/
```

---

## 2. Manual Verification Walkthrough (PM / QA Steps)

### Flow 1: Bottom Navigation & Tab Switching
1. Launch the app on an Android device or emulator.
2. Verify the permanent bottom navigation bar is visible with 4 Arabic tabs:
   - **الرئيسية (Feed)**
   - **الاشتراكات (Subscriptions)**
   - **المشاهدة لاحقاً (Watch Later)**
   - **الإعدادات (Settings)**
3. Tap on each tab in sequence.
4. Verify instant tab switching with active accent color in `PureTheme.colors.primary` and no screen flickering.

### Flow 2: Mindful Feed & Curated Starter Pack
1. If first boot: Verify the Curated Starter Pack channels are seeded and their latest uploads appear in the Feed.
2. Perform pull-to-refresh on the Feed.
3. Observe the subtle loading indicator and verify items are sorted by newest upload date.
4. Tap the bookmark icon on any video card; confirm the Snackbar appears notifying that the video was added to "Watch Later".

### Flow 3: Subscriptions Management
1. Switch to the **الاشتراكات** tab.
2. Check the count of followed channels.
3. Type `@AlJazeeraChannel` or paste a YouTube channel link into the search bar, then tap "إضافة".
4. Confirm the new channel appears in the list.
5. Tap unsubscribe on any channel; verify the confirmation dialog appears before removal.

### Flow 4: Watch Later Screen
1. Switch to the **المشاهدة لاحقاً** tab.
2. Confirm the video bookmarked in Flow 2 is present with its thumbnail, title, and duration.
3. Tap the delete icon to remove the video; verify the list updates immediately.

### Flow 5: Settings & Theme Persistence
1. Switch to the **الإعدادات** tab.
2. Scroll to the "المظهر والثيمات" section.
3. Select another theme preset (e.g. "سواد الأوليد - OLED Pure Black" or "سكينة الطبيعة - Nordic Sage").
4. Verify the entire application transforms into the new color scheme immediately.
5. Close and terminate the app completely from Recent Apps.
6. Re-open the app; confirm that the selected theme preset remains active without resetting!
