package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.LocalDuoDeskPalette
import com.example.ui.viewmodel.CreateDefaults
import com.example.util.FileHelpers
import com.example.util.Formatters
import com.example.util.PickedFileMeta
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

private val CATEGORY_PRESETS = listOf("Study", "Engineering", "Academic", "Personal", "Design")

@Composable
fun CreateScreen(
    defaults: CreateDefaults,
    onBack: () -> Unit,
    onCreateTask: (String, String, Visibility, Priority, String, String?, String, List<AttachmentRef>) -> Unit,
    onCreateReminder: (String, String, Visibility, String, String, Priority, RepeatOption, NotificationPreference) -> Unit,
    onCreateAssignment: (String, String, String, Visibility, String, String?, Priority, List<AttachmentRef>) -> Unit,
    onCreateNote: (String, String, NoteType, List<ChecklistRow>, Visibility) -> Unit,
    onUploadDocument: (String, DocumentKind, String, String, Long, Visibility, String?, String) -> Unit,
    onDone: (Visibility) -> Unit
) {
    val palette = LocalDuoDeskPalette.current
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    var kind by remember(defaults) { mutableStateOf(defaults.kind) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember(defaults) { mutableStateOf(defaults.date ?: Formatters.getTodayIso()) }
    var dueTime by remember { mutableStateOf("17:00") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var visibility by remember(defaults) { mutableStateOf(defaults.visibility) }
    var category by remember { mutableStateOf("Study") }
    var repeat by remember { mutableStateOf(RepeatOption.NONE) }
    var notifPref by remember { mutableStateOf(NotificationPreference.PUSH_IN_APP) }
    var noteType by remember { mutableStateOf(NoteType.NOTE) }
    var checklistText by remember { mutableStateOf("First item\nSecond item") }
    var attachments by remember { mutableStateOf<List<AttachmentRef>>(emptyList()) }
    var pickedDocMeta by remember { mutableStateOf<PickedFileMeta?>(null) }

    val multiAttachLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val newRefs = uris.map { FileHelpers.toAttachmentRef(FileHelpers.readPickedFileMeta(context, it)) }
        attachments = attachments + newRefs
    }

    val singleDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val meta = FileHelpers.readPickedFileMeta(context, uri)
            pickedDocMeta = meta
            if (title.isBlank()) title = meta.name
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back & Kind Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = CircleShape,
                border = BorderStroke(1.dp, palette.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = palette.card,
                    contentColor = palette.textPrimary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("create_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back", style = MaterialTheme.typography.labelMedium)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CreateKind.entries.forEach { k ->
                    val active = kind == k
                    Surface(
                        onClick = { kind = k },
                        shape = CircleShape,
                        color = if (active) palette.primaryButtonBg else palette.card,
                        border = BorderStroke(1.dp, if (active) palette.primaryButtonBg else palette.border),
                        modifier = Modifier.testTag("create_kind_${k.name.lowercase()}")
                    ) {
                        Text(
                            text = k.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) palette.primaryButtonText else palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }

        // Main Form Card
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = when (kind) {
                            CreateKind.TASK -> "Create Task"
                            CreateKind.REMINDER -> "Create Reminder"
                            CreateKind.ASSIGNMENT -> "Create Assignment"
                            CreateKind.NOTE -> "Create Note"
                            CreateKind.DOCUMENT -> "Upload Document"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose My Space to keep it private or Shared Space to collaborate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }

                // Workspace Toggle
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Workspace", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { visibility = Visibility.PRIVATE },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (visibility == Visibility.PRIVATE) palette.pink else palette.border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (visibility == Visibility.PRIVATE) palette.pinkSoft else palette.subtleSurface,
                                contentColor = if (visibility == Visibility.PRIVATE) palette.pinkInk else palette.textSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("create_workspace_private")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("My Space", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { visibility = Visibility.SHARED },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (visibility == Visibility.SHARED) palette.lavender else palette.border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (visibility == Visibility.SHARED) palette.lavenderSoft else palette.subtleSurface,
                                contentColor = if (visibility == Visibility.SHARED) palette.lavenderInk else palette.textSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("create_workspace_shared")
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shared Space", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (kind == CreateKind.DOCUMENT) {
                    Surface(
                        onClick = {
                            singleDocLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "image/*"
                                )
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = palette.subtleSurface,
                        border = BorderStroke(1.5.dp, palette.lavender),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = palette.lavender, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pickedDocMeta?.name ?: "Choose PDF, Word doc, or image",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                color = palette.textPrimary
                            )
                            Text(
                                text = pickedDocMeta?.let { Formatters.formatFileSize(it.sizeBytes) } ?: "Tap to browse device files",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = palette.textSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("File title") },
                        placeholder = { Text("e.g., DBMS_Assignment.pdf") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Short note (optional)") },
                        minLines = 2,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (kind == CreateKind.TASK) "Task title" else "Title") },
                        placeholder = {
                            Text(
                                when (kind) {
                                    CreateKind.TASK -> "e.g., Complete Python project"
                                    CreateKind.REMINDER -> "e.g., Sync with Alex at 3 PM"
                                    CreateKind.ASSIGNMENT -> "e.g., Submit DBMS assignment"
                                    else -> "e.g., Weekly reading notes"
                                }
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_title_input")
                    )

                    if (kind == CreateKind.NOTE) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = noteType == NoteType.NOTE,
                                onClick = { noteType = NoteType.NOTE },
                                label = { Text("Note") },
                                shape = CircleShape
                            )
                            FilterChip(
                                selected = noteType == NoteType.CHECKLIST,
                                onClick = { noteType = NoteType.CHECKLIST },
                                label = { Text("Checklist") },
                                shape = CircleShape
                            )
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        placeholder = { Text("Add helpful context or details...") },
                        minLines = if (kind == CreateKind.NOTE && noteType == NoteType.NOTE) 4 else 2,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_description_input")
                    )

                    if (kind == CreateKind.NOTE && noteType == NoteType.CHECKLIST) {
                        OutlinedTextField(
                            value = checklistText,
                            onValueChange = { checklistText = it },
                            label = { Text("Checklist items (one per line)") },
                            minLines = 4,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (kind != CreateKind.NOTE) {
                        // Date & Time pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val parsed = runCatching { LocalDate.parse(dueDate) }.getOrElse { LocalDate.now() }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            dueDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                        },
                                        parsed.year,
                                        parsed.monthValue - 1,
                                        parsed.dayOfMonth
                                    ).show()
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Formatters.formatShortDate(dueDate), style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    val parts = dueTime.split(":")
                                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 17
                                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    TimePickerDialog(
                                        context,
                                        { _, selH, selM ->
                                            dueTime = String.format(Locale.US, "%02d:%02d", selH, selM)
                                        },
                                        h,
                                        m,
                                        false
                                    ).show()
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Formatters.formatTime12h(dueTime), style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Priority Pills
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Priority", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Priority.entries.forEach { p ->
                                    val active = priority == p
                                    val activeBg = when (p) {
                                        Priority.LOW -> palette.sageSoft
                                        Priority.MEDIUM -> palette.peachSoft
                                        Priority.HIGH -> palette.pinkSoft
                                    }
                                    val activeFg = when (p) {
                                        Priority.LOW -> palette.sageInk
                                        Priority.MEDIUM -> palette.peachInk
                                        Priority.HIGH -> palette.pinkInk
                                    }
                                    OutlinedButton(
                                        onClick = { priority = p },
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, if (active) activeFg else palette.border),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (active) activeBg else palette.subtleSurface,
                                            contentColor = if (active) activeFg else palette.textSecondary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(p.shortLabel(), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        if (kind == CreateKind.REMINDER) {
                            var repeatExpanded by remember { mutableStateOf(false) }
                            var alertExpanded by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { repeatExpanded = true },
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, palette.border),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(repeat.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                    DropdownMenu(expanded = repeatExpanded, onDismissRequest = { repeatExpanded = false }) {
                                        RepeatOption.entries.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt.label) },
                                                onClick = {
                                                    repeat = opt
                                                    repeatExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { alertExpanded = true },
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, palette.border),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(notifPref.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                    DropdownMenu(expanded = alertExpanded, onDismissRequest = { alertExpanded = false }) {
                                        NotificationPreference.entries.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt.label) },
                                                onClick = {
                                                    notifPref = opt
                                                    alertExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (kind == CreateKind.TASK || kind == CreateKind.ASSIGNMENT) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (kind == CreateKind.ASSIGNMENT) "Subject" else "Category (optional)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CATEGORY_PRESETS.forEach { preset ->
                                        val selected = category == preset
                                        FilterChip(
                                            selected = selected,
                                            onClick = { category = preset },
                                            label = { Text(preset) },
                                            shape = CircleShape
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = { category = it },
                                    placeholder = { Text("Or enter a custom category...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        multiAttachLauncher.launch(
                                            arrayOf(
                                                "application/pdf",
                                                "application/msword",
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                "image/*"
                                            )
                                        )
                                    },
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, palette.border)
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Attach PDF, Word doc, or image", style = MaterialTheme.typography.labelMedium)
                                }

                                attachments.forEach { att ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = palette.subtleSurface
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.Description, contentDescription = null, tint = palette.lavender, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(att.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            TextButton(onClick = { attachments = attachments.filterNot { it.id == att.id } }) {
                                                Text("Remove", color = palette.error, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val trimmedTitle = title.trim()
                        if (kind != CreateKind.DOCUMENT && trimmedTitle.isEmpty()) return@Button

                        when (kind) {
                            CreateKind.TASK -> onCreateTask(
                                trimmedTitle,
                                description.trim(),
                                visibility,
                                priority,
                                dueDate,
                                dueTime.ifBlank { null },
                                category.trim().ifBlank { "General" },
                                attachments
                            )
                            CreateKind.REMINDER -> onCreateReminder(
                                trimmedTitle,
                                description.trim(),
                                visibility,
                                dueDate,
                                dueTime.ifBlank { "09:00" },
                                priority,
                                repeat,
                                notifPref
                            )
                            CreateKind.ASSIGNMENT -> onCreateAssignment(
                                trimmedTitle,
                                category.trim().ifBlank { "General" },
                                description.trim(),
                                visibility,
                                dueDate,
                                dueTime.ifBlank { "23:59" },
                                priority,
                                attachments
                            )
                            CreateKind.NOTE -> {
                                val rows = if (noteType == NoteType.CHECKLIST) {
                                    checklistText
                                        .lines()
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .mapIndexed { idx, txt ->
                                            ChecklistRow("chk-${UUID.randomUUID().toString().take(6)}-$idx", txt, false)
                                        }
                                } else {
                                    emptyList()
                                }
                                onCreateNote(trimmedTitle, description.trim(), noteType, rows, visibility)
                            }
                            CreateKind.DOCUMENT -> {
                                val meta = pickedDocMeta
                                val docName = trimmedTitle.ifBlank { meta?.name ?: "Shared_Document.pdf" }
                                val (docKind, ext) = if (meta != null) meta.kind to meta.extension else Formatters.detectDocumentKind(docName)
                                onUploadDocument(
                                    docName,
                                    docKind,
                                    ext,
                                    meta?.mimeType ?: "application/pdf",
                                    meta?.sizeBytes ?: 380_000L,
                                    visibility,
                                    if (docKind == DocumentKind.IMAGE) meta?.uriString else null,
                                    description.trim()
                                )
                            }
                        }
                        onDone(visibility)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryButtonBg,
                        contentColor = palette.primaryButtonText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("create_submit_button")
                ) {
                    Text(
                        text = when (kind) {
                            CreateKind.TASK -> "Save Task"
                            CreateKind.REMINDER -> "Save Reminder"
                            CreateKind.ASSIGNMENT -> "Save Assignment"
                            CreateKind.NOTE -> "Save Note"
                            CreateKind.DOCUMENT -> "Upload File"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
