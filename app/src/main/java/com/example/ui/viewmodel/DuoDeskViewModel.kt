package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DuoDeskRepository
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class CreateDefaults(
    val kind: CreateKind = CreateKind.TASK,
    val visibility: Visibility = Visibility.PRIVATE,
    val date: String? = null
)

data class DuoDeskUiState(
    val currentUserId: String? = "rushil",
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val users: Map<String, UserProfile> = emptyMap(),
    val tasks: List<TaskItem> = emptyList(),
    val reminders: List<ReminderItem> = emptyList(),
    val assignments: List<AssignmentItem> = emptyList(),
    val documents: List<DocumentItem> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val notificationSettings: NotificationSettings = NotificationSettings(
        userId = "rushil",
        deadlineAlerts = true,
        sharedUpdates = true,
        fileUploads = true,
        dailyDigest = false
    ),
    val createDefaults: CreateDefaults = CreateDefaults(),
    val inspectTarget: InspectableItem? = null,
    val previewDocument: DocumentItem? = null,
    val toasts: List<ToastMessage> = emptyList(),
    val isReady: Boolean = false
) {
    val currentUser: UserProfile? get() = currentUserId?.let { users[it] }
    val otherUser: UserProfile?
        get() {
            val otherId = if (currentUserId == "rushil") "alex" else "rushil"
            return users[otherId]
        }
    val unreadNotificationsCount: Int get() = notifications.count { !it.read }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DuoDeskViewModel(private val repo: DuoDeskRepository) : ViewModel() {

    private val _createDefaults = MutableStateFlow(CreateDefaults())
    private val _inspectTarget = MutableStateFlow<InspectableItem?>(null)
    private val _previewDocument = MutableStateFlow<DocumentItem?>(null)
    private val _toasts = MutableStateFlow<List<ToastMessage>>(emptyList())
    private val _isReady = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repo.ensureSeeded()
            _isReady.value = true
        }
    }

    private val activeUserFlow = repo.prefs.activeUserIdFlow

    private val dataFlow = activeUserFlow.flatMapLatest { userId ->
        if (userId == null) {
            combine(
                repo.observeUsers(),
                repo.prefs.themeModeFlow
            ) { users, theme ->
                DuoDeskUiState(
                    currentUserId = null,
                    themeMode = theme,
                    users = users
                )
            }
        } else {
            val firstGroup = combine(
                repo.observeUsers(),
                repo.prefs.themeModeFlow,
                repo.observeTasks(userId),
                repo.observeReminders(userId),
                repo.observeAssignments(userId)
            ) { users, theme, tasks, reminders, assignments ->
                Tuple5(users, theme, tasks, reminders, assignments)
            }
            val secondGroup = combine(
                repo.observeDocuments(userId),
                repo.observeNotes(userId),
                repo.observeNotifications(userId),
                repo.observeNotificationSettings(userId)
            ) { docs, notes, notifications, settings ->
                Tuple4(docs, notes, notifications, settings)
            }
            combine(firstGroup, secondGroup) { g1, g2 ->
                DuoDeskUiState(
                    currentUserId = userId,
                    themeMode = g1.second,
                    users = g1.first,
                    tasks = g1.third,
                    reminders = g1.fourth,
                    assignments = g1.fifth,
                    documents = g2.first,
                    notes = g2.second,
                    notifications = g2.third,
                    notificationSettings = g2.fourth
                )
            }
        }
    }

    private val uiOverlayFlow = combine(
        _createDefaults,
        _inspectTarget,
        _previewDocument,
        _toasts,
        _isReady
    ) { createDefaults, inspect, preview, toasts, ready ->
        Tuple5(createDefaults, inspect, preview, toasts, ready)
    }

    val uiState: StateFlow<DuoDeskUiState> = combine(dataFlow, uiOverlayFlow) { base, overlay ->
        base.copy(
            createDefaults = overlay.first,
            inspectTarget = overlay.second,
            previewDocument = overlay.third,
            toasts = overlay.fourth,
            isReady = overlay.fifth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DuoDeskUiState()
    )

    fun pushToast(title: String, description: String? = null, tone: ToastTone = ToastTone.NEUTRAL) {
        val id = "toast-${UUID.randomUUID()}"
        _toasts.update { current -> (current.takeLast(2) + ToastMessage(id, title, description, tone)) }
        viewModelScope.launch {
            delay(3200)
            dismissToast(id)
        }
    }

    fun dismissToast(id: String) {
        _toasts.update { current -> current.filterNot { it.id == id } }
    }

    fun quickLogin(userId: String) {
        viewModelScope.launch {
            repo.prefs.setActiveUserId(userId)
            val name = uiState.value.users[userId]?.name ?: if (userId == "alex") "Alex" else "Rushil"
            pushToast("Welcome back, $name", "Personal space & Shared Space ready.", ToastTone.SUCCESS)
        }
    }

    fun signInWithEmail(email: String) {
        val target = if (email.contains("alex", ignoreCase = true)) "alex" else "rushil"
        quickLogin(target)
    }

    fun switchUser(userId: String) {
        viewModelScope.launch {
            repo.prefs.setActiveUserId(userId)
            val name = uiState.value.users[userId]?.name ?: if (userId == "alex") "Alex" else "Rushil"
            pushToast("Switched to $name", "Private space updated. Shared items stay synced.", ToastTone.SUCCESS)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.prefs.setActiveUserId(null)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repo.prefs.setThemeMode(mode)
        }
    }

    fun setCreateDefaults(defaults: CreateDefaults) {
        _createDefaults.value = defaults
    }

    fun openInspector(target: InspectableItem?) {
        _inspectTarget.value = target
    }

    fun openDocumentPreview(doc: DocumentItem?) {
        _previewDocument.value = doc
    }

    fun toggleTaskCompleted(task: TaskItem) {
        val actorId = uiState.value.currentUserId ?: return
        val actorName = uiState.value.currentUser?.name ?: "Member"
        viewModelScope.launch {
            repo.updateTask(actorId, actorName, task.copy(completed = !task.completed))
        }
    }

    fun saveTask(updated: TaskItem) {
        val actorId = uiState.value.currentUserId ?: return
        val actorName = uiState.value.currentUser?.name ?: "Member"
        viewModelScope.launch {
            repo.updateTask(actorId, actorName, updated)
            pushToast("Task updated", updated.title, ToastTone.SUCCESS)
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            repo.deleteTask(id)
            pushToast("Task removed")
        }
    }

    fun createTask(
        title: String,
        description: String,
        visibility: Visibility,
        priority: Priority,
        dueDate: String,
        dueTime: String?,
        category: String,
        attachments: List<AttachmentRef>
    ) {
        val ownerId = uiState.value.currentUserId ?: return
        val actorName = uiState.value.currentUser?.name ?: "Member"
        viewModelScope.launch {
            repo.createTask(ownerId, actorName, title, description, visibility, priority, dueDate, dueTime, category, attachments)
            pushToast(
                "Task created",
                "$title (${if (visibility == Visibility.SHARED) "Shared Space" else "My Space"})",
                ToastTone.SUCCESS
            )
        }
    }

    fun toggleReminderCompleted(rem: ReminderItem) {
        viewModelScope.launch {
            repo.updateReminder(rem.copy(completed = !rem.completed))
        }
    }

    fun saveReminder(updated: ReminderItem) {
        viewModelScope.launch {
            repo.updateReminder(updated)
            pushToast("Reminder updated", updated.title, ToastTone.SUCCESS)
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            repo.deleteReminder(id)
            pushToast("Reminder deleted")
        }
    }

    fun createReminder(
        title: String,
        description: String,
        visibility: Visibility,
        date: String,
        time: String,
        priority: Priority,
        repeat: RepeatOption,
        notificationPref: NotificationPreference
    ) {
        val ownerId = uiState.value.currentUserId ?: return
        viewModelScope.launch {
            repo.createReminder(ownerId, title, description, visibility, date, time, priority, repeat, notificationPref)
            pushToast("Reminder scheduled", title, ToastTone.SUCCESS)
        }
    }

    fun updateAssignmentStatus(asg: AssignmentItem, status: AssignmentStatus) {
        viewModelScope.launch {
            repo.updateAssignment(asg.copy(status = status))
        }
    }

    fun addAssignmentAttachments(asg: AssignmentItem, added: List<AttachmentRef>) {
        viewModelScope.launch {
            repo.updateAssignment(asg.copy(attachments = asg.attachments + added))
            pushToast("Attachment added", tone = ToastTone.SUCCESS)
        }
    }

    fun saveAssignment(updated: AssignmentItem) {
        viewModelScope.launch {
            repo.updateAssignment(updated)
            pushToast("Assignment updated", updated.title, ToastTone.SUCCESS)
        }
    }

    fun deleteAssignment(id: String) {
        viewModelScope.launch {
            repo.deleteAssignment(id)
            pushToast("Assignment deleted")
        }
    }

    fun createAssignment(
        title: String,
        subject: String,
        description: String,
        visibility: Visibility,
        deadline: String,
        deadlineTime: String?,
        priority: Priority,
        attachments: List<AttachmentRef>
    ) {
        val ownerId = uiState.value.currentUserId ?: return
        val actorName = uiState.value.currentUser?.name ?: "Member"
        viewModelScope.launch {
            repo.createAssignment(ownerId, actorName, title, subject, description, visibility, deadline, deadlineTime, priority, attachments)
            pushToast("Assignment added", title, ToastTone.SUCCESS)
        }
    }

    fun createNote(
        title: String,
        content: String,
        type: NoteType,
        checklist: List<ChecklistRow>,
        visibility: Visibility,
        onCreated: (String) -> Unit = {}
    ) {
        val ownerId = uiState.value.currentUserId ?: return
        viewModelScope.launch {
            val created = repo.createNote(ownerId, title, content, type, checklist, visibility)
            onCreated(created.id)
            pushToast(
                if (type == NoteType.CHECKLIST) "Checklist created" else "Note saved",
                title,
                ToastTone.SUCCESS
            )
        }
    }

    fun updateNote(updated: NoteItem) {
        viewModelScope.launch {
            repo.updateNote(updated)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repo.deleteNote(id)
            pushToast("Note deleted")
        }
    }

    fun uploadDocument(
        name: String,
        kind: DocumentKind,
        extension: String,
        mimeType: String,
        sizeBytes: Long,
        visibility: Visibility,
        previewUri: String?,
        summary: String
    ) {
        val ownerId = uiState.value.currentUserId ?: return
        val actorName = uiState.value.currentUser?.name ?: "Member"
        viewModelScope.launch {
            repo.createDocument(ownerId, actorName, name, kind, extension, mimeType, sizeBytes, visibility, previewUri, summary)
            pushToast(
                "File uploaded",
                "$name saved to ${if (visibility == Visibility.SHARED) "Shared Space" else "My Space"}",
                ToastTone.SUCCESS
            )
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repo.deleteDocument(id)
            pushToast("Document deleted")
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repo.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        val userId = uiState.value.currentUserId ?: return
        viewModelScope.launch {
            repo.markAllNotificationsRead(userId)
            pushToast("All notifications marked as read", tone = ToastTone.SUCCESS)
        }
    }

    fun clearNotifications() {
        val userId = uiState.value.currentUserId ?: return
        viewModelScope.launch {
            repo.clearNotifications(userId)
            pushToast("Notifications cleared")
        }
    }

    fun updateProfile(name: String, email: String, role: String) {
        val current = uiState.value.currentUser ?: return
        val updatedFullName = if (name.isNotBlank() && current.fullName.startsWith(current.name)) {
            current.fullName.replaceFirst(current.name, name.trim())
        } else {
            name.trim().ifBlank { current.fullName }
        }
        viewModelScope.launch {
            repo.updateUserProfile(
                current.copy(
                    name = name.trim().ifBlank { current.name },
                    fullName = updatedFullName,
                    email = email.trim().ifBlank { current.email },
                    role = role.trim().ifBlank { current.role }
                )
            )
            pushToast("Profile updated", tone = ToastTone.SUCCESS)
        }
    }

    fun updateNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            repo.updateNotificationSettings(settings)
            pushToast("Notification preferences saved", tone = ToastTone.SUCCESS)
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repo.resetDemoData()
            pushToast(
                "Demonstration data restored",
                "All sample tasks, assignments, and documents reset.",
                ToastTone.SUCCESS
            )
        }
    }

    private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    private data class Tuple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

    class Factory(private val repo: DuoDeskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DuoDeskViewModel(repo) as T
        }
    }
}
