package com.example.practice.ProfessorApp.HomePages

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.ProfessorApp.ProfessorViewModel.AllStudentsState
import com.example.practice.ResponsesModel.Student
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*
import java.io.BufferedReader
import java.io.InputStreamReader

private const val TAG = "ModifyStudentsInCourse"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ModifyStudentsInCourse(
    navController: NavController,
    courseId: String,
    courseBatch: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val professorViewModel: ProfessorViewModel = viewModel()
    val studentData by professorViewModel.courseStudentsData.observeAsState()
    val allStudentsData by professorViewModel.AllStudentsData.observeAsState()

    val selectedStudents = remember { mutableStateMapOf<Int, Boolean>() }
    var searchQuery by remember { mutableStateOf("") }
    var unregisteredStudents by remember { mutableStateOf(listOf<String>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            processCsvFile(context, it, selectedStudents, allStudentsData) { unregistered ->
                unregisteredStudents = unregistered
                if (unregistered.isNotEmpty()) {
                    errorMessage = "Roll numbers not found: ${unregistered.joinToString(", ")}"
                    showErrorDialog = true
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        professorViewModel.fetchAllStudents()
    }

    LaunchedEffect(studentData, allStudentsData) {
        if (studentData is ProfessorViewModel.CourseStudentsState.Success && allStudentsData is AllStudentsState.Success) {
            val courseStudents = (studentData as ProfessorViewModel.CourseStudentsState.Success).students
            val courseStudentIds = courseStudents.map { it.rollno }.toSet()
            val allStudents = (allStudentsData as AllStudentsState.Success).student
            allStudents.forEach { student ->
                selectedStudents[student.rollno] = courseStudentIds.contains(student.rollno)
            }
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
                    text = "Manage Students",
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

                // Search & Upload
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Roll No", color = TextSecondaryDark) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryIndigo) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = GlassBorder,
                            cursorColor = PrimaryIndigo
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.background(DarkSurfaceVariant, androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.AttachFile, "Upload CSV", tint = PrimaryIndigo)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Student List
                when (val state = allStudentsData) {
                    is AllStudentsState.Loading -> {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryIndigo)
                        }
                    }
                    is AllStudentsState.Success -> {
                        val filteredStudents = state.student.filter { 
                            it.rollno.toString().contains(searchQuery, ignoreCase = true) 
                        }

                        if (filteredStudents.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredStudents) { student ->
                                    AllStudentCard(
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
                                Text("No matching students found", color = TextSecondaryDark)
                            }
                        }
                    }
                    is AllStudentsState.Error -> {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.message}", color = ErrorCoral)
                        }
                    }
                    null -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                GradientButton(
                    text = "Save Changes",
                    onClick = {
                        val selectedStudentIds = selectedStudents.entries.filter { it.value }.map { it.key }
                        // professorViewModel.modifyStudents(courseId, courseBatch, selectedStudentIds)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (showErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showErrorDialog = false },
                    title = { Text("CSV Import Issues", color = TextPrimaryDark) },
                    text = {
                        Column {
                            Text("Unregistered Roll Numbers:", color = TextSecondaryDark)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                                Text(unregisteredStudents.joinToString(", "), color = ErrorCoral)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showErrorDialog = false }) { Text("Close", color = PrimaryIndigo) }
                    },
                    containerColor = DarkSurface,
                    textContentColor = TextSecondaryDark,
                    titleContentColor = TextPrimaryDark
                )
            }
        }
    }
}

fun processCsvFile(
    context: Context,
    uri: Uri,
    selectedStudents: MutableMap<Int, Boolean>,
    allStudentsData: AllStudentsState?,
    onUnregisteredStudents: (List<String>) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val rollNumbers = mutableListOf<String>()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            line?.trim()?.let { if (it.isNotEmpty()) rollNumbers.add(it) }
        }
        reader.close()

        val unregisteredRollNumbers = mutableListOf<String>()

        if (allStudentsData is AllStudentsState.Success) {
            val allStudents = allStudentsData.student
            val allStudentRollNumberMap = allStudents.associateBy { it.rollno.toString() }

            selectedStudents.keys.forEach { selectedStudents[it] = false }

            rollNumbers.forEach { rollNo ->
                try {
                    val cleanedRollNo = rollNo.trim().replace("\uFEFF", "")
                    val rollNoInt = cleanedRollNo.toInt()
                    if (allStudentRollNumberMap.containsKey(rollNoInt.toString())) {
                        selectedStudents[rollNoInt] = true
                    } else {
                        unregisteredRollNumbers.add(rollNo)
                    }
                } catch (e: Exception) {
                    unregisteredRollNumbers.add(rollNo)
                }
            }
        } else {
            unregisteredRollNumbers.addAll(rollNumbers)
        }
        onUnregisteredStudents(unregisteredRollNumbers)
    } catch (e: Exception) {
        onUnregisteredStudents(listOf("Error: ${e.message}"))
    }
}

@Composable
fun AllStudentCard(
    student: Student,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassmorphismCard(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!isChecked) }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(student.name, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 16.sp)
                Text("Roll: ${student.rollno}", color = TextSecondaryDark, fontSize = 14.sp)
            }
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryIndigo,
                    uncheckedColor = TextSecondaryDark,
                    checkmarkColor = Color.White
                )
            )
        }
    }
}
