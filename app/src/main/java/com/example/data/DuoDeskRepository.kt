package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class DuoDeskRepository(
    private val dao: DuoDeskDao,
    val prefs: AppPreferences
) {
    private fun uid(prefix: String): String =
        "$prefix-${System.currentTimeMillis().toString(36)}-${UUID.randomUUID().toString().take(4)}"

    suspend fun ensureSeeded() {
        if (dao.getUserCount() == 0) {
            seedAll()
        }
    }

    suspend fun resetDemoData() {
        dao.clearTasks()
        dao.clearReminders()
        dao.clearAssignments()
        dao.clearDocuments()
        dao.clearNotes()
        dao.clearNotifications()
        dao.clearNotificationSettings()
        dao.clearUsers()
        seedAll()
    }

    private suspend fun seedAll() {
        dao.upsertUsers(SeedData.users().map { it.toEntity() })
        dao.upsertAllNotificationSettings(SeedData.notificationSettings().map { it.toEntity() })
        dao.upsertTasks(SeedData.tasks().map { it.toEntity() })
        dao.upsertReminders(SeedData.reminders().map { it.toEntity() })
        dao.upsertAssignments(SeedData.assignments().map { it.toEntity() })
        dao.upsertDocuments(SeedData.documents().map { it.toEntity() })
        dao.upsertNotes(SeedData.notes().map { it.toEntity() })
        dao.upsertNotifications(SeedData.notifications().map { it.toEntity() })
    }

    fun observeUsers(): Flow<Map<String, UserProfile>> =
        dao.observeUsers().map { list -> list.associate { it.id to it.toModel() } }

    fun observeTasks(userId: String): Flow<List<TaskItem>> =
        dao.observeVisibleTasks(userId).map { list -> list.map { it.toModel() } }

    fun observeReminders(userId: String): Flow<List<ReminderItem>> =
        dao.observeVisibleReminders(userId).map { list -> list.map { it.toModel() } }

    fun observeAssignments(userId: String): Flow<List<AssignmentItem>> =
        dao.observeVisibleAssignments(userId).map { list -> list.map { it.toModel() } }

    fun observeDocuments(userId: String): Flow<List<DocumentItem>> =
        dao.observeVisibleDocuments(userId).map { list -> list.map { it.toModel() } }

    fun observeNotes(userId: String): Flow<List<NoteItem>> =
        dao.observeVisibleNotes(userId).map { list -> list.map { it.toModel() } }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> =
        dao.observeVisibleNotifications(userId).map { list -> list.map { it.toModel() } }

    fun observeNotificationSettings(userId: String): Flow<NotificationSettings> =
        dao.observeNotificationSettings(userId).map { entity ->
            entity?.toModel() ?: NotificationSettings(
                userId = userId,
                deadlineAlerts = true,
                sharedUpdates = true,
                fileUploads = true,
                dailyDigest = false
            )
        }

    suspend fun updateUserProfile(user: UserProfile) {
        dao.upsertUser(user.toEntity())
    }

    suspend fun createTask(
        ownerId: String,
        actorName: String,
        title: String,
        description: String,
        visibility: Visibility,
        priority: Priority,
        dueDate: String,
        dueTime: String?,
        category: String,
        attachments: List<AttachmentRef>
    ): TaskItem {
        val now = System.currentTimeMillis()
        val item = TaskItem(
            id = uid("task"),
            title = title,
            description = description,
            ownerId = ownerId,
            visibility = visibility,
            priority = priority,
            dueDate = dueDate,
            dueTime = dueTime,
            completed = false,
            category = category,
            attachments = attachments,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertTask(item.toEntity())
        if (visibility == Visibility.SHARED) {
            dao.upsertNotification(
                NotificationItem(
                    id = uid("notif"),
                    title = "New shared task from $actorName",
                    message = title,
                    category = NotificationCategory.SHARED_TASK,
                    targetRoute = "/shared-space",
                    actorId = ownerId,
                    visibility = Visibility.SHARED,
                    read = false,
                    createdAt = now
                ).toEntity()
            )
        }
        return item
    }

    suspend fun updateTask(actorId: String, actorName: String, updated: TaskItem) {
        val prev = dao.getTaskById(updated.id)?.toModel()
        val now = System.currentTimeMillis()
        val saved = updated.copy(updatedAt = now)
        dao.upsertTask(saved.toEntity())
        if (saved.visibility == Visibility.SHARED && prev != null && !prev.completed && saved.completed) {
            dao.upsertNotification(
                NotificationItem(
                    id = uid("notif"),
                    title = "$actorName completed a shared task",
                    message = saved.title,
                    category = NotificationCategory.COMPLETED,
                    targetRoute = "/shared-space",
                    actorId = actorId,
                    visibility = Visibility.SHARED,
                    read = false,
                    createdAt = now
                ).toEntity()
            )
        }
    }

    suspend fun deleteTask(id: String) {
        dao.deleteTaskById(id)
    }

    suspend fun createReminder(
        ownerId: String,
        title: String,
        description: String,
        visibility: Visibility,
        date: String,
        time: String,
        priority: Priority,
        repeat: RepeatOption,
        notificationPref: NotificationPreference
    ): ReminderItem {
        val now = System.currentTimeMillis()
        val item = ReminderItem(
            id = uid("rem"),
            title = title,
            description = description,
            ownerId = ownerId,
            visibility = visibility,
            date = date,
            time = time,
            priority = priority,
            repeat = repeat,
            notificationPref = notificationPref,
            completed = false,
            createdAt = now
        )
        dao.upsertReminder(item.toEntity())
        return item
    }

    suspend fun updateReminder(updated: ReminderItem) {
        dao.upsertReminder(updated.toEntity())
    }

    suspend fun deleteReminder(id: String) {
        dao.deleteReminderById(id)
    }

    suspend fun createAssignment(
        ownerId: String,
        actorName: String,
        title: String,
        subject: String,
        description: String,
        visibility: Visibility,
        deadline: String,
        deadlineTime: String?,
        priority: Priority,
        attachments: List<AttachmentRef>
    ): AssignmentItem {
        val now = System.currentTimeMillis()
        val item = AssignmentItem(
            id = uid("asg"),
            title = title,
            subject = subject,
            description = description,
            ownerId = ownerId,
            visibility = visibility,
            deadline = deadline,
            deadlineTime = deadlineTime,
            priority = priority,
            status = AssignmentStatus.PENDING,
            attachments = attachments,
            createdAt = now
        )
        dao.upsertAssignment(item.toEntity())
        if (visibility == Visibility.SHARED) {
            dao.upsertNotification(
                NotificationItem(
                    id = uid("notif"),
                    title = "New shared assignment from $actorName",
                    message = "$title ($subject)",
                    category = NotificationCategory.SHARED_TASK,
                    targetRoute = "/shared-space",
                    actorId = ownerId,
                    visibility = Visibility.SHARED,
                    read = false,
                    createdAt = now
                ).toEntity()
            )
        }
        return item
    }

    suspend fun updateAssignment(updated: AssignmentItem) {
        dao.upsertAssignment(updated.toEntity())
    }

    suspend fun deleteAssignment(id: String) {
        dao.deleteAssignmentById(id)
    }

    suspend fun createDocument(
        ownerId: String,
        actorName: String,
        name: String,
        kind: DocumentKind,
        extension: String,
        mimeType: String,
        sizeBytes: Long,
        visibility: Visibility,
        previewUri: String?,
        summary: String
    ): DocumentItem {
        val now = System.currentTimeMillis()
        val item = DocumentItem(
            id = uid("doc"),
            name = name,
            kind = kind,
            extension = extension,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            ownerId = ownerId,
            visibility = visibility,
            uploadedAt = now,
            previewUri = previewUri,
            summary = summary
        )
        dao.upsertDocument(item.toEntity())
        if (visibility == Visibility.SHARED) {
            dao.upsertNotification(
                NotificationItem(
                    id = uid("notif"),
                    title = "$actorName uploaded $name",
                    message = "Available now in Shared Space.",
                    category = NotificationCategory.FILE,
                    targetRoute = "/shared-space",
                    actorId = ownerId,
                    visibility = Visibility.SHARED,
                    read = false,
                    createdAt = now
                ).toEntity()
            )
        }
        return item
    }

    suspend fun deleteDocument(id: String) {
        dao.deleteDocumentById(id)
    }

    suspend fun createNote(
        ownerId: String,
        title: String,
        content: String,
        type: NoteType,
        checklist: List<ChecklistRow>,
        visibility: Visibility
    ): NoteItem {
        val now = System.currentTimeMillis()
        val item = NoteItem(
            id = uid("note"),
            title = title,
            content = content,
            type = type,
            checklist = checklist,
            ownerId = ownerId,
            visibility = visibility,
            updatedAt = now,
            createdAt = now
        )
        dao.upsertNote(item.toEntity())
        return item
    }

    suspend fun updateNote(updated: NoteItem) {
        dao.upsertNote(updated.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteNote(id: String) {
        dao.deleteNoteById(id)
    }

    suspend fun markNotificationRead(id: String) {
        dao.markNotificationRead(id)
    }

    suspend fun markAllNotificationsRead(userId: String) {
        dao.markAllNotificationsRead(userId)
    }

    suspend fun clearNotifications(userId: String) {
        dao.clearVisibleNotifications(userId)
    }

    suspend fun updateNotificationSettings(settings: NotificationSettings) {
        dao.upsertNotificationSettings(settings.toEntity())
    }
}
