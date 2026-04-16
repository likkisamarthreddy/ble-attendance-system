package com.example.practice.adminApp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ResponsesModel.SearchStudent
import com.example.practice.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStudentExplorer(
    modifier: Modifier = Modifier,
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    val searchState by adminViewModel.searchStudentsData.observeAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf("") }
    var branchFilter by remember { mutableStateOf("") }
    var sectionFilter by remember { mutableStateOf("") }
    var yearFilter by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }

    val branchOptions = listOf("All", "CSE", "ECE", "EEE", "MECH", "CIVIL", "IT", "AIML", "DS")
    val sectionOptions = listOf("All", "A", "B", "C", "D")
    val yearOptions = listOf("All", "1", "2", "3", "4")

    LaunchedEffect(branchFilter, sectionFilter, yearFilter) {
        val b = if (branchFilter == "All" || branchFilter.isEmpty()) null else branchFilter
        val s = if (sectionFilter == "All" || sectionFilter.isEmpty()) null else sectionFilter
        val y = if (yearFilter == "All" || yearFilter.isEmpty()) null else yearFilter
        
        // Try parsing searchQuery as rollno or name
        val rollno = searchQuery.toIntOrNull()?.toString()
        val name = if (rollno == null && searchQuery.isNotEmpty()) searchQuery else null
        
        adminViewModel.searchStudents(b, s, y, rollno, name)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Deep)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonSearch, contentDescription = "Explorer", tint = Neon_Purple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Student Explorer", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Text_Primary)
            }
            IconButton(onClick = { showFilters = !showFilters }) {
                Icon(
                    imageVector = if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = if (showFilters) Neon_Cyan else Text_Secondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            placeholder = { Text("Search by RollNo or Name", color = Text_Secondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Text_Secondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { 
                        searchQuery = "" 
                        keyboardController?.hide()
                        // Re-trigger search with empty query
                        val b = if (branchFilter == "All" || branchFilter.isEmpty()) null else branchFilter
                        val s = if (sectionFilter == "All" || sectionFilter.isEmpty()) null else sectionFilter
                        val y = if (yearFilter == "All" || yearFilter.isEmpty()) null else yearFilter
                        adminViewModel.searchStudents(b, s, y, null, null)
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Text_Secondary)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Neon_Cyan,
                unfocusedBorderColor = Surface_Elevated,
                focusedContainerColor = Surface_Elevated,
                unfocusedContainerColor = Surface_Elevated,
                focusedTextColor = Text_Primary,
                unfocusedTextColor = Text_Primary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { 
                keyboardController?.hide()
                val b = if (branchFilter == "All" || branchFilter.isEmpty()) null else branchFilter
                val s = if (sectionFilter == "All" || sectionFilter.isEmpty()) null else sectionFilter
                val y = if (yearFilter == "All" || yearFilter.isEmpty()) null else yearFilter
                val rollno = searchQuery.toIntOrNull()?.toString()
                val name = if (rollno == null && searchQuery.isNotEmpty()) searchQuery else null
                adminViewModel.searchStudents(b, s, y, rollno, name)
            })
        )

        AnimatedVisibility(visible = showFilters) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                // Branch 
                Text("Branch", color = Text_Secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                ) {
                    items(branchOptions) { b ->
                        FilterChipLite(text = b, isSelected = branchFilter == b) { branchFilter = b }
                    }
                }
                
                // Section
                Text("Section", color = Text_Secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                ) {
                    items(sectionOptions) { s ->
                        FilterChipLite(text = s, isSelected = sectionFilter == s) { sectionFilter = s }
                    }
                }
                
                // Year
                Text("Year", color = Text_Secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                ) {
                    items(yearOptions) { y ->
                        FilterChipLite(text = y, isSelected = yearFilter == y) { yearFilter = y }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = searchState) {
            is AdminViewModel.SearchStudentState.Loading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Neon_Cyan)
                }
            }
            is AdminViewModel.SearchStudentState.Success -> {
                val students = state.data.students
                
                Text(
                    "Found ${students.size} students", 
                    color = Neon_Cyan, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (students.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No students match the criteria.", color = Text_Secondary, fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(students) { student ->
                            StudentExplorerCard(student)
                        }
                    }
                }
            }
            is AdminViewModel.SearchStudentState.Error -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Neon_Red, fontSize = 14.sp)
                }
            }
            else -> {}
        }
    }
}

@Composable
fun FilterChipLite(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(
                if (isSelected) Neon_Cyan.copy(alpha = 0.2f) else Surface_Elevated,
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (isSelected) Neon_Cyan else Text_Secondary.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Neon_Cyan else Text_Primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StudentExplorerCard(student: SearchStudent) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface_Elevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Initial Circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Neon_Purple.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = Neon_Purple,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = student.name, fontWeight = FontWeight.Bold, color = Text_Primary, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = student.rollno.toString(), color = Text_Secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        if (!student.branch.isNullOrEmpty()) {
                            Text("•", color = Text_Secondary, fontSize = 12.sp)
                            Text(text = "${student.branch} ${student.section ?: ""}".trim(), color = Neon_Cyan, fontSize = 12.sp)
                        }
                    }
                }
                
                // Attendance Badge
                val pct = student.attendancePercentage
                if (pct != null) {
                    val color = when {
                        pct >= 85 -> Neon_Green
                        pct >= 75 -> Neon_Cyan
                        pct >= 60 -> Neon_Orange
                        else -> Neon_Red
                    }
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("$pct%", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            
            if (expanded && student.courseAttendance != null && student.courseAttendance.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Course Attendance", fontSize = 12.sp, color = Text_Secondary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                
                student.courseAttendance.forEach { course ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(course.courseName, color = Text_Primary, fontSize = 13.sp)
                            Text("Yr: ${course.year} • Batch: ${course.batch}", color = Text_Secondary, fontSize = 11.sp)
                        }
                        
                        Text(
                            "${course.present}/${course.total}", 
                            color = Text_Secondary, 
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        val cpct = course.percentage
                        val ccolor = when {
                            cpct == null -> Text_Secondary
                            cpct >= 85 -> Neon_Green
                            cpct >= 75 -> Neon_Cyan
                            cpct >= 60 -> Neon_Orange
                            else -> Neon_Red
                        }
                        Text(
                            "${cpct ?: 0}%", 
                            color = ccolor, 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(42.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
