package com.example.practice.ProfessorApp.HomePages

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.BroadcastingViewModel
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.ProfessorApp.ViewAttendance.ErrorScreen
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*
import java.net.URLEncoder

private const val TAG = "TakeAttendance"

@Composable
fun TakeAttendance(
    modifier: Modifier = Modifier,
    navController: NavController,
    professorViewModel: ProfessorViewModel,
    courseExpiry: String,
    joiningCode: String,
    courseTitle: String,
    batchName: String
) {
    val isCourseInfoInvalid = courseTitle.isEmpty() || batchName.isEmpty()

    AnimatedGradientBackground(modifier = modifier) {
        if (isCourseInfoInvalid) {
            ErrorScreen(message = "Course information is missing or invalid")
            return@AnimatedGradientBackground
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Take Attendance",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Text_Primary
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            CourseCardTakeAttendance(
                courseName = courseTitle,
                batchName = batchName,
                courseExpiry = courseExpiry,
                joiningCode = joiningCode,
                navController = navController,
                professorViewModel = professorViewModel
            )
        }
    }
}

@Composable
fun CourseCardTakeAttendance(
    courseName: String,
    batchName: String,
    courseExpiry: String,
    joiningCode: String,
    navController: NavController,
    professorViewModel: ProfessorViewModel
) {
    val context = LocalContext.current
    val broadcastingViewModel: BroadcastingViewModel = viewModel()
    var manualButtonClicked by remember { mutableStateOf(false) }
    var broadcastButtonClicked by remember { mutableStateOf(false) }
    var activeCourseName by remember { mutableStateOf<String?>(null) }
    var showGeofenceDialog by remember { mutableStateOf(false) }

    val attendanceState by professorViewModel.createAttendanceState.observeAsState(ProfessorViewModel.CreateAttendanceState.Idle)

    LaunchedEffect(Unit) {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            broadcastingViewModel.initialize(bluetoothManager?.adapter)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing BLE", e)
        }
    }

    LaunchedEffect(attendanceState) {
        if (activeCourseName == courseName && attendanceState is ProfessorViewModel.CreateAttendanceState.Success) {
            val successState = attendanceState as ProfessorViewModel.CreateAttendanceState.Success
            val responseId = successState.attendanceId
            val secret = successState.sessionSecret
            val encodedCourseName = com.example.practice.utils.EncoderHelper.safeEncode(courseName)
            val encodedBatch = com.example.practice.utils.EncoderHelper.safeEncode(batchName)
            val encodedJoiningCode = com.example.practice.utils.EncoderHelper.safeEncode(joiningCode)
            val encodedSecret = com.example.practice.utils.EncoderHelper.safeEncode(secret)

            if (manualButtonClicked) {
                navController.navigate("takeManualAttendance/$encodedCourseName/$encodedBatch/$responseId/$encodedJoiningCode")
            } else if (broadcastButtonClicked) {
                navController.navigate("broadcastAttendance/$encodedCourseName/$encodedBatch/$responseId/$encodedSecret")
            }
            
            manualButtonClicked = false
            broadcastButtonClicked = false
            activeCourseName = null
            professorViewModel.resetCreateAttendanceState()
        }
    }

    GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = courseName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Text_Primary
                )
                Text(
                    text = "Batch: $batchName",
                    fontSize = 14.sp,
                    color = Text_Secondary
                )
            }

            Divider(color = Surface_Elevated)

            // Manual Mode
            GradientButton(
                text = "Manual Attendance",
                icon = Icons.Default.Edit,
                isLoading = activeCourseName == courseName && manualButtonClicked && attendanceState is ProfessorViewModel.CreateAttendanceState.Loading,
                onClick = {
                    manualButtonClicked = true
                    broadcastButtonClicked = false
                    activeCourseName = courseName
                    professorViewModel.createAttendance(courseName, batchName, courseExpiry, joiningCode)
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Broadcast Mode
            GradientButton(
                text = "Broadcast Attendance",
                icon = Icons.Default.Bluetooth,
                isLoading = activeCourseName == courseName && broadcastButtonClicked && attendanceState is ProfessorViewModel.CreateAttendanceState.Loading,
                onClick = {
                    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                    broadcastingViewModel.initialize(bluetoothManager?.adapter)
                    
                    val bleCapabilities = broadcastingViewModel.checkBleCapabilities(context)
                    
                    if (!broadcastingViewModel.hasRequiredBluetoothPermissions(context)) {
                        android.widget.Toast.makeText(context, "Please grant Nearby Devices (Bluetooth) permissions in settings", android.widget.Toast.LENGTH_LONG).show()
                    } else if (!broadcastingViewModel.isBluetoothEnabled()) {
                        android.widget.Toast.makeText(context, "Please turn on Bluetooth to broadcast", android.widget.Toast.LENGTH_LONG).show()
                    } else if (!bleCapabilities.isFullyCapable) {
                        android.widget.Toast.makeText(context, "Your device doesn't support BLE broadcasting", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        // Show geofence prompt instead of immediately creating attendance
                        activeCourseName = courseName
                        showGeofenceDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (activeCourseName == courseName && attendanceState is ProfessorViewModel.CreateAttendanceState.Error) {
                Text(
                    text = (attendanceState as ProfessorViewModel.CreateAttendanceState.Error).message,
                    color = Neon_Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    // Geofence Confirmation Dialog
    if (showGeofenceDialog) {
        val encodedCourseName = com.example.practice.utils.EncoderHelper.safeEncode(courseName)
        val encodedJoiningCode = com.example.practice.utils.EncoderHelper.safeEncode(joiningCode)

        AlertDialog(
            onDismissRequest = { showGeofenceDialog = false },
            title = {
                Text(
                    text = "Geofence Settings",
                    fontWeight = FontWeight.Bold,
                    color = Text_Primary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Have you set your current location and radius for this course?\n\nStudents must be inside the geofence to mark attendance.",
                        color = Text_Secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Neon_Yellow.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Make sure your device location (GPS) is turned ON before using 'Use Current Location' in geofence settings.",
                            color = Neon_Yellow,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGeofenceDialog = false
                        broadcastButtonClicked = true
                        manualButtonClicked = false
                        professorViewModel.createAttendance(courseName, batchName, courseExpiry, joiningCode)
                    }
                ) {
                    Text("START SESSION", color = Neon_Cyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGeofenceDialog = false
                        navController.navigate("geofenceSettings/$encodedJoiningCode/$encodedCourseName")
                    }
                ) {
                    Text("UPDATE GEOFENCE", color = Text_Secondary)
                }
            },
            containerColor = Surface_Elevated,
            titleContentColor = Text_Primary,
            textContentColor = Text_Secondary
        )
    }
}
