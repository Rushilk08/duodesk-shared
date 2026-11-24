package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DuoDeskDao {
    // Users
    @Query("SELECT * FROM users")
    fun observeUsers(): Flow<List<UserProfileEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsers(users: List<UserProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserProfileEntity)

    // Tasks
    @Query("SELECT * FROM tasks WHERE visibility = 'SHARED' OR ownerId = :userId ORDER BY dueDate ASC, createdAt DESC")
    fun observeVisibleTasks(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    // Reminders
    @Query("SELECT * FROM reminders WHERE visibility = 'SHARED' OR ownerId = :userId ORDER BY date ASC, time ASC")
    fun observeVisibleReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminders(reminders: List<ReminderEntity>)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    // Assignments
    @Query("SELECT * FROM assignments WHERE visibility = 'SHARED' OR ownerId = :userId ORDER BY deadline ASC")
    fun observeVisibleAssignments(userId: String): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE id = :id LIMIT 1")
    suspend fun getAssignmentById(id: String): AssignmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignment(assignment: AssignmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignments(assignments: List<AssignmentEntity>)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteAssignmentById(id: String)

    // Documents
    @Query("SELECT * FROM documents WHERE visibility = 'SHARED' OR ownerId = :userId ORDER BY uploadedAt DESC")
    fun observeVisibleDocuments(userId: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocuments(documents: List<DocumentEntity>)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    // Notes
    @Query("SELECT * FROM notes WHERE visibility = 'SHARED' OR ownerId = :userId ORDER BY updatedAt DESC")
    fun observeVisibleNotes(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    // Notifications
    @Query("SELECT * FROM notifications WHERE visibility = 'SHARED' OR ownerId = :userId ORDER BY createdAt DESC")
    fun observeVisibleNotifications(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: String)

    @Query("UPDATE notifications SET read = 1 WHERE visibility = 'SHARED' OR ownerId = :userId")
    suspend fun markAllNotificationsRead(userId: String)

    @Query("DELETE FROM notifications WHERE visibility = 'SHARED' OR ownerId = :userId")
    suspend fun clearVisibleNotifications(userId: String)

    // Notification Settings
    @Query("SELECT * FROM notification_settings WHERE userId = :userId LIMIT 1")
    fun observeNotificationSettings(userId: String): Flow<NotificationSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotificationSettings(settings: NotificationSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllNotificationSettings(settings: List<NotificationSettingsEntity>)

    // Clear all for seed reset
    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()

    @Query("DELETE FROM assignments")
    suspend fun clearAssignments()

    @Query("DELETE FROM documents")
    suspend fun clearDocuments()

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()

    @Query("DELETE FROM notification_settings")
    suspend fun clearNotificationSettings()
}
