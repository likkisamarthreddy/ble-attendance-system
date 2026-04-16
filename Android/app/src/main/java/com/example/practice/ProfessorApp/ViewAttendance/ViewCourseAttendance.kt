package com.example.practice.ProfessorApp.ViewAttendance

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.ProfessorApp.ProfessorViewModel.ViewAllAttendanceRecordsState
import com.example.practice.ResponsesModel.RecordXX
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val TAG = "ViewCourseAttendance"

@Composable
fun ViewCourseAttendance(
    navController: NavController,
    courseId: String?,
    courseBatch: String?,
    joiningCode: String,
    modifier: Modifier = Modifier,
    professorViewModel: ProfessorViewModel
){
    val isCourseInfoInvalid = courseId.isNullOrEmpty() || courseBatch.isNullOrEmpty()
    val isArchivedSelected by professorViewModel.isArchivedSelected.observeAsState()
    val courseYear by professorViewModel.courseYear.observeAsState()
    val viewAllAttendanceRecords by professorViewModel.viewAllAttendanceRecords.observeAsState()
    val fullAttendanceData by professorViewModel.fullAttendanceData.observeAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isCourseInfoInvalid) {
            courseYear?.let {
                isArchivedSelected?.let { it1 ->
                    professorViewModel.fetchAllAttendanceRecords(joiningCode, courseId!!, courseBatch!!, it, it1)
                }
            }
        }
    }

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
            // Header
            Text(
                text = "Course Records",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Text(
                text = "$courseId - $courseBatch",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Download Button
            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                GradientButton(
                    text = "Download Excel Report",
                    icon = Icons.Default.Download,
                    isLoading = isDownloading,
                    onClick = {
                        isDownloading = true
                        courseYear?.let { year ->
                            isArchivedSelected?.let { archived ->
                                professorViewModel.fetchFullAttendanceData(joiningCode, courseId!!, courseBatch!!, year, archived)
                            }
                        }

                        coroutineScope.launch {
                            var attempts = 0
                            while (attempts < 10) { 
                                delay(500)
                                val currentState = fullAttendanceData
                                if (currentState is ProfessorViewModel.FullAttendanceDataState.Success) {
                                    val attendanceData = currentState.data
                                    if (courseId != null && courseBatch != null && courseYear != null) {
                                        val excelUri = professorViewModel.createExcelFile(context, attendanceData, courseId, courseBatch, courseYear!!)
                                        if (excelUri != null) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(excelUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(intent)
                                                Toast.makeText(context, "Report saved to Downloads", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "File saved to Downloads folder", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Failed to create Excel file", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    isDownloading = false
                                    return@launch
                                } else if (currentState is ProfessorViewModel.FullAttendanceDataState.Error) {
                                    Toast.makeText(context, "Error: ${currentState.message}", Toast.LENGTH_SHORT).show()
                                    isDownloading = false
                                    return@launch
                                }
                                attempts++
                            }
                            isDownloading = false
                            Toast.makeText(context, "Timeout downloading report", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Records List
            when(val state = viewAllAttendanceRecords){
                is ViewAllAttendanceRecordsState.Loading -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                }
                is ViewAllAttendanceRecordsState.Success -> {
                    val sortedRecords = state.data.sortedByDescending { record ->
                        try {
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                            inputFormat.parse(record.date)?.time ?: 0L
                        } catch (e: Exception) { 0L }
                    }

                    if (sortedRecords.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(sortedRecords) { record ->
                                AttendanceRecordCourseCard(record, courseId!!, courseBatch!!, navController)
                            }
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("No attendance records found", color = TextSecondaryDark)
                        }
                    }
                }
                is ViewAllAttendanceRecordsState.Error -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = ErrorCoral)
                    }
                }
                null -> {}
            }
        }
    }
}

@Composable
fun AttendanceRecordCourseCard(record: RecordXX, courseId: String, courseBatch: String, navController: NavController) {
    GlassmorphismCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val encodedRecordId = com.example.practice.utils.EncoderHelper.safeEncode(record._id ?: record.id?.toString())
                val encodedRecordDate = com.example.practice.utils.EncoderHelper.safeEncode(record.date)
                val encodedCourseId = com.example.practice.utils.EncoderHelper.safeEncode(courseId)
                val encodedCourseBatch = com.example.practice.utils.EncoderHelper.safeEncode(courseBatch)
                navController.navigate("ViewRecordAttendance/$encodedRecordId/$encodedRecordDate/$encodedCourseId/$encodedCourseBatch")
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PrimaryIndigo.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = PrimaryIndigo)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = formatDate(record.date),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Tap to view details",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

fun formatDate(isoDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date: Date = inputFormat.parse(isoDate)!!
        val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) { "Invalid Date" }
}

@Composable
fun ErrorScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Error", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ErrorCoral)
            Text(message, color = TextPrimaryDark)
        }
    }
}