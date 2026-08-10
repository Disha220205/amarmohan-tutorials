package com.example.presentation.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.notification.NotificationHelper
import com.example.domain.model.*
import com.example.presentation.viewmodel.MainViewModel
import com.example.core.keyboard.moveFocusOnEnter
import kotlinx.coroutines.launch

// Constant tutor number for dialing/messaging
const val TUTOR_OFFICIAL_NUMBER = "9986736360"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val student by viewModel.currentParentStudent.collectAsState()
    val kids by viewModel.parentStudents.collectAsState()
    val sessionSchedules by viewModel.sessionSchedules.collectAsState()
    val context = LocalContext.current

    // Refresh data on entry to sync latest reminders & schedules
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    // Automatic system alarm scheduler for upcoming classes
    val sharedPrefs = remember(context) { context.getSharedPreferences("parent_alarms_prefs", Context.MODE_PRIVATE) }
    LaunchedEffect(sessionSchedules, student) {
        student?.let { kid ->
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Calendar.getInstance().time)
            sessionSchedules.filter { it.studentSlNo == kid.slNo && it.date >= todayStr && it.time.isNotEmpty() && !it.isCompleted }.forEach { sched ->
                val alarmKey = "alarm_scheduled_${sched.id}"
                val alreadyScheduled = sharedPrefs.getBoolean(alarmKey, false)
                if (!alreadyScheduled) {
                    viewModel.scheduleClassAlarmDirect(sched.topic, sched.date, sched.time)
                    sharedPrefs.edit().putBoolean(alarmKey, true).apply()
                    Toast.makeText(context, "Automatic alarm set for class: ${sched.topic} at ${sched.time}!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val localFocusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Tab state indices
    var activeCoreTab by remember { mutableStateOf(0) } // 0 = Performance, 1 = Attendance
    var activeSubTab by remember { mutableStateOf(0) }  // 0 = Reminders, 1 = Testpaper, 2 = Syllabus

    // UI state controllers
    var showUploadDialog by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showSMSDialog by remember { mutableStateOf(false) }
    var showSyllabusDialog by remember { mutableStateOf(false) }
    var showPerformanceLedgerDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showEditDetailsDialog by remember { mutableStateOf(false) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var changePasswordOld by remember { mutableStateOf("") }
    var changePasswordNew by remember { mutableStateOf("") }
    var changePasswordConfirm by remember { mutableStateOf("") }
    var changePasswordError by remember { mutableStateOf<String?>(null) }

    // Dialog Input states
    var uploadSubjectText by remember { mutableStateOf("") }
    var uploadMarksText by remember { mutableStateOf("") }
    var uploadMaxMarksText by remember { mutableStateOf("100") }
    var smsMessageText by remember { mutableStateOf("") }
    var tempSyllabusText by remember { mutableStateOf("") }
    var customReminderText by remember { mutableStateOf("") }

    // Direct Image upload properties
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            capturedBitmap = null
            Toast.makeText(context, "Direct File Selected!", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedImageUri = null
            Toast.makeText(context, "Direct Camera Photo Captured!", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos.", Toast.LENGTH_LONG).show()
        }
    }

    val onLaunchCamera = {
        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    if (student == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Color(0xFF6D28D9))
                Text(
                    text = "Retrieving Verified Student Details...",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.logOut() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("parent_dashboard_error_logout_btn")
                ) {
                    Text("Cancel & Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val studentData = student!!
    val subjects = studentData.getSubjects()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = Color(0xFF6D28D9),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Shree AmarMohan 🧿",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6D28D9)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(modifier = Modifier.height(12.dp))

                NavigationDrawerItem(
                    label = { Text("Progress & Performance", fontWeight = FontWeight.SemiBold) },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Filled.People, contentDescription = null, tint = Color(0xFF6D28D9)) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Change Password", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        changePasswordOld = ""
                        changePasswordNew = ""
                        changePasswordConfirm = ""
                        changePasswordError = null
                        showChangePasswordDialog = true
                    },
                    icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF6D28D9)) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                NavigationDrawerItem(
                    label = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showLogoutConfirmDialog = true
                    },
                    icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Color(0xFFF9F7FF),
            topBar = {
                LargeTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open Navigation Menu", tint = Color(0xFF423870))
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "Shree AmarMohan 🧿",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF5138B8)
                            )
                            Text(
                                text = "Student Profile Dashboard",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF423870)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color(0xFF423870))
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color(0xFFE2D6FF)
                    )
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
            ) {

                // Identical Student Profile Card visually matching the Tutor's detail overlay
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDFF)),
                        border = BorderStroke(1.dp, Color(0xFFE2D6FF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, Color(0xFF7A68E6), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Sl No ${studentData.slNo}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF5138B8),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = studentData.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF32236F)
                                )
                                Text(
                                    text = "${studentData.standard} • ${studentData.board} • ${studentData.school}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TextButton(
                                        onClick = { showEditDetailsDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp).testTag("parent_edit_student_details_btn")
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF6D28D9))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit Details", fontSize = 12.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(
                                        onClick = { showAddStudentDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp).testTag("parent_add_student_btn")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF6D28D9))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Student", fontSize = 12.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
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
                }

                // Multiple child profiles switcher list (if any)
                if (kids.size > 1) {
                    item {
                        Column(modifier = Modifier.padding(bottom = 4.dp)) {
                            Text(
                                text = "Switch Student Profile:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1B4B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(kids.size) { index ->
                                    val kid = kids[index]
                                    val isSelected = kid.slNo == studentData.slNo
                                    Card(
                                        modifier = Modifier
                                            .width(160.dp)
                                            .clickable { viewModel.selectParentStudent(kid.slNo) }
                                            .testTag("kid_selector_${kid.slNo}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0xFFF5F3FF) else Color.White
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF6D28D9) else Color(0xFFE5E7EB)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(
                                                        if (isSelected) Color(0xFF6D28D9) else Color(0xFFEDE9FE),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = kid.name.take(1).uppercase(),
                                                    color = if (isSelected) Color.White else Color(0xFF6D28D9),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = kid.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    color = Color.DarkGray
                                                )
                                                Text(
                                                    text = "SL No: ${kid.slNo}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Overall average accuracy card
                item {
                    val overallAverage = if (subjects.isNotEmpty()) {
                        (subjects.map { it.marks / it.maxMarks }.average() * 100).toInt()
                    } else {
                        0
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPerformanceLedgerDialog = true }
                            .testTag("overall_performance_average_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                        border = BorderStroke(1.5.dp, Color(0xFFDCD6FF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFFEDE9FE), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Assessment,
                                        contentDescription = null,
                                        tint = Color(0xFF6D28D9),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "OVERALL AVERAGE SUCCESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF5B21B6)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Average score calculated across all subjects",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Click to View Performance Ledger",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6D28D9)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$overallAverage%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF6D28D9)
                                )
                                Text(
                                    text = "Status: OK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }

                // 1. CORE CONTAINER: PERFORMANCE & ATTENDANCE SELECTOR TAB
                item {
                    TabRow(
                        selectedTabIndex = activeCoreTab,
                        containerColor = Color.White,
                        contentColor = Color(0xFF6D28D9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = activeCoreTab == 0,
                            onClick = { activeCoreTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Performance Graph", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        )
                        Tab(
                            selected = activeCoreTab == 1,
                            onClick = { activeCoreTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Attendance Calendar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        )
                    }
                }

                // Render Active Core Tab Content (Performance Area or Attendance Area)
                item {
                    AnimatedVisibility(
                        visible = activeCoreTab == 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Subject Wise Custom Graph Visualizer
                            AccuracyChartCard(
                                subjects = subjects,
                                testPapers = studentData.getTestPapers(),
                                onGraphClick = { showPerformanceLedgerDialog = true }
                            )

                            // Quick metrics cards list
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPerformanceLedgerDialog = true }
                                    .testTag("parent_marks_ledger_card"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFE)),
                                border = BorderStroke(1.5.dp, Color(0xFFEDE9FE))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(Color(0xFFF5F3FF), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Assignment,
                                                contentDescription = null,
                                                tint = Color(0xFF6D28D9),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Subject Marks Ledger",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color(0xFF1E1B4B)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "View date-wise performance transcript of all subjects",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { showPerformanceLedgerDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("Open Ledger", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = activeCoreTab == 1,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.FactCheck, contentDescription = null, tint = Color(0xFF10B981))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Interactive Attendance Calendar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }
                                Text("Green indicates Present, Red indicates Absent.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Draw Legend Keys
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Present", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Absent", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                val todayCalendar = remember { java.util.Calendar.getInstance() }
                                val todayString = remember {
                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                    sdf.format(todayCalendar.time)
                                }
                                var parentSelectedCalendarDate by remember { mutableStateOf(todayString) }
                                var currentYear by remember { mutableStateOf(todayCalendar.get(java.util.Calendar.YEAR)) }
                                var currentMonth by remember { mutableStateOf(todayCalendar.get(java.util.Calendar.MONTH) + 1) } // 1-indexed

                                // Navigation Row for Month/Year Selection
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (currentMonth == 1) {
                                                currentMonth = 12
                                                currentYear -= 1
                                            } else {
                                                currentMonth -= 1
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ChevronLeft,
                                            contentDescription = "Previous Month",
                                            tint = Color(0xFF6D28D9)
                                        )
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

                                    Text(
                                        text = "$monthName $currentYear",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF1E1B4B)
                                    )

                                    IconButton(
                                        onClick = {
                                            if (currentMonth == 12) {
                                                currentMonth = 1
                                                currentYear += 1
                                            } else {
                                                currentMonth += 1
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ChevronRight,
                                            contentDescription = "Next Month",
                                            tint = Color(0xFF6D28D9)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Day of Week Headers Row
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val daysOfWeek = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                                    daysOfWeek.forEach { dayName ->
                                        Text(
                                            text = dayName,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }

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
                                                    val dayString = String.format("%04d-%02d-%02d", currentYear, currentMonth, dayNum)
                                                    val isSelected = parentSelectedCalendarDate == dayString

                                                    // Attendance mapping lookup
                                                    val attendanceStatus = studentData.getAttendance()[dayString]

                                                    val dayBg = when {
                                                        attendanceStatus == true -> Color(0xFF10B981) // Green for Present
                                                        attendanceStatus == false -> Color(0xFFEF4444) // Red for Absent
                                                        isSelected -> Color(0xFFEDE9FE)
                                                        else -> Color.Transparent
                                                    }

                                                    val dayTextCol = when {
                                                        attendanceStatus != null -> Color.White
                                                        isSelected -> Color(0xFF6D28D9)
                                                        else -> Color.DarkGray
                                                    }

                                                    val hasSched = sessionSchedules.any { it.studentSlNo == studentData.slNo && it.date == dayString }
                                                    val hasRem = studentData.getReminders().any { it.date == dayString }

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(2.dp)
                                                            .aspectRatio(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(dayBg)
                                                            .border(
                                                                width = if (isSelected) 1.5.dp else 0.dp,
                                                                color = if (isSelected) Color(0xFF6D28D9) else Color.Transparent,
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable { parentSelectedCalendarDate = dayString },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Text(
                                                                text = "$dayNum",
                                                                color = dayTextCol,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp
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

                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Class updates for: $parentSelectedCalendarDate", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                                val daySchedules = sessionSchedules
                                    .filter { it.studentSlNo == studentData.slNo }
                                    .filter { it.date == parentSelectedCalendarDate }

                                if (daySchedules.isEmpty()) {
                                    Text("No sessions recorded on this day.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                } else {
                                    for (s in daySchedules) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .background(Color(0xFFFAFAFA), shape = RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(s.topic, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                Text("Time: ${s.time} • ${s.durationMinutes} mins", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(if (s.isCompleted) Color(0xFFE8F5E9) else Color(0xFFFFF9C4), shape = CircleShape)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (s.isCompleted) "Completed" else "Expected",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (s.isCompleted) Color(0xFF2E7D32) else Color(0xFFF57F17)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Fees Control Checklist Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                        border = BorderStroke(1.dp, Color(0xFFEDE9FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Outstanding Dues:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF32236F))
                                Text("Subject-wise custom subscription fee", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Text(
                                text = "₹${studentData.dueAmount}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF6D28D9)
                            )
                        }
                    }
                }

                // 2. SUB-TABS ACTIONS HEADER (Reminders, Testpaper, Syllabus)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEDE9FE), RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Reminders", Icons.Filled.Notifications, 0),
                            Triple("Testpaper", Icons.Filled.PhotoLibrary, 1)
                        ).forEach { (label, icon, index) ->
                            val isSelected = activeSubTab == index
                            Button(
                                onClick = { activeSubTab = index },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color.White else Color.Transparent,
                                    contentColor = if (isSelected) Color(0xFF6D28D9) else Color(0xFF7C3AED)
                                ),
                                elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 3.dp) else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 18.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = label,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // Render Subtab Content (Reminders, Testpaper, Syllabus)
                if (activeSubTab == 0) {
                    // ===================================
                    // TAB: REMINDERS & TIMER ALERTS
                    // ===================================

                    // Alerts list
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Notes & Teacher Reminders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFF1E1B4B))
                                Spacer(modifier = Modifier.height(10.dp))

                                val alertsList = studentData.getReminders()
                                if (alertsList.isEmpty()) {
                                    Text("No custom reminders entered.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                } else {
                                    alertsList.forEach { rem ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .background(Color(0xFFF9FAFB), shape = RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (rem.isByParent) Icons.Filled.Chat else Icons.Filled.PushPin,
                                                    contentDescription = null,
                                                    tint = if (rem.isByParent) Color(0xFF7C3AED) else Color(0xFF3B82F6),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = if (rem.isByParent) Color(0xFFF5EFFF) else Color(0xFFEFF6FF),
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (rem.isByParent) "Parent" else "Tutor",
                                                        color = if (rem.isByParent) Color(0xFF7C3AED) else Color(0xFF1D4ED8),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(rem.text, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                            }
                                            IconButton(onClick = { viewModel.removeReminder(studentData.slNo, rem.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Publish New Sync Reminder Node (Tutor/Parent Viewable):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customReminderText,
                                        onValueChange = { customReminderText = it },
                                        placeholder = { Text("e.g. Next fee pay date is tomorrow") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            if (customReminderText.isBlank()) return@Button
                                            viewModel.addReminder(studentData.slNo, customReminderText.trim(), isImportant = true, isByParent = true)
                                            customReminderText = ""
                                            Toast.makeText(context, "Reminder synced and added!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }

                } else if (activeSubTab == 1) {
                    // ===================================
                    // TAB: TESTPAPERS SUBMISSIONS DESK
                    // ===================================
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Description, contentDescription = null, tint = Color(0xFF10B981))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enrolled Course Testpapers", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    }
                                    Button(
                                        onClick = {
                                            selectedImageUri = null
                                            capturedBitmap = null
                                            if (subjects.isNotEmpty()) {
                                                uploadSubjectText = subjects.first().subjectName
                                            } else {
                                                uploadSubjectText = "Mathematics"
                                            }
                                            showUploadDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Upload Paper")
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val testPapersList = studentData.getTestPapers()
                                // Filter, sort latest first by date, and group subject-wise
                                val groupedPapers = testPapersList
                                    .filter { paper ->
                                        subjects.any { sub -> sub.subjectName.equals(paper.subjectName, ignoreCase = true) }
                                    }
                                    .sortedByDescending { it.testDate }
                                    .groupBy { it.subjectName }

                                if (groupedPapers.isEmpty()) {
                                    Text("No submissions loaded for your registered subjects.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                } else {
                                    groupedPapers.forEach { (subjectTitle, papersList) ->
                                        Surface(
                                            color = Color(0xFFFAF9FF),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                                .border(1.dp, Color(0xFFEBE6FF), RoundedCornerShape(10.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.MenuBook,
                                                        contentDescription = null,
                                                        tint = Color(0xFF6D28D9),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = subjectTitle.uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF6D28D9),
                                                        letterSpacing = 0.5.sp
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = "${papersList.size} Paper(s)",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }

                                                papersList.forEach { paper ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                                                    ) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text("Marksheet Entry", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.DarkGray)
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(Color(0xFFE1F5FE), shape = RoundedCornerShape(12.dp))
                                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = if (paper.marksObtained != null && paper.totalMarks != null) {
                                                                            "${paper.marksObtained.toInt()}/${paper.totalMarks.toInt()}"
                                                                        } else {
                                                                            "No Score"
                                                                        },
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF0288D1)
                                                                    )
                                                                }
                                                            }
                                                            Text("Uploaded: ${paper.testDate} • Status: ${paper.status}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                            if (paper.tutorPhotoUri != null) {
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Icon(Icons.Filled.Description, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text("Tutor Co-Signed Approved!", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(8.dp))

                                                            val finalUrl = when {
                                                                paper.parentPhotoUri?.startsWith("/data/") == true -> paper.parentPhotoUri
                                                                paper.parentPhotoUri?.startsWith("content://") == true -> paper.parentPhotoUri
                                                                paper.parentPhotoUri?.startsWith("http") == true -> paper.parentPhotoUri
                                                                else -> "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=500&auto=format&fit=crop"
                                                            }

                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(130.dp)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(Color(0xFFEEF2F6)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                AsyncImage(
                                                                    model = finalUrl,
                                                                    contentDescription = "Testpaper Result Page",
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentScale = ContentScale.Crop
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

                }

                // Render Course Syllabus Tracking permanently at the bottom of the Parent Dashboard!
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color(0xFF6D28D9))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Course Syllabus Tracking", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }
                                TextButton(onClick = {
                                    tempSyllabusText = studentData.syllabus
                                    showSyllabusDialog = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Syllabus")
                                }
                            }

                            Text("Subject syllabus portions configured for tests:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(10.dp))

                            if (subjects.isEmpty()) {
                                Text("No subjects enrolled to trace syllabus.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            } else {
                                // Parse current syllabus text. If it is flat, show nice cards per subject.
                                val currentSyllabus = studentData.syllabus
                                subjects.forEach { subj ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.ImportContacts, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(subj.subjectName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4338CA))
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Filter out portions matching this subject or show default helper
                                            val portionText = if (currentSyllabus.contains(subj.subjectName, ignoreCase = true)) {
                                                currentSyllabus.lines()
                                                    .filter { it.contains(subj.subjectName, ignoreCase = true) }
                                                    .joinToString("\n")
                                            } else {
                                                "Preparation Topic: Complete portions as marked by teacher."
                                            }

                                            Text(portionText, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Call / Message Deck Docked at Bottom
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Emergency Call & Message Deck: $TUTOR_OFFICIAL_NUMBER",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:$TUTOR_OFFICIAL_NUMBER")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot dial", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call")
                                }

                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    onClick = { showSMSDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SMS")
                                }
                            }
                        }
                    }
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
        }
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("The default password is your student's name.", fontSize = 12.sp, color = Color.Gray)

                    if (changePasswordError != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = changePasswordError ?: "",
                                color = Color(0xFF991B1B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = changePasswordOld,
                        onValueChange = { changePasswordOld = it },
                        label = { Text("Current Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = changePasswordNew,
                        onValueChange = { changePasswordNew = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = changePasswordConfirm,
                        onValueChange = { changePasswordConfirm = it },
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (changePasswordOld.trim().isEmpty() || changePasswordNew.trim().isEmpty()) {
                            changePasswordError = "All fields are required."
                            return@Button
                        }
                        if (changePasswordNew != changePasswordConfirm) {
                            changePasswordError = "New passwords do not match."
                            return@Button
                        }
                        viewModel.changeParentPassword(
                            phone = studentData.parentPhone,
                            oldPass = changePasswordOld,
                            newPass = changePasswordNew,
                            onSuccess = {
                                Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                showChangePasswordDialog = false
                            },
                            onError = {
                                changePasswordError = it
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                ) {
                    Text("Update Password", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
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

    if (showEditDetailsDialog) {
        var editedName by remember { mutableStateOf<String>(studentData.name) }
        var editedStandard by remember { mutableStateOf<String>(studentData.standard) }
        var editedBoard by remember { mutableStateOf<String>(studentData.board) }
        var editedSchool by remember { mutableStateOf<String>(studentData.school) }
        var editedFlatNo by remember { mutableStateOf<String>(studentData.parentFlatNo) }

        AlertDialog(
            onDismissRequest = { showEditDetailsDialog = false },
            title = { Text("Edit Student Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("Student Name", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_student_name")
                    )

                    Text("Standard / Class", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = editedStandard,
                        onValueChange = { editedStandard = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_student_standard")
                    )

                    Text("Education Board", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = editedBoard,
                        onValueChange = { editedBoard = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_student_board")
                    )

                    Text("School Name", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = editedSchool,
                        onValueChange = { editedSchool = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_student_school")
                    )

                    Text("Flat / House Number", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = editedFlatNo,
                        onValueChange = { editedFlatNo = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_student_flatno")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedName.isBlank() || editedStandard.isBlank()) {
                            Toast.makeText(context, "Name and Standard are required!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showEditDetailsDialog = false
                        viewModel.addOrUpdateStudentDetails(
                            slNo = studentData.slNo,
                            name = editedName,
                            phone = studentData.parentPhone,
                            school = editedSchool,
                            board = editedBoard,
                            standard = editedStandard,
                            email = studentData.studentEmail,
                            subjects = studentData.getSubjects(),
                            syllabus = studentData.syllabus,
                            tuitionFee = studentData.tuitionFee,
                            isPaid = studentData.isFeesPaid,
                            demoFee = studentData.demoFee,
                            parentFlatNo = editedFlatNo
                        )
                        Toast.makeText(context, "Student details updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                    modifier = Modifier.testTag("save_edited_student_btn")
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDetailsDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showAddStudentDialog) {
        var newName by remember { mutableStateOf<String>("") }
        var newStandard by remember { mutableStateOf<String>("") }
        var newBoard by remember { mutableStateOf<String>("") }
        var newSchool by remember { mutableStateOf<String>("") }
        var newFlatNo by remember { mutableStateOf<String>("") }

        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = { Text("Add New Student Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("Student Name", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("e.g. Aarav Kumar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_student_name")
                    )

                    Text("Standard / Class", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = newStandard,
                        onValueChange = { newStandard = it },
                        placeholder = { Text("e.g. 10th Standard") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_student_standard")
                    )

                    Text("Education Board", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = newBoard,
                        onValueChange = { newBoard = it },
                        placeholder = { Text("e.g. CBSE") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_student_board")
                    )

                    Text("School Name", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = newSchool,
                        onValueChange = { newSchool = it },
                        placeholder = { Text("e.g. National Public School") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_student_school")
                    )

                    Text("Flat / House Number", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = newFlatNo,
                        onValueChange = { newFlatNo = it },
                        placeholder = { Text("e.g. Flat 302, Phase 1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_student_flatno")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isBlank() || newStandard.isBlank()) {
                            Toast.makeText(context, "Name and Standard are required!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showAddStudentDialog = false
                        viewModel.addOrUpdateStudentDetails(
                            slNo = "SL${(100..999).random()}",
                            name = newName,
                            phone = studentData.parentPhone,
                            school = newSchool,
                            board = newBoard,
                            standard = newStandard,
                            parentFlatNo = newFlatNo
                        )
                        Toast.makeText(context, "New student details added successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                    modifier = Modifier.testTag("confirm_add_student_btn")
                ) {
                    Text("Add Student", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Performance Ledger Dialog modal overlays
    if (showPerformanceLedgerDialog) {
        PerformanceLedgerDialog(
            showDialog = showPerformanceLedgerDialog,
            onDismiss = { showPerformanceLedgerDialog = false },
            subjects = subjects,
            testPapers = studentData.getTestPapers()
        )
    }

    // B. SYLLABUS EDIT DIALOG
    if (showSyllabusDialog) {
        val subjectNames = (subjects.map { it.subjectName } + com.example.domain.model.ALLOWED_SUBJECTS).distinct()
        var subjectSyllabusMap by remember {
            val map = mutableMapOf<String, String>()
            subjectNames.forEach { map[it] = "" }
            val lines = tempSyllabusText.split(Regex("[\\n;]+"))
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                val parts = trimmed.split(":", limit = 2)
                if (parts.size == 2) {
                    val subjKey = parts[0].trim()
                    val portion = parts[1].trim()
                    val matchedSubj = subjectNames.find { it.equals(subjKey, ignoreCase = true) }
                    if (matchedSubj != null) {
                        map[matchedSubj] = portion
                    }
                } else {
                    val matchedSubj = subjectNames.find { trimmed.startsWith(it, ignoreCase = true) }
                    if (matchedSubj != null) {
                        val portion = trimmed.substring(matchedSubj.length).trim().removePrefix(":").removePrefix("-").trim()
                        map[matchedSubj] = portion
                    }
                }
            }
            mutableStateOf(map.toMap())
        }

        AlertDialog(
            onDismissRequest = { showSyllabusDialog = false },
            title = { Text("Update Next Portion / Syllabus", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Update syllabus portions subject-wise:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val subjectsToShow = if (subjects.isNotEmpty()) subjects.map { it.subjectName } else com.example.domain.model.ALLOWED_SUBJECTS.take(6)

                        subjectsToShow.forEach { subject ->
                            val currentPortion = subjectSyllabusMap[subject] ?: ""
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "$subject Syllabus:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF6D28D9)
                                )
                                OutlinedTextField(
                                    value = currentPortion,
                                    onValueChange = { newValue ->
                                        subjectSyllabusMap = subjectSyllabusMap.toMutableMap().apply {
                                            put(subject, newValue)
                                        }
                                    },
                                    placeholder = { Text("Enter portion specifics for $subject...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalSyllabusText = subjectSyllabusMap.entries
                            .filter { it.value.trim().isNotEmpty() }
                            .joinToString("\n") { "${it.key}: ${it.value.trim()}" }

                        viewModel.saveSyllabus(studentData.slNo, finalSyllabusText)
                        tempSyllabusText = finalSyllabusText
                        showSyllabusDialog = false
                        Toast.makeText(context, "Syllabus details updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyllabusDialog = false }) { Text("Cancel") }
            }
        )
    }

    // C. DIRECT UPLOAD TEST-PAPER PICTURE DIALOG (REMOVE LINKING SYSTEM - CAMERA / FILE SELECTION)
    if (showUploadDialog) {
        var expandSubjDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Direct Photo Upload Desk", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Capture a real-time photo of your child's marked paper or pick from local gallery files. Linking code system has been completely removed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }

                    // Direct choice buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onLaunchCamera() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { filePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Files / Gallery", fontSize = 11.sp)
                            }
                        }
                    }

                    // Direct photo preview rendering
                    item {
                        if (capturedBitmap != null || selectedImageUri != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                if (capturedBitmap != null) {
                                    AsyncImage(
                                        model = capturedBitmap,
                                        contentDescription = "Live Captured Paper Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (selectedImageUri != null) {
                                    AsyncImage(
                                        model = selectedImageUri,
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
                                    .height(100.dp)
                                    .background(Color(0xFFF3F4F6), shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFD1D5DB), shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.ImageNotSupported, contentDescription = null, tint = Color.Gray)
                                    Text("No direct photo loaded yet.", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    // Dropdown course selection
                    item {
                        Text("Select Target Course Subject:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandSubjDropdown = !expandSubjDropdown },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(uploadSubjectText)
                            }
                            DropdownMenu(
                                expanded = expandSubjDropdown,
                                onDismissRequest = { expandSubjDropdown = false }
                            ) {
                                if (subjects.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No Enrolled Subjects", color = Color.Red) },
                                        onClick = { expandSubjDropdown = false }
                                    )
                                } else {
                                    subjects.map { it.subjectName }.distinct().forEach { subName ->
                                        DropdownMenuItem(
                                            text = { Text(subName) },
                                            onClick = {
                                                uploadSubjectText = subName
                                                expandSubjDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Marks Obtained / Total Max
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uploadMarksText,
                                onValueChange = { uploadMarksText = it },
                                label = { Text("Obtained Marks") },
                                placeholder = { Text("e.g. 85") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { localFocusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                                modifier = Modifier.weight(1f).moveFocusOnEnter(localFocusManager, androidx.compose.ui.focus.FocusDirection.Next)
                            )
                            OutlinedTextField(
                                value = uploadMaxMarksText,
                                onValueChange = { uploadMaxMarksText = it },
                                label = { Text("Max Marks") },
                                placeholder = { Text("e.g. 100") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (capturedBitmap == null && selectedImageUri == null) {
                            Toast.makeText(context, "Please take a photo or select a file first.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val savedPath = if (capturedBitmap != null) {
                            viewModel.saveCapturedBitmap(capturedBitmap!!)
                        } else {
                            selectedImageUri?.let { viewModel.saveSelectedUri(it) }
                        }

                        if (savedPath == null) {
                            Toast.makeText(context, "Error saving test paper image locally.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val finalUri = savedPath
                        val marksVal = uploadMarksText.toDoubleOrNull()
                        val maxMarksVal = uploadMaxMarksText.toDoubleOrNull() ?: 100.0

                        viewModel.uploadTestPaper(
                            studentSlNo = studentData.slNo,
                            subject = uploadSubjectText,
                            marksObtained = marksVal,
                            totalMarks = maxMarksVal,
                            sourceUri = finalUri
                        )

                        if (marksVal != null) {
                            viewModel.addTestScore(
                                studentSlNo = studentData.slNo,
                                subject = uploadSubjectText,
                                score = marksVal,
                                maxScore = maxMarksVal,
                                testType = "Direct Capture"
                            )
                        }

                        showUploadDialog = false
                        Toast.makeText(context, "Direct Upload Completed and Synced!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Upload Content")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) { Text("Cancel") }
            }
        )
    }

    // D. SMS COMPOSE DIALOG
    if (showSMSDialog) {
        AlertDialog(
            onDismissRequest = { showSMSDialog = false },
            title = { Text("Compose Feedback SMS for Tutor", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Compose feedback text context. We will launch your cell phone standard SMS client:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = smsMessageText,
                        onValueChange = { smsMessageText = it },
                        placeholder = { Text("Tutor, please check Malayalam portions...") },
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (smsMessageText.trim().isEmpty()) return@Button
                        try {
                            val msg = "Shree Amarmohan Support - Parent Feedback (${studentData.name}): $smsMessageText"
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:$TUTOR_OFFICIAL_NUMBER")
                                putExtra("sms_body", msg)
                            }
                            context.startActivity(intent)
                            showSMSDialog = false
                        } catch (e: Exception) {
                            clipboardManager.setText(AnnotatedString(smsMessageText))
                            Toast.makeText(context, "Launcher error. Body copied to clipboard!", Toast.LENGTH_LONG).show()
                            showSMSDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                ) {
                    Text("Open SMS Client")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSMSDialog = false }) { Text("Cancel") }
            }
        )
    }

}

@Composable
fun AccuracyChartCard(
    subjects: List<SubjectMark>,
    testPapers: List<com.example.domain.model.TestPaper>,
    onGraphClick: () -> Unit
) {
    // Aggregate unique subjects list
    val allSubjectNames = subjects.map { it.subjectName }.distinct()

    var selectedSubjectName by remember(subjects) {
        mutableStateOf(allSubjectNames.firstOrNull() ?: "")
    }

    val displayPoints = remember(selectedSubjectName, subjects, testPapers) {
        if (selectedSubjectName.isEmpty()) {
            emptyList()
        } else {
            // Filter historical test papers for this subject
            val history = testPapers.filter { it.subjectName.equals(selectedSubjectName, ignoreCase = true) }
                .sortedBy { it.testDate }
            if (history.isEmpty()) {
                val current = subjects.find { it.subjectName.equals(selectedSubjectName, ignoreCase = true) }
                if (current != null) {
                    listOf(Pair("Current", (current.marks / current.maxMarks).toFloat().coerceIn(0f, 1f)))
                } else {
                    emptyList()
                }
            } else {
                history.map {
                    Pair(it.testDate, ((it.marksObtained ?: 0.0) / (it.totalMarks ?: 100.0)).toFloat().coerceIn(0f, 1f))
                }
            }
        }
    }

    val currentAvg = remember(selectedSubjectName, subjects) {
        if (selectedSubjectName.isEmpty()) {
            0.0
        } else {
            val current = subjects.find { it.subjectName.equals(selectedSubjectName, ignoreCase = true) }
            if (current != null) (current.marks / current.maxMarks) * 100 else 0.0
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF9FF)),
        border = BorderStroke(1.dp, Color(0xFFECE6FF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedSubjectName.isEmpty()) "No Subject Progress" else "$selectedSubjectName Progress",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF3B2896)
                    )
                    Text(
                        text = "Historical test paper progression",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEDE9FE), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${currentAvg.toInt()}% Acc",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color(0xFF6D28D9)
                        )
                    }
                    Button(
                        onClick = onGraphClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Performance", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable subject selection row
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allSubjectNames.size) { index ->
                    val name = allSubjectNames[index]
                    val isSelected = name == selectedSubjectName
                    val containerCol = if (isSelected) Color(0xFF6D28D9) else Color(0xFFF3F0FF)
                    val contentCol = if (isSelected) Color.White else Color(0xFF5B49C4)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(containerCol)
                            .clickable { selectedSubjectName = name }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = contentCol
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Customized Peak Curve Chart
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp).clickable { onGraphClick() }) {
                val width = size.width
                val height = size.height

                // Draw horizontal auxiliary gridlines
                val lineCount = 4
                for (i in 0..lineCount) {
                    val y = height * i / lineCount
                    drawLine(
                        color = Color(0xFFEFE8FF),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Convert points to UI Offsets
                val pointOffsets = displayPoints.mapIndexed { index, pair ->
                    val pct = pair.second
                    val x = if (displayPoints.size <= 1) {
                        width / 2f
                    } else {
                        index * (width / (displayPoints.size - 1))
                    }
                    val y = height - (pct * height * 0.75f) - (height * 0.1f)
                    androidx.compose.ui.geometry.Offset(x, y)
                }

                // Draw gradient path fill
                if (pointOffsets.isNotEmpty()) {
                    if (pointOffsets.size == 1) {
                        // Just single point
                        drawCircle(
                            color = Color(0xFF6D28D9),
                            radius = 6.dp.toPx(),
                            center = pointOffsets.first()
                        )
                    } else {
                        val fillPath = Path().apply {
                            moveTo(pointOffsets.first().x, height)
                            lineTo(pointOffsets.first().x, pointOffsets.first().y)
                            for (i in 1 until pointOffsets.size) {
                                val prev = pointOffsets[i - 1]
                                val curr = pointOffsets[i]
                                cubicTo(
                                    (prev.x + curr.x) / 2, prev.y,
                                    (prev.x + curr.x) / 2, curr.y,
                                    curr.x, curr.y
                                )
                            }
                            lineTo(pointOffsets.last().x, height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFC7B8FF).copy(alpha = 0.5f), Color.Transparent)
                            )
                        )

                        // Draw main connecting curve line
                        val curvePath = Path().apply {
                            moveTo(pointOffsets.first().x, pointOffsets.first().y)
                            for (i in 1 until pointOffsets.size) {
                                val prev = pointOffsets[i - 1]
                                val curr = pointOffsets[i]
                                cubicTo(
                                    (prev.x + curr.x) / 2, prev.y,
                                    (prev.x + curr.x) / 2, curr.y,
                                    curr.x, curr.y
                                )
                            }
                        }
                        drawPath(
                            path = curvePath,
                            color = Color(0xFF6D28D9),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )

                        // Draw glowing anchor dots
                        pointOffsets.forEach { pt ->
                            drawCircle(
                                color = Color.White,
                                radius = 5.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color(0xFF6D28D9),
                                radius = 3.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // X-Axis Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (displayPoints.size <= 1) Arrangement.Center else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                displayPoints.forEach { pair ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val labelText = if (pair.first.length > 8) pair.first.take(6) + ".." else pair.first
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B2896)
                        )
                        Text(
                            text = "${(pair.second * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceLedgerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    subjects: List<SubjectMark>,
    testPapers: List<com.example.domain.model.TestPaper>
) {
    if (!showDialog) return

    val avgPct = if (subjects.isNotEmpty()) {
        (subjects.map { it.marks / it.maxMarks }.average() * 100).toInt()
    } else {
        0
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFAFAFE)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
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
                                imageVector = Icons.Filled.Assessment,
                                contentDescription = null,
                                tint = Color(0xFF6D28D9),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Performance & Marks Ledger",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF1E1B4B)
                            )
                            Text(
                                text = "Detailed academic history",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6), shape = CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Dashboard Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("OVERALL ACCURACY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$avgPct%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF6D28D9))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TOTAL SUBJECTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${subjects.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DATE-WISE SUBJECT CHRONOLOGY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Main Scrollable subjects with date-wise historical marks
                Box(modifier = Modifier.weight(1f)) {
                    if (subjects.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No subject-wise performance registered.", color = Color.Gray)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            subjects.forEach { sub ->
                                // Filtering test papers matching this subject
                                val matchingPapers = testPapers.filter {
                                    it.subjectName.equals(sub.subjectName, ignoreCase = true)
                                }.sortedByDescending { it.testDate }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFEEF2F6))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        // Subject General Info Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color(0xFF6D28D9), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = sub.subjectName,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color(0xFF1E1B4B)
                                                )
                                            }

                                            // Latest/average mark bubble
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFF3E8FF), shape = RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Average: ${sub.marks.toInt()}/${sub.maxMarks.toInt()}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF6D28D9)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = Color(0xFFF3F4F6))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Chronological Tests List
                                        if (matchingPapers.isEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Info,
                                                    contentDescription = null,
                                                    tint = Color.LightGray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "No date-wise history uploaded yet.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.LightGray
                                                )
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                matchingPapers.forEach { paper ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                                            .padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.CalendarMonth,
                                                                    contentDescription = null,
                                                                    tint = Color.Gray,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = paper.testDate,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    fontSize = 11.sp,
                                                                    color = Color.DarkGray
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "Status: ${paper.status}",
                                                                fontSize = 9.sp,
                                                                color = if (paper.status.contains("Completed", ignoreCase = true)) Color(0xFF10B981) else Color(0xFFF59E0B)
                                                            )
                                                        }

                                                        // Marks block
                                                        val scoreText = if (paper.marksObtained != null && paper.totalMarks != null) {
                                                            "${paper.marksObtained.toInt()}/${paper.totalMarks.toInt()} Marks"
                                                        } else {
                                                            "Pending Evaluation"
                                                        }

                                                        Text(
                                                            text = scoreText,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = if (paper.marksObtained != null) Color(0xFF6D28D9) else Color.Gray
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

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close Ledger Screen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
