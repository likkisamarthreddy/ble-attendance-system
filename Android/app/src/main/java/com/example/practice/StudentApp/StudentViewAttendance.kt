package com.example.practice.StudentApp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.practice.ResponsesModel.AttendanceX
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*

@Composable
fun studentViewAttendance(
    navController: NavController,
    courseId: String,
    courseBatch: String,
    modifier: Modifier = Modifier
) {
    val studentViewModel: StudentViewModel = viewModel()
    val studentViewAttendanceData by studentViewModel.studentViewAttendanceData.observeAsState()
    var attendanceCount by remember { mutableIntStateOf(0) }
    var presentCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        studentViewModel.fetchStudentAttendance(courseId, courseBatch)
    }

    AnimatedGradientBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = courseId,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            StatusChip(text = courseBatch, color = SecondaryPurple)
            
            Spacer(modifier = Modifier.height(24.dp))

            // Stats Dashboard
            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttendanceStat(label = "Total", value = attendanceCount.toString(), color = PrimaryIndigo)
                    
                    // Percentage Circle
                    Box(contentAlignment = Alignment.Center) {
                        val percentage = if (attendanceCount > 0) (presentCount.toFloat() / attendanceCount) else 0f
                        CircularProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier.size(64.dp),
                            color = if (percentage >= 0.75f) SuccessGreen else if (percentage >= 0.6f) WarningAmber else ErrorCoral,
                            trackColor = DarkSurfaceVariant,
                            strokeWidth = 6.dp,
                        )
                        Text(
                            text = "${(percentage * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }

                    AttendanceStat(label = "Present", value = presentCount.toString(), color = SuccessGreen)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (val state = studentViewAttendanceData) {
                is StudentViewModel.StudentViewAttendanceState.Loading -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                }
                is StudentViewModel.StudentViewAttendanceState.Success -> {
                    val attendance = state.data.attendance

                    if (attendance.isNotEmpty()) {
                        attendanceCount = attendance.size
                        presentCount = attendance.count { it.status == "Present" }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(attendance) { record ->
                                AttendanceCardStudent(record)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No attendance records yet", color = TextSecondaryDark, fontSize = 16.sp)
                            }
                        }
                    }
                }
                is StudentViewModel.StudentViewAttendanceState.Error -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        GlassmorphismCard {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Error loading attendance", color = ErrorCoral)
                                Text(state.message, color = TextSecondaryDark, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { studentViewModel.fetchStudentAttendance(courseId, courseBatch) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                                ) { Text("Retry") }
                            }
                        }
                    }
                }
                null -> {}
            }
        }
    }
}

@Composable
fun AttendanceStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 12.sp, color = TextSecondaryDark)
    }
}

@Composable
fun AttendanceCardStudent(attendance: AttendanceX) {
    val isPresent = attendance.status == "Present"
    val statusColor = if (isPresent) SuccessGreen else ErrorCoral
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = attendance.date,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryDark
                )
            }
            
            // Status Badge
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = attendance.status,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}