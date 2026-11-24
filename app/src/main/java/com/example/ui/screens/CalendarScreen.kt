package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.LocalDuoDeskPalette
import com.example.util.Formatters
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private data class CalEvent(
    val id: String,
    val date: String,
    val time: String?,
    val title: String,
    val visibility: Visibility,
    val kindLabel: String,
    val raw: InspectableItem
)

@Composable
fun CalendarScreen(
    tasks: List<TaskItem>,
    reminders: List<ReminderItem>,
    assignments: List<AssignmentItem>,
    onInspectItem: (InspectableItem) -> Unit,
    onCreateOnDate: (String) -> Unit
) {
    val palette = LocalDuoDeskPalette.current
    val todayIso = remember { Formatters.getTodayIso() }
    var viewMode by remember { mutableStateOf(CalendarViewMode.WEEK) }
    var cursorIso by remember { mutableStateOf(todayIso) }

    val events = remember(tasks, reminders, assignments) {
        buildList {
            tasks.forEach { t ->
                add(
                    CalEvent(
                        id = "t-${t.id}",
                        date = t.dueDate,
                        time = t.dueTime,
                        title = t.title,
                        visibility = t.visibility,
                        kindLabel = if (t.visibility == Visibility.SHARED) "Shared task" else "Personal task",
                        raw = InspectableItem.Task(t)
                    )
                )
            }
            reminders.forEach { r ->
                add(
                    CalEvent(
                        id = "r-${r.id}",
                        date = r.date,
                        time = r.time,
                        title = r.title,
                        visibility = r.visibility,
                        kindLabel = if (r.visibility == Visibility.SHARED) "Shared reminder" else "Reminder",
                        raw = InspectableItem.Reminder(r)
                    )
                )
            }
            assignments.forEach { a ->
                add(
                    CalEvent(
                        id = "a-${a.id}",
                        date = a.deadline,
                        time = a.deadlineTime,
                        title = a.title,
                        visibility = a.visibility,
                        kindLabel = "Assignment · ${a.subject}",
                        raw = InspectableItem.Assignment(a)
                    )
                )
            }
        }
    }

    val cursorDate = remember(cursorIso) {
        runCatching { LocalDate.parse(cursorIso) }.getOrElse { LocalDate.now() }
    }

    fun stepPeriod(dir: Long) {
        cursorIso = when (viewMode) {
            CalendarViewMode.MONTH -> cursorDate.plusMonths(dir).withDayOfMonth(1).toString()
            CalendarViewMode.WEEK -> cursorDate.plusDays(dir * 7).toString()
            CalendarViewMode.DAY -> cursorDate.plusDays(dir).toString()
        }
    }

    val weekDays = remember(cursorDate, events) {
        val dow = cursorDate.dayOfWeek.value % 7 // Sunday = 0
        val start = cursorDate.minusDays(dow.toLong())
        (0L..6L).map { offset ->
            val dt = start.plusDays(offset)
            val iso = dt.toString()
            Triple(
                iso,
                dt.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US) to dt.dayOfMonth,
                events.filter { it.date == iso }
            )
        }
    }

    val monthCells = remember(cursorDate, events) {
        val first = cursorDate.withDayOfMonth(1)
        val offset = first.dayOfWeek.value % 7
        val gridStart = first.minusDays(offset.toLong())
        (0L..34L).map { i ->
            val dt = gridStart.plusDays(i)
            val iso = dt.toString()
            Triple(
                iso,
                dt.dayOfMonth to (dt.monthValue == cursorDate.monthValue),
                events.filter { it.date == iso }
            )
        }
    }

    val selectedDayEvents = remember(events, cursorIso) {
        events.filter { it.date == cursorIso }.sortedBy { it.time ?: "23:59" }
    }

    @Composable
    fun EventChipRow(ev: CalEvent) {
        val bg = if (ev.visibility == Visibility.SHARED) palette.lavenderSoft else palette.pinkSoft
        val fg = if (ev.visibility == Visibility.SHARED) palette.lavenderInk else palette.pinkInk
        Surface(
            onClick = { onInspectItem(ev.raw) },
            shape = RoundedCornerShape(18.dp),
            color = bg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ev.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ev.kindLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = fg.copy(alpha = 0.82f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (!ev.time.isNullOrBlank()) Formatters.formatTime12h(ev.time) else "All day",
                    style = MaterialTheme.typography.labelSmall,
                    color = fg
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 960.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month/Year Heading & Navigation
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Formatters.formatMonthYear(cursorIso),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { stepPeriod(-1) },
                            shape = CircleShape,
                            color = palette.card,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = palette.textSecondary)
                            }
                        }
                        OutlinedButton(
                            onClick = { cursorIso = todayIso },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, palette.border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = palette.card,
                                contentColor = palette.textPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Today", style = MaterialTheme.typography.labelMedium)
                        }
                        Surface(
                            onClick = { stepPeriod(1) },
                            shape = CircleShape,
                            color = palette.card,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = palette.textSecondary)
                            }
                        }
                    }
                }

                // Week | Month | Day segmented pills
                Surface(
                    shape = CircleShape,
                    color = palette.card,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CalendarViewMode.entries.forEach { mode ->
                            val selected = viewMode == mode
                            Surface(
                                onClick = { viewMode = mode },
                                shape = CircleShape,
                                color = if (selected) palette.primaryButtonBg else Color.Transparent,
                                modifier = Modifier.testTag("calendar_mode_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) palette.primaryButtonText else palette.textSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(palette.pink))
                        Text("Personal deadlines", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = palette.textSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(palette.lavender))
                        Text("Shared deadlines", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = palette.textSecondary)
                    }
                }
            }
        }

        when (viewMode) {
            CalendarViewMode.WEEK -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.card,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            weekDays.forEach { (iso, dayInfo, dayEvents) ->
                                val (shortDay, dayNum) = dayInfo
                                val isSelected = iso == cursorIso
                                val isToday = iso == todayIso
                                val bg = when {
                                    isSelected -> palette.primaryButtonBg
                                    isToday -> palette.pinkSoft
                                    else -> Color.Transparent
                                }
                                val fg = when {
                                    isSelected -> palette.primaryButtonText
                                    isToday -> palette.pinkInk
                                    else -> palette.textSecondary
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(bg)
                                        .clickable { cursorIso = iso }
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(shortDay, fontSize = 11.sp, color = fg)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dayNum.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) palette.primaryButtonText else palette.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
                                        dayEvents.take(3).forEach { ev ->
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (ev.visibility == Visibility.SHARED) palette.lavender else palette.pink)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = Formatters.formatFullDate(cursorIso),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = "${selectedDayEvents.size} scheduled item(s)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = palette.textSecondary
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onCreateOnDate(cursorIso) },
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, palette.border)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            if (selectedDayEvents.isEmpty()) {
                                Text(
                                    text = "No deadlines or reminders on this day.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
                                )
                            } else {
                                selectedDayEvents.forEach { ev ->
                                    EventChipRow(ev)
                                }
                            }
                        }
                    }
                }
            }

            CalendarViewMode.MONTH -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.card,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { d ->
                                    Text(
                                        text = d,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 8.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = palette.divider)

                            monthCells.chunked(7).forEach { weekRow ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    weekRow.forEach { (iso, cellInfo, dayEvents) ->
                                        val (dayNum, inMonth) = cellInfo
                                        val isToday = iso == todayIso
                                        val isSelected = iso == cursorIso
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 76.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    when {
                                                        isSelected -> palette.lavenderSoft.copy(alpha = 0.45f)
                                                        !inMonth -> palette.subtleSurface.copy(alpha = 0.6f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable {
                                                    cursorIso = iso
                                                    if (dayEvents.isNotEmpty()) viewMode = CalendarViewMode.DAY
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isToday) palette.primaryButtonBg else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                                    color = when {
                                                        isToday -> palette.primaryButtonText
                                                        inMonth -> palette.textPrimary
                                                        else -> palette.textTertiary
                                                    }
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            dayEvents.take(2).forEach { ev ->
                                                val bg = if (ev.visibility == Visibility.SHARED) palette.lavenderSoft else palette.pinkSoft
                                                val fg = if (ev.visibility == Visibility.SHARED) palette.lavenderInk else palette.pinkInk
                                                Text(
                                                    text = ev.title,
                                                    fontSize = 9.sp,
                                                    color = fg,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 2.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(bg)
                                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                            if (dayEvents.size > 2) {
                                                Text(
                                                    text = "+${dayEvents.size - 2}",
                                                    fontSize = 9.sp,
                                                    color = palette.textSecondary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = palette.divider)
                            }
                        }
                    }
                }
            }

            CalendarViewMode.DAY -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.card,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = Formatters.formatFullDate(cursorIso),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = "${selectedDayEvents.size} item(s) scheduled",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary
                                    )
                                }
                                Button(
                                    onClick = { onCreateOnDate(cursorIso) },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primaryButtonBg,
                                        contentColor = palette.primaryButtonText
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add item", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            if (selectedDayEvents.isEmpty()) {
                                Text(
                                    text = "Nothing scheduled for this day.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp)
                                )
                            } else {
                                selectedDayEvents.forEach { ev ->
                                    EventChipRow(ev)
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
