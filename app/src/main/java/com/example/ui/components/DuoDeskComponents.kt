package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SeedData
import com.example.model.NotificationItem
import com.example.model.ToastMessage
import com.example.model.ToastTone
import com.example.model.UserProfile
import com.example.ui.theme.LocalDuoDeskPalette
import com.example.ui.theme.PriorityLowDot
import com.example.ui.theme.PriorityMedLight
import com.example.util.Formatters

@Composable
fun DuoDeskLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val palette = LocalDuoDeskPalette.current
    val bgColor = if (palette.isDark) palette.lavender else palette.lavenderSoft
    val fgColor = if (palette.isDark) palette.background else palette.lavenderInk

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.54f)) {
            val stroke = Stroke(width = 2.dp.toPx())
            val w = this.size.width
            val h = this.size.height
            val pillW = w * 0.38f
            val pillH = h * 0.72f
            val top = (h - pillH) / 2f
            val radius = CornerRadius(pillW / 2f, pillW / 2f)

            drawRoundRect(
                color = fgColor,
                topLeft = Offset(w * 0.08f, top),
                size = Size(pillW, pillH),
                cornerRadius = radius,
                style = stroke
            )
            drawRoundRect(
                color = fgColor.copy(alpha = 0.28f),
                topLeft = Offset(w * 0.54f, top),
                size = Size(pillW, pillH),
                cornerRadius = radius
            )
            drawRoundRect(
                color = fgColor,
                topLeft = Offset(w * 0.54f, top),
                size = Size(pillW, pillH),
                cornerRadius = radius,
                style = stroke
            )
        }
    }
}

@Composable
fun UserAvatar(
    user: UserProfile?,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val palette = LocalDuoDeskPalette.current
    val resId = when (user?.avatarKey) {
        SeedData.AVATAR_RUSHIL -> R.drawable.img_avatar_rushil
        SeedData.AVATAR_ALEX -> R.drawable.img_avatar_alex
        else -> null
    }
    if (resId != null) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = "${user?.name ?: "Member"} avatar",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(1.dp, palette.border, CircleShape)
        )
    } else {
        val initials = (user?.name ?: "U")
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(user?.accentHex ?: 0xFF6C8FF5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.36f).sp
            )
        }
    }
}

enum class CheckboxAccent {
    LAVENDER,
    PINK,
    PEACH,
    SAGE
}

