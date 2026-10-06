package com.quicknotes.app.data.local.dao

import androidx.room.*
import com.quicknotes.app.data.local.entity.NoteEntity
import com.quicknotes.app.data.local.entity.NoteTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    // archived = 0: archiving from the Inbox only flips `archived`, so the note must drop out here.
    @Query("SELECT * FROM notes WHERE inbox = 1 AND archived = 0 ORDER BY createdAt DESC")
    fun observeInbox(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE favorite = 1 AND archived = 0 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE archived = 1 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND archived = 0 ORDER BY updatedAt DESC")
    fun observeByFolder(folderId: Long): Flow<List<NoteEntity>>

    // search() deliberately still includes archived notes (Keep-style).
    @Query("SELECT * FROM notes WHERE archived = 0 ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT DISTINCT note.* FROM notes note
        LEFT JOIN note_tags nt ON nt.noteId = note.id
        LEFT JOIN tags tag ON tag.id = nt.tagId
        LEFT JOIN folders folder ON folder.id = note.folderId
        WHERE note.title LIKE '%' || :query || '%'
           OR note.content LIKE '%' || :query || '%'
           OR tag.name LIKE '%' || :query || '%'
           OR folder.name LIKE '%' || :query || '%'
        ORDER BY note.updatedAt DESC
        """
    )
    suspend fun search(query: String): List<NoteEntity>

    @Query("UPDATE notes SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    // Unarchiving returns the note to the Inbox (same as the editor's toggle); archiving keeps `inbox` as is.
    @Query("UPDATE notes SET archived = :archived, inbox = CASE WHEN :archived THEN inbox ELSE 1 END WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTags(noteTags: List<NoteTagEntity>)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun clearNoteTags(noteId: Long)

    @Query("SELECT tagId FROM note_tags WHERE noteId = :noteId")
    suspend fun getTagIdsForNote(noteId: Long): List<Long>
}
