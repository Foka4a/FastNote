package com.quicknotes.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.quicknotes.app.data.local.dao.FolderDao
import com.quicknotes.app.data.local.dao.NoteDao
import com.quicknotes.app.data.local.dao.NoteLinkDao
import com.quicknotes.app.data.local.dao.TagDao
import com.quicknotes.app.data.local.entity.FolderEntity
import com.quicknotes.app.data.local.entity.NoteEntity
import com.quicknotes.app.data.local.entity.NoteLinkEntity
import com.quicknotes.app.data.local.entity.NoteTagEntity
import com.quicknotes.app.data.local.entity.TagEntity

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
