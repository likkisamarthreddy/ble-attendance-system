package com.example.practice.adminApp

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.HomePages.formatExpiryDate
import com.example.practice.ResponsesModel.Course
import com.example.practice.adminApp.AdminViewModel.AdminViewCurrrentCourseState
import com.example.practice.ui.theme.*

@Composable
fun AdminHome(
    modifier: Modifier = Modifier,
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    val adminData by adminViewModel.adminViewCurrentData.observeAsState()
    val dashboardStats by adminViewModel.dashboardStats.observeAsState()
    val isArchivedSelected by adminViewModel.isArchivedSelected.observeAsState()

    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        isArchivedSelected?.let { adminViewModel.fetchAllActiveCourses(it) }
        adminViewModel.fetchDashboardStats()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Deep)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Overview",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Text_Primary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Dashboard Stats ──
        when (val statsState = dashboardStats) {
            is AdminViewModel.DashboardStatsState.Loading -> {
                CircularProgressIndicator(color = Neon_Cyan, modifier = Modifier.size(24.dp))
            }
            is AdminViewModel.DashboardStatsState.Success -> {
                val stats = statsState.data
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardStatCard(
                            title = "Students",
                            value = "${stats.totalStudents}",
                            icon = Icons.Default.People,
                            color = Neon_Cyan,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            title = "Professors",
                            value = "${stats.totalProfessors}",
                            icon = Icons.Default.School,
                            color = Neon_Purple,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            title = "Courses",
                            value = "${stats.totalCourses}",
                            icon = Icons.Default.DateRange,
                            color = Neon_Blue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardStatCard(
                            title = "Overall Att.",
                            value = "${stats.overallAttendancePercentage}%",
                            icon = Icons.Default.Percent,
                            color = Neon_Green,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            title = "Critical (<75%)",
                            value = "${stats.criticalCount + stats.warningCount}",
                            icon = Icons.Default.Warning,
                            color = Neon_Red,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            is AdminViewModel.DashboardStatsState.Error -> {
                Text(
                    text = "Failed to load stats: ${statsState.message}",
                    color = Neon_Red,
                    fontSize = 12.sp
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArchivedSelected == true) "Archived Courses" else "Active Courses",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Text_Primary
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isArchivedSelected == false,
                    onClick = { if (isArchivedSelected == true) adminViewModel.ArchiveSection() },
                    label = { Text("Active") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Neon_Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = Neon_Cyan,
                        containerColor = Surface_Elevated,
                        labelColor = Text_Primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Text_Secondary.copy(alpha = 0.3f),
                        selectedBorderColor = Neon_Cyan,
                        enabled = true,
                        selected = isArchivedSelected == false
                    )
                )
                FilterChip(
                    selected = isArchivedSelected == true,
                    onClick = { if (isArchivedSelected == false) adminViewModel.ArchiveSection() },
                    label = { Text("Archived") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Neon_Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = Neon_Cyan,
                        containerColor = Surface_Elevated,
                        labelColor = Text_Primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Text_Secondary.copy(alpha = 0.3f),
                        selectedBorderColor = Neon_Cyan,
                        enabled = true,
                        selected = isArchivedSelected == true
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            placeholder = {
                Text(
                    text = "Search by name/batch",
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
            colors = OutlinedTextFieldDefaults.colors(
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

        Spacer(modifier = Modifier.height(16.dp))

        // Courses List
        when(val state = adminData){
            is AdminViewCurrrentCourseState.Loading -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Neon_Cyan)
                }
            }
            is AdminViewCurrrentCourseState.Success -> {
                val courses = state.data

                if (courses.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No active courses available.",
                            fontSize = 16.sp,
                            color = Text_Secondary
                        )
                    }
                } else {
                    val filteredCourses = if (searchQuery.isEmpty()) {
                        courses
                    } else {
                        courses.filter { course ->
                            course.name.contains(searchQuery, ignoreCase = true) ||
                            course.batch.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredCourses.isEmpty() && searchQuery.isNotEmpty()) {
                         Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No results found for '$searchQuery'",
                                fontSize = 16.sp,
                                color = Text_Secondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredCourses) { course ->
                                AdminCourseCard(course = course, adminViewModel = adminViewModel)
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
            is AdminViewCurrrentCourseState.Error -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        color = Neon_Red,
                        fontSize = 14.sp
                    )
                }
            }
            else -> {
                // Initial State
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Surface_Elevated),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Text_Secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                color = Text_Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AdminCourseCard(course: Course, adminViewModel: AdminViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                adminViewModel.selectCourse(
                    course.name,
                    course.batch,
                    course.courseExpiry,
                    course.year,
                    course.joiningCode
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface_Elevated),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Neon_Cyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Course Icon",
                    tint = Neon_Cyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Text_Primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Batch: ${course.batch}",
                        fontSize = 14.sp,
                        color = Text_Secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "•",
                        fontSize = 14.sp,
                        color = Text_Secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Year ${course.year}",
                        fontSize = 14.sp,
                        color = Text_Secondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                     text = "Expires: ${formatExpiryDate(course.courseExpiry)}",
                     fontSize = 12.sp,
                     color = Text_Secondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}