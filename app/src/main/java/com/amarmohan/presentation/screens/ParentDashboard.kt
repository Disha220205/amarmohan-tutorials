package com.amarmohan.presentation.screens

import android.graphics.Bitmap
import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amarmohan.domain.model.*
import com.amarmohan.presentation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentStudent by viewModel.currentParentStudent.collectAsState()
    val siblingStudents by viewModel.parentStudents.collectAsState()

    if (currentStudent == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF6200EE))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading student details...", color = Color.Gray)
            }
        }
        return
    }

    val student = currentStudent!!

    // Active sub-view tabs (0: Dashboard, 1: Test Papers, 2: Edit Student)
    var activeSubViewTab by remember { mutableStateOf(0) }
    // Dashboard sub-tabs (0: Performance, 1: Attendance, 2: Syllabus Notes)
    var activeDashboardSubTab by remember { mutableStateOf(0) }

    // Settings dropdown
    var showSettingsMenu by remember { mutableStateOf(false) }

    // Edit Student Registry Parameters State
    var editName by remember(student.name) { mutableStateOf(student.name) }
    var editPhone by remember(student.parentPhone) { mutableStateOf(student.parentPhone) }
    var editFlatNo by remember(student.parentFlatNo) { mutableStateOf(student.parentFlatNo) }
    var editClass by remember(student.standard) { mutableStateOf(student.standard) }
    var editSchool by remember(student.school) { mutableStateOf(student.school) }
    var editBoard by remember(student.board) { mutableStateOf(student.board) }
    var editEmail by remember(student.studentEmail) { mutableStateOf(student.studentEmail) }

    var editSubjectsList by remember(student.subjectsJson) { mutableStateOf(student.getSubjects()) }
    var selectedSubjectDropdown by remember { mutableStateOf("") }
    var showAddSubjectMenu by remember { mutableStateOf(false) }

    val availableSubjects = remember {
        listOf("Physics", "Mathematics", "Chemistry", "Biology", "English", "Social Studies", "Hindi", "Computer Science", "Science")
    }

    // Syllabus States
    var newSyllabusSubjectInput by remember { mutableStateOf("") }
    var newSyllabusTopicInput by remember { mutableStateOf("") }
    var isEditingSyllabus by remember { mutableStateOf(false) }

    // Test Paper Upload States
    var attachmentSubjectText by remember { mutableStateOf("") }
    var attachmentMarksText by remember { mutableStateOf("") }
    var attachmentMaxMarksText by remember { mutableStateOf("100") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showTestPaperSubjectMenu by remember { mutableStateOf(false) }

    // Confirmation dialog state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Change Password Dialog States
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var oldPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Camera & File Pickers
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            capturedBitmap = null
            Toast.makeText(context, "File Selected!", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedImageUri = null
            Toast.makeText(context, "Photo Captured!", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission required.", Toast.LENGTH_LONG).show()
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

    val subjects = student.getSubjects()
    val attendance = student.getAttendance()
    val presentDays = attendance.count { it.value }
    val totalDays = attendance.size.coerceAtLeast(1)
    val attendancePct = presentDays.toFloat() / totalDays.toFloat()

    val avgScorePct = if (subjects.isNotEmpty()) {
        (subjects.map { if (it.maxMarks > 0) it.marks / it.maxMarks else 0.0 }.average() * 100).toInt()
    } else {
        0
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2D6FF)) // Lavender Toolbar Header
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showLogoutConfirmDialog = true },
                            modifier = Modifier.testTag("parent_app_logo_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF5138B8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "App Logo / Log Out",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Student Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF423870)
                        )
                    }

                    Box {
                        IconButton(onClick = { showSettingsMenu = !showSettingsMenu }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFF423870)
                            )
                        }
                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh Records") },
                                onClick = {
                                    showSettingsMenu = false
                                    viewModel.refreshData()
                                    Toast.makeText(context, "Refreshed data!", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Change Password") },
                                onClick = {
                                    showSettingsMenu = false
                                    oldPasswordInput = ""
                                    newPasswordInput = ""
                                    confirmPasswordInput = ""
                                    showChangePasswordDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF7A68E6)) },
                                modifier = Modifier.testTag("change_password_menu_item")
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Student Profile") },
                                onClick = {
                                    showSettingsMenu = false
                                    activeSubViewTab = 2
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Log Out") },
                                onClick = {
                                    showSettingsMenu = false
                                    showLogoutConfirmDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red) }
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF9F7FF),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Sibling Switcher if parent has multiple children
            if (siblingStudents.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEDE9FE))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Child Profile:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5138B8)
                    )
                    siblingStudents.forEach { sibling ->
                        val isSelected = sibling.slNo == student.slNo
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectParentStudent(sibling.slNo) },
                            label = { Text(sibling.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White,
                                selectedLabelColor = Color(0xFF4F46E5),
                                containerColor = Color.Transparent,
                                labelColor = Color(0xFF64748B)
                            )
                        )
                    }
                }
            }

            // STUDENT INFO BADGE CARD (Matches Screenshot 1, 2, 3)
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
                        // Sl No pill tag
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
                        val schoolInfo = listOf(student.standard, student.board, student.school)
                            .filter { it.isNotEmpty() }
                            .joinToString(" • ")
                        Text(
                            text = schoolInfo.ifEmpty { "Class N/A" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    // Smiling face circular avatar
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

            // HORIZONTAL SUB-VIEW TABS (Dashboard | Test Papers | Edit Student)
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
                            .padding(vertical = 6.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (activeSubViewTab == idx) FontWeight.Bold else FontWeight.Normal,
                            color = if (activeSubViewTab == idx) Color(0xFF6200EE) else Color.Gray,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (activeSubViewTab == idx) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(2.dp)
                                    .background(Color(0xFF6200EE))
                            )
                        } else {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TAB CONTENTS SWITCHING
            when (activeSubViewTab) {
                0 -> {
                    // TAB 0: DASHBOARD TAB
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // TOP SUMMARY CARDS (Marks Score & Attendance)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Marks Score Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "${avgScorePct}%",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF5138B8)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Marks Score",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            // Attendance Summary Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F2)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "$presentDays/$totalDays",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEC4899)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.FactCheck,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Attendance",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        // CATEGORY PILL ROW (Performance | Attendance | Syllabus Notes)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEDE9FE), RoundedCornerShape(16.dp))
                                .padding(8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Performance Button
                            Button(
                                onClick = { activeDashboardSubTab = 0 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeDashboardSubTab == 0) Color.White else Color.Transparent,
                                    contentColor = if (activeDashboardSubTab == 0) Color(0xFF6D28D9) else Color(0xFF5138B8)
                                ),
                                elevation = if (activeDashboardSubTab == 0) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 125.dp),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = "Performance",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Performance", fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                }
                            }

                            // 2. Attendance Button
                            Button(
                                onClick = { activeDashboardSubTab = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeDashboardSubTab == 1) Color.White else Color.Transparent,
                                    contentColor = if (activeDashboardSubTab == 1) Color(0xFF6D28D9) else Color(0xFF5138B8)
                                ),
                                elevation = if (activeDashboardSubTab == 1) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 125.dp),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FactCheck,
                                        contentDescription = "Attendance",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Attendance", fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                }
                            }

                            // 3. Syllabus Notes Button
                            Button(
                                onClick = { activeDashboardSubTab = 2 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeDashboardSubTab == 2) Color.White else Color.Transparent,
                                    contentColor = if (activeDashboardSubTab == 2) Color(0xFF6D28D9) else Color(0xFF5138B8)
                                ),
                                elevation = if (activeDashboardSubTab == 2) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 125.dp),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "Syllabus Notes",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Syllabus Notes", fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // SUB-CATEGORY CONTENT SWITCH
                        when (activeDashboardSubTab) {
                            0 -> {
                                // PERFORMANCE SUB-TAB: Graph + Records
                                ParentPerformanceMarksLineChart(subjects)

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Subject-wise Performance Records:",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF32236F),
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    if (subjects.isEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFECE6FF))
                                        ) {
                                            Text(
                                                text = "No test marks recorded for this student yet.",
                                                modifier = Modifier.padding(16.dp),
                                                color = Color.Gray,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        subjects.forEach { score ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                border = BorderStroke(1.dp, Color(0xFFEDE9FE)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = score.subjectName,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF1F2937),
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                        Text(
                                                            text = "Type: ${score.testType} • Date: ${score.testDate.ifEmpty { "15-07-2026" }}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = "${score.marks.toInt()} / ${score.maxMarks.toInt()}",
                                                            fontWeight = FontWeight.Black,
                                                            color = Color(0xFF7C3AED),
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                        val percent = if (score.maxMarks > 0) (score.marks / score.maxMarks * 100).toInt() else 0
                                                        Text(
                                                            text = "$percent% Acc",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // FEES DUE CARD
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
                                            Text(
                                                text = "Fees Due",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1E3A8A)
                                            )
                                            val actualDue = student.dueAmount.toInt()
                                            Text(
                                                text = "₹$actualDue",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF1E3A8A)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val feeSubtext = if (student.isFeesPaid || student.dueAmount <= 0.0) "All dues cleared!" else "Pending Outstanding Due"
                                        Text(
                                            text = feeSubtext,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (student.isFeesPaid || student.dueAmount <= 0.0) Color(0xFF10B981) else Color(0xFF2563EB)
                                        )
                                    }
                                }

                                // BOTTOM ACTION BUTTONS (Call Tutor / Message Tutor)
                                val tutorPhoneToContact = "9986736360"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tutorPhoneToContact"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("call_tutor_button_${student.slNo}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Call Tutor", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$tutorPhoneToContact")).apply {
                                                putExtra("sms_body", "Hello Amarmohan Sir, regarding student ${student.name}.")
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("message_tutor_button_${student.slNo}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Message", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            1 -> {
                                // ATTENDANCE SUB-TAB
                                ParentAttendanceDonutChart(attendancePct, presentDays)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFECE6FF))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "Past Days Attendance Record:",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF32236F),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        val daysList = listOf("2026-07-22", "2026-07-23", "2026-07-24", "2026-07-25", "2026-07-26")
                                        daysList.forEach { dateStr ->
                                            val isPresent = attendance[dateStr] ?: true
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),
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
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
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

                            2 -> {
                                // SYLLABUS NOTES SUB-TAB
                                val syllabusItems = student.getSyllabusItems()
                                val studentSubjects = student.getSubjects().map { it.subjectName }.distinct()

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
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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

                                        if (isEditingSyllabus) {
                                            // Add Portion Form
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
                                                            text = "No enrolled subjects. Add subjects in Edit Student tab first.",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color.Red
                                                        )
                                                    } else {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .horizontalScroll(rememberScrollState())
                                                        ) {
                                                            studentSubjects.forEach { subj ->
                                                                val isSelected = newSyllabusSubjectInput == subj
                                                                FilterChip(
                                                                    selected = isSelected,
                                                                    onClick = { newSyllabusSubjectInput = subj },
                                                                    label = { Text(subj, fontSize = 11.sp) },
                                                                    colors = FilterChipDefaults.filterChipColors(
                                                                        selectedContainerColor = Color(0xFFEEEBFF),
                                                                        selectedLabelColor = Color(0xFF6D28D9)
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }

                                                    OutlinedTextField(
                                                        value = newSyllabusTopicInput,
                                                        onValueChange = { newSyllabusTopicInput = it },
                                                        label = { Text("Topic Portion Description") },
                                                        placeholder = { Text("e.g. Kinematics") },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )

                                                    Button(
                                                        onClick = {
                                                            val subject = newSyllabusSubjectInput.trim()
                                                            val topic = newSyllabusTopicInput.trim()
                                                            if (subject.isEmpty() || topic.isEmpty()) {
                                                                Toast.makeText(context, "Please enter subject and topic", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                val newItem = SyllabusItem(
                                                                    id = System.currentTimeMillis().toString(),
                                                                    subjectName = subject,
                                                                    topic = topic,
                                                                    isCompleted = false
                                                                )
                                                                val updatedSyllabus = syllabusItems + newItem
                                                                val updatedStudent = student.withSyllabusItems(updatedSyllabus)
                                                                viewModel.updateStudent(updatedStudent)
                                                                newSyllabusTopicInput = ""
                                                                Toast.makeText(context, "Added portion to $subject!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Add Portion Topic")
                                                    }
                                                }
                                            }
                                        }

                                        // Portion List Checklist
                                        if (syllabusItems.isEmpty()) {
                                            Text(
                                                text = "No syllabus notes added yet. Click 'Edit Syllabus' above to add portions.",
                                                color = Color.Gray,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        } else {
                                            val groupedSyllabus = syllabusItems.groupBy { it.subjectName }
                                            groupedSyllabus.forEach { (subjectName, items) ->
                                                Text(
                                                    text = subjectName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF4F46E5),
                                                    modifier = Modifier.padding(top = 6.dp)
                                                )

                                                items.forEach { item ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.White)
                                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable {
                                                                    val updatedList = syllabusItems.map {
                                                                        if (it.id == item.id) it.copy(isCompleted = !it.isCompleted) else it
                                                                    }
                                                                    viewModel.updateStudent(student.withSyllabusItems(updatedList))
                                                                }
                                                        ) {
                                                            Checkbox(
                                                                checked = item.isCompleted,
                                                                onCheckedChange = { checked ->
                                                                    val updatedList = syllabusItems.map {
                                                                        if (it.id == item.id) it.copy(isCompleted = checked) else it
                                                                    }
                                                                    viewModel.updateStudent(student.withSyllabusItems(updatedList))
                                                                },
                                                                colors = CheckboxDefaults.colors(
                                                                    checkedColor = Color(0xFF10B981)
                                                                )
                                                            )
                                                            Text(
                                                                text = item.topic,
                                                                fontSize = 13.sp,
                                                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                                color = if (item.isCompleted) Color.Gray else Color.Black
                                                            )
                                                        }

                                                        if (isEditingSyllabus) {
                                                            IconButton(
                                                                onClick = {
                                                                    val updatedList = syllabusItems.filter { it.id != item.id }
                                                                    viewModel.updateStudent(student.withSyllabusItems(updatedList))
                                                                },
                                                                modifier = Modifier.size(28.dp)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Delete,
                                                                    contentDescription = "Delete Portion",
                                                                    tint = Color.Red,
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
                }

                1 -> {
                    // TAB 1: TEST PAPERS TAB (Matches Screenshot 3)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Co-Upload Testpaper Files (Supabase)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1B4B)
                                )
                                Text(
                                    text = "Tutors and Parents can register high-quality copies of worksheets on Supabase bucket systems.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )

                                // Subject Dropdown Picker
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = attachmentSubjectText,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Select Enrolled Subject...") },
                                        trailingIcon = {
                                            IconButton(onClick = { showTestPaperSubjectMenu = !showTestPaperSubjectMenu }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showTestPaperSubjectMenu = true },
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    DropdownMenu(
                                        expanded = showTestPaperSubjectMenu,
                                        onDismissRequest = { showTestPaperSubjectMenu = false }
                                    ) {
                                        val studentSubjects = student.getSubjects().map { it.subjectName }.distinct()
                                        val displayList = if (studentSubjects.isNotEmpty()) studentSubjects else availableSubjects
                                        displayList.forEach { subj ->
                                            DropdownMenuItem(
                                                text = { Text(subj) },
                                                onClick = {
                                                    attachmentSubjectText = subj
                                                    showTestPaperSubjectMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = attachmentMarksText,
                                        onValueChange = { attachmentMarksText = it },
                                        label = { Text("Marks Obtained") },
                                        placeholder = { Text("e.g. 85") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    OutlinedTextField(
                                        value = attachmentMaxMarksText,
                                        onValueChange = { attachmentMaxMarksText = it },
                                        label = { Text("Max Total") },
                                        placeholder = { Text("100") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                // Photo & File Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { onLaunchCamera() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Capture Photo")
                                    }

                                    Button(
                                        onClick = { filePickerLauncher.launch("image/*") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Upload File")
                                    }
                                }

                                // Photo Preview Box
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selectedImageUri != null) {
                                            AsyncImage(
                                                model = selectedImageUri,
                                                contentDescription = "Selected Image",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else if (capturedBitmap != null) {
                                            Text("Photo captured! Ready for sync.", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                        } else {
                                            Text(
                                                text = "No direct photo loaded yet.",
                                                color = Color.Gray,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }

                                // Upload Button
                                Button(
                                    onClick = {
                                        if (attachmentSubjectText.isEmpty()) {
                                            Toast.makeText(context, "Please select a subject first", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val marks = attachmentMarksText.toDoubleOrNull() ?: 0.0
                                            val maxMarks = attachmentMaxMarksText.toDoubleOrNull() ?: 100.0
                                            val newPaper = TestPaper(
                                                id = System.currentTimeMillis().toString(),
                                                subjectName = attachmentSubjectText,
                                                testDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                                parentPhotoUri = selectedImageUri?.toString() ?: "https://via.placeholder.com/300",
                                                marksObtained = marks,
                                                totalMarks = maxMarks,
                                                status = "Completed"
                                            )
                                            val currentTestPapers = student.getTestPapers()
                                            val updatedStudent = student.withTestPapers(currentTestPapers + newPaper)
                                            viewModel.updateStudent(updatedStudent)

                                            attachmentMarksText = ""
                                            capturedBitmap = null
                                            selectedImageUri = null
                                            Toast.makeText(context, "Test paper submitted!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload File to Supabase", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // History section
                        val testPapers = student.getTestPapers()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Uploaded marksheets history:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF32236F),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (testPapers.isEmpty()) {
                                Text("No marksheets uploaded yet.", color = Color.Gray, fontSize = 13.sp)
                            } else {
                                testPapers.forEach { paper ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.InsertDriveFile,
                                                    contentDescription = null,
                                                    tint = Color(0xFF6200EE),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(paper.subjectName, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                                                    Text("Date: ${paper.testDate}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                }
                                            }
                                            val scoreText = if (paper.marksObtained != null && paper.totalMarks != null) {
                                                "${paper.marksObtained!!.toInt()} / ${paper.totalMarks!!.toInt()}"
                                            } else {
                                                "Uploaded"
                                            }
                                            Text(
                                                text = scoreText,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: EDIT STUDENT TAB (Matches Screenshot 2)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Enrolled Subjects & Marks Ledger Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Enrolled Subjects & Marks Ledger",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1B4B)
                                )
                                Text(
                                    text = "Add, delete, or quick-edit student subjects here. Note: Changes are persistent only on saving below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )

                                // Current Subjects List
                                editSubjectsList.forEachIndexed { index, score ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = score.subjectName,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = if (score.marks == 0.0) "" else score.marks.toInt().toString(),
                                            onValueChange = { newScoreText ->
                                                val scoreVal = newScoreText.toDoubleOrNull() ?: 0.0
                                                editSubjectsList = editSubjectsList.toMutableList().also { list ->
                                                    list[index] = score.copy(marks = scoreVal)
                                                }
                                            },
                                            label = { Text("Score") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.width(90.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        IconButton(onClick = {
                                            editSubjectsList = editSubjectsList.filterIndexed { i, _ -> i != index }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Subject", tint = Color.Red)
                                        }
                                    }
                                }

                                // Add Subject Dropdown Row
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedSubjectDropdown,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Add New Subject Registration...") },
                                        trailingIcon = {
                                            IconButton(onClick = { showAddSubjectMenu = !showAddSubjectMenu }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showAddSubjectMenu = true },
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    DropdownMenu(
                                        expanded = showAddSubjectMenu,
                                        onDismissRequest = { showAddSubjectMenu = false }
                                    ) {
                                        availableSubjects.forEach { newSubName ->
                                            DropdownMenuItem(
                                                text = { Text(newSubName) },
                                                onClick = {
                                                    selectedSubjectDropdown = newSubName
                                                    showAddSubjectMenu = false
                                                    if (editSubjectsList.none { it.subjectName == newSubName }) {
                                                        val newMark = SubjectMark(
                                                            subjectName = newSubName,
                                                            marks = 0.0,
                                                            maxMarks = 100.0,
                                                            testDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                                            testType = "Monthly"
                                                        )
                                                        editSubjectsList = editSubjectsList + newMark
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Database Core Parameters Form
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Core Registry Parameters (Database)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1B4B)
                                )

                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Student Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = editPhone,
                                    onValueChange = { editPhone = it },
                                    label = { Text("Parent Mobile Phone") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = editFlatNo,
                                    onValueChange = { editFlatNo = it },
                                    label = { Text("Parent Flat Number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editClass,
                                        onValueChange = { editClass = it },
                                        label = { Text("Standard Class") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    OutlinedTextField(
                                        value = editBoard,
                                        onValueChange = { editBoard = it },
                                        label = { Text("Secondary Board") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = editSchool,
                                    onValueChange = { editSchool = it },
                                    label = { Text("School Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = editEmail,
                                    onValueChange = { editEmail = it },
                                    label = { Text("Email Address") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val updatedStudent = student.copy(
                                            name = editName.ifEmpty { student.name },
                                            parentPhone = editPhone,
                                            parentFlatNo = editFlatNo,
                                            standard = editClass,
                                            board = editBoard,
                                            school = editSchool,
                                            studentEmail = editEmail
                                        ).withSubjects(editSubjectsList)

                                        viewModel.updateStudent(updatedStudent)
                                        Toast.makeText(context, "Registry updated successfully!", Toast.LENGTH_SHORT).show()
                                        activeSubViewTab = 0
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Registry", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        oldPasswordInput = ""
                                        newPasswordInput = ""
                                        confirmPasswordInput = ""
                                        showChangePasswordDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("change_password_profile_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF423870)),
                                    border = BorderStroke(1.dp, Color(0xFF7A68E6)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF7A68E6))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change Parent Password", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showDeleteConfirmDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    border = BorderStroke(1.dp, Color.Red),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Delete Student Profile", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out Confirmation",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1B4B)
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to log out from the parent dashboard?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4B5563)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logOut()
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Student Profile") },
            text = { Text("Are you sure you want to permanently delete ${student.name} from the registry?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteStudent(student.slNo)
                        Toast.makeText(context, "Student deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF423870)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Change Parent Password",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF423870)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Default password is student name (${student.name}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = oldPasswordInput,
                        onValueChange = { oldPasswordInput = it },
                        label = { Text("Current Password") },
                        placeholder = { Text("Enter current password") },
                        singleLine = true,
                        visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                                Icon(
                                    imageVector = if (oldPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (oldPasswordVisible) "Hide password" else "Show password",
                                    tint = Color.Gray
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("current_password_input")
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Password") },
                        placeholder = { Text("Enter new password") },
                        singleLine = true,
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
                                    tint = Color.Gray
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_password_input")
                    )

                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it },
                        label = { Text("Confirm New Password") },
                        placeholder = { Text("Re-enter new password") },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                    tint = Color.Gray
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldPasswordInput.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter your current password", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPasswordInput.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter a new password", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPasswordInput.trim().length < 3) {
                            Toast.makeText(context, "New password must be at least 3 characters", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPasswordInput.trim() != confirmPasswordInput.trim()) {
                            Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.changeParentPassword(
                            phone = student.parentPhone,
                            oldPass = oldPasswordInput,
                            newPass = newPasswordInput,
                            onSuccess = {
                                Toast.makeText(context, "Password changed successfully!", Toast.LENGTH_LONG).show()
                                showChangePasswordDialog = false
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF423870)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_password_button")
                ) {
                    Text("Update Password", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// COMPOSABLE COMPONENT: Performance Marks Line Chart for Parent Dashboard
@Composable
private fun ParentPerformanceMarksLineChart(subjects: List<SubjectMark>) {
    val subjectNames = remember(subjects) {
        subjects.map { it.subjectName }.distinct().ifEmpty { listOf("Physics", "Mathematics") }
    }

    var selectedChartSubject by remember(subjectNames) {
        mutableStateOf(subjectNames.first())
    }

    val filteredScores = remember(subjects, selectedChartSubject) {
        subjects.filter { it.subjectName.equals(selectedChartSubject, ignoreCase = true) }
            .ifEmpty {
                listOf(
                    SubjectMark(selectedChartSubject, 0.0, 100.0, "15-07-2026", "Monthly"),
                    SubjectMark(selectedChartSubject, 20.0, 100.0, "2026-07-23", "Unit Test"),
                    SubjectMark(selectedChartSubject, 30.0, 100.0, "2026-07-26", "Unit Test")
                )
            }
    }

    val avgAccuracy = remember(filteredScores) {
        if (filteredScores.isNotEmpty()) {
            (filteredScores.map { if (it.maxMarks > 0) (it.marks / it.maxMarks * 100) else 0.0 }.average()).toInt()
        } else {
            0
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECE6FF))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Marks Trend ($selectedChartSubject)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF32236F)
                )

                Box(
                    modifier = Modifier
                        .background(Color(0xFFEDE9FE), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$avgAccuracy% avg accuracy",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6D28D9),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subject Pill Filter Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjectNames.forEach { subj ->
                    val isSelected = subj.equals(selectedChartSubject, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedChartSubject = subj },
                        label = { Text(subj, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6D28D9),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF3E8FF),
                            labelColor = Color(0xFF6D28D9)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Line Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Draw grid lines
                    val gridColor = Color(0xFFF1F5F9)
                    for (i in 0..3) {
                        val y = height * (i / 3f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (filteredScores.size >= 2) {
                        val points = filteredScores.mapIndexed { idx, score ->
                            val x = width * (idx.toFloat() / (filteredScores.size - 1).coerceAtLeast(1))
                            val pct = if (score.maxMarks > 0) (score.marks / score.maxMarks).toFloat() else 0f
                            val y = height * (1f - pct.coerceIn(0f, 1f))
                            Offset(x, y)
                        }

                        // Draw lines
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = Color(0xFF7C3AED),
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = 3.dp.toPx()
                            )
                        }

                        // Draw points
                        points.forEach { point ->
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = Color(0xFF7C3AED),
                                radius = 4.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }
            }

            // Dates Label Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                filteredScores.forEach { score ->
                    val displayDate = score.testDate.takeLast(5).ifEmpty { "07-23" }
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// COMPOSABLE COMPONENT: Attendance Donut Chart for Parent Dashboard
@Composable
private fun ParentAttendanceDonutChart(pct: Float, presentDays: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECE6FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Attendance Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF32236F)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()

                    // Track Background
                    drawArc(
                        color = Color(0xFFFFEBEE),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )

                    // Progress Arc
                    drawArc(
                        color = Color(0xFF10B981),
                        startAngle = -90f,
                        sweepAngle = 360f * pct.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(pct * 100).toInt()}%",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "$presentDays Days",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
