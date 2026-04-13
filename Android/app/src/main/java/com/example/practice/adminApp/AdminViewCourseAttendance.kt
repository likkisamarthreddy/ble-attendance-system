package com.example.practice.adminApp

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.ViewAttendance.AttendanceRecordCourseCard
import com.example.practice.ProfessorApp.ViewAttendance.ErrorScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private const val TAG = "AdminViewCourseAttendance"

@Composable
fun AdminViewCourseAttendance(
    navController: NavController,
    courseId: String?,
    courseBatch: String?,
    joiningCode: String,
    modifier: Modifier,
    adminViewModel: AdminViewModel
){
    val isCourseInfoInvalid = courseId.isNullOrEmpty() || courseBatch.isNullOrEmpty()

    val isArchivedSelected by adminViewModel.isArchivedSelected.observeAsState()
    val courseYear by adminViewModel.courseYear.observeAsState()
    val viewAllAttendanceRecords by adminViewModel.viewAllAttendanceRecords.observeAsState()
    val fullAttendanceData by adminViewModel.fullAttendanceData.observeAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!isCourseInfoInvalid) {
            Log.d(TAG, "LaunchedEffect: Fetching all attendance records")
            courseYear?.let {
                isArchivedSelected?.let { it1 ->
                    adminViewModel.fetchAllAttendanceRecords(joiningCode, courseId!!, courseBatch!!,
                        it, it1
                    )
                }
            }
        } else {
            Log.e(TAG, "LaunchedEffect: Invalid course info, cannot fetch attendance records")
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){

        // Show error UI if course info is invalid
        if (isCourseInfoInvalid) {
            ErrorScreen(
                message = "Course information is missing or invalid"
            )
            return@Column
        }

        Text(
            text = "All records for course $courseId: $courseBatch",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                Log.d(TAG, "Download Button: Fetching full attendance data")

                // Trigger data fetch and handle state changes
                courseYear?.let { year ->
                    isArchivedSelected?.let { archived ->
                        adminViewModel.fetchFullAttendanceData(joiningCode, courseId!!, courseBatch!!,
                            year, archived
                        )
                    }
                }

                coroutineScope.launch {
                    // Wait for the data to be loaded
                    var attempts = 0
                    while (attempts < 10) { // Prevent infinite loop
                        delay(500) // Wait a bit between checks

                        val currentState = fullAttendanceData
                        Log.d(TAG, "Attempt $attempts: Current state - $currentState")

                        when (currentState) {
                            is AdminViewModel.FullAttendanceDataState.Success -> {
                                Log.i(TAG, "Full Attendance Data: Successfully fetched")
                                val attendanceData = currentState.data

                                // Fixed section: Simplifies the conditional logic
                                if (courseId != null && courseBatch != null && courseYear != null) {
                                    val excelFile = adminViewModel.createExcelFile(
                                        context,
                                        attendanceData,
                                        courseId,
                                        courseBatch,
                                        courseYear!!
                                    )

                                    if (excelFile != null) {
                                        Log.d(TAG, "Excel File created: ${excelFile.absolutePath}")

                                        try {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                excelFile
                                            )

                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error sharing file", e)
                                            Toast.makeText(
                                                context,
                                                "Failed to share file: ${e.localizedMessage}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Log.e(TAG, "Excel File creation failed")
                                        Toast.makeText(
                                            context,
                                            "Failed to create Excel file",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Log.e(TAG, "Missing required parameters for Excel file creation")
                                    Toast.makeText(
                                        context,
                                        "Missing course information for Excel file creation",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                // Exit the coroutine after successful processing
                                return@launch
                            }
                            is AdminViewModel.FullAttendanceDataState.Error -> {
                                Log.e(TAG, "Full Attendance Data Error: ${currentState.message}")
                                Toast.makeText(
                                    context,
                                    "Failed to fetch attendance data: ${currentState.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }
                            is AdminViewModel.FullAttendanceDataState.Loading -> {
                                attempts++
                                if (attempts >= 10) {
                                    Log.w(TAG, "Timeout waiting for attendance data")
                                    Toast.makeText(
                                        context,
                                        "Timeout loading attendance data",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }
                            }
                            null -> {
                                attempts++
                                if (attempts >= 10) {
                                    Log.w(TAG, "Full Attendance Data: Null state")
                                    Toast.makeText(
                                        context,
                                        "No attendance data available",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Download Excel for ${courseId} - ${courseBatch}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }


        when(val state = viewAllAttendanceRecords){
            is AdminViewModel.ViewAllAttendanceRecordsState.Loading -> {
                Log.d(TAG, "View Attendance Records: Loading")
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is AdminViewModel.ViewAllAttendanceRecordsState.Success -> {
                Log.i(TAG, "View Attendance Records: Successfully fetched")
                val sortedRecords = state.data.sortedBy { record ->
                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                        inputFormat.parse(record.date)?.time ?: 0L
                    } catch (e: Exception) {
                        Log.e(TAG, "Date parsing error for record: ${record.date}", e)
                        0L
                    }
                }

                Log.d(TAG, "Sorted Records Count: ${sortedRecords.size}")

                if (sortedRecords.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedRecords) { record ->
                            AttendanceRecordCourseCard(record, courseId!!, courseBatch!!, navController = navController)
                        }
                    }
                } else {
                    Log.w(TAG, "No Attendance Records Found")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Attendance taken so far",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            is AdminViewModel.ViewAllAttendanceRecordsState.Error -> {
                Log.e(TAG, "View Attendance Records Error: ${state.message}")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                Log.d(TAG, "Retry button clicked: Fetching attendance records")
                                courseYear?.let {
                                    isArchivedSelected?.let { it1 ->
                                        adminViewModel.fetchAllAttendanceRecords(joiningCode, courseId!!, courseBatch!!,
                                            it, it1
                                        )
                                    }
                                }
                            }
                        ) {
                            Text(text = "Retry")
                        }
                    }
                }
            }

            null -> {
                Log.w(TAG, "View Attendance Records: Null state")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading courses...",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp
                    )
                }
            }
        }

    }

}