package com.example.practice.adminApp

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ResponsesModel.DetailedStudent
import com.example.practice.adminApp.AdminViewModel.DetailedStudentsState
import com.example.practice.adminApp.AdminViewModel.DeleteStudentState
import com.example.practice.ui.theme.*

@Composable
fun ViewAllStudents(
    modifier: Modifier = Modifier,
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    val studentsState by adminViewModel.detailedStudentsData.observeAsState()
    val deleteState by adminViewModel.deleteStudentState.observeAsState()

    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        adminViewModel.fetchDetailedStudents()
    }

    LaunchedEffect(deleteState) {
        if (deleteState is DeleteStudentState.Success) {
            adminViewModel.resetDeleteStudentState()
        }
    }

    var showDeleteConfirmFor by remember { mutableStateOf<Int?>(null) }

    if (showDeleteConfirmFor != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmFor = null },
            title = { Text("Delete Student", color = Text_Primary) },
            text = { Text("Are you sure you want to permanently delete this student?", color = Text_Secondary) },
            containerColor = Surface_Elevated,
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmFor?.let { adminViewModel.deleteStudent(it) }
                    showDeleteConfirmFor = null
                }) {
                    Text("Delete", color = Neon_Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmFor = null }) {
                    Text("Cancel", color = Neon_Cyan)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Deep)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Manage Students",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Text_Primary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            placeholder = { Text(text = "Search by name, roll no, or batch", color = Text_Secondary) },
            textStyle = TextStyle(color = Text_Primary),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Text_Secondary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        keyboardController?.hide()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Text_Secondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Neon_Cyan,
                unfocusedBorderColor = Surface_Elevated,
                focusedContainerColor = Surface_Elevated,
                unfocusedContainerColor = Surface_Elevated,
                cursorColor = Neon_Cyan
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = studentsState) {
            is DetailedStudentsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Neon_Cyan)
                }
            }
            is DetailedStudentsState.Success -> {
                val students = state.data.students
                if (students.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No students found.", color = Text_Secondary, fontSize = 16.sp)
                    }
                } else {
                    val filtered = if (searchQuery.isEmpty()) {
                        students
                    } else {
                        val q = searchQuery.lowercase()
                        students.filter {
                            it.name.lowercase().contains(q) ||
                            it.rollno.toString().contains(q) ||
                            it.batch.any { b -> b.lowercase().contains(q) }
                        }
                    }

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No results found.", color = Text_Secondary, fontSize = 16.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id }) { student ->
                                DetailedStudentCard(
                                    student = student,
                                    onDelete = { showDeleteConfirmFor = student.id },
                                    navController = navController
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
            is DetailedStudentsState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = Neon_Red, fontSize = 14.sp)
                }
            }
            else -> {}
        }
    }
}

@Composable
fun DetailedStudentCard(
    student: DetailedStudent,
    onDelete: () -> Unit,
    navController: NavController
) {
    val pct = student.attendancePercentage
    val isCritical = pct != null && pct < 75

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                navController.navigate("ViewRecordAttendance/${student.name}/${student.rollno}")
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface_Elevated),
        border = BorderStroke(1.dp, if (isCritical) Neon_Red.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Surface_Elevated, CircleShape)
                    .border(2.dp, if (isCritical) Neon_Red else Neon_Cyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!student.profilePicture.isNullOrEmpty()) {
                    var bitmapMap: android.graphics.Bitmap? = null
                    try {
                        val prefix = "data:image/jpeg;base64,"
                        val cleanStr = if (student.profilePicture.startsWith(prefix)) 
                            student.profilePicture.substring(prefix.length) 
                        else 
                            student.profilePicture
                        
                        val decodedBytes = Base64.decode(cleanStr, Base64.DEFAULT)
                        bitmapMap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    } catch (e: Exception) {
                    }
                    if (bitmapMap != null) {
                        Image(
                            bitmap = bitmapMap.asImageBitmap(),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(student.name.take(1).uppercase(), color = Neon_Cyan, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(student.name.take(1).uppercase(), color = Neon_Cyan, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = student.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Text_Primary
                    )
                    if (isCritical) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Warning, contentDescription = "Critical", tint = Neon_Red, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Roll No: ${student.rollno}", fontSize = 13.sp, color = Text_Secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("•", fontSize = 13.sp, color = Text_Secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    val batchStr = if (student.batch.isNotEmpty()) student.batch.joinToString(", ") else "None"
                    Text("Batch: $batchStr", fontSize = 13.sp, color = Text_Secondary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Face Registration status
                    if (student.hasRegisteredFace) {
                        Icon(Icons.Default.CheckCircle, null, tint = Neon_Green, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Face Enrolled", fontSize = 12.sp, color = Neon_Green)
                    } else {
                        Icon(Icons.Default.ErrorOutline, null, tint = Neon_Yellow, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("No Face Profile", fontSize = 12.sp, color = Neon_Yellow)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Attendance Percentage
                    if (pct != null) {
                        val attColor = when {
                            pct >= 85 -> Neon_Green
                            pct >= 75 -> Neon_Cyan
                            pct >= 60 -> Neon_Yellow
                            else -> Neon_Red
                        }
                        Text("Attendance: $pct%", fontSize = 12.sp, color = attColor, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("Attendance: N/A", fontSize = 12.sp, color = Text_Secondary)
                    }
                }
            }

            // Delete Action
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Neon_Red.copy(alpha = 0.8f))
            }
        }
    }
}