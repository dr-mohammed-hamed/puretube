---
name: sqlite-database-expert
risk_level: MEDIUM
description: Expert in Android Room & SQLite embedded database development with Kotlin, Coroutines, Flow, transactions, migrations, FTS search, and secure offline-first data persistence.
version: 2.0.0
tags: [database, room, sqlite, android, kotlin, migrations, coroutines, fts]
---

# Android Room & SQLite Database Expert

## 1. Overview & Architectural Scope

This skill governs embedded SQLite database architecture, persistence design, and query optimization for **Modern Android applications using Room and Kotlin Coroutines/Flow**.

### Core Principles
1. **Compile-Time Query Verification**: Rely on Room's static SQL verification to catch schema and syntax mismatches at compile time.
2. **Strict Thread Concurrency**: Database I/O must NEVER run on the main thread. Always leverage Coroutines (`suspend` functions) and asynchronous reactive streams (`Flow<T>`) executed on `Dispatchers.IO`.
3. **Transaction Atomicity**: Any operation mutating multiple entities or performing cascading inserts/updates must be wrapped inside `@Transaction`.
4. **Data Integrity & Foreign Keys**: Enforce referential integrity via SQLite foreign key constraints and cascade rules.
5. **Robust Migration Discipline**: Maintain schema versioning with deterministic migrations to guarantee zero data loss during app updates.

---

## 2. General Architectural Patterns (Kotlin & Room)

### 2.1 Entity & Schema Modeling
- Keep entities clean, immutable, and decoupled from UI models.
- Explicitly define column names and indexing strategies to optimize read/query paths.

```kotlin
@Entity(
    tableName = "items",
    indices = [Index(value = ["external_id"], unique = true)]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "external_id")
    val externalId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2.2 DAO Design & Parameterized Queries
- Use Room parameter binding (`:paramName`) for all queries to guarantee protection against SQL injection.
- Return reactive `Flow<List<T>>` for continuous UI updates, or `suspend fun` for one-shot operations.

```kotlin
@Dao
interface BaseItemDao {
    @Query("SELECT * FROM items ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE external_id = :externalId LIMIT 1")
    suspend fun getByExternalId(externalId: String): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Query("DELETE FROM items WHERE external_id = :externalId")
    suspend fun deleteByExternalId(externalId: String)
}
```

### 2.3 Multi-Table Operations & Atomic Transactions
- When updating multiple tables or refreshing collections, wrap the operation in `@Transaction` to maintain atomic database state.

```kotlin
@Dao
interface CompositeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: ItemDetailsEntity)

    @Transaction
    suspend fun upsertItemWithDetails(item: ItemEntity, details: ItemDetailsEntity) {
        insertItem(item)
        insertDetails(details)
    }
}
```

### 2.4 Full-Text Search (FTS4 / FTS5)
- For fast, localized searching over titles and descriptions, configure SQLite Virtual Tables using Room's `@Fts4` or `@Fts5`.

```kotlin
@Entity(tableName = "items_fts")
@Fts4(contentEntity = ItemEntity::class)
data class ItemFtsEntity(
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String
)

// DAO Query
@Query("""
    SELECT items.* FROM items
    JOIN items_fts ON items.id = items_fts.rowid
    WHERE items_fts MATCH :searchQuery
""")
fun searchItems(searchQuery: String): Flow<List<ItemEntity>>
```

---

## 3. Database Migrations & Versioning

1. **Schema Export**: Always enable `room.schemaLocation` in Gradle to generate schema JSON artifacts for version tracking.
2. **Deterministic Migrations**:
   - Write explicit `Migration(startVersion, endVersion)` implementations.
   - For simple column additions or table creations, leverage Room `AutoMigration`.
   - Never use `.fallbackToDestructiveMigration()` in production environments.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE items ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
    }
}
```

---

## 4. Performance & Reliability Standards

- **WAL Mode**: Room enables SQLite Write-Ahead Logging (WAL) by default; keep it enabled for concurrent reads and writes.
- **Index Selectivity**: Index columns frequently used in `WHERE`, `JOIN`, and `ORDER BY` clauses.
- **Background Dispatchers**: Ensure repository implementations call Room DAOs with `withContext(Dispatchers.IO)` or return `Flow` which Room automatically executes asynchronously.

---

## 5. Testing & Verification

### In-Memory Testing Pattern
Always test DAOs and database migrations using Room's in-memory builder in Android instrumentation tests:

```kotlin
@RunWith(AndroidJUnit4::class)
class DatabaseDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: BaseItemDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Safe ONLY in tests
            .build()
        dao = db.itemDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieve_preservesDataIntegrity() = runTest {
        // Assert expected query results against in-memory database
    }
}
```
