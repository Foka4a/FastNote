package com.quicknotes.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.quicknotes.app.data.local.entity.NoteLinkEntity
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.domain.model.NoteTagRef
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteLinkDao {
    // Plain @Insert (ABORT): (sourceId, index) is always rewritten after deleteForSource, never replaced.
    @Insert
    suspend fun insertAll(links: List<NoteLinkEntity>)

    @Query("SELECT * FROM note_links WHERE sourceId = :sourceId ORDER BY `index`")
    suspend fun getForSource(sourceId: Long): List<NoteLinkEntity>

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
}
