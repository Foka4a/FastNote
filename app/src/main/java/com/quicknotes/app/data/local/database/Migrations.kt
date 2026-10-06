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
