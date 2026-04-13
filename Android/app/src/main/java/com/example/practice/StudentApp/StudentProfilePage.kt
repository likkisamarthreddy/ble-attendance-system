package com.example.practice.StudentApp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ResponsesModel.Course
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun StudentProfilePage(
    navController: NavController,
    modifier: Modifier = Modifier,
    studentViewModel: StudentViewModel
) {
    val studentProfileDetails by studentViewModel.studentProfileDetails.observeAsState()

    LaunchedEffect(Unit) {
        studentViewModel.fetchStudentProfileDetails()
    }

    AnimatedGradientBackground(modifier = modifier) {
        when (val state = studentProfileDetails) {
            is StudentViewModel.StudentProfileDetailsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            }
            is StudentViewModel.StudentProfileDetailsState.Success -> {
                val data = state.data
                StudentProfileContent(
                    name = data.name,
                    email = data.email,
                    rollno = data.rollno,
                    courses = data.courses ?: emptyList(),
                    onChangePasswordClick = { navController.navigate("changePassword") }
                )
            }
            is StudentViewModel.StudentProfileDetailsState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassmorphismCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error loading profile", color = ErrorCoral, fontWeight = FontWeight.Bold)
                            Text(state.message, color = TextSecondaryDark, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { studentViewModel.fetchStudentProfileDetails() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) { Text("Retry") }
                        }
                    }
                }
            }
            null -> {}
        }
    }
}

@Composable
fun StudentProfileContent(
    name: String,
    email: String,
    rollno: Int,
    courses: List<Course>,
    onChangePasswordClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Profile Info Card
        GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(PrimaryIndigo.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Text(text = "Roll No: $rollno", fontSize = 14.sp, color = TextSecondaryDark)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = GlassBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileDetailRow(Icons.Default.Email, "Email", email)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Courses Section
        Text(
            text = "Enrolled Courses",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (courses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DarkSurface.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No courses enrolled", color = TextSecondaryDark)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(courses) { course ->
                    CourseItem(course = course)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GradientButton(
            text = "Change Password",
            onClick = onChangePasswordClick,
            modifier = Modifier.fillMaxWidth(),
            gradientColors = GradientSecondary // Use purple for secondary action
        )
    }
}

@Composable
fun ProfileDetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = TextSecondaryDark)
            Text(text = value, fontSize = 14.sp, color = TextPrimaryDark)
        }
    }
}

@Composable
fun CourseItem(course: Course) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = course.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                StatusChip(text = course.batch, color = TertiaryCyan)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Code: ${course.joiningCode}",
                    fontSize = 12.sp,
                    color = PrimaryIndigo,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Expires: ${formatDateString(course.courseExpiry)}",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

private fun formatDateString(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        dateString
    }
}