package com.amarmohan

import android.os.Bundle
import android.content.Intent
import android.provider.AlarmClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amarmohan.presentation.screens.LoginScreen
import com.amarmohan.presentation.screens.ParentDashboard
import com.amarmohan.presentation.screens.TutorDashboard
import com.amarmohan.ui.theme.MyApplicationTheme
import com.amarmohan.ui.theme.meshBackground
import com.amarmohan.presentation.viewmodel.MainViewModel
import com.amarmohan.presentation.viewmodel.MainViewModelFactory
import com.amarmohan.presentation.viewmodel.UserSession

class MainActivity : ComponentActivity() {

    // Injects Global State Coordinator with Simple Constructor Context Factory
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge drawing capabilities configured
        enableEdgeToEdge()

        // Schedule Tuitions alarm quietly on device
        schedulePhoneAlarmOnce()

        setContent {
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
                                    text = "Portal system app 🧿",
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
}

