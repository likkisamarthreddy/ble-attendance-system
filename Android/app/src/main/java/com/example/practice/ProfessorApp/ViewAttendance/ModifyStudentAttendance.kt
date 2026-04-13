package com.example.practice.ProfessorApp.ViewAttendance

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.ResponsesModel.AttendanceXX
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ModifyStudentAttendance(
    modifier: Modifier,
    professorViewModel: ProfessorViewModel,
    navController: NavController,
    courseId: String,
    courseBatch: String,
    recordDate: String,
    id: String
){
    val context = LocalContext.current
    val records by professorViewModel.currentRecords.observeAsState()
    val modifyAttendanceState by professorViewModel.modifyAttendanceState.observeAsState()

    val selectedStudents = remember { mutableStateMapOf<Int, Boolean>() }
    var searchQuery by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var navigateTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(records) {
        records?.forEach { student ->
            selectedStudents[student.rollno] = student.status == "Present"
        }
    }

    LaunchedEffect(modifyAttendanceState) {
        when (modifyAttendanceState) {
            is ProfessorViewModel.ModifyAttendanceState.Success -> {
                Toast.makeText(context, "Attendance updated!", Toast.LENGTH_SHORT).show()
                navigateTriggered = true
                delay(1000)
                navController.popBackStack()
            }
            is ProfessorViewModel.ModifyAttendanceState.Error -> {
                val errorMessage = (modifyAttendanceState as ProfessorViewModel.ModifyAttendanceState.Error).message
                Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                isSubmitting = false
            }
            is ProfessorViewModel.ModifyAttendanceState.Loading -> {
                isSubmitting = true
            }
            else -> {}
        }
    }

    AnimatedGradientBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Modify Attendance",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Text_Primary
            )
            Text(
                text = "$courseId ($courseBatch)",
                style = MaterialTheme.typography.bodyMedium,
                color = Text_Secondary
            )
            Text(
                text = formatDate(recordDate),
                style = MaterialTheme.typography.bodySmall,
                color = Neon_Cyan
            )

            Spacer(modifier = Modifier.height(16.dp))

            val keyboardController = LocalSoftwareKeyboardController.current

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Roll No", color = Text_Secondary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Neon_Cyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Text_Primary,
                    unfocusedTextColor = Text_Primary,
                    focusedBorderColor = Neon_Cyan,
                    unfocusedBorderColor = Surface_Elevated,
                    cursorColor = Neon_Cyan
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (records != null) {
                val filteredStudents = records!!.sortedBy { it.rollno }.filter { student ->
                    student.rollno.toString().startsWith(searchQuery, ignoreCase = true)
                }

                if (filteredStudents.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredStudents) { student ->
                            ModifyStudentAttendanceCard(
                                Attendance = student,
                                isChecked = selectedStudents[student.rollno] ?: false,
                                onCheckedChange = { isChecked ->
                                    selectedStudents[student.rollno] = isChecked
                                }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No students found", color = Text_Secondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientButton(
                    text = if (isSubmitting) "Saving..." else "Save Changes",
                    isLoading = isSubmitting,
                    onClick = {
                        if (!isSubmitting && !navigateTriggered) {
                            val selectedStudentIds = selectedStudents.entries.filter { it.value }.map { it.key }
                            professorViewModel.modifyStudentsAttendance(selectedStudentIds, id)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting && !navigateTriggered
                )

                TextButton(
                    onClick = { if (!navigateTriggered) navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !navigateTriggered
                ) {
                    Text("Cancel", color = Text_Secondary)
                }
            }
        }
    }
}

@Composable
fun ModifyStudentAttendanceCard(
    Attendance: AttendanceXX,
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
                    text = Attendance.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Text_Primary
                )
                Text(
                    text = "Roll No: ${Attendance.rollno}",
                    fontSize = 14.sp,
                    color = Text_Secondary
                )
            }

            Checkbox(
                checked = isChecked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = Neon_Cyan,
                    uncheckedColor = Text_Secondary,
                    checkmarkColor = Background_Deep
                )
            )
        }
    }
}