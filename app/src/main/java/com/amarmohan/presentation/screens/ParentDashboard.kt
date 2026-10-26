package com.amarmohan.presentation.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import coil.compose.AsyncImage
import com.amarmohan.domain.model.*
import com.amarmohan.presentation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
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
                Text(
                    text = "Loading verified student profile...",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    val student = currentStudent!!

    // Active sub-view tabs (0: Dashboard, 1: Marksheets, 2: Student Information)
    var activeSubViewTab by remember { mutableStateOf(0) }
    // Dashboard sub-tabs (0: Performance, 1: Attendance, 2: Syllabus Notes)
    var activeDashboardSubTab by remember { mutableStateOf(0) }

    // Settings dropdown
    var showSettingsMenu by remember { mutableStateOf(false) }

    // Marksheet preview dialog
    var previewPaperUrl by remember { mutableStateOf<String?>(null) }
    var previewPaperTitle by remember { mutableStateOf("") }

    // Test Paper Query Dialog States
    var querySelectedPaper by remember { mutableStateOf<TestPaper?>(null) }
    var queryText by remember { mutableStateOf("") }

    // Confirmation dialog state
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Change Password Dialog States
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var oldPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val subjects = student.getSubjects()
    val testPapers = student.getTestPapers()
    val attendance = student.getAttendance()
    val presentDays = attendance.count { it.value }
    val totalDays = attendance.size.coerceAtLeast(1)
    val attendancePct = presentDays.toFloat() / totalDays.toFloat()

    // Performance calculated from all tutor-uploaded marksheets & subject scores
    val allScoreEntries = remember(subjects, testPapers) {
        val sScores = subjects.filter { it.maxMarks > 0 }.map { it.marks / it.maxMarks }
        val pScores = testPapers.filter { (it.totalMarks ?: 0.0) > 0.0 && (it.marksObtained != null) }
            .map { it.marksObtained!! / it.totalMarks!! }
        sScores + pScores
    }

    val avgScorePct = if (allScoreEntries.isNotEmpty()) {
        (allScoreEntries.average() * 100).toInt()
    } else if (subjects.isNotEmpty()) {
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
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Log Out") },
                                onClick = {
                                    showSettingsMenu = false
                                    showLogoutConfirmDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // SIBLING QUICK-SWITCH BAR
            if (siblingStudents.size > 1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDFF)),
                    border = BorderStroke(1.dp, Color(0xFFDDD0FC))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color(0xFF5138B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Registered Children (${siblingStudents.size}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5138B8)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            siblingStudents.forEach { sib ->
                                val isSelected = sib.slNo == student.slNo
                                Surface(
                                    onClick = { viewModel.selectParentStudent(sib.slNo) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) Color(0xFF6200EE) else Color.White,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF6200EE) else Color(0xFFDCD6FF)
                                    ),
                                    modifier = Modifier.testTag("sibling_chip_${sib.slNo}")
                                ) {
                                    Text(
                                        text = sib.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF5138B8),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // STUDENT HEADER SECTION (Matching App Header & Showing Parent Details)
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
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF32236F)
                        )

                        // Parent name and number displayed directly below student name
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
                                    tint = Color(0xFF6D28D9),
                                    modifier = Modifier.size(15.dp)
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

                        val schoolInfo = listOf(student.standard, student.board, student.school)
                            .filter { it.isNotEmpty() }
                            .joinToString(" • ")
                        Text(
                            text = schoolInfo.ifEmpty { "Class N/A" },
                            style = MaterialTheme.typography.bodySmall,
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

            // HORIZONTAL SUB-VIEW TABS (Dashboard | Marksheets | Student Information)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Dashboard", "Marksheets", "Student Information").forEachIndexed { idx, label ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { activeSubViewTab = idx }
                            .padding(vertical = 6.dp, horizontal = 12.dp)
                            .testTag("parent_tab_$idx")
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
                    // ==========================================
                    // TAB 0: MAIN DASHBOARD TAB
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // TOP SUMMARY CARDS (Overall Performance & Attendance)
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

                        // CATEGORY PILL ROW (Performance | Attendance Calendar | Syllabus Notes)
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
                                modifier = Modifier.defaultMinSize(minWidth = 120.dp),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = "Performance",
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Performance", fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                }
                            }

                            // 2. Attendance Calendar Button
                            Button(
                                onClick = { activeDashboardSubTab = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeDashboardSubTab == 1) Color.White else Color.Transparent,
                                    contentColor = if (activeDashboardSubTab == 1) Color(0xFF6D28D9) else Color(0xFF5138B8)
                                ),
                                elevation = if (activeDashboardSubTab == 1) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 120.dp),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Attendance Calendar",
                                        modifier = Modifier.size(22.dp)
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
                                modifier = Modifier.defaultMinSize(minWidth = 120.dp),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "Syllabus Notes",
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Syllabus", fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // SUB-CATEGORY CONTENT SWITCH
                        when (activeDashboardSubTab) {
                            0 -> {
                                // PERFORMANCE SUB-TAB: Graph + Records Calculated from Tutor-Uploaded Marksheets
                                ParentPerformanceMarksLineChart(subjects = subjects, testPapers = testPapers)

                                val studentChosenEvalSubjects = remember(subjects, testPapers) {
                                    (subjects.map { it.subjectName } + testPapers.map { it.subjectName }).filter { it.isNotBlank() }.distinct()
                                }
                                val expandedParentEvalSubjects = remember { mutableStateMapOf<String, Boolean>() }

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Subject-wise Marks Visual (All Chosen Subjects):",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF32236F),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (studentChosenEvalSubjects.isNotEmpty()) {
                                            Text("${studentChosenEvalSubjects.size} Subjects", fontSize = 11.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (studentChosenEvalSubjects.isEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFECE6FF))
                                        ) {
                                            Text(
                                                text = "No test marks recorded by tutor yet.",
                                                modifier = Modifier.padding(16.dp),
                                                color = Color.Gray,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        // Render individual dropdown for all subjects chosen by the student (Subject 1 dropdown, Subject 2 dropdown, etc.)
                                        studentChosenEvalSubjects.forEachIndexed { index, subjName ->
                                            val isExpanded = expandedParentEvalSubjects[subjName] ?: true
                                            val subjRecords = subjects.filter { it.subjectName.equals(subjName, ignoreCase = true) }
                                            val tpRecords = testPapers.filter { it.subjectName.equals(subjName, ignoreCase = true) }
                                            val primaryScore = subjRecords.firstOrNull()
                                            val marksVal = primaryScore?.marks ?: (tpRecords.firstOrNull()?.marksObtained ?: 0.0)
                                            val maxMarksVal = primaryScore?.maxMarks ?: (tpRecords.firstOrNull()?.totalMarks ?: 100.0)
                                            val pct = if (maxMarksVal > 0) ((marksVal / maxMarksVal) * 100).toInt() else 0
                                            val statusColor = if (pct >= 80) Color(0xFF10B981) else if (pct >= 60) Color(0xFF7C3AED) else Color(0xFFF59E0B)

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
                                                            .clickable { expandedParentEvalSubjects[subjName] = !isExpanded }
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
                                                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
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
                                                                    text = if (isExpanded) "Tap to collapse" else "Tap to view subject marks visual",
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
                                                                tint = Color(0xFF7C3AED)
                                                            )
                                                        }
                                                    }

                                                    // Dropdown Content
                                                    if (isExpanded) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(14.dp),
                                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            // Visual Marks Gauge
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

                                                            // Evaluation Details
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    text = "Evaluation: ${primaryScore?.testType ?: "Monthly Assessment"}",
                                                                    fontSize = 11.sp,
                                                                    color = Color(0xFF475569),
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                                Text(
                                                                    text = "Date: ${primaryScore?.testDate?.ifEmpty { "15-07-2026" } ?: (tpRecords.firstOrNull()?.testDate?.ifEmpty { "15-07-2026" } ?: "15-07-2026")}",
                                                                    fontSize = 11.sp,
                                                                    color = Color(0xFF64748B)
                                                                )
                                                            }

                                                            if (tpRecords.isNotEmpty()) {
                                                                Text("Test Paper Records for this Subject:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                                                tpRecords.forEach { tp ->
                                                                    Row(
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                                    ) {
                                                                        Text("Paper (${tp.testDate})", fontSize = 10.sp, color = Color(0xFF334155))
                                                                        Text("${tp.marksObtained?.toInt() ?: 0}/${tp.totalMarks?.toInt() ?: 0}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
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
                                            viewModel.openSmsApp(context, tutorPhoneToContact, "Hello Amarmohan Sir, regarding student ${student.name}.")
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
                                // ATTENDANCE SUB-TAB WITH CALENDAR
                                ParentAttendanceDonutChart(attendancePct, presentDays)

                                // Interactive Monthly Attendance Calendar
                                ParentAttendanceCalendar(attendance = attendance)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFECE6FF))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "Recent Attendance Entries:",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF32236F),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        val sortedDates = attendance.keys.sortedDescending().take(10)
                                        if (sortedDates.isEmpty()) {
                                            Text("No attendance records logged yet.", color = Color.Gray, fontSize = 13.sp)
                                        } else {
                                            sortedDates.forEach { dateStr ->
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
                            }

                            2 -> {
                                // SYLLABUS NOTES SUB-TAB (Read-Only)
                                val syllabusItems = student.getSyllabusItems()

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
                                                    text = "Curriculum & Syllabus Portions",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF32236F),
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            }
                                        }

                                        if (syllabusItems.isEmpty()) {
                                            Text(
                                                text = "No syllabus topics assigned by tutor yet.",
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
                                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                                contentDescription = null,
                                                                tint = if (item.isCompleted) Color(0xFF10B981) else Color.Gray,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = item.topic,
                                                                fontSize = 13.sp,
                                                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                                color = if (item.isCompleted) Color.Gray else Color.Black
                                                            )
                                                        }

                                                        if (item.isCompleted) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("Completed", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
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
                    // ==========================================
                    // TAB 1: OFFICIAL MARKSHEETS (TUTOR-UPLOADED ONLY)
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Information Banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9FE)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF6D28D9),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Official Marksheets & Test Papers",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF4C1D95)
                                    )
                                    Text(
                                        text = "All evaluation results and test papers are uploaded directly by the tutor.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF5B21B6)
                                    )
                                }
                            }
                        }

                        val allUploadedPapers = testPapers
                        if (allUploadedPapers.isEmpty() && subjects.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No marksheets uploaded yet.",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Evaluated papers will appear here once uploaded by the tutor.",
                                        fontSize = 12.sp,
                                        color = Color.LightGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            var selectedMarksheetSubjectFilter by remember { mutableStateOf("All Chosen Subjects") }
                            var showMarksheetSubjectDropdown by remember { mutableStateOf(false) }
                            val studentChosenMarksheetSubjects = remember(subjects, allUploadedPapers) {
                                (subjects.map { it.subjectName } + allUploadedPapers.map { it.subjectName }).filter { it.isNotBlank() }.distinct()
                            }

                            if (studentChosenMarksheetSubjects.isNotEmpty()) {
                                // Subject-wise dropdown selector for all subjects chosen by the student
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedCard(
                                        onClick = { showMarksheetSubjectDropdown = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("Filter Marksheets by Subject", fontSize = 10.sp, color = Color.Gray)
                                                    Text(
                                                        text = selectedMarksheetSubjectFilter,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1E1B4B)
                                                    )
                                                }
                                            }
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF6D28D9))
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showMarksheetSubjectDropdown,
                                        onDismissRequest = { showMarksheetSubjectDropdown = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("All Chosen Subjects (${studentChosenMarksheetSubjects.size})", fontWeight = if (selectedMarksheetSubjectFilter == "All Chosen Subjects") FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                selectedMarksheetSubjectFilter = "All Chosen Subjects"
                                                showMarksheetSubjectDropdown = false
                                            }
                                        )
                                        HorizontalDivider()
                                        studentChosenMarksheetSubjects.forEach { subj ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        subj,
                                                        fontWeight = if (selectedMarksheetSubjectFilter == subj) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedMarksheetSubjectFilter == subj) Color(0xFF6D28D9) else Color.Unspecified
                                                    )
                                                },
                                                onClick = {
                                                    selectedMarksheetSubjectFilter = subj
                                                    showMarksheetSubjectDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            val filteredUploadedPapers = if (selectedMarksheetSubjectFilter == "All Chosen Subjects") {
                                allUploadedPapers
                            } else {
                                allUploadedPapers.filter { it.subjectName.equals(selectedMarksheetSubjectFilter, ignoreCase = true) }
                            }

                            val filteredLedgerSubjects = if (selectedMarksheetSubjectFilter == "All Chosen Subjects") {
                                subjects
                            } else {
                                subjects.filter { it.subjectName.equals(selectedMarksheetSubjectFilter, ignoreCase = true) }
                            }

                            if (filteredUploadedPapers.isEmpty() && filteredLedgerSubjects.isEmpty()) {
                                Text(
                                    text = "No marksheets or evaluations found for $selectedMarksheetSubjectFilter.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            // Render all tutor marksheets
                            filteredUploadedPapers.forEach { paper ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFEDE9FE)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.InsertDriveFile,
                                                        contentDescription = null,
                                                        tint = Color(0xFF6D28D9),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = paper.subjectName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = Color(0xFF1E1B4B)
                                                    )
                                                    Text(
                                                        text = "Test Date: ${paper.testDate}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }

                                            if (paper.marksObtained != null && paper.totalMarks != null) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "${paper.marksObtained!!.toInt()} / ${paper.totalMarks!!.toInt()}",
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF10B981),
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    val pct = if (paper.totalMarks!! > 0) ((paper.marksObtained!! / paper.totalMarks!!) * 100).toInt() else 0
                                                    Text(
                                                        text = "$pct% Score",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF059669),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        // Status badge and preview button if photo available
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFECFDF5), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "✓ Evaluated by Tutor",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF065F46)
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        querySelectedPaper = paper
                                                        queryText = ""
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4F46E5)),
                                                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Ask Query", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                val photoUrl = (paper.parentPhotoUri ?: "").ifEmpty { paper.tutorPhotoUri ?: "" }
                                                if (photoUrl.isNotBlank() && photoUrl != "https://via.placeholder.com/300") {
                                                    Button(
                                                        onClick = {
                                                            previewPaperUrl = photoUrl
                                                            previewPaperTitle = "${paper.subjectName} (${paper.testDate})"
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("View Paper", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Show subject evaluation scores
                            filteredLedgerSubjects.forEach { sm ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEDE9FE)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Grade,
                                                    contentDescription = null,
                                                    tint = Color(0xFF6D28D9),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = sm.subjectName,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = Color(0xFF1E1B4B)
                                                )
                                                Text(
                                                    text = "${sm.testType} Evaluation",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${sm.marks.toInt()} / ${sm.maxMarks.toInt()}",
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF7C3AED),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            val pct = if (sm.maxMarks > 0) ((sm.marks / sm.maxMarks) * 100).toInt() else 0
                                            Text(
                                                text = "$pct% Acc",
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

                2 -> {
                    // ==========================================
                    // TAB 2: STUDENT INFORMATION (READ-ONLY OFFICIAL INFO SECTION)
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Notice Banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                            border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF6D28D9),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Official Student Information",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF4C1D95)
                                    )
                                    Text(
                                        text = "Registered and maintained by Tutor (Amarmohan Sir). Contact tutor for any updates.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6D28D9)
                                    )
                                }
                            }
                        }

                        // 1. Academic Details Card
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
                                    text = "Academic Profile",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1B4B)
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                InfoRow(label = "Student Name", value = student.name)
                                InfoRow(label = "Date of Birth", value = student.birthDate.ifEmpty { "N/A" })
                                InfoRow(label = "Class / Standard", value = student.standard.ifEmpty { "N/A" })
                                InfoRow(label = "Education Board", value = student.board.ifEmpty { "CBSE / ICSE / State" })
                                InfoRow(label = "School", value = student.school.ifEmpty { "N/A" })
                                if (student.studentEmail.isNotBlank()) {
                                    InfoRow(label = "Student Email", value = student.studentEmail)
                                }
                            }
                        }

                        // 2. Parent & Contact Details Card
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
                                    text = "Parent & Contact Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1B4B)
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                InfoRow(label = "Parent / Guardian", value = student.parentName.ifEmpty { "Parent" })
                                InfoRow(label = "Contact Number", value = student.parentPhone.ifEmpty { "N/A" })
                                InfoRow(label = "Residence Flat No.", value = student.parentFlatNo.ifEmpty { "N/A" })
                            }
                        }

                        // 3. Enrolled Subjects & Marks Ledger Card
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Enrolled Subjects & Marks Ledger",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E1B4B)
                                    )
                                    if (subjects.isNotEmpty()) {
                                        Text("${subjects.size} Subjects", fontSize = 11.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                if (subjects.isEmpty()) {
                                    Text("No specific subjects enrolled.", color = Color.Gray, fontSize = 13.sp)
                                } else {
                                    val expandedLedgerCardSubjects = remember { mutableStateMapOf<String, Boolean>() }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Subject-wise Marks Visual (All Chosen Subjects):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF32236F))
                                            Text("${subjects.size} Subjects", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                                        }

                                        // Render individual dropdown for all subjects chosen by the student (Subject 1 dropdown, Subject 2 dropdown, etc.)
                                        subjects.forEachIndexed { index, sm ->
                                            val isExpanded = expandedLedgerCardSubjects[sm.subjectName] ?: true
                                            val pct = if (sm.maxMarks > 0) ((sm.marks / sm.maxMarks) * 100).toInt() else 0
                                            val statusColor = if (pct >= 80) Color(0xFF10B981) else if (pct >= 60) Color(0xFF7C3AED) else Color(0xFFF59E0B)

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                border = BorderStroke(1.dp, if (isExpanded) Color(0xFFC7D2FE) else Color(0xFFEDE9FE)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    // Dropdown Header Row
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(if (isExpanded) Color(0xFFF5F3FF) else Color.White)
                                                            .clickable { expandedLedgerCardSubjects[sm.subjectName] = !isExpanded }
                                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(22.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(0xFFEDE9FE)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Column {
                                                                Text(
                                                                    text = "Subject ${index + 1}: ${sm.subjectName}",
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 13.sp,
                                                                    color = Color(0xFF1E1B4B)
                                                                )
                                                                Text(
                                                                    text = if (isExpanded) "Tap to collapse" else "Tap to view subject marks visual",
                                                                    fontSize = 9.sp,
                                                                    color = Color.Gray
                                                                )
                                                            }
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = statusColor.copy(alpha = 0.12f),
                                                                modifier = Modifier.padding(end = 4.dp)
                                                            ) {
                                                                Text(
                                                                    text = "${sm.marks.toInt()}/${sm.maxMarks.toInt()} ($pct%)",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = statusColor,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                            Icon(
                                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                                contentDescription = "Toggle Dropdown",
                                                                tint = Color(0xFF7C3AED),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }

                                                    // Dropdown Content
                                                    if (isExpanded) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(12.dp),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column {
                                                                    Text("Marks Obtained", fontSize = 10.sp, color = Color.Gray)
                                                                    Row(verticalAlignment = Alignment.Bottom) {
                                                                        Text(
                                                                            text = "${sm.marks.toInt()}",
                                                                            fontSize = 20.sp,
                                                                            fontWeight = FontWeight.Black,
                                                                            color = Color(0xFF1E1B4B)
                                                                        )
                                                                        Text(
                                                                            text = " / ${sm.maxMarks.toInt()}",
                                                                            fontSize = 12.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color.Gray,
                                                                            modifier = Modifier.padding(bottom = 1.dp, start = 2.dp)
                                                                        )
                                                                    }
                                                                }

                                                                Column(horizontalAlignment = Alignment.End) {
                                                                    val gradeTag = if (pct >= 90) "A+ Outstanding" else if (pct >= 75) "A Excellent" else if (pct >= 60) "B Good" else "Needs Improvement"
                                                                    Surface(
                                                                        shape = RoundedCornerShape(6.dp),
                                                                        color = statusColor.copy(alpha = 0.15f)
                                                                    ) {
                                                                        Text(
                                                                            text = gradeTag,
                                                                            fontSize = 10.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = statusColor,
                                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                                        )
                                                                    }
                                                                    Text("Accuracy: $pct%", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 1.dp))
                                                                }
                                                            }

                                                            LinearProgressIndicator(
                                                                progress = (pct / 100f).coerceIn(0f, 1f),
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(6.dp)
                                                                    .clip(RoundedCornerShape(3.dp)),
                                                                color = statusColor,
                                                                trackColor = Color(0xFFF1F5F9)
                                                            )

                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text("Status: Recorded", fontSize = 10.sp, color = Color(0xFF475569))
                                                                Text("Verified Evaluation", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF10B981))
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



                        // 4. Tuition Fee Ledger Card
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
                                    text = "Tuition Fee Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1B4B)
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                InfoRow(label = "Monthly Tuition Fee", value = "₹${student.tuitionFee.toInt()}")
                                InfoRow(label = "Current Outstanding Due", value = "₹${student.dueAmount.toInt()}")
                                InfoRow(
                                    label = "Payment Status",
                                    value = if (student.isFeesPaid || student.dueAmount <= 0.0) "Cleared ✓" else "Pending"
                                )
                            }
                        }

                        // Security & Account Settings
                        OutlinedButton(
                            onClick = {
                                oldPasswordInput = ""
                                newPasswordInput = ""
                                confirmPasswordInput = ""
                                showChangePasswordDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .testTag("change_password_profile_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF423870)),
                            border = BorderStroke(1.dp, Color(0xFF7A68E6)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF7A68E6))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Parent Login Password", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "🧿",
                fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )
        }
    }

    // Marksheet Paper Preview Dialog
    if (previewPaperUrl != null) {
        AlertDialog(
            onDismissRequest = { previewPaperUrl = null },
            title = {
                Text(
                    text = previewPaperTitle.ifEmpty { "Marksheet Evaluation Paper" },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = previewPaperUrl,
                        contentDescription = "Marksheet Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { previewPaperUrl = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Test Paper SMS Query Dialog
    if (querySelectedPaper != null) {
        val paper = querySelectedPaper!!
        AlertDialog(
            onDismissRequest = { querySelectedPaper = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Query on ${paper.subjectName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Direct SMS will be automatically sent to Tutor (9986736360).",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Quick option chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = { queryText = "Please review my child's test evaluation marks." },
                            label = { Text("Review Marks", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = { queryText = "Requesting clarification on question correction." },
                            label = { Text("Correction Doubt", fontSize = 11.sp) }
                        )
                    }

                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        label = { Text("Describe your query / doubt") },
                        placeholder = { Text("e.g., Request re-evaluating section B marks...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (queryText.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter your query before sending", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.sendTestPaperQuerySms(
                            context = context,
                            tutorPhone = "9986736360",
                            studentName = student.name,
                            subject = paper.subjectName,
                            queryText = queryText.trim()
                        )
                        viewModel.addReminder(
                            studentSlNo = student.slNo,
                            text = "Query on ${paper.subjectName}: ${queryText.trim()}",
                            isImportant = true,
                            isByParent = true
                        )
                        querySelectedPaper = null
                        queryText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send SMS to Tutor")
                }
            },
            dismissButton = {
                TextButton(onClick = { querySelectedPaper = null }) {
                    Text("Cancel")
                }
            }
        )
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

// COMPOSABLE HELPER: Simple read-only key-value information row
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E1B4B)
        )
    }
}

// COMPOSABLE COMPONENT: Attendance Monthly Calendar for Parent Dashboard
@Composable
fun ParentAttendanceCalendar(
    attendance: Map<String, Boolean>,
    modifier: Modifier = Modifier
) {
    var calendarMonthOffset by remember { mutableStateOf(0) }
    val baseCal = remember(calendarMonthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, calendarMonthOffset)
        cal
    }

    val currentYear = baseCal.get(Calendar.YEAR)
    val currentMonth = baseCal.get(Calendar.MONTH) // 0-based
    val monthName = remember(baseCal) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(baseCal.time)
    }

    val daysInMonth = remember(baseCal) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonth)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(baseCal) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
    }

    var selectedDateKey by remember { mutableStateOf<String?>(null) }

    val monthPresentCount = remember(attendance, currentYear, currentMonth) {
        val prefix = String.format(Locale.getDefault(), "%04d-%02d", currentYear, currentMonth + 1)
        attendance.entries.count { it.key.startsWith(prefix) && it.value }
    }
    val monthAbsentCount = remember(attendance, currentYear, currentMonth) {
        val prefix = String.format(Locale.getDefault(), "%04d-%02d", currentYear, currentMonth + 1)
        attendance.entries.count { it.key.startsWith(prefix) && !it.value }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECE6FF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Header with Prev/Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { calendarMonthOffset -= 1 },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = Color(0xFF6D28D9)
                    )
                }

                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF32236F)
                )

                IconButton(
                    onClick = { calendarMonthOffset += 1 },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = Color(0xFF6D28D9)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day of week headers
            val daysOfWeek = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            for (r in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (c in 0..6) {
                        val cellIndex = r * 7 + c
                        val dayNum = cellIndex - firstDayOfWeek + 1
                        if (dayNum in 1..daysInMonth) {
                            val dateKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", currentYear, currentMonth + 1, dayNum)
                            val attStatus = attendance[dateKey]
                            val isSelected = selectedDateKey == dateKey

                            val (bg, textColor) = when (attStatus) {
                                true -> Color(0xFF10B981) to Color.White
                                false -> Color(0xFFEF4444) to Color.White
                                null -> if (isSelected) Color(0xFFEDE9FE) to Color(0xFF6D28D9) else Color.Transparent to Color.DarkGray
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(bg)
                                    .then(
                                        if (isSelected && attStatus != null) Modifier.border(2.dp, Color(0xFF4338CA), CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedDateKey = dateKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$dayNum",
                                    fontSize = 12.sp,
                                    fontWeight = if (attStatus != null || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Calendar Legend & Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAF9FF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Present ($monthPresentCount)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF065F46))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Absent ($monthAbsentCount)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF991B1B))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.LightGray))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("No Class", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Selected date inspection card
            if (selectedDateKey != null) {
                val dateKey = selectedDateKey!!
                val status = attendance[dateKey]
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (status) {
                            true -> Color(0xFFECFDF5)
                            false -> Color(0xFFFEF2F2)
                            null -> Color(0xFFF8FAFC)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date: $dateKey",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E1B4B)
                        )
                        val (statusLabel, statusColor) = when (status) {
                            true -> "Present ✅" to Color(0xFF10B981)
                            false -> "Absent ❌" to Color(0xFFEF4444)
                            null -> "No class recorded" to Color.Gray
                        }
                        Text(
                            text = statusLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

// COMPOSABLE COMPONENT: Performance Marks Line Chart for Parent Dashboard
@Composable
private fun ParentPerformanceMarksLineChart(
    subjects: List<SubjectMark>,
    testPapers: List<TestPaper>
) {
    val subjectNames = remember(subjects, testPapers) {
        val sNames = subjects.map { it.subjectName.trim() }
        val tpNames = testPapers.map { it.subjectName.trim() }
        (sNames + tpNames).filter { it.isNotBlank() }.distinct()
    }

    if (subjectNames.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFECE6FF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No test marks recorded yet.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    var selectedChartSubject by remember(subjectNames) {
        mutableStateOf(subjectNames.first())
    }

    val filteredScores = remember(subjects, testPapers, selectedChartSubject) {
        val sList = subjects.filter { it.subjectName.equals(selectedChartSubject, ignoreCase = true) }
        val tpList = testPapers.filter { it.subjectName.equals(selectedChartSubject, ignoreCase = true) && it.marksObtained != null && it.totalMarks != null }
            .map { tp ->
                SubjectMark(
                    subjectName = tp.subjectName,
                    marks = tp.marksObtained!!,
                    maxMarks = tp.totalMarks!!,
                    testDate = tp.testDate,
                    testType = "Paper"
                )
            }
        (sList + tpList).distinctBy { "${it.testDate}_${it.marks}_${it.maxMarks}" }
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

            if (filteredScores.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No test marks recorded for $selectedChartSubject yet.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            } else {
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

                            // Draw points and marks badges
                            points.forEachIndexed { idx, point ->
                                val score = filteredScores[idx]

                                drawCircle(
                                    color = Color(0x337C3AED),
                                    radius = 10.dp.toPx(),
                                    center = point
                                )
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

                                // Draw marks badge
                                drawIntoCanvas { canvas ->
                                    val marksText = if (score.marks % 1.0 == 0.0 && score.maxMarks % 1.0 == 0.0) {
                                        "${score.marks.toInt()}/${score.maxMarks.toInt()}"
                                    } else {
                                        "${score.marks}/${score.maxMarks}"
                                    }

                                    val textPaint = Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 9.5f * density
                                        typeface = Typeface.DEFAULT_BOLD
                                        isAntiAlias = true
                                        textAlign = Paint.Align.CENTER
                                    }

                                    val bgPaint = Paint().apply {
                                        color = android.graphics.Color.parseColor("#4C1D95")
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

                                    val badgeCenterY = if (point.y < 28f * density) point.y + 15f * density else point.y - 12f * density
                                    val rectLeft = (point.x - textWidth / 2f - padH).coerceIn(2f * density, size.width - textWidth - padH * 2 - 2f * density)
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
                        } else if (filteredScores.size == 1) {
                            val score = filteredScores.first()
                            val pct = if (score.maxMarks > 0) (score.marks / score.maxMarks).toFloat() else 0f
                            val center = Offset(width / 2f, height * (1f - pct.coerceIn(0f, 1f)))

                            drawCircle(
                                color = Color(0x337C3AED),
                                radius = 10.dp.toPx(),
                                center = center
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = center
                            )
                            drawCircle(
                                color = Color(0xFF7C3AED),
                                radius = 4.dp.toPx(),
                                center = center
                            )

                            // Draw marks badge for single score
                            drawIntoCanvas { canvas ->
                                val marksText = if (score.marks % 1.0 == 0.0 && score.maxMarks % 1.0 == 0.0) {
                                    "${score.marks.toInt()}/${score.maxMarks.toInt()}"
                                } else {
                                    "${score.marks}/${score.maxMarks}"
                                }

                                val textPaint = Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 9.5f * density
                                    typeface = Typeface.DEFAULT_BOLD
                                    isAntiAlias = true
                                    textAlign = Paint.Align.CENTER
                                }

                                val bgPaint = Paint().apply {
                                    color = android.graphics.Color.parseColor("#4C1D95")
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

                                val badgeCenterY = if (center.y < 28f * density) center.y + 15f * density else center.y - 12f * density
                                val rectLeft = (center.x - textWidth / 2f - padH).coerceIn(2f * density, size.width - textWidth - padH * 2 - 2f * density)
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
                }

                // Dates Label Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    filteredScores.forEach { score ->
                        val displayDate = if (score.testDate.contains("-")) score.testDate.takeLast(5) else score.testDate
                        Text(
                            text = displayDate.ifEmpty { "Test" },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
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
                        text = "$presentDays Days Present",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
