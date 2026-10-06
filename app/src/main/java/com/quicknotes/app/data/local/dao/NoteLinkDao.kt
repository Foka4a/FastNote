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
