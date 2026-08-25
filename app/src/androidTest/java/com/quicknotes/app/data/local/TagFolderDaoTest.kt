package com.quicknotes.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicknotes.app.data.local.database.AppDatabase
import com.quicknotes.app.data.local.entity.FolderEntity
import com.quicknotes.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagFolderDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun createAndDeleteTag() = runBlocking {
        val id = db.tagDao().insert(TagEntity(name = "ideia"))
        assertEquals(1, db.tagDao().observeAll().first().size)
        db.tagDao().delete(id)
        assertEquals(0, db.tagDao().observeAll().first().size)
    }

    @Test
    fun createNestedFolders() = runBlocking {
        val parentId = db.folderDao().insert(FolderEntity(name = "Projetos", parentId = null))
        db.folderDao().insert(FolderEntity(name = "FastNote", parentId = parentId))
        assertEquals(2, db.folderDao().observeAll().first().size)
    }
}
