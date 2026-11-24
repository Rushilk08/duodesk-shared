package com.example.data

import com.example.model.*
import com.example.util.Formatters

object SeedData {
    const val AVATAR_RUSHIL = "drawable:img_avatar_rushil"
    const val AVATAR_ALEX = "drawable:img_avatar_alex"
    const val PREVIEW_PRESENTATION = "drawable:img_doc_presentation_preview"
    const val PREVIEW_WIREFRAME = "drawable:img_doc_architecture_wireframe"

    fun users(): List<UserProfile> = listOf(
        UserProfile(
            id = "rushil",
            name = "Rushil",
            fullName = "Rushil Kapoor",
            email = "rushil@duodesk.app",
            role = "Product & Engineering",
            avatarKey = AVATAR_RUSHIL,
            accentHex = 0xFF6C8FF5
        ),
        UserProfile(
            id = "alex",
            name = "Alex",
            fullName = "Alex Rivera",
            email = "alex@duodesk.app",
            role = "Systems & Design",
            avatarKey = AVATAR_ALEX,
            accentHex = 0xFF8B7CF6
        )
    )

    fun notificationSettings(): List<NotificationSettings> = listOf(
        NotificationSettings(
            userId = "rushil",
            deadlineAlerts = true,
            sharedUpdates = true,
            fileUploads = true,
            dailyDigest = false
        ),
        NotificationSettings(
            userId = "alex",
            deadlineAlerts = true,
            sharedUpdates = true,
            fileUploads = true,
            dailyDigest = true
        )
    )

    fun tasks(): List<TaskItem> {
        val today = Formatters.getTodayIso()
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000L
        val threeHoursAgo = now - 3 * 3600_000L
        val yesterday = now - 26 * 3600_000L

        return listOf(
            TaskItem(
                id = "task-1",
                title = "Complete Python project",
                description = "Finish FastAPI endpoints, data validation schemas, and unit test coverage for module 4.",
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                priority = Priority.HIGH,
                dueDate = today,
                dueTime = "17:00",
                completed = false,
                category = "Engineering",
                attachments = emptyList(),
                createdAt = yesterday,
                updatedAt = oneHourAgo
            ),
            TaskItem(
                id = "task-2",
                title = "Revise database management",
                description = "Review B-tree indexing, normalization up to BCNF, and transaction isolation levels.",
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                priority = Priority.MEDIUM,
                dueDate = Formatters.addDaysIso(today, 1),
                dueTime = "19:30",
                completed = false,
                category = "Study",
                attachments = emptyList(),
                createdAt = yesterday,
                updatedAt = yesterday
            ),
            TaskItem(
                id = "task-3",
                title = "Prepare presentation",
                description = "Refine speaker notes and verify slide pacing for tomorrow morning walkthrough.",
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                priority = Priority.MEDIUM,
                dueDate = Formatters.addDaysIso(today, 2),
                dueTime = "10:00",
                completed = true,
                category = "Presentation",
                attachments = emptyList(),
                createdAt = yesterday,
                updatedAt = threeHoursAgo
            ),
            TaskItem(
                id = "task-4",
                title = "Submit DBMS assignment",
                description = "Finalize ER diagram, relational schema SQL scripts, and export combined PDF report.",
                ownerId = "rushil",
                visibility = Visibility.SHARED,
                priority = Priority.HIGH,
                dueDate = today,
                dueTime = "23:59",
                completed = false,
                category = "Academic",
                attachments = listOf(
                    AttachmentRef(
                        id = "att-dbms",
                        name = "DBMS_Assignment.pdf",
                        sizeBytes = 2_480_000L,
                        kind = "pdf",
                        mimeType = "application/pdf"
                    )
                ),
                createdAt = yesterday,
                updatedAt = oneHourAgo
            ),
            TaskItem(
                id = "task-5",
                title = "Complete group project",
                description = "Integrate frontend architecture wireframes with the shared schema specification.",
                ownerId = "alex",
                visibility = Visibility.SHARED,
                priority = Priority.HIGH,
                dueDate = Formatters.addDaysIso(today, 2),
                dueTime = "18:00",
                completed = false,
                category = "Collaboration",
                attachments = listOf(
                    AttachmentRef(
                        id = "att-report",
                        name = "Project_Report.docx",
                        sizeBytes = 1_120_000L,
                        kind = "doc",
                        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    )
                ),
                createdAt = yesterday,
                updatedAt = threeHoursAgo
            ),
            TaskItem(
                id = "task-6",
                title = "Prepare for presentation",
                description = "Review shared demo script, check visual slides, and run 10-minute dry rehearsal together.",
                ownerId = "alex",
                visibility = Visibility.SHARED,
                priority = Priority.MEDIUM,
                dueDate = Formatters.addDaysIso(today, 3),
                dueTime = "14:00",
                completed = false,
                category = "Presentation",
                attachments = listOf(
                    AttachmentRef(
                        id = "att-pres",
                        name = "Presentation.png",
                        sizeBytes = 1_850_000L,
                        kind = "image",
                        mimeType = "image/png",
                        previewUri = PREVIEW_PRESENTATION
                    )
                ),
                createdAt = yesterday,
                updatedAt = oneHourAgo
            ),
            TaskItem(
                id = "task-7",
                title = "Audit API response contracts",
                description = "Cross-check Supabase storage bucket policies before shared demo.",
                ownerId = "alex",
                visibility = Visibility.PRIVATE,
                priority = Priority.MEDIUM,
                dueDate = Formatters.addDaysIso(today, 1),
                dueTime = "15:00",
                completed = false,
                category = "Backend",
                attachments = emptyList(),
                createdAt = yesterday,
                updatedAt = yesterday
            ),
            TaskItem(
                id = "task-8",
                title = "Draft sprint retrospective summary",
                description = "Capture key blockers resolved and next week milestones.",
                ownerId = "rushil",
                visibility = Visibility.SHARED,
                priority = Priority.LOW,
                dueDate = Formatters.addDaysIso(today, -1),
                dueTime = "16:00",
                completed = true,
                category = "Planning",
                attachments = emptyList(),
                createdAt = yesterday,
                updatedAt = yesterday
            )
        )
    }

    fun reminders(): List<ReminderItem> {
        val today = Formatters.getTodayIso()
        val yesterday = System.currentTimeMillis() - 26 * 3600_000L
        return listOf(
            ReminderItem(
                id = "rem-1",
                title = "Sync with Alex on DBMS schema constraints",
                description = "Verify foreign key cascades and composite index choices before final export.",
                ownerId = "rushil",
                visibility = Visibility.SHARED,
                date = today,
                time = "14:30",
                priority = Priority.HIGH,
                repeat = RepeatOption.NONE,
                notificationPref = NotificationPreference.PUSH_IN_APP,
                completed = false,
                createdAt = yesterday
            ),
            ReminderItem(
                id = "rem-2",
                title = "Run Python test suite & type check",
                description = "Ensure zero mypy warnings on core service modules.",
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                date = today,
                time = "16:15",
                priority = Priority.MEDIUM,
                repeat = RepeatOption.DAILY,
                notificationPref = NotificationPreference.IN_APP,
                completed = false,
                createdAt = yesterday
            ),
            ReminderItem(
                id = "rem-3",
                title = "Upload final slide deck export",
                description = "Save high-resolution PNG and PDF copies into Shared Space.",
                ownerId = "alex",
                visibility = Visibility.SHARED,
                date = Formatters.addDaysIso(today, 1),
                time = "11:00",
                priority = Priority.MEDIUM,
                repeat = RepeatOption.NONE,
                notificationPref = NotificationPreference.PUSH_IN_APP,
                completed = false,
                createdAt = yesterday
            ),
            ReminderItem(
                id = "rem-4",
                title = "Renew design research library bookmark",
                description = "Archive older SaaS reference boards for Q4 sprint.",
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                date = Formatters.addDaysIso(today, -1),
                time = "09:00",
                priority = Priority.LOW,
                repeat = RepeatOption.WEEKLY,
                notificationPref = NotificationPreference.EMAIL_DIGEST,
                completed = false,
                createdAt = yesterday
            ),
            ReminderItem(
                id = "rem-5",
                title = "Morning reading: Systems Architecture chapter 6",
                description = "Summarize consensus notes in My Space.",
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                date = Formatters.addDaysIso(today, -1),
                time = "08:30",
                priority = Priority.LOW,
                repeat = RepeatOption.NONE,
                notificationPref = NotificationPreference.IN_APP,
                completed = true,
                createdAt = yesterday
            ),
            ReminderItem(
                id = "rem-6",
                title = "Alex personal dentist appointment",
                description = "Downtown clinic, bring insurance card.",
                ownerId = "alex",
                visibility = Visibility.PRIVATE,
                date = Formatters.addDaysIso(today, 2),
                time = "09:30",
                priority = Priority.MEDIUM,
                repeat = RepeatOption.NONE,
                notificationPref = NotificationPreference.PUSH_IN_APP,
                completed = false,
                createdAt = yesterday
            )
        )
    }

