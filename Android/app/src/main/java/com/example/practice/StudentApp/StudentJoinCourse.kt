package com.example.practice.StudentApp

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*

@Composable
fun StudentJoinCourse(
    navController: NavController,
    modifier: Modifier = Modifier,
    studentViewModel: StudentViewModel,
    onJoinSuccess: () -> Unit = {}
) {
    var joiningCode by remember { mutableStateOf("") }
    var joiningCodeError by remember { mutableStateOf("") }
    val studentJoiningCodeData by studentViewModel.studentJoiningCodeData.observeAsState()
    val context = LocalContext.current

    // Clean up joining state
    DisposableEffect(Unit) {
        studentViewModel.resetJoiningCodeState()
        onDispose { studentViewModel.resetJoiningCodeState() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                studentViewModel.resetJoiningCodeState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(studentJoiningCodeData) {
        when (val state = studentJoiningCodeData) {
            is StudentViewModel.StudentJoiningCodeState.Success -> {
                Toast.makeText(context, state.data, Toast.LENGTH_SHORT).show()
                onJoinSuccess()
                navController.navigate("studentHome") {
                    popUpTo("studentHome") { inclusive = true }
                }
                joiningCode = ""
                studentViewModel.resetJoiningCodeState()
            }
            is StudentViewModel.StudentJoiningCodeState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    AnimatedGradientBackground(modifier = modifier) {
        FloatingParticles(color = PrimaryIndigo.copy(alpha = 0.1f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(64.dp)
                )
            }

            Text(
                text = "Join New Course",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            
            Text(
                text = "Enter the unique code provided by your professor",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = joiningCode,
                        onValueChange = {
                            joiningCode = it
                            joiningCodeError = if (it.isBlank()) "Code cannot be empty" else ""
                        },
                        label = { Text("Joining Code", color = TextSecondaryDark) },
                        leadingIcon = { 
                            Icon(Icons.Default.Code, contentDescription = null, tint = PrimaryIndigo) 
                        },
                        isError = joiningCodeError.isNotEmpty(),
                        supportingText = {
                            if (joiningCodeError.isNotEmpty()) {
                                Text(text = joiningCodeError, color = ErrorCoral)
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = GlassBorder,
                            cursorColor = PrimaryIndigo
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    GradientButton(
                        text = "Join Course",
                        isLoading = studentJoiningCodeData is StudentViewModel.StudentJoiningCodeState.Loading,
                        enabled = joiningCode.isNotBlank() && studentJoiningCodeData !is StudentViewModel.StudentJoiningCodeState.Loading,
                        onClick = {
                            if (joiningCode.isBlank()) {
                                joiningCodeError = "Code cannot be empty"
                            } else {
                                studentViewModel.studentJoinCourse(joiningCode)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}