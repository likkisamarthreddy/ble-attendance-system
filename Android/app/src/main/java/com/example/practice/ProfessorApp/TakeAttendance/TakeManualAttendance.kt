package com.example.practice.ProfessorApp.TakeAttendance

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.ProfessorApp.ProfessorViewModel.CourseStudentsState
import com.example.practice.ResponsesModel.Student
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*

@Composable
fun TakeManualAttendance(
    navController: NavController,
    courseId: String,
    courseBatch: String,
    attendanceId: String,
    joiningCode: String,
    modifier: Modifier = Modifier,
    professorViewModel: ProfessorViewModel
) {
    val studentData by professorViewModel.courseStudentsData.observeAsState()
    val markManualAttendanceState by professorViewModel.markManualAttendanceState.observeAsState()
    val courseYear by professorViewModel.courseYear.observeAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedStudents = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(Unit) {
        courseYear?.let {
            professorViewModel.fetchStudentsInCourse(joiningCode, courseBatch, courseId, false, it)
        }
    }

    LaunchedEffect(markManualAttendanceState) {
        if (markManualAttendanceState is ProfessorViewModel.MarkManualAttendanceState.Success) {
            professorViewModel.resetMarkManualAttendanceState()
            navController.popBackStack()
        } else if (markManualAttendanceState is ProfessorViewModel.MarkManualAttendanceState.Error) {
            snackbarHostState.showSnackbar("Error: ${(markManualAttendanceState as ProfessorViewModel.MarkManualAttendanceState.Error).message}")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        AnimatedGradientBackground(modifier = modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Mark Attendance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "$courseId ($courseBatch)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Student List
                when (val state = studentData) {
                    is CourseStudentsState.Loading -> {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryIndigo)
                        }
                    }
                    is CourseStudentsState.Success -> {
                        val students = state.students
                        if (students.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(students) { student ->
                                    StudentCardManualAttendance(
                                        student = student,
                                        isChecked = selectedStudents[student.rollno] ?: false,
                                        onCheckedChange = { isChecked ->
                                            selectedStudents[student.rollno] = isChecked
                                        }
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text("No students enrolled", color = TextSecondaryDark)
                            }
                        }
                    }
                    is CourseStudentsState.Error -> {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.message}", color = ErrorCoral)
                        }
                    }
                    null -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                GradientButton(
                    text = "Confirm Attendance",
                    isLoading = markManualAttendanceState is ProfessorViewModel.MarkManualAttendanceState.Loading,
                    onClick = {
                        val selectedStudentIds = selectedStudents.entries
                            .filter { it.value }
                            .map { it.key.toString() }
                        professorViewModel.MarkManualAttendance(attendanceId, selectedStudentIds)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StudentCardManualAttendance(
    student: Student,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassmorphismCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = student.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Roll: ${student.rollno}",
                    fontSize = 14.sp,
                    color = TextSecondaryDark
                )
            }
            Checkbox(
                checked = isChecked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryIndigo,
                    uncheckedColor = TextSecondaryDark,
                    checkmarkColor = Color.White
                )
            )
        }
    }
}
