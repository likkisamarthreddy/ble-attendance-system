package com.example.practice.adminApp

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.practice.ProfessorApp.ViewAttendance.RecordDataCard
import com.example.practice.ProfessorApp.ViewAttendance.formatDate
import com.example.practice.adminApp.AdminViewModel.ViewRecordDataState

@Composable
fun AdminViewRecordAttendance(
    navController: NavController,
    recordId: String,
    recordDate: String,
    courseId: String,
    courseBatch: String,
    modifier: Modifier,
    adminViewModel: AdminViewModel
){
    val viewRecordData by adminViewModel.viewRecordData.observeAsState()
    var studentCount by remember { mutableIntStateOf(0) }
    var presentStudentCount by remember { mutableIntStateOf(0) }
    var sortByRollNoAscending by remember { mutableStateOf(true) }
    val isArchivedASelected by adminViewModel.isArchivedSelected.observeAsState()

    DisposableEffect(Unit) {
        onDispose {
            adminViewModel.resetViewRecordData()
        }
    }

    LaunchedEffect(Unit) {
        isArchivedASelected?.let { adminViewModel.fetchRecordData(recordId, it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){

        Text(
            text = "Attendance Record for $courseId ($courseBatch) - ${formatDate(recordDate)}",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Total Students present- $presentStudentCount/$studentCount",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = modifier.height(10.dp))

        when(val state = viewRecordData){
            is ViewRecordDataState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is ViewRecordDataState.Success ->    {
                val records = state.data

                if (records.isNotEmpty()) {
                    studentCount = records.size
                    presentStudentCount = records.count { it.status == "Present" }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = { sortByRollNoAscending = !sortByRollNoAscending }) {
                            Text(text = if (sortByRollNoAscending) "Sort ↓" else "Sort ↑")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sort the records based on roll number
                    val sortedRecords = if (sortByRollNoAscending) {
                        records.sortedBy {
                            // Try to parse roll number as an integer if possible
                            try {
                                it.rollno.toInt()
                            } catch (e: NumberFormatException) {
                                // If roll number is not a pure number, sort as string
                                Int.MAX_VALUE
                            }
                        }
                    } else {
                        records.sortedByDescending {
                            try {
                                it.rollno.toInt()
                            } catch (e: NumberFormatException) {
                                Int.MIN_VALUE
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedRecords) { record ->
                            RecordDataCard(record, navController = navController)
                        }
                    }
//                    if(isArchivedASelected == false) {
//                        Button(
//                            onClick = {
//                                adminViewModel.setCurrentRecords(records)
//                                navController.navigate("modifyAttendance/$courseId/$courseBatch/${recordDate}/${recordId}")
//                            },
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 16.dp)
//                        ) {
//                            Text(
//                                text = "Modify Attendance",
//                                fontSize = 16.sp,
//                                fontWeight = FontWeight.Medium
//                            )
//                        }
//                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Student Enrolled in this Course",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp
                        )
                    }
                }
            }


            is ViewRecordDataState.Error -> {
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
                                isArchivedASelected?.let {
                                    adminViewModel.fetchRecordData(recordId,
                                        it
                                    )
                                }
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
                        text = "Loading Students...",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp
                    )
                }
            }

            ViewRecordDataState.Idle -> {}
        }

    }

    Spacer(modifier = Modifier.height(8.dp))

}