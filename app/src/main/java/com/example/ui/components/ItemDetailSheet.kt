package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.theme.LocalDuoDeskPalette
import com.example.util.Formatters
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
    target: InspectableItem,
    users: Map<String, UserProfile>,
    onDismiss: () -> Unit,
    onSaveTask: (TaskItem) -> Unit,
    onDeleteTask: (String) -> Unit,
    onSaveReminder: (ReminderItem) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onSaveAssignment: (AssignmentItem) -> Unit,
    onDeleteAssignment: (String) -> Unit
) {
    val palette = LocalDuoDeskPalette.current
    val context = LocalContext.current

    var title by remember(target) {
        mutableStateOf(
            when (target) {
                is InspectableItem.Task -> target.item.title
                is InspectableItem.Reminder -> target.item.title
                is InspectableItem.Assignment -> target.item.title
            }
        )
    }
    var description by remember(target) {
        mutableStateOf(
            when (target) {
                is InspectableItem.Task -> target.item.description
                is InspectableItem.Reminder -> target.item.description
                is InspectableItem.Assignment -> target.item.description
            }
        )
    }
    var date by remember(target) {
        mutableStateOf(
            when (target) {
                is InspectableItem.Task -> target.item.dueDate
                is InspectableItem.Reminder -> target.item.date
                is InspectableItem.Assignment -> target.item.deadline
            }
        )
    }
    var time by remember(target) {
        mutableStateOf(
            when (target) {
                is InspectableItem.Task -> target.item.dueTime.orEmpty()
                is InspectableItem.Reminder -> target.item.time
                is InspectableItem.Assignment -> target.item.deadlineTime.orEmpty()
            }
        )
    }
    var priority by remember(target) {
        mutableStateOf(
            when (target) {
                is InspectableItem.Task -> target.item.priority
                is InspectableItem.Reminder -> target.item.priority
                is InspectableItem.Assignment -> target.item.priority
            }
        )
    }
    var visibility by remember(target) {
        mutableStateOf(
            when (target) {
                is InspectableItem.Task -> target.item.visibility
                is InspectableItem.Reminder -> target.item.visibility
                is InspectableItem.Assignment -> target.item.visibility
            }
        )
    }
    var completed by remember(target) {
        mutableStateOf(
            when (target) {
                is InspectableItem.Task -> target.item.completed
                is InspectableItem.Reminder -> target.item.completed
                is InspectableItem.Assignment -> false
            }
        )
    }
    var assignmentStatus by remember(target) {
        mutableStateOf(
            when (target) {
                is InspectableItem.Assignment -> target.item.status
                else -> AssignmentStatus.PENDING
            }
        )
    }

    val ownerId = when (target) {
        is InspectableItem.Task -> target.item.ownerId
        is InspectableItem.Reminder -> target.item.ownerId
        is InspectableItem.Assignment -> target.item.ownerId
    }
    val owner = users[ownerId]
    val typeLabel = when (target) {
        is InspectableItem.Task -> "Task"
        is InspectableItem.Reminder -> "Reminder"
        is InspectableItem.Assignment -> "Assignment"
    }
    val attachments = when (target) {
        is InspectableItem.Task -> target.item.attachments
        is InspectableItem.Assignment -> target.item.attachments
        is InspectableItem.Reminder -> emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(user = owner, size = 34.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "$typeLabel by ${owner?.name ?: "Member"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = buildString {
                                append(Formatters.formatFullDate(date))
                                if (time.isNotBlank()) append(" · ${Formatters.formatTime12h(time)}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close detail view", tint = palette.textSecondary)
                }
            }

            HorizontalDivider(color = palette.divider)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_title_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_notes_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val parsed = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now() }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                date = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                            },
                            parsed.year,
                            parsed.monthValue - 1,
                            parsed.dayOfMonth
                        ).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Formatters.formatShortDate(date), style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = {
                        val parts = time.split(":")
                        val h = parts.getOrNull(0)?.toIntOrNull() ?: 17
                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        TimePickerDialog(
                            context,
                            { _, selH, selM ->
                                time = String.format(Locale.US, "%02d:%02d", selH, selM)
                            },
                            h,
                            m,
                            false
                        ).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (time.isBlank()) "All day" else Formatters.formatTime12h(time),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Priority, Visibility, Status row
            Text("Priority & Workspace", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.entries.forEach { p ->
                    val selected = priority == p
                    FilterChip(
                        selected = selected,
                        onClick = { priority = p },
                        label = { Text(p.shortLabel()) },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        visibility = if (visibility == Visibility.PRIVATE) Visibility.SHARED else Visibility.PRIVATE
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (visibility == Visibility.SHARED) palette.lavender else palette.pink),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (visibility == Visibility.SHARED) palette.lavenderSoft.copy(alpha = 0.5f) else palette.pinkSoft.copy(alpha = 0.5f),
                        contentColor = if (visibility == Visibility.SHARED) palette.lavenderInk else palette.pinkInk
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (visibility == Visibility.SHARED) Icons.Default.Group else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (visibility == Visibility.SHARED) "Shared" else "My Space", style = MaterialTheme.typography.labelMedium)
                }

                if (target is InspectableItem.Assignment) {
                    OutlinedButton(
                        onClick = {
                            assignmentStatus = when (assignmentStatus) {
                                AssignmentStatus.PENDING -> AssignmentStatus.IN_PROGRESS
                                AssignmentStatus.IN_PROGRESS -> AssignmentStatus.COMPLETED
                                AssignmentStatus.COMPLETED -> AssignmentStatus.PENDING
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(assignmentStatus.label, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = { completed = !completed },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (completed) palette.sage else palette.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (completed) palette.sageSoft else palette.subtleSurface,
                            contentColor = if (completed) palette.sageInk else palette.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (completed) "Completed" else "Pending", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (attachments.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.subtleSurface,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        attachments.forEach { att ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = palette.lavender,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = att.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = Formatters.formatFileSize(att.sizeBytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        when (target) {
                            is InspectableItem.Task -> onDeleteTask(target.item.id)
                            is InspectableItem.Reminder -> onDeleteReminder(target.item.id)
                            is InspectableItem.Assignment -> onDeleteAssignment(target.item.id)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = palette.error),
                    modifier = Modifier.testTag("detail_delete_button")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = palette.textSecondary)
                    }
                    Button(
                        onClick = {
                            val trimmedTitle = title.trim()
                            if (trimmedTitle.isEmpty()) return@Button
                            when (target) {
                                is InspectableItem.Task -> onSaveTask(
                                    target.item.copy(
                                        title = trimmedTitle,
                                        description = description.trim(),
                                        dueDate = date,
                                        dueTime = time.ifBlank { null },
                                        priority = priority,
                                        visibility = visibility,
                                        completed = completed
                                    )
                                )
                                is InspectableItem.Reminder -> onSaveReminder(
                                    target.item.copy(
                                        title = trimmedTitle,
                                        description = description.trim(),
                                        date = date,
                                        time = time.ifBlank { "09:00" },
                                        priority = priority,
                                        visibility = visibility,
                                        completed = completed
                                    )
                                )
                                is InspectableItem.Assignment -> onSaveAssignment(
                                    target.item.copy(
                                        title = trimmedTitle,
                                        description = description.trim(),
                                        deadline = date,
                                        deadlineTime = time.ifBlank { null },
                                        priority = priority,
                                        visibility = visibility,
                                        status = assignmentStatus
                                    )
                                )
                            }
                            onDismiss()
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryButtonBg,
                            contentColor = palette.primaryButtonText
                        ),
                        modifier = Modifier.testTag("detail_save_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
