package com.amarmohan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.provider.AlarmClock
import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amarmohan.core.receiver.BirthdayAlarmReceiver
import com.amarmohan.presentation.screens.LoginScreen
import com.amarmohan.presentation.screens.ParentDashboard
import com.amarmohan.presentation.screens.TutorDashboard
import com.amarmohan.ui.theme.MyApplicationTheme
import com.amarmohan.ui.theme.meshBackground
import com.amarmohan.presentation.viewmodel.MainViewModel
import com.amarmohan.presentation.viewmodel.MainViewModelFactory
import com.amarmohan.presentation.viewmodel.UserSession
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    // Injects Global State Coordinator with Simple Constructor Context Factory
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(applicationContext)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge drawing capabilities configured
        enableEdgeToEdge()

        // Auto request SMS & Notification permissions on startup for seamless automated SMS
        requestRequiredPermissions()

        // Schedule daily midnight 00:00 birthday trigger alarm
        BirthdayAlarmReceiver.scheduleNextMidnightAlarm(applicationContext)

        // Handle incoming action from notification tap
        handleIncomingSmsIntent(intent)

        setContent {
            val stableDensity = DisplayMetrics.DENSITY_DEVICE_STABLE / DisplayMetrics.DENSITY_DEFAULT.toFloat()
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = stableDensity,
                    fontScale = 1f
                )
            ) {
                MyApplicationTheme {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .meshBackground(),
                        color = Color.Transparent
                    ) {
                        var isSplashShowing by remember { mutableStateOf(false) }

                        LaunchedEffect(Unit) {
                            isSplashShowing = false
                        }

                        if (isSplashShowing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Card(
                                        shape = CircleShape,
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEADBFC)),
                                        modifier = Modifier.size(100.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.School,
                                                contentDescription = null,
                                                tint = Color(0xFF7A68E6),
                                                modifier = Modifier.size(54.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = "SHREE AMARMOHAN",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        letterSpacing = 2.sp,
                                        color = Color(0xFF708090)
                                    )
                                    Text(
                                        text = "Portal system app",
                                        fontSize = 12.sp,
                                        letterSpacing = 1.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            val sessionState by viewModel.session.collectAsState()

                            // Main Route Director with statusbar and navigation bar padding
                            Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                                when (sessionState) {
                                    is UserSession.Guest -> {
                                        LoginScreen(viewModel = viewModel)
                                    }
                                    is UserSession.Tutor -> {
                                        TutorDashboard(viewModel = viewModel)
                                    }
                                    is UserSession.Parent -> {
                                        ParentDashboard(viewModel = viewModel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun schedulePhoneAlarmOnce() {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, "Amarmohan Tuition Class Reminder")
                putExtra(AlarmClock.EXTRA_HOUR, 16) // 4:30 PM daily
                putExtra(AlarmClock.EXTRA_MINUTES, 30)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingSmsIntent(intent)
    }

    private fun handleIncomingSmsIntent(incomingIntent: Intent?) {
        if (incomingIntent?.getBooleanExtra("ACTION_DISPATCH_SMS", false) == true) {
            val phone = incomingIntent.getStringExtra("EXTRA_PHONE") ?: return
            val message = incomingIntent.getStringExtra("EXTRA_MESSAGE") ?: return
            val studentSlNo = incomingIntent.getStringExtra("EXTRA_STUDENT_SL_NO")
            val reminderId = incomingIntent.getStringExtra("EXTRA_REMINDER_ID")
            val isBirthday = incomingIntent.getBooleanExtra("EXTRA_IS_BIRTHDAY", false)

            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager

            if (!studentSlNo.isNullOrEmpty()) {
                if (!reminderId.isNullOrEmpty()) {
                    viewModel.removeReminder(studentSlNo, reminderId)
                } else {
                    viewModel.removeEnrollmentReminders(studentSlNo)
                }
                notificationManager?.cancel((studentSlNo + "_enroll").hashCode())
                notificationManager?.cancel((studentSlNo + "_signin").hashCode())
            }
            if (!studentSlNo.isNullOrEmpty() && isBirthday) {
                viewModel.markBirthdaySmsSent(studentSlNo)
                val matchingStudent = viewModel.students.value.find { it.slNo == studentSlNo }
                if (matchingStudent != null) {
                    notificationManager?.cancel(matchingStudent.name.hashCode())
                }
            }

            viewModel.openSmsApp(this, phone, message)
        }
    }
}

