
package com.example.practice.ProfessorApp.HomePages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.ProfessorApp.ProfessorViewModel.CourseStudentsState
import com.example.practice.ProfessorApp.ViewAttendance.ErrorScreen
import com.example.practice.ResponsesModel.Student
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*

@Composable
fun CourseStudentDetailScreen(
    navController: NavController,
    courseId: String,
    courseBatch: String,
    joiningCode: String,
    modifier: Modifier = Modifier,
    professorViewModel: ProfessorViewModel
) {
    val studentData by professorViewModel.courseStudentsData.observeAsState()
    val isArchivedSelected by professorViewModel.isArchivedSelected.observeAsState()
    val courseyear by professorViewModel.courseYear.observeAsState()

    var sortByAttendance by remember { mutableStateOf(false) }
    var sortAscending by remember { mutableStateOf(false) }

    val isCourseInfoInvalid = courseId.isNullOrEmpty() || courseBatch.isNullOrEmpty()

    LaunchedEffect(courseId, courseBatch, isArchivedSelected) {
        if (!isCourseInfoInvalid) {
            courseyear?.let {
                professorViewModel.fetchStudentsInCourse(joiningCode, courseBatch, courseId, isArchivedSelected ?: false, it)
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
                text = "$courseId ($courseBatch)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Joining Code Card
            if (isArchivedSelected == false) {
                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Joining Code", color = TextSecondaryDark, fontSize = 12.sp)
                        SelectionContainer {
                            Text(
                                text = joiningCode,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigo,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sorting Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilterChip(
                    selected = sortByAttendance,
                    onClick = {
                        if (sortByAttendance) {
                            sortAscending = !sortAscending
                        } else {
                            sortByAttendance = true
                            sortAscending = false
                        }
                    },
                    label = { 
                        Text(
                            text = when {
                                !sortByAttendance -> "Sort by Attendance"
                                sortAscending -> "Attendance (Low to High)"
                                else -> "Attendance (High to Low)"
                            }
                        ) 
                    },
                    leadingIcon = { Icon(Icons.Default.Sort, null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryIndigo,
                        labelColor = TextPrimaryDark,
                        selectedLabelColor = TextPrimaryDark,
                        selectedLeadingIconColor = TextPrimaryDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Student List
            when (val state = studentData) {
                is CourseStudentsState.Loading -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                }
                is CourseStudentsState.Success -> {
                    val students = state.students
                    val sortedStudents = if (sortByAttendance) {
                        students.sortedWith(compareBy<Student> {
                            it.attendancePercentage.toFloatOrNull() ?: 0f
                        }.let { comparator ->
                            if (sortAscending) comparator else comparator.reversed()
                        })
                    } else {
                        students
                    }

                    if (sortedStudents.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(sortedStudents) { student ->
                                StudentCard(student)
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

            if (isArchivedSelected == false) {
                GradientButton(
                    text = "Add/Remove Students",
                    onClick = { navController.navigate("modifyStudentsInCourse/${courseId}/${courseBatch}") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val encodedJC = java.net.URLEncoder.encode(joiningCode, "UTF-8")
                        val encodedName = java.net.URLEncoder.encode(courseId, "UTF-8")
                        navController.navigate("geofenceSettings/$encodedJC/$encodedName")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Neon_Cyan
                    )
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Neon_Cyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Geofence Settings", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
