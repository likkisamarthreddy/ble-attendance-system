package com.example.practice.ProfessorApp.HomePages

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.ProfessorApp.ProfessorViewModel.ProfessorState
import com.example.practice.ResponsesModel.Course
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.sin

@Composable
fun ProfessorHome(
    modifier: Modifier = Modifier,
    navController: NavController,
    professorViewModel: ProfessorViewModel
) {
    val professorData by professorViewModel.professorData.observeAsState()
    val isArchivedSelected by professorViewModel.isArchivedSelected.observeAsState()
    
    val professorName = "Professor"

    LaunchedEffect(Unit) {
        isArchivedSelected?.let { professorViewModel.fetchProfessorCourses(it) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Deep)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Live Pulse Background
        LivePulseBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Command Center Greeting
            Text(
                text = "SYSTEM ONLINE",
                color = Neon_Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Welcome, Dr. $professorName",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Text_Primary
                )
                
                // Add Course Button
                if (isArchivedSelected == false) {
                    IconButton(
                        onClick = { navController.navigate("createCourse") },
                        modifier = Modifier
                            .background(Surface_Elevated, RoundedCornerShape(12.dp))
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Course", tint = Neon_Cyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Archive Filter Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                FilterChip(
                    selected = isArchivedSelected == false,
                    onClick = { 
                        if (isArchivedSelected == true) {
                            professorViewModel.ArchiveSection()
                            professorViewModel.fetchProfessorCourses(false)
                            professorViewModel.resetSelectedCourse()
                        }
                    },
                    label = { Text("Active Deployments", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Neon_Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = Neon_Cyan,
                        labelColor = Text_Secondary,
                        containerColor = Surface_Elevated
                    ),
                    border = null,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                FilterChip(
                    selected = isArchivedSelected == true,
                    onClick = { 
                        if (isArchivedSelected == false) {
                            professorViewModel.ArchiveSection()
                            professorViewModel.fetchProfessorCourses(true)
                            professorViewModel.resetSelectedCourse()
                        }
                    },
                    label = { Text("Archived", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Surface_Highlight,
                        selectedLabelColor = Text_Primary,
                        labelColor = Text_Secondary,
                        containerColor = Surface_Elevated
                    ),
                    border = null,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Content
            when (val state = professorData) {
                is ProfessorState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Neon_Cyan)
                    }
                }
                is ProfessorState.Success -> {
                    val courses = state.data.courses
                    if (courses.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 80.dp) // Space for bottom nav
                        ) {
                            items(courses) { course ->
                                CommandCourseCard(course, navController, professorViewModel)
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CellTower, null, tint = Text_Secondary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No active deployments", color = Text_Secondary, fontSize = 16.sp)
                            }
                        }
                    }
                }
                is ProfessorState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Connection Lost", color = Neon_Red, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            GradientButton(text = "RETRY ALIGNMENT", onClick = { isArchivedSelected?.let { professorViewModel.fetchProfessorCourses(it) } })
                        }
                    }
                }
                null -> {}
            }
        }
    }
}

@Composable
fun CommandCourseCard(course: Course, navController: NavController, professorViewModel: ProfessorViewModel) {
    val courseTitle by professorViewModel.courseTitle.observeAsState()
    val batchName by professorViewModel.batchName.observeAsState()
    val courseExpiry by professorViewModel.courseExpiry.observeAsState()
    val courseYear by professorViewModel.courseYear.observeAsState()
    val joiningCode by professorViewModel.joiningCode.observeAsState()

    val isSelected = (course.name == courseTitle && course.batch == batchName && course.courseExpiry == courseExpiry && course.year == courseYear && course.joiningCode == joiningCode)
    val isArchived = professorViewModel.isArchivedSelected.observeAsState().value == true

    GlassmorphismCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (!isSelected) {
                    professorViewModel.selectCourse(course.name, course.batch, course.courseExpiry, course.year, course.joiningCode) 
                } else {
                    professorViewModel.resetSelectedCourse() // Toggle off
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Neon_Cyan else Text_Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(if (isSelected) Neon_Cyan else Text_Secondary))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Batch: ${course.batch}",
                            fontSize = 13.sp,
                            color = Text_Secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    if (!isArchived) {
                        Text(
                            text = "Expires: ${formatExpiryDate(course.courseExpiry)}",
                            fontSize = 12.sp,
                            color = Text_Secondary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    } else {
                        Text(
                            text = "Year: ${course.year}",
                            fontSize = 12.sp,
                            color = Text_Secondary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isSelected) Neon_Cyan.copy(alpha = 0.15f) else Surface_Highlight, 
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        tint = if (isSelected) Neon_Cyan else Text_Secondary
                    )
                }
            }
            
            // Expanded Action Area
            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Divider(color = Surface_Highlight, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!isArchived) {
                        val encodedCourseName = java.net.URLEncoder.encode(course.name, "UTF-8")
                        val encodedBatch = java.net.URLEncoder.encode(course.batch, "UTF-8")
                        val encodedJoiningCode = java.net.URLEncoder.encode(course.joiningCode, "UTF-8")
                        val encodedExpiry = java.net.URLEncoder.encode(course.courseExpiry, "UTF-8")

                        GradientButton(
                            text = "START NEW SESSION",
                            onClick = { 
                                navController.navigate("takeAttendance/$encodedCourseName/$encodedBatch/$encodedJoiningCode/$encodedExpiry")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val encodedCourseName = java.net.URLEncoder.encode(course.name, "UTF-8")
                                val encodedBatch = java.net.URLEncoder.encode(course.batch, "UTF-8")
                                val encodedJoiningCode = java.net.URLEncoder.encode(course.joiningCode, "UTF-8")
                                navController.navigate("viewCourseAttendance/$encodedCourseName/$encodedBatch/$encodedJoiningCode")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Text_Primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Surface_Highlight)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp), tint = Text_Secondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reports", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val encodedCourseName = java.net.URLEncoder.encode(course.name, "UTF-8")
                                val encodedBatch = java.net.URLEncoder.encode(course.batch, "UTF-8")
                                val encodedJoiningCode = java.net.URLEncoder.encode(course.joiningCode, "UTF-8")
                                navController.navigate("courseStudentDetails/$encodedCourseName/$encodedBatch/$encodedJoiningCode")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Text_Primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Surface_Highlight)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp), tint = Text_Secondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Students", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LivePulseBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.05f)) {
        val width = size.width
        val height = size.height
        val centerY = height * 0.3f
        
        val path = Path()
        val amplitude = 50f
        val frequency = 3f
        
        for (x in 0..width.toInt() step 5) {
            val normalizedX = x / width
            val y = centerY + sin(normalizedX * frequency * 2 * Math.PI + phase).toFloat() * amplitude
            if (x == 0) {
                path.moveTo(x.toFloat(), y)
            } else {
                path.lineTo(x.toFloat(), y)
            }
        }
        
        drawPath(
            path = path,
            color = Neon_Cyan,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        
        val path2 = Path()
        for (x in 0..width.toInt() step 5) {
            val normalizedX = x / width
            val y = centerY + 150f + sin(normalizedX * 2f * 2 * Math.PI - phase).toFloat() * (amplitude * 1.5f)
            if (x == 0) {
                path2.moveTo(x.toFloat(), y)
            } else {
                path2.lineTo(x.toFloat(), y)
            }
        }
        
        drawPath(
            path = path2,
            color = Neon_Green,
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

fun formatExpiryDate(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        zonedDateTime.format(formatter)
    } catch (e: Exception) {
        "Invalid Date"
    }
}
