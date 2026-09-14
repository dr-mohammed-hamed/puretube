# Android & Kotlin Testing Reference (Test Guard)

Testing standards and failure-mode checks for Android applications with Kotlin, Coroutines, Room SQLite, and Jetpack Compose.

---

## 1. Coroutines & Flow Testing

### Core Principles
- Always wrap asynchronous unit tests in `kotlinx.coroutines.test.runTest`.
- Replace `Dispatchers.Main` in unit tests with a test dispatcher via a custom JUnit TestRule.
- Use `StandardTestDispatcher` when verifying specific scheduling sequences (`advanceUntilIdle()`), and `UnconfinedTestDispatcher` for simple eager collection.

### Testing Flows (Turbine Pattern)
Test reactive `Flow` emissions at caller boundaries rather than polling or sleeping:

```kotlin
@Test
fun streamEmitsUpdatedState_whenTriggered() = runTest {
    repository.observeItems().test {
        // First item emitted (initial or cached state)
        val initial = awaitItem()
        assertTrue(initial.isEmpty())

        // Trigger action that updates the data source
        repository.addItem(...)

        // Verify next emission reflects new state
        val updated = awaitItem()
        assertEquals(1, updated.size)
        
        cancelAndIgnoreRemainingEvents()
    }
}
```

---

## 2. Room Database Testing

### Rule: Use Real In-Memory Database (No Mocking)
Never mock Room DAOs or SQLite statements. Always test persistence behavior against a real in-memory SQLite database:

```kotlin
@RunWith(AndroidJUnit4::class)
class DaoPersistenceTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.itemDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadEntity_persistsCorrectly() = runTest {
        val entity = createSampleEntity()
        dao.insert(entity)
        val result = dao.getById(entity.id)
        assertEquals(entity, result)
    }
}
```

---

## 3. Jetpack Compose UI Testing

### Rule: Test User Observable Semantics, Not Implementation
Compose tests must interact with semantics visible to the user (text, content descriptions, click actions) rather than verifying internal composable functions:

```kotlin
@RunWith(AndroidJUnit4::class)
class ComponentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun buttonClick_triggersExpectedStateTransition() {
        var clicked = false
        composeTestRule.setContent {
            CustomActionButton(onClick = { clicked = true })
        }

        composeTestRule
            .onNodeWithText("Action Label")
            .performClick()

        assertTrue(clicked)
    }
}
```

---

## 4. Common Android Testing Failure Modes

1. **Flaky Delay Loops**: Using `delay(500)` in tests instead of `advanceUntilIdle()` or Turbine flow collectors.
2. **Mocking Value/Data Classes**: Mocking Kotlin `data class` models instead of constructing real instances.
3. **Testing Framework Guarantees**: Writing unit tests that only verify Room inserts return primary keys or AndroidX Bundle serializes data without testing any custom business logic.
4. **Unscoped Coroutine Launches**: Launching coroutines using `GlobalScope` in production code which outlive test runners.
