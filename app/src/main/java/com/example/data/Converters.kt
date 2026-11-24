package com.example.data

import androidx.room.TypeConverter
import com.example.model.AttachmentRef
import com.example.model.ChecklistRow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromAttachmentList(value: List<AttachmentRef>): String = json.encodeToString(value)

    @TypeConverter
    fun toAttachmentList(value: String): List<AttachmentRef> =
        if (value.isBlank()) emptyList() else runCatching {
            json.decodeFromString<List<AttachmentRef>>(value)
        }.getOrDefault(emptyList())

    @TypeConverter
    fun fromChecklistList(value: List<ChecklistRow>): String = json.encodeToString(value)

    @TypeConverter
    fun toChecklistList(value: String): List<ChecklistRow> =
        if (value.isBlank()) emptyList() else runCatching {
            json.decodeFromString<List<ChecklistRow>>(value)
        }.getOrDefault(emptyList())
}