@Composable
fun AnimatedCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    contentDescription: String,
    accent: CheckboxAccent = CheckboxAccent.LAVENDER,
    modifier: Modifier = Modifier
) {
    val palette = LocalDuoDeskPalette.current
    val accentColor = when (accent) {
        CheckboxAccent.LAVENDER -> palette.lavender
        CheckboxAccent.PINK -> palette.pink
        CheckboxAccent.PEACH -> palette.peach
        CheckboxAccent.SAGE -> palette.sage
    }
    val fill by animateColorAsState(
        targetValue = if (checked) accentColor else palette.card,
        label = "checkbox_fill"
    )
    val border by animateColorAsState(
        targetValue = if (checked) accentColor else palette.border,
        label = "checkbox_border"
    )
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "checkbox_scale"
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(
                role = Role.Checkbox,
                onClickLabel = contentDescription,
                onClick = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(fill)
                .border(2.dp, border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.16f, size.height * 0.52f)
                        lineTo(size.width * 0.42f, size.height * 0.78f)
                        lineTo(size.width * 0.84f, size.height * 0.24f)
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalDuoDeskPalette.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = palette.card,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.pinkSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.pinkInk,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAction,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryButtonBg,
                        contentColor = palette.primaryButtonText
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("empty_state_action")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun priorityDotColor(priority: com.example.model.Priority): Color {
    val palette = LocalDuoDeskPalette.current
    return when (priority) {
        com.example.model.Priority.HIGH -> palette.error
        com.example.model.Priority.MEDIUM -> PriorityMedLight
        com.example.model.Priority.LOW -> PriorityLowDot
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuoDeskTopHeader(
    isHome: Boolean,
    routeTitle: String,
    currentUser: UserProfile?,
    notifications: List<NotificationItem>,
    unreadCount: Int,
    onProfileClick: () -> Unit,
    onCreateClick: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearNotifications: () -> Unit
) {
    val palette = LocalDuoDeskPalette.current
    var showNotificationsSheet by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isHome) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .clickable(onClick = onProfileClick)
                        .testTag("header_avatar_button")
                ) {
                    UserAvatar(user = currentUser, size = 40.dp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = Formatters.formatTodayHeader(),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "${Formatters.getGreetingByHour(currentUser?.name ?: "Rushil")}!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DuoDesk · ${Formatters.formatTodayHeader()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
                Text(
                    text = routeTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Surface(
                    onClick = { showNotificationsSheet = true },
                    shape = CircleShape,
                    color = palette.card,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("notifications_bell_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (unreadCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                            .clip(CircleShape)
                            .background(palette.pink)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Button(
                onClick = onCreateClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryButtonBg,
                    contentColor = palette.primaryButtonText
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("header_new_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            containerColor = palette.card,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (unreadCount > 0) {
                            TextButton(onClick = onMarkAllRead) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp), tint = palette.lavender)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark all read", color = palette.lavender, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (notifications.isNotEmpty()) {
                            IconButton(onClick = onClearNotifications) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear notifications", tint = palette.textSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (notifications.isEmpty()) {
                    Text(
                        text = "All caught up. No new alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        items(notifications, key = { it.id }) { notif ->
                            Surface(
                                onClick = {
                                    if (!notif.read) onMarkNotificationRead(notif.id)
                                    showNotificationsSheet = false
                                    onNotificationClick(notif)
                                },
                                shape = RoundedCornerShape(18.dp),
                                color = if (!notif.read) palette.lavenderSoft.copy(alpha = 0.45f) else palette.subtleSurface,
                                border = BorderStroke(1.dp, palette.divider)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (notif.read) FontWeight.Medium else FontWeight.Bold
                                            ),
                                            color = if (notif.read) palette.textSecondary else palette.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = notif.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = Formatters.formatRelativeTime(notif.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textTertiary
                                        )
                                    }
                                    if (!notif.read) {
                                        IconButton(
                                            onClick = { onMarkNotificationRead(notif.id) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Mark as read",
                                                tint = palette.lavender,
                                                modifier = Modifier.size(18.dp)
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
    }
}

enum class NavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    MY_SPACE("My Space", Icons.Filled.FolderShared, Icons.Outlined.Folder),
    SHARED("Shared", Icons.Filled.FolderShared, Icons.Outlined.Group),
    CALENDAR("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun DuoDeskBottomNav(
    selectedTab: NavTab,
    onSelectTab: (NavTab) -> Unit
) {
    val palette = LocalDuoDeskPalette.current
    Surface(
        color = palette.navSurface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.entries.forEach { tab ->
                val active = tab == selectedTab
                val pillBg = if (active) palette.pinkSoft else Color.Transparent
                val iconTint = if (active) palette.pinkInk else palette.textSecondary
                val labelColor = if (active) palette.textPrimary else palette.textSecondary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(18.dp))
                        .background(pillBg)
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 8.dp)
                        .testTag("nav_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (active) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DuoDeskNavRail(
    selectedTab: NavTab,
    currentUser: UserProfile?,
    otherUser: UserProfile?,
    onSelectTab: (NavTab) -> Unit,
    onSwitchUser: (String) -> Unit
) {
    val palette = LocalDuoDeskPalette.current
    Surface(
        color = palette.navSurface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxHeight()
            .width(216.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectTab(NavTab.HOME) }
                        .padding(4.dp)
                ) {
                    DuoDeskLogoMark(size = 38.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DuoDesk",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Your space. Our progress.",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NavTab.entries.forEach { tab ->
                        val active = tab == selectedTab
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(if (active) palette.pinkSoft else Color.Transparent)
                                .clickable { onSelectTab(tab) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("rail_${tab.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (active) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                tint = if (active) palette.pinkInk else palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (tab == NavTab.SHARED) "Shared Space" else tab.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (active) palette.textPrimary else palette.textSecondary
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.subtleSurface,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(user = currentUser, size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.name ?: "Member",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (currentUser?.id == "rushil") "User A" else "User B",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                    if (otherUser != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onSwitchUser(otherUser.id) },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, palette.border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = palette.card,
                                contentColor = palette.textPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Switch to ${otherUser.name}",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToastBannerHost(
    toasts: List<ToastMessage>,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDuoDeskPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        toasts.forEach { toast ->
            val accent = when (toast.tone) {
                ToastTone.SUCCESS -> palette.sage
                ToastTone.WARNING -> palette.error
                ToastTone.NEUTRAL -> palette.lavender
            }
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.card,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.widthIn(max = 420.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = toast.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textPrimary
                                )
                                if (!toast.description.isNullOrBlank()) {
                                    Text(
                                        text = toast.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { onDismiss(toast.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss notification",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
