package com.example.model

import kotlinx.serialization.Serializable

enum class Visibility {
    PRIVATE,
    SHARED;

    fun toWire(): String = name.lowercase()
    companion object {
        fun fromWire(value: String): Visibility =
            if (value.equals("shared", ignoreCase = true)) SHARED else PRIVATE
    }
}

enum class Priority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High priority");

    fun shortLabel(): String = when (this) {
        LOW -> "Low"
        MEDIUM -> "Medium"
        HIGH -> "High"
    }
}

enum class AssignmentStatus(val label: String) {
    PENDING("Pending"),
    IN_PROGRESS("In progress"),
    COMPLETED("Done")
}

enum class RepeatOption(val label: String) {
    NONE("No repeat"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

enum class NotificationPreference(val label: String) {
    PUSH_IN_APP("Push & In-App"),
    IN_APP("In-App only"),
    EMAIL_DIGEST("Email digest")
}

enum class DocumentKind {
    PDF,
    DOC,
    IMAGE
}

enum class ThemeMode {
    LIGHT,
    DARK
}

enum class NoteType {
    NOTE,
    CHECKLIST
}

enum class NotificationCategory {
    DEADLINE,
    REMINDER,
    SHARED_TASK,
    FILE,
    COMPLETED
}

enum class SpaceTab(val label: String) {
    TASKS("Tasks"),
    REMINDERS("Reminders"),
    ASSIGNMENTS("Assignments"),
    NOTES("Notes"),
    DOCUMENTS("Documents")
}

enum class CreateKind(val label: String) {
    TASK("Task"),
    REMINDER("Reminder"),
    ASSIGNMENT("Assignment"),
    NOTE("Note"),
    DOCUMENT("Document")
}

enum class CalendarViewMode(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    DAY("Day")
}

enum class ToastTone {
    NEUTRAL,
    SUCCESS,
    WARNING
}

data class UserProfile(
    val id: String,
    val name: String,
    val fullName: String,
    val email: String,
    val role: String,
    val avatarKey: String,
    val accentHex: Long
)

@Serializable
data class AttachmentRef(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val kind: String, // "pdf" | "doc" | "image"
    val mimeType: String,
    val previewUri: String? = null
)

data class TaskItem(
    val id: String,
    val title: String,
    val description: String = "",
    val ownerId: String,
    val visibility: Visibility,
    val priority: Priority,
    val dueDate: String, // YYYY-MM-DD
    val dueTime: String? = null, // HH:mm
    val completed: Boolean,
    val category: String = "",
    val attachments: List<AttachmentRef> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)

data class ReminderItem(
    val id: String,
    val title: String,
    val description: String = "",
    val ownerId: String,
    val visibility: Visibility,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val priority: Priority,
    val repeat: RepeatOption,
    val notificationPref: NotificationPreference,
    val completed: Boolean,
    val createdAt: Long
)

data class AssignmentItem(
    val id: String,
    val title: String,
    val subject: String,
    val description: String = "",
    val ownerId: String,
    val visibility: Visibility,
    val deadline: String, // YYYY-MM-DD
    val deadlineTime: String? = null, // HH:mm
    val priority: Priority,
    val status: AssignmentStatus,
    val attachments: List<AttachmentRef> = emptyList(),
    val createdAt: Long
)

data class DocumentItem(
    val id: String,
    val name: String,
    val kind: DocumentKind,
    val extension: String,
    val mimeType: String,
    val sizeBytes: Long,
    val ownerId: String,
    val visibility: Visibility,
    val uploadedAt: Long,
    val previewUri: String? = null,
    val summary: String = ""
)

@Serializable
data class ChecklistRow(
    val id: String,
    val text: String,
    val checked: Boolean
)

data class NoteItem(
    val id: String,
    val title: String,
    val content: String,
    val type: NoteType,
    val checklist: List<ChecklistRow>,
    val ownerId: String,
    val visibility: Visibility,
    val updatedAt: Long,
    val createdAt: Long
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val category: NotificationCategory,
    val targetRoute: String,
    val actorId: String,
    val visibility: Visibility,
    val ownerId: String? = null,
    val read: Boolean,
    val createdAt: Long
)

data class NotificationSettings(
    val userId: String,
    val deadlineAlerts: Boolean,
    val sharedUpdates: Boolean,
    val fileUploads: Boolean,
    val dailyDigest: Boolean
)

sealed interface InspectableItem {
    data class Task(val item: TaskItem) : InspectableItem
    data class Reminder(val item: ReminderItem) : InspectableItem
    data class Assignment(val item: AssignmentItem) : InspectableItem
}

data class ToastMessage(
    val id: String,
    val title: String,
    val description: String? = null,
    val tone: ToastTone = ToastTone.NEUTRAL
)
