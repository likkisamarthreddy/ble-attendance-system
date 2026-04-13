package com.example.practice.StudentApp

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.livedata.observeAsState
import com.example.practice.features.FaceVerificationScreen
import com.example.practice.features.FaceRegistrationScreen
import com.example.practice.StudentApp.StudentViewModel
import com.example.practice.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "ScanAttendance"

// ═══════════════════════════════════════════════════════════
// SINGLE STATE MACHINE — one variable controls everything
// ═══════════════════════════════════════════════════════════
sealed class ScanScreenState {
    object Landing : ScanScreenState()
    object CheckingProfile : ScanScreenState()
    object FaceRegistration : ScanScreenState()
    object FaceVerification : ScanScreenState()
    data class Processing(val statusText: String = "Validating attendance...") : ScanScreenState()
    object Success : ScanScreenState()
}

@Composable
fun ScanAttendance(
    modifier: Modifier = Modifier,
    navController: NavController,
    studentViewModel: StudentViewModel,
    context: Context = LocalContext.current,
) {
    val scanningViewModel: ScanningViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // ── SINGLE state variable — the ONLY thing that decides what is rendered ──
    var screenState by remember { mutableStateOf<ScanScreenState>(ScanScreenState.Landing) }
    var faceEmbedding by remember { mutableStateOf<FloatArray?>(null) }
    var attendanceError by remember { mutableStateOf<String?>(null) }

    // ViewModel states for BLE
    val isScanning by scanningViewModel.isScanning.collectAsState()
    val scanResultMessage by scanningViewModel.scanResultMessage.collectAsState()

    // BLE Init
    var bluetoothEnabled by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }
    var isInitializing by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            scanningViewModel.initialize(bluetoothAdapter, context)
            isInitializing = false
        } catch (e: Exception) {
            isInitializing = false
        }
    }

    LaunchedEffect(isInitializing) {
        if (!isInitializing) {
            bluetoothEnabled = scanningViewModel.isBluetoothEnabled()
            permissionsGranted = scanningViewModel.hasRequiredBluetoothPermissions(context)
        }
    }

    var successHandled by remember { mutableStateOf(false) }

    // ── Attendance BLE result handler ──
    // RULE: Once Success is shown, NO error can override it.
    //       successHandled is only reset on Idle (fresh attempt).
    LaunchedEffect(Unit) {
        scanningViewModel.attendanceMarked.collect { state ->
            when (state) {
                is AttendanceMarkedState.Success -> {
                    if (!successHandled) {
                        successHandled = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        screenState = ScanScreenState.Success
                        attendanceError = null
                        Log.d(TAG, "✅ SUCCESS — attendance confirmed, showing success page")
                    }
                }
                is AttendanceMarkedState.Error -> {
                    // If success was already shown, IGNORE the error
                    if (!successHandled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        attendanceError = state.message
                        screenState = ScanScreenState.Landing
                        Log.e(TAG, "❌ ERROR — ${state.message}")
                    } else {
                        Log.w(TAG, "⚠️ Error arrived after success — ignoring: ${state.message}")
                    }
                }
                is AttendanceMarkedState.Idle -> {
                    // Reset only on Idle (fresh start / user navigated away)
                    successHandled = false
                }
                else -> {}
            }
        }
    }

    // User will click Close button on success screen to go back


    // ══════════════════════════════════════════════════════════
    // PROFILE FETCH → transitions state ONCE to the right screen
    // Only fires when screenState == CheckingProfile
    // ══════════════════════════════════════════════════════════
    val profileState by studentViewModel.studentProfileDetails.observeAsState()
    LaunchedEffect(profileState) {
        if (screenState !is ScanScreenState.CheckingProfile) return@LaunchedEffect
        when (val ps = profileState) {
            is StudentViewModel.StudentProfileDetailsState.Success -> {
                Log.d(TAG, "PROFILE_LOADED: hasRegisteredFace=${ps.data.hasRegisteredFace}")
                screenState = if (ps.data.hasRegisteredFace) {
                    ScanScreenState.FaceVerification
                } else {
                    ScanScreenState.FaceRegistration
                }
            }
            is StudentViewModel.StudentProfileDetailsState.Error -> {
                Log.e(TAG, "PROFILE_ERROR: ${ps.message}")
                attendanceError = ps.message
                screenState = ScanScreenState.Landing
            }
            else -> {} // Loading — keep showing CheckingProfile
        }
    }

    // ══════════════════════════════════════════════════════════
    // FACE REGISTRATION backend result
    // ══════════════════════════════════════════════════════════
    val faceRegisterState by studentViewModel.faceRegisterState.observeAsState()
    LaunchedEffect(faceRegisterState) {
        when (faceRegisterState) {
            is StudentViewModel.FaceRegisterState.Success -> {
                Log.d(TAG, "Face registered in DB! Starting BLE scan...")
                try { scanningViewModel.startScanning(context) } catch (_: Exception) {}
                studentViewModel.resetFaceRegisterState()
            }
            is StudentViewModel.FaceRegisterState.Error -> {
                val msg = (faceRegisterState as StudentViewModel.FaceRegisterState.Error).message
                Log.e(TAG, "Registration failed: $msg")
                attendanceError = "Registration failed: $msg"
                screenState = ScanScreenState.Landing
                studentViewModel.resetFaceRegisterState()
            }
            else -> {}
        }
    }

    // ══════════════════════════════════════════════════════════
    // FACE VERIFICATION backend result
    // ══════════════════════════════════════════════════════════
    val faceVerifyState by studentViewModel.faceVerifyState.observeAsState()
    LaunchedEffect(faceVerifyState) {
        when (faceVerifyState) {
            is StudentViewModel.FaceVerifyState.Success -> {
                val sim = (faceVerifyState as StudentViewModel.FaceVerifyState.Success).similarity
                Log.d(TAG, "Face verified! sim=$sim. Starting BLE...")
                try { scanningViewModel.startScanning(context) } catch (_: Exception) {}
                studentViewModel.resetFaceVerifyState()
            }
            is StudentViewModel.FaceVerifyState.Failed -> {
                val s = faceVerifyState as StudentViewModel.FaceVerifyState.Failed
                attendanceError = "Face doesn't match! (${String.format("%.0f", s.similarity * 100)}%)"
                screenState = ScanScreenState.Landing
                studentViewModel.resetFaceVerifyState()
            }
            is StudentViewModel.FaceVerifyState.Error -> {
                val msg = (faceVerifyState as StudentViewModel.FaceVerifyState.Error).message
                attendanceError = "Verification error: $msg"
                screenState = ScanScreenState.Landing
                studentViewModel.resetFaceVerifyState()
            }
            else -> {}
        }
    }

    // Update processing status from BLE scan messages.
    // Only update when currently in Processing state and message is non-empty.
    // Success/Landing/other states are NOT touched by scanResultMessage changes.
    LaunchedEffect(scanResultMessage) {
        if (screenState is ScanScreenState.Processing && scanResultMessage.isNotEmpty()) {
            screenState = ScanScreenState.Processing(scanResultMessage)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isScanning) {
                try { scanningViewModel.stopScanning() } catch (_: Exception) {}
            }
        }
    }

    // ═══════════════════════════════════ UI ═══════════════════════════════════
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Deep)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Text_Primary)
            }
        }

        // ── ONE when on ONE state ──
        Box(modifier = Modifier.align(Alignment.Center).fillMaxSize()) {
            Log.d(TAG, "RENDER: $screenState")

            when (screenState) {

                is ScanScreenState.Landing -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LandingScreen(
                            onStartVerification = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                attendanceError = null
                                studentViewModel.fetchStudentProfileDetails()
                                screenState = ScanScreenState.CheckingProfile
                            },
                            errorMessage = attendanceError
                        )
                    }
                }

                is ScanScreenState.CheckingProfile -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ProcessingScreen(statusText = "Checking profile...")
                    }
                }

                is ScanScreenState.FaceRegistration -> {
                    Log.d(TAG, "SHOWING FaceRegistrationScreen")
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FaceRegistrationScreen(
                            onRegistrationComplete = { embeddings, profilePicture ->
                                Log.d(TAG, "Registration done → sending ${embeddings.size} embeddings to backend")
                                // Use first embedding for attendance verification
                                faceEmbedding = embeddings.firstOrNull() ?: FloatArray(0)
                                scanningViewModel.setVerifiedFaceEmbedding(faceEmbedding!!)
                                studentViewModel.registerFace(embeddings, profilePicture)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                screenState = ScanScreenState.Processing("Registering face...")
                            },
                            onBack = { screenState = ScanScreenState.Landing }
                        )
                    }
                }

                is ScanScreenState.FaceVerification -> {
                    Log.d(TAG, "SHOWING FaceVerificationScreen")
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FaceVerificationScreen(
                            onVerificationComplete = { embedding ->
                                Log.d(TAG, "Verification done → sending to backend")
                                faceEmbedding = embedding
                                scanningViewModel.setVerifiedFaceEmbedding(embedding)
                                studentViewModel.verifyFace(embedding)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                screenState = ScanScreenState.Processing("Verifying face...")
                            },
                            onCancel = { screenState = ScanScreenState.Landing }
                        )
                    }
                }

                is ScanScreenState.Processing -> {
                    val state = screenState as ScanScreenState.Processing
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ProcessingScreen(statusText = state.statusText)
                    }
                }

                is ScanScreenState.Success -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SuccessScreen(onClose = { navController.popBackStack() })
                    }
                }
            }
        }

        // Error overlay if BLE not ready
        if (!isInitializing && (!bluetoothEnabled || !permissionsGranted)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface_Elevated)
                    .padding(16.dp)
            ) {
                Column {
                    Text("BLE Setup Required", color = Neon_Yellow, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bluetooth and location permissions are required to scan.", color = Text_Secondary, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun LandingScreen(onStartVerification: () -> Unit, errorMessage: String? = null) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Surface_Elevated)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                            onStartVerification()
                        }
                    )
                }
                .drawBehind {
                    if (isPressed) {
                        drawCircle(
                            color = Neon_Cyan.copy(alpha = 0.2f),
                            radius = size.width / 2f
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TAP",
                color = Neon_Cyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Identity Verification Required",
            color = Text_Primary,
            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
            fontWeight = FontWeight.SemiBold
        )
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = Neon_Red,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun ProcessingScreen(statusText: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .drawBehind {
                    rotate(angle) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(Color.Transparent, Neon_Cyan, Neon_Cyan)
                            ),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SCANNING",
                color = Neon_Cyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = statusText,
            color = Neon_Yellow,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SuccessScreen(onClose: () -> Unit = {}) {
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Neon_Green.copy(alpha = 0.1f), CircleShape)
                .border(2.dp, Neon_Green, CircleShape)
                .drawBehind {
                    // Inner particle burst
                    val p = progress.value
                    if (p > 0f && p < 1f) {
                        for (i in 0 until 6) {
                            val a = (i * 60f) * (Math.PI / 180f)
                            val distance = (size.width / 2f) + (p * 50.dp.toPx())
                            val x = center.x + (distance * cos(a)).toFloat()
                            val y = center.y + (distance * sin(a)).toFloat()
                            drawCircle(
                                color = Neon_Green.copy(alpha = 1f - p),
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Neon_Green,
                modifier = Modifier
                    .size(80.dp)
                    .scale(progress.value)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Attendance Confirmed",
            color = Neon_Green,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.alpha(progress.value)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(50.dp)
                .alpha(progress.value),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Neon_Green.copy(alpha = 0.15f),
                contentColor = Neon_Green
            )
        ) {
            Text(
                text = "Close",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}