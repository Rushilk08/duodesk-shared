package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.UserAvatar
import com.example.ui.theme.LocalDuoDeskPalette
import com.example.util.Formatters

@Composable
fun ProfileScreen(
    currentUser: UserProfile?,
    otherUser: UserProfile?,
    themeMode: ThemeMode,
    notifications: List<NotificationItem>,
    unreadCount: Int,
    notificationSettings: NotificationSettings,
    onSwitchUser: (String) -> Unit,
    onSaveProfile: (String, String, String) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onClearNotifications: () -> Unit,
    onUpdateNotificationSettings: (NotificationSettings) -> Unit,
    onResetDemoData: () -> Unit,
    onLogout: () -> Unit
) {
    val palette = LocalDuoDeskPalette.current

    var name by remember(currentUser?.id, currentUser?.name) { mutableStateOf(currentUser?.name.orEmpty()) }
    var email by remember(currentUser?.id, currentUser?.email) { mutableStateOf(currentUser?.email.orEmpty()) }
    var role by remember(currentUser?.id, currentUser?.role) { mutableStateOf(currentUser?.role.orEmpty()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Card
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
                                listOf(palette.card, palette.peachSoft.copy(alpha = 0.55f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(user = currentUser, size = 54.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.fullName ?: "Member",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "${currentUser?.email.orEmpty()} · ${if (currentUser?.id == "rushil") "User A" else "User B"}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = palette.textSecondary
                                )
                            }
                        }

                        if (otherUser != null) {
                            OutlinedButton(
                                onClick = { onSwitchUser(otherUser.id) },
                                shape = CircleShape,
                                border = BorderStroke(1.dp, palette.border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = palette.card,
                                    contentColor = palette.textPrimary
                                ),
                                modifier = Modifier.testTag("profile_switch_user_button")
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = palette.lavender, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Switch to ${otherUser.name}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Profile Details Form
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
                    Text("Profile details", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text("Role") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { onSaveProfile(name, email, role) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryButtonBg,
                                contentColor = palette.primaryButtonText
                            ),
                            modifier = Modifier.testTag("save_profile_button")
                        ) {
                            Text("Save profile", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Notifications Center
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.card,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = palette.pink, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notifications ($unreadCount unread)",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (unreadCount > 0) {
                                TextButton(onClick = onMarkAllNotificationsRead) {
                                    Text("Mark all read", color = palette.lavenderInk, fontSize = 12.sp)
                                }
                            }
                            if (notifications.isNotEmpty()) {
                                IconButton(onClick = onClearNotifications) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear all notifications", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (notifications.isEmpty()) {
                        Text(
                            text = "No notifications right now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp)
                        )
                    } else {
                        notifications.forEachIndexed { idx, n ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = n.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (n.read) FontWeight.Medium else FontWeight.Bold
                                        ),
                                        color = if (n.read) palette.textSecondary else palette.textPrimary
                                    )
                                    Text(
                                        text = n.message,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = Formatters.formatRelativeTime(n.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textTertiary
                                    )
                                }
                                if (!n.read) {
                                    IconButton(onClick = { onMarkNotificationRead(n.id) }) {
                                        Icon(Icons.Default.Check, contentDescription = "Mark as read", tint = palette.lavender, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            if (idx < notifications.lastIndex) {
                                HorizontalDivider(color = palette.divider)
                            }
                        }
                    }
                }
            }
        }

        // Appearance
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
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSetThemeMode(ThemeMode.LIGHT) },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (themeMode == ThemeMode.LIGHT) palette.pink else palette.border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (themeMode == ThemeMode.LIGHT) palette.pinkSoft else palette.card,
                                contentColor = if (themeMode == ThemeMode.LIGHT) palette.pinkInk else palette.textSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("theme_warm_cream")
                        ) {
                            Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Warm Cream", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { onSetThemeMode(ThemeMode.DARK) },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (themeMode == ThemeMode.DARK) palette.lavender else palette.border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (themeMode == ThemeMode.DARK) palette.lavenderSoft else palette.card,
                                contentColor = if (themeMode == ThemeMode.DARK) palette.textPrimary else palette.textSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("theme_soft_dark")
                        ) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Soft Dark", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Alert Preferences
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.card,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Alert Preferences", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))

                    listOf(
                        Triple("Deadline reminders", notificationSettings.deadlineAlerts) { checked: Boolean ->
                            onUpdateNotificationSettings(notificationSettings.copy(deadlineAlerts = checked))
                        },
                        Triple("Shared space updates", notificationSettings.sharedUpdates) { checked: Boolean ->
                            onUpdateNotificationSettings(notificationSettings.copy(sharedUpdates = checked))
                        },
                        Triple("File upload alerts", notificationSettings.fileUploads) { checked: Boolean ->
                            onUpdateNotificationSettings(notificationSettings.copy(fileUploads = checked))
                        }
                    ).forEach { (label, checked, onToggle) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { onToggle(!checked) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                            Checkbox(
                                checked = checked,
                                onCheckedChange = onToggle
                            )
                        }
                    }
                }
            }
        }

        // Reset & Sign Out
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
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onResetDemoData,
                        modifier = Modifier.testTag("reset_demo_data_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset sample data", color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onLogout,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.pinkSoft,
                            contentColor = palette.pinkInk
                        ),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign out", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
