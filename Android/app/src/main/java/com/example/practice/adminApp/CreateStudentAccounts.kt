package com.example.practice.adminApp

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.R
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val TAG = "createStudentAccount" // Tag for logging

@Composable
fun CreateStudentAccounts(
    modifier: Modifier = Modifier,
    navController: NavController,
    adminViewModel: AdminViewModel,
    isProfessor: Boolean,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isUploading by remember { mutableStateOf(false) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // Observe the student registration state
    val registrationState by adminViewModel.studentRegisterCsv.observeAsState()
    val studentAccountCreated by adminViewModel.studentAccountCreated.observeAsState()

    LaunchedEffect(studentAccountCreated) {
        if(studentAccountCreated is AdminViewModel.CreateStudentAccountState.Success){
            Toast.makeText(context, if(!isProfessor) "Student account created successfully" else "Professor account created successfully", Toast.LENGTH_LONG).show()
            adminViewModel.resetCreateStudentAccountState()
            name = ""
            rollNo = ""
            email = ""
        }
        if(studentAccountCreated is AdminViewModel.CreateStudentAccountState.Error){
            Toast.makeText(context,
                (studentAccountCreated as AdminViewModel.CreateStudentAccountState.Error).message, Toast.LENGTH_LONG).show()
            adminViewModel.resetCreateStudentAccountState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState())) {
            LaunchedEffect(registrationState) {
                when (registrationState) {
                    is AdminViewModel.StudentRegisterCsvState.Loading -> {
                        isUploading = true
                    }
                    is AdminViewModel.StudentRegisterCsvState.Success -> {
                        isUploading = false
                        val data = (registrationState as AdminViewModel.StudentRegisterCsvState.Success).data
                        val successMessage = if(!isProfessor) "Successfully processed all ${data.total} student records" else "Successfully processed all ${data.total} professor records"
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(successMessage)
                        }
                        selectedFileName = null
                    }
                    is AdminViewModel.StudentRegisterCsvState.PartialSuccess -> {
                        isUploading = false
                        val state = (registrationState as AdminViewModel.StudentRegisterCsvState.PartialSuccess)
                        val data = state.data

                        // Show a toast with the detailed message
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()

                        // Also show a snackbar with summary
//                onSuccess("Processed ${data.total} records: ${data.success} successful, ${data.errors.size} failed")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Processed ${data.total} records: ${data.success} successful, ${data.errors.size} failed")
                        }
                        selectedFileName = null
                    }
                    is AdminViewModel.StudentRegisterCsvState.Error -> {
                        isUploading = false
                        val errorMessage = (registrationState as AdminViewModel.StudentRegisterCsvState.Error).message
//                onError(errorMessage)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(errorMessage)
                        }
                    }
                    null -> {
                        // Initial state, do nothing
                    }
                }
            }
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri == null) {
                    Log.w(TAG, "File selection cancelled or failed")
                    return@rememberLauncherForActivityResult
                }

                Log.d(TAG, "File selected: $uri")
                selectedFileName = getFileNameFromUri(context, uri)
                Log.d(TAG, "Selected file name: $selectedFileName")

                try {
                    val file = createFileFromUri(context, uri)
                    if (file != null) {
                        Log.d(TAG, "Created file from URI: ${file.absolutePath}, size: ${file.length()} bytes")
                        adminViewModel.registerStudentsCsv(file, isProfessor)
                    } else {
                        Log.e(TAG, "Failed to create file from URI")
//                onError("Failed to read the selected file")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Failed to read the selected file")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while processing file: ${e.message}", e)
//            onError("Error: ${e.message ?: "Unknown error occurred"}")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Error: ${e.message ?: "Unknown error occurred"}")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // File upload info
                if (registrationState is AdminViewModel.StudentRegisterCsvState.PartialSuccess) {
                    val data = (registrationState as AdminViewModel.StudentRegisterCsvState.PartialSuccess).data

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Last Upload Summary",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text("Total records: ${data.total}")
                            Text("Successfully processed: ${data.success}")
                            Text("Failed records: ${data.errors.size}")

                            if (data.errors.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Errors:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                // Add scrolling container with fixed height for errors
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp) // Fixed height for scroll area
                                ) {
                                    items(data.errors.size) { index ->
                                        val error = data.errors[index]
                                        Text(
                                            text = "• ${error.email}: ${error.error}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(text = if(!isProfessor)"Create Student Account" else "Create Professor Account", fontSize = 32.sp,fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.trim() }, // ✅ Trim input
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Name Icon") },
                    label = { Text(text = "Name") },
                    textStyle = TextStyle(color = Color.Black)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if(!isProfessor) {
                    OutlinedTextField(
                        value = rollNo,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.baseline_numbers_24),
                                contentDescription = "Rollno Icon"
                            )
                        },
                        onValueChange = { rollNo = it.trim() }, // ✅ Trim input
                        label = { Text(text = "Roll Number") },
                        textStyle = TextStyle(color = Color.Black)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = email,
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Name Icon") },
                    onValueChange = { email = it.trim() }, // ✅ Prevent accidental spaces
                    label = { Text(text = "Email") },
                    textStyle = TextStyle(color = Color.Black)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {

                        var rollNumber = rollNo.toIntOrNull()
                        if (name.isBlank() || email.isBlank()) {
                            Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                        }else if (!isProfessor && (rollNo.isBlank() || rollNumber == null)) {
                            Toast.makeText(context, "Roll Number must be a valid number", Toast.LENGTH_SHORT).show()
                        } else {
                            if(rollNumber == null){
                                rollNumber = 0
                            }
                            adminViewModel.createStudentAccount(email, name, rollNumber, isProfessor)
                        }
                    },
                    enabled = !isUploading && adminViewModel.studentAccountCreated.value !is AdminViewModel.CreateStudentAccountState.Loading
                ) {
                    if (studentAccountCreated is AdminViewModel.CreateStudentAccountState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "Create Account")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


                OutlinedButton(
                    onClick = {
                        Log.d(TAG, "CSV upload button clicked")
                        filePickerLauncher.launch("*/*")
                    },
                    modifier = Modifier.padding(vertical = 8.dp),
                    enabled = !isUploading
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Upload CSV"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if(!isProfessor) "Upload Student CSV" else "Upload Professor CSV")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    Text(text = "Uploading and processing...", style = MaterialTheme.typography.bodyMedium)
                }

                selectedFileName?.let {
                    Text(
                        text = "Selected file: $it",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }


                val tableData : List<List<String>>
                if(isProfessor){
                    tableData = listOf(
                        listOf("name", "Student1", "Student1"), // Header
                        listOf("email", "student1@iiitg.ac.in", "student2@iiitg.ac.in"),
                        listOf("password", "optional", "optional")
                    )
                } else{
                    tableData = listOf(
                        listOf("name", "Student1", "Student1"), // Header
                        listOf("email", "student1@iiitg.ac.in", "student2@iiitg.ac.in"),
                        listOf("rollno", "2201001", "2201002"),
                        listOf("password", "optional", "optional")
                    )
                }


// Fixed table with horizontal scroll
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CSV Format Example:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Scrollable table
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 8.dp)
                        ) {
                            tableData.forEachIndexed { rowIndex, rowData ->
                                Column(
                                    modifier = Modifier.padding(end = if (rowIndex < tableData.size - 1) 16.dp else 0.dp)
                                ) {
                                    rowData.forEachIndexed { cellIndex, cell ->
                                        Text(
                                            text = cell,
                                            modifier = Modifier
                                                .padding(vertical = 4.dp)
                                                .width(120.dp), // Fixed width for each column
                                            fontWeight = if (cellIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp,
                                            color = Color.Black,
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

// Fixed text fields - removed incorrect weight modifiers
                Text(
                    text = "If password is not provided, then it would be taken from email as password@iiitg.ac.in",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Start
                )

                Text(
                    text = "Password must be at least 6 characters long and make sure to upload only csv",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Start
                )

            }
        }
    }
}


fun getFileNameFromUri(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && it.moveToFirst()) {
            return it.getString(nameIndex)
        }
        null
    } ?: uri.lastPathSegment
}

fun createFileFromUri(context: Context, uri: Uri): File? {
    return try {
        Log.d(TAG, "Creating file from URI: $uri")
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.e(TAG, "Failed to open input stream for URI")
            return null
        }

        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.csv") // Unique temp file name
        Log.d(TAG, "Creating temporary file: ${file.absolutePath}")

        val outputStream = FileOutputStream(file)
        val buffer = ByteArray(1024)
        var read: Int
        var totalRead = 0

        while (inputStream.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
            totalRead += read
        }

        Log.d(TAG, "File copy complete. Total bytes: $totalRead")
        inputStream.close()
        outputStream.close()
        file
    } catch (e: Exception) {
        Log.e(TAG, "Exception in createFileFromUri: ${e.message}", e)
        null
    }
}
