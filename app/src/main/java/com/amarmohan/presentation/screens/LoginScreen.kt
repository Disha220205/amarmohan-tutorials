package com.amarmohan.presentation.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amarmohan.presentation.viewmodel.MainViewModel
import com.amarmohan.presentation.viewmodel.UserSession
import com.amarmohan.core.keyboard.moveFocusOnEnter
import com.amarmohan.ui.theme.getResponsiveButtonShape
import com.amarmohan.ui.theme.getResponsiveButtonFontSize
import com.amarmohan.ui.theme.responsiveButton
import com.amarmohan.ui.theme.responsiveContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextOverflow

enum class OnboardingState {
    STARTUP,
    LOGIN,
    SIGNUP
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var screenState by remember { mutableStateOf(OnboardingState.STARTUP) }

    // Splash/Startup Animation states
    val headerProgress = remember { Animatable(0f) }
    val textScale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val buttonAlpha = remember { Animatable(0f) }
    val footerAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Stage 1: Animate header sliding down with a soft physical spring
        headerProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        )
        // Stage 2: Staggered bouncy text popup
        launch {
            textScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )
        }

        delay(400)

        // Stage 3: Fade in and slide up Continue button and (by invictus ventures) footer
        launch {
            buttonAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
        }
        launch {
            footerAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
        }
    }

    // Core Login State variables
    var isTutorMode by remember { mutableStateOf(false) }
    var tutorPassword by remember { mutableStateOf("") }
    var tutorPasswordVisible by remember { mutableStateOf(false) }

    // Parent Input state variables
    var parentName by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var parentFlatNo by remember { mutableStateOf("") }
    var parentEmail by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isOnline by viewModel.isSupabaseOnline.collectAsState()
    val supabaseError by viewModel.supabaseError.collectAsState()
    val focusManager = LocalFocusManager.current
    val localContext = LocalContext.current

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = screenState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenStateFlipper"
            ) { state ->
                when (state) {
                    OnboardingState.STARTUP -> {
                        // 1. STARTUP SCREEN (Wavy header, large title, Continue button)
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                        ) {
                            val maxHeightDp = maxHeight
                            val waveTargetHeight = if (maxHeightDp < 640.dp) maxHeightDp * 0.46f else maxHeightDp * 0.58f
                            val textOffset = if (maxHeightDp < 640.dp) (-50).dp else (-110).dp

                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(waveTargetHeight)
                                        .graphicsLayer {
                                            translationY = -waveTargetHeight.toPx() * (1f - headerProgress.value)
                                        }
                                ) {
                                    TopographicWavyHeader(
                                        modifier = Modifier.fillMaxSize(),
                                        heightDp = waveTargetHeight.value.toInt()
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp, vertical = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .offset(y = textOffset)
                                            .graphicsLayer {
                                                scaleX = textScale.value
                                                scaleY = textScale.value
                                                alpha = textAlpha.value
                                            }
                                    ) {
                                        Text(
                                            text = "Shree AmarMohan\nLearning Centre",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            lineHeight = 18.sp,
                                            color = Color(0xFF708090),
                                            textAlign = TextAlign.Start,
                                            maxLines = 2,
                                            softWrap = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                alpha = buttonAlpha.value
                                                translationY = 15f * (1f - buttonAlpha.value)
                                            }
                                    ) {
                                        Button(
                                            onClick = { screenState = OnboardingState.LOGIN },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1EBFD)),
                                            shape = RoundedCornerShape(24.dp),
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                            modifier = Modifier.testTag("continue_to_login_btn")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Continue",
                                                    color = Color(0xFF7A68E6),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(Color(0xFFCEB1FC), shape = CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.ArrowForward,
                                                        contentDescription = "Next",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "by Invictus Ventures",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            alpha = footerAlpha.value
                                        }
                                        .padding(bottom = 20.dp)
                                )
                            }
                        }
                    }

                    OnboardingState.LOGIN -> {
                        // 2. SIGN IN SCREEN
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TopographicWavyHeader(heightDp = 200)

                            Column(
                                modifier = Modifier
                                    .responsiveContainer()
                                    .padding(horizontal = 28.dp, vertical = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Sign in",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 34.sp,
                                    color = Color(0xFF1E1A2C)
                                )

                                errorMessage?.let { ErrorBox(it) }

                                // Interactive Tab Selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF5F4FA), shape = RoundedCornerShape(24.dp))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = {
                                            isTutorMode = false
                                            errorMessage = null
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!isTutorMode) Color(0xFFCEB1FC) else Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = if (!isTutorMode) Color.White else Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Parent",
                                                color = if (!isTutorMode) Color.White else Color.Gray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            isTutorMode = true
                                            errorMessage = null
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isTutorMode) Color(0xFFCEB1FC) else Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.School,
                                                contentDescription = null,
                                                tint = if (isTutorMode) Color.White else Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Tutor",
                                                color = if (isTutorMode) Color.White else Color.Gray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }

                                if (!isTutorMode) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(text = "Phone number", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                                        OutlinedTextField(
                                            value = parentPhone,
                                            onValueChange = { parentPhone = it },
                                            placeholder = { Text("Enter your registered phone") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFFCEB1FC)) },
                                            modifier = Modifier.fillMaxWidth().testTag("parent_phone_input")
                                        )

                                        var parentPassword by remember { mutableStateOf("") }
                                        var passwordVisible by remember { mutableStateOf(false) }

                                        Text(text = "Password", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                                        OutlinedTextField(
                                            value = parentPassword,
                                            onValueChange = { parentPassword = it },
                                            placeholder = { Text("Enter password") },
                                            singleLine = true,
                                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFCEB1FC)) },
                                            trailingIcon = {
                                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                    Icon(image, contentDescription = if (passwordVisible) "Hide password" else "Show password", tint = Color.Gray)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("parent_password_input")
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                var rememberMeChecked by remember { mutableStateOf(true) }
                                                Checkbox(
                                                    checked = rememberMeChecked,
                                                    onCheckedChange = { rememberMeChecked = it },
                                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFCEB1FC))
                                                )
                                                Text(text = "Remember Me", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1A2C))
                                            }
                                        }


                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                focusManager.clearFocus()
                                                viewModel.loginAsParent(
                                                    phone = parentPhone,
                                                    password = parentPassword,
                                                    onSuccess = {
                                                        Toast.makeText(localContext, "Logged in successfully!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { errorMessage = it }
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCEB1FC)),
                                            shape = getResponsiveButtonShape(),
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .responsiveButton("parent_login_btn")
                                        ) {
                                            Text(
                                                text = "Sign In",
                                                fontSize = getResponsiveButtonFontSize(),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(text = "Tutor Password", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                                        OutlinedTextField(
                                            value = tutorPassword,
                                            onValueChange = {
                                                tutorPassword = it
                                                errorMessage = null
                                            },
                                            placeholder = { Text("Enter tutor password") },
                                            singleLine = true,
                                            visualTransformation = if (tutorPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFCEB1FC)) },
                                            trailingIcon = {
                                                val image = if (tutorPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                                IconButton(onClick = { tutorPasswordVisible = !tutorPasswordVisible }) {
                                                    Icon(image, contentDescription = if (tutorPasswordVisible) "Hide password" else "Show password", tint = Color.Gray)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("tutor_password_input")
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                focusManager.clearFocus()
                                                val cleanPass = tutorPassword.trim()
                                                if (cleanPass == "071014") {
                                                    viewModel.loginAsTutor(
                                                        onSuccess = {
                                                            Toast.makeText(localContext, "Tutor login successful! Live backend loaded.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        onError = { err -> errorMessage = err }
                                                    )
                                                } else if (cleanPass == "123456") {
                                                    viewModel.loginAsTutorTestMode(
                                                        onSuccess = {
                                                            Toast.makeText(localContext, "Test Mode: Fresh dashboard opened!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        onError = { err -> errorMessage = err }
                                                    )
                                                } else {
                                                    errorMessage = "Incorrect tutor password."
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCEB1FC)),
                                            shape = getResponsiveButtonShape(),
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .responsiveButton("tutor_login_btn")
                                        ) {
                                            Text(
                                                text = "Sign In as Tutor",
                                                fontSize = getResponsiveButtonFontSize(),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "🧿",
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun TopographicWavyHeader(
    modifier: Modifier = Modifier,
    heightDp: Int = 260
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (width == 0f || height == 0f) return@Canvas

            // Define the precise front/main S-wave path matching the mockup
            val mainWavePath = Path().apply {
                moveTo(0f, 0f)
                lineTo(width, 0f)
                lineTo(width, height * 0.90f)
                cubicTo(
                    x1 = width * 0.70f, y1 = height * 1.15f, // Deep purple dip on the right
                    x2 = width * 0.30f, y2 = height * 0.65f, // Higher sweep on the left-middle
                    x3 = 0f, y3 = height * 0.80f             // Left boundary height
                )
                close()
            }

            // Define middle wave path (slightly shifted/dilated for depth)
            val midWavePath = Path().apply {
                moveTo(0f, 0f)
                lineTo(width, 0f)
                lineTo(width, height * 0.94f)
                cubicTo(
                    x1 = width * 0.74f, y1 = height * 1.20f,
                    x2 = width * 0.34f, y2 = height * 0.68f,
                    x3 = 0f, y3 = height * 0.84f
                )
                close()
            }

            // Define back wave path (further offset)
            val backWavePath = Path().apply {
                moveTo(0f, 0f)
                lineTo(width, 0f)
                lineTo(width, height * 0.98f)
                cubicTo(
                    x1 = width * 0.78f, y1 = height * 1.25f,
                    x2 = width * 0.38f, y2 = height * 0.72f,
                    x3 = 0f, y3 = height * 0.88f
                )
                close()
            }

            // Draw Layer 1: Back Wave (Soft purple)
            drawPath(backWavePath, color = Color(0xFFF1E4FF).copy(alpha = 0.5f))

            // Draw Layer 2: Middle Wave (Slightly darker translucent lavender)
            drawPath(midWavePath, color = Color(0xFFEADBFC).copy(alpha = 0.7f))

            // Draw Layer 3: Main Front Wave with gorgeous gradient matching mockup
            val brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(Color(0xFFE5DBFF), Color(0xFFCEB1FC))
            )
            drawPath(mainWavePath, brush = brush)

            // Clip nested topographic curves inside the main visual wave so it never bleeds into the white area
            clipPath(mainWavePath) {
                // Math-based generator for organic contour lines to match topographic map mockup
                fun drawOrganicContour(cx: Float, cy: Float, baseRadius: Float, waveAmp: Float) {
                    val numPoints = 12
                    val points = (0..numPoints).map { i ->
                        val angle = i * (2 * Math.PI / numPoints)
                        // Compound harmonic waves to create beautiful organic ripples
                        val wave = Math.sin(angle * 3).toFloat() * 0.12f + Math.cos(angle * 2).toFloat() * 0.08f + Math.sin(angle * 5).toFloat() * 0.04f
                        val r = baseRadius * (1f + wave * waveAmp)
                        val px = cx + Math.cos(angle).toFloat() * r
                        val py = cy + Math.sin(angle).toFloat() * r * 0.85f // Symmetrical squish
                        px to py
                    }

                    val path = Path()
                    path.moveTo(points[0].first, points[0].second)
                    for (i in 0 until numPoints) {
                        val curr = points[i]
                        val next = points[i + 1]
                        val xc = (curr.first + next.first) / 2
                        val yc = (curr.second + next.second) / 2
                        path.quadraticTo(curr.first, curr.second, xc, yc)
                    }
                    path.close()

                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.28f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.3.dp.toPx())
                    )
                }

                // Center 1: Main concentric hub on the right
                val c1x = width * 0.72f
                val c1y = height * 0.45f
                val radiiCenter1 = listOf(0.12f, 0.24f, 0.36f, 0.48f, 0.60f, 0.72f, 0.84f, 0.96f)
                radiiCenter1.forEach { factor ->
                    drawOrganicContour(c1x, c1y, width * factor, waveAmp = 0.8f)
                }

                // Center 2: Secondary organic hub on the top-left (creates gorgeous intersection)
                val c2x = width * 0.20f
                val c2y = height * 0.28f
                val radiiCenter2 = listOf(0.08f, 0.18f, 0.28f, 0.38f, 0.48f)
                radiiCenter2.forEach { factor ->
                    drawOrganicContour(c2x, c2y, width * factor, waveAmp = 0.6f)
                }
            }
        }
    }
}

@Composable
fun ConnectionStrip(
    isOnline: Boolean,
    supabaseError: String?,
    onRefresh: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val localContext = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                showDialog = true
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF2E7D32) else Color(0xFFFFB300))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isOnline) "Supabase Sync Connected" else "Supabase Offline (using cache)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                    if (!isOnline) {
                        Text(
                            text = "Tap to view diagnostics / troubleshoot",
                            fontSize = 11.sp,
                            color = Color(0xFFD35400)
                        )
                    }
                }
            }
            Icon(
                imageVector = if (isOnline) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = "Connection Details",
                tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOnline) "Connection Successful!" else "Connection Troubleshooter",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isOnline) {
                        Text(
                            text = "Your app is fully connected to Supabase Cloud Services. Real-time synchronizations of parent/student accounts, schedules, attendance, and exam papers are active.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "The connection setup is offline or experiencing an issue.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        Text(
                            text = "Captured Error Details:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )

                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = supabaseError ?: "No detailed error captured. Attempting setup connection sequence...",
                                color = Color(0xFFC62828),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        Text(
                            text = "Common Troubleshooting Steps:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )

                        Text(
                            text = "1. Table Schema Initialized?\n" +
                                    "If the error mentions that the 'students' relation is not found, you must run the database query setup inside your Supabase project (https://supabase.com). Copy the SQL statements from 'supabase_schema.sql' (found at the root of your workspace) and paste/execute them inside the SQL Editor of your Supabase dashboard.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "2. Secret API Key Configuration?\n" +
                                    "Make sure your `SUPABASE_URL` and `SUPABASE_ANON_KEY` are copy-pasted correctly. They can be injected via the secure Secrets panel in the AI Studio editor.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRefresh()
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOnline) Color(0xFF2E7D32) else Color(0xFF6D28D9)
                    )
                ) {
                    Text("Refresh Connection")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ErrorBox(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFFFFCDD2))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFC62828))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC62828)
            )
        }
    }
}

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    helperText: String? = null
) {
    val localFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val finalKeyboardOptions = if (keyboardOptions.imeAction == androidx.compose.ui.text.input.ImeAction.Default || keyboardOptions.imeAction == androidx.compose.ui.text.input.ImeAction.None) {
        keyboardOptions.copy(imeAction = androidx.compose.ui.text.input.ImeAction.Next)
    } else {
        keyboardOptions
    }
    val finalKeyboardActions = if (keyboardActions == androidx.compose.foundation.text.KeyboardActions.Default) {
        androidx.compose.foundation.text.KeyboardActions(
            onNext = { localFocusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) },
            onDone = { localFocusManager.clearFocus() }
        )
    } else {
        keyboardActions
    }

    Column(modifier = modifier) {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            modifier = Modifier.fillMaxWidth().moveFocusOnEnter(localFocusManager, androidx.compose.ui.focus.FocusDirection.Next),
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = finalKeyboardOptions,
            keyboardActions = finalKeyboardActions,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF7F5FC),
                unfocusedContainerColor = Color(0xFFF7F5FC),
                focusedIndicatorColor = Color(0xFFCEB1FC),
                unfocusedIndicatorColor = Color(0xFFE5DFFF)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (helperText != null) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
        }
    }
}




