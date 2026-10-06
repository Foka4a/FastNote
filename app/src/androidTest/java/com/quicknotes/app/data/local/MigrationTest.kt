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
