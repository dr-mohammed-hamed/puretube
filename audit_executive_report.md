# Audit Executive Report — Search/Library/Playlists + Player Hardening (Multi-Agent Reviewers)

**التاريخ:** 2026-10-12 | **النطاق:** 24 ملف معدل + ~20 ملف جديد (DB v2, Playlist/Search repos, search/library/playlists UI, captions, player)
**الوضع:** تعديل متعدد الطبقات → **تفريع شجري هرمي كامل** (Lead + 3 أطفال Tier-2 بالتوازي) + حلقتا إصلاح + تدقيق دستوري

## 1. الشجرة المنفذة والمشاكل المكتشفة
- **Child 1A (Data & Persistence):** 0 حرج / 4 رئيسي / 7 ثانوي — أبرزها: سباق `getItemCount+addItem` يفسد `position`، نجاح فارغ بلا streams (DASH منتهي)، تسرب `pendingPages` غير محدود، تلوث `videoId` بـ `?list=`.
- **Child 1B (Concurrency & State):** 1 حرج / 2 رئيسي / 4 ثانوي — أبرزها: **[CRITICAL] استرداد 403 يخطف فيديو جديدا** (`PurePlayerManager:404` يفحص null فقط)، `toggleInPlaylist` بلا حارس نقر مزدوج، استيراد بلا `isLoading`.
- **Child 1C (UI & Design System):** 1 حرج / 6 رئيسي / 4 ثانوي — أبرزها: **[CRITICAL] `LaunchedEffect` داخل `LazyColumn` = حلقة `loadMore` لانهائية**، مفاتيح `$index` غير مستقرة، سهم `PlayArrow` للتنقل، `remember` ناقص، حقل URL بلا عزل LTR. الرموز اللونية سليمة 100% (`PureTheme.colors.*` فقط في `Color.kt`)، صفر `!!` في الواجهات.

## 2. الحل الجراحي المطبق (مبدأ الكاتب المنفرد، Minimal Delta)
| الجولة | الإصلاح | الدليل |
|---|---|---|
| 1 (13 ملفا) | حارس `videoId == details.videoId` لاسترداد 403؛ hoist `listState+LaunchedEffect` وحذف effect داخل item؛ `PlaylistDao.addItemAtEnd @Transaction` + `remove/clear+touch`؛ `IOException` عند خلو القائمتين؛ سقف `pendingPages=10` مع evict؛ `substringBefore ?/#`؛ حارس `togglingPlaylist ConcurrentHashMap` في VM2 + `isLoading` للاستيراد؛ مفاتيح مستقرة `videoId`؛ `ArrowForward AutoMirrored`؛ `remember(isLandscape)` + `remember(sheetDetails)`؛ عزل LTR لحقل URL؛ `remember listOf` | `assembleDebug` ناجح 2m35s |
| 2 (8 إصلاحات) | `currentDetailsId` بدل nav-arg للعضوية؛ مفاتيح بحث `$index:v_/c_` + `distinctBy` في VM؛ strip `#/&` لكل الفروع؛ `linkedMapOf` + سقف restore؛ `if(removed>0) touch`؛ `remember(...,activity,onBackClick)`؛ `AtomicBoolean creatingPlaylist` في 3 VMs؛ حذف `import items` الميت | `compileDebugKotlin` ناجح |
| Leader | حذف `import PlaylistImportState` الزائد في `PlayerScreen:45` | `assembleDebug` ناجح 17s |

إعادة الفحص النهائية: **0 حرج، 0 رئيسي، 0 ثانوي مانع** (مؤجل عمدا: تدويل ~40 literal عربي — خارج Blast Radius).

## 3. التدقيق الدستوري: **100% متوافق** ✅
- I: بحث صريح فقط، بلا kiosk/trending (grep صفر)، related same-channel فقط، pagination بـ token (لا doomscroll).
- II: خطافات `VideoFrameHook/AudioFilterHook` NoOp محفوظة بلا overhead.
- III: Room v2 على الجهاز فقط، صفر telemetry، دعم Takeout CSV + NewPipe JSON، history محلي بسقف 30.
- IV: `Semaphore(4)` + timeout 20s في كل المسارات، دمج DASH `MergingMediaSource` سليم، NIO desugar سليم.
- V: صفر hex خارج `Color.kt`، RTL كامل، صفر `TODO`/`!!`، سقف 1000 سطر محترم (max `PlayerViewModel:581`).
- ملاحظة خارج النطاق: `SubscriptionsViewModel` يمرر `localizedMessage` للـ UI بدون sanitize — متابعة لاحقة.

## 4. مصفوفة الاختبارات (القائد — تحقق مستقل)
- `./gradlew :app:testDebugUnitTest --offline`: **BUILD SUCCESSFUL 2m36s — 107 اختبار / 0 فشل / 0 خطأ / 0 متخطى** عبر 23 suite (منها `SearchRepositoryTest 12/12`، `PlaylistRepositoryTest 8/8`، `PlaybackPrefsTest 6/6`، `SearchViewModelTest 5/5`، `PlaylistViewModelTest 4/4`، `LibraryViewModelTest 3/3`).
- `./gradlew :app:assembleDebug --offline`: **BUILD SUCCESSFUL 17s** (تحذيرات deprecation مسبقة فقط).
- ملاحظة بنية تحتية: أول محاولة test فشلت باتصال Kotlin daemon العابر؛ النجاح مع fallback بدون daemon.

## 5. ملفات معدلة بالإصلاح الجراحي
`player/PurePlayerManager.kt`، `features/search/SearchScreen.kt`+`SearchViewModel.kt`+`SearchFilterRow.kt`+`SearchHistoryList.kt`، `core/data/repository/{PlaylistRepository,SearchRepository,FeedRepository,VideoDetailsRepository}.kt`، `core/database/dao/PlaylistDao.kt`، `features/player/{PlayerScreen,PlayerViewModel}.kt`، `features/playlists/PlaylistViewModel.kt`+`PlaylistDetailScreen.kt`+`sheets/AddToPlaylistSheet.kt`، `features/library/LibraryViewModel.kt`+`PlaylistRowCard.kt`، `features/feed/FeedScreen.kt`، `features/channel/components/ChannelVideosTab.kt`
