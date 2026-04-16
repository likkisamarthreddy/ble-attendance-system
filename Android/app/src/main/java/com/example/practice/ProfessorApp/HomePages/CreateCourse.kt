package com.example.practice.ProfessorApp.HomePages

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.ProfessorApp.ProfessorViewModel.CreateCourseState
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*
import java.util.Calendar

@Composable
fun CreateCourse(
    navController: NavController,
    modifier: Modifier = Modifier,
    professorViewModel: ProfessorViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        professorViewModel.resetCreateCourseState()
    }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val createCourseState by professorViewModel.createCourseState.observeAsState()

    var courseName by remember { mutableStateOf("") }
    var courseBatch by remember { mutableStateOf("") }
    var courseYear by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()) }
    var courseExpiry by remember { mutableStateOf("") }
    var courseNameError by remember { mutableStateOf("") }
    var courseBatchError by remember { mutableStateOf("") }
    var courseYearError by remember { mutableStateOf("") }
    var courseExpiryError by remember { mutableStateOf("") }

    LaunchedEffect(createCourseState) {
        when (createCourseState) {
            is CreateCourseState.Success -> {
                Toast.makeText(context, "Course created successfully!", Toast.LENGTH_SHORT).show()
                navController.navigate("professorHome") {
                    popUpTo("professorHome") { inclusive = true }
                }
            }
            is CreateCourseState.Error -> {
                val errorMsg = (createCourseState as CreateCourseState.Error).message
                snackbarHostState.showSnackbar(errorMsg)
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        AnimatedGradientBackground(
            modifier = modifier.padding(paddingValues),
            colors = listOf(DarkBackground, DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Create New Course",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        // Course Name
                        OutlinedTextField(
                            value = courseName,
                            onValueChange = {
                                courseName = it
                                courseNameError = if (it.isBlank()) "Required" else ""
                            },
                            label = { Text("Course Name", color = TextSecondaryDark) },
                            isError = courseNameError.isNotEmpty(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark,
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = GlassBorder,
                                cursorColor = PrimaryIndigo
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (courseNameError.isNotEmpty()) {
                            Text(courseNameError, color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Batch
                        OutlinedTextField(
                            value = courseBatch,
                            onValueChange = {
                                courseBatch = it
                                courseBatchError = if (it.isBlank()) "Required" else ""
                            },
                            label = { Text("Batch (e.g. 2024-A)", color = TextSecondaryDark) },
                            isError = courseBatchError.isNotEmpty(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark,
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = GlassBorder,
                                cursorColor = PrimaryIndigo
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (courseBatchError.isNotEmpty()) {
                            Text(courseBatchError, color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Year
                        OutlinedTextField(
                            value = courseYear,
                            onValueChange = {
                                courseYear = it.filter { c -> c.isDigit() }
                                courseYearError = if (it.isBlank()) "Required" else ""
                            },
                            label = { Text("Year (e.g. 2026)", color = TextSecondaryDark) },
                            isError = courseYearError.isNotEmpty(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark,
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = GlassBorder,
                                cursorColor = PrimaryIndigo
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (courseYearError.isNotEmpty()) {
                            Text(courseYearError, color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
                        }

                        // Expiry Date
                        val calendar = Calendar.getInstance()
                        var showDatePicker by remember { mutableStateOf(false) }

                        if (showDatePicker) {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    courseExpiry = "$d/${m + 1}/$y"
                                    courseExpiryError = if (courseExpiry.isBlank()) "Required" else ""
                                    showDatePicker = false
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }

                        Text("Course Expiry", color = TextSecondaryDark, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (courseExpiryError.isNotEmpty()) ErrorCoral else GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (courseExpiry.isNotEmpty()) courseExpiry else "Select Date",
                                    color = if (courseExpiry.isNotEmpty()) TextPrimaryDark else TextSecondaryDark
                                )
                                Icon(Icons.Default.DateRange, null, tint = PrimaryIndigo)
                            }
                        }
                        if (courseExpiryError.isNotEmpty()) {
                            Text(courseExpiryError, color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        GradientButton(
                            text = "Create Course",
                            isLoading = createCourseState is CreateCourseState.Loading,
                            onClick = {
                                courseNameError = if (courseName.isBlank()) "Required" else ""
                                courseBatchError = if (courseBatch.isBlank()) "Required" else ""
                                courseYearError = if (courseYear.isBlank()) "Required" else ""
                                courseExpiryError = if (courseExpiry.isBlank()) "Required" else ""

                                if (courseNameError.isEmpty() && courseBatchError.isEmpty() && courseYearError.isEmpty() && courseExpiryError.isEmpty()) {
                                    val year = courseYear.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                    professorViewModel.createCourse(courseName, courseBatch, courseExpiry, year)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        TextButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel", color = TextSecondaryDark)
                        }
                    }
                }
            }
        }
    }
}