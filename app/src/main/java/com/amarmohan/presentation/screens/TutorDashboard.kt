package com.amarmohan.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amarmohan.domain.model.*
import com.amarmohan.presentation.viewmodel.MainViewModel
import com.amarmohan.core.keyboard.moveFocusOnEnter

// Screens Enumeration
enum class TutorScreen {
    HOME,
    STUDENT_LIST,
    ATTENDANCE,
    REMINDERS,
    FEES,
    ENROLL_STUDENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val studentsList by viewModel.students.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val attendanceDate by viewModel.attendanceSelectedDate.collectAsState()
    val sessionSchedules by viewModel.sessionSchedules.collectAsState()

    // Modern local navigation screen routing
    var currentScreen by remember { mutableStateOf(TutorScreen.HOME) }
    var isEditMode by remember { mutableStateOf(false) }

    // Dialogs States
    var selectedStudentForDetail by remember { mutableStateOf<Student?>(null) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    var showSettingsMenu by remember { mutableStateOf(false) }

    // TODAY's Attendance metrics
    val presentToday = studentsList.count { viewModel.getAttendanceForDate(it, attendanceDate) == true }
    val absentToday = studentsList.count { viewModel.getAttendanceForDate(it, attendanceDate) == false }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFFBF9FF),
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFEFEAFF), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF7A68E6),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "AmarMohan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF423870)
                        )
                        Text(
                            text = "Tutor Controls",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B5E9F)
                        )
                    }
                }

                Divider(
                    color = Color(0xFFECE6FF),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Home Dash Item
                NavigationDrawerItem(
                    label = { Text("Home Dash", fontWeight = FontWeight.Bold) },
                    selected = currentScreen == TutorScreen.HOME,
                    onClick = {
                        currentScreen = TutorScreen.HOME
                        selectedStudentForDetail = null
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF7A68E6)) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFFEFEAFF),
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = Color(0xFF423870),
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Student List Item
                NavigationDrawerItem(
                    label = { Text("Student List", fontWeight = FontWeight.Bold) },
                    selected = currentScreen == TutorScreen.STUDENT_LIST,
                    onClick = {
                        currentScreen = TutorScreen.STUDENT_LIST
                        selectedStudentForDetail = null
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF7A68E6)) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFFEFEAFF),
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = Color(0xFF423870),
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Attendance sheet Item
                NavigationDrawerItem(
                    label = { Text("Attendance sheet", fontWeight = FontWeight.Bold) },
                    selected = currentScreen == TutorScreen.ATTENDANCE,
                    onClick = {
                        currentScreen = TutorScreen.ATTENDANCE
                        selectedStudentForDetail = null
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color(0xFF7A68E6)) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFFEFEAFF),
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = Color(0xFF423870),
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Fees Item
                NavigationDrawerItem(
                    label = { Text("Fees status", fontWeight = FontWeight.Bold) },
                    selected = currentScreen == TutorScreen.FEES,
                    onClick = {
                        currentScreen = TutorScreen.FEES
                        selectedStudentForDetail = null
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF7A68E6)) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFFEFEAFF),
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = Color(0xFF423870),
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Reminders Item
                NavigationDrawerItem(
                    label = { Text("Reminders", fontWeight = FontWeight.Bold) },
                    selected = currentScreen == TutorScreen.REMINDERS,
                    onClick = {
                        currentScreen = TutorScreen.REMINDERS
                        selectedStudentForDetail = null
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFF7A68E6)) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFFEFEAFF),
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = Color(0xFF423870),
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Divider(
                    color = Color(0xFFECE6FF),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                // Log out Item
                NavigationDrawerItem(
                    label = { Text("Log Out Teacher", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        showLogoutConfirmDialog = true
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color.Transparent,
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = MaterialTheme.colorScheme.error,
                        unselectedTextColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            containerColor = Color(0xFFFBF9FF), // Eye-pleasing clean modern canvas
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    TutorScreen.HOME -> {
                        TutorHomeScreen(
                            studentsCount = studentsList.size,
                            sessionSchedules = sessionSchedules,
                            onNavigate = { screen ->
                                currentScreen = screen
                                isEditMode = false
                            },
                            onSettingsClick = { showSettingsMenu = !showSettingsMenu },
                            showSettingsMenu = showSettingsMenu,
                            onDismissSettings = { showSettingsMenu = false },
                            viewModel = viewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }

                    TutorScreen.STUDENT_LIST -> {
                        TutorStudentListScreen(
                            studentsList = studentsList,
                            searchQuery = searchQuery,
                            isEditMode = isEditMode,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onToggleEdit = { isEditMode = !isEditMode },
                            onBack = { currentScreen = TutorScreen.HOME },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onStudentClick = { selectedStudentForDetail = it },
                            onAddStudentClick = { currentScreen = TutorScreen.ENROLL_STUDENT },
                            onDeleteStudent = { viewModel.deleteStudent(it.slNo) },
                            onSettingsClick = { showSettingsMenu = !showSettingsMenu },
                            showSettingsMenu = showSettingsMenu,
                            onDismissSettings = { showSettingsMenu = false },
                            viewModel = viewModel
                        )
                    }

                    TutorScreen.ATTENDANCE -> {
                        TutorAttendanceScreen(
                            studentsList = studentsList,
                            attendanceDate = attendanceDate,
                            viewModel = viewModel,
                            onBack = { currentScreen = TutorScreen.HOME },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onSettingsClick = { showSettingsMenu = !showSettingsMenu },
                            showSettingsMenu = showSettingsMenu,
                            onDismissSettings = { showSettingsMenu = false }
                        )
                    }

                    TutorScreen.REMINDERS -> {
                        TutorRemindersScreen(
                            studentsList = studentsList,
                            sessionSchedules = sessionSchedules,
                            viewModel = viewModel,
                            onBack = { currentScreen = TutorScreen.HOME },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onSettingsClick = { showSettingsMenu = !showSettingsMenu },
                            showSettingsMenu = showSettingsMenu,
                            onDismissSettings = { showSettingsMenu = false }
                        )
                    }

                    TutorScreen.FEES -> {
                        TutorFeesScreen(
                            studentsList = studentsList,
                            viewModel = viewModel,
                            onBack = { currentScreen = TutorScreen.HOME },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onSettingsClick = { showSettingsMenu = !showSettingsMenu },
                            showSettingsMenu = showSettingsMenu,
                            onDismissSettings = { showSettingsMenu = false }
                        )
                    }

                    TutorScreen.ENROLL_STUDENT -> {
                        TutorEnrollStudentScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = TutorScreen.STUDENT_LIST },
                            onEnrollSuccess = { newStudent ->
                                currentScreen = TutorScreen.STUDENT_LIST
                                selectedStudentForDetail = newStudent
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail overlay sheet
    selectedStudentForDetail?.let { activeStudentReference ->
        val liveStudent = studentsList.firstOrNull { it.slNo == activeStudentReference.slNo } ?: activeStudentReference

        StudentDetailOverlay(
            student = liveStudent,
            viewModel = viewModel,
            onDismiss = {
                selectedStudentForDetail = null
                currentScreen = TutorScreen.HOME
            }
        )
    }

    // Add Student registry modal
    if (showAddStudentDialog) {
        AddStudentDialog(
            viewModel = viewModel,
            onDismiss = {
                showAddStudentDialog = false
                if (selectedStudentForDetail == null && studentsList.isNotEmpty()) {
                    selectedStudentForDetail = studentsList.first()
                }
            },
            onEnrollSuccess = { newStudent ->
                showAddStudentDialog = false
                selectedStudentForDetail = newStudent
            }
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Log Out Confirmation", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of Shree AmarMohan?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun TutorRefreshIconButton(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp
) {
    val context = LocalContext.current
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    IconButton(
        onClick = {
            if (!isRefreshing) {
                Toast.makeText(context, "Syncing & refreshing data...", Toast.LENGTH_SHORT).show()
                viewModel.refreshData { success ->
                    val statusMsg = if (success) "Data synced with server!" else "Local data refreshed!"
                    Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                }
            }
        },
        enabled = !isRefreshing,
        modifier = modifier.testTag("refresh_button")
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                color = Color(0xFF423870),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = Color(0xFF423870),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

// ==========================================
// CUSTOM TOP BAR WITH LOGOUT BUTTON INSTEAD OF SETTINGS
// ==========================================
@Composable
fun TutorTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onOpenDrawer: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    showSettingsMenu: Boolean = false,
    onDismissSettings: () -> Unit = {},
    viewModel: MainViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0D7FF)) // Classic solid pastel lavender
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onOpenDrawer != null) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Drawer",
                            tint = Color(0xFF423870)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Back",
                            tint = Color(0xFF423870)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column {
                    Text(
                        text = "Shree AmarMohan 🧿",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5138B8)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF423870)
                    )
                }
            }

            TutorRefreshIconButton(viewModel = viewModel)
        }
    }
}

// ==========================================
// 1. HOME SCREEN MENU VIEW
// ==========================================
@Composable
fun TutorHomeScreen(
    studentsCount: Int,
    sessionSchedules: List<SessionSchedule>,
    onNavigate: (TutorScreen) -> Unit,
    onSettingsClick: () -> Unit = {},
    showSettingsMenu: Boolean = false,
    onDismissSettings: () -> Unit = {},
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit = {}
) {
    val studentsList by viewModel.students.collectAsState()
    val attendanceDates by viewModel.attendanceDates.collectAsState()
    var showRemindersDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(studentsList) {
        if (studentsList.isNotEmpty()) {
            viewModel.checkAndSendAutomaticBirthdayGreetings(context)
        }
    }

    // Pre-calculate unread and all reminders
    val allReminders = mutableListOf<Pair<Student, Reminder>>()
    studentsList.forEach { s ->
        s.getReminders().forEach { r ->
            allReminders.add(Pair(s, r))
        }
    }
    allReminders.sortByDescending { it.second.id }

    val unreadCount = allReminders.count { it.second.isByParent && !it.second.isReadByTutor }

    val birthdayStudents = remember(studentsList) {
        studentsList.filter { viewModel.isBirthdayToday(it.birthDate) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            // Elegant Pastel Header Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFE0D7FF), Color(0xFFF1EDFF))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Side Menu",
                                tint = Color(0xFF423870),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Home menu",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF423870)
                            )
                            Text(
                                text = "Shree AmarMohan Board 🧿",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF6B5E9F)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showRemindersDialog = true },
                            modifier = Modifier.testTag("notification_button")
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Reminders",
                                    tint = Color(0xFF423870),
                                    modifier = Modifier.size(28.dp)
                                )
                                if (unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, shape = CircleShape)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TutorRefreshIconButton(viewModel = viewModel, iconSize = 28.dp)
                    }
                }
            }
        }

        item {
            var birthdaySmsTriggerCount by remember { mutableIntStateOf(0) }
            var showAllStudentsBirthdayList by remember { mutableStateOf(false) }

            val unsentBirthdayStudents = remember(studentsList, birthdaySmsTriggerCount) {
                studentsList.filter { !viewModel.isBirthdaySmsSentToday(it.slNo) }
            }

            if (birthdayStudents.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F8)),
                    border = BorderStroke(1.dp, Color(0xFFFBCFE8)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎂", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Today's Student Birthday Celebration!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9D174D)
                            )
                        }
                        Text(
                            "Automatic SMS birthday greetings are scheduled for student birthdates. You can also send or resend birthday SMS instantly below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309)
                        )
                        birthdayStudents.forEach { bStudent ->
                            val isSmsSent = remember(bStudent.slNo, birthdaySmsTriggerCount) {
                                viewModel.isBirthdaySmsSentToday(bStudent.slNo)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFFCE7F3), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        bStudent.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF831843),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "DOB: ${bStudent.birthDate.ifBlank { "Not set" }} • Parent: ${bStudent.parentPhone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }

                                if (!isSmsSent) {
                                    Button(
                                        onClick = {
                                            viewModel.sendBirthdayGreetingManually(context, bStudent)
                                            birthdaySmsTriggerCount++
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("send_birthday_sms_${bStudent.slNo}")
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Send SMS", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Send Birthday SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = Color(0xFFFDF2F8),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFFF472B6))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Filled.Verified,
                                                    contentDescription = null,
                                                    tint = Color(0xFFDB2777),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "SMS Sent 🎂",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF9D174D)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = {
                                                viewModel.sendBirthdayGreetingManually(context, bStudent)
                                                birthdaySmsTriggerCount++
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Resend SMS",
                                                tint = Color(0xFFDB2777),
                                                modifier = Modifier.size(16.dp)
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

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ===================================
        // DUE FEES & RECENT CALENDAR
        // ===================================

        // UNPAID FEES Card
        item {
            val unpaidCount = studentsList.count { !it.isFeesPaid }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(86.dp)
                        .clickable { onNavigate(TutorScreen.FEES) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$unpaidCount",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Awaiting Tuition",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE11D48).copy(alpha = 0.8f)
                            )
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE11D48).copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(86.dp)
                        .clickable { onNavigate(TutorScreen.STUDENT_LIST) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2F6))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$studentsCount",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Students List",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF1E3A8A).copy(alpha = 0.8f)
                            )
                            Icon(
                                imageVector = Icons.Filled.People,
                                contentDescription = null,
                                tint = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // UNPAID STUDENTS LIST DIRECT ON DASHBOARD (OMITTED REDUNDANT VIEW AS REQUESTED)

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ATTENDANCE BY DATE Header
        item {
            Text(
                text = "ATTENDANCE SHEET DATEWISE",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF7A68E6),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        // Display how many were present datewise, clicking opens that day's attendance sheet
        val recentDates = attendanceDates.take(5)
        items(recentDates) { d ->
            val presentCount = studentsList.count { viewModel.getAttendanceForDate(it, d) == true }
            AttendanceDatewiseItem(
                date = d,
                presentCount = presentCount,
                totalCount = studentsList.size,
                onClick = {
                    viewModel.selectAttendanceDate(d)
                    onNavigate(TutorScreen.ATTENDANCE)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "🧿",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )
        }
    }

    if (showRemindersDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showRemindersDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reminders & Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF423870)
                        )
                        IconButton(onClick = { showRemindersDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (allReminders.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                                    .background(Color(0xFFF3F4F6), shape = RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No alerts or parent reminders registered right now.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            allReminders.forEach { (student, reminder) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (reminder.isByParent && !reminder.isReadByTutor) Color(0xFFF5EFFF) else Color.White
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (reminder.isByParent && !reminder.isReadByTutor) Color(0xFFD3BDF2) else Color(0xFFECECEC)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                color = if (reminder.isByParent) Color(0xFF7C3AED) else Color(0xFF3B82F6),
                                                                shape = RoundedCornerShape(6.dp)
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (reminder.isByParent) "Parent" else "Tutor",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    if (reminder.isByParent && !reminder.isReadByTutor) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .background(Color(0xFF10B981), shape = RoundedCornerShape(6.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "Unread",
                                                                color = Color.White,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Student: 🧿 ${student.name} (Reg No: ${student.slNo})",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = reminder.text,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.DarkGray
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (reminder.isByParent && !reminder.isReadByTutor) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(
                                                        onClick = { viewModel.markReminderReadByTutor(student.slNo, reminder.id) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Mark Read",
                                                            tint = Color(0xFF10B981),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { viewModel.removeReminder(student.slNo, reminder.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = Color.Red.copy(alpha = 0.7f),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showRemindersDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                    ) {
                        Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MenuRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF7C3AED).copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AttendanceDatewiseItem(
    date: String,
    presentCount: Int,
    totalCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFECE6FF), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color(0xFF7A68E6),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Text(
                    text = "Attendance Records",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (presentCount > 0) Color(0xFFDCFCE7) else Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$presentCount / $totalCount Present",
                    color = if (presentCount > 0) Color(0xFF16A34A) else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ==========================================
// 2. STUDENT REGISTRY VIEW WITH EDIT SWITCH
// ==========================================
@Composable
fun TutorStudentListScreen(
    studentsList: List<Student>,
    searchQuery: String,
    isEditMode: Boolean,
    onSearchChange: (String) -> Unit,
    onToggleEdit: () -> Unit,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onStudentClick: (Student) -> Unit,
    onAddStudentClick: () -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsMenu: Boolean,
    onDismissSettings: () -> Unit,
    viewModel: MainViewModel
) {
    val filtered = studentsList.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.slNo.contains(searchQuery, ignoreCase = true) ||
                it.standard.contains(searchQuery, ignoreCase = true)
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TutorTopBar(
                title = "Student List",
                onBack = onBack,
                onOpenDrawer = onOpenDrawer,
                onSettingsClick = onSettingsClick,
                showSettingsMenu = showSettingsMenu,
                onDismissSettings = onDismissSettings,
                viewModel = viewModel
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status Summary Top bar matching screenshot
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF8FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, Color(0xFFECE5FF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFEDE9FE), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CoPresent,
                                    contentDescription = null,
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${studentsList.size} Students",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Room: Main Hall",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Edit Pill Toggle
                        if (!isEditMode) {
                            Button(
                                onClick = onToggleEdit,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE9E4FF)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit List Mode Toggle",
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit",
                                    color = Color(0xFF7C3AED),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = onToggleEdit,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Confirm",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Done",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            androidx.compose.material3.OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search Students...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("student_search_bar"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color(0xFFE4E0FC),
                    focusedContainerColor = Color(0xFFFAF9FF),
                    unfocusedContainerColor = Color(0xFFFAF9FF)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Student list body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No matching registry found.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(filtered) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!isEditMode) onStudentClick(s) }
                                    .padding(horizontal = 24.dp, vertical = 14.dp)
                                    .testTag("student_item_${s.slNo}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🧿 " + s.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                    val parentInfo = listOfNotNull(
                                        if (s.parentName.isNotBlank()) "Parent: ${s.parentName}" else null,
                                        if (s.parentPhone.isNotBlank()) s.parentPhone else null
                                    ).joinToString(" • ")
                                    if (parentInfo.isNotBlank()) {
                                        Text(
                                            text = parentInfo,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF6D28D9)
                                        )
                                    }
                                }

                                if (!isEditMode) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = "Room Options Grid",
                                            tint = Color(0xFF7C3AED).copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { onDeleteStudent(s) },
                                        modifier = Modifier
                                            .border(1.dp, Color(0xFFFFD4D4), RoundedCornerShape(8.dp))
                                            .size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete from Register",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF3F1FA), modifier = Modifier.padding(horizontal = 24.dp))
                        }
                    }
                }

                // Add Student Lavender centered button always visible
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Button(
                        onClick = onAddStudentClick,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .width(180.dp)
                            .height(48.dp)
                            .testTag("add_student_fab"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCD6FF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF5138B8)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Add Student",
                            color = Color(0xFF5138B8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. ATTENDANCE SCREEN
// ==========================================
@Composable
fun TutorAttendanceScreen(
    studentsList: List<Student>,
    attendanceDate: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsMenu: Boolean,
    onDismissSettings: () -> Unit
) {
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TutorTopBar(
                title = "Attendance Sheet",
                onBack = onBack,
                onOpenDrawer = onOpenDrawer,
                onSettingsClick = onSettingsClick,
                showSettingsMenu = showSettingsMenu,
                onDismissSettings = onDismissSettings,
                viewModel = viewModel
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal chron sliding dates selection bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAF9FF))
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Column {
                    val attendanceDates by viewModel.attendanceDates.collectAsState()
                    val context = androidx.compose.ui.platform.LocalContext.current

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "WORK DATE REGISTRY",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    val calendar = java.util.Calendar.getInstance()
                                    val parts = attendanceDate.split("-")
                                    if (parts.size == 3) {
                                        try {
                                            calendar.set(java.util.Calendar.YEAR, parts[0].toInt())
                                            calendar.set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                                            calendar.set(java.util.Calendar.DAY_OF_MONTH, parts[2].toInt())
                                        } catch (e: Exception) {}
                                    }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedDateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                            viewModel.selectAttendanceDate(selectedDateStr)
                                        },
                                        calendar.get(java.util.Calendar.YEAR),
                                        calendar.get(java.util.Calendar.MONTH),
                                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Search custom date",
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Search Custom Date",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED)
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Special Calendar Picker chip at the start
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val calendar = java.util.Calendar.getInstance()
                                    val parts = attendanceDate.split("-")
                                    if (parts.size == 3) {
                                        try {
                                            calendar.set(java.util.Calendar.YEAR, parts[0].toInt())
                                            calendar.set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                                            calendar.set(java.util.Calendar.DAY_OF_MONTH, parts[2].toInt())
                                        } catch (e: Exception) {}
                                    }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedDateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                            viewModel.selectAttendanceDate(selectedDateStr)
                                        },
                                        calendar.get(java.util.Calendar.YEAR),
                                        calendar.get(java.util.Calendar.MONTH),
                                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(0xFF7C3AED)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Calendar Picker")
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFFF5F3FF),
                                    labelColor = Color(0xFF7C3AED)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = Color(0xFFDDD6FE)
                                ),
                                modifier = Modifier.testTag("calendar_picker_chip")
                            )
                        }

                        items(attendanceDates) { d ->
                            val isSelected = d == attendanceDate
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectAttendanceDate(d) },
                                label = { Text(d) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF7C3AED),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = Color.DarkGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color(0xFFEDE9FE)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            var showAttendanceDropdown by remember { mutableStateOf(false) }
            val pastMonthsList = remember { viewModel.getPastTwelveMonths() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Roll Call Checklist:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Box {
                    Button(
                        onClick = { showAttendanceDropdown = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("download_attendance_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Attendance ▾", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = showAttendanceDropdown,
                        onDismissRequest = { showAttendanceDropdown = false }
                    ) {
                        pastMonthsList.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(viewModel.formatMonthYear(m)) },
                                onClick = {
                                    showAttendanceDropdown = false
                                    val result = viewModel.downloadAttendanceExcelForMonth(context, m)
                                    android.widget.Toast.makeText(context, result, android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                }
            }

            if (studentsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No students currently registered.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                ) {
                    items(studentsList) { s ->
                        val wasPresent = viewModel.getAttendanceForDate(s, attendanceDate)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧿 " + s.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )

                            // Modern Pill Buttons Side-by-Side matching mockup
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Present Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(if (wasPresent == true) Color(0xFF22C55E) else Color(0xFFECEAEF))
                                        .clickable {
                                            val newStatus = if (wasPresent == true) null else true
                                            viewModel.setAttendanceStatus(s.slNo, attendanceDate, newStatus)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("✔", color = if (wasPresent == true) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Present",
                                            color = if (wasPresent == true) Color.White else Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Absent Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(if (wasPresent == false) Color(0xFFEF4444) else Color(0xFFECEAEF))
                                        .clickable {
                                            val newStatus = if (wasPresent == false) null else false
                                            viewModel.setAttendanceStatus(s.slNo, attendanceDate, newStatus)
                                            if (newStatus == false) {
                                                viewModel.sendAbsentNotificationSms(context, s.parentPhone, attendanceDate)
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("✖", color = if (wasPresent == false) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Absent",
                                            color = if (wasPresent == false) Color.White else Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF3F1FA), modifier = Modifier.padding(horizontal = 24.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. REMINDERS VIEW WITH MONTHLY CALENDAR
// ==========================================
@Composable
fun TutorRemindersScreen(
    studentsList: List<Student>,
    sessionSchedules: List<SessionSchedule>,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsMenu: Boolean,
    onDismissSettings: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val localContext = LocalContext.current
    var activeSubTab by remember { mutableStateOf(0) } // 0: Fast Alerts, 1: Schedule Classes

    val demosCount by viewModel.demosCount.collectAsState()
    val checkedInsCount by viewModel.checkedInsCount.collectAsState()
    val enquiriesCount by viewModel.enquiriesCount.collectAsState()
    val unitTestsCount by viewModel.unitTestsCount.collectAsState()

    val todayCalendar = remember { java.util.Calendar.getInstance() }
    val todayString = remember {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.format(todayCalendar.time)
    }
    var selectedCalendarDay by remember { mutableStateOf(todayString) }
    var currentYear by remember { mutableStateOf(todayCalendar.get(java.util.Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(todayCalendar.get(java.util.Calendar.MONTH) + 1) } // 1-indexed

    val allReminders = mutableListOf<Pair<Student, Reminder>>()
    studentsList.forEach { s ->
        s.getReminders().forEach { r ->
            allReminders.add(Pair(s, r))
        }
    }

    Scaffold(
        topBar = {
            TutorTopBar(
                title = "Reminders",
                onBack = onBack,
                onOpenDrawer = onOpenDrawer,
                onSettingsClick = onSettingsClick,
                showSettingsMenu = showSettingsMenu,
                onDismissSettings = onDismissSettings,
                viewModel = viewModel
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Upper Grid Cards Group matching Reminders columns
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Demos
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("$demosCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF7C3AED))
                                    Text("Demos", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewModel.updateDemosCount(-1) },
                                        modifier = Modifier.size(24.dp).background(Color.White, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = Color(0xFF7C3AED), modifier = Modifier.size(12.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.updateDemosCount(1) },
                                        modifier = Modifier.size(24.dp).background(Color(0xFF7C3AED), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increment", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }

                        // Card 3: Checked ins
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDDD2FF)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("$checkedInsCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF5138B8))
                                    Text("Checked-ins", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewModel.updateCheckedInsCount(-1) },
                                        modifier = Modifier.size(24.dp).background(Color.White, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = Color(0xFF5138B8), modifier = Modifier.size(12.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.updateCheckedInsCount(1) },
                                        modifier = Modifier.size(24.dp).background(Color(0xFF5138B8), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increment", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 2: Enquiries
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE2D9FF)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("$enquiriesCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF63569B))
                                    Text("Enquiries", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewModel.updateEnquiriesCount(-1) },
                                        modifier = Modifier.size(24.dp).background(Color.White, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = Color(0xFF63569B), modifier = Modifier.size(12.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.updateEnquiriesCount(1) },
                                        modifier = Modifier.size(24.dp).background(Color(0xFF63569B), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increment", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }

                        // Card 4: 7 Unit Tests
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFFEFE8FC))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("$unitTestsCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF8B5CF6))
                                    Text("Unit Tests", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewModel.updateUnitTestsCount(-1) },
                                        modifier = Modifier.size(24.dp).background(Color.White, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = Color(0xFF8B5CF6), modifier = Modifier.size(12.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.updateUnitTestsCount(1) },
                                        modifier = Modifier.size(24.dp).background(Color(0xFF8B5CF6), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increment", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CALENDAR MONTH SELECTOR BLOCK CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFEEEAF4))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (currentMonth == 1) {
                                    currentMonth = 12
                                    currentYear -= 1
                                } else {
                                    currentMonth -= 1
                                }
                            }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.Gray)
                            }

                            val monthName = when (currentMonth) {
                                1 -> "January"
                                2 -> "February"
                                3 -> "March"
                                4 -> "April"
                                5 -> "May"
                                6 -> "June"
                                7 -> "July"
                                8 -> "August"
                                9 -> "September"
                                10 -> "October"
                                11 -> "November"
                                12 -> "December"
                                else -> ""
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(monthName, fontWeight = FontWeight.Bold, color = Color.DarkGray, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$currentYear", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                }
                            }
                            IconButton(onClick = {
                                if (currentMonth == 12) {
                                    currentMonth = 1
                                    currentYear += 1
                                } else {
                                    currentMonth += 1
                                }
                            }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Days titles row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { d ->
                                Text(
                                    text = d,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Calculate Weeks and Days dynamically
                        val calendar = remember(currentYear, currentMonth) {
                            java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.YEAR, currentYear)
                                set(java.util.Calendar.MONTH, currentMonth - 1)
                                set(java.util.Calendar.DAY_OF_MONTH, 1)
                            }
                        }
                        val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                        val firstDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.

                        val padding = firstDayOfWeek - 1
                        val daysList = remember(currentYear, currentMonth, daysInMonth, padding) {
                            val list = mutableListOf<Int?>()
                            for (i in 0 until padding) {
                                list.add(null)
                            }
                            for (i in 1..daysInMonth) {
                                list.add(i)
                            }
                            list
                        }
                        val weeks = remember(daysList) { daysList.chunked(7) }

                        weeks.forEach { week ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                for (i in 0 until 7) {
                                    if (i < week.size) {
                                        val dayNum = week[i]
                                        if (dayNum != null) {
                                            val dateString = String.format("%04d-%02d-%02d", currentYear, currentMonth, dayNum)
                                            val isSelected = selectedCalendarDay == dateString
                                            val hasSched = sessionSchedules.any { it.date == dateString }
                                            val hasRem = allReminders.any { it.second.date == dateString }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1.1f)
                                                    .padding(2.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color(0xFF1E1E24) else Color.Transparent)
                                                    .clickable { selectedCalendarDay = dateString },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                    Text(
                                                        text = "$dayNum",
                                                        color = if (isSelected) Color.White else Color.DarkGray,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 11.sp
                                                    )
                                                    if (hasSched || hasRem) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(top = 1.dp)
                                                        ) {
                                                            if (hasSched) {
                                                                Box(modifier = Modifier.size(4.dp).background(Color(0xFF3B82F6), CircleShape))
                                                            }
                                                            if (hasRem) {
                                                                Box(modifier = Modifier.size(4.dp).background(Color(0xFF8B5CF6), CircleShape))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tabs for Reminders Actions (Broadcast and Timetable Schedules)
            item {
                TabRow(
                    selectedTabIndex = activeSubTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFF7C3AED),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Tab(
                        selected = activeSubTab == 0,
                        onClick = { activeSubTab = 0 },
                        text = { Text("Broadcast portion", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = activeSubTab == 1,
                        onClick = { activeSubTab = 1 },
                        text = { Text("Schedule timing", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }

            if (activeSubTab == 0) {
                // ==========================================
                // SUB-TAB A: BROADCAST ALERT PORTION
                // ==========================================
                item {
                    var reminderText by remember { mutableStateOf("") }
                    var alertStudentSlNos by remember { mutableStateOf(setOf<String>()) }
                    var dropdownExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF9FF)),
                        border = BorderStroke(1.dp, Color(0xFFECE5FF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Fast Alert Broadcast", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { dropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth().testTag("add_reminder_dropdown"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    val selectedText = when {
                                        alertStudentSlNos.isEmpty() -> "Select student targets"
                                        alertStudentSlNos.size == studentsList.size -> "All Students (${studentsList.size})"
                                        else -> "${alertStudentSlNos.size} student(s) selected"
                                    }
                                    Text(selectedText)
                                }

                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    val isAllSelected = alertStudentSlNos.size == studentsList.size
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = isAllSelected,
                                                    onCheckedChange = null
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Select All", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        onClick = {
                                            alertStudentSlNos = if (isAllSelected) {
                                                emptySet()
                                            } else {
                                                studentsList.map { it.slNo }.toSet()
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                    studentsList.forEach { s ->
                                        val isSelected = alertStudentSlNos.contains(s.slNo)
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = null
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("🧿 " + s.name)
                                                }
                                            },
                                            onClick = {
                                                alertStudentSlNos = if (isSelected) {
                                                    alertStudentSlNos - s.slNo
                                                } else {
                                                    alertStudentSlNos + s.slNo
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            androidx.compose.material3.OutlinedTextField(
                                value = reminderText,
                                onValueChange = { reminderText = it },
                                placeholder = { Text("Enter portion specifics, exams, payments context...") },
                                modifier = Modifier.fillMaxWidth().testTag("add_reminder_text"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Button(
                                enabled = alertStudentSlNos.isNotEmpty() && reminderText.trim().isNotEmpty(),
                                onClick = {
                                    alertStudentSlNos.forEach { slNo ->
                                        viewModel.addReminder(slNo, reminderText.trim(), isImportant = true)
                                    }
                                    reminderText = ""
                                    alertStudentSlNos = emptySet()
                                    Toast.makeText(localContext, "Broadcast logged for selected students!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().testTag("submit_reminder_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                            ) {
                                Text("Publish Notification")
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Broadcast Logs:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                if (allReminders.isEmpty()) {
                    item {
                        Text(
                            "No broadcasts yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    items(allReminders) { (student, r) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFECE5FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("🧿 " + student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7C3AED))
                                    Text(r.text, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                                }
                                IconButton(
                                    onClick = { viewModel.removeReminder(student.slNo, r.id) },
                                    modifier = Modifier.testTag("dismiss_reminder_${r.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // SUB-TAB B: TIMETABLE SCHEDULES
                // ==========================================
                item {
                    var topicText by remember { mutableStateOf("") }
                    var classTimeText by remember { mutableStateOf("") }
                    var scheduleStudentSlNos by remember { mutableStateOf(setOf<String>()) }
                    var durationText by remember { mutableStateOf("60") }
                    var selectDropdownExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF9FF)),
                        border = BorderStroke(1.dp, Color(0xFFECE5FF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Schedule class for $selectedCalendarDay", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { selectDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    val selectedText = when {
                                        scheduleStudentSlNos.isEmpty() -> "Select student targets"
                                        scheduleStudentSlNos.size == studentsList.size -> "All Students (${studentsList.size})"
                                        else -> "${scheduleStudentSlNos.size} student(s) selected"
                                    }
                                    Text(selectedText)
                                }

                                DropdownMenu(
                                    expanded = selectDropdownExpanded,
                                    onDismissRequest = { selectDropdownExpanded = false }
                                ) {
                                    val isAllSelected = scheduleStudentSlNos.size == studentsList.size
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = isAllSelected,
                                                    onCheckedChange = null
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Select All", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        onClick = {
                                            scheduleStudentSlNos = if (isAllSelected) {
                                                emptySet()
                                            } else {
                                                studentsList.map { it.slNo }.toSet()
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                    studentsList.forEach { s ->
                                        val isSelected = scheduleStudentSlNos.contains(s.slNo)
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = null
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("🧿 " + s.name)
                                                }
                                            },
                                            onClick = {
                                                scheduleStudentSlNos = if (isSelected) {
                                                    scheduleStudentSlNos - s.slNo
                                                } else {
                                                    scheduleStudentSlNos + s.slNo
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            androidx.compose.material3.OutlinedTextField(
                                value = topicText,
                                onValueChange = { topicText = it },
                                placeholder = { Text("Algebra, physics portion...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.material3.OutlinedTextField(
                                    value = classTimeText,
                                    onValueChange = { classTimeText = it },
                                    placeholder = { Text("Time: 5:30 PM") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                androidx.compose.material3.OutlinedTextField(
                                    value = durationText,
                                    onValueChange = { durationText = it },
                                    placeholder = { Text("60 mins") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Button(
                                enabled = scheduleStudentSlNos.isNotEmpty() && topicText.trim().isNotEmpty(),
                                onClick = {
                                    val dur = durationText.toIntOrNull() ?: 60
                                    scheduleStudentSlNos.forEach { slNo ->
                                        viewModel.addSessionSchedule(
                                            studentSlNo = slNo,
                                            topic = topicText.trim(),
                                            date = selectedCalendarDay,
                                            time = classTimeText.trim(),
                                            durationMinutes = dur
                                        )
                                    }
                                    topicText = ""
                                    classTimeText = ""
                                    scheduleStudentSlNos = emptySet()
                                    Toast.makeText(localContext, "Sessions Scheduled!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                            ) {
                                Text("Schedule timing")
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Configured timings for $selectedCalendarDay:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                val daySchedules = sessionSchedules.filter { it.date == selectedCalendarDay }

                if (daySchedules.isEmpty()) {
                    item {
                        Text(
                            "No timings configured.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    items(daySchedules) { sched ->
                        val targetStudent = studentsList.find { it.slNo == sched.studentSlNo }
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFECE5FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(targetStudent?.name?.let { "🧿 $it" } ?: "Student Sl: ${sched.studentSlNo}", fontWeight = FontWeight.Bold)
                                    Text(sched.topic, style = MaterialTheme.typography.bodyMedium)
                                    Text("Time: ${sched.time} (${sched.durationMinutes} mins)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = sched.isCompleted,
                                        onCheckedChange = { viewModel.markScheduleCompleted(sched, it) }
                                    )
                                    IconButton(onClick = { viewModel.deleteSessionSchedule(sched) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Reminders for $selectedCalendarDay:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                val dayReminders = allReminders.filter { it.second.date == selectedCalendarDay }

                if (dayReminders.isEmpty()) {
                    item {
                        Text(
                            "No reminders configured for this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                        )
                    }
                } else {
                    items(dayReminders) { (student, reminder) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
                            border = BorderStroke(1.dp, Color(0xFFEFE6FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(if (reminder.isByParent) Color(0xFF7C3AED) else Color(0xFF3B82F6), shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (reminder.isByParent) "Parent" else "Tutor",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(reminder.text, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { viewModel.removeReminder(student.slNo, reminder.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Reminder", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. FEES LEDGER VIEW
// ==========================================
@Composable
fun TutorFeesScreen(
    studentsList: List<Student>,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsMenu: Boolean,
    onDismissSettings: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentMonthStr = remember { java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date()) }
    var selectedMonth by remember { mutableStateOf(currentMonthStr) }
    val pastMonthsList = remember { viewModel.getPastTwelveMonths() }
    var showMonthDropdown by remember { mutableStateOf(false) }

    val demosCount by viewModel.demosCount.collectAsState()

    // Dynamic summary calculations based on selected month
    var totalTuitionEarned = 0.0
    var totalDues = 0.0

    studentsList.forEach { s ->
        val isPaid = viewModel.getStudentFeePaidForMonth(s, selectedMonth)
        if (isPaid) {
            totalTuitionEarned += s.tuitionFee
        } else {
            totalDues += s.tuitionFee
        }
    }

    val totalDemoFees = demosCount * 500.0
    val netEarnings = totalTuitionEarned + totalDemoFees

    Scaffold(
        topBar = {
            TutorTopBar(
                title = "Fees Ledger",
                onBack = onBack,
                onOpenDrawer = onOpenDrawer,
                onSettingsClick = onSettingsClick,
                showSettingsMenu = showSettingsMenu,
                onDismissSettings = onDismissSettings,
                viewModel = viewModel
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card Board
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F3FF)),
                    border = BorderStroke(1.dp, Color(0xFFECE6FF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Financial Summary Sheet",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF4A3E7D)
                                )
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clickable { showMonthDropdown = true }
                                            .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = viewModel.formatMonthYear(selectedMonth),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF7C3AED)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Month",
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMonthDropdown,
                                        onDismissRequest = { showMonthDropdown = false }
                                    ) {
                                        pastMonthsList.forEach { m ->
                                            DropdownMenuItem(
                                                text = { Text(viewModel.formatMonthYear(m)) },
                                                onClick = {
                                                    selectedMonth = m
                                                    showMonthDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        val logResult = viewModel.downloadLedgerText(context)
                                        clipboardManager.setText(AnnotatedString(logResult.substringBefore("\nCopy")))
                                        Toast.makeText(context, "$logResult", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("download_ledger_btn")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Text", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        val logResult = viewModel.downloadLedgerExcelForMonth(context, selectedMonth)
                                        Toast.makeText(context, "$logResult", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("download_ledger_excel_btn")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Grid metrics layout
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEFEAFF)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Tuition Earned", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text(
                                            "₹${totalTuitionEarned.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xFF7C3AED)
                                        )
                                    }
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEFEAFF)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Tuition Dues", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text(
                                            "₹${totalDues.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEFEAFF)),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Demos Earned (${demosCount})", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "₹${totalDemoFees.toInt()}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color(0xFF7C3AED)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { viewModel.updateDemosCount(-1) },
                                                    modifier = Modifier.size(24.dp).background(Color(0xFFEDE9FE), CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = Color(0xFF7C3AED), modifier = Modifier.size(12.dp))
                                                }
                                                IconButton(
                                                    onClick = { viewModel.updateDemosCount(1) },
                                                    modifier = Modifier.size(24.dp).background(Color(0xFF7C3AED), CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Increment", tint = Color.White, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEFEAFF)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Net Earnings", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text(
                                            "₹${netEarnings.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xFF7C3AED)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Spreadsheet Inputs Matrix:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            if (studentsList.isEmpty()) {
                item {
                    Text("No student records available.", modifier = Modifier.padding(horizontal = 24.dp))
                }
            } else {
                items(studentsList) { s ->
                    var tempFeeText by remember(s.slNo) { mutableStateOf(s.tuitionFee.toInt().toString()) }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFECE5FF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("🧿 " + s.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = Color.DarkGray)
                                    Text("SL No: ${s.slNo} • Grade: ${s.standard}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Paid?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Checkbox(
                                        checked = viewModel.getStudentFeePaidForMonth(s, selectedMonth),
                                        onCheckedChange = { isChecked ->
                                            val f = tempFeeText.toDoubleOrNull() ?: s.tuitionFee
                                            viewModel.updateStudentPaymentForMonth(s.slNo, selectedMonth, isChecked, f)
                                        },
                                        modifier = Modifier.testTag("pay_checkbox_${s.slNo}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Tuition Input
                                androidx.compose.material3.OutlinedTextField(
                                    value = tempFeeText,
                                    onValueChange = { newValue ->
                                        tempFeeText = newValue
                                        val f = newValue.toDoubleOrNull() ?: 0.0
                                        val isPaid = viewModel.getStudentFeePaidForMonth(s, selectedMonth)
                                        viewModel.updateStudentPaymentForMonth(s.slNo, selectedMonth, isPaid, f)
                                    },
                                    placeholder = { Text("Tuition Fee") },
                                    label = { Text("Tuition") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tuition_box_${s.slNo}"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                var tempDueText by remember(s.slNo) { mutableStateOf(s.dueAmount.toInt().toString()) }
                                androidx.compose.material3.OutlinedTextField(
                                    value = tempDueText,
                                    onValueChange = { newValue ->
                                        tempDueText = newValue
                                        val d = newValue.toDoubleOrNull() ?: 0.0
                                        viewModel.updateStudentDueAmount(s.slNo, d)
                                    },
                                    placeholder = { Text("Due Amount") },
                                    label = { Text("Due") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("due_box_${s.slNo}"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // Demo columns controls
                                Column(
                                    modifier = Modifier
                                        .background(
                                            Color(0xFFF3E8FF),
                                            shape = RoundedCornerShape(10.dp)
                                        ).padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Demos: ₹${s.demoFee.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.modifyDemoFee(s.slNo, isIncrement = false) },
                                            modifier = Modifier.size(28.dp).testTag("demo_minus_${s.slNo}"),
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White)
                                        ) {
                                            Text("-", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 12.sp)
                                        }
                                        Text("₹500", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        IconButton(
                                            onClick = { viewModel.modifyDemoFee(s.slNo, isIncrement = true) },
                                            modifier = Modifier.size(28.dp).testTag("demo_plus_${s.slNo}"),
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF7C3AED))
                                        ) {
                                            Text("+", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // Dynamic Dues displays
                            if (!s.isFeesPaid && s.dueAmount > 0.0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Pending Outstanding Due: ₹${s.dueAmount.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-PANEL: DETAILED STUDENT OVERLAY EDITOR (REDESIGNED)
// ==========================================

@Composable
fun MarksLineChart(
    subjects: List<SubjectMark>,
    testPapers: List<TestPaper> = emptyList()
) {
    val combinedScores = remember(subjects, testPapers) {
        val sList = subjects
        val tpList = testPapers.filter { it.marksObtained != null && it.totalMarks != null }
            .map { tp ->
                SubjectMark(
                    subjectName = tp.subjectName,
                    marks = tp.marksObtained!!,
                    maxMarks = tp.totalMarks!!,
                    testDate = tp.testDate,
                    testType = "Paper"
                )
            }
        (sList + tpList).distinctBy { "${it.subjectName}_${it.testDate}_${it.marks}_${it.maxMarks}" }
    }

    // Get unique subjects
    val uniqueSubjects = remember(combinedScores) {
        combinedScores.map { it.subjectName.trim() }.filter { it.isNotBlank() }.distinct()
    }

    if (uniqueSubjects.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFECE6FF)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No test marks recorded for this student yet.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    var selectedSubject by remember(uniqueSubjects) {
        mutableStateOf(uniqueSubjects.first())
    }

    // Filter by selected subject
    val filteredSubjects = remember(combinedScores, selectedSubject) {
        combinedScores.filter { it.subjectName.equals(selectedSubject, ignoreCase = true) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECE6FF)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Marks Trend ($selectedSubject)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6200EE)
                    )
                    Text(
                        if (filteredSubjects.isNotEmpty()) {
                            val acc = (filteredSubjects.map { if (it.maxMarks > 0) (it.marks / it.maxMarks * 100) else 0.0 }.average()).toInt()
                            "$acc% avg accuracy"
                        } else {
                            "0% accuracy"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            }

            // Chips row to filter by subject
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uniqueSubjects) { subj ->
                    val isSelected = subj.equals(selectedSubject, ignoreCase = true)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF7C3AED) else Color(0xFFF3E8FF)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { selectedSubject = subj }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = subj,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = if (isSelected) Color.White else Color(0xFF7C3AED),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (filteredSubjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No test records for $selectedSubject.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            } else {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().weight(1f)) {
                    val width = size.width
                    val height = size.height
                    val numGridLines = filteredSubjects.size.coerceAtLeast(1)

                    // Vertical pastel highlights
                    val sectionWidth = width / numGridLines
                    for (i in 0 until numGridLines) {
                        if (i % 2 == 0) {
                            drawRect(
                                color = Color(0xFFFFECEC),
                                topLeft = androidx.compose.ui.geometry.Offset(i * sectionWidth, 0f),
                                size = androidx.compose.ui.geometry.Size(sectionWidth, height)
                            )
                        }
                    }

                    // Horizontal grid lines
                    for (i in 1..3) {
                        val y = height * (i / 4f)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1.5f
                        )
                    }

                    // Map out coordinates
                    val points = filteredSubjects.mapIndexed { idx, item ->
                        val pct = (if (item.maxMarks > 0) (item.marks / item.maxMarks).toFloat() else 0f).coerceIn(0f, 1f)
                        val x = if (filteredSubjects.size <= 1) width / 2f else idx * (width / (filteredSubjects.size - 1))
                        val y = height - (pct * height * 0.8f) - (height * 0.1f)
                        androidx.compose.ui.geometry.Offset(x, y)
                    }

                    // Draw line curves
                    if (points.size >= 2) {
                        for (idx in 0 until points.size - 1) {
                            drawLine(
                                color = Color(0xFF8B5CF6),
                                start = points[idx],
                                end = points[idx + 1],
                                strokeWidth = 3f
                            )
                        }
                    }

                    // Node joints and marks value badges
                    points.forEachIndexed { idx, pt ->
                        val item = filteredSubjects[idx]

                        // Outer halo
                        drawCircle(
                            color = Color(0x337C3AED),
                            radius = 12.dp.toPx() / 2,
                            center = pt
                        )
                        // Outer white ring
                        drawCircle(
                            color = Color.White,
                            radius = 8.dp.toPx() / 2,
                            center = pt
                        )
                        // Inner point
                        drawCircle(
                            color = Color(0xFF6D28D9),
                            radius = 5.dp.toPx() / 2,
                            center = pt
                        )

                        // Render marks badge over dot
                        drawIntoCanvas { canvas ->
                            val marksText = if (item.marks % 1.0 == 0.0 && item.maxMarks % 1.0 == 0.0) {
                                "${item.marks.toInt()}/${item.maxMarks.toInt()}"
                            } else {
                                "${item.marks}/${item.maxMarks}"
                            }

                            val textPaint = Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 9.5f * density
                                typeface = Typeface.DEFAULT_BOLD
                                isAntiAlias = true
                                textAlign = Paint.Align.CENTER
                            }

                            val bgPaint = Paint().apply {
                                color = android.graphics.Color.parseColor("#4C1D95") // Deep Violet
                                isAntiAlias = true
                                style = Paint.Style.FILL
                            }

                            val borderPaint = Paint().apply {
                                color = android.graphics.Color.WHITE
                                isAntiAlias = true
                                style = Paint.Style.STROKE
                                strokeWidth = 1.2f * density
                            }

                            val textBounds = android.graphics.Rect()
                            textPaint.getTextBounds(marksText, 0, marksText.length, textBounds)
                            val textWidth = textBounds.width().toFloat()
                            val textHeight = textBounds.height().toFloat()

                            val padH = 5f * density
                            val padV = 2.5f * density

                            // If point is too close to top, show badge below, otherwise above
                            val badgeCenterY = if (pt.y < 30f * density) pt.y + 16f * density else pt.y - 13f * density
                            val rectLeft = (pt.x - textWidth / 2f - padH).coerceIn(2f * density, size.width - textWidth - padH * 2 - 2f * density)
                            val rectRight = rectLeft + textWidth + padH * 2
                            val rectTop = badgeCenterY - textHeight / 2f - padV
                            val rectBottom = badgeCenterY + textHeight / 2f + padV
                            val rectF = RectF(rectLeft, rectTop, rectRight, rectBottom)

                            val cornerRadius = 5f * density
                            canvas.nativeCanvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bgPaint)
                            canvas.nativeCanvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)

                            val textBaseline = rectTop + padV + textHeight - textBounds.bottom
                            val textCenterX = (rectLeft + rectRight) / 2f
                            canvas.nativeCanvas.drawText(marksText, textCenterX, textBaseline, textPaint)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (filteredSubjects.size <= 1) Arrangement.Center else Arrangement.SpaceBetween
                ) {
                    filteredSubjects.forEach { s ->
                        val displayDate = if (s.testDate.contains("-")) s.testDate.takeLast(5) else s.testDate
                        Text(
                            text = displayDate.ifEmpty { "${s.marks.toInt()}/${s.maxMarks.toInt()}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceDonutChart(presentPercent: Float, attendedDays: Int, totalDaysInMonth: Int = 30) {
    val present = presentPercent.coerceIn(0f, 1f)
    val absent = 1.0f - present

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECE6FF)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Attendance",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                "$attendedDays / $totalDaysInMonth Days Present",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val rect = androidx.compose.ui.geometry.Rect(
                        topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                        bottomRight = androidx.compose.ui.geometry.Offset(size.width - strokeWidth / 2, size.height - strokeWidth / 2)
                    )

                    // Present slice (blue)
                    drawArc(
                        color = Color(0xFF2563EB),
                        startAngle = -90f,
                        sweepAngle = present * 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
                        topLeft = rect.topLeft,
                        size = rect.size
                    )

                    // Absent slice (lilac)
                    drawArc(
                        color = Color(0xFFA78BFA),
                        startAngle = -90f + (present * 360f),
                        sweepAngle = absent * 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
                        topLeft = rect.topLeft,
                        size = rect.size
                    )
                }

                Text(
                    text = "${(present * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF2563EB), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Present", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFA78BFA), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Absent", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailOverlay(
    student: Student,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Core parameters state
    var syllabusText by remember(student.syllabus) { mutableStateOf(student.syllabus) }
    var newSyllabusSubjectInput by remember { mutableStateOf("") }
    var newSyllabusTopicInput by remember { mutableStateOf("") }
    var isEditingSyllabus by remember { mutableStateOf(false) }
    var activeSubViewTab by remember { mutableStateOf(0) } // 0: Dashboard overview, 1: Test Papers, 2: Edit Student
    var activeDashboardSubTab by remember { mutableStateOf(0) } // 0: Performance, 1: Attendance, 2: Syllabus portions
    var newAlertText by remember { mutableStateOf("") }

    // Local form states
    var scoreInput by remember { mutableStateOf("") }
    var scoreMaxInput by remember { mutableStateOf("100") }
    var testSubjectInput by remember { mutableStateOf("") }
    var testTypeInput by remember { mutableStateOf("Monthly") }
    var testPaperPhotoUri by remember { mutableStateOf<String?>(null) }

    // File upload mock simulation states
    var attachmentSubjectText by remember { mutableStateOf("") }
    var attachedPhotoUri by remember { mutableStateOf<String?>(null) }

    var tutorSelectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tutorCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var attachmentMarksText by remember { mutableStateOf("") }
    var attachmentMaxMarksText by remember { mutableStateOf("100") }

    val tutorFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            tutorSelectedImageUri = uri
            tutorCapturedBitmap = null
            testPaperPhotoUri = uri.toString()
            attachedPhotoUri = uri.toString()
            Toast.makeText(context, "File Selected!", Toast.LENGTH_SHORT).show()
        }
    }

    val tutorCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            tutorCapturedBitmap = bitmap
            tutorSelectedImageUri = null
            testPaperPhotoUri = "/data/simulated/tutor_captured_${System.currentTimeMillis()}.jpg"
            attachedPhotoUri = "/data/simulated/tutor_captured_${System.currentTimeMillis()}.jpg"
            Toast.makeText(context, "Camera Photo Captured!", Toast.LENGTH_SHORT).show()
        }
    }

    val tutorCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            tutorCameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos.", Toast.LENGTH_LONG).show()
        }
    }

    val onTutorLaunchCamera = {
        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            tutorCameraLauncher.launch(null)
        } else {
            tutorCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Core registry updates state
    var editName by remember { mutableStateOf(student.name) }
    var editParentName by remember { mutableStateOf(student.parentName) }
    var editPhone by remember { mutableStateOf(student.parentPhone) }
    var editFlatNo by remember { mutableStateOf(student.parentFlatNo) }
    var editBirthDate by remember { mutableStateOf(student.birthDate) }
    var editClass by remember { mutableStateOf(student.standard) }
    var editSchool by remember { mutableStateOf(student.school) }
    var editBoard by remember { mutableStateOf(student.board) }
    var editEmail by remember { mutableStateOf(student.studentEmail) }
    var editSubjectsList by remember { mutableStateOf(student.getSubjects()) }
    var newSubjectName by remember { mutableStateOf("") }
    val tutorCustomSubjectsList = remember {
        mutableStateListOf(
            "Mathematics", "Biology", "Physics", "Chemistry", "Kannada", "Hindi",
            "Science", "History", "Civics", "English 1", "English 2", "Geography"
        )
    }

    val subjects = student.getSubjects()
    val attendance = student.getAttendance()
    val cal = java.util.Calendar.getInstance()
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val currentMonthKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(cal.time)
    val monthAttendance = attendance.filterKeys { it.startsWith(currentMonthKey) && !it.startsWith("fee_") }
    val presentDays = if (monthAttendance.isNotEmpty()) {
        monthAttendance.count { it.value }
    } else {
        attendance.filterKeys { !it.startsWith("fee_") }.count { it.value }
    }
    val attendancePct = (presentDays.toFloat() / daysInMonth.toFloat()).coerceIn(0f, 1f)

    val avgScorePct = if (subjects.isNotEmpty()) {
        (subjects.map { it.marks / it.maxMarks }.average() * 100).toInt()
    } else {
        0
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = {
            if (activeSubViewTab != 0) {
                activeSubViewTab = 0
            } else {
                onDismiss()
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler {
            if (activeSubViewTab != 0) {
                activeSubViewTab = 0
            } else {
                onDismiss()
            }
        }

        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE2D6FF)) // Pastel Lavender
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (activeSubViewTab != 0) {
                                activeSubViewTab = 0
                            } else {
                                onDismiss()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF423870)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Student Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF423870)
                        )
                    }
                    IconButton(onClick = { activeSubViewTab = 2 }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Edit student registry",
                            tint = Color(0xFF423870)
                        )
                    }
                }
            },
            containerColor = Color(0xFFF9F7FF) // Visual pastel light canvas background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // REDESIGNED STUDENT PROFILE SECTION MATCHING THE CUTE AVATAR AND BADGE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3EDFF))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // "Sl No 1" rounded outline tag
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color(0xFF7A68E6), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Sl No ${student.slNo}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF5138B8),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = student.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF32236F)
                            )
                            val parentDetail = listOfNotNull(
                                if (student.parentName.isNotBlank()) "Parent: ${student.parentName}" else if (student.parentPhone.isNotBlank()) "Parent" else null,
                                if (student.parentPhone.isNotBlank()) student.parentPhone else null
                            ).joinToString(" • ")
                            if (parentDetail.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = parentDetail,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF6D28D9)
                                    )
                                }
                            }
                            if (student.birthDate.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cake,
                                        contentDescription = null,
                                        tint = Color(0xFFDB2777),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DOB: ${student.birthDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF9D174D)
                                    )
                                }
                            }
                            Text(
                                text = "${student.standard} • ${student.board} • ${student.school}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }

                        // Circular avatar matching cartoon profile guy in image
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEECC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Profile Avatar",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                // SUB-NAVIGATION VIEW TABS INSTEAD OF OVERFLOWS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Dashboard", "Test Papers", "Edit Student").forEachIndexed { idx, label ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { activeSubViewTab = idx }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (activeSubViewTab == idx) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeSubViewTab == idx) Color(0xFF6200EE) else Color.Gray,
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (activeSubViewTab == idx) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(2.dp)
                                        .background(Color(0xFF6200EE))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SUB-VIEWS CHOSEN CONTROLLERS:
                when (activeSubViewTab) {
                    0 -> {
                        // 0: MAIN DASHBOARD OVERVIEW: MATCHES USER'S GORGEOUS CHART AND STATS PANELS
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Quick Stats widgets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Marks summary Card
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFAF9CFFE)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("${avgScorePct}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF5138B8))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Marks Score", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                    }
                                }

                                // Attendance summary Card
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECEE)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("$presentDays/$daysInMonth", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Attendance", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                    }
                                }
                            }

                            // SEGMENTED TAB SELECTOR BASED ON THE THREE ICONS IN THE IMAGE (Performance, Attendance, Syllabus)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEDE9FE), RoundedCornerShape(16.dp))
                                    .padding(8.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Score/Performance Tab
                                Button(
                                    onClick = { activeDashboardSubTab = 0 },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeDashboardSubTab == 0) Color.White else Color.Transparent,
                                        contentColor = if (activeDashboardSubTab == 0) Color(0xFF6D28D9) else Color(0xFF5138B8)
                                    ),
                                    elevation = if (activeDashboardSubTab == 0) ButtonDefaults.buttonElevation(defaultElevation = 3.dp) else null,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.defaultMinSize(minWidth = 125.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.School, contentDescription = "Performance", modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Performance", fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                    }
                                }

                                // 2. Attendance Checklist Tab
                                Button(
                                    onClick = { activeDashboardSubTab = 1 },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeDashboardSubTab == 1) Color.White else Color.Transparent,
                                        contentColor = if (activeDashboardSubTab == 1) Color(0xFF6D28D9) else Color(0xFF5138B8)
                                    ),
                                    elevation = if (activeDashboardSubTab == 1) ButtonDefaults.buttonElevation(defaultElevation = 3.dp) else null,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.defaultMinSize(minWidth = 125.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.FactCheck, contentDescription = "Attendance", modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Attendance", fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                    }
                                }

                                // 3. Syllabus/Alerts Tab
                                Button(
                                    onClick = { activeDashboardSubTab = 2 },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeDashboardSubTab == 2) Color.White else Color.Transparent,
                                        contentColor = if (activeDashboardSubTab == 2) Color(0xFF6D28D9) else Color(0xFF5138B8)
                                    ),
                                    elevation = if (activeDashboardSubTab == 2) ButtonDefaults.buttonElevation(defaultElevation = 3.dp) else null,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.defaultMinSize(minWidth = 125.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = "Syllabus portions", modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Syllabus Notes", fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            when (activeDashboardSubTab) {
                                0 -> {
                                    // SECTION 1: PERFORMANCE GRAPHS & DETAILED TEST RECORDS WITH TEST PAPERS
                                    val testPapersList = student.getTestPapers()
                                    MarksLineChart(subjects = subjects, testPapers = testPapersList)

                                    val allEvaluatedRecords = remember(subjects, testPapersList) {
                                        val sList = subjects
                                        val tpList = testPapersList.filter { it.marksObtained != null && it.totalMarks != null }
                                            .map { tp ->
                                                SubjectMark(
                                                    subjectName = tp.subjectName,
                                                    marks = tp.marksObtained!!,
                                                    maxMarks = tp.totalMarks!!,
                                                    testDate = tp.testDate,
                                                    testType = "Paper"
                                                )
                                            }
                                        (sList + tpList).distinctBy { "${it.subjectName}_${it.testDate}_${it.marks}_${it.maxMarks}" }
                                    }

                                    if (allEvaluatedRecords.isEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFECE6FF))
                                        ) {
                                            Text(
                                                "No test marks recorded for this student yet.",
                                                modifier = Modifier.padding(16.dp),
                                                color = Color.Gray,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    } else {
                                        val studentChosenSubjects = remember(subjects, allEvaluatedRecords) {
                                            (subjects.map { it.subjectName } + allEvaluatedRecords.map { it.subjectName }).filter { it.isNotBlank() }.distinct()
                                        }
                                        val expandedPerfSubjects = remember { mutableStateMapOf<String, Boolean>() }

                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Subject-wise Marks Breakdown :", fontWeight = FontWeight.Bold, color = Color(0xFF32236F), style = MaterialTheme.typography.bodyMedium)
                                                if (studentChosenSubjects.isNotEmpty()) {
                                                    Text("${studentChosenSubjects.size} Subjects", fontSize = 11.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Render individual dropdown for all subjects chosen by the student (Subject 1 dropdown, Subject 2 dropdown, etc.)
                                            studentChosenSubjects.forEachIndexed { index, subjName ->
                                                val isExpanded = expandedPerfSubjects[subjName] ?: true
                                                val subjRecords = allEvaluatedRecords.filter { it.subjectName.equals(subjName, ignoreCase = true) }
                                                val primaryRecord = subjRecords.firstOrNull() ?: subjects.firstOrNull { it.subjectName.equals(subjName, ignoreCase = true) }
                                                val marksVal = primaryRecord?.marks ?: 0.0
                                                val maxMarksVal = primaryRecord?.maxMarks ?: 100.0
                                                val pct = if (maxMarksVal > 0) ((marksVal / maxMarksVal) * 100).toInt() else 0
                                                val statusColor = if (pct >= 80) Color(0xFF10B981) else if (pct >= 60) Color(0xFF6366F1) else Color(0xFFF59E0B)

                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    border = BorderStroke(1.dp, if (isExpanded) Color(0xFFC7D2FE) else Color(0xFFEDE9FE)),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Column(modifier = Modifier.fillMaxWidth()) {
                                                        // Dropdown Header Row
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(if (isExpanded) Color(0xFFF5F3FF) else Color.White)
                                                                .clickable { expandedPerfSubjects[subjName] = !isExpanded }
                                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(24.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color(0xFFEDE9FE)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9))
                                                                }
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Column {
                                                                    Text(
                                                                        text = "Subject ${index + 1}: $subjName",
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF1E1B4B),
                                                                        fontSize = 14.sp
                                                                    )
                                                                    Text(
                                                                        text = if (isExpanded) "Tap to collapse" else "Tap to view subject marks and evaluation history",
                                                                        fontSize = 10.sp,
                                                                        color = Color.Gray
                                                                    )
                                                                }
                                                            }
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = statusColor.copy(alpha = 0.12f),
                                                                    modifier = Modifier.padding(end = 6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "${marksVal.toInt()}/${maxMarksVal.toInt()} ($pct%)",
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = statusColor,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                                    )
                                                                }
                                                                Icon(
                                                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                                    contentDescription = "Toggle Dropdown",
                                                                    tint = Color(0xFF6D28D9)
                                                                )
                                                            }
                                                        }

                                                        // Dropdown Body Content (Subject Marks Visual & Breakdown)
                                                        if (isExpanded) {
                                                            Column(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(14.dp),
                                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Column {
                                                                        Text("Marks Obtained", fontSize = 11.sp, color = Color.Gray)
                                                                        Row(verticalAlignment = Alignment.Bottom) {
                                                                            Text(
                                                                                text = "${marksVal.toInt()}",
                                                                                fontSize = 24.sp,
                                                                                fontWeight = FontWeight.Black,
                                                                                color = Color(0xFF1E1B4B)
                                                                            )
                                                                            Text(
                                                                                text = " / ${maxMarksVal.toInt()}",
                                                                                fontSize = 14.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = Color.Gray,
                                                                                modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                                                                            )
                                                                        }
                                                                    }

                                                                    Column(horizontalAlignment = Alignment.End) {
                                                                        val gradeTag = if (pct >= 90) "A+ Outstanding" else if (pct >= 75) "A Excellent" else if (pct >= 60) "B Good" else "Needs Attention"
                                                                        Surface(
                                                                            shape = RoundedCornerShape(8.dp),
                                                                            color = statusColor.copy(alpha = 0.15f)
                                                                        ) {
                                                                            Text(
                                                                                text = gradeTag,
                                                                                fontSize = 11.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = statusColor,
                                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                            )
                                                                        }
                                                                        Text(
                                                                            text = "Accuracy: $pct%",
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.SemiBold,
                                                                            color = Color.Gray,
                                                                            modifier = Modifier.padding(top = 2.dp)
                                                                        )
                                                                    }
                                                                }

                                                                Row(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                                ) {
                                                                    Text(
                                                                        text = "Evaluation: ${primaryRecord?.testType ?: "Monthly Test"}",
                                                                        fontSize = 11.sp,
                                                                        color = Color(0xFF475569),
                                                                        fontWeight = FontWeight.Medium
                                                                    )
                                                                    Text(
                                                                        text = "Date: ${primaryRecord?.testDate?.ifEmpty { "15-07-2026" } ?: "15-07-2026"}",
                                                                        fontSize = 11.sp,
                                                                        color = Color(0xFF64748B)
                                                                    )
                                                                }

                                                                if (subjRecords.size > 1) {
                                                                    Text("Historical Records for this Subject:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                                                    subjRecords.forEach { rec ->
                                                                        Row(
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                                        ) {
                                                                            Text("${rec.testType} (${rec.testDate})", fontSize = 10.sp, color = Color(0xFF334155))
                                                                            Text("${rec.marks.toInt()}/${rec.maxMarks.toInt()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
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

                                1 -> {
                                    // SECTION 2: ATTENDANCE CHANNELS & CALENDAR WITH ATTENDANCE LOG
                                    AttendanceDonutChart(attendancePct, presentDays, daysInMonth)

                                    ParentAttendanceCalendar(attendance = attendance)

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFECE6FF))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Attendance History Log:",
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF32236F),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))

                                            val sortedEntries = attendance.entries.sortedByDescending { it.key }
                                            if (sortedEntries.isEmpty()) {
                                                Text(
                                                    "No attendance marked yet. Mark attendance from the Daily Attendance Sheet.",
                                                    color = Color.Gray,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                )
                                            } else {
                                                sortedEntries.take(15).forEach { (dateStr, isPresent) ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(dateStr, fontWeight = FontWeight.Medium)

                                                        val statusText = if (isPresent) "Present" else "Absent"
                                                        val containerColor = if (isPresent) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                                        val contentColor = if (isPresent) Color(0xFF2E7D32) else Color(0xFFC62828)

                                                        Box(
                                                            modifier = Modifier
                                                                .background(containerColor, shape = RoundedCornerShape(8.dp))
                                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = statusText,
                                                                color = contentColor,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    // SECTION 3: SYLLABUS NOTES PORTIONS (By Tutor)
                                    val syllabusItems = student.getSyllabusItems()
                                    val studentSubjects = student.getSubjects().map { it.subjectName }.distinct()

                                    // Default the subject input to the first enrolled subject if empty
                                    if (newSyllabusSubjectInput.isEmpty() && studentSubjects.isNotEmpty()) {
                                        newSyllabusSubjectInput = studentSubjects.first()
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0xFFEFE8FF)),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F7FF))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF7A68E6))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Syllabus Notes Portions",
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF32236F),
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                }

                                                Button(
                                                    onClick = { isEditingSyllabus = !isEditingSyllabus },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isEditingSyllabus) Color(0xFF10B981) else Color(0xFF6D28D9)
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isEditingSyllabus) Icons.Filled.CheckCircle else Icons.Filled.Edit,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isEditingSyllabus) "View Checklist" else "Edit Syllabus",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Text(
                                                text = if (isEditingSyllabus) "Edit Mode: Add/Delete topics subject-wise" else "View Mode: Ticking completed topics (Portions are grouped subject-wise)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )

                                            if (isEditingSyllabus) {
                                                // Add Syllabus Topic Form
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Text("Add New Portion Topic", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color(0xFF4F46E5))

                                                        if (studentSubjects.isEmpty()) {
                                                            Text(
                                                                text = "This student is not enrolled in any subjects. Please enroll them in subjects under the 'Edit Student' tab first.",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color.Red,
                                                                modifier = Modifier.padding(vertical = 4.dp)
                                                            )
                                                        } else {
                                                            Text(
                                                                text = "Selected Subject: $newSyllabusSubjectInput",
                                                                fontWeight = FontWeight.SemiBold,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color(0xFF6D28D9)
                                                            )

                                                            // Quick subject chips of selected subjects only
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .horizontalScroll(rememberScrollState())
                                                            ) {
                                                                studentSubjects.forEach { subj ->
                                                                    val isSelected = newSyllabusSubjectInput == subj
                                                                    AssistChip(
                                                                        onClick = { newSyllabusSubjectInput = subj },
                                                                        label = { Text(subj, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                                                        colors = AssistChipDefaults.assistChipColors(
                                                                            containerColor = if (isSelected) Color(0xFFEEEBFF) else Color(0xFFF1F5F9),
                                                                            labelColor = if (isSelected) Color(0xFF6D28D9) else Color(0xFF475569)
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        // Topic input
                                                        androidx.compose.material3.OutlinedTextField(
                                                            value = newSyllabusTopicInput,
                                                            onValueChange = { newSyllabusTopicInput = it },
                                                            label = { Text("Topic Portion Description") },
                                                            placeholder = { Text("e.g. Coordinate Geometry") },
                                                            singleLine = true,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )

                                                        Button(
                                                            onClick = {
                                                                val subject = newSyllabusSubjectInput.trim()
                                                                val topic = newSyllabusTopicInput.trim()
                                                                if (subject.isEmpty()) {
                                                                    Toast.makeText(context, "Please select an enrolled subject first", Toast.LENGTH_SHORT).show()
                                                                    return@Button
                                                                }
                                                                if (topic.isEmpty()) {
                                                                    Toast.makeText(context, "Topic description cannot be empty", Toast.LENGTH_SHORT).show()
                                                                    return@Button
                                                                }

                                                                val finalSubject = subject
                                                                val newItem = SyllabusItem(
                                                                    id = "item_${System.currentTimeMillis()}",
                                                                    subjectName = finalSubject,
                                                                    topic = topic,
                                                                    isCompleted = false
                                                                )
                                                                val updatedItems = syllabusItems + newItem
                                                                viewModel.saveSyllabus(student.slNo, student.withSyllabusItems(updatedItems).syllabus)

                                                                newSyllabusTopicInput = ""
                                                                Toast.makeText(context, "Syllabus topic added successfully!", Toast.LENGTH_SHORT).show()
                                                            },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text("Add Portion Topic")
                                                        }
                                                    }
                                                }
                                            }

                                            // Render Syllabus topics subject-wise
                                            if (syllabusItems.isEmpty()) {
                                                Text(
                                                    text = "No syllabus portions set up yet for this student. Click 'Edit Syllabus' to start adding topics.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(vertical = 12.dp)
                                                )
                                            } else {
                                                val groupedItems = syllabusItems.groupBy { it.subjectName }
                                                groupedItems.forEach { (subj, items) ->
                                                    val doneCount = items.count { it.isCompleted }
                                                    val remainingCount = items.size - doneCount
                                                    val progress = if (items.isNotEmpty()) doneCount.toFloat() / items.size else 0f

                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Icon(Icons.Filled.Bookmark, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(14.dp))
                                                                    Text(subj, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = Color(0xFF1E1B4B))
                                                                }

                                                                // Progress count
                                                                Text(
                                                                    text = "Done: $doneCount | Remaining: $remainingCount",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color.Gray,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }

                                                            // Progress Bar
                                                            LinearProgressIndicator(
                                                                progress = { progress },
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(4.dp)
                                                                    .clip(RoundedCornerShape(2.dp)),
                                                                color = Color(0xFF10B981),
                                                                trackColor = Color(0xFFF1F5F9)
                                                            )

                                                            Divider(color = Color(0xFFF1F5F9))

                                                            items.forEach { item ->
                                                                Row(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(vertical = 2.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    if (!isEditingSyllabus) {
                                                                        // VIEW MODE: Checklist with check/uncheck on tick
                                                                        Checkbox(
                                                                            checked = item.isCompleted,
                                                                            onCheckedChange = { checked ->
                                                                                val updatedList = syllabusItems.map {
                                                                                    if (it.id == item.id) it.copy(isCompleted = checked) else it
                                                                                }
                                                                                viewModel.saveSyllabus(student.slNo, student.withSyllabusItems(updatedList).syllabus)

                                                                                Toast.makeText(context, "Syllabus updated!", Toast.LENGTH_SHORT).show()
                                                                            },
                                                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                                                                        )
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                    } else {
                                                                        // EDIT MODE: Static icon
                                                                        Icon(
                                                                            imageVector = Icons.Filled.Book,
                                                                            contentDescription = null,
                                                                            tint = Color(0xFF94A3B8),
                                                                            modifier = Modifier.padding(6.dp).size(16.dp)
                                                                        )
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                    }

                                                                    Text(
                                                                        text = item.topic,
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        textDecoration = if (item.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                                                        color = if (item.isCompleted) Color.Gray else Color(0xFF1E293B),
                                                                        modifier = Modifier.weight(1f)
                                                                    )

                                                                    if (isEditingSyllabus) {
                                                                        // EDIT MODE: Delete Button
                                                                        IconButton(
                                                                            onClick = {
                                                                                val updatedList = syllabusItems.filter { it.id != item.id }
                                                                                viewModel.saveSyllabus(student.slNo, student.withSyllabusItems(updatedList).syllabus)

                                                                                Toast.makeText(context, "Syllabus topic removed", Toast.LENGTH_SHORT).show()
                                                                            },
                                                                            modifier = Modifier.size(36.dp)
                                                                        ) {
                                                                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
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
                            }

                            // FEES DUE lavender high-resolution card block
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF1FF)),
                                border = BorderStroke(1.dp, Color(0xFFD3E0FF))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Fees Due", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E3A8A))
                                        val actualDue = student.dueAmount.toInt()
                                        Text("₹$actualDue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF1E3A8A))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val feeSubtext = if (student.isFeesPaid || student.dueAmount <= 0.0) "All dues cleared!" else "Pending Outstanding Due"
                                    Text(feeSubtext, style = MaterialTheme.typography.labelSmall, color = if (student.isFeesPaid || student.dueAmount <= 0.0) Color(0xFF10B981) else Color(0xFF2563EB))
                                }
                            }

                            // Parent details contact buttons
                            val parentPhoneToContact = if (student.parentPhone.isNotEmpty()) student.parentPhone else "9876543210"
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$parentPhoneToContact"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f).testTag("call_button_${student.slNo}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call Parent")
                                }

                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$parentPhoneToContact")).apply {
                                            putExtra("sms_body", "Hello, regarding student ${student.name}.")
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f).testTag("message_button_${student.slNo}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Message")
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    1 -> {
                        // 2: TEST PAPER UPLOAD INTERACTIVE FORUM (TUTOR & PARENT SUPABASE CO-UPLOAD)
                        var expandSubjDropdownTab2 by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Co-Upload Testpaper Files (Supabase)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF32236F))
                            Text("Tutors and Parents can register high-quality copies of worksheets on Supabase bucket systems.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                            // 1. Subject Dropdown with options of selected subjects
                            Text("Select Subject:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandSubjDropdownTab2 = !expandSubjDropdownTab2 },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                                ) {
                                    Text(attachmentSubjectText.ifEmpty { "Select Enrolled Subject..." })
                                }
                                DropdownMenu(
                                    expanded = expandSubjDropdownTab2,
                                    onDismissRequest = { expandSubjDropdownTab2 = false }
                                ) {
                                    if (subjects.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No Enrolled Subjects (Use Edit Student tab)", color = Color.Red) },
                                            onClick = { expandSubjDropdownTab2 = false }
                                        )
                                    } else {
                                        subjects.map { it.subjectName }.distinct().forEach { subName ->
                                            DropdownMenuItem(
                                                text = { Text(subName) },
                                                onClick = {
                                                    attachmentSubjectText = subName
                                                    expandSubjDropdownTab2 = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Marks Obtained, 3. Over total marks
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.material3.OutlinedTextField(
                                    value = attachmentMarksText,
                                    onValueChange = { attachmentMarksText = it },
                                    label = { Text("Marks Obtained") },
                                    placeholder = { Text("e.g. 90") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                androidx.compose.material3.OutlinedTextField(
                                    value = attachmentMaxMarksText,
                                    onValueChange = { attachmentMaxMarksText = it },
                                    label = { Text("Total Max Marks") },
                                    placeholder = { Text("e.g. 100") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            // 4. Upload test paper has 2 options upload and capture
                            Text("Upload Test Paper Image:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onTutorLaunchCamera() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Capture Photo")
                                }

                                Button(
                                    onClick = { tutorFilePickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Upload File")
                                }
                            }

                            // Preview Card for selected/captured image
                            if (tutorCapturedBitmap != null || tutorSelectedImageUri != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp),
                                    border = BorderStroke(1.dp, Color(0xFF10B981))
                                ) {
                                    if (tutorCapturedBitmap != null) {
                                        AsyncImage(
                                            model = tutorCapturedBitmap,
                                            contentDescription = "Captured Paper Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (tutorSelectedImageUri != null) {
                                        AsyncImage(
                                            model = tutorSelectedImageUri,
                                            contentDescription = "Selected Local Sheet Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .background(Color(0xFFF3F4F6), shape = RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFE5E7EB), shape = RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.ImageNotSupported, contentDescription = null, tint = Color.Gray)
                                        Text("No direct photo loaded yet.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (attachmentSubjectText.trim().isEmpty() || attachedPhotoUri == null) {
                                        Toast.makeText(context, "Please select subject and attach image first!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val scoreVal = attachmentMarksText.toDoubleOrNull()
                                    val scoreMax = attachmentMaxMarksText.toDoubleOrNull()

                                    if (scoreVal == null || attachmentMarksText.trim().isEmpty()) {
                                        Toast.makeText(context, "Marks obtained is compulsory for uploading test paper!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (scoreMax == null || scoreMax <= 0) {
                                        Toast.makeText(context, "Total Max Marks is compulsory and must be greater than zero!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (scoreVal < 0) {
                                        Toast.makeText(context, "Marks obtained cannot be negative!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (scoreVal > scoreMax) {
                                        Toast.makeText(context, "Marks obtained ($scoreVal) cannot be greater than Total Max Marks ($scoreMax)!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val savedPath = if (tutorCapturedBitmap != null) {
                                        viewModel.saveCapturedBitmap(tutorCapturedBitmap!!)
                                    } else {
                                        tutorSelectedImageUri?.let { viewModel.saveSelectedUri(it) }
                                    }

                                    if (savedPath == null) {
                                        Toast.makeText(context, "Error saving test paper image locally.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val finalUri = savedPath
                                    val subjectUploaded = attachmentSubjectText.trim()

                                    // Submit on active student registry
                                    viewModel.uploadTestPaper(student.slNo, subjectUploaded, scoreVal, scoreMax, finalUri)
                                    viewModel.addTestScore(student.slNo, subjectUploaded, scoreVal, scoreMax, "Co-Upload")

                                    // Send SMS notification to parent
                                    viewModel.sendTestPaperUploadedSms(context, student.parentPhone, subjectUploaded)

                                    Toast.makeText(context, "Test paper uploaded & SMS notification sent to parent!", Toast.LENGTH_SHORT).show()
                                    attachmentSubjectText = ""
                                    attachmentMarksText = ""
                                    attachedPhotoUri = null
                                    tutorCapturedBitmap = null
                                    tutorSelectedImageUri = null
                                    testPaperPhotoUri = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                            ) {
                                Text("Upload File to Supabase")
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            Text("Uploaded marksheets history:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            val papers = student.getTestPapers()
                            // Group papers by subject, sorted by date latest first
                            val groupedPapers = papers.sortedByDescending { it.testDate }.groupBy { it.subjectName }

                            if (groupedPapers.isEmpty()) {
                                Text("No uploaded test papers found yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            } else {
                                groupedPapers.forEach { (subName, paperList) ->
                                    Surface(
                                        color = Color(0xFFFBFBFE),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .border(1.dp, Color(0xFFEADBFC), RoundedCornerShape(10.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.MenuBook,
                                                    contentDescription = null,
                                                    tint = Color(0xFF7A68E6),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = subName.uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF7A68E6)
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                    text = "${paperList.size} Submission(s)",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            paperList.forEach { paper ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    border = BorderStroke(1.dp, Color(0xFFECEBFF)),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Entry",
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF32236F),
                                                                style = MaterialTheme.typography.titleSmall
                                                            )
                                                            val marksText = if (paper.marksObtained != null && paper.totalMarks != null) {
                                                                val pct = if (paper.totalMarks > 0) (paper.marksObtained / paper.totalMarks) * 100 else 0.0
                                                                val pctStr = String.format(java.util.Locale.US, "%.1f", pct)
                                                                val obtainedStr = if (paper.marksObtained % 1.0 == 0.0) paper.marksObtained.toInt().toString() else paper.marksObtained.toString()
                                                                val totalStr = if (paper.totalMarks % 1.0 == 0.0) paper.totalMarks.toInt().toString() else paper.totalMarks.toString()
                                                                "$obtainedStr/$totalStr ($pctStr%)"
                                                            } else {
                                                                "N/A Marks"
                                                            }
                                                            Badge(
                                                                containerColor = Color(0xFF6D28D9),
                                                                contentColor = Color.White
                                                            ) {
                                                                Text(
                                                                    text = marksText,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "Date: ${paper.testDate}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.Gray
                                                        )

                                                        Spacer(modifier = Modifier.height(10.dp))

                                                        // Display original uploaded image elegantly
                                                        val displayPhoto = if (paper.parentPhotoUri?.startsWith("uri://") == true || paper.parentPhotoUri == null) {
                                                            // Simulated/placeholder graphic for offline mock tests
                                                            "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=500&auto=format&fit=crop"
                                                        } else {
                                                            paper.parentPhotoUri
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(180.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color(0xFFEFE8FF)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            AsyncImage(
                                                                model = displayPhoto,
                                                                contentDescription = "Test Paper Sheet",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        if (paper.tutorPhotoUri == null) {
                                                            Button(
                                                                onClick = {
                                                                    viewModel.tutorUpdateTestPaper(
                                                                        student.slNo,
                                                                        paper.id,
                                                                        "https://supabase-bucket/tutor_verified_${System.currentTimeMillis()}.jpg"
                                                                    )
                                                                    viewModel.sendTestPaperVerifiedSms(
                                                                        context = context,
                                                                        parentPhone = student.parentPhone,
                                                                        studentName = student.name,
                                                                        subject = paper.subjectName,
                                                                        marksObtained = paper.marksObtained,
                                                                        totalMarks = paper.totalMarks
                                                                    )
                                                                    Toast.makeText(context, "Test paper verified & SMS sent to parent!", Toast.LENGTH_SHORT).show()
                                                                },
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = Color(0xFFF3EDFF),
                                                                    contentColor = Color(0xFF6D28D9)
                                                                ),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("Tutor Co-Sign Co-Upload & Verify")
                                                            }
                                                        } else {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                                                Text(
                                                                    text = "Tutor Co-Sign Approved!",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color(0xFF10B981),
                                                                    fontWeight = FontWeight.Bold
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

                    2 -> {
                        // 2: EDIT STUDENT PROFILE PARAMETERS & SUBJECT LEDGER

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Enrolled subjects editor card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFE)),
                                border = BorderStroke(1.dp, Color(0xFFEADBFC)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Edit, contentDescription = null, tint = Color(0xFF7A68E6), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Enrolled Subjects & Marks Ledger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFF1E1A2C))
                                        }
                                        if (editSubjectsList.isNotEmpty()) {
                                            Text("${editSubjectsList.size} Subjects", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                                        }
                                    }

                                    Text("Add, delete, or quick-edit student subjects here. Note: Changes are persistent only on saving below.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                                    val expandedLedgerSubjects = remember { mutableStateMapOf<String, Boolean>() }

                                    if (editSubjectsList.isEmpty()) {
                                        Text("No subjects registered for this student yet.", style = MaterialTheme.typography.bodySmall, color = Color.Red, modifier = Modifier.padding(vertical = 4.dp))
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Subject-wise Marks Ledger :", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF32236F))
                                                Text("${editSubjectsList.size} Subjects", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                                            }

                                            // Render individual dropdown for all subjects chosen by the student (Subject 1 dropdown, Subject 2 dropdown, etc.)
                                            editSubjectsList.forEachIndexed { index, subItem ->
                                                val isSubjExpanded = expandedLedgerSubjects[subItem.subjectName] ?: true
                                                val pct = if (subItem.maxMarks > 0) ((subItem.marks / subItem.maxMarks) * 100).toInt() else 0
                                                val statusColor = if (pct >= 80) Color(0xFF10B981) else if (pct >= 60) Color(0xFF6366F1) else Color(0xFFF59E0B)

                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    border = BorderStroke(1.dp, if (isSubjExpanded) Color(0xFFC7D2FE) else Color(0xFFECEBFF)),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Column(modifier = Modifier.fillMaxWidth()) {
                                                        // Subject Dropdown Header Row
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(if (isSubjExpanded) Color(0xFFF5F3FF) else Color.White)
                                                                .clickable { expandedLedgerSubjects[subItem.subjectName] = !isSubjExpanded }
                                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(22.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color(0xFFEEF2FF)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                                                                }
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Column {
                                                                    Text(
                                                                        text = "Subject ${index + 1}: ${subItem.subjectName}",
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 13.sp,
                                                                        color = Color(0xFF1E1A2C)
                                                                    )
                                                                    Text(
                                                                        text = if (isSubjExpanded) "Tap to collapse dropdown" else "Tap to expand & edit marks",
                                                                        fontSize = 9.sp,
                                                                        color = Color.Gray
                                                                    )
                                                                }
                                                            }
                                                            Icon(
                                                                imageVector = if (isSubjExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                                contentDescription = "Toggle Subject Dropdown",
                                                                tint = Color(0xFF4F46E5),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }

                                                        // Subject Dropdown Body
                                                        if (isSubjExpanded) {
                                                            Column(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(12.dp),
                                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                // Marks Edit Fields
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    var quickMarksInput by remember(subItem.subjectName, subItem.marks) { mutableStateOf(if (subItem.marks % 1.0 == 0.0) subItem.marks.toInt().toString() else subItem.marks.toString()) }
                                                                    var quickMaxMarksInput by remember(subItem.subjectName, subItem.maxMarks) { mutableStateOf(if (subItem.maxMarks % 1.0 == 0.0) subItem.maxMarks.toInt().toString() else subItem.maxMarks.toString()) }

                                                                    androidx.compose.material3.OutlinedTextField(
                                                                        value = quickMarksInput,
                                                                        onValueChange = { newValue ->
                                                                            quickMarksInput = newValue
                                                                            val cleanNum = newValue.toDoubleOrNull() ?: 0.0
                                                                            editSubjectsList = editSubjectsList.map { item ->
                                                                                if (item.subjectName == subItem.subjectName) item.copy(marks = cleanNum) else item
                                                                            }
                                                                        },
                                                                        label = { Text("Score", fontSize = 9.sp) },
                                                                        modifier = Modifier.weight(1f),
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        singleLine = true,
                                                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                                    )

                                                                    androidx.compose.material3.OutlinedTextField(
                                                                        value = quickMaxMarksInput,
                                                                        onValueChange = { newValue ->
                                                                            quickMaxMarksInput = newValue
                                                                            val cleanNum = newValue.toDoubleOrNull() ?: 100.0
                                                                            editSubjectsList = editSubjectsList.map { item ->
                                                                                if (item.subjectName == subItem.subjectName) item.copy(maxMarks = cleanNum) else item
                                                                            }
                                                                        },
                                                                        label = { Text("Max", fontSize = 9.sp) },
                                                                        modifier = Modifier.weight(1f),
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        singleLine = true,
                                                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.Gray)
                                                                    )

                                                                    IconButton(
                                                                        onClick = {
                                                                            editSubjectsList = editSubjectsList.filter { it.subjectName != subItem.subjectName }
                                                                        },
                                                                        modifier = Modifier.size(36.dp)
                                                                    ) {
                                                                        Icon(Icons.Filled.Delete, contentDescription = "Remove Subject", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Divider(color = Color(0xFFEADBFC))
                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text("Add New Subject Registration:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        var showSubjectMenu by remember { mutableStateOf(false) }
                                        val commonSubjects = tutorCustomSubjectsList

                                        Box(modifier = Modifier.weight(1f)) {
                                            androidx.compose.material3.OutlinedTextField(
                                                value = newSubjectName,
                                                onValueChange = { /* read only */ },
                                                readOnly = true,
                                                placeholder = { Text("Select subject...") },
                                                modifier = Modifier.fillMaxWidth().clickable { showSubjectMenu = !showSubjectMenu },
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp),
                                                trailingIcon = {
                                                    IconButton(onClick = { showSubjectMenu = !showSubjectMenu }) {
                                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                                    }
                                                }
                                            )

                                            DropdownMenu(
                                                expanded = showSubjectMenu,
                                                onDismissRequest = { showSubjectMenu = false }
                                            ) {
                                                commonSubjects.forEach { subj ->
                                                    DropdownMenuItem(
                                                        text = { Text(subj) },
                                                        onClick = {
                                                            newSubjectName = subj
                                                            showSubjectMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val cleanName = newSubjectName.trim()
                                                if (cleanName.isNotEmpty()) {
                                                    // 1. Save in tutor custom dropdown registry if new
                                                    val containsOption = tutorCustomSubjectsList.any { it.equals(cleanName, ignoreCase = true) }
                                                    if (!containsOption) {
                                                        tutorCustomSubjectsList.add(cleanName)
                                                    }
                                                    // 2. Add to student's enrolled subjects list if new
                                                    if (!editSubjectsList.any { it.subjectName.equals(cleanName, ignoreCase = true) }) {
                                                        editSubjectsList = editSubjectsList + SubjectMark(
                                                            subjectName = com.amarmohan.domain.model.ALLOWED_SUBJECTS.find { it.equals(cleanName, ignoreCase = true) } ?: cleanName,
                                                            marks = 0.0,
                                                            maxMarks = 100.0,
                                                            testDate = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                                                        )
                                                    }
                                                    newSubjectName = ""
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                        ) {
                                            Text("Add", color = Color.White)
                                        }
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            Text("Core Student Registry Parameters (* All Compulsory):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF32236F))

                            val localFocusManager = androidx.compose.ui.platform.LocalFocusManager.current

                            androidx.compose.material3.OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Student Full Name * (Compulsory)") },
                                modifier = Modifier.fillMaxWidth().moveFocusOnEnter(localFocusManager, androidx.compose.ui.focus.FocusDirection.Next),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { localFocusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) })
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = editParentName,
                                onValueChange = { editParentName = it },
                                label = { Text("Parent Full Name * (Compulsory)") },
                                modifier = Modifier.fillMaxWidth().moveFocusOnEnter(localFocusManager, androidx.compose.ui.focus.FocusDirection.Next),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { localFocusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) })
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Parent Mobile Phone * (Compulsory)") },
                                modifier = Modifier.fillMaxWidth().moveFocusOnEnter(localFocusManager, androidx.compose.ui.focus.FocusDirection.Next),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { localFocusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) })
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = editFlatNo,
                                onValueChange = { editFlatNo = it },
                                label = { Text("Parent Flat Number * (Compulsory)") },
                                modifier = Modifier.fillMaxWidth().moveFocusOnEnter(localFocusManager, androidx.compose.ui.focus.FocusDirection.Next),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { localFocusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) })
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = editBirthDate,
                                onValueChange = { editBirthDate = it },
                                label = { Text("Date of Birth * (DD-MM-YYYY) (Compulsory)") },
                                placeholder = { Text("e.g. 15-08-2012") },
                                modifier = Modifier.fillMaxWidth().moveFocusOnEnter(localFocusManager, androidx.compose.ui.focus.FocusDirection.Next),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { localFocusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) })
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = editClass,
                                onValueChange = { editClass = it },
                                label = { Text("Standard Class * (Compulsory)") },
                                modifier = Modifier.fillMaxWidth().moveFocusOnEnter(localFocusManager, androidx.compose.ui.focus.FocusDirection.Next),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { localFocusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) })
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = editSchool,
                                onValueChange = { editSchool = it },
                                label = { Text("School Name * (Compulsory)") },
                                modifier = Modifier.fillMaxWidth().moveFocusOnEnter(localFocusManager, androidx.compose.ui.focus.FocusDirection.Next),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { localFocusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) })
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = editBoard,
                                onValueChange = { editBoard = it },
                                label = { Text("Secondary Board * (Compulsory)") },
                                modifier = Modifier.fillMaxWidth().moveFocusOnEnter(localFocusManager, androidx.compose.ui.focus.FocusDirection.Next),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { localFocusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) })
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = editEmail,
                                onValueChange = { editEmail = it },
                                label = { Text("Email Address") },
                                modifier = Modifier.fillMaxWidth().moveFocusOnEnter(localFocusManager, null),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { localFocusManager.clearFocus() })
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (editName.trim().isEmpty()) {
                                            Toast.makeText(context, "Student Full Name is compulsory", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (editParentName.trim().isEmpty()) {
                                            Toast.makeText(context, "Parent Full Name is compulsory", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (editPhone.trim().isEmpty() || editPhone.trim().length < 10) {
                                            Toast.makeText(context, "Valid 10-digit Parent Phone is compulsory", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (editFlatNo.trim().isEmpty()) {
                                            Toast.makeText(context, "Parent Flat Number is compulsory", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (editBirthDate.trim().isEmpty()) {
                                            Toast.makeText(context, "Student Date of Birth is compulsory", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (editClass.trim().isEmpty()) {
                                            Toast.makeText(context, "Standard Class is compulsory", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (editSchool.trim().isEmpty()) {
                                            Toast.makeText(context, "School Name is compulsory", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (editBoard.trim().isEmpty()) {
                                            Toast.makeText(context, "Secondary Board is compulsory", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (editSubjectsList.isEmpty()) {
                                            Toast.makeText(context, "At least one enrolled subject is compulsory", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        viewModel.addOrUpdateStudentDetails(
                                            slNo = student.slNo,
                                            name = editName.trim(),
                                            parentName = editParentName.trim(),
                                            phone = editPhone.trim(),
                                            school = editSchool.trim(),
                                            board = editBoard.trim(),
                                            standard = editClass.trim(),
                                            email = editEmail.trim(),
                                            subjects = editSubjectsList,
                                            syllabus = syllabusText.trim(),
                                            tuitionFee = student.tuitionFee,
                                            isPaid = student.isFeesPaid,
                                            demoFee = student.demoFee,
                                            parentFlatNo = editFlatNo.trim(),
                                            birthDate = editBirthDate.trim()
                                        )
                                        Toast.makeText(context, "Student profile parameters saved on Cloud!", Toast.LENGTH_SHORT).show()
                                        activeSubViewTab = 0
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                                ) {
                                    Text("Save Registry Changes")
                                }

                                OutlinedButton(
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                    onClick = {
                                        viewModel.deleteStudent(student.slNo)
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Delete Student")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-PANEL: ADD STUDENT REGISTRATIONS MODAL
// ==========================================
@Composable
fun AddStudentDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onEnrollSuccess: (com.amarmohan.domain.model.Student) -> Unit = {}
) {
    var slNo by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var parentName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var parentFlatNo by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var standard by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var board by remember { mutableStateOf("CBSE") }
    var tuitionFeeText by remember { mutableStateOf("") }
    var selectedSubjects by remember { mutableStateOf(emptySet<String>()) }
    var customSubjectsList by remember { mutableStateOf(emptySet<String>()) }
    var newSubjectInput by remember { mutableStateOf("") }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enroll New Student", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Set up standard authorization details. All fields are compulsory.")

                androidx.compose.material3.OutlinedTextField(
                    value = slNo,
                    onValueChange = { slNo = it },
                    placeholder = { Text("SL Register No * (e.g. 103)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_stu_sl").moveFocusOnEnter(focusManager, androidx.compose.ui.focus.FocusDirection.Next),
                    shape = RoundedCornerShape(10.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Student Full Name *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_stu_name").moveFocusOnEnter(focusManager, androidx.compose.ui.focus.FocusDirection.Next),
                    shape = RoundedCornerShape(10.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = parentName,
                    onValueChange = { parentName = it },
                    placeholder = { Text("Parent Full Name *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_stu_parent_name").moveFocusOnEnter(focusManager, androidx.compose.ui.focus.FocusDirection.Next),
                    shape = RoundedCornerShape(10.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = { Text("Parent Mobile Phone *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_stu_phone").moveFocusOnEnter(focusManager, androidx.compose.ui.focus.FocusDirection.Next),
                    shape = RoundedCornerShape(10.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = parentFlatNo,
                    onValueChange = { parentFlatNo = it },
                    placeholder = { Text("Parent Flat Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_stu_flat").moveFocusOnEnter(focusManager, androidx.compose.ui.focus.FocusDirection.Next),
                    shape = RoundedCornerShape(10.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    placeholder = { Text("Date of Birth * (DD-MM-YYYY)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_stu_dob").moveFocusOnEnter(focusManager, androidx.compose.ui.focus.FocusDirection.Next),
                    shape = RoundedCornerShape(10.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = standard,
                    onValueChange = { standard = it },
                    placeholder = { Text("Standard Grade Class *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().moveFocusOnEnter(focusManager, androidx.compose.ui.focus.FocusDirection.Next),
                    shape = RoundedCornerShape(10.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = school,
                    onValueChange = { school = it },
                    placeholder = { Text("School Name *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().moveFocusOnEnter(focusManager, androidx.compose.ui.focus.FocusDirection.Next),
                    shape = RoundedCornerShape(10.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = board,
                    onValueChange = { board = it },
                    placeholder = { Text("Board * (e.g. CBSE / ICSE)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().moveFocusOnEnter(focusManager, androidx.compose.ui.focus.FocusDirection.Next),
                    shape = RoundedCornerShape(10.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = tuitionFeeText,
                    onValueChange = { tuitionFeeText = it },
                    placeholder = { Text("Tuition Fee * (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().moveFocusOnEnter(focusManager, androidx.compose.ui.focus.FocusDirection.Next),
                    shape = RoundedCornerShape(10.dp)
                )

                androidx.compose.material3.OutlinedTextField(
                    value = newSubjectInput,
                    onValueChange = { newSubjectInput = it },
                    placeholder = { Text("Add Subject (Press '+' button)") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val trimmed = newSubjectInput.trim()
                            if (trimmed.isNotEmpty()) {
                                customSubjectsList = customSubjectsList + trimmed
                                selectedSubjects = selectedSubjects + trimmed
                                newSubjectInput = ""
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Custom Subject")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().moveFocusOnEnter(focusManager, null),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text("Select Enrolled Subjects *:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                val combinedSubjects = (com.amarmohan.domain.model.ALLOWED_SUBJECTS + customSubjectsList).distinct()
                combinedSubjects.chunked(3).forEach { rowSubjects ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSubjects.forEach { subj ->
                            val isSelected = selectedSubjects.contains(subj)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE2D6FF) else Color(0xFFF3F4F6))
                                    .clickable {
                                        selectedSubjects = if (isSelected) {
                                            selectedSubjects - subj
                                        } else {
                                            selectedSubjects + subj
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = subj,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color(0xFF5138B8) else Color.DarkGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        repeat(3 - rowSubjects.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (slNo.trim().isEmpty()) {
                        Toast.makeText(context, "SL Register No is compulsory", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (name.trim().isEmpty()) {
                        Toast.makeText(context, "Student Full Name is compulsory", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (parentName.trim().isEmpty()) {
                        Toast.makeText(context, "Parent Full Name is compulsory", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (phone.trim().isEmpty() || phone.trim().length < 10) {
                        Toast.makeText(context, "Valid 10-digit Parent Phone is compulsory", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (parentFlatNo.trim().isEmpty()) {
                        Toast.makeText(context, "Parent Flat Number is compulsory", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (birthDate.trim().isEmpty()) {
                        Toast.makeText(context, "Student Date of Birth is compulsory", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (standard.trim().isEmpty()) {
                        Toast.makeText(context, "Standard Class is compulsory", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (school.trim().isEmpty()) {
                        Toast.makeText(context, "School Name is compulsory", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (board.trim().isEmpty()) {
                        Toast.makeText(context, "Secondary Board is compulsory", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val passFee = tuitionFeeText.toDoubleOrNull()
                    if (passFee == null || tuitionFeeText.trim().isEmpty()) {
                        Toast.makeText(context, "Tuition Fee is compulsory", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedSubjects.isEmpty()) {
                        Toast.makeText(context, "At least one enrolled subject is compulsory", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val initialSubjectsList = selectedSubjects.map { subjName ->
                        com.amarmohan.domain.model.SubjectMark(
                            subjectName = subjName,
                            marks = 0.0,
                            maxMarks = 100.0,
                            testDate = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                        )
                    }
                    val newStu = com.amarmohan.domain.model.Student(
                        slNo = slNo.trim(),
                        name = name.trim(),
                        parentName = parentName.trim(),
                        parentPhone = phone.trim(),
                        parentFlatNo = parentFlatNo.trim(),
                        birthDate = birthDate.trim(),
                        school = school.trim(),
                        board = board.trim(),
                        standard = standard.trim(),
                        tuitionFee = passFee,
                        isFeesPaid = false,
                        dueAmount = passFee,
                        subjectsJson = com.amarmohan.domain.model.JsonUtils.listToJson(initialSubjectsList, com.amarmohan.domain.model.SubjectMark::class.java),
                        syllabus = ""
                    )
                    viewModel.addOrUpdateStudentDetails(
                        slNo = slNo.trim(),
                        name = name.trim(),
                        parentName = parentName.trim(),
                        phone = phone.trim(),
                        school = school.trim(),
                        board = board.trim(),
                        standard = standard.trim(),
                        tuitionFee = passFee,
                        isPaid = false,
                        subjects = initialSubjectsList,
                        parentFlatNo = parentFlatNo.trim(),
                        birthDate = birthDate.trim()
                    )
                    viewModel.sendWelcomeSmsToParent(context, phone.trim(), name.trim(), parentName.trim())
                    onEnrollSuccess(newStu)
                    Toast.makeText(context, "Student Enrolled & Welcome SMS sent to parent!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag("confirm_add_stu_btn")
            ) {
                Text("Enroll Student")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorEnrollStudentScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEnrollSuccess: (com.amarmohan.domain.model.Student) -> Unit = {}
) {
    var slNo by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var parentName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var parentFlatNo by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var standard by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var board by remember { mutableStateOf("CBSE") }
    var tuitionFeeText by remember { mutableStateOf("") }
    var selectedSubjects by remember { mutableStateOf(emptySet<String>()) }
    var customSubjectsList by remember { mutableStateOf(emptySet<String>()) }
    var newSubjectInput by remember { mutableStateOf("") }

    var activeFieldIndex by remember { mutableStateOf(0) }

    val slNoFocusRequester = remember { FocusRequester() }
    val nameFocusRequester = remember { FocusRequester() }
    val parentNameFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val parentFlatNoFocusRequester = remember { FocusRequester() }
    val birthDateFocusRequester = remember { FocusRequester() }
    val standardFocusRequester = remember { FocusRequester() }
    val schoolFocusRequester = remember { FocusRequester() }
    val boardFocusRequester = remember { FocusRequester() }
    val tuitionFeeTextFocusRequester = remember { FocusRequester() }
    val customSubjectFocusRequester = remember { FocusRequester() }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val scrollHelper: suspend (Int) -> Unit = { index ->
        scrollState.animateScrollTo(with(density) { (index * 95).dp.roundToPx() })
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TutorTopBar(
                title = "Enroll New Student",
                onBack = onBack,
                viewModel = viewModel
            )
        },
        containerColor = Color(0xFFFBF9FF),
        bottomBar = {
            Surface(
                color = Color.White,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // "Next Field" Button
                        if (activeFieldIndex < 10) {
                            Button(
                                onClick = {
                                    val nextIndex = activeFieldIndex + 1
                                    when (nextIndex) {
                                        1 -> nameFocusRequester.requestFocus()
                                        2 -> parentNameFocusRequester.requestFocus()
                                        3 -> phoneFocusRequester.requestFocus()
                                        4 -> parentFlatNoFocusRequester.requestFocus()
                                        5 -> birthDateFocusRequester.requestFocus()
                                        6 -> standardFocusRequester.requestFocus()
                                        7 -> schoolFocusRequester.requestFocus()
                                        8 -> boardFocusRequester.requestFocus()
                                        9 -> tuitionFeeTextFocusRequester.requestFocus()
                                        10 -> customSubjectFocusRequester.requestFocus()
                                    }
                                    activeFieldIndex = nextIndex
                                    coroutineScope.launch {
                                        scrollHelper(nextIndex)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("enroll_next_field_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Next Field", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // "Enroll Student" Button
                        Button(
                            onClick = {
                                if (slNo.trim().isEmpty()) {
                                    Toast.makeText(context, "SL Register No is compulsory", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (name.trim().isEmpty()) {
                                    Toast.makeText(context, "Student Full Name is compulsory", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (parentName.trim().isEmpty()) {
                                    Toast.makeText(context, "Parent Full Name is compulsory", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (phone.trim().isEmpty() || phone.trim().length < 10) {
                                    Toast.makeText(context, "Valid 10-digit Parent Phone is compulsory", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (parentFlatNo.trim().isEmpty()) {
                                    Toast.makeText(context, "Parent Flat Number is compulsory", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (birthDate.trim().isEmpty()) {
                                    Toast.makeText(context, "Student Date of Birth is compulsory", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (standard.trim().isEmpty()) {
                                    Toast.makeText(context, "Standard Class is compulsory", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (school.trim().isEmpty()) {
                                    Toast.makeText(context, "School Name is compulsory", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (board.trim().isEmpty()) {
                                    Toast.makeText(context, "Secondary Board is compulsory", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val passFee = tuitionFeeText.toDoubleOrNull()
                                if (passFee == null || tuitionFeeText.trim().isEmpty()) {
                                    Toast.makeText(context, "Tuition Fee is compulsory", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedSubjects.isEmpty()) {
                                    Toast.makeText(context, "At least one enrolled subject is compulsory", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val initialSubjectsList = selectedSubjects.map { subjName ->
                                    com.amarmohan.domain.model.SubjectMark(
                                        subjectName = subjName,
                                        marks = 0.0,
                                        maxMarks = 100.0,
                                        testDate = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                                    )
                                }
                                val newStu = com.amarmohan.domain.model.Student(
                                    slNo = slNo.trim(),
                                    name = name.trim(),
                                    parentName = parentName.trim(),
                                    parentPhone = phone.trim(),
                                    parentFlatNo = parentFlatNo.trim(),
                                    birthDate = birthDate.trim(),
                                    school = school.trim(),
                                    board = board.trim(),
                                    standard = standard.trim(),
                                    tuitionFee = passFee,
                                    isFeesPaid = false,
                                    dueAmount = passFee,
                                    subjectsJson = com.amarmohan.domain.model.JsonUtils.listToJson(initialSubjectsList, com.amarmohan.domain.model.SubjectMark::class.java),
                                    syllabus = ""
                                )
                                viewModel.addOrUpdateStudentDetails(
                                    slNo = slNo.trim(),
                                    name = name.trim(),
                                    parentName = parentName.trim(),
                                    phone = phone.trim(),
                                    school = school.trim(),
                                    board = board.trim(),
                                    standard = standard.trim(),
                                    tuitionFee = passFee,
                                    isPaid = false,
                                    subjects = initialSubjectsList,
                                    parentFlatNo = parentFlatNo.trim(),
                                    birthDate = birthDate.trim()
                                )
                                viewModel.sendWelcomeSmsToParent(context, phone.trim(), name.trim(), parentName.trim())
                                onEnrollSuccess(newStu)
                                Toast.makeText(context, "Student Enrolled & Welcome SMS sent to parent!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("confirm_enroll_student_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enroll Student", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Set up student authorization details. All fields are compulsory.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    // 1. SL No
                    androidx.compose.material3.OutlinedTextField(
                        value = slNo,
                        onValueChange = { slNo = it },
                        label = { Text("SL Register No * (e.g. 103)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = {
                            nameFocusRequester.requestFocus()
                            activeFieldIndex = 1
                            coroutineScope.launch { scrollHelper(1) }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(slNoFocusRequester)
                            .onFocusChanged { if (it.isFocused) { activeFieldIndex = 0; coroutineScope.launch { scrollHelper(0) } } }
                            .testTag("add_stu_sl"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 2. Full Name
                    androidx.compose.material3.OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Student Full Name *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = {
                            parentNameFocusRequester.requestFocus()
                            activeFieldIndex = 2
                            coroutineScope.launch { scrollHelper(2) }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester)
                            .onFocusChanged { if (it.isFocused) { activeFieldIndex = 1; coroutineScope.launch { scrollHelper(1) } } }
                            .testTag("add_stu_name"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 3. Parent Full Name
                    androidx.compose.material3.OutlinedTextField(
                        value = parentName,
                        onValueChange = { parentName = it },
                        label = { Text("Parent Full Name *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = {
                            phoneFocusRequester.requestFocus()
                            activeFieldIndex = 3
                            coroutineScope.launch { scrollHelper(3) }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(parentNameFocusRequester)
                            .onFocusChanged { if (it.isFocused) { activeFieldIndex = 2; coroutineScope.launch { scrollHelper(2) } } }
                            .testTag("add_stu_parent_name"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 4. Parent Phone
                    androidx.compose.material3.OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Parent Mobile Phone *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = {
                            parentFlatNoFocusRequester.requestFocus()
                            activeFieldIndex = 4
                            coroutineScope.launch { scrollHelper(4) }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(phoneFocusRequester)
                            .onFocusChanged { if (it.isFocused) { activeFieldIndex = 3; coroutineScope.launch { scrollHelper(3) } } }
                            .testTag("add_stu_phone"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 5. Parent Flat No
                    androidx.compose.material3.OutlinedTextField(
                        value = parentFlatNo,
                        onValueChange = { parentFlatNo = it },
                        label = { Text("Parent Flat Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = {
                            birthDateFocusRequester.requestFocus()
                            activeFieldIndex = 5
                            coroutineScope.launch { scrollHelper(5) }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(parentFlatNoFocusRequester)
                            .onFocusChanged { if (it.isFocused) { activeFieldIndex = 4; coroutineScope.launch { scrollHelper(4) } } }
                            .testTag("add_stu_flat"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 6. Birth Date
                    androidx.compose.material3.OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        label = { Text("Date of Birth * (DD-MM-YYYY)") },
                        placeholder = { Text("e.g. 15-08-2012") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = {
                            standardFocusRequester.requestFocus()
                            activeFieldIndex = 6
                            coroutineScope.launch { scrollHelper(6) }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(birthDateFocusRequester)
                            .onFocusChanged { if (it.isFocused) { activeFieldIndex = 5; coroutineScope.launch { scrollHelper(5) } } }
                            .testTag("add_stu_dob"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 7. Standard Grade Class
                    androidx.compose.material3.OutlinedTextField(
                        value = standard,
                        onValueChange = { standard = it },
                        label = { Text("Standard Grade Class *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = {
                            schoolFocusRequester.requestFocus()
                            activeFieldIndex = 7
                            coroutineScope.launch { scrollHelper(7) }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(standardFocusRequester)
                            .onFocusChanged { if (it.isFocused) { activeFieldIndex = 6; coroutineScope.launch { scrollHelper(6) } } },
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 8. School Name
                    androidx.compose.material3.OutlinedTextField(
                        value = school,
                        onValueChange = { school = it },
                        label = { Text("School Name *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = {
                            boardFocusRequester.requestFocus()
                            activeFieldIndex = 8
                            coroutineScope.launch { scrollHelper(8) }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(schoolFocusRequester)
                            .onFocusChanged { if (it.isFocused) { activeFieldIndex = 7; coroutineScope.launch { scrollHelper(7) } } },
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 9. Board (CBSE / ICSE)
                    androidx.compose.material3.OutlinedTextField(
                        value = board,
                        onValueChange = { board = it },
                        label = { Text("Board * (e.g. CBSE / ICSE)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = {
                            tuitionFeeTextFocusRequester.requestFocus()
                            activeFieldIndex = 9
                            coroutineScope.launch { scrollHelper(9) }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(boardFocusRequester)
                            .onFocusChanged { if (it.isFocused) { activeFieldIndex = 8; coroutineScope.launch { scrollHelper(8) } } },
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 10. Tuition Fee (₹)
                    androidx.compose.material3.OutlinedTextField(
                        value = tuitionFeeText,
                        onValueChange = { tuitionFeeText = it },
                        label = { Text("Tuition Fee * (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = {
                            customSubjectFocusRequester.requestFocus()
                            activeFieldIndex = 10
                            coroutineScope.launch { scrollHelper(10) }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(tuitionFeeTextFocusRequester)
                            .onFocusChanged { if (it.isFocused) { activeFieldIndex = 9; coroutineScope.launch { scrollHelper(9) } } },
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 11. Custom Subject Addition
                    androidx.compose.material3.OutlinedTextField(
                        value = newSubjectInput,
                        onValueChange = { newSubjectInput = it },
                        label = { Text("Add Subject (Press '+' button)") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val trimmed = newSubjectInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    customSubjectsList = customSubjectsList + trimmed
                                    selectedSubjects = selectedSubjects + trimmed
                                    newSubjectInput = ""
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Custom Subject")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(customSubjectFocusRequester)
                            .onFocusChanged { if (it.isFocused) { activeFieldIndex = 10; coroutineScope.launch { scrollHelper(10) } } },
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select Enrolled Subjects *:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                    val combinedSubjects = (com.amarmohan.domain.model.ALLOWED_SUBJECTS + customSubjectsList).distinct()
                    combinedSubjects.chunked(3).forEach { rowSubjects ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSubjects.forEach { subj ->
                                val isSelected = selectedSubjects.contains(subj)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFE2D6FF) else Color(0xFFF3F4F6))
                                        .clickable {
                                            selectedSubjects = if (isSelected) {
                                                selectedSubjects - subj
                                            } else {
                                                selectedSubjects + subj
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = subj,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color(0xFF5138B8) else Color.DarkGray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                            repeat(3 - rowSubjects.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp)) // Buffer space so last elements can scroll fully into view
        }
    }
}

