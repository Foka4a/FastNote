package com.quicknotes.app.data.local.dao

import androidx.room.*
import com.quicknotes.app.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun observeAll(): Flow<List<FolderEntity>>
}
