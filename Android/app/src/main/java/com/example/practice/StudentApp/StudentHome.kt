package com.example.practice.StudentApp

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.sin
import androidx.navigation.NavController
import com.example.practice.ResponsesModel.CourseWithAttendance
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*

@Composable
fun StudentHome(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val studentViewModel: StudentViewModel = viewModel()
    val attendanceState by studentViewModel.coursesWithAttendance.observeAsState()

    LaunchedEffect(Unit) {
        studentViewModel.fetchCoursesWithAttendance()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Deep)
    ) {
        // Animated wave background
        StudentPulseBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(GradientCyan)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "My Courses",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "View attendance records",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = attendanceState) {
                is StudentViewModel.CoursesAttendanceState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Neon_Cyan,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading courses...", color = TextSecondaryDark, fontSize = 14.sp)
                        }
                    }
                }

                is StudentViewModel.CoursesAttendanceState.Success -> {
                    val courses = state.data.courses

                    if (courses.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(courses) { course ->
                                CourseCardWithAttendance(
                                    course = course,
                                    navController = navController
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = TextSecondaryDark.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No courses enrolled yet",
                                    color = TextSecondaryDark,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Join a course to get started",
                                    color = TextSecondaryDark.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                is StudentViewModel.CoursesAttendanceState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassmorphismCard(modifier = Modifier.fillMaxWidth(0.9f)) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = ErrorCoral,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = state.message,
                                    color = ErrorCoral,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { studentViewModel.fetchCoursesWithAttendance() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Neon_Cyan,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Neon_Cyan,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading courses...", color = TextSecondaryDark, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CourseCardWithAttendance(course: CourseWithAttendance, navController: NavController) {
    val cardColors = listOf(
        listOf(Neon_Cyan, Color(0xFF38BDF8)),
        listOf(Color(0xFF6366F1), Neon_Cyan),
        listOf(Neon_Green, Neon_Cyan)
    )
    val colorPair = cardColors[course.name.hashCode().mod(cardColors.size).let { if (it < 0) it + cardColors.size else it }]

    // Color based on attendance percentage
    val percentageColor = when {
        course.percentage >= 75 -> Color(0xFF4CAF50)   // Green
        course.percentage >= 50 -> Color(0xFFFF9800)   // Orange
        else -> Color(0xFFF44336)                       // Red
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val encodedCourseName = com.example.practice.utils.EncoderHelper.safeEncode(course.name)
                val encodedBatch = com.example.practice.utils.EncoderHelper.safeEncode(course.batch)
                navController.navigate("studentViewAttendance/$encodedCourseName/$encodedBatch")
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Course icon with gradient
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(colorPair)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = course.name.take(2).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Course info + attendance stats
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Batch: ${course.batch}",
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )
                if (course.totalClasses > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✔ ${course.presentCount}",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "✘ ${course.absentCount}",
                            fontSize = 12.sp,
                            color = Color(0xFFF44336),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Total: ${course.totalClasses}",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Circular attendance percentage
            if (course.totalClasses > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(50.dp)
                ) {
                    Canvas(modifier = Modifier.size(50.dp)) {
                        // Background arc
                        drawArc(
                            color = percentageColor.copy(alpha = 0.15f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                            size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
                        )
                        // Progress arc
                        drawArc(
                            color = percentageColor,
                            startAngle = -90f,
                            sweepAngle = 360f * course.percentage / 100f,
                            useCenter = false,
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                            size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
                        )
                    }
                    Text(
                        text = "${course.percentage}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = percentageColor
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StudentPulseBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "studentPulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.04f)) {
        val width = size.width
        val height = size.height
        val centerY = height * 0.25f

        val path = Path()
        val amplitude = 40f
        val frequency = 2.5f

        for (x in 0..width.toInt() step 5) {
            val normalizedX = x / width
            val y = centerY + sin(normalizedX * frequency * 2 * Math.PI + phase).toFloat() * amplitude
            if (x == 0) path.moveTo(x.toFloat(), y) else path.lineTo(x.toFloat(), y)
        }
        drawPath(path, color = Neon_Cyan, style = Stroke(width = 2f))

        val path2 = Path()
        for (x in 0..width.toInt() step 5) {
            val normalizedX = x / width
            val y = centerY + 200f + sin(normalizedX * 2f * 2 * Math.PI - phase).toFloat() * (amplitude * 1.3f)
            if (x == 0) path2.moveTo(x.toFloat(), y) else path2.lineTo(x.toFloat(), y)
        }
        drawPath(path2, color = Neon_Green, style = Stroke(width = 1.5f))
    }
}