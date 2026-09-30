# Quick Notes Knowledge Graph (Fase 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect notes through `[[Título]]` links (typed or inserted by a "ligar nota" button), persist them in a `note_links` table (Room v1 → v2 with backfill), show backlinks/outgoing links in the editor and render notes + tags as an interactive force-directed graph tab.

**Architecture:** Text is the single source of truth: every `saveNote` re-parses the content with a pure-Kotlin `LinkParser` and rewrites that note's rows in `note_links` inside the same Room transaction, also resolving pending ghosts and propagating renames. Title matching happens in Kotlin (`trim().lowercase()`), never in SQL, because SQLite `LIKE`/`NOCASE` only fold ASCII. The graph tab is a Compose `Canvas` driven by a hand-written `ForceLayout` (O(n²), ~300 nodes) held in an activity-scoped `GraphViewModel` so node positions survive navigation.

**Tech Stack:** Kotlin, Jetpack Compose (Canvas, pointerInput, TextMeasurer), Room 2.6.1 (+ `room-testing` for `MigrationTestHelper`), Navigation Compose, kotlinx-coroutines-test. No new runtime dependency.

**Spec:** `docs/superpowers/specs/2026-09-30-quick-notes-knowledge-graph-design.md`

## Global Constraints

- Package `com.quicknotes.app`; `minSdk = 26`, `targetSdk = 34`, `compileSdk = 34`; Room `2.6.1` via `kapt`; Compose BOM `2024.09.00`.
- No new runtime dependency: graph = Compose `Canvas` + own force simulation ("sem bibliotecas externas como D3.js ou Force Graph"). The only new artifact is `androidTestImplementation("androidx.room:room-testing:2.6.1")`.
- Room migration is non-destructive: never `fallbackToDestructiveMigration`; `MIGRATION_1_2` creates `note_links` and backfills it from existing content.
- Never write notes with `OnConflictStrategy.REPLACE`: a REPLACE deletes the row and would CASCADE-delete its links.
- Link syntax: regex `\[\[(.*?)]]`, title trimmed, match case-insensitive, original text preserved for UI. Blank `[[ ]]` is not a link.
- `note_links` primary key is `(sourceId, index)`; `sourceId` FK CASCADE, `targetId` FK SET NULL (null = fantasma).
- Graph: nodes = notes (circle ~20dp) + tags (square ~18dp) + ghosts (50% opacity, dashed border). Folders are NOT nodes. Simulation ≤ 10 iterations per frame; known limit ~300 nodes.
- Branch `feature/quick-notes-graph` from `develop`; commit messages `feature[quick-notes-graph]: <descrição>` (CONTRIBUTING.md).
- Unit tests: `./gradlew :app:testDebugUnitTest`; instrumented: `./gradlew :app:connectedDebugAndroidTest` (single class: `-Pandroid.testInstrumentationRunnerArguments.class=<FQCN>`).
- Two fronts. **developer-core** owns data/domain/migration files; **developer-ui** owns everything under `ui/` plus `EditorViewModelTest`/`QuickNotesNavHostTest`. A task never edits a file owned by the other front; if it must, stop and ask the coordinator.

## Review Focus

- Accented or mixed-case titles (`Reunião` vs `[[REUNIÃO]]`) must resolve to the same note — pinned in Task 1 (`titleIndexMatchesCaseAndAccentsInsensitively…`) and Task 4 (`savingStoresOrderedLinksResolvedCaseAndAccentInsensitively`).
- Titles with regex/SQL metacharacters (`C++ & 100% (rascunho)`, `$1`) must rename cleanly without touching other text — pinned in Task 1 (`renameHandlesSpecialCharacters`) and Task 4 (`renamePropagatesToLinkingNotesWithSpecialCharacters`).
- Blank titles (quick captures) must never become link targets, and renaming a note to a blank title must not rewrite `[[Old]]` into `[[]]` — pinned in Task 1 (`ignoresSingleBracketsAndBlankLinks`) and Task 4 (`blankTitlesNeverResolveAndBlankRenameKeepsText`).
- Deleting a note that others link to must turn those links into ghosts (not drop them) and a ghost tap must open the editor with the title prefilled — pinned in Task 4 (`deletingTargetTurnsLinksIntoGhostsAndSourceDeleteRemovesLinks`) and Task 6 (`ghostRowCallsCreateWithItsTitle`, `ghostPrefillStartsNewNoteWithTitle`).
- Degenerate graph input — a note linking to itself, two bodies on the same point, a tag deleted while it is the active filter — must not crash or produce NaN/empty graph — pinned in Task 5 (`coincidentNodesDoNotProduceNaN`) and Task 8 (`selfLinksAreDroppedAndDeletedFilterTagShowsFullGraph`).

## Ownership and dependencies

| Task | Dono | Depende de |
|---|---|---|
| 1 LinkParser | developer-core | — |
| 2 Room v2 + migração | developer-core | 1 |
| 3 API de leitura de links | developer-core | 2 |
| 4 Manutenção de links no save | developer-core | 3 |
| 5 ForceLayout | developer-ui | — |
| 6 Seção Ligações + rota prefillTitle | developer-ui | 3 |
| 7 Botão "ligar nota" | developer-ui | 6 |
| 8 GraphModel + GraphViewModel | developer-ui | 3, 5 |
| 9 Tela Grafo + aba | developer-ui | 5, 6, 8 |
| 10 README + verificação final | coordenador | 1–9 |

Start in parallel: developer-core on Task 1, developer-ui on Task 5. UI Tasks 6/8 unblock when Task 3 lands (they test against `FakeNoteRepository`, so Task 4 is only needed for the manual checks in Task 10).

## File Structure

```text
app/
├── build.gradle.kts                                   (T2: schemaLocation, room-testing, androidTest assets)   [core]
├── schemas/com.quicknotes.app.data.local.database.AppDatabase/1.json, 2.json   (T2, generated)          [core]
├── src/main/java/com/quicknotes/app/
│   ├── domain/link/LinkParser.kt                      (T1 new: extract, normalize, titleIndex, renameLinks)  [core]
│   ├── domain/model/NoteRef.kt                        (T1 new: id + title)                                   [core]
│   ├── domain/model/NoteLink.kt                       (T3 new: NoteLink, NoteGraph)                          [core]
│   ├── domain/repository/NoteRepository.kt           (T3: observeOutgoingLinks/Backlinks/Graph)             [core]
│   ├── data/local/entity/NoteLinkEntity.kt            (T2 new)                                               [core]
│   ├── data/local/dao/NoteLinkDao.kt                  (T2 new, T3 reads, T4 writes)                          [core]
│   ├── data/local/database/AppDatabase.kt             (T2: v2, noteLinkDao, addMigrations)                   [core]
│   ├── data/local/database/Migrations.kt              (T2 new: MIGRATION_1_2)                                [core]
│   ├── data/repository/NoteRepositoryImpl.kt          (T3 reads, T4 saveNote)                                [core]
│   ├── ui/editor/EditorViewModel.kt                   (T6 links + prefillTitle, T7 picker)                   [ui]
│   ├── ui/editor/EditorScreen.kt                      (T6 section, T7 button + cursor)                       [ui]
│   ├── ui/editor/LinksSection.kt                      (T6 new)                                               [ui]
│   ├── ui/editor/LinkPickerSheet.kt                   (T7 new)                                               [ui]
│   ├── ui/graph/ForceLayout.kt                        (T5 new)                                               [ui]
│   ├── ui/graph/GraphModel.kt                         (T8 new)                                               [ui]
│   ├── ui/graph/GraphViewModel.kt                     (T8 new)                                               [ui]
│   ├── ui/graph/GraphScreen.kt                        (T9 new)                                               [ui]
│   ├── ui/ViewModelFactory.kt                         (T6 editor factory, T9 GraphViewModel)                 [ui]
│   └── ui/nav/QuickNotesNavHost.kt                    (T6 editor route, T9 graph tab)                        [ui]
├── src/test/java/com/quicknotes/app/
│   ├── domain/FakeNoteRepository.kt                   (T3: link/graph flows derived from content)            [core]
│   ├── domain/link/LinkParserTest.kt                  (T1 new)                                               [core]
│   ├── ui/editor/EditorViewModelTest.kt               (T6, T7)                                               [ui]
│   ├── ui/graph/ForceLayoutTest.kt                    (T5 new)                                               [ui]
│   └── ui/graph/GraphModelTest.kt                     (T8 new)                                               [ui]
└── src/androidTest/java/com/quicknotes/app/
    ├── data/local/MigrationTest.kt                    (T2 new)                                               [core]
    ├── data/repository/NoteRepositoryLinksTest.kt     (T3 new, T4 extends)                                   [core]
    ├── ui/editor/LinksSectionTest.kt                  (T6 new)                                               [ui]
    └── ui/nav/QuickNotesNavHostTest.kt                (T7 picker, T9 graph tab tests)                        [ui]
```

---

### Task 1: `LinkParser` (developer-core, sem dependências)

**Files:**
- Create: `app/src/main/java/com/quicknotes/app/domain/model/NoteRef.kt`
- Create: `app/src/main/java/com/quicknotes/app/domain/link/LinkParser.kt`
- Test: `app/src/test/java/com/quicknotes/app/domain/link/LinkParserTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `data class NoteRef(val id: Long, val title: String)` (package `com.quicknotes.app.domain.model`; Room maps `SELECT id, title` straight into it)
  - `object LinkParser` (package `com.quicknotes.app.domain.link`):
    - `data class LinkInfo(val title: String, val index: Int)`
    - `fun extractLinks(content: String): List<LinkInfo>`
    - `fun normalize(title: String): String`
    - `fun titleIndex(notes: List<NoteRef>): Map<String, Long>` (key = `normalize(title)`)
    - `fun renameLinks(content: String, oldTitle: String, newTitle: String): String`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.quicknotes.app.domain.link

import com.quicknotes.app.domain.link.LinkParser.LinkInfo
import com.quicknotes.app.domain.model.NoteRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkParserTest {
    @Test
    fun extractsLinksInOrderWithTrimmedTitles() {
        assertEquals(
            listOf(LinkInfo("Nota A", 0), LinkInfo("Outra", 1)),
            LinkParser.extractLinks("ver [[ Nota A ]] e depois [[Outra]]")
        )
    }

    @Test
    fun ignoresSingleBracketsAndBlankLinks() {
        // Blank links do not consume an index: indexes stay dense for the (sourceId, index) PK.
        assertEquals(listOf(LinkInfo("Real", 0)), LinkParser.extractLinks("[texto normal] [[  ]] [[Real]]"))
    }

    @Test
    fun noLinksGivesEmptyList() {
        assertTrue(LinkParser.extractLinks("sem ligações").isEmpty())
    }

    @Test
    fun keepsInnerSpacesAndDuplicatesInOrder() {
        assertEquals(
            listOf(LinkInfo("Plano de aula", 0), LinkInfo("plano de aula", 1)),
            LinkParser.extractLinks("[[Plano de aula]] e [[plano de aula]]")
        )
    }

    @Test
    fun linksDoNotSpanLines() {
        assertTrue(LinkParser.extractLinks("[[abre\nfecha]]").isEmpty())
    }

    @Test
    fun titleIndexMatchesCaseAndAccentsInsensitivelyAndPrefersOldestNote() {
        val index = LinkParser.titleIndex(
            listOf(NoteRef(5, "Reunião"), NoteRef(2, "reunião"), NoteRef(3, "   "))
        )

        assertEquals(2L, index[LinkParser.normalize("REUNIÃO")])
        assertEquals(1, index.size) // blank title never becomes a target
    }

    @Test
    fun renameRewritesOnlyMatchingLinks() {
        assertEquals(
            "ver [[Nova]] e [[Nova]] mas não [[Outra]] nem Antiga",
            LinkParser.renameLinks("ver [[Antiga]] e [[ antiga ]] mas não [[Outra]] nem Antiga", "Antiga", "Nova")
        )
    }

    @Test
    fun renameHandlesSpecialCharacters() {
        assertEquals(
            "x [[Nova (v2) \$1]] y",
            LinkParser.renameLinks("x [[C++ & 100% (rascunho)]] y", "C++ & 100% (rascunho)", "Nova (v2) \$1")
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.quicknotes.app.domain.link.LinkParserTest"`
Expected: FAIL — compilation error `Unresolved reference: LinkParser`.

- [ ] **Step 3: Write minimal implementation**

`app/src/main/java/com/quicknotes/app/domain/model/NoteRef.kt`:

```kotlin
package com.quicknotes.app.domain.model

/** Lightweight note reference (id + title) for links, backlinks, pickers and the graph. */
data class NoteRef(val id: Long, val title: String)
```

`app/src/main/java/com/quicknotes/app/domain/link/LinkParser.kt`:

```kotlin
package com.quicknotes.app.domain.link

import com.quicknotes.app.domain.model.NoteRef

/**
 * Extracts and rewrites `[[Título]]` links. Pure Kotlin so the repository, the Room
 * migration and the test fakes share exactly the same rules.
 */
object LinkParser {
    data class LinkInfo(val title: String, val index: Int)

    private val LINK = Regex("""\[\[(.*?)]]""")

    /** Links in order of appearance; blank `[[ ]]` are skipped and do not consume an index. */
    fun extractLinks(content: String): List<LinkInfo> =
        LINK.findAll(content)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { i, title -> LinkInfo(title, i) }
            .toList()

    /** Match key: trimmed + lowercase. Done in Kotlin because SQLite LIKE/NOCASE only fold ASCII (Ã, Ç...). */
    fun normalize(title: String): String = title.trim().lowercase()

    /** normalized title -> note id. Blank titles never resolve; on duplicate titles the oldest note (lowest id) wins. */
    fun titleIndex(notes: List<NoteRef>): Map<String, Long> = buildMap {
        for (note in notes.sortedBy { it.id }) {
            val key = normalize(note.title)
            if (key.isNotEmpty()) putIfAbsent(key, note.id)
        }
    }

    /** Rewrites every `[[oldTitle]]` (any case/padding) to `[[newTitle]]`; the replacement is literal (no `$` groups). */
    fun renameLinks(content: String, oldTitle: String, newTitle: String): String {
        val old = normalize(oldTitle)
        return LINK.replace(content) { match ->
            if (normalize(match.groupValues[1]) == old) "[[${newTitle.trim()}]]" else match.value
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.quicknotes.app.domain.link.LinkParserTest"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/quicknotes/app/domain/model/NoteRef.kt app/src/main/java/com/quicknotes/app/domain/link/LinkParser.kt app/src/test/java/com/quicknotes/app/domain/link/LinkParserTest.kt
git commit -m "feature[quick-notes-graph]: add LinkParser for [[title]] links"
```

---

### Task 2: Room v2 — `note_links` + `MIGRATION_1_2` com backfill (developer-core, depende de 1)

**Files:**
- Modify: `app/build.gradle.kts` (kapt `room.schemaLocation`, androidTest assets, `room-testing`)
- Create: `app/schemas/com.quicknotes.app.data.local.database.AppDatabase/1.json` (generated at v1, Step 1)
- Create: `app/schemas/com.quicknotes.app.data.local.database.AppDatabase/2.json` (generated at v2, Step 6)
- Create: `app/src/main/java/com/quicknotes/app/data/local/entity/NoteLinkEntity.kt`
- Create: `app/src/main/java/com/quicknotes/app/data/local/dao/NoteLinkDao.kt`
- Create: `app/src/main/java/com/quicknotes/app/data/local/database/Migrations.kt`
- Modify: `app/src/main/java/com/quicknotes/app/data/local/database/AppDatabase.kt`
- Test: `app/src/androidTest/java/com/quicknotes/app/data/local/MigrationTest.kt`

**Interfaces:**
- Consumes: `LinkParser.extractLinks`, `LinkParser.normalize`, `LinkParser.titleIndex`, `NoteRef` (Task 1).
- Produces:
  - `data class NoteLinkEntity(val sourceId: Long, val index: Int, val targetId: Long?, val targetTitle: String)` — table `note_links`, PK `(sourceId, index)`, index on `targetId`. `index` is an SQL keyword: every hand-written query must quote it as `` `index` ``.
  - `interface NoteLinkDao` with `suspend fun insertAll(links: List<NoteLinkEntity>)` and `suspend fun getForSource(sourceId: Long): List<NoteLinkEntity>` (Tasks 3/4 add more queries to this same DAO).
  - `val MIGRATION_1_2: Migration` (package `com.quicknotes.app.data.local.database`).
  - `AppDatabase.noteLinkDao(): NoteLinkDao`; `AppDatabase` is `version = 2`, `exportSchema = true`.

- [ ] **Step 1: Export the v1 schema before touching any entity**

`MigrationTestHelper` needs `1.json`, and it can only be generated while `AppDatabase` is still at version 1. Edit `app/build.gradle.kts`:

```kotlin
android {
    // ...existing config...
    sourceSets {
        // MigrationTestHelper reads the exported schemas as androidTest assets.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

kapt {
    arguments { arg("room.schemaLocation", "$projectDir/schemas") }
}
```

and in `dependencies { }` next to the other `androidTestImplementation` lines:

```kotlin
    androidTestImplementation("androidx.room:room-testing:2.6.1")
```

Run: `./gradlew :app:kaptDebugKotlin`
Expected: BUILD SUCCESSFUL and `app/schemas/com.quicknotes.app.data.local.database.AppDatabase/1.json` exists with `"version": 1`. Do not edit that file by hand.

- [ ] **Step 2: Write the failing migration test**

`app/src/androidTest/java/com/quicknotes/app/data/local/MigrationTest.kt`:

```kotlin
package com.quicknotes.app.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quicknotes.app.data.local.database.AppDatabase
import com.quicknotes.app.data.local.database.MIGRATION_1_2
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val DB_NAME = "migration-test"

private fun SupportSQLiteDatabase.insertNote(id: Long, title: String, content: String) = execSQL(
    "INSERT INTO notes (id, title, content, createdAt, updatedAt, folderId, favorite, archived, inbox, captureSource) " +
        "VALUES (?, ?, ?, 1, 1, NULL, 0, 0, 1, 'APP')",
    arrayOf<Any?>(id, title, content)
)

private fun SupportSQLiteDatabase.linkRows(): List<List<Any?>> =
    query("SELECT sourceId, `index`, targetId, targetTitle FROM note_links ORDER BY sourceId, `index`").use { c ->
        buildList {
            while (c.moveToNext()) {
                add(listOf(c.getLong(0), c.getInt(1), if (c.isNull(2)) null else c.getLong(2), c.getString(3)))
            }
        }
    }

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate1To2PreservesNotesAndBackfillsLinksAndGhosts() {
        helper.createDatabase(DB_NAME, 1).apply {
            insertNote(1, "Reunião", "pauta")
            insertNote(2, "Plano", "ver [[REUNIÃO]] e [[Futura]] e [[ ]]")
            insertNote(3, "Solta", "sem links")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        db.query("SELECT id, title, content FROM notes ORDER BY id").use { c ->
            assertEquals(3, c.count)
            c.moveToPosition(1)
            assertEquals("Plano", c.getString(1))
            assertEquals("ver [[REUNIÃO]] e [[Futura]] e [[ ]]", c.getString(2))
        }
        // Accent/case-insensitive match resolves to note 1; unknown title becomes a ghost (targetId NULL);
        // the blank link is skipped and does not consume an index.
        assertEquals(
            listOf(listOf<Any?>(2L, 0, 1L, "REUNIÃO"), listOf<Any?>(2L, 1, null, "Futura")),
            db.linkRows()
        )
    }

    @Test
    fun migratedDatabaseEnforcesCascadeAndSetNullThroughRoom() {
        helper.createDatabase(DB_NAME, 1).apply {
            insertNote(1, "Alvo", "")
            insertNote(2, "Fonte", "[[Alvo]]")
            close()
        }
        val room = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking { room.noteDao().delete(1) }
            assertEquals(listOf(listOf<Any?>(2L, 0, null, "Alvo")), room.openHelper.readableDatabase.linkRows())

            runBlocking { room.noteDao().delete(2) }
            assertTrue(room.openHelper.readableDatabase.linkRows().isEmpty())
        } finally {
            room.close()
        }
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.data.local.MigrationTest`
Expected: FAIL — compilation error `Unresolved reference: MIGRATION_1_2`.

- [ ] **Step 4: Add the entity and DAO**

`app/src/main/java/com/quicknotes/app/data/local/entity/NoteLinkEntity.kt`:

```kotlin
package com.quicknotes.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** One `[[Título]]` occurrence. targetId == null means "fantasma" (no note with that title yet). */
@Entity(
    tableName = "note_links",
    primaryKeys = ["sourceId", "index"],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["targetId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("targetId")]
)
data class NoteLinkEntity(
    val sourceId: Long,
    val index: Int,
    val targetId: Long?,
    val targetTitle: String
)
```

`app/src/main/java/com/quicknotes/app/data/local/dao/NoteLinkDao.kt`:

```kotlin
package com.quicknotes.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.quicknotes.app.data.local.entity.NoteLinkEntity

@Dao
interface NoteLinkDao {
    // Plain @Insert (ABORT): (sourceId, index) is always rewritten after deleteForSource, never replaced.
    @Insert
    suspend fun insertAll(links: List<NoteLinkEntity>)

    @Query("SELECT * FROM note_links WHERE sourceId = :sourceId ORDER BY `index`")
    suspend fun getForSource(sourceId: Long): List<NoteLinkEntity>
}
```

- [ ] **Step 5: Write the migration and bump the database**

`app/src/main/java/com/quicknotes/app/data/local/database/Migrations.kt`:

```kotlin
package com.quicknotes.app.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.quicknotes.app.domain.link.LinkParser
import com.quicknotes.app.domain.model.NoteRef

/**
 * v1 -> v2: creates note_links and backfills it from every note's content. Title matching runs in
 * Kotlin (LinkParser) because SQLite LIKE/NOCASE don't fold non-ASCII letters (Ã, Ç).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Must match "createSql" in schemas/.../2.json; runMigrationsAndValidate fails on any drift.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `note_links` (`sourceId` INTEGER NOT NULL, `index` INTEGER NOT NULL, " +
                "`targetId` INTEGER, `targetTitle` TEXT NOT NULL, PRIMARY KEY(`sourceId`, `index`), " +
                "FOREIGN KEY(`sourceId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`targetId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_links_targetId` ON `note_links` (`targetId`)")

        val notes = mutableListOf<Triple<Long, String, String>>()
        db.query("SELECT id, title, content FROM notes").use { c ->
            while (c.moveToNext()) notes += Triple(c.getLong(0), c.getString(1), c.getString(2))
        }
        val titles = LinkParser.titleIndex(notes.map { NoteRef(it.first, it.second) })
        for ((id, _, content) in notes) {
            for (link in LinkParser.extractLinks(content)) {
                db.execSQL(
                    "INSERT INTO note_links (sourceId, `index`, targetId, targetTitle) VALUES (?, ?, ?, ?)",
                    arrayOf<Any?>(id, link.index, titles[LinkParser.normalize(link.title)], link.title)
                )
            }
        }
    }
}
```

In `AppDatabase.kt` add the import `com.quicknotes.app.data.local.dao.NoteLinkDao` and `com.quicknotes.app.data.local.entity.NoteLinkEntity`, then change the annotation, add the DAO accessor and register the migration:

```kotlin
@Database(
    entities = [NoteEntity::class, TagEntity::class, FolderEntity::class, NoteTagEntity::class, NoteLinkEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun folderDao(): FolderDao
    abstract fun noteLinkDao(): NoteLinkDao

    companion object {
        // Never fallbackToDestructiveMigration: user notes must survive every upgrade.
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "quicknotes.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
```

- [ ] **Step 6: Generate the v2 schema and check the SQL**

Run: `./gradlew :app:kaptDebugKotlin`
Expected: BUILD SUCCESSFUL and `2.json` exists. Compare its `note_links` `createSql` (with `${TABLE_NAME}` read as `note_links`) against the string in `MIGRATION_1_2`; if they differ, copy Room's version into the migration.

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.data.local.MigrationTest`
Expected: PASS (2 tests). Then the whole instrumented suite, since every in-memory DB now has the new table: `./gradlew :app:connectedDebugAndroidTest` → PASS.

- [ ] **Step 8: Commit**

```bash
git add app/build.gradle.kts app/schemas app/src/main/java/com/quicknotes/app/data/local/entity/NoteLinkEntity.kt app/src/main/java/com/quicknotes/app/data/local/dao/NoteLinkDao.kt app/src/main/java/com/quicknotes/app/data/local/database/Migrations.kt app/src/main/java/com/quicknotes/app/data/local/database/AppDatabase.kt app/src/androidTest/java/com/quicknotes/app/data/local/MigrationTest.kt
git commit -m "feature[quick-notes-graph]: add note_links table and v1->v2 migration with backfill"
```

---

### Task 3: API de leitura de links (developer-core, depende de 2)

**Files:**
- Create: `app/src/main/java/com/quicknotes/app/domain/model/NoteLink.kt`
- Modify: `app/src/main/java/com/quicknotes/app/domain/repository/NoteRepository.kt`
- Modify: `app/src/main/java/com/quicknotes/app/data/local/dao/NoteLinkDao.kt` (observe queries)
- Modify: `app/src/main/java/com/quicknotes/app/data/repository/NoteRepositoryImpl.kt` (reads only; `saveNote` is Task 4)
- Modify: `app/src/test/java/com/quicknotes/app/domain/FakeNoteRepository.kt`
- Test: `app/src/androidTest/java/com/quicknotes/app/data/repository/NoteRepositoryLinksTest.kt`

**Interfaces:**
- Consumes: `NoteLinkEntity`, `NoteLinkDao.insertAll`, `AppDatabase.noteLinkDao()` (Task 2); `NoteRef`, `LinkParser` (Task 1).
- Produces (package `com.quicknotes.app.domain.model`):
  - `data class NoteLink(val sourceId: Long, val index: Int, val targetId: Long?, val targetTitle: String)` with `val isGhost: Boolean get() = targetId == null`
  - `data class NoteTagRef(val noteId: Long, val tagId: Long)`
  - `data class NoteGraph(val notes: List<NoteRef> = emptyList(), val links: List<NoteLink> = emptyList(), val tags: List<Tag> = emptyList(), val noteTags: List<NoteTagRef> = emptyList())`
- Produces (`NoteRepository`):
  - `fun observeOutgoingLinks(noteId: Long): Flow<List<NoteLink>>` — ordered by `index`, ghosts included.
  - `fun observeBacklinks(noteId: Long): Flow<List<NoteRef>>` — distinct source notes linking to `noteId`, self excluded, ordered by id.
  - `fun observeGraph(): Flow<NoteGraph>` — all notes (archived included), all links, all tags, all note–tag pairs. Consumers must drop pairs whose note/tag no longer exists (`note_tags` has no FKs).
- Produces (test): `FakeNoteRepository.tags: MutableStateFlow<List<Tag>>`; the fake derives links from note content with `LinkParser`, so a deleted target shows up as a ghost automatically.

- [ ] **Step 1: Write the failing test**

`app/src/androidTest/java/com/quicknotes/app/data/repository/NoteRepositoryLinksTest.kt`:

```kotlin
package com.quicknotes.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicknotes.app.data.local.database.AppDatabase
import com.quicknotes.app.data.local.entity.NoteEntity
import com.quicknotes.app.data.local.entity.NoteLinkEntity
import com.quicknotes.app.data.local.entity.NoteTagEntity
import com.quicknotes.app.data.local.entity.TagEntity
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.domain.model.NoteTagRef
import com.quicknotes.app.domain.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteRepositoryLinksTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: NoteRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NoteRepositoryImpl(db)
    }

    @After
    fun tearDown() { db.close() }

    private suspend fun rawNote(title: String, content: String = ""): Long = db.noteDao().insert(
        NoteEntity(title = title, content = content, createdAt = 1, updatedAt = 1, folderId = null,
            favorite = false, archived = false, inbox = true, captureSource = "APP")
    )

    @Test
    fun outgoingLinksAreOrderedAndBacklinksAreDistinctWithoutSelf() = runBlocking {
        val a = rawNote("A")
        val b = rawNote("B")
        db.noteLinkDao().insertAll(listOf(
            NoteLinkEntity(b, 2, a, "a"),
            NoteLinkEntity(b, 0, a, "A"),
            NoteLinkEntity(b, 1, null, "Futura"),
            NoteLinkEntity(a, 0, a, "A") // self link
        ))

        assertEquals(
            listOf(NoteLink(b, 0, a, "A"), NoteLink(b, 1, null, "Futura"), NoteLink(b, 2, a, "a")),
            repository.observeOutgoingLinks(b).first()
        )
        assertEquals(listOf(NoteRef(b, "B")), repository.observeBacklinks(a).first())
        assertEquals(true, repository.observeOutgoingLinks(b).first()[1].isGhost)
    }

    @Test
    fun graphCombinesNotesLinksTagsAndNoteTags() = runBlocking {
        val a = rawNote("A")
        val b = rawNote("B")
        val tag = db.tagDao().insert(TagEntity(name = "ideia"))
        db.noteDao().insertNoteTags(listOf(NoteTagEntity(a, tag)))
        db.noteLinkDao().insertAll(listOf(NoteLinkEntity(b, 0, a, "A")))

        val graph = repository.observeGraph().first()

        assertEquals(listOf(NoteRef(a, "A"), NoteRef(b, "B")), graph.notes.sortedBy { it.id })
        assertEquals(listOf(NoteLink(b, 0, a, "A")), graph.links)
        assertEquals(listOf(Tag(tag, "ideia")), graph.tags)
        assertEquals(listOf(NoteTagRef(a, tag)), graph.noteTags)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.data.repository.NoteRepositoryLinksTest`
Expected: FAIL — compilation error `Unresolved reference: NoteLink`.

- [ ] **Step 3: Add the domain models and repository contract**

`app/src/main/java/com/quicknotes/app/domain/model/NoteLink.kt`:

```kotlin
package com.quicknotes.app.domain.model

/** A `[[targetTitle]]` in note `sourceId`, at position `index`. targetId == null -> fantasma. */
data class NoteLink(val sourceId: Long, val index: Int, val targetId: Long?, val targetTitle: String) {
    val isGhost: Boolean get() = targetId == null
}

data class NoteTagRef(val noteId: Long, val tagId: Long)

/** Raw graph snapshot; the UI decides what becomes a node (folders never do). */
data class NoteGraph(
    val notes: List<NoteRef> = emptyList(),
    val links: List<NoteLink> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val noteTags: List<NoteTagRef> = emptyList()
)
```

Append to `interface NoteRepository` (add imports `NoteGraph`, `NoteLink`, `NoteRef`):

```kotlin
    fun observeOutgoingLinks(noteId: Long): Flow<List<NoteLink>>
    fun observeBacklinks(noteId: Long): Flow<List<NoteRef>>
    fun observeGraph(): Flow<NoteGraph>
```

- [ ] **Step 4: Add the observe queries to `NoteLinkDao`**

Add imports `com.quicknotes.app.domain.model.NoteLink`, `NoteRef`, `NoteTagRef`, `kotlinx.coroutines.flow.Flow`, then inside the interface:

```kotlin
    @Query("SELECT sourceId, `index`, targetId, targetTitle FROM note_links WHERE sourceId = :noteId ORDER BY `index`")
    fun observeOutgoing(noteId: Long): Flow<List<NoteLink>>

    @Query(
        """
        SELECT DISTINCT n.id, n.title FROM note_links l
        JOIN notes n ON n.id = l.sourceId
        WHERE l.targetId = :noteId AND l.sourceId != :noteId
        ORDER BY n.id
        """
    )
    fun observeBacklinks(noteId: Long): Flow<List<NoteRef>>

    @Query("SELECT id, title FROM notes")
    fun observeNoteRefs(): Flow<List<NoteRef>>

    @Query("SELECT sourceId, `index`, targetId, targetTitle FROM note_links ORDER BY sourceId, `index`")
    fun observeAll(): Flow<List<NoteLink>>

    @Query("SELECT noteId, tagId FROM note_tags")
    fun observeNoteTags(): Flow<List<NoteTagRef>>
```

- [ ] **Step 5: Implement the reads in `NoteRepositoryImpl`**

Add the constructor parameter `private val linkDao: NoteLinkDao = database.noteLinkDao()` after `noteDao`, imports (`NoteLinkDao`, `NoteGraph`, `NoteLink`, `NoteRef`, `Tag`, `kotlinx.coroutines.flow.combine`) and:

```kotlin
    override fun observeOutgoingLinks(noteId: Long): Flow<List<NoteLink>> = linkDao.observeOutgoing(noteId)
    override fun observeBacklinks(noteId: Long): Flow<List<NoteRef>> = linkDao.observeBacklinks(noteId)
    override fun observeGraph(): Flow<NoteGraph> = combine(
        linkDao.observeNoteRefs(), linkDao.observeAll(), database.tagDao().observeAll(), linkDao.observeNoteTags()
    ) { notes, links, tags, noteTags -> NoteGraph(notes, links, tags.map { Tag(it.id, it.name) }, noteTags) }
```

- [ ] **Step 6: Teach `FakeNoteRepository` the same contract**

Add imports (`LinkParser`, `NoteGraph`, `NoteLink`, `NoteRef`, `NoteTagRef`, `Tag`, `kotlinx.coroutines.flow.combine`) and:

```kotlin
    val tags = MutableStateFlow<List<Tag>>(emptyList())

    // Derived from content on every emission, so deleting a target turns its links into ghosts.
    private fun resolvedLinks(all: List<Note>): List<NoteLink> {
        val titles = LinkParser.titleIndex(all.map { NoteRef(it.id, it.title) })
        return all.sortedBy { it.id }.flatMap { note ->
            LinkParser.extractLinks(note.content).map { NoteLink(note.id, it.index, titles[LinkParser.normalize(it.title)], it.title) }
        }
    }

    override fun observeOutgoingLinks(noteId: Long): Flow<List<NoteLink>> =
        notes.map { all -> resolvedLinks(all).filter { it.sourceId == noteId } }

    override fun observeBacklinks(noteId: Long): Flow<List<NoteRef>> = notes.map { all ->
        val sources = resolvedLinks(all).filter { it.targetId == noteId && it.sourceId != noteId }.map { it.sourceId }.toSet()
        all.filter { it.id in sources }.sortedBy { it.id }.map { NoteRef(it.id, it.title) }
    }

    override fun observeGraph(): Flow<NoteGraph> = combine(notes, tags) { all, allTags ->
        NoteGraph(
            notes = all.map { NoteRef(it.id, it.title) },
            links = resolvedLinks(all),
            tags = allTags,
            noteTags = all.flatMap { note -> note.tagIds.map { NoteTagRef(note.id, it) } }
        )
    }
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.data.repository.NoteRepositoryLinksTest`
Expected: PASS (2 tests).
Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS (fake still compiles against the widened interface).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/quicknotes/app/domain/model/NoteLink.kt app/src/main/java/com/quicknotes/app/domain/repository/NoteRepository.kt app/src/main/java/com/quicknotes/app/data/local/dao/NoteLinkDao.kt app/src/main/java/com/quicknotes/app/data/repository/NoteRepositoryImpl.kt app/src/test/java/com/quicknotes/app/domain/FakeNoteRepository.kt app/src/androidTest/java/com/quicknotes/app/data/repository/NoteRepositoryLinksTest.kt
git commit -m "feature[quick-notes-graph]: expose outgoing links, backlinks and graph snapshot"
```

---

### Task 4: Manutenção de links no `saveNote` (developer-core, depende de 3)

**Files:**
- Modify: `app/src/main/java/com/quicknotes/app/data/local/dao/NoteLinkDao.kt` (write/resolve queries)
- Modify: `app/src/main/java/com/quicknotes/app/data/repository/NoteRepositoryImpl.kt` (`saveNote` + private helpers)
- Test: `app/src/androidTest/java/com/quicknotes/app/data/repository/NoteRepositoryLinksTest.kt` (extend)

**Interfaces:**
- Consumes: `NoteLinkDao.insertAll/getForSource`, `NoteLinkEntity` (Task 2); `observeBacklinks`, `linkDao` constructor param (Task 3); `LinkParser.*` (Task 1).
- Produces: no new public API. `saveNote(note)` now guarantees, inside its single `withTransaction`:
  1. the note's own `note_links` rows equal `extractLinks(content)` resolved against current titles;
  2. if the trimmed title changed from a non-blank value: non-blank new title → every note linking to it gets `[[Old]]` rewritten to `[[New]]` (content only, `updatedAt` untouched) and its links recomputed; blank new title → text untouched, links to it become ghosts;
  3. every ghost whose `targetTitle` now matches a title is resolved.
  `deleteNote` is unchanged: CASCADE/SET NULL do the work (Room turns on `PRAGMA foreign_keys` for DBs with FKs).
- New `NoteLinkDao` methods: `deleteForSource(sourceId: Long)`, `getNoteRefs(): List<NoteRef>`, `getGhostLinks(): List<NoteLinkEntity>`, `setTarget(sourceId: Long, index: Int, targetId: Long)`, `getSourceIdsLinkingTo(targetId: Long): List<Long>`, `clearTarget(targetId: Long)` — all `suspend`.

- [ ] **Step 1: Write the failing tests**

Add to `NoteRepositoryLinksTest` (imports `com.quicknotes.app.domain.model.CaptureSource`, `com.quicknotes.app.domain.model.Note`, `org.junit.Assert.assertTrue`):

```kotlin
    private fun note(title: String, content: String = "", id: Long = 0) = Note(
        id = id, title = title, content = content, createdAt = 1, updatedAt = 1, folderId = null,
        favorite = false, archived = false, inbox = true, captureSource = CaptureSource.APP
    )

    private suspend fun links(sourceId: Long) = db.noteLinkDao().getForSource(sourceId)

    @Test
    fun savingStoresOrderedLinksResolvedCaseAndAccentInsensitively() = runBlocking {
        val reuniao = repository.saveNote(note("Reunião"))
        val src = repository.saveNote(note("Diário", "[[REUNIÃO]] depois [[Futura]]"))

        assertEquals(listOf(NoteLinkEntity(src, 0, reuniao, "REUNIÃO"), NoteLinkEntity(src, 1, null, "Futura")), links(src))

        repository.saveNote(note("Diário", "só [[ reunião ]]", id = src))
        assertEquals(listOf(NoteLinkEntity(src, 0, reuniao, "reunião")), links(src))
    }

    @Test
    fun creatingNoteResolvesPendingGhosts() = runBlocking {
        val src = repository.saveNote(note("Diário", "[[Futura Nota]]"))
        assertEquals(null, links(src).single().targetId)

        val futura = repository.saveNote(note("  futura NOTA "))

        assertEquals(futura, links(src).single().targetId)
        assertEquals(listOf(NoteRef(src, "Diário")), repository.observeBacklinks(futura).first())
    }

    @Test
    fun renamePropagatesToLinkingNotesWithSpecialCharacters() = runBlocking {
        val alvo = repository.saveNote(note("C++ & 100% (rascunho)"))
        val a = repository.saveNote(note("A", "ver [[c++ & 100% (rascunho)]] e [[Outra]] e C++ & 100% (rascunho)"))

        repository.saveNote(note("Nova (v2) \$1", id = alvo))

        assertEquals("ver [[Nova (v2) \$1]] e [[Outra]] e C++ & 100% (rascunho)", repository.getNote(a)!!.content)
        assertEquals(1L, repository.getNote(a)!!.updatedAt) // a rename is not an edit of the linking note
        assertEquals(listOf(NoteLinkEntity(a, 0, alvo, "Nova (v2) \$1"), NoteLinkEntity(a, 1, null, "Outra")), links(a))
        assertEquals(listOf(NoteRef(a, "A")), repository.observeBacklinks(alvo).first())
    }

    @Test
    fun blankTitlesNeverResolveAndBlankRenameKeepsText() = runBlocking {
        repository.saveNote(note("", "captura rápida sem título"))
        val src = repository.saveNote(note("A", "[[ ]] [[Alvo]]"))
        assertEquals(listOf(NoteLinkEntity(src, 0, null, "Alvo")), links(src))

        val alvo = repository.saveNote(note("Alvo"))
        assertEquals(alvo, links(src).single().targetId)

        repository.saveNote(note("   ", id = alvo))

        assertEquals("[[ ]] [[Alvo]]", repository.getNote(src)!!.content)
        assertEquals(listOf(NoteLinkEntity(src, 0, null, "Alvo")), links(src))
    }

    @Test
    fun deletingTargetTurnsLinksIntoGhostsAndSourceDeleteRemovesLinks() = runBlocking {
        val alvo = repository.saveNote(note("Alvo"))
        val src = repository.saveNote(note("Fonte", "[[Alvo]]"))

        repository.deleteNote(alvo)
        assertEquals(listOf(NoteLinkEntity(src, 0, null, "Alvo")), links(src))

        repository.deleteNote(src)
        assertTrue(links(src).isEmpty())
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.data.repository.NoteRepositoryLinksTest`
Expected: FAIL — the four `saveNote`-based tests fail (e.g. `expected:<[NoteLinkEntity(...)]> but was:<[]>`); `deletingTarget…` fails on its first assertion for the same reason.

- [ ] **Step 3: Add the write/resolve queries to `NoteLinkDao`**

```kotlin
    @Query("DELETE FROM note_links WHERE sourceId = :sourceId")
    suspend fun deleteForSource(sourceId: Long)

    @Query("SELECT id, title FROM notes")
    suspend fun getNoteRefs(): List<NoteRef>

    @Query("SELECT * FROM note_links WHERE targetId IS NULL")
    suspend fun getGhostLinks(): List<NoteLinkEntity>

    @Query("UPDATE note_links SET targetId = :targetId WHERE sourceId = :sourceId AND `index` = :index")
    suspend fun setTarget(sourceId: Long, index: Int, targetId: Long)

    @Query("SELECT DISTINCT sourceId FROM note_links WHERE targetId = :targetId")
    suspend fun getSourceIdsLinkingTo(targetId: Long): List<Long>

    @Query("UPDATE note_links SET targetId = NULL WHERE targetId = :targetId")
    suspend fun clearTarget(targetId: Long)
```

- [ ] **Step 4: Rewrite `saveNote` in `NoteRepositoryImpl`**

Add imports `com.quicknotes.app.data.local.entity.NoteLinkEntity` and `com.quicknotes.app.domain.link.LinkParser`. Replace the whole `saveNote` override with:

```kotlin
    override suspend fun saveNote(note: Note): Long = database.withTransaction {
        val previousTitle = if (note.id == 0L) null else noteDao.getById(note.id)?.title
        val entity = NoteEntity(
            id = note.id, title = note.title, content = note.content,
            createdAt = note.createdAt, updatedAt = note.updatedAt, folderId = note.folderId,
            favorite = note.favorite, archived = note.archived, inbox = note.inbox,
            captureSource = note.captureSource.name
        )
        // @Update, never REPLACE: REPLACE deletes the row and CASCADE would wipe its links.
        val id = if (note.id == 0L) noteDao.insert(entity) else { noteDao.update(entity); note.id }
        noteDao.clearNoteTags(id)
        if (note.tagIds.isNotEmpty()) {
            noteDao.insertNoteTags(note.tagIds.map { NoteTagEntity(id, it) })
        }

        val titles = LinkParser.titleIndex(linkDao.getNoteRefs())
        writeLinks(id, note.content, titles)
        if (!previousTitle.isNullOrBlank() && previousTitle.trim() != note.title.trim()) {
            if (note.title.isBlank()) linkDao.clearTarget(id) // keep "[[Old]]" text, links become ghosts
            else propagateRename(id, previousTitle, note.title, titles)
        }
        resolveGhosts(titles)
        id
    }

    private suspend fun writeLinks(sourceId: Long, content: String, titles: Map<String, Long>) {
        linkDao.deleteForSource(sourceId)
        linkDao.insertAll(
            LinkParser.extractLinks(content).map {
                NoteLinkEntity(sourceId, it.index, titles[LinkParser.normalize(it.title)], it.title)
            }
        )
    }

    private suspend fun propagateRename(targetId: Long, oldTitle: String, newTitle: String, titles: Map<String, Long>) {
        for (sourceId in linkDao.getSourceIdsLinkingTo(targetId)) {
            val source = noteDao.getById(sourceId) ?: continue
            val renamed = LinkParser.renameLinks(source.content, oldTitle, newTitle)
            // updatedAt untouched: the linking note wasn't edited by its user.
            if (renamed != source.content) noteDao.update(source.copy(content = renamed))
            writeLinks(sourceId, renamed, titles)
        }
    }

    // ponytail: scans every ghost on each save; fine for a personal notes DB, index targetTitle_normalized if it ever shows up in profiling.
    private suspend fun resolveGhosts(titles: Map<String, Long>) {
        for (ghost in linkDao.getGhostLinks()) {
            titles[LinkParser.normalize(ghost.targetTitle)]?.let { linkDao.setTarget(ghost.sourceId, ghost.index, it) }
        }
    }
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.data.repository.NoteRepositoryLinksTest`
Expected: PASS (7 tests).
Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.data.repository.NoteRepositoryImplTest`
Expected: PASS (MVP save/tag behavior unchanged).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/quicknotes/app/data/local/dao/NoteLinkDao.kt app/src/main/java/com/quicknotes/app/data/repository/NoteRepositoryImpl.kt app/src/androidTest/java/com/quicknotes/app/data/repository/NoteRepositoryLinksTest.kt
git commit -m "feature[quick-notes-graph]: recompute links on save, resolve ghosts and propagate renames"
```

---

### Task 5: `ForceLayout` (developer-ui, sem dependências)

**Files:**
- Create: `app/src/main/java/com/quicknotes/app/ui/graph/ForceLayout.kt`
- Test: `app/src/test/java/com/quicknotes/app/ui/graph/ForceLayoutTest.kt`

**Interfaces:**
- Consumes: nothing (pure Kotlin, no Android types, so it runs in JVM unit tests).
- Produces (package `com.quicknotes.app.ui.graph`):
  - `class ForceLayout` with nested `class Body(var x: Float, var y: Float)` (`vx`, `vy`, `pinned`)
  - `val keys: Set<String>`, `fun body(key: String): Body?`
  - `fun sync(nodeKeys: List<String>, edgeKeys: List<Pair<String, String>>)` — keeps positions of surviving keys
  - `fun pin(key: String, x: Float, y: Float)`, `fun release(key: String)`
  - `fun step(iterations: Int = 1, restSpeed: Float = 0.05f, isActive: (Body) -> Boolean = { true }): Boolean` — `true` while still moving
  - World units = px at zoom 1, origin = canvas center.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.quicknotes.app.ui.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class ForceLayoutTest {
    private fun ForceLayout.dist(a: String, b: String): Float {
        val p = body(a)!!; val q = body(b)!!
        return hypot(p.x - q.x, p.y - q.y)
    }

    @Test
    fun unconnectedNodesRepel() {
        val layout = ForceLayout().apply { sync(listOf("a", "b"), emptyList()) }
        val before = layout.dist("a", "b")
        layout.step(iterations = 20)
        assertTrue(layout.dist("a", "b") > before)
    }

    @Test
    fun connectedPairEndsCloserThanUnconnectedAndSettles() {
        val layout = ForceLayout().apply { sync(listOf("a", "b", "c"), listOf("a" to "b")) }
        layout.step(iterations = 2000)
        assertTrue(layout.dist("a", "b") < layout.dist("a", "c"))
        assertTrue(layout.dist("a", "b") < layout.dist("b", "c"))
        assertFalse(layout.step())
    }

    @Test
    fun coincidentNodesDoNotProduceNaN() {
        val layout = ForceLayout().apply { sync(listOf("a", "b", "c"), listOf("a" to "a", "a" to "b")) }
        listOf("a", "b", "c").forEach { layout.pin(it, 0f, 0f); layout.release(it) }
        layout.step(iterations = 10)
        layout.keys.forEach { key ->
            val body = layout.body(key)!!
            assertTrue(body.x.isFinite() && body.y.isFinite())
        }
        assertTrue(layout.dist("a", "b") > 0f)
    }

    @Test
    fun syncKeepsSurvivorsAndDropsRemovedKeys() {
        val layout = ForceLayout().apply { sync(listOf("a", "b"), emptyList()) }
        layout.pin("a", 123f, -45f)
        layout.sync(listOf("a", "c"), listOf("a" to "b")) // edge to a removed key is ignored
        assertEquals(setOf("a", "c"), layout.keys)
        assertEquals(123f, layout.body("a")!!.x)
        assertEquals(-45f, layout.body("a")!!.y)
    }

    @Test
    fun pinnedAndInactiveBodiesDoNotMove() {
        val layout = ForceLayout().apply { sync(listOf("a", "b", "c"), listOf("a" to "b")) }
        layout.pin("a", 10f, 10f)
        val c = layout.body("c")!!
        val cx = c.x; val cy = c.y
        layout.step(iterations = 50, isActive = { it !== c })
        assertEquals(10f, layout.body("a")!!.x)
        assertEquals(cx, c.x); assertEquals(cy, c.y)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.quicknotes.app.ui.graph.ForceLayoutTest"`
Expected: FAIL — compilation error `Unresolved reference: ForceLayout`.

- [ ] **Step 3: Write minimal implementation**

`app/src/main/java/com/quicknotes/app/ui/graph/ForceLayout.kt`:

```kotlin
package com.quicknotes.app.ui.graph

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Force-directed layout: Coulomb repulsion between every pair, Hooke springs on edges,
 * a weak pull to the origin and viscous damping. Held by GraphViewModel so positions
 * survive navigation. Tuning knobs are the constructor params.
 */
class ForceLayout(
    private val repulsion: Float = 12_000f,
    private val springLength: Float = 110f,
    private val springStrength: Float = 0.04f,
    private val gravity: Float = 0.01f,
    private val damping: Float = 0.85f,
    private val maxSpeed: Float = 40f
) {
    class Body(var x: Float, var y: Float) {
        var vx = 0f
        var vy = 0f
        var pinned = false
    }

    private val bodies = LinkedHashMap<String, Body>()
    private var edges: List<Pair<String, String>> = emptyList()

    val keys: Set<String> get() = bodies.keys
    fun body(key: String): Body? = bodies[key]

    /** Keeps positions of surviving keys, drops removed ones, seeds new ones on a golden-angle spiral. */
    fun sync(nodeKeys: List<String>, edgeKeys: List<Pair<String, String>>) {
        bodies.keys.retainAll(nodeKeys.toSet())
        for (key in nodeKeys) {
            if (key !in bodies) {
                val i = bodies.size
                val r = 40f * sqrt(i + 1f)
                val a = i * 2.39996f
                bodies[key] = Body(r * cos(a), r * sin(a))
            }
        }
        edges = edgeKeys.filter { (a, b) -> a != b && a in bodies && b in bodies }
    }

    fun pin(key: String, x: Float, y: Float) {
        bodies[key]?.apply { this.x = x; this.y = y; vx = 0f; vy = 0f; pinned = true }
    }

    fun release(key: String) { bodies[key]?.pinned = false }

    /**
     * Runs up to [iterations] steps (the screen caps this at 10 per frame). Bodies for which
     * [isActive] is false (off-screen culling) or that are pinned are not moved, but still push/pull.
     * ponytail: O(n²) repulsion, fine to ~300 nodes; Barnes-Hut if graphs grow beyond that.
     */
    fun step(iterations: Int = 1, restSpeed: Float = 0.05f, isActive: (Body) -> Boolean = { true }): Boolean {
        val list = bodies.values.toList()
        val indexOf = bodies.keys.withIndex().associate { it.value to it.index }
        val edgeIdx = edges.map { (a, b) -> indexOf.getValue(a) to indexOf.getValue(b) }
        var moving = false
        repeat(iterations) {
            val fx = FloatArray(list.size)
            val fy = FloatArray(list.size)
            for (i in list.indices) for (j in i + 1 until list.size) {
                var dx = list[i].x - list[j].x
                var dy = list[i].y - list[j].y
                var d2 = dx * dx + dy * dy
                if (d2 < 0.01f) { // coincident bodies: deterministic nudge instead of dividing by ~0
                    val a = (i * 31 + j) * 0.7f
                    dx = cos(a); dy = sin(a); d2 = 1f
                }
                val d = sqrt(d2)
                val f = repulsion / d2
                fx[i] += f * dx / d; fy[i] += f * dy / d
                fx[j] -= f * dx / d; fy[j] -= f * dy / d
            }
            for ((i, j) in edgeIdx) {
                val dx = list[j].x - list[i].x
                val dy = list[j].y - list[i].y
                val d = sqrt(maxOf(dx * dx + dy * dy, 0.01f))
                val f = springStrength * (d - springLength)
                fx[i] += f * dx / d; fy[i] += f * dy / d
                fx[j] -= f * dx / d; fy[j] -= f * dy / d
            }
            moving = false
            for ((i, body) in list.withIndex()) {
                if (body.pinned || !isActive(body)) { body.vx = 0f; body.vy = 0f; continue }
                body.vx = (body.vx + fx[i] - gravity * body.x) * damping
                body.vy = (body.vy + fy[i] - gravity * body.y) * damping
                val speed = sqrt(body.vx * body.vx + body.vy * body.vy)
                if (speed > maxSpeed) { body.vx *= maxSpeed / speed; body.vy *= maxSpeed / speed }
                body.x += body.vx
                body.y += body.vy
                if (speed > restSpeed) moving = true
            }
        }
        return moving
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.quicknotes.app.ui.graph.ForceLayoutTest"`
Expected: PASS (5 tests). If `connectedPairEndsCloserThanUnconnectedAndSettles` fails only on `assertFalse(layout.step())`, raise `damping` friction (lower the value, e.g. `0.8f`) rather than loosening the test.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/quicknotes/app/ui/graph/ForceLayout.kt app/src/test/java/com/quicknotes/app/ui/graph/ForceLayoutTest.kt
git commit -m "feature[quick-notes-graph]: add force-directed layout simulation"
```

---

### Task 6: Seção "Ligações" + rota `prefillTitle` (developer-ui, depende de 3)

**Files:**
- Modify: `app/src/main/java/com/quicknotes/app/ui/editor/EditorViewModel.kt` (`prefillTitle`, link flows)
- Create: `app/src/main/java/com/quicknotes/app/ui/editor/LinksSection.kt`
- Modify: `app/src/main/java/com/quicknotes/app/ui/editor/EditorScreen.kt` (new callbacks + section item)
- Modify: `app/src/main/java/com/quicknotes/app/ui/ViewModelFactory.kt` (`editorViewModelFactory` gains `prefillTitle`)
- Modify: `app/src/main/java/com/quicknotes/app/ui/nav/QuickNotesNavHost.kt` (`EDITOR_ROUTE` + callbacks)
- Test: `app/src/test/java/com/quicknotes/app/ui/editor/EditorViewModelTest.kt`
- Test: `app/src/androidTest/java/com/quicknotes/app/ui/editor/LinksSectionTest.kt`

**Interfaces:**
- Consumes: `NoteRepository.observeOutgoingLinks/observeBacklinks`, `NoteLink`, `NoteRef`, `FakeNoteRepository` (Task 3); `LinkParser.normalize` (Task 1).
- Produces:
  - `EditorViewModel(noteRepository, tagRepository, folderRepository, noteId: Long?, prefillContent: String? = null, prefillTitle: String? = null)`
  - `EditorViewModel.outgoingLinks: StateFlow<List<NoteLink>>`, `EditorViewModel.backlinks: StateFlow<List<NoteRef>>` (empty for a new note; they reflect the saved note, i.e. update after save)
  - `@Composable fun LinksSection(backlinks: List<NoteRef>, outgoing: List<NoteLink>, onOpenNote: (Long) -> Unit, onCreateGhost: (String) -> Unit, modifier: Modifier = Modifier)` — renders nothing when both lists are empty; test tag `links_section`
  - `EditorScreen(viewModel, onSaved, onClose = onSaved, onOpenNote: (Long) -> Unit = {}, onCreateNote: (String) -> Unit = {})`
  - Route `editor?noteId={noteId}&prefillContent={prefillContent}&prefillTitle={prefillTitle}`; `editorViewModelFactory(container, noteId, prefillContent = null, prefillTitle = null)`. Task 9 navigates to `editor?noteId=0&prefillTitle=${Uri.encode(title)}` for ghost nodes.

- [ ] **Step 1: Write the failing view-model tests**

In `EditorViewModelTest.kt`, widen the helper and add imports `com.quicknotes.app.domain.model.NoteLink`, `com.quicknotes.app.domain.model.NoteRef`, `kotlinx.coroutines.flow.first`:

```kotlin
private fun editorViewModel(
    repository: FakeNoteRepository,
    noteId: Long?,
    prefillContent: String? = null,
    prefillTitle: String? = null
) = EditorViewModel(repository, FakeTagRepository(), FakeFolderRepository(), noteId, prefillContent, prefillTitle)
```

and inside the class:

```kotlin
    private fun plainNote(title: String, content: String = "") = Note(
        title = title, content = content, createdAt = 1, updatedAt = 1, folderId = null,
        favorite = false, archived = false, inbox = true, captureSource = CaptureSource.APP
    )

    @Test
    fun ghostPrefillStartsNewNoteWithTitle() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = editorViewModel(repository, noteId = null, prefillTitle = "Futura Nota")

        assertEquals("Futura Nota", viewModel.uiState.value.title)
        assertEquals("", viewModel.uiState.value.content)
        viewModel.save {}
        assertEquals("Futura Nota", repository.notes.value.single().title)
    }

    @Test
    fun prefillTitleIsIgnoredForExistingNote() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.saveNote(plainNote("Original"))

        val viewModel = editorViewModel(repository, noteId = id, prefillTitle = "Outro")

        assertEquals("Original", viewModel.uiState.value.title)
    }

    @Test
    fun exposesOutgoingLinksAndBacklinksOfSavedNote() = runTest {
        val repository = FakeNoteRepository()
        val a = repository.saveNote(plainNote("A", "[[B]] e [[Fantasma]]"))
        val b = repository.saveNote(plainNote("B", "volta para [[a]]"))

        val viewModel = editorViewModel(repository, noteId = a)

        assertEquals(
            listOf(NoteLink(a, 0, b, "B"), NoteLink(a, 1, null, "Fantasma")),
            viewModel.outgoingLinks.first { it.isNotEmpty() }
        )
        assertEquals(listOf(NoteRef(b, "B")), viewModel.backlinks.first { it.isNotEmpty() })
    }

    @Test
    fun newNoteHasNoLinks() = runTest {
        val viewModel = editorViewModel(FakeNoteRepository(), noteId = null)

        assertTrue(viewModel.outgoingLinks.first().isEmpty())
        assertTrue(viewModel.backlinks.first().isEmpty())
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.quicknotes.app.ui.editor.EditorViewModelTest"`
Expected: FAIL — compilation error (`Too many arguments for EditorViewModel` / `Unresolved reference: outgoingLinks`).

- [ ] **Step 3: Implement in `EditorViewModel`**

Add imports `com.quicknotes.app.domain.model.NoteLink`, `com.quicknotes.app.domain.model.NoteRef`, `kotlinx.coroutines.flow.flowOf`. Change the constructor tail and initial state:

```kotlin
    private val noteId: Long?,
    prefillContent: String? = null,
    prefillTitle: String? = null
) : ViewModel() {
    // Prefills only apply to a brand new note (voice transcription, or a ghost link being created).
    private val _uiState = MutableStateFlow(
        if (noteId == null) EditorUiState(title = prefillTitle.orEmpty(), content = prefillContent.orEmpty())
        else EditorUiState()
    )
```

and below `allFolders`:

```kotlin
    val outgoingLinks: StateFlow<List<NoteLink>> =
        (if (noteId == null) flowOf(emptyList()) else noteRepository.observeOutgoingLinks(noteId))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val backlinks: StateFlow<List<NoteRef>> =
        (if (noteId == null) flowOf(emptyList()) else noteRepository.observeBacklinks(noteId))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.quicknotes.app.ui.editor.EditorViewModelTest"`
Expected: PASS (existing tests + 4 new).

- [ ] **Step 5: Write the failing `LinksSection` UI test**

`app/src/androidTest/java/com/quicknotes/app/ui/editor/LinksSectionTest.kt`:

```kotlin
package com.quicknotes.app.ui.editor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinksSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hiddenWhenThereAreNoLinks() {
        composeRule.setContent { LinksSection(emptyList(), emptyList(), onOpenNote = {}, onCreateGhost = {}) }
        composeRule.onNodeWithTag("links_section").assertDoesNotExist()
    }

    @Test
    fun ghostRowCallsCreateWithItsTitle() {
        val opened = mutableListOf<Long>()
        val created = mutableListOf<String>()
        composeRule.setContent {
            LinksSection(
                backlinks = listOf(NoteRef(3, "Origem")),
                outgoing = listOf(NoteLink(1, 0, 2, "Real"), NoteLink(1, 1, null, "Futura"), NoteLink(1, 2, 2, "real")),
                onOpenNote = { opened += it },
                onCreateGhost = { created += it }
            )
        }

        composeRule.onNodeWithText("fantasma").assertExists()
        composeRule.onNodeWithText("Futura").performClick()
        composeRule.onNodeWithText("Real").performClick()
        composeRule.onNodeWithText("Origem").performClick()

        assertEquals(listOf("Futura"), created)
        assertEquals(listOf(2L, 3L), opened)
        composeRule.onNodeWithText("real").assertDoesNotExist() // same target listed once, first occurrence wins
    }
}
```

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.ui.editor.LinksSectionTest`
Expected: FAIL — compilation error `Unresolved reference: LinksSection`.

- [ ] **Step 6: Implement `LinksSection`**

`app/src/main/java/com/quicknotes/app/ui/editor/LinksSection.kt`:

```kotlin
package com.quicknotes.app.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.domain.link.LinkParser
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.ui.components.SectionLabel
import com.quicknotes.app.ui.theme.Nocturne

/** Backlinks + outgoing links of the saved note; tapping a ghost creates it with the title prefilled. */
@Composable
fun LinksSection(
    backlinks: List<NoteRef>,
    outgoing: List<NoteLink>,
    onOpenNote: (Long) -> Unit,
    onCreateGhost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (backlinks.isEmpty() && outgoing.isEmpty()) return
    Column(modifier.testTag("links_section")) {
        SectionLabel("Ligações", Modifier.padding(top = 22.dp, bottom = 9.dp))
        if (backlinks.isNotEmpty()) {
            GroupLabel("← Backlinks")
            backlinks.forEach { ref -> LinkRow(ref.title.ifBlank { "(sem título)" }, ghost = false) { onOpenNote(ref.id) } }
        }
        if (outgoing.isNotEmpty()) {
            GroupLabel("→ Links de saída")
            // Already ordered by index; the same title typed twice is listed once.
            outgoing.distinctBy { LinkParser.normalize(it.targetTitle) }.forEach { link ->
                LinkRow(link.targetTitle, ghost = link.isGhost) {
                    link.targetId?.let(onOpenNote) ?: onCreateGhost(link.targetTitle)
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(text, color = Nocturne.TextMuted, fontSize = 11.5.sp, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
}

@Composable
private fun LinkRow(label: String, ghost: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            if (ghost) Icons.Filled.Add else Icons.Filled.Link,
            contentDescription = null,
            tint = if (ghost) Nocturne.TextMuted else Nocturne.AccentText,
            modifier = Modifier.size(17.dp)
        )
        Text(
            label, color = if (ghost) Nocturne.TextSecondary else Nocturne.TextPrimary, fontSize = 13.5.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        if (ghost) Text("fantasma", color = Nocturne.TextFaint, fontSize = 11.sp)
    }
}
```

- [ ] **Step 7: Wire the section into the editor, factory and route**

`EditorScreen.kt` — new signature and state (keep the body otherwise unchanged):

```kotlin
fun EditorScreen(
    viewModel: EditorViewModel,
    onSaved: () -> Unit,
    onClose: () -> Unit = onSaved,
    onOpenNote: (Long) -> Unit = {},
    onCreateNote: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val folders by viewModel.allFolders.collectAsState()
    val outgoing by viewModel.outgoingLinks.collectAsState()
    val backlinks by viewModel.backlinks.collectAsState()
```

and a new `item` between the "Pasta" item and the Inbox toggle item:

```kotlin
            item {
                LinksSection(backlinks = backlinks, outgoing = outgoing, onOpenNote = onOpenNote, onCreateGhost = onCreateNote)
            }
```

`ViewModelFactory.kt`:

```kotlin
fun editorViewModelFactory(
    container: AppContainer,
    noteId: Long?,
    prefillContent: String? = null,
    prefillTitle: String? = null
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditorViewModel(
                container.noteRepository, container.tagRepository, container.folderRepository,
                noteId, prefillContent, prefillTitle
            ) as T
    }
```

`QuickNotesNavHost.kt` — route constant, argument and callbacks:

```kotlin
private const val EDITOR_ROUTE = "editor?noteId={noteId}&prefillContent={prefillContent}&prefillTitle={prefillTitle}"
```

```kotlin
                    composable(
                        EDITOR_ROUTE,
                        arguments = listOf(
                            navArgument("noteId") { type = NavType.LongType; defaultValue = 0L },
                            navArgument("prefillContent") { type = NavType.StringType; nullable = true; defaultValue = null },
                            navArgument("prefillTitle") { type = NavType.StringType; nullable = true; defaultValue = null }
                        )
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getLong("noteId")?.takeIf { it != 0L }
                        val prefill = backStackEntry.arguments?.getString("prefillContent")
                        val prefillTitle = backStackEntry.arguments?.getString("prefillTitle")
                        val vm: EditorViewModel = viewModel(factory = editorViewModelFactory(container, noteId, prefill, prefillTitle))
                        EditorScreen(
                            vm,
                            onSaved = { navController.popBackStack() },
                            onOpenNote = { id -> navController.navigate("editor?noteId=$id") },
                            onCreateNote = { title -> navController.navigate("editor?noteId=0&prefillTitle=${Uri.encode(title)}") }
                        )
                    }
```

Keep the existing comment about the Inbox root above `EditorScreen(...)`. Opening a linked note pushes a new editor on top, so back returns to the note that was being read.

- [ ] **Step 8: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.
Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.ui.editor.LinksSectionTest`
Expected: PASS (2 tests).
Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.ui.nav.QuickNotesNavHostTest`
Expected: PASS (editor route still opens from the FAB).

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/quicknotes/app/ui/editor/EditorViewModel.kt app/src/main/java/com/quicknotes/app/ui/editor/LinksSection.kt app/src/main/java/com/quicknotes/app/ui/editor/EditorScreen.kt app/src/main/java/com/quicknotes/app/ui/ViewModelFactory.kt app/src/main/java/com/quicknotes/app/ui/nav/QuickNotesNavHost.kt app/src/test/java/com/quicknotes/app/ui/editor/EditorViewModelTest.kt app/src/androidTest/java/com/quicknotes/app/ui/editor/LinksSectionTest.kt
git commit -m "feature[quick-notes-graph]: show backlinks and outgoing links in the editor"
```

---

### Task 7: Botão "ligar nota" (developer-ui, depende de 6)

**Files:**
- Modify: `app/src/main/java/com/quicknotes/app/ui/editor/EditorViewModel.kt` (`insertLinkText`, `insertLink`, picker search)
- Create: `app/src/main/java/com/quicknotes/app/ui/editor/LinkPickerSheet.kt`
- Modify: `app/src/main/java/com/quicknotes/app/ui/editor/EditorScreen.kt` (toolbar button, content field with cursor)
- Test: `app/src/test/java/com/quicknotes/app/ui/editor/EditorViewModelTest.kt`
- Test: `app/src/androidTest/java/com/quicknotes/app/ui/nav/QuickNotesNavHostTest.kt`

**Interfaces:**
- Consumes: `NoteRepository.search(query): List<Note>` (MVP; empty query matches every note), `NoteRef` (Task 1), `EditorViewModel`/`EditorScreen` from Task 6.
- Produces:
  - `internal fun insertLinkText(content: String, title: String, cursor: Int?): Pair<String, Int>` (top level in `EditorViewModel.kt`) — returns new content + cursor right after the inserted `[[title]]`. `cursor == null` → append at the end on its own line (no leading newline when content is empty or already ends with one); cursor is clamped to `0..content.length`.
  - `EditorViewModel.insertLink(title: String, cursor: Int?): Int` — applies `insertLinkText` to `uiState.content`, returns the new cursor.
  - `EditorViewModel.linkCandidates: StateFlow<List<NoteRef>>` and `fun searchLinkTargets(query: String)` — excludes the note being edited and blank titles, latest query wins.
  - `@Composable fun LinkPickerSheet(candidates: List<NoteRef>, onQueryChange: (String) -> Unit, onPick: (NoteRef) -> Unit, onDismiss: () -> Unit)`; toolbar button content description `Ligar nota`; search field test tag `link_picker_query`.

- [ ] **Step 1: Write the failing tests**

Add to `EditorViewModelTest` (reuses `plainNote` from Task 6):

```kotlin
    @Test
    fun insertLinkTextPlacesLinkAtCursorOrOnNewLine() {
        assertEquals("ver [[B]] agora" to 9, insertLinkText("ver  agora", "B", cursor = 4))
        assertEquals("texto\n[[B]]" to 11, insertLinkText("texto", " B ", cursor = null))
        assertEquals("[[B]]" to 5, insertLinkText("", "B", cursor = null))
        assertEquals("a\n[[B]]" to 7, insertLinkText("a\n", "B", cursor = null))
        assertEquals("ab[[B]]" to 7, insertLinkText("ab", "B", cursor = 99))
    }

    @Test
    fun insertLinkUpdatesContentAndReturnsCursor() = runTest {
        val viewModel = editorViewModel(FakeNoteRepository(), noteId = null, prefillContent = "ver  agora")

        val cursor = viewModel.insertLink("Plano de aula", cursor = 4)

        assertEquals("ver [[Plano de aula]] agora", viewModel.uiState.value.content)
        assertEquals(21, cursor)
    }

    @Test
    fun linkCandidatesExcludeCurrentAndUntitledNotes() = runTest {
        val repository = FakeNoteRepository()
        val current = repository.saveNote(plainNote("Atual"))
        val other = repository.saveNote(plainNote("Outra"))
        repository.saveNote(plainNote("   ", "captura sem título"))
        val viewModel = editorViewModel(repository, noteId = current)

        viewModel.searchLinkTargets("")

        assertEquals(listOf(NoteRef(other, "Outra")), viewModel.linkCandidates.value)
    }
```

Add to `QuickNotesNavHostTest` (import `androidx.compose.ui.test.onNodeWithTag` is already there):

```kotlin
    @Test
    fun linkButtonOpensNotePicker() {
        composeRule.onNodeWithTag("new_note_fab").performClick()
        composeRule.onNodeWithContentDescription("Ligar nota").performClick()

        composeRule.onNodeWithTag("link_picker_query").assertExists()
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.quicknotes.app.ui.editor.EditorViewModelTest"`
Expected: FAIL — compilation error `Unresolved reference: insertLinkText`.

- [ ] **Step 3: Implement the view-model side**

In `EditorViewModel.kt` add imports `kotlinx.coroutines.Job`; then at top level (below `EditorUiState`):

```kotlin
/** Inserts `[[title]]` at [cursor], or on a new last line when there is no cursor. Returns text + new cursor. */
internal fun insertLinkText(content: String, title: String, cursor: Int?): Pair<String, Int> {
    val link = "[[${title.trim()}]]"
    if (cursor == null) {
        val prefix = if (content.isEmpty() || content.endsWith("\n")) content else content + "\n"
        return (prefix + link).let { it to it.length }
    }
    val at = cursor.coerceIn(0, content.length)
    return content.substring(0, at) + link + content.substring(at) to at + link.length
}
```

and inside the class:

```kotlin
    private val _linkCandidates = MutableStateFlow<List<NoteRef>>(emptyList())
    val linkCandidates: StateFlow<List<NoteRef>> = _linkCandidates.asStateFlow()
    private var linkSearch: Job? = null

    fun searchLinkTargets(query: String) {
        linkSearch?.cancel() // latest keystroke wins
        linkSearch = viewModelScope.launch {
            val currentId = _uiState.value.id
            _linkCandidates.value = noteRepository.search(query.trim())
                .filter { it.id != currentId && it.title.isNotBlank() }
                .map { NoteRef(it.id, it.title) }
        }
    }

    fun insertLink(title: String, cursor: Int?): Int {
        val (content, newCursor) = insertLinkText(_uiState.value.content, title, cursor)
        _uiState.value = _uiState.value.copy(content = content)
        return newCursor
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.quicknotes.app.ui.editor.EditorViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Add the picker sheet**

`app/src/main/java/com/quicknotes/app/ui/editor/LinkPickerSheet.kt`:

```kotlin
package com.quicknotes.app.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.ui.theme.Nocturne

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkPickerSheet(
    candidates: List<NoteRef>,
    onQueryChange: (String) -> Unit,
    onPick: (NoteRef) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { onQueryChange("") } // show every note before the first keystroke
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Nocturne.Surface) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 18.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; onQueryChange(it) },
                placeholder = { Text("Buscar nota para ligar…", color = Nocturne.TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("link_picker_query")
            )
            if (candidates.isEmpty()) {
                Text("Nenhuma nota encontrada", color = Nocturne.TextMuted, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 14.dp))
            }
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(candidates, key = { it.id }) { ref ->
                    Text(
                        ref.title, color = Nocturne.TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(ref) }.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 6: Track the cursor and add the toolbar button in `EditorScreen`**

Imports to add: `androidx.compose.material.icons.filled.Link`, `androidx.compose.runtime.LaunchedEffect`, `mutableStateOf`, `remember`, `setValue`, `androidx.compose.ui.focus.onFocusChanged`, `androidx.compose.ui.text.TextRange`, `androidx.compose.ui.text.input.TextFieldValue`.

Right after the `val backlinks by …` line:

```kotlin
    val linkCandidates by viewModel.linkCandidates.collectAsState()
    var pickerOpen by remember { mutableStateOf(false) }
    // Local TextFieldValue so we know the cursor; "cursor defined" = the user has focused the field.
    var contentValue by remember { mutableStateOf(TextFieldValue(state.content)) }
    var cursorPlaced by remember { mutableStateOf(false) }
    LaunchedEffect(state.content) { // async note load / prefill
        if (state.content != contentValue.text) contentValue = TextFieldValue(state.content, TextRange(state.content.length))
    }
```

Toolbar: insert before the favorite `IconButton`:

```kotlin
            IconButton(onClick = { pickerOpen = true }) {
                Icon(Icons.Filled.Link, contentDescription = "Ligar nota", tint = Nocturne.TextSecondary)
            }
```

Replace the content `item { PlainField(value = state.content, …) }` with:

```kotlin
            item {
                PlainField(
                    value = contentValue,
                    onValueChange = { contentValue = it; viewModel.updateContent(it.text) },
                    placeholder = "Escreva…", fontSize = 14.5.sp, minHeight = 180.dp,
                    modifier = Modifier.onFocusChanged { if (it.isFocused) cursorPlaced = true }
                )
            }
```

At the end of the outer `Column { … }` (after the `LazyColumn`):

```kotlin
        if (pickerOpen) {
            LinkPickerSheet(
                candidates = linkCandidates,
                onQueryChange = viewModel::searchLinkTargets,
                onPick = { ref ->
                    pickerOpen = false
                    val cursor = viewModel.insertLink(ref.title, if (cursorPlaced) contentValue.selection.end else null)
                    contentValue = TextFieldValue(viewModel.uiState.value.content, TextRange(cursor))
                },
                onDismiss = { pickerOpen = false }
            )
        }
```

Replace the private `PlainField` with a shared style plus two overloads (title keeps `String`, content uses `TextFieldValue`):

```kotlin
@Composable
private fun plainColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    cursorColor = Nocturne.Accent
)

private fun plainStyle(fontSize: androidx.compose.ui.unit.TextUnit, fontWeight: FontWeight?) =
    TextStyle(color = Nocturne.TextPrimary, fontSize = fontSize, fontWeight = fontWeight, lineHeight = fontSize * 1.5f)

private fun Modifier.plainSize(minHeight: androidx.compose.ui.unit.Dp) =
    fillMaxWidth().let { if (minHeight != androidx.compose.ui.unit.Dp.Unspecified) it.height(minHeight) else it }

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Nocturne.TextMuted, fontSize = fontSize) },
        textStyle = plainStyle(fontSize, fontWeight), colors = plainColors(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PlainField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    minHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Nocturne.TextMuted, fontSize = fontSize) },
        textStyle = plainStyle(fontSize, null), colors = plainColors(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        modifier = modifier.plainSize(minHeight)
    )
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.
Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.ui.nav.QuickNotesNavHostTest`
Expected: PASS (4 tests, including `linkButtonOpensNotePicker`).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/quicknotes/app/ui/editor/EditorViewModel.kt app/src/main/java/com/quicknotes/app/ui/editor/LinkPickerSheet.kt app/src/main/java/com/quicknotes/app/ui/editor/EditorScreen.kt app/src/test/java/com/quicknotes/app/ui/editor/EditorViewModelTest.kt app/src/androidTest/java/com/quicknotes/app/ui/nav/QuickNotesNavHostTest.kt
git commit -m "feature[quick-notes-graph]: add link-note button with incremental note picker"
```

---

### Task 8: `GraphModel` + `GraphViewModel` (developer-ui, depende de 3 e 5)

**Files:**
- Create: `app/src/main/java/com/quicknotes/app/ui/graph/GraphModel.kt`
- Create: `app/src/main/java/com/quicknotes/app/ui/graph/GraphViewModel.kt`
- Test: `app/src/test/java/com/quicknotes/app/ui/graph/GraphModelTest.kt`

**Interfaces:**
- Consumes: `NoteGraph`, `NoteLink`, `NoteTagRef`, `NoteRepository.observeGraph()`, `FakeNoteRepository.tags` (Task 3); `LinkParser.normalize`, `NoteRef` (Task 1); `ForceLayout` (Task 5).
- Produces (package `com.quicknotes.app.ui.graph`):
  - `enum class NodeKind { NOTE, TAG, GHOST }`
  - `data class GraphNode(val key: String, val label: String, val kind: NodeKind, val noteId: Long? = null, val tagId: Long? = null)` — keys `n:<id>`, `t:<id>`, `g:<normalized title>`
  - `data class GraphEdge(val from: String, val to: String, val isTagEdge: Boolean)`
  - `data class GraphData(val nodes: List<GraphNode> = emptyList(), val edges: List<GraphEdge> = emptyList())`
  - `fun buildGraph(graph: NoteGraph, filterTagId: Long? = null): GraphData` — drops self links and edges to missing notes/tags; one ghost node per normalized title; unknown filter tag → full graph.
  - `class GraphViewModel(noteRepository: NoteRepository)` with `val graph: StateFlow<GraphData>`, `val filterTagId: StateFlow<Long?>`, `val layout: ForceLayout`, `fun toggleTagFilter(tagId: Long)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.quicknotes.app.ui.graph

import com.quicknotes.app.domain.FakeNoteRepository
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.model.NoteGraph
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.domain.model.NoteTagRef
import com.quicknotes.app.domain.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GraphModelTest {
    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val sample = NoteGraph(
        notes = listOf(NoteRef(1, "A"), NoteRef(2, "B"), NoteRef(3, "")),
        links = listOf(
            NoteLink(1, 0, 2, "B"), NoteLink(1, 1, null, "Futura"), NoteLink(2, 0, null, "FUTURA"),
            NoteLink(1, 2, 1, "A"),   // self link
            NoteLink(2, 1, 99, "Sumiu") // target no longer exists
        ),
        tags = listOf(Tag(10, "ideia"), Tag(11, "aula")),
        noteTags = listOf(NoteTagRef(1, 10), NoteTagRef(1, 11), NoteTagRef(3, 11), NoteTagRef(2, 77)) // tag 77 deleted
    )

    @Test
    fun buildsNoteTagAndSharedGhostNodes() {
        val data = buildGraph(sample)

        assertEquals(
            listOf("n:1" to NodeKind.NOTE, "n:2" to NodeKind.NOTE, "n:3" to NodeKind.NOTE,
                "t:10" to NodeKind.TAG, "t:11" to NodeKind.TAG, "g:futura" to NodeKind.GHOST),
            data.nodes.map { it.key to it.kind }
        )
        assertEquals("(sem título)", data.nodes.first { it.key == "n:3" }.label)
        assertEquals("Futura", data.nodes.first { it.key == "g:futura" }.label)
        assertEquals(
            setOf(GraphEdge("n:1", "n:2", false), GraphEdge("n:1", "g:futura", false), GraphEdge("n:2", "g:futura", false),
                GraphEdge("n:1", "t:10", true), GraphEdge("n:1", "t:11", true), GraphEdge("n:3", "t:11", true)),
            data.edges.toSet()
        )
    }

    @Test
    fun selfLinksAreDroppedAndDeletedFilterTagShowsFullGraph() {
        val full = buildGraph(sample)
        assertEquals(false, full.edges.any { it.from == it.to })
        assertEquals(full, buildGraph(sample, filterTagId = 77))
    }

    @Test
    fun tagFilterKeepsTagItsNotesAndTheirOtherTags() {
        val data = buildGraph(sample, filterTagId = 10)

        assertEquals(setOf("t:10", "n:1", "t:11"), data.nodes.map { it.key }.toSet())
        assertEquals(setOf(GraphEdge("n:1", "t:10", true), GraphEdge("n:1", "t:11", true)), data.edges.toSet())
    }

    @Test
    fun viewModelFollowsRepositoryAndTogglesFilter() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = GraphViewModel(repository)
        fun note(title: String, content: String = "", tagIds: List<Long> = emptyList()) = Note(
            title = title, content = content, createdAt = 1, updatedAt = 1, folderId = null, favorite = false,
            archived = false, inbox = true, captureSource = CaptureSource.APP, tagIds = tagIds
        )

        repository.tags.value = listOf(Tag(10, "ideia"))
        repository.saveNote(note("A", "[[B]]", tagIds = listOf(10)))
        assertEquals(setOf("n:1", "t:10", "g:b"), viewModel.graph.first { it.nodes.size == 3 }.nodes.map { it.key }.toSet())

        repository.saveNote(note("B")) // ghost resolves into a real note
        assertEquals(setOf("n:1", "n:2", "t:10"), viewModel.graph.first { "n:2" in it.nodes.map { n -> n.key } }.nodes.map { it.key }.toSet())

        viewModel.toggleTagFilter(10)
        assertEquals(setOf("n:1", "t:10"), viewModel.graph.first { it.nodes.size == 2 }.nodes.map { it.key }.toSet())
        viewModel.toggleTagFilter(10)
        assertEquals(null, viewModel.filterTagId.value)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.quicknotes.app.ui.graph.GraphModelTest"`
Expected: FAIL — compilation error `Unresolved reference: buildGraph`.

- [ ] **Step 3: Write minimal implementation**

`app/src/main/java/com/quicknotes/app/ui/graph/GraphModel.kt`:

```kotlin
package com.quicknotes.app.ui.graph

import com.quicknotes.app.domain.link.LinkParser
import com.quicknotes.app.domain.model.NoteGraph

enum class NodeKind { NOTE, TAG, GHOST }

data class GraphNode(val key: String, val label: String, val kind: NodeKind, val noteId: Long? = null, val tagId: Long? = null)

data class GraphEdge(val from: String, val to: String, val isTagEdge: Boolean)

data class GraphData(val nodes: List<GraphNode> = emptyList(), val edges: List<GraphEdge> = emptyList())

private fun noteKey(id: Long) = "n:$id"
private fun tagKey(id: Long) = "t:$id"
private fun ghostKey(title: String) = "g:${LinkParser.normalize(title)}"

/** Notes + tags + ghosts. Folders are never nodes. An unknown [filterTagId] (tag deleted) shows the full graph. */
fun buildGraph(graph: NoteGraph, filterTagId: Long? = null): GraphData {
    val noteIds = graph.notes.map { it.id }.toSet()
    val tagIds = graph.tags.map { it.id }.toSet()
    val nodes = LinkedHashMap<String, GraphNode>()
    graph.notes.forEach { nodes[noteKey(it.id)] = GraphNode(noteKey(it.id), it.title.ifBlank { "(sem título)" }, NodeKind.NOTE, noteId = it.id) }
    graph.tags.forEach { nodes[tagKey(it.id)] = GraphNode(tagKey(it.id), "#${it.name}", NodeKind.TAG, tagId = it.id) }

    val edges = LinkedHashSet<GraphEdge>()
    for (link in graph.links) {
        if (link.sourceId !in noteIds) continue
        val target = link.targetId
        val to = when {
            target == null -> ghostKey(link.targetTitle).also { key ->
                nodes.getOrPut(key) { GraphNode(key, link.targetTitle, NodeKind.GHOST) }
            }
            target == link.sourceId || target !in noteIds -> continue
            else -> noteKey(target)
        }
        edges += GraphEdge(noteKey(link.sourceId), to, isTagEdge = false)
    }
    for (pair in graph.noteTags) {
        if (pair.noteId in noteIds && pair.tagId in tagIds) edges += GraphEdge(noteKey(pair.noteId), tagKey(pair.tagId), isTagEdge = true)
    }
    val full = GraphData(nodes.values.toList(), edges.toList())
    if (filterTagId == null || filterTagId !in tagIds) return full

    val tagged = graph.noteTags.filter { it.tagId == filterTagId && it.noteId in noteIds }.map { noteKey(it.noteId) }.toSet()
    val keep = tagged + tagKey(filterTagId) + edges.filter { it.isTagEdge && it.from in tagged }.map { it.to }
    return GraphData(full.nodes.filter { it.key in keep }, full.edges.filter { it.from in keep && it.to in keep })
}
```

`app/src/main/java/com/quicknotes/app/ui/graph/GraphViewModel.kt`:

```kotlin
package com.quicknotes.app.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicknotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Activity-scoped (see QuickNotesNavHost) so [layout] keeps node positions between tab switches. */
class GraphViewModel(noteRepository: NoteRepository) : ViewModel() {
    private val _filterTagId = MutableStateFlow<Long?>(null)
    val filterTagId: StateFlow<Long?> = _filterTagId.asStateFlow()

    val graph: StateFlow<GraphData> = combine(noteRepository.observeGraph(), _filterTagId) { graph, tagId -> buildGraph(graph, tagId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GraphData())

    val layout = ForceLayout()

    fun toggleTagFilter(tagId: Long) {
        _filterTagId.value = if (_filterTagId.value == tagId) null else tagId
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.quicknotes.app.ui.graph.GraphModelTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/quicknotes/app/ui/graph/GraphModel.kt app/src/main/java/com/quicknotes/app/ui/graph/GraphViewModel.kt app/src/test/java/com/quicknotes/app/ui/graph/GraphModelTest.kt
git commit -m "feature[quick-notes-graph]: build graph nodes/edges with tag filter"
```

---

### Task 9: Tela Grafo + aba (developer-ui, depende de 5, 6 e 8)

**Files:**
- Create: `app/src/main/java/com/quicknotes/app/ui/graph/GraphScreen.kt`
- Modify: `app/src/main/java/com/quicknotes/app/ui/ViewModelFactory.kt` (`GraphViewModel`)
- Modify: `app/src/main/java/com/quicknotes/app/ui/nav/QuickNotesNavHost.kt` (tab, header, activity-scoped route)
- Test: `app/src/androidTest/java/com/quicknotes/app/ui/nav/QuickNotesNavHostTest.kt`

**Interfaces:**
- Consumes: `GraphViewModel.graph/filterTagId/layout/toggleTagFilter`, `GraphNode`, `NodeKind` (Task 8); `ForceLayout.sync/step/pin/release/body` (Task 5); editor route with `prefillTitle` (Task 6).
- Produces:
  - `@Composable fun GraphScreen(viewModel: GraphViewModel, onOpenNote: (Long) -> Unit, onCreateNote: (String) -> Unit)`; canvas test tag `graph_canvas`, content description `Grafo com N nós`.
  - Bottom-bar tab route `graph` (test tag `tab_graph`, label "Grafo").
  - Gestures: one finger on a node drags it (simulation keeps running); one finger on empty space pans; two fingers pinch-zoom (0.3×–3×) and pan; tap note → editor; tap tag → toggle filter; tap ghost → new note with the title prefilled.

- [ ] **Step 1: Write the failing navigation test**

Add to `QuickNotesNavHostTest`:

```kotlin
    @Test
    fun graphTabShowsGraphCanvas() {
        composeRule.onNodeWithTag("tab_graph").performClick()

        composeRule.onNodeWithTag("graph_canvas").assertExists()
        composeRule.onNodeWithText("notas, tags e ligações").assertExists()
    }
```

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.ui.nav.QuickNotesNavHostTest`
Expected: FAIL — `graphTabShowsGraphCanvas`: `Failed: assertExists. Reason: Expected exactly '1' node but could not find any node that satisfies: (TestTag = 'tab_graph')`.

- [ ] **Step 2: Write `GraphScreen` (simulation loop + drawing)**

`app/src/main/java/com/quicknotes/app/ui/graph/GraphScreen.kt` — part 1 (the function body continues in Step 3):

```kotlin
package com.quicknotes.app.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.ui.components.EmptyHint
import com.quicknotes.app.ui.theme.Nocturne

private const val LABEL_MAX = 22

@Composable
fun GraphScreen(viewModel: GraphViewModel, onOpenNote: (Long) -> Unit, onCreateNote: (String) -> Unit) {
    val data by viewModel.graph.collectAsState()
    val filterTagId by viewModel.filterTagId.collectAsState()
    val layout = viewModel.layout
    val density = LocalDensity.current
    val noteRadius = with(density) { 20.dp.toPx() }
    val tagHalf = with(density) { 9.dp.toPx() }
    val minTouch = with(density) { 24.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var frame by remember { mutableIntStateOf(0) } // bumped per simulated frame so the Canvas redraws
    var wake by remember { mutableIntStateOf(0) }  // bumped on drag start to restart a settled simulation
    var dragKey by remember { mutableStateOf<String?>(null) }
    val labels = remember(data) {
        data.nodes.associate { node ->
            val text = if (node.label.length > LABEL_MAX) node.label.take(LABEL_MAX) + "…" else node.label
            node.key to textMeasurer.measure(text, TextStyle(color = Nocturne.TextSecondary, fontSize = 10.5.sp))
        }
    }
    val onTap by rememberUpdatedState<(GraphNode) -> Unit> { node ->
        when (node.kind) {
            NodeKind.NOTE -> onOpenNote(node.noteId!!)
            NodeKind.TAG -> viewModel.toggleTagFilter(node.tagId!!)
            NodeKind.GHOST -> onCreateNote(node.label)
        }
    }

    fun toScreen(body: ForceLayout.Body) =
        Offset(canvasSize.width / 2f + offset.x + body.x * scale, canvasSize.height / 2f + offset.y + body.y * scale)

    fun hitTest(p: Offset): GraphNode? = data.nodes.lastOrNull { node ->
        val body = layout.body(node.key) ?: return@lastOrNull false
        (toScreen(body) - p).getDistance() <= maxOf(noteRadius * scale, minTouch)
    }

    // Lazy updates: runs when the graph changes or a drag starts, stops once settled (or after ~10 s).
    LaunchedEffect(data, wake) {
        layout.sync(data.nodes.map { it.key }, data.edges.map { it.from to it.to })
        var idleFrames = 0
        while (true) {
            withFrameNanos { }
            val margin = 200f
            // Spec caps work at 10 iterations/frame; off-screen bodies are frozen (culling).
            val moving = layout.step(iterations = 5) { body ->
                val p = toScreen(body)
                p.x in -margin..canvasSize.width + margin && p.y in -margin..canvasSize.height + margin
            }
            frame++
            if (dragKey != null) idleFrames = 0 else if (!moving || ++idleFrames > 600) break
        }
    }
    // continued in Step 3
```

- [ ] **Step 3: Finish `GraphScreen` (gestures + drawing)**

Continue the same function body:

```kotlin
    Box(Modifier.fillMaxSize().background(Nocturne.Canvas)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .testTag("graph_canvas")
                .semantics { contentDescription = "Grafo com ${data.nodes.size} nós" }
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val hit = hitTest(down.position)
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size >= 2) { // pinch zoom + two-finger pan
                                moved = true
                                dragKey?.let { layout.release(it); dragKey = null }
                                scale = (scale * event.calculateZoom()).coerceIn(0.3f, 3f)
                                offset += event.calculatePan()
                            } else {
                                val change = pressed.first()
                                if (!moved && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    moved = true
                                    if (hit != null) { dragKey = hit.key; wake++ }
                                }
                                if (moved) {
                                    val delta = change.positionChange()
                                    val key = dragKey
                                    val body = key?.let(layout::body)
                                    if (key != null && body != null) layout.pin(key, body.x + delta.x / scale, body.y + delta.y / scale)
                                    else offset += delta // one finger on empty space pans
                                }
                            }
                            event.changes.forEach { it.consume() }
                        }
                        dragKey?.let { layout.release(it); dragKey = null }
                        if (!moved && hit != null) onTap(hit)
                    }
                }
        ) {
            frame // read so every simulated frame invalidates the draw
            val stroke = 1.5.dp.toPx()
            for (edge in data.edges) {
                val a = layout.body(edge.from) ?: continue
                val b = layout.body(edge.to) ?: continue
                drawLine(
                    if (edge.isTagEdge) Nocturne.Success.copy(alpha = .35f) else Nocturne.TextMuted,
                    toScreen(a), toScreen(b), strokeWidth = stroke
                )
            }
            for (node in data.nodes) {
                val body = layout.body(node.key) ?: continue
                val c = toScreen(body)
                val r = noteRadius * scale
                if (c.x < -r || c.y < -r || c.x > size.width + r || c.y > size.height + r) continue // off-screen
                when (node.kind) {
                    NodeKind.NOTE -> drawCircle(Nocturne.Accent, r, c)
                    NodeKind.GHOST -> {
                        drawCircle(Nocturne.Accent, r, c, alpha = .5f)
                        drawCircle(
                            Nocturne.AccentText, r, c, alpha = .5f,
                            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                        )
                    }
                    NodeKind.TAG -> {
                        val h = tagHalf * scale
                        drawRect(
                            if (node.tagId == filterTagId) Nocturne.Warning else Nocturne.Success,
                            topLeft = Offset(c.x - h, c.y - h), size = Size(2 * h, 2 * h)
                        )
                    }
                }
                labels[node.key]?.let { label ->
                    drawText(
                        label,
                        topLeft = Offset(c.x - label.size.width / 2f, c.y + maxOf(r, tagHalf * scale) + 4.dp.toPx()),
                        alpha = if (node.kind == NodeKind.GHOST) .5f else 1f
                    )
                }
            }
        }
        if (data.nodes.isEmpty()) {
            EmptyHint("Nenhuma nota ainda. Escreva [[Título]] numa nota para criar ligações.")
        }
    }
}
```

- [ ] **Step 4: Register the view model and the tab**

`ViewModelFactory.kt` — import `com.quicknotes.app.ui.graph.GraphViewModel` and add a branch before `else`:

```kotlin
        GraphViewModel::class.java -> GraphViewModel(container.noteRepository) as T
```

`QuickNotesNavHost.kt` — imports `androidx.compose.material.icons.filled.AccountTree`, `androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner`, `com.quicknotes.app.ui.graph.GraphScreen`, `com.quicknotes.app.ui.graph.GraphViewModel`. Then:

```kotlin
private val TABS = listOf(
    TabDef("inbox", "Inbox", Icons.Filled.Inbox),
    TabDef("search", "Busca", Icons.Filled.Search),
    TabDef("tags", "Tags", Icons.Filled.Tag),
    TabDef("folders", "Pastas", Icons.Filled.Folder),
    TabDef("graph", "Grafo", Icons.Filled.AccountTree)
)
```

In `HEADERS` add:

```kotlin
    "graph" to ScreenHeader("Grafo", "notas, tags e ligações"),
```

First line inside `QuickNotesNavHost(...)` (before `rememberNavController()`), while `LocalViewModelStoreOwner` is still the activity:

```kotlin
    // GraphViewModel lives at activity scope so node positions survive leaving the tab (spec: state preservation).
    val activityOwner = checkNotNull(LocalViewModelStoreOwner.current)
```

New destination next to `composable("folders")`:

```kotlin
                    composable("graph") {
                        val vm: GraphViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = factory)
                        GraphScreen(
                            vm,
                            onOpenNote = { id -> navController.navigate("editor?noteId=$id") },
                            onCreateNote = { title -> navController.navigate("editor?noteId=0&prefillTitle=${Uri.encode(title)}") }
                        )
                    }
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.
Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quicknotes.app.ui.nav.QuickNotesNavHostTest`
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/quicknotes/app/ui/graph/GraphScreen.kt app/src/main/java/com/quicknotes/app/ui/ViewModelFactory.kt app/src/main/java/com/quicknotes/app/ui/nav/QuickNotesNavHost.kt app/src/androidTest/java/com/quicknotes/app/ui/nav/QuickNotesNavHostTest.kt
git commit -m "feature[quick-notes-graph]: add interactive graph tab"
```

---

### Task 10: README, checklist manual e verificação final (coordenador, depende de 1–9)

**Files:**
- Modify: `README` (section "## Relacionamento entre notas")
- Create: `docs/superpowers/plans/2026-09-30-quick-notes-knowledge-graph-manual-checklist.md`

**Interfaces:**
- Consumes: the whole branch (Tasks 1–9).
- Produces: user-facing docs of the link syntax and the known ~300-node limit (spec: "Registrar esse limite na documentação de features"); a manual checklist for what automated tests can't cover.

- [ ] **Step 1: Document the feature in `README`**

Insert right after the paragraph "Isso permite construir uma pequena rede de conhecimento ao longo do tempo." (before the `---`):

```markdown
### Como funciona (fase 2)

* Escreva `[[Título da nota]]` no conteúdo ou use o botão **Ligar nota** no editor. O match ignora maiúsculas/minúsculas e espaços nas pontas.
* Um link para uma nota que ainda não existe vira **fantasma**; ao criar uma nota com esse título, o link é resolvido.
* Renomear uma nota reescreve `[[Título Antigo]]` em todas as notas que apontam para ela.
* O editor mostra a seção **Ligações** (backlinks e links de saída); a aba **Grafo** mostra notas, tags e fantasmas.
* **Limite conhecido:** o grafo usa simulação O(n²) e fica lento acima de ~300 nós (notas + tags + fantasmas). Evolução prevista: Barnes-Hut.
```

- [ ] **Step 2: Write the manual checklist**

`docs/superpowers/plans/2026-09-30-quick-notes-knowledge-graph-manual-checklist.md`:

```markdown
# Quick Notes Knowledge Graph — manual verification checklist

Run on a real device or emulator (API 26+), installed **over** the MVP build (v1 database) first.

- [ ] Upgrade from the MVP build keeps every note; notes that already had `[[...]]` show them under "Ligações"
- [ ] Typing `[[Nota Existente]]` (different case/accents) and saving lists it under "Links de saída"; the target shows a backlink
- [ ] `[[Futura Nota]]` shows as "fantasma"; tapping it opens a new note titled "Futura Nota"; saving turns the link normal
- [ ] "Ligar nota" inserts `[[Título]]` at the cursor after tapping into the text, and on a new last line otherwise
- [ ] Renaming a linked note rewrites `[[Antigo]]` in the linking notes (reopen them to check)
- [ ] Deleting a linked note turns its incoming links into ghosts (editor + graph)
- [ ] Graph: one-finger pan on empty space, pinch zoom, drag a node (neighbours follow), tap note opens editor
- [ ] Graph: tap a tag filters to its notes/tags (tag turns yellow); tap it again restores the full graph
- [ ] Graph: ghost nodes are faded with a dashed border; tapping one opens a prefilled new note
- [ ] Graph: leaving the tab and coming back keeps node positions
- [ ] Graph: with ~300 nodes (generate notes with links) panning/zooming stays usable; note FPS/lag in the PR
```

- [ ] **Step 3: Full verification**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.
Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS (includes `MigrationTest`, `NoteRepositoryLinksTest`, `LinksSectionTest`, `QuickNotesNavHostTest`).
Run: `./gradlew :app:assembleDebug`, install over an MVP build, then walk the manual checklist.
Expected: every item checked, or failures filed before merging `feature/quick-notes-graph` into `develop`.

- [ ] **Step 4: Commit**

```bash
git add README docs/superpowers/plans/2026-09-30-quick-notes-knowledge-graph-manual-checklist.md
git commit -m "feature[quick-notes-graph]: document links, graph limits and manual checklist"
```

