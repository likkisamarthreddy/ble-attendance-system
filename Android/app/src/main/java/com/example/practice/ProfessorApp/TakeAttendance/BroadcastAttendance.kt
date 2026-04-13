package com.example.practice.ProfessorApp.TakeAttendance

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.BroadcastingViewModel
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.ProfessorApp.ViewAttendance.ModifyStudentAttendanceCard
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "BroadcastAttendance"

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BroadcastAttendance(
    modifier: Modifier,
    navController: NavController,
    courseId: String,
    courseBatch: String,
    attendanceId: String,
    sessionSecret: String,
    context: Context = LocalContext.current,
    professorViewModel: ProfessorViewModel
) {
    Log.d(TAG, "BroadcastAttendance composable started")

    val broadcastingViewModel: BroadcastingViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by broadcastingViewModel.errorMessage.collectAsState()

    // Manual attendance marking states
    val viewRecordData by professorViewModel.viewRecordData.observeAsState()
    var selectedStudents by remember { mutableStateOf(setOf<Int>()) }
    var studentsFetched by remember { mutableStateOf(false) }
    val modifyLiveAttendance by professorViewModel.modifyLiveAttendanceState.observeAsState()
    
    val endSessionState by professorViewModel.endSessionState.observeAsState(ProfessorViewModel.EndSessionState.Idle)

    // Token state
    val currentToken by broadcastingViewModel.currentToken.collectAsState()
    val currentTimeSlot by broadcastingViewModel.currentTimeSlot.collectAsState()
    val tokenTimeRemaining by broadcastingViewModel.tokenTimeRemaining.collectAsState()
    
    // Extracted live student count if available from record data
    val presentCount = remember(viewRecordData) {
        if (viewRecordData is ProfessorViewModel.ViewRecordDataState.Success) {
            (viewRecordData as ProfessorViewModel.ViewRecordDataState.Success).data.count { it.status == "Present" }
        } else {
            0
        }
    }
    val totalCount = remember(viewRecordData) {
        if (viewRecordData is ProfessorViewModel.ViewRecordDataState.Success) {
            (viewRecordData as ProfessorViewModel.ViewRecordDataState.Success).data.size
        } else {
            0 // Or a placeholder total if known
        }
    }

    LaunchedEffect(modifyLiveAttendance) {
        if (modifyLiveAttendance is ProfessorViewModel.ModifyLiveAttendanceState.Success) {
            selectedStudents = setOf()
            Toast.makeText(context, "Attendance updated successfully!", Toast.LENGTH_SHORT).show()
            professorViewModel.resetModifyLiveAttendanceState()
        } else if (modifyLiveAttendance is ProfessorViewModel.ModifyLiveAttendanceState.Error) {
            val errMessage = (modifyLiveAttendance as ProfessorViewModel.ModifyLiveAttendanceState.Error).message
            Toast.makeText(context, "Error: $errMessage", Toast.LENGTH_LONG).show()
            professorViewModel.resetModifyLiveAttendanceState()
        }
    }

    val isAdvertising by broadcastingViewModel.isAdvertising.collectAsState()

    LaunchedEffect(endSessionState) {
        when (endSessionState) {
            is ProfessorViewModel.EndSessionState.Success -> {
                Toast.makeText(context, "Session Ended", Toast.LENGTH_SHORT).show()
                professorViewModel.resetEndSessionState()
                if (isAdvertising) {
                    try { broadcastingViewModel.stopAdvertising() } catch (_: Exception) {}
                }
                navController.popBackStack()
            }
            is ProfessorViewModel.EndSessionState.Error -> {
                val errMessage = (endSessionState as ProfessorViewModel.EndSessionState.Error).message
                Toast.makeText(context, "Error ending session: $errMessage", Toast.LENGTH_LONG).show()
                professorViewModel.resetEndSessionState()
            }
            else -> {}
        }
    }

    LaunchedEffect(viewRecordData) {
        studentsFetched = viewRecordData is ProfessorViewModel.ViewRecordDataState.Success &&
                (viewRecordData as ProfessorViewModel.ViewRecordDataState.Success).data.isNotEmpty()
    }

    var isInitializing by remember { mutableStateOf(true) }
    var initializationError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(attendanceId) { onDispose { professorViewModel.resetViewRecordData() } }

    LaunchedEffect(Unit) {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            broadcastingViewModel.initialize(bluetoothAdapter)
            isInitializing = false
        } catch (e: Exception) {
            isInitializing = false
            initializationError = "Failed to initialize Bluetooth"
        }
    }

    val statusMessage by broadcastingViewModel.statusMessage.collectAsState()

    var bluetoothEnabled by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }

    LaunchedEffect(isInitializing) {
        if (!isInitializing) {
            bluetoothEnabled = broadcastingViewModel.isBluetoothEnabled()
            permissionsGranted = broadcastingViewModel.hasRequiredBluetoothPermissions(context)
        }
    }

    var bleCapabilities by remember { mutableStateOf(BroadcastingViewModel.BleCapabilities(false, false, false, false, false)) }

    val canBroadcast = bluetoothEnabled && permissionsGranted && bleCapabilities.isFullyCapable
    
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            broadcastingViewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            if (!isInitializing) {
                val wasEnabled = bluetoothEnabled
                bluetoothEnabled = broadcastingViewModel.isBluetoothEnabled()
                permissionsGranted = broadcastingViewModel.hasRequiredBluetoothPermissions(context)
                
                if (!wasEnabled && bluetoothEnabled) {
                    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    broadcastingViewModel.initialize(bluetoothManager?.adapter)
                }
                
                bleCapabilities = broadcastingViewModel.checkBleCapabilities(context)
            }
        }
    }

    // Auto-start broadcasting
    LaunchedEffect(canBroadcast, attendanceId) {
        if (canBroadcast && !isAdvertising && !isInitializing) {
            try {
                broadcastingViewModel.setSessionInfo(attendanceId, sessionSecret)
                broadcastingViewModel.setCustomData(attendanceId, context)
                broadcastingViewModel.startAdvertising(context)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to start broadcasting")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isAdvertising) {
                try { broadcastingViewModel.stopAdvertising() } catch (_: Exception) {}
            }
        }
    }

    // ─── Premium UX UI ───
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Deep)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Text_Primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Session Active: $courseId",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Text_Primary
                    )
                    Text(
                        text = "Batch: $courseBatch",
                        color = Text_Secondary,
                        fontSize = 13.sp
                    )
                }
                // Animated Live Indicator
                if (isAdvertising) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Neon_Cyan)
                                .alpha(alpha)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE",
                            color = Neon_Cyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isInitializing) {
                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Neon_Cyan, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Initializing Command Center...", color = Text_Secondary)
                    }
                }
            } else if (!bleCapabilities.isFullyCapable || !bluetoothEnabled || !permissionsGranted) {
                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("⚠️ Setup Required", fontWeight = FontWeight.Bold, color = Neon_Yellow, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                !bleCapabilities.isFullyCapable -> "Your device doesn't support required BLE features."
                                !bluetoothEnabled -> "Please enable Bluetooth to broadcast."
                                else -> "Please grant Bluetooth permissions."
                            },
                            color = Text_Secondary,
                            fontSize = 14.sp
                        )
                        if (!bluetoothEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            GradientButton(
                                text = "Enable Bluetooth",
                                onClick = {
                                    if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                        try { (context as? Activity)?.startActivityForResult(intent, 1) }
                                        catch (_: Exception) {}
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // ─── Massive Rolling Token Display ───
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Surface_Elevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAdvertising) {
                        // Progress ring
                        val remainingSec = tokenTimeRemaining / 1000f
                        val progress = remainingSec / 7f
                        val ringColor = if (remainingSec <= 2f) Neon_Yellow else Neon_Cyan

                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = ringColor.copy(alpha = 0.1f),
                                        style = Stroke(width = 8.dp.toPx())
                                    )
                                    drawArc(
                                        color = ringColor,
                                        startAngle = -90f,
                                        sweepAngle = progress * 360f,
                                        useCenter = false,
                                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Token Slide Animation
                            val tokenText = currentToken.ifEmpty { "WAIT" }
                            AnimatedContent(
                                targetState = tokenText,
                                transitionSpec = {
                                    slideInVertically { height -> height } + fadeIn() togetherWith
                                            slideOutVertically { height -> -height } + fadeOut()
                                }
                            ) { targetToken ->
                                Text(
                                    text = targetToken,
                                    color = Text_Primary,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 4.sp
                                )
                            }
                        }
                    } else {
                        Text("Broadcast Offline", color = Text_Secondary, fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ─── Real-Time Student Counter ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ATTENDANCE COUNT", color = Text_Secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Counter pop animation
                        val scale = remember { Animatable(1f) }
                        LaunchedEffect(presentCount) {
                            if (presentCount > 0) {
                                scale.animateTo(1.2f, animationSpec = tween(100))
                                scale.animateTo(1.0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        }
                        
                        Text(
                            text = if (totalCount > 0) "$presentCount / $totalCount" else "$presentCount",
                            color = Neon_Green,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.scale(scale.value)
                        )
                    }

                    // Secondary info
                    if (isAdvertising) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PULSE RATE", color = Text_Secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                            Text("Active", color = Text_Primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Broadcast toggle
                GradientButton(
                    text = if (isAdvertising) "STOP BROADCASTING" else "START BROADCASTING",
                    onClick = {
                        if (isAdvertising) {
                            try { broadcastingViewModel.stopAdvertising() }
                            catch (e: Exception) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Failed to stop") }
                            }
                        } else {
                            try {
                                broadcastingViewModel.setSessionInfo(attendanceId, sessionSecret)
                                broadcastingViewModel.setCustomData(attendanceId, context)
                                broadcastingViewModel.startAdvertising(context)
                            } catch (e: Exception) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Failed to start") }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // End Session button
                Button(
                    onClick = { professorViewModel.endSession(attendanceId) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Neon_Red.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Neon_Red.copy(alpha = 0.5f))
                ) {
                    if (endSessionState is ProfessorViewModel.EndSessionState.Loading) {
                        CircularProgressIndicator(color = Neon_Red, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("END SESSION", color = Neon_Red, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ─── Manual attendance marking section ───
                GlassmorphismCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column {
                        if (!studentsFetched) {
                            GradientButton(
                                text = "Fetch Absent Students",
                                onClick = { professorViewModel.fetchRecordData(attendanceId, false) },
                                enabled = !isAdvertising,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isAdvertising) "Stop broadcasting to modify" else "View and manually override",
                                color = Text_Secondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!isAdvertising) {
                            when (val state = viewRecordData) {
                                is ProfessorViewModel.ViewRecordDataState.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Neon_Cyan, strokeWidth = 3.dp)
                                    }
                                }
                                is ProfessorViewModel.ViewRecordDataState.Success -> {
                                    val record = state.data
                                    if (record.isNotEmpty()) {
                                        val sortedRecords = record.sortedBy {
                                            try { it.rollno.toInt() } catch (_: NumberFormatException) { Int.MAX_VALUE }
                                        }

                                        GradientButton(
                                            text = "Mark ${selectedStudents.size} Present",
                                            onClick = { professorViewModel.modifyLiveAttendace(selectedStudents.toList(), attendanceId) },
                                            enabled = selectedStudents.isNotEmpty(),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        LazyColumn(
                                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(sortedRecords) { rec ->
                                                if (rec.status == "Absent") {
                                                    ModifyStudentAttendanceCard(
                                                        Attendance = rec,
                                                        isChecked = rec.rollno in selectedStudents,
                                                        onCheckedChange = { isChecked ->
                                                            selectedStudents = if (isChecked) selectedStudents + rec.rollno
                                                            else selectedStudents - rec.rollno
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "All students present",
                                            color = Neon_Green,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                is ProfessorViewModel.ViewRecordDataState.Error -> {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.message,
                                        color = Neon_Red,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    GradientButton(
                                        text = "Retry",
                                        onClick = { professorViewModel.fetchRecordData(attendanceId, false) }
                                    )
                                }
                                is ProfessorViewModel.ViewRecordDataState.Idle, null -> {}
                            }
                        } else {
                            if (studentsFetched) {
                                Text(
                                    text = "Stop broadcasting to modify attendance manually.",
                                    color = Text_Secondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) { snackbarData ->
            Snackbar(
                containerColor = Surface_Elevated,
                contentColor = Text_Primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(snackbarData.visuals.message)
            }
        }
    }
}
