package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.CreateKind
import com.example.model.ThemeMode
import com.example.model.Visibility
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.DuoDeskTheme
import com.example.ui.theme.LocalDuoDeskPalette
import com.example.ui.viewmodel.CreateDefaults
import com.example.ui.viewmodel.DuoDeskViewModel
import kotlinx.serialization.Serializable

@Serializable object RouteHome
@Serializable object RouteMySpace
@Serializable object RouteSharedSpace
@Serializable object RouteCalendar
@Serializable object RouteProfile
@Serializable object RouteCreate

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DuoDeskApplication

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
            val vm: DuoDeskViewModel = viewModel(factory = DuoDeskViewModel.Factory(app.repository))
            val state by vm.uiState.collectAsStateWithLifecycle()

            DuoDeskTheme(darkTheme = state.themeMode == ThemeMode.DARK) {
                val palette = LocalDuoDeskPalette.current

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.background)
                ) {
                    if (state.currentUserId == null) {
                        LoginScreen(
                            users = state.users,
                            onQuickLogin = { vm.quickLogin(it) },
                            onFormSignIn = { vm.signInWithEmail(it) }
                        )
                    } else {
                        val navController = rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDest = navBackStackEntry?.destination

                        val selectedTab = when {
                            currentDest?.hasRoute<RouteMySpace>() == true -> NavTab.MY_SPACE
                            currentDest?.hasRoute<RouteSharedSpace>() == true -> NavTab.SHARED
                            currentDest?.hasRoute<RouteCalendar>() == true -> NavTab.CALENDAR
                            currentDest?.hasRoute<RouteProfile>() == true -> NavTab.PROFILE
                            else -> NavTab.HOME
                        }

                        val isHome = currentDest == null || currentDest.hasRoute<RouteHome>()
                        val routeTitle = when {
                            currentDest?.hasRoute<RouteMySpace>() == true -> "My Space"
                            currentDest?.hasRoute<RouteSharedSpace>() == true -> "Shared Space"
                            currentDest?.hasRoute<RouteCalendar>() == true -> "Calendar"
                            currentDest?.hasRoute<RouteProfile>() == true -> "Profile"
                            currentDest?.hasRoute<RouteCreate>() == true -> "New Item"
                            else -> "Home"
                        }

                        fun navigateTab(tab: NavTab) {
                            val target: Any = when (tab) {
                                NavTab.HOME -> RouteHome
                                NavTab.MY_SPACE -> RouteMySpace
                                NavTab.SHARED -> RouteSharedSpace
                                NavTab.CALENDAR -> RouteCalendar
                                NavTab.PROFILE -> RouteProfile
                            }
                            navController.navigate(target) {
                                popUpTo(RouteHome) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        Row(modifier = Modifier.fillMaxSize()) {
                            if (isWideScreen) {
                                DuoDeskNavRail(
                                    selectedTab = selectedTab,
                                    currentUser = state.currentUser,
                                    otherUser = state.otherUser,
                                    onSelectTab = ::navigateTab,
                                    onSwitchUser = { vm.switchUser(it) }
                                )
                            }

                            Scaffold(
                                containerColor = palette.background,
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                topBar = {
                                    DuoDeskTopHeader(
                                        isHome = isHome,
                                        routeTitle = routeTitle,
                                        currentUser = state.currentUser,
                                        notifications = state.notifications,
                                        unreadCount = state.unreadNotificationsCount,
                                        onProfileClick = { navigateTab(NavTab.PROFILE) },
                                        onCreateClick = {
                                            vm.setCreateDefaults(
                                                CreateDefaults(
                                                    kind = CreateKind.TASK,
                                                    visibility = if (selectedTab == NavTab.SHARED) Visibility.SHARED else Visibility.PRIVATE
                                                )
                                            )
                                            navController.navigate(RouteCreate)
                                        },
                                        onNotificationClick = { notif ->
                                            when (notif.targetRoute) {
                                                "/shared-space" -> navigateTab(NavTab.SHARED)
                                                "/calendar" -> navigateTab(NavTab.CALENDAR)
                                                else -> navigateTab(NavTab.MY_SPACE)
                                            }
                                        },
                                        onMarkNotificationRead = { vm.markNotificationRead(it) },
                                        onMarkAllRead = { vm.markAllNotificationsRead() },
                                        onClearNotifications = { vm.clearNotifications() }
                                    )
                                },
                                bottomBar = {
                                    if (!isWideScreen && currentDest?.hasRoute<RouteCreate>() != true) {
                                        DuoDeskBottomNav(
                                            selectedTab = selectedTab,
                                            onSelectTab = ::navigateTab
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    NavHost(
                                        navController = navController,
                                        startDestination = RouteHome
                                    ) {
                                        composable<RouteHome> {
                                            HomeScreen(
                                                tasks = state.tasks,
                                                assignments = state.assignments,
                                                reminders = state.reminders,
                                                onToggleTask = { vm.toggleTaskCompleted(it) },
                                                onInspectItem = { vm.openInspector(it) },
                                                onViewAllTasks = { navigateTab(NavTab.MY_SPACE) },
                                                onOpenCalendar = { navigateTab(NavTab.CALENDAR) },
                                                onCreateTask = {
                                                    vm.setCreateDefaults(CreateDefaults(CreateKind.TASK, Visibility.PRIVATE))
                                                    navController.navigate(RouteCreate)
                                                }
                                            )
                                        }

                                        composable<RouteMySpace> {
                                            SpaceScreen(
                                                isShared = false,
                                                currentUser = state.currentUser,
                                                users = state.users,
                                                tasks = state.tasks,
                                                reminders = state.reminders,
                                                assignments = state.assignments,
                                                notes = state.notes,
                                                documents = state.documents,
                                                onToggleTask = { vm.toggleTaskCompleted(it) },
                                                onDeleteTask = { vm.deleteTask(it) },
                                                onToggleReminder = { vm.toggleReminderCompleted(it) },
                                                onDeleteReminder = { vm.deleteReminder(it) },
                                                onUpdateAssignmentStatus = { a, s -> vm.updateAssignmentStatus(a, s) },
                                                onAttachToAssignment = { a, refs -> vm.addAssignmentAttachments(a, refs) },
                                                onDeleteAssignment = { vm.deleteAssignment(it) },
                                                onCreateNote = { t, c, type, list, vis, cb -> vm.createNote(t, c, type, list, vis, cb) },
                                                onUpdateNote = { vm.updateNote(it) },
                                                onDeleteNote = { vm.deleteNote(it) },
                                                onUploadDocument = { n, k, ext, mime, sz, vis, uri, sum ->
                                                    vm.uploadDocument(n, k, ext, mime, sz, vis, uri, sum)
                                                },
                                                onPreviewDocument = { vm.openDocumentPreview(it) },
                                                onDeleteDocument = { vm.deleteDocument(it) },
                                                onInspectItem = { vm.openInspector(it) },
                                                onOpenCreate = { kind, vis ->
                                                    vm.setCreateDefaults(CreateDefaults(kind, vis))
                                                    navController.navigate(RouteCreate)
                                                }
                                            )
                                        }

                                        composable<RouteSharedSpace> {
                                            SpaceScreen(
                                                isShared = true,
                                                currentUser = state.currentUser,
                                                users = state.users,
                                                tasks = state.tasks,
                                                reminders = state.reminders,
                                                assignments = state.assignments,
                                                notes = state.notes,
                                                documents = state.documents,
                                                onToggleTask = { vm.toggleTaskCompleted(it) },
                                                onDeleteTask = { vm.deleteTask(it) },
                                                onToggleReminder = { vm.toggleReminderCompleted(it) },
                                                onDeleteReminder = { vm.deleteReminder(it) },
                                                onUpdateAssignmentStatus = { a, s -> vm.updateAssignmentStatus(a, s) },
                                                onAttachToAssignment = { a, refs -> vm.addAssignmentAttachments(a, refs) },
                                                onDeleteAssignment = { vm.deleteAssignment(it) },
                                                onCreateNote = { t, c, type, list, vis, cb -> vm.createNote(t, c, type, list, vis, cb) },
                                                onUpdateNote = { vm.updateNote(it) },
                                                onDeleteNote = { vm.deleteNote(it) },
                                                onUploadDocument = { n, k, ext, mime, sz, vis, uri, sum ->
                                                    vm.uploadDocument(n, k, ext, mime, sz, vis, uri, sum)
                                                },
                                                onPreviewDocument = { vm.openDocumentPreview(it) },
                                                onDeleteDocument = { vm.deleteDocument(it) },
                                                onInspectItem = { vm.openInspector(it) },
                                                onOpenCreate = { kind, vis ->
                                                    vm.setCreateDefaults(CreateDefaults(kind, vis))
                                                    navController.navigate(RouteCreate)
                                                }
                                            )
                                        }

                                        composable<RouteCalendar> {
                                            CalendarScreen(
                                                tasks = state.tasks,
                                                reminders = state.reminders,
                                                assignments = state.assignments,
                                                onInspectItem = { vm.openInspector(it) },
                                                onCreateOnDate = { dateIso ->
                                                    vm.setCreateDefaults(CreateDefaults(CreateKind.TASK, Visibility.PRIVATE, dateIso))
                                                    navController.navigate(RouteCreate)
                                                }
                                            )
                                        }

                                        composable<RouteProfile> {
                                            ProfileScreen(
                                                currentUser = state.currentUser,
                                                otherUser = state.otherUser,
                                                themeMode = state.themeMode,
                                                notifications = state.notifications,
                                                unreadCount = state.unreadNotificationsCount,
                                                notificationSettings = state.notificationSettings,
                                                onSwitchUser = { vm.switchUser(it) },
                                                onSaveProfile = { n, e, r -> vm.updateProfile(n, e, r) },
                                                onSetThemeMode = { vm.setThemeMode(it) },
                                                onMarkNotificationRead = { vm.markNotificationRead(it) },
                                                onMarkAllNotificationsRead = { vm.markAllNotificationsRead() },
                                                onClearNotifications = { vm.clearNotifications() },
                                                onUpdateNotificationSettings = { vm.updateNotificationSettings(it) },
                                                onResetDemoData = { vm.resetDemoData() },
                                                onLogout = { vm.logout() }
                                            )
                                        }

                                        composable<RouteCreate> {
                                            CreateScreen(
                                                defaults = state.createDefaults,
                                                onBack = { navController.popBackStack() },
                                                onCreateTask = { t, d, v, p, dd, dt, cat, atts ->
                                                    vm.createTask(t, d, v, p, dd, dt, cat, atts)
                                                },
                                                onCreateReminder = { t, d, v, dt, tm, p, rep, pref ->
                                                    vm.createReminder(t, d, v, dt, tm, p, rep, pref)
                                                },
                                                onCreateAssignment = { t, subj, d, v, dl, dlt, p, atts ->
                                                    vm.createAssignment(t, subj, d, v, dl, dlt, p, atts)
                                                },
                                                onCreateNote = { t, c, type, list, vis ->
                                                    vm.createNote(t, c, type, list, vis)
                                                },
                                                onUploadDocument = { n, k, ext, mime, sz, vis, uri, sum ->
                                                    vm.uploadDocument(n, k, ext, mime, sz, vis, uri, sum)
                                                },
                                                onDone = { vis ->
                                                    navController.popBackStack()
                                                    navigateTab(if (vis == Visibility.SHARED) NavTab.SHARED else NavTab.MY_SPACE)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    state.inspectTarget?.let { target ->
                        ItemDetailSheet(
                            target = target,
                            users = state.users,
                            onDismiss = { vm.openInspector(null) },
                            onSaveTask = { vm.saveTask(it) },
                            onDeleteTask = { vm.deleteTask(it) },
                            onSaveReminder = { vm.saveReminder(it) },
                            onDeleteReminder = { vm.deleteReminder(it) },
                            onSaveAssignment = { vm.saveAssignment(it) },
                            onDeleteAssignment = { vm.deleteAssignment(it) }
                        )
                    }

                    state.previewDocument?.let { doc ->
                        DocumentPreviewDialog(
                            doc = doc,
                            onDismiss = { vm.openDocumentPreview(null) }
                        )
                    }

                    ToastBannerHost(
                        toasts = state.toasts,
                        onDismiss = { vm.dismissToast(it) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.statusBars)
                    )
                }
            }
        }
    }
}