    fun assignments(): List<AssignmentItem> {
        val today = Formatters.getTodayIso()
        val yesterday = System.currentTimeMillis() - 26 * 3600_000L
        return listOf(
            AssignmentItem(
                id = "asg-1",
                title = "Submit DBMS assignment",
                subject = "Database Management Systems",
                description = "Normalized relational schema, SQL query optimization benchmarks, and indexing rationale.",
                ownerId = "rushil",
                visibility = Visibility.SHARED,
                deadline = today,
                deadlineTime = "23:59",
                priority = Priority.HIGH,
                status = AssignmentStatus.IN_PROGRESS,
                attachments = listOf(
                    AttachmentRef(
                        id = "att-asg-1",
                        name = "DBMS_Assignment.pdf",
                        sizeBytes = 2_480_000L,
                        kind = "pdf",
                        mimeType = "application/pdf"
                    )
                ),
                createdAt = yesterday
            ),
            AssignmentItem(
                id = "asg-2",
                title = "Complete group project deliverable",
                subject = "Software Engineering Studio",
                description = "End-to-end architecture report covering frontend state, API contracts, and UI wireframes.",
                ownerId = "alex",
                visibility = Visibility.SHARED,
                deadline = Formatters.addDaysIso(today, 3),
                deadlineTime = "18:00",
                priority = Priority.HIGH,
                status = AssignmentStatus.IN_PROGRESS,
                attachments = listOf(
                    AttachmentRef(
                        id = "att-asg-2a",
                        name = "Project_Report.docx",
                        sizeBytes = 1_120_000L,
                        kind = "doc",
                        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    ),
                    AttachmentRef(
                        id = "att-asg-2b",
                        name = "Architecture_Wireframe.png",
                        sizeBytes = 1_640_000L,
                        kind = "image",
                        mimeType = "image/png",
                        previewUri = PREVIEW_WIREFRAME
                    )
                ),
                createdAt = yesterday
            ),
            AssignmentItem(
                id = "asg-3",
                title = "Python Async Pipeline Lab",
                subject = "Applied Python Systems",
                description = "Implement concurrent task runner with structured logging and graceful cancellation.",
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                deadline = Formatters.addDaysIso(today, 1),
                deadlineTime = "20:00",
                priority = Priority.HIGH,
                status = AssignmentStatus.PENDING,
                attachments = emptyList(),
                createdAt = yesterday
            ),
            AssignmentItem(
                id = "asg-4",
                title = "Presentation Visual Deck Review",
                subject = "Product Design Seminar",
                description = "Upload final slide sequence and visual hierarchy audit.",
                ownerId = "alex",
                visibility = Visibility.SHARED,
                deadline = Formatters.addDaysIso(today, 4),
                deadlineTime = "14:00",
                priority = Priority.MEDIUM,
                status = AssignmentStatus.PENDING,
                attachments = listOf(
                    AttachmentRef(
                        id = "att-asg-4",
                        name = "Presentation.png",
                        sizeBytes = 1_850_000L,
                        kind = "image",
                        mimeType = "image/png",
                        previewUri = PREVIEW_PRESENTATION
                    )
                ),
                createdAt = yesterday
            ),
            AssignmentItem(
                id = "asg-5",
                title = "Query Execution Plan Summary",
                subject = "Database Management Systems",
                description = "Annotated EXPLAIN ANALYZE breakdown for join strategies.",
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                deadline = Formatters.addDaysIso(today, -2),
                deadlineTime = "17:00",
                priority = Priority.MEDIUM,
                status = AssignmentStatus.COMPLETED,
                attachments = emptyList(),
                createdAt = yesterday
            ),
            AssignmentItem(
                id = "asg-6",
                title = "Literature Review Draft",
                subject = "Research Methods",
                description = "Submit annotated bibliography for peer check.",
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                deadline = Formatters.addDaysIso(today, -1),
                deadlineTime = "12:00",
                priority = Priority.LOW,
                status = AssignmentStatus.PENDING,
                attachments = emptyList(),
                createdAt = yesterday
            )
        )
    }

    fun documents(): List<DocumentItem> {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000L
        val threeHoursAgo = now - 3 * 3600_000L
        val yesterday = now - 26 * 3600_000L
        return listOf(
            DocumentItem(
                id = "doc-1",
                name = "DBMS_Assignment.pdf",
                kind = DocumentKind.PDF,
                extension = "PDF",
                mimeType = "application/pdf",
                sizeBytes = 2_480_000L,
                ownerId = "rushil",
                visibility = Visibility.SHARED,
                uploadedAt = oneHourAgo,
                summary = "Final ER diagram, relational schema definitions, and BCNF normalization proof."
            ),
            DocumentItem(
                id = "doc-2",
                name = "Project_Report.docx",
                kind = DocumentKind.DOC,
                extension = "DOCX",
                mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                sizeBytes = 1_120_000L,
                ownerId = "alex",
                visibility = Visibility.SHARED,
                uploadedAt = threeHoursAgo,
                summary = "Collaborative milestone report with system architecture sections and timeline."
            ),
            DocumentItem(
                id = "doc-3",
                name = "Presentation.png",
                kind = DocumentKind.IMAGE,
                extension = "PNG",
                mimeType = "image/png",
                sizeBytes = 1_850_000L,
                ownerId = "alex",
                visibility = Visibility.SHARED,
                uploadedAt = threeHoursAgo,
                previewUri = PREVIEW_PRESENTATION,
                summary = "Slide deck layout preview and visual hierarchy sign-off sheet."
            ),
            DocumentItem(
                id = "doc-4",
                name = "Architecture_Wireframe.png",
                kind = DocumentKind.IMAGE,
                extension = "PNG",
                mimeType = "image/png",
                sizeBytes = 1_640_000L,
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                uploadedAt = yesterday,
                previewUri = PREVIEW_WIREFRAME,
                summary = "Personal workspace interface mapping and component boundary sketches."
            ),
            DocumentItem(
                id = "doc-5",
                name = "Python_Concurrency_Notes.pdf",
                kind = DocumentKind.PDF,
                extension = "PDF",
                mimeType = "application/pdf",
                sizeBytes = 890_000L,
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                uploadedAt = yesterday,
                summary = "Personal revision cheat-sheet covering asyncio task groups and queues."
            ),
            DocumentItem(
                id = "doc-6",
                name = "Alex_Research_Outline.docx",
                kind = DocumentKind.DOC,
                extension = "DOCX",
                mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                sizeBytes = 640_000L,
                ownerId = "alex",
                visibility = Visibility.PRIVATE,
                uploadedAt = yesterday,
                summary = "Private research draft belonging strictly to Alex."
            )
        )
    }

    fun notes(): List<NoteItem> {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000L
        val threeHoursAgo = now - 3 * 3600_000L
        val yesterday = now - 26 * 3600_000L
        return listOf(
            NoteItem(
                id = "note-1",
                title = "DBMS Submission Checklist",
                content = "Final verification steps before we submit the joint DBMS assignment tonight.",
                type = NoteType.CHECKLIST,
                checklist = listOf(
                    ChecklistRow("c-1", "Verify all foreign key constraints in schema.sql", true),
                    ChecklistRow("c-2", "Include query execution time comparisons", true),
                    ChecklistRow("c-3", "Attach updated ER diagram in DBMS_Assignment.pdf", false),
                    ChecklistRow("c-4", "Confirm Alex reviewed section 4 conclusions", false)
                ),
                ownerId = "rushil",
                visibility = Visibility.SHARED,
                createdAt = yesterday,
                updatedAt = oneHourAgo
            ),
            NoteItem(
                id = "note-2",
                title = "Python Project Architecture Notes",
                content = "Keep FastAPI routers decoupled from repository interfaces.\n\n1. Use Pydantic v2 strict models for request validation.\n2. Centralize Supabase storage signed-URL helper.\n3. Add integration tests for private vs. shared visibility guards so User A never sees User B private rows.",
                type = NoteType.NOTE,
                checklist = emptyList(),
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                createdAt = yesterday,
                updatedAt = threeHoursAgo
            ),
            NoteItem(
                id = "note-3",
                title = "Presentation Rehearsal Talking Points",
                content = "Key flow for our 10-minute product walkthrough with Alex.",
                type = NoteType.CHECKLIST,
                checklist = listOf(
                    ChecklistRow("c-5", "Rushil introduces problem & dual-workspace model", true),
                    ChecklistRow("c-6", "Alex walks through Shared Space real-time handoffs", false),
                    ChecklistRow("c-7", "Show calendar deadline color differentiation", false)
                ),
                ownerId = "alex",
                visibility = Visibility.SHARED,
                createdAt = yesterday,
                updatedAt = threeHoursAgo
            ),
            NoteItem(
                id = "note-4",
                title = "Exam Revision Focus Topics",
                content = "Topics to review tonight:\n- ACID properties & serializability conflict graphs\n- Two-phase locking deadlocks\n- B+ Tree split/merge edge cases",
                type = NoteType.NOTE,
                checklist = emptyList(),
                ownerId = "rushil",
                visibility = Visibility.PRIVATE,
                createdAt = yesterday,
                updatedAt = yesterday
            ),
            NoteItem(
                id = "note-5",
                title = "Alex Private Reading Queue",
                content = "Personal weekend reading list — private to Alex.",
                type = NoteType.CHECKLIST,
                checklist = listOf(
                    ChecklistRow("c-8", "Designing Data-Intensive Applications Ch. 7", false),
                    ChecklistRow("c-9", "Review typography spacing notes", true)
                ),
                ownerId = "alex",
                visibility = Visibility.PRIVATE,
                createdAt = yesterday,
                updatedAt = yesterday
            )
        )
    }

    fun notifications(): List<NotificationItem> {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000L
        val threeHoursAgo = now - 3 * 3600_000L
        val yesterday = now - 26 * 3600_000L
        return listOf(
            NotificationItem(
                id = "notif-1",
                title = "Shared deadline today",
                message = "Submit DBMS assignment is due tonight at 11:59 PM.",
                category = NotificationCategory.DEADLINE,
                targetRoute = "/shared-space",
                actorId = "rushil",
                visibility = Visibility.SHARED,
                read = false,
                createdAt = oneHourAgo
            ),
            NotificationItem(
                id = "notif-2",
                title = "Alex uploaded Presentation.png",
                message = "Added to Shared Space · 1.8 MB PNG image.",
                category = NotificationCategory.FILE,
                targetRoute = "/shared-space",
                actorId = "alex",
                visibility = Visibility.SHARED,
                read = false,
                createdAt = threeHoursAgo
            ),
            NotificationItem(
                id = "notif-3",
                title = "Alex updated shared task",
                message = "Complete group project now includes Project_Report.docx.",
                category = NotificationCategory.SHARED_TASK,
                targetRoute = "/shared-space",
                actorId = "alex",
                visibility = Visibility.SHARED,
                read = false,
                createdAt = threeHoursAgo
            ),
            NotificationItem(
                id = "notif-4",
                title = "Upcoming personal reminder",
                message = "Run Python test suite & type check scheduled for 4:15 PM today.",
                category = NotificationCategory.REMINDER,
                targetRoute = "/my-space",
                actorId = "rushil",
                visibility = Visibility.PRIVATE,
                ownerId = "rushil",
                read = true,
                createdAt = yesterday
            ),
            NotificationItem(
                id = "notif-5",
                title = "Task marked complete",
                message = "Draft sprint retrospective summary was completed.",
                category = NotificationCategory.COMPLETED,
                targetRoute = "/shared-space",
                actorId = "rushil",
                visibility = Visibility.SHARED,
                read = true,
                createdAt = yesterday
            )
        )
    }
}
