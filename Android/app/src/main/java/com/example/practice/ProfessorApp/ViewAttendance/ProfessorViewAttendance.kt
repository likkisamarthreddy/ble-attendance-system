package com.example.practice.ProfessorApp.ViewAttendance

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.ProfessorApp.ProfessorViewModel.ProfessorState
import com.example.practice.ResponsesModel.Course

@Composable
fun ProfessorViewAttendance(
    modifier: Modifier = Modifier,
    navController: NavController,
    professorViewModel: ProfessorViewModel
) {
    val professorData by professorViewModel.professorData.observeAsState()

    LaunchedEffect(Unit) {
//        professorViewModel.fetchProfessorCourses()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        // 📌 Header
        Text(
            text = "View Attendance",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))
        // 📌 Handle Different States
        when (val state = professorData) {
            is ProfessorState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is ProfessorState.Success -> {
                val courses = state.data
                Log.d("ProfessorHome", "Received courses: $courses")

                if (courses.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(courses) { course ->
                            ViewAttendanceCourseCard(course = course, navController = navController)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No courses available",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            is ProfessorState.Error -> {
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
//                                professorViewModel.fetchProfessorCourses()
                            }
                        ) {
                            Text(text = "Retry")
                        }
                    }
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
                        text = "Loading courses...",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ViewAttendanceCourseCard(course: Course, navController: NavController) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable {
                // Use URL encoded course name to avoid navigation issues with special characters
                val encodedCourseName = com.example.practice.utils.EncoderHelper.safeEncode(course.name)
                val encodedBatch = com.example.practice.utils.EncoderHelper.safeEncode(course.batch)
                navController.navigate("viewCourseAttendance/$encodedCourseName/$encodedBatch")
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = course.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Batch: ${course.batch}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}