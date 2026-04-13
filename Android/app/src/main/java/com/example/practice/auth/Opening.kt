package com.example.practice.auth

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.practice.R
import com.example.practice.api.NetworkResponse
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*

private const val REQUEST_CODE_PERMISSIONS = 1001

@Composable
fun Opening(modifier: Modifier = Modifier, activity: Activity, navController: NavController, authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.observeAsState()
    val roleState by authViewModel.roleResult.observeAsState()
    val simBindState by authViewModel.simBindResult.observeAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        authViewModel.checkAuthStatus(context)
        checkAndRequestPermissions(activity)
    }

    LaunchedEffect(simBindState) {
        when (val sim = simBindState) {
            is NetworkResponse.Success -> navController.navigate("StudentHome")
            is NetworkResponse.Error -> {
                Toast.makeText(context, sim.message, Toast.LENGTH_SHORT).show()
                authViewModel.signOut()
            }
            NetworkResponse.Loading -> {}
            else -> Unit
        }
    }

    LaunchedEffect(roleState) {
        when (val role = roleState) {
            is NetworkResponse.Success -> {
                val userRole = role.data.role
                val destination = when (userRole) {
                    "student" -> "StudentHome"
                    "admin" -> "AdminHome"
                    else -> "ProfessorHome"
                }
                if (destination == "StudentHome") {
                    authViewModel.verifySubscriptionId(context)
                } else {
                    navController.navigate(destination) {
                        popUpTo("opening") { inclusive = true }
                    }
                }
            }
            is NetworkResponse.Error -> {
                Toast.makeText(context, role.message, Toast.LENGTH_SHORT).show()
            }
            NetworkResponse.Loading -> {}
            else -> Unit
        }
    }

    // ─── Animated Background ───
    AnimatedGradientBackground(modifier = modifier) {
        FloatingParticles(color = PrimaryIndigo.copy(alpha = 0.1f))

        if (authState is AuthState.Loading || roleState is NetworkResponse.Loading) {
            // Loading state
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = PrimaryIndigo,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Checking authentication...",
                        color = TextSecondaryDark,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (authState is AuthState.Error) {
            val errorMessage = (authState as AuthState.Error).message
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }

        if (authState is AuthState.Unauthenticated) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // Logo with glow
                Box(contentAlignment = Alignment.Center) {
                    // Glow ring
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.1f))
                    )
                    Image(
                        painter = painterResource(id = R.drawable.indian_institute_of_information_technology__guwahati_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(110.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "IIITG Attendance",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Secure BLE-based attendance system",
                    fontSize = 14.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.weight(1f))

                // ─── Role Selection Cards ───
                Text(
                    text = "Sign in as",
                    color = TextSecondaryDark,
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                RoleCard(
                    icon = Icons.Filled.Face,
                    title = "Student",
                    subtitle = "Mark attendance via BLE scan",
                    accentColor = PrimaryIndigo,
                    onClick = { navController.navigate("StudentLogin") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                RoleCard(
                    icon = Icons.Filled.Person,
                    title = "Professor",
                    subtitle = "Broadcast attendance session",
                    accentColor = SecondaryPurple,
                    onClick = { navController.navigate("ProfessorLogin") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                RoleCard(
                    icon = Icons.Outlined.Settings,
                    title = "Admin",
                    subtitle = "Manage courses and students",
                    accentColor = TertiaryCyan,
                    onClick = { navController.navigate("AdminLogin") }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    GlassmorphismCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
            }

            // Arrow indicator
            Text(
                text = "→",
                color = accentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun checkAndRequestPermissions(activity: Activity) {
    val context: Context = activity
    val missingPermissions = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_PHONE_STATE)
        }
    } else {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.BLUETOOTH)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_PHONE_STATE)
        }
    }

    // Add camera and location for all versions (face verify + geofence)
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        missingPermissions.add(Manifest.permission.CAMERA)
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (missingPermissions.isNotEmpty()) {
        ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
    }
}
