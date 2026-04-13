package com.example.practice.ProfessorApp.ViewAttendance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.practice.ResponsesModel.AttendanceXX
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*

@Composable
fun ViewRecordAttendance(
    navController: NavController,
    recordId: String,
    recordDate: String,
    courseId: String,
    courseBatch: String,
    modifier: Modifier = Modifier,
    professorViewModel: ProfessorViewModel
){
    val viewRecordData by professorViewModel.viewRecordData.observeAsState()
    var studentCount by remember { mutableIntStateOf(0) }
    var presentStudentCount by remember { mutableIntStateOf(0) }
    var sortByRollNoAscending by remember { mutableStateOf(true) }
    val isArchivedASelected by professorViewModel.isArchivedSelected.observeAsState()

    DisposableEffect(Unit) {
        onDispose { professorViewModel.resetViewRecordData() }
    }

    LaunchedEffect(Unit) {
        isArchivedASelected?.let { professorViewModel.fetchRecordData(recordId, it) }
    }

    AnimatedGradientBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "${formatDate(recordDate)}",
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

            // Stats Card
            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttendanceStat(label = "Total", value = studentCount.toString(), color = PrimaryIndigo)
                    
                    val percentage = if (studentCount > 0) (presentStudentCount.toFloat() / studentCount) else 0f
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier.size(60.dp),
                            color = if (percentage >= 0.75f) SuccessGreen else if (percentage >= 0.6f) WarningAmber else ErrorCoral,
                            trackColor = DarkSurfaceVariant
                        )
                        Text("${(percentage * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }

                    AttendanceStat(label = "Present", value = presentStudentCount.toString(), color = SuccessGreen)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sort & List
            when(val state = viewRecordData){
                is ProfessorViewModel.ViewRecordDataState.Loading -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                }
                is ProfessorViewModel.ViewRecordDataState.Success -> {
                    val records = state.data
                    if (records.isNotEmpty()) {
                        studentCount = records.size
                        presentStudentCount = records.count { it.status == "Present" }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            FilterChip(
                                selected = sortByRollNoAscending,
                                onClick = { sortByRollNoAscending = !sortByRollNoAscending },
                                label = { Text(if (sortByRollNoAscending) "Roll No (Asc)" else "Roll No (Desc)") },
                                leadingIcon = { Icon(Icons.Default.Sort, null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryIndigo, labelColor = TextPrimaryDark)
                            )
                        }

                        val sortedRecords = if (sortByRollNoAscending) {
                            records.sortedBy { try { it.rollno.toInt() } catch (e: Exception) { Int.MAX_VALUE } }
                        } else {
                            records.sortedByDescending { try { it.rollno.toInt() } catch (e: Exception) { Int.MIN_VALUE } }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(sortedRecords) { record ->
                                RecordDataCard(record, navController = navController)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if(isArchivedASelected == false) {
                            GradientButton(
                                text = "Modify Attendance",
                                onClick = {
                                    professorViewModel.setCurrentRecords(records)
                                    navController.navigate("modifyAttendance/$courseId/$courseBatch/${recordDate}/${recordId}")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("No students enrolled", color = TextSecondaryDark)
                        }
                    }
                }
                is ProfessorViewModel.ViewRecordDataState.Error -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = ErrorCoral)
                    }
                }
                null -> {}
                ProfessorViewModel.ViewRecordDataState.Idle -> {}
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
fun RecordDataCard(record: AttendanceXX, navController: NavController) {
    val isPresent = record.status == "Present"
    GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${record.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Roll No: ${record.rollno}",
                    fontSize = 14.sp,
                    color = TextSecondaryDark
                )
            }
            
            Surface(
                color = if (isPresent) SuccessGreen.copy(alpha = 0.15f) else ErrorCoral.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = record.status,
                    color = if (isPresent) SuccessGreen else ErrorCoral,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}