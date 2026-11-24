package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.SeedData
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalDuoDeskPalette
import com.example.util.FileHelpers
import com.example.util.Formatters
import java.util.UUID

private enum class StatusFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Done")
}

@Composable
fun SpaceScreen(
    isShared: Boolean,
    currentUser: UserProfile?,
    users: Map<String, UserProfile>,
    tasks: List<TaskItem>,
    reminders: List<ReminderItem>,
    assignments: List<AssignmentItem>,
    notes: List<NoteItem>,
    documents: List<DocumentItem>,
    onToggleTask: (TaskItem) -> Unit,
    onDeleteTask: (String) -> Unit,
    onToggleReminder: (ReminderItem) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onUpdateAssignmentStatus: (AssignmentItem, AssignmentStatus) -> Unit,
    onAttachToAssignment: (AssignmentItem, List<AttachmentRef>) -> Unit,
    onDeleteAssignment: (String) -> Unit,
    onCreateNote: (String, String, NoteType, List<ChecklistRow>, Visibility, (String) -> Unit) -> Unit,
    onUpdateNote: (NoteItem) -> Unit,
    onDeleteNote: (String) -> Unit,
    onUploadDocument: (String, DocumentKind, String, String, Long, Visibility, String?, String) -> Unit,
    onPreviewDocument: (DocumentItem) -> Unit,
    onDeleteDocument: (String) -> Unit,
    onInspectItem: (InspectableItem) -> Unit,
    onOpenCreate: (CreateKind, Visibility) -> Unit
) {
    val palette = LocalDuoDeskPalette.current
    val context = LocalContext.current
    val currentUserId = currentUser?.id.orEmpty()
    val targetVisibility = if (isShared) Visibility.SHARED else Visibility.PRIVATE

    var activeTab by remember { mutableStateOf(SpaceTab.TASKS) }
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(StatusFilter.ALL) }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }
    var newChecklistLine by remember { mutableStateOf("") }
    var attachingAssignment by remember { mutableStateOf<AssignmentItem?>(null) }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            val meta = FileHelpers.readPickedFileMeta(context, uri)
            onUploadDocument(
                meta.name,
                meta.kind,
                meta.extension,
                meta.mimeType,
                meta.sizeBytes,
                targetVisibility,
                if (meta.kind == DocumentKind.IMAGE) meta.uriString else null,
                if (isShared) "Shared in DuoDesk Shared Space" else "Saved in My Space"
            )
        }
    }

    val attachPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val target = attachingAssignment
        if (target != null && uris.isNotEmpty()) {
            val refs = uris.map { FileHelpers.toAttachmentRef(FileHelpers.readPickedFileMeta(context, it)) }
            onAttachToAssignment(target, refs)
        }
        attachingAssignment = null
    }

    val q = search.trim().lowercase()

    val filteredTasks = remember(tasks, isShared, currentUserId, statusFilter, q) {
        tasks.filter { t ->
            if (isShared) t.visibility == Visibility.SHARED
            else t.visibility == Visibility.PRIVATE && t.ownerId == currentUserId
        }.filter { t ->
            when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.PENDING -> !t.completed
                StatusFilter.COMPLETED -> t.completed
            }
        }.filter { t -> q.isEmpty() || t.title.lowercase().contains(q) }
            .sortedBy { it.dueDate }
    }

    val filteredReminders = remember(reminders, isShared, currentUserId, statusFilter, q) {
        reminders.filter { r ->
            if (isShared) r.visibility == Visibility.SHARED
            else r.visibility == Visibility.PRIVATE && r.ownerId == currentUserId
        }.filter { r ->
            when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.PENDING -> !r.completed
                StatusFilter.COMPLETED -> r.completed
            }
        }.filter { r -> q.isEmpty() || r.title.lowercase().contains(q) }
            .sortedBy { it.date }
    }

    val filteredAssignments = remember(assignments, isShared, currentUserId, statusFilter, q) {
        assignments.filter { a ->
            if (isShared) a.visibility == Visibility.SHARED
            else a.visibility == Visibility.PRIVATE && a.ownerId == currentUserId
        }.filter { a ->
            when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.PENDING -> a.status != AssignmentStatus.COMPLETED
                StatusFilter.COMPLETED -> a.status == AssignmentStatus.COMPLETED
            }
        }.filter { a ->
            q.isEmpty() || a.title.lowercase().contains(q) || a.subject.lowercase().contains(q)
        }.sortedBy { it.deadline }
    }

    val filteredNotes = remember(notes, isShared, currentUserId, q) {
        notes.filter { n ->
            if (isShared) n.visibility == Visibility.SHARED
            else n.visibility == Visibility.PRIVATE && n.ownerId == currentUserId
        }.filter { n ->
            q.isEmpty() || n.title.lowercase().contains(q) || n.content.lowercase().contains(q)
        }.sortedByDescending { it.updatedAt }
    }

    val filteredDocuments = remember(documents, isShared, currentUserId, q) {
        documents.filter { d ->
            if (isShared) d.visibility == Visibility.SHARED
            else d.visibility == Visibility.PRIVATE && d.ownerId == currentUserId
        }.filter { d -> q.isEmpty() || d.name.lowercase().contains(q) }
    }

    LaunchedEffect(filteredNotes) {
        if (filteredNotes.isNotEmpty() && (selectedNoteId == null || filteredNotes.none { it.id == selectedNoteId })) {
            selectedNoteId = filteredNotes.first().id
        }
    }
    val activeNote = filteredNotes.firstOrNull { it.id == selectedNoteId }

    val memberList = remember(users) { users.values.take(2) }
    val memberLabel = if (memberList.size >= 2) {
        "${memberList[0].name} & ${memberList[1].name}"
    } else {
        memberList.firstOrNull()?.name ?: "DuoDesk"
    }

    val tabOrder = if (isShared) {
        listOf(SpaceTab.TASKS, SpaceTab.ASSIGNMENTS, SpaceTab.REMINDERS, SpaceTab.NOTES, SpaceTab.DOCUMENTS)
    } else {
        listOf(SpaceTab.TASKS, SpaceTab.REMINDERS, SpaceTab.ASSIGNMENTS, SpaceTab.NOTES, SpaceTab.DOCUMENTS)
    }

    fun openCreateForTab(tab: SpaceTab) {
        val kind = when (tab) {
            SpaceTab.TASKS -> CreateKind.TASK
            SpaceTab.REMINDERS -> CreateKind.REMINDER
            SpaceTab.ASSIGNMENTS -> CreateKind.ASSIGNMENT
            SpaceTab.NOTES -> CreateKind.NOTE
            SpaceTab.DOCUMENTS -> CreateKind.DOCUMENT
        }
        onOpenCreate(kind, targetVisibility)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 960.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Space Header Card
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isShared) {
                                Brush.linearGradient(listOf(palette.sharedHeaderStart, palette.sharedHeaderEnd))
                            } else {
                                Brush.linearGradient(listOf(palette.card, palette.card))
                            }
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isShared) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.width(46.dp)) {
                                    memberList.getOrNull(0)?.let {
                                        UserAvatar(user = it, size = 28.dp)
                                    }
                                    memberList.getOrNull(1)?.let {
                                        UserAvatar(
                                            user = it,
                                            size = 28.dp,
                                            modifier = Modifier.offset(x = 18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$memberLabel · Shared Space",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.lavenderInk
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = palette.pinkInk,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Private to ${currentUser?.name ?: "Member"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.pinkInk
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isShared) "Shared Space" else "My Space",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isShared) {
                                        "Everything you both collaborate on, cleanly separated from private workspaces."
                                    } else {
                                        "Personal tasks, reminders, assignments, notes, and private files."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { openCreateForTab(activeTab) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isShared) palette.lavender else palette.primaryButtonBg,
                                contentColor = if (isShared) palette.primaryButtonText else palette.primaryButtonText
                            ),
                            modifier = Modifier.testTag(if (isShared) "add_shared_item_button" else "add_my_space_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isShared) "Add shared item" else "Add to My Space", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Category Pills Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabOrder.forEach { tab ->
                    val count = when (tab) {
                        SpaceTab.TASKS -> filteredTasks.size
                        SpaceTab.REMINDERS -> filteredReminders.size
                        SpaceTab.ASSIGNMENTS -> filteredAssignments.size
                        SpaceTab.NOTES -> filteredNotes.size
                        SpaceTab.DOCUMENTS -> filteredDocuments.size
                    }
                    val active = activeTab == tab
                    val activeBg = if (isShared) palette.lavenderSoft else palette.pinkSoft
                    val activeFg = if (isShared) palette.lavenderInk else palette.pinkInk

                    Surface(
                        onClick = { activeTab = tab },
                        shape = CircleShape,
                        color = if (active) activeBg else palette.card,
                        border = BorderStroke(1.dp, if (active) activeBg else palette.border),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("space_tab_${tab.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) activeFg else palette.textSecondary
                            )
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = (if (active) activeFg else palette.textSecondary).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Search & Status Filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text(if (isShared) "Search Shared Space..." else "Search My Space...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("space_search_input")
                )

                if (activeTab == SpaceTab.TASKS || activeTab == SpaceTab.REMINDERS || activeTab == SpaceTab.ASSIGNMENTS) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, palette.border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = palette.card,
                                contentColor = palette.textPrimary
                            ),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text(statusFilter.label, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Filter status", modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            StatusFilter.entries.forEach { sf ->
                                DropdownMenuItem(
                                    text = { Text(sf.label) },
                                    onClick = {
                                        statusFilter = sf
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Tab Body
        when (activeTab) {
            SpaceTab.TASKS -> {
                if (filteredTasks.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = if (isShared) Icons.Default.Group else Icons.Default.CheckBox,
                            title = if (isShared) "No shared tasks here" else "No personal tasks yet",
                            description = if (isShared) {
                                "Add a shared task so both Rushil and Alex can track and complete it together."
                            } else {
                                "Create a private task to organize your day."
                            },
                            actionLabel = if (isShared) "Add shared task" else "Create task",
                            onAction = { openCreateForTab(SpaceTab.TASKS) }
                        )
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        val creator = users[task.ownerId]
                        val overdue = Formatters.isOverdueDate(task.dueDate, task.completed)
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = palette.card,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { onInspectItem(InspectableItem.Task(task)) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedCheckbox(
                                    checked = task.completed,
                                    onCheckedChange = { onToggleTask(task) },
                                    contentDescription = "Complete ${task.title}",
                                    accent = if (isShared) CheckboxAccent.LAVENDER else CheckboxAccent.PINK
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                        color = if (task.completed) palette.textTertiary else palette.textPrimary,
                                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isShared) {
                                            UserAvatar(user = creator, size = 18.dp)
                                            Text(
                                                text = "By ${creator?.name ?: "Member"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textPrimary
                                            )
                                            Text("·", color = palette.textSecondary)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(priorityDotColor(task.priority))
                                        )
                                        Text(
                                            text = task.priority.shortLabel(),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                            color = palette.textSecondary
                                        )
                                        if (!isShared && task.category.isNotBlank()) {
                                            Text("· ${task.category}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = palette.textSecondary)
                                        }
                                        if (isShared && task.attachments.isNotEmpty()) {
                                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(13.dp))
                                            Text("${task.attachments.size}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = Formatters.formatShortDate(task.dueDate),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (overdue) palette.error else palette.textSecondary,
                                        fontWeight = if (overdue) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (!isShared) {
                                        IconButton(
                                            onClick = { onDeleteTask(task.id) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${task.title}", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SpaceTab.REMINDERS -> {
                if (filteredReminders.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Notifications,
                            title = if (isShared) "No shared reminders" else "No private reminders",
                            description = if (isShared) {
                                "Schedule a shared reminder so neither of you misses a sync or submission."
                            } else {
                                "Schedule a personal reminder with custom repeat and alert settings."
                            },
                            actionLabel = if (isShared) "Add shared reminder" else "Add reminder",
                            onAction = { openCreateForTab(SpaceTab.REMINDERS) }
                        )
                    }
                } else {
                    items(filteredReminders, key = { it.id }) { rem ->
                        val creator = users[rem.ownerId]
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = palette.card,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { onInspectItem(InspectableItem.Reminder(rem)) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedCheckbox(
                                    checked = rem.completed,
                                    onCheckedChange = { onToggleReminder(rem) },
                                    contentDescription = "Toggle ${rem.title}",
                                    accent = if (isShared) CheckboxAccent.LAVENDER else CheckboxAccent.PEACH
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rem.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                        color = if (rem.completed) palette.textTertiary else palette.textPrimary,
                                        textDecoration = if (rem.completed) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isShared) {
                                            UserAvatar(user = creator, size = 18.dp)
                                            Text("By ${creator?.name ?: "Member"} ·", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                        }
                                        Text(
                                            text = buildString {
                                                append("${Formatters.formatShortDate(rem.date)} · ${Formatters.formatTime12h(rem.time)}")
                                                if (rem.repeat != RepeatOption.NONE) append(" · ${rem.repeat.label}")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                }
                                if (!isShared) {
                                    IconButton(onClick = { onDeleteReminder(rem.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${rem.title}", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SpaceTab.ASSIGNMENTS -> {
                if (filteredAssignments.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.MenuBook,
                            title = if (isShared) "No shared assignments" else "No private assignments",
                            description = if (isShared) {
                                "Add a joint project or assignment for you and Alex."
                            } else {
                                "Track coursework deadlines and attach PDFs or documents privately."
                            },
                            actionLabel = if (isShared) "Add shared assignment" else "Add assignment",
                            onAction = { openCreateForTab(SpaceTab.ASSIGNMENTS) }
                        )
                    }
                } else {
                    items(filteredAssignments, key = { it.id }) { asg ->
                        val creator = users[asg.ownerId]
                        var statusMenuOpen by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = palette.card,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { onInspectItem(InspectableItem.Assignment(asg)) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = asg.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                        color = palette.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isShared) {
                                            UserAvatar(user = creator, size = 18.dp)
                                            Text("By ${creator?.name ?: "Member"} ·", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                        }
                                        Text(
                                            text = "${asg.subject} · Due ${Formatters.formatShortDate(asg.deadline)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                            color = palette.textSecondary
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            attachingAssignment = asg
                                            attachPickerLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/*"))
                                        },
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, palette.border),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = "Attach file", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${asg.attachments.size}", style = MaterialTheme.typography.labelSmall)
                                    }

                                    Box {
                                        OutlinedButton(
                                            onClick = { statusMenuOpen = true },
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, palette.border),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(asg.status.label, style = MaterialTheme.typography.labelMedium)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        DropdownMenu(expanded = statusMenuOpen, onDismissRequest = { statusMenuOpen = false }) {
                                            AssignmentStatus.entries.forEach { st ->
                                                DropdownMenuItem(
                                                    text = { Text(st.label) },
                                                    onClick = {
                                                        onUpdateAssignmentStatus(asg, st)
                                                        statusMenuOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    if (!isShared) {
                                        IconButton(onClick = { onDeleteAssignment(asg.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${asg.title}", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SpaceTab.NOTES -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    onCreateNote(
                                        if (isShared) "Shared note" else "Untitled note",
                                        "",
                                        NoteType.NOTE,
                                        emptyList(),
                                        targetVisibility
                                    ) { createdId -> selectedNoteId = createdId }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isShared) palette.lavender else palette.primaryButtonBg,
                                    contentColor = palette.primaryButtonText
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+ Note", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = {
                                    onCreateNote(
                                        if (isShared) "Shared checklist" else "New checklist",
                                        "",
                                        NoteType.CHECKLIST,
                                        listOf(ChecklistRow("c-${UUID.randomUUID().toString().take(6)}", "First item", false)),
                                        targetVisibility
                                    ) { createdId -> selectedNoteId = createdId }
                                },
                                shape = CircleShape,
                                border = BorderStroke(1.dp, palette.border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = palette.card,
                                    contentColor = palette.textPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+ Checklist", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (filteredNotes.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.Default.StickyNote2,
                                title = "No notes yet",
                                description = "Create a note or interactive checklist using the buttons above."
                            )
                        } else {
                            // Selector Strip
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filteredNotes.forEach { n ->
                                    val selected = n.id == activeNote?.id
                                    val creator = users[n.ownerId]
                                    Surface(
                                        onClick = { selectedNoteId = n.id },
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (selected) palette.lavenderSoft.copy(alpha = 0.65f) else palette.card,
                                        border = BorderStroke(1.dp, if (selected) palette.lavender else palette.border),
                                        modifier = Modifier.widthIn(min = 150.dp, max = 210.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = n.title.ifBlank { "Untitled" },
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = palette.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = if (isShared) {
                                                    "By ${creator?.name ?: "Member"} · ${Formatters.formatRelativeTime(n.updatedAt)}"
                                                } else {
                                                    Formatters.formatRelativeTime(n.updatedAt)
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Active Note Editor
                            if (activeNote != null) {
                                var noteTitle by remember(activeNote.id) { mutableStateOf(activeNote.title) }
                                var noteContent by remember(activeNote.id) { mutableStateOf(activeNote.content) }

                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = palette.card,
                                    border = BorderStroke(1.dp, palette.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = noteTitle,
                                                onValueChange = {
                                                    noteTitle = it
                                                    onUpdateNote(activeNote.copy(title = it))
                                                },
                                                singleLine = true,
                                                label = { Text("Title") },
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(onClick = { onDeleteNote(activeNote.id) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete note", tint = palette.error)
                                            }
                                        }

                                        OutlinedTextField(
                                            value = noteContent,
                                            onValueChange = {
                                                noteContent = it
                                                onUpdateNote(activeNote.copy(content = it))
                                            },
                                            placeholder = { Text(if (isShared) "Write together..." else "Write your thoughts...") },
                                            minLines = if (activeNote.type == NoteType.CHECKLIST) 2 else 6,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (activeNote.type == NoteType.CHECKLIST) {
                                            HorizontalDivider(color = palette.divider)
                                            activeNote.checklist.forEach { row ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AnimatedCheckbox(
                                                        checked = row.checked,
                                                        onCheckedChange = {
                                                            val next = activeNote.checklist.map {
                                                                if (it.id == row.id) it.copy(checked = !it.checked) else it
                                                            }
                                                            onUpdateNote(activeNote.copy(checklist = next))
                                                        },
                                                        contentDescription = row.text,
                                                        accent = if (isShared) CheckboxAccent.LAVENDER else CheckboxAccent.SAGE
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = row.text,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (row.checked) palette.textTertiary else palette.textPrimary,
                                                        textDecoration = if (row.checked) TextDecoration.LineThrough else TextDecoration.None,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    TextButton(
                                                        onClick = {
                                                            val next = activeNote.checklist.filterNot { it.id == row.id }
                                                            onUpdateNote(activeNote.copy(checklist = next))
                                                        }
                                                    ) {
                                                        Text("Remove", color = palette.textSecondary, fontSize = 11.sp)
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = newChecklistLine,
                                                    onValueChange = { newChecklistLine = it },
                                                    placeholder = { Text("Add item...", fontSize = 13.sp) },
                                                    singleLine = true,
                                                    shape = CircleShape,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Button(
                                                    onClick = {
                                                        val trimmed = newChecklistLine.trim()
                                                        if (trimmed.isNotEmpty()) {
                                                            val next = activeNote.checklist + ChecklistRow(
                                                                id = "c-${UUID.randomUUID().toString().take(6)}",
                                                                text = trimmed,
                                                                checked = false
                                                            )
                                                            onUpdateNote(activeNote.copy(checklist = next))
                                                            newChecklistLine = ""
                                                        }
                                                    },
                                                    shape = CircleShape,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = palette.primaryButtonBg,
                                                        contentColor = palette.primaryButtonText
                                                    )
                                                ) {
                                                    Text("Add")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SpaceTab.DOCUMENTS -> {
                item {
                    Surface(
                        onClick = {
                            docPickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "image/*"
                                )
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = palette.card,
                        border = BorderStroke(1.5.dp, if (isShared) palette.lavender else palette.pink),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.UploadFile,
                                contentDescription = "Upload document",
                                tint = if (isShared) palette.lavender else palette.pink,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isShared) "Tap to upload shared PDFs, Word docs, or images" else "Tap to upload private PDFs, Word docs, or images",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isShared) "Visible to $memberLabel" else "Only visible to ${currentUser?.name ?: "you"}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = palette.textSecondary
                            )
                        }
                    }
                }

                if (filteredDocuments.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Description,
                            title = if (isShared) "No shared documents" else "No private documents",
                            description = "Upload study sheets, PDFs, or images here."
                        )
                    }
                } else {
                    items(filteredDocuments, key = { it.id }) { doc ->
                        val owner = users[doc.ownerId]
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = palette.card,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                val drawableId = when (doc.previewUri) {
                                    SeedData.PREVIEW_PRESENTATION -> R.drawable.img_doc_presentation_preview
                                    SeedData.PREVIEW_WIREFRAME -> R.drawable.img_doc_architecture_wireframe
                                    else -> null
                                }
                                if (drawableId != null) {
                                    Image(
                                        painter = painterResource(id = drawableId),
                                        contentDescription = doc.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clickable { onPreviewDocument(doc) }
                                    )
                                } else if (!doc.previewUri.isNullOrBlank() && doc.previewUri.startsWith("content://")) {
                                    AsyncImage(
                                        model = doc.previewUri,
                                        contentDescription = doc.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clickable { onPreviewDocument(doc) }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(84.dp)
                                            .background(palette.subtleSurface)
                                            .clickable { onPreviewDocument(doc) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (doc.kind == DocumentKind.IMAGE) Icons.Default.Image else Icons.Default.Description,
                                            contentDescription = null,
                                            tint = if (isShared) palette.lavender else palette.pink,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = doc.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isShared) {
                                            UserAvatar(user = owner, size = 18.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("By ${owner?.name ?: "Member"} · ", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                        }
                                        Text(
                                            text = "${doc.extension} · ${Formatters.formatFileSize(doc.sizeBytes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = palette.divider)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { onPreviewDocument(doc) }) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Preview")
                                        }
                                        TextButton(
                                            onClick = { onDeleteDocument(doc.id) },
                                            colors = ButtonDefaults.textButtonColors(contentColor = palette.error)
                                        ) {
                                            Text("Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
