package com.example.practice.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.R
import com.example.practice.api.NetworkResponse
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun StudentLoginPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val simBindState by authViewModel.simBindResult.observeAsState()
    val focusManager = LocalFocusManager.current

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    LaunchedEffect(simBindState) {
        when (val sim = simBindState) {
            is NetworkResponse.Success -> {
                navController.navigate("StudentHome") {
                    popUpTo("StudentLogin") { inclusive = true }
                }
            }
            is NetworkResponse.Error -> {
                Toast.makeText(context, sim.message, Toast.LENGTH_SHORT).show()
                authViewModel.signOut()
            }
            NetworkResponse.Loading -> {}
            else -> Unit
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                isLoading = false
                authViewModel.verifySubscriptionId(context)
            }
            is AuthState.Error -> {
                isLoading = false
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
            }
            is AuthState.Loading -> { isLoading = true }
            is AuthState.WrongRole -> {
                isLoading = false
                val wrongRoleState = authState as AuthState.WrongRole
                Toast.makeText(context, wrongRoleState.message, Toast.LENGTH_LONG).show()
                delay(1000)
                Toast.makeText(context, "Please use the Professor Login page instead.", Toast.LENGTH_LONG).show()
                delay(2000)
                authViewModel.resetAfterError()
            }
            else -> { isLoading = false }
        }
    }

    // ─── Premium UI ───
    AnimatedGradientBackground(modifier = modifier) {
        FloatingParticles(color = PrimaryIndigo.copy(alpha = 0.06f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo with glow
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .alpha(0.15f)
                )
                Image(
                    painter = painterResource(id = R.drawable.indian_institute_of_information_technology__guwahati_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(90.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Student Login",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Text(
                text = "Sign in to mark your attendance",
                color = TextSecondaryDark,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email field
            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text("Email", color = TextSecondaryDark) },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email", tint = PrimaryIndigo) },
                    singleLine = true,
                    isError = email.isNotEmpty() && !email.contains("@"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = GlassBorder,
                        cursorColor = PrimaryIndigo
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Password field
            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = TextSecondaryDark) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    isError = password.isNotEmpty() && password.length < 6,
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password", tint = PrimaryIndigo) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                painter = painterResource(
                                    id = if (isPasswordVisible) R.drawable.visibility_on else R.drawable.baseline_visibility_off_24
                                ),
                                contentDescription = if (isPasswordVisible) "Hide" else "Show",
                                tint = TextSecondaryDark
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = GlassBorder,
                        cursorColor = PrimaryIndigo
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!isNetworkAvailable(context)) {
                Text(
                    text = "⚠️ No internet connection",
                    color = ErrorCoral,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login button
            GradientButton(
                text = "Login",
                isLoading = isLoading,
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && email.contains("@"),
                onClick = {
                    if (isNetworkAvailable(context)) {
                        authViewModel.loginStudent(email, password)
                    } else {
                        Toast.makeText(context, "No internet connection.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Switch role
            TextButton(onClick = { navController.navigate("ProfessorLogin") }) {
                Text(
                    text = "Login as Professor",
                    color = TertiaryCyan,
                    fontSize = 14.sp
                )
            }
        }
    }
}