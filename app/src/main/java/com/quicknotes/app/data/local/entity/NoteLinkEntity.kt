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
