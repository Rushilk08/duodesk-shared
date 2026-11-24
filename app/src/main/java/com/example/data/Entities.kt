package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.*

@Entity(tableName = "users")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val fullName: String,
    val email: String,
    val role: String,
    val avatarKey: String,
    val accentHex: Long
)

fun UserProfileEntity.toModel() = UserProfile(
    id = id,
    name = name,
    fullName = fullName,
    email = email,
    role = role,
    avatarKey = avatarKey,
    accentHex = accentHex
)

fun UserProfile.toEntity() = UserProfileEntity(
    id = id,
    name = name,
    fullName = fullName,
    email = email,
    role = role,
    avatarKey = avatarKey,
    accentHex = accentHex
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val ownerId: String,
    val visibility: String,
    val priority: String,
    val dueDate: String,
    val dueTime: String?,
    val completed: Boolean,
    val category: String,
    val attachments: List<AttachmentRef>,
    val createdAt: Long,
    val updatedAt: Long
)

fun TaskEntity.toModel() = TaskItem(
    id = id,
    title = title,
    description = description,
    ownerId = ownerId,
    visibility = Visibility.valueOf(visibility),
    priority = Priority.valueOf(priority),
    dueDate = dueDate,
    dueTime = dueTime,
    completed = completed,
    category = category,
    attachments = attachments,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TaskItem.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    ownerId = ownerId,
    visibility = visibility.name,
    priority = priority.name,
    dueDate = dueDate,
    dueTime = dueTime,
    completed = completed,
    category = category,
    attachments = attachments,
    createdAt = createdAt,
    updatedAt = updatedAt
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val ownerId: String,
    val visibility: String,
    val date: String,
    val time: String,
    val priority: String,
    val repeat: String,
    val notificationPref: String,
    val completed: Boolean,
    val createdAt: Long
)

fun ReminderEntity.toModel() = ReminderItem(
    id = id,
    title = title,
    description = description,
    ownerId = ownerId,
    visibility = Visibility.valueOf(visibility),
    date = date,
    time = time,
    priority = Priority.valueOf(priority),
    repeat = RepeatOption.valueOf(repeat),
    notificationPref = NotificationPreference.valueOf(notificationPref),
    completed = completed,
    createdAt = createdAt
)

fun ReminderItem.toEntity() = ReminderEntity(
    id = id,
    title = title,
    description = description,
    ownerId = ownerId,
    visibility = visibility.name,
    date = date,
    time = time,
    priority = priority.name,
    repeat = repeat.name,
    notificationPref = notificationPref.name,
    completed = completed,
    createdAt = createdAt
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val description: String,
    val ownerId: String,
    val visibility: String,
    val deadline: String,
    val deadlineTime: String?,
    val priority: String,
    val status: String,
    val attachments: List<AttachmentRef>,
    val createdAt: Long
)

fun AssignmentEntity.toModel() = AssignmentItem(
    id = id,
    title = title,
    subject = subject,
    description = description,
    ownerId = ownerId,
    visibility = Visibility.valueOf(visibility),
    deadline = deadline,
    deadlineTime = deadlineTime,
    priority = Priority.valueOf(priority),
    status = AssignmentStatus.valueOf(status),
    attachments = attachments,
    createdAt = createdAt
)

fun AssignmentItem.toEntity() = AssignmentEntity(
    id = id,
    title = title,
    subject = subject,
    description = description,
    ownerId = ownerId,
    visibility = visibility.name,
    deadline = deadline,
    deadlineTime = deadlineTime,
    priority = priority.name,
    status = status.name,
    attachments = attachments,
    createdAt = createdAt
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val extension: String,
    val mimeType: String,
    val sizeBytes: Long,
    val ownerId: String,
    val visibility: String,
    val uploadedAt: Long,
    val previewUri: String?,
    val summary: String
)

fun DocumentEntity.toModel() = DocumentItem(
    id = id,
    name = name,
    kind = DocumentKind.valueOf(kind),
    extension = extension,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    ownerId = ownerId,
    visibility = Visibility.valueOf(visibility),
    uploadedAt = uploadedAt,
    previewUri = previewUri,
    summary = summary
)

fun DocumentItem.toEntity() = DocumentEntity(
    id = id,
    name = name,
    kind = kind.name,
    extension = extension,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    ownerId = ownerId,
    visibility = visibility.name,
    uploadedAt = uploadedAt,
    previewUri = previewUri,
    summary = summary
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val type: String,
    val checklist: List<ChecklistRow>,
    val ownerId: String,
    val visibility: String,
    val updatedAt: Long,
    val createdAt: Long
)

fun NoteEntity.toModel() = NoteItem(
    id = id,
    title = title,
    content = content,
    type = NoteType.valueOf(type),
    checklist = checklist,
    ownerId = ownerId,
    visibility = Visibility.valueOf(visibility),
    updatedAt = updatedAt,
    createdAt = createdAt
)

fun NoteItem.toEntity() = NoteEntity(
    id = id,
    title = title,
    content = content,
    type = type.name,
    checklist = checklist,
    ownerId = ownerId,
    visibility = visibility.name,
    updatedAt = updatedAt,
    createdAt = createdAt
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val category: String,
    val targetRoute: String,
    val actorId: String,
    val visibility: String,
    val ownerId: String?,
    val read: Boolean,
    val createdAt: Long
)

fun NotificationEntity.toModel() = NotificationItem(
    id = id,
    title = title,
    message = message,
    category = NotificationCategory.valueOf(category),
    targetRoute = targetRoute,
    actorId = actorId,
    visibility = Visibility.valueOf(visibility),
    ownerId = ownerId,
    read = read,
    createdAt = createdAt
)

fun NotificationItem.toEntity() = NotificationEntity(
    id = id,
    title = title,
    message = message,
    category = category.name,
    targetRoute = targetRoute,
    actorId = actorId,
    visibility = visibility.name,
    ownerId = ownerId,
    read = read,
    createdAt = createdAt
)

@Entity(tableName = "notification_settings")
data class NotificationSettingsEntity(
    @PrimaryKey val userId: String,
    val deadlineAlerts: Boolean,
    val sharedUpdates: Boolean,
    val fileUploads: Boolean,
    val dailyDigest: Boolean
)

fun NotificationSettingsEntity.toModel() = NotificationSettings(
    userId = userId,
    deadlineAlerts = deadlineAlerts,
    sharedUpdates = sharedUpdates,
    fileUploads = fileUploads,
    dailyDigest = dailyDigest
)

fun NotificationSettings.toEntity() = NotificationSettingsEntity(
    userId = userId,
    deadlineAlerts = deadlineAlerts,
    sharedUpdates = sharedUpdates,
    fileUploads = fileUploads,
    dailyDigest = dailyDigest
)
