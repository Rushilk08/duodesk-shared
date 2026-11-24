package com.example.util

import com.example.model.DocumentKind
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getTodayIso(): String = LocalDate.now().format(isoDateFormatter)

    fun addDaysIso(baseIso: String, days: Long): String {
        val date = runCatching { LocalDate.parse(baseIso, isoDateFormatter) }.getOrElse { LocalDate.now() }
        return date.plusDays(days).format(isoDateFormatter)
    }

    fun formatShortDate(isoDate: String): String {
        if (isoDate.isBlank()) return ""
        val date = runCatching { LocalDate.parse(isoDate, isoDateFormatter) }.getOrNull() ?: return isoDate
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
        }
    }

    fun formatFullDate(isoDate: String): String {
        if (isoDate.isBlank()) return ""
        val date = runCatching { LocalDate.parse(isoDate, isoDateFormatter) }.getOrNull() ?: return isoDate
        return date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US))
    }

    fun formatTodayHeader(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US))
    }

    fun formatMonthYear(isoDate: String): String {
        val date = runCatching { LocalDate.parse(isoDate, isoDateFormatter) }.getOrElse { LocalDate.now() }
        return date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
    }

    fun formatTime12h(time24: String?): String {
        if (time24.isNullOrBlank()) return ""
        val parts = time24.split(":")
        val hh = parts.getOrNull(0)?.toIntOrNull() ?: return time24
        val mm = parts.getOrNull(1)?.toIntOrNull() ?: return time24
        val suffix = if (hh >= 12) "PM" else "AM"
        val h12 = if (hh % 12 == 0) 12 else hh % 12
        return String.format(Locale.US, "%d:%02d %s", h12, mm, suffix)
    }

    fun formatRelativeTime(timestampMs: Long): String {
        val diffMs = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
        val mins = diffMs / 60_000L
        if (mins < 1) return "Just now"
        if (mins < 60) return "${mins}m ago"
        val hrs = mins / 60
        if (hrs < 24) return "${hrs}h ago"
        val days = hrs / 24
        if (days < 7) return "${days}d ago"
        val dt = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()).toLocalDate()
        return dt.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.1f MB", mb)
    }

    fun isOverdueDate(isoDate: String, completed: Boolean): Boolean {
        if (completed || isoDate.isBlank()) return false
        return isoDate < getTodayIso()
    }

    fun detectDocumentKind(fileName: String): Pair<DocumentKind, String> {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.US)
        return when (ext) {
            "pdf" -> DocumentKind.PDF to "PDF"
            "png", "jpg", "jpeg", "webp" -> DocumentKind.IMAGE to ext.uppercase(Locale.US)
            else -> DocumentKind.DOC to (if (ext.isNotEmpty()) ext.uppercase(Locale.US) else "DOC")
        }
    }

    fun getGreetingByHour(name: String): String {
        val hour = LocalTime.now().hour
        return when {
            hour < 12 -> "Good morning, $name"
            hour < 17 -> "Good afternoon, $name"
            else -> "Good evening, $name"
        }
    }
}
