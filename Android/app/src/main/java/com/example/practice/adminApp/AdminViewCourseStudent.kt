package com.example.practice.adminApp

import com.example.practice.ui.components.StudentCard
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.ViewAttendance.ErrorScreen
import com.example.practice.ResponsesModel.Student
import com.example.practice.adminApp.AdminViewModel.CourseStudentsState

@Composable
fun AdminViewCourseStudent(
    navController: NavController, // Use nested navController
    courseId: String,
    courseBatch: String,
    joiningCode: String,
    modifier: Modifier,
    adminViewModel: AdminViewModel
){
    val studentData by adminViewModel.courseStudentsData.observeAsState()
    val isArchivedSelected by adminViewModel.isArchivedSelected.observeAsState()
    val courseyear by adminViewModel.courseYear.observeAsState()



    // Sorting
    var sortByAttendance by remember { mutableStateOf(false) }
    var sortAscending by remember { mutableStateOf(false) }

    val isCourseInfoValid = courseId.isNotEmpty() && courseBatch.isNotEmpty() && joiningCode.isNotEmpty()

    LaunchedEffect(courseId, courseBatch, isArchivedSelected) {
        Log.d("ViewStudentsDebug", "${courseId}")
        Log.d("ViewStudentsDebug", "${courseBatch}")
        Log.d("ViewStudentsDebug", "${joiningCode}")
        Log.d("ViewStudentsDebug", "${courseyear}")
        Log.d("ViewStudentsDebug", "LaunchedEffect triggered with isArchivedSelected: $isArchivedSelected")

        if (isCourseInfoValid) {
            // Always log the value before using it
            Log.d("ViewStudentsDebug", "Fetching students with isArchived: $isArchivedSelected")
            courseyear?.let {
                adminViewModel.fetchStudentsInCourse(joiningCode, courseBatch, courseId, isArchivedSelected ?: false,
                    it
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isCourseInfoValid) {
            ErrorScreen(
                message = "Course information is missing or invalid"
            )
            return@Column
        }

        Text(
            text = "Course Students - $courseId ($courseBatch)",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isArchivedSelected == false) {
            SelectionContainer {
                Text(
                    text = "Joining Code: $joiningCode",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)

                .padding(12.dp), // inner padding for spacing inside border
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ){
                Button(
                    onClick = {
                        if (sortByAttendance) {
                            // Already sorting by attendance, toggle direction
                            sortAscending = !sortAscending
                        } else {
                            // Start sorting by attendance (descending by default)
                            sortByAttendance = true
                            sortAscending = false
                        }
                    }
                ) {
                    val buttonText = when {
                        !sortByAttendance -> "Sort By Attendance"
                        sortAscending -> "Attendance (Low to High)"
                        else -> "Attendance (High to Low)"
                    }
                    Text(text = buttonText)
                }
            }

        }

        when (val state = studentData) {
            is CourseStudentsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is CourseStudentsState.Success -> {
                val students = state.students

                // Improved sorting logic with proper numeric comparison
                val sortedStudents = if (sortByAttendance) {
                    students.sortedWith(compareBy<Student> {
                        // Convert attendance percentage string to float for proper numeric sorting
                        it.attendancePercentage.toFloatOrNull() ?: 0f
                    }.let { comparator ->
                        if (sortAscending) comparator else comparator.reversed()
                    })
                } else {
                    students
                }

                if (sortedStudents.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedStudents) { student ->
                            StudentCard(student)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No students enrolled in this course.",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            is CourseStudentsState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        fontSize = 18.sp,
                        color = Color.Red
                    )
                }
            }

            null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Something went wrong, please try reloading",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

//        if(isArchivedSelected == false) {
//            Button(
//                onClick = {
//                    navController.navigate("modifyStudentsInCourse/${courseId}/${courseBatch}")
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp)
//            ) {
//                Text(
//                    text = "Add/Remove Students",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Medium
//                )
//            }
//        }

    }
}