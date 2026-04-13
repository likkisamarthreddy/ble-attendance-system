package com.example.practice.ProfessorApp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ProfessorApp.ProfessorViewModel.ProfessorProfileDeatilsState
import com.example.practice.ResponsesModel.Course
import com.example.practice.StudentApp.ProfileDetailRow
import com.example.practice.StudentApp.CourseItem
import com.example.practice.auth.AuthViewModel
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*

@Composable
fun ProfessorProfilePage(
    navController: NavController,
    modifier: Modifier,
    professorViewModel: ProfessorViewModel,
    authViewModel: AuthViewModel
){
    val professorProfileDetails by professorViewModel.professorProfileDetails.observeAsState()

    LaunchedEffect(Unit) {
        professorViewModel.fetchProfessorProfileDetails()
    }

    AnimatedGradientBackground(modifier = modifier) {
        when(val state = professorProfileDetails){
            is ProfessorProfileDeatilsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            }
            is ProfessorProfileDeatilsState.Success -> {
                val data = state.data
                val courses: List<Course> = data.courses ?: emptyList()
                ProfessorProfileContent(
                    name = data.name,
                    email = data.email,
                    courses = courses,
                    onChangePasswordClick = { navController.navigate("changePassword") },
                    onLogoutClick = { authViewModel.signOut() }
                )
            }
            is ProfessorProfileDeatilsState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassmorphismCard {
                        Text("Error loading profile", color = ErrorCoral)
                        Button(onClick = { professorViewModel.fetchProfessorProfileDetails() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            null -> {}
        }
    }
}

@Composable
fun ProfessorProfileContent(
    name: String,
    email: String,
    courses: List<Course>,
    onChangePasswordClick: () -> Unit,
    onLogoutClick: () -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ){
        // Profile Info
        GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(SecondaryPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryPurple
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Text(text = "Professor", fontSize = 14.sp, color = TextSecondaryDark)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = GlassBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileDetailRow(Icons.Default.Email, "Email", email)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Courses Header
        Text(
            text = "My Courses (${courses.size})",
            style = MaterialTheme.typography.headlineSmall,
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
                Text(
                    text = "No courses created yet",
                    color = TextSecondaryDark
                )
            }
        }
        else {
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
            gradientColors = GradientSecondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        GradientButton(
            text = "Logout",
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth(),
            gradientColors = listOf(ErrorCoral, ErrorCoral.copy(alpha = 0.7f))
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}