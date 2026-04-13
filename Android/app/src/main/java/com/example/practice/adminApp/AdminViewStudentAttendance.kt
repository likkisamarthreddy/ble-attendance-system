package com.example.practice.adminApp

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ResponsesModel.AttendanceData
import com.example.practice.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun AdminViewStudentAttendance(
    navController: NavController,
    name: String,
    rollno: String,
    modifier: Modifier = Modifier,
    adminViewModel: AdminViewModel
) {
    val studentAttendanceByCourseData by adminViewModel.studentAttendanceByCourseData.observeAsState()

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    var sortAscending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminViewModel.fetchStudentAttendance(name, rollno)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Courses and Attendance",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Text_Primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(12.dp), // inner padding for spacing inside border
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        if (sortAscending) {
                            // Already sorting by attendance, toggle direction
                            sortAscending = !sortAscending
                        } else {
                            sortAscending = true
                        }
                    }
                ) {
                    val buttonText = when {
                        sortAscending -> "Attendance (high-low)"
                        else -> "Attendance(high-low)"
                    }
                    Text(text = buttonText)
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            placeholder = {
                Text(
                    text = "Search by Course/Batch",
                    color = Text_Secondary
                )
            },
            textStyle = TextStyle(color = Text_Primary),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon",
                    tint = Text_Secondary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            keyboardController?.hide()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Search",
                            tint = Text_Secondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Neon_Cyan,
                unfocusedBorderColor = Surface_Elevated,
                focusedContainerColor = Surface_Elevated,
                unfocusedContainerColor = Surface_Elevated,
                cursorColor = Neon_Cyan
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                }
            )
        )

        when (val state = studentAttendanceByCourseData) {
            is AdminViewModel.StudentAttendanceByCourseState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Neon_Cyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading attendance data...",
                            fontSize = 16.sp,
                            color = Text_Primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is AdminViewModel.StudentAttendanceByCourseState.Success -> {
                val Allcourses = state.data.attendanceData
                Log.d("AdminViewStudentAttendance", "${Allcourses}")

                val courses = if(searchQuery.isEmpty()){
                    Allcourses
                } else{
                    Allcourses.filter{ course ->
                        course.course.contains(searchQuery, ignoreCase = true) ||
                                course.batch.contains(searchQuery, ignoreCase = true)
                    }
                }

                // Improved sorting logic with proper numeric comparison
                val sortedCourse = if (sortAscending) {
                    courses.sortedBy { it.course }
                } else {
                    courses.sortedByDescending { it.course }
                }

                if (sortedCourse.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedCourse) { course ->
                            StudentAttendanceByCourseCard(course, navController)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Surface_Elevated
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "No courses found",
                                    tint = Neon_Yellow,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Courses Found",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Text_Primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isEmpty()) {
                                        "This student is not enrolled in any courses"
                                    } else {
                                        "No courses match your search criteria"
                                    },
                                    fontSize = 14.sp,
                                    color = Text_Secondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            is AdminViewModel.StudentAttendanceByCourseState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Surface_Elevated
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = Neon_Red,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Something went wrong",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Neon_Red,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                fontSize = 14.sp,
                                color = Text_Secondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    adminViewModel.fetchStudentAttendance(name, rollno)
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Neon_Red)
                            ) {
                                Text("Retry", color = Color.White)
                            }
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Surface_Elevated
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Loading error",
                                tint = Text_Secondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Unable to Load Data",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Text_Primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please check your connection and try again",
                                fontSize = 14.sp,
                                color = Text_Secondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    adminViewModel.fetchStudentAttendance(name, rollno)
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Neon_Cyan)
                            ) {
                                Text("Reload", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceByCourseCard(Course: AttendanceData, navController: NavController) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface_Elevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .shadow(8.dp, shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f) // Left content takes up remaining space
            ) {
                Text(
                    text = Course.course,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Text_Primary
                )
                Text(
                    text = "Batch: ${Course.batch}",
                    fontSize = 14.sp,
                    color = Text_Secondary
                )
                Text(
                    text = "Year: ${Course.courseYear}",
                    fontSize = 14.sp,
                    color = Text_Secondary
                )
            }

            // Fixed percentage calculation using proper division
            if(Course.totalTaken == 0){
                Text(
                    text = "No Classes\nTaken Yet",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                // Convert to Double for proper division, then round to nearest integer
                val percentage = ((Course.presentCount.toDouble() / Course.totalTaken.toDouble()) * 100).roundToInt()
                Log.d("AdminViewStudentAttendance", "Present: ${Course.presentCount}, Total: ${Course.totalTaken}, Percentage: ${percentage}")

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${percentage}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            percentage < 75 -> Neon_Red
                            percentage < 85 -> Neon_Yellow
                            else -> Neon_Green
                        }
                    )
                    Text(
                        text = "${Course.presentCount}/${Course.totalTaken}",
                        fontSize = 12.sp,
                        color = Text_Secondary
                    )
                }
            }
        }
    }
}