//package com.example.practice.adminApp
//
//import android.content.Context
//import android.net.Uri
//import android.util.Log
//import android.widget.Toast
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.AttachFile
//import androidx.compose.material.icons.filled.Email
//import androidx.compose.material.icons.filled.Person
//import androidx.compose.material3.Button
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedButton
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.SnackbarHost
//import androidx.compose.material3.SnackbarHostState
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.livedata.observeAsState
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.example.practice.R
//import kotlinx.coroutines.launch
//import java.io.File
//import java.io.FileOutputStream
//
//private const val TAG = "createProfessorAccount" // Tag for logging
//
//@Composable
//fun CreateProfessorAccounts(
//    modifier: Modifier = Modifier,
//    navController: NavController,
//    adminViewModel: AdminViewModel,
//) {
//    val snackbarHostState = remember { SnackbarHostState() }
//    val coroutineScope = rememberCoroutineScope()
//    val context = LocalContext.current
//    var isUploading by remember { mutableStateOf(false) }
//    var selectedFileName by remember { mutableStateOf<String?>(null) }
//    var name by remember { mutableStateOf("") }
//    var email by remember { mutableStateOf("") }
//
//    // Observe the professor registration state
//    val registrationState by adminViewModel.professorRegisterCsv.observeAsState()
//    val professorAccountCreated by adminViewModel.professorAccountCreated.observeAsState()
//
//    LaunchedEffect(professorAccountCreated) {
//        if(professorAccountCreated is AdminViewModel.CreateProfessorAccountState.Success){
//            Toast.makeText(context, "Professor account created successfully", Toast.LENGTH_LONG).show()
//            adminViewModel.resetCreateProfessorAccountState()
//            name = ""
//            email = ""
//        }
//        if(professorAccountCreated is AdminViewModel.CreateProfessorAccountState.Error){
//            Toast.makeText(context,
//                (professorAccountCreated as AdminViewModel.CreateProfessorAccountState.Error).message, Toast.LENGTH_LONG).show()
//            adminViewModel.resetCreateProfessorAccountState()
//        }
//    }
//
//    Scaffold(
//        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
//    ) { innerPadding ->
//        Box(modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState())) {
//            LaunchedEffect(registrationState) {
//                when (registrationState) {
//                    is AdminViewModel.ProfessorRegisterCsvState.Loading -> {
//                        isUploading = true
//                    }
//                    is AdminViewModel.ProfessorRegisterCsvState.Success -> {
//                        isUploading = false
//                        val data = (registrationState as AdminViewModel.ProfessorRegisterCsvState.Success).data
//                        val successMessage = "Successfully processed all ${data.total} professor records"
////                onSuccess(successMessage)
//                        coroutineScope.launch {
//                            snackbarHostState.showSnackbar(successMessage)
//                        }
//                        selectedFileName = null
//                    }
//                    is AdminViewModel.ProfessorRegisterCsvState.PartialSuccess -> {
//                        isUploading = false
//                        val state = (registrationState as AdminViewModel.ProfessorRegisterCsvState.PartialSuccess)
//                        val data = state.data
//
//                        // Show a toast with the detailed message
//                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
//
//                        // Also show a snackbar with summary
////                onSuccess("Processed ${data.total} records: ${data.success} successful, ${data.errors.size} failed")
//                        coroutineScope.launch {
//                            snackbarHostState.showSnackbar("Processed ${data.total} records: ${data.success} successful, ${data.errors.size} failed")
//                        }
//                        selectedFileName = null
//                    }
//                    is AdminViewModel.ProfessorRegisterCsvState.Error -> {
//                        isUploading = false
//                        val errorMessage = (registrationState as AdminViewModel.ProfessorRegisterCsvState.Error).message
////                onError(errorMessage)
//                        coroutineScope.launch {
//                            snackbarHostState.showSnackbar(errorMessage)
//                        }
//                    }
//                    null -> {
//                        // Initial state, do nothing
//                    }
//                }
//            }
//            val filePickerLauncher = rememberLauncherForActivityResult(
//                contract = ActivityResultContracts.GetContent()
//            ) { uri: Uri? ->
//                if (uri == null) {
//                    Log.w(TAG, "File selection cancelled or failed")
//                    return@rememberLauncherForActivityResult
//                }
//
//                Log.d(TAG, "File selected: $uri")
//                selectedFileName = getFileNameFromUri(context, uri)
//                Log.d(TAG, "Selected file name: $selectedFileName")
//
//                try {
//                    val file = createFileFromUri(context, uri)
//                    if (file != null) {
//                        Log.d(TAG, "Created file from URI: ${file.absolutePath}, size: ${file.length()} bytes")
//                        adminViewModel.registerStudentsCsv(file)
//                    } else {
//                        Log.e(TAG, "Failed to create file from URI")
////                onError("Failed to read the selected file")
//                        coroutineScope.launch {
//                            snackbarHostState.showSnackbar("Failed to read the selected file")
//                        }
//                    }
//                } catch (e: Exception) {
//                    Log.e(TAG, "Exception while processing file: ${e.message}", e)
////            onError("Error: ${e.message ?: "Unknown error occurred"}")
//                    coroutineScope.launch {
//                        snackbarHostState.showSnackbar("Error: ${e.message ?: "Unknown error occurred"}")
//                    }
//                }
//            }
//
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                // File upload info
//                if (registrationState is AdminViewModel.ProfessorRegisterCsvState.PartialSuccess) {
//                    val data = (registrationState as AdminViewModel.StudentRegisterCsvState.PartialSuccess).data
//
//                    Card(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(bottom = 16.dp),
//                        colors = CardDefaults.cardColors(
//                            containerColor = MaterialTheme.colorScheme.secondaryContainer
//                        )
//                    ) {
//                        Column(
//                            modifier = Modifier.padding(16.dp)
//                        ) {
//                            Text(
//                                text = "Last Upload Summary",
//                                style = MaterialTheme.typography.titleMedium,
//                                modifier = Modifier.padding(bottom = 8.dp)
//                            )
//                            Text("Total records: ${data.total}")
//                            Text("Successfully processed: ${data.success}")
//                            Text("Failed records: ${data.errors.size}")
//
//                            if (data.errors.isNotEmpty()) {
//                                Spacer(modifier = Modifier.height(8.dp))
//                                Text(
//                                    text = "Errors:",
//                                    style = MaterialTheme.typography.bodyMedium,
//                                    fontWeight = FontWeight.Bold
//                                )
//
//                                // Add scrolling container with fixed height for errors
//                                androidx.compose.foundation.lazy.LazyColumn(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .height(120.dp) // Fixed height for scroll area
//                                ) {
//                                    items(data.errors.size) { index ->
//                                        val error = data.errors[index]
//                                        Text(
//                                            text = "• ${error.email}: ${error.error}",
//                                            style = MaterialTheme.typography.bodySmall,
//                                            color = MaterialTheme.colorScheme.error,
//                                            modifier = Modifier.padding(vertical = 2.dp)
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//
//                Text(text = "Create Student Account", fontSize = 32.sp,fontWeight = FontWeight.Bold,
//                    color = MaterialTheme.colorScheme.primary,
//                    textAlign = TextAlign.Center,)
//
//                OutlinedTextField(
//                    value = name,
//                    onValueChange = { name = it.trim() }, // ✅ Trim input
//                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Name Icon") },
//                    label = { Text(text = "Name") },
//                    textStyle = TextStyle(color = Color.Black)
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                OutlinedTextField(
//                    value = rollNo,
//                    leadingIcon = { Icon(painter = painterResource(R.drawable.baseline_numbers_24), contentDescription = "Rollno Icon") },
//                    onValueChange = { rollNo = it.trim() }, // ✅ Trim input
//                    label = { Text(text = "Roll Number") },
//                    textStyle = TextStyle(color = Color.Black)
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                OutlinedTextField(
//                    value = email,
//                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Name Icon") },
//                    onValueChange = { email = it.trim() }, // ✅ Prevent accidental spaces
//                    label = { Text(text = "Email") },
//                    textStyle = TextStyle(color = Color.Black)
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Button(
//                    onClick = {
//                        val rollNumber = rollNo.toIntOrNull()
//                        if (name.isBlank() || email.isBlank() || rollNumber == null) {
//                            Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
//                        } else if (rollNumber == null) {
//                            Toast.makeText(context, "Roll Number must be a valid number", Toast.LENGTH_SHORT).show()
//                        } else {
//                            adminViewModel.createStudentAccount(email, name, rollNumber)
//                        }
//                    },
//                    enabled = !isUploading && adminViewModel.studentAccountCreated.value !is AdminViewModel.CreateStudentAccountState.Loading
//                ) {
//                    if (studentAccountCreated is AdminViewModel.CreateStudentAccountState.Loading) {
//                        CircularProgressIndicator(
//                            modifier = Modifier.size(24.dp),
//                            strokeWidth = 2.dp
//                        )
//                    } else {
//                        Text(text = "Create Account")
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//
//                OutlinedButton(
//                    onClick = {
//                        Log.d(TAG, "CSV upload button clicked")
//                        filePickerLauncher.launch("*/*")
//                    },
//                    modifier = Modifier.padding(vertical = 8.dp),
//                    enabled = !isUploading
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.AttachFile,
//                        contentDescription = "Upload CSV"
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Text("Upload Student CSV")
//                }
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                if (isUploading) {
//                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
//                    Text(text = "Uploading and processing...", style = MaterialTheme.typography.bodyMedium)
//                }
//
//                selectedFileName?.let {
//                    Text(
//                        text = "Selected file: $it",
//                        style = MaterialTheme.typography.bodySmall,
//                        modifier = Modifier.padding(top = 4.dp)
//                    )
//                }
//            }
//        }
//    }
//}