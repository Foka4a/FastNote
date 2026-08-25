package com.quicknotes.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "note_tags", primaryKeys = ["noteId", "tagId"])
data class NoteTagEntity(
    val noteId: Long,
    val tagId: Long
)
