package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.AnimatedCheckbox
import com.example.ui.components.CheckboxAccent
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.priorityDotColor
import com.example.ui.theme.LocalDuoDeskPalette
import com.example.util.Formatters

private data class UpcomingEntry(
    val id: String,
    val title: String,
    val date: String,
    val visibility: Visibility,
    val kindLabel: String,
    val raw: InspectableItem
)

@Composable
fun HomeScreen(
    tasks: List<TaskItem>,
    assignments: List<AssignmentItem>,
    reminders: List<ReminderItem>,
    onToggleTask: (TaskItem) -> Unit,
    onInspectItem: (InspectableItem) -> Unit,
    onViewAllTasks: () -> Unit,
    onOpenCalendar: () -> Unit,
    onCreateTask: () -> Unit
) {
    val palette = LocalDuoDeskPalette.current
    val todayIso = remember { Formatters.getTodayIso() }

    val todayTasksAll = remember(tasks, todayIso) {
        val dueToday = tasks.filter { it.dueDate == todayIso }
        if (dueToday.isNotEmpty()) dueToday else tasks.take(6)
    }

    val totalCount = todayTasksAll.size
    val completedCount = todayTasksAll.count { it.completed }
    val pendingCount = totalCount - completedCount
    val completionPct = if (totalCount > 0) ((completedCount.toFloat() / totalCount) * 100f).toInt() else 0

    val displayTodayTasks = remember(todayTasksAll) {
        todayTasksAll
            .sortedWith(compareBy<TaskItem> { it.completed }.thenBy { it.dueTime ?: "23:59" })
            .take(4)
    }

    val upcomingItems = remember(assignments, reminders, tasks, todayIso) {
        buildList {
            assignments
                .filter { it.status != AssignmentStatus.COMPLETED }
                .forEach { a ->
                    add(
                        UpcomingEntry(
                            id = "a-${a.id}",
                            title = a.title,
                            date = a.deadline,
                            visibility = a.visibility,
                            kindLabel = "Assignment",
                            raw = InspectableItem.Assignment(a)
                        )
                    )
                }
            reminders
                .filter { !it.completed && it.date >= todayIso }
                .forEach { r ->
                    add(
                        UpcomingEntry(
                            id = "r-${r.id}",
                            title = r.title,
                            date = r.date,
                            visibility = r.visibility,
                            kindLabel = "Reminder",
                            raw = InspectableItem.Reminder(r)
                        )
                    )
                }
            tasks
                .filter { !it.completed && it.dueDate > todayIso }
                .forEach { t ->
                    add(
                        UpcomingEntry(
                            id = "t-${t.id}",
                            title = t.title,
                            date = t.dueDate,
                            visibility = t.visibility,
                            kindLabel = "Task",
                            raw = InspectableItem.Task(t)
                        )
                    )
                }
        }.sortedBy { it.date }.take(3)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = completionPct / 100f,
        animationSpec = tween(durationMillis = 550),
        label = "today_progress_ring"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 960.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Let's make today productive.",
                style = MaterialTheme.typography.displaySmall,
                color = palette.textPrimary
            )
        }

        // Today's Progress Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(palette.heroStart, palette.heroMid, palette.heroEnd)
                            )
                        )
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TODAY'S PROGRESS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = palette.lavenderInk
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$completionPct%",
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "completed today",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "$totalCount Total  ·  $completedCount Completed  ·  $pendingCount Pending",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(palette.card.copy(alpha = 0.78f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val trackColor = palette.border
                            val progressColor = palette.lavender
                            Canvas(modifier = Modifier.size(60.dp)) {
                                val strokeWidth = 6.dp.toPx()
                                drawArc(
                                    color = trackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = progressColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedProgress,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "$completionPct percent completed",
                                tint = palette.lavender,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Today's Tasks Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Tasks",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    TextButton(
                        onClick = onViewAllTasks,
                        modifier = Modifier.testTag("home_view_all_tasks")
                    ) {
                        Text("View All", color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                if (displayTodayTasks.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Default.CheckCircle,
                        title = "Nothing queued for today",
                        description = "Add a task to your personal space or collaborate with Alex in Shared Space.",
                        actionLabel = "Create task",
                        onAction = onCreateTask
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        displayTodayTasks.forEach { task ->
                            val isShared = task.visibility == Visibility.SHARED
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = palette.card,
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable { onInspectItem(InspectableItem.Task(task)) }
                                    .testTag("home_task_card_${task.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.Top
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
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (!task.dueTime.isNullOrBlank()) {
                                                Text(
                                                    text = Formatters.formatTime12h(task.dueTime),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textSecondary
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
                                            Text("·", color = palette.textSecondary)
                                            Icon(
                                                imageVector = if (isShared) Icons.Default.Group else Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = if (isShared) palette.lavender else palette.pink,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = if (isShared) "Shared" else "Personal",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                                color = palette.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Compact Upcoming Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    TextButton(
                        onClick = onOpenCalendar,
                        modifier = Modifier.testTag("home_open_calendar")
                    ) {
                        Text("Open calendar", color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = palette.card,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (upcomingItems.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "No upcoming deadlines in the next few days.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onCreateTask) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = palette.lavender, modifier = Modifier.size(16.dp))
                                Text("Add one", color = palette.lavender)
                            }
                        }
                    } else {
                        Column {
                            upcomingItems.forEachIndexed { idx, item ->
                                val isShared = item.visibility == Visibility.SHARED
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onInspectItem(item.raw) }
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isShared) palette.lavenderSoft else palette.peachSoft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = if (isShared) palette.lavenderInk else palette.peachInk,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                                color = palette.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.kindLabel} · ${if (isShared) "Shared Space" else "My Space"}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                                color = palette.textSecondary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Formatters.formatShortDate(item.date),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                                if (idx < upcomingItems.lastIndex) {
                                    HorizontalDivider(color = palette.divider)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}
