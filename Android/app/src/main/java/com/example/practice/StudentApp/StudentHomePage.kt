package com.example.practice.StudentApp

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.practice.auth.AuthState
import com.example.practice.auth.AuthViewModel
import com.example.practice.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    studentViewModel: StudentViewModel,
    biometricViewModel: BiometricViewModel,
    activity: AppCompatActivity
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val authState by authViewModel.authState.observeAsState()
    val authenticated by biometricViewModel.authenticated.collectAsState()
    var biometricPromptManager by remember { mutableStateOf<BiometricPromptManager?>(null) }
    val biometricState by biometricViewModel.biometricState.collectAsState()
    val context = LocalContext.current

    // Launch biometric authentication once
    LaunchedEffect(Unit) {
        biometricPromptManager = BiometricPromptManager(activity)
        showBiometricPrompt(biometricPromptManager)
    }

    // Listen for lifecycle changes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                biometricViewModel.setAuthenticated(false)
                showBiometricPrompt(biometricPromptManager)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(biometricPromptManager) {
        biometricPromptManager?.promptResults?.collect { result ->
            when (result) {
                is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                    biometricViewModel.setAuthenticated(true)
                    biometricViewModel.setBiometricState(BiometricState.Success)
                }
                is BiometricPromptManager.BiometricResult.AuthenticationFailed -> {
                    biometricViewModel.setAuthenticated(false)
                    biometricViewModel.setBiometricState(BiometricState.Failed)
                }
                is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                    biometricViewModel.setAuthenticated(false)
                    biometricViewModel.setBiometricState(BiometricState.Error(result.error))
                }
                is BiometricPromptManager.BiometricResult.HardwareUnavailable -> {
                    biometricViewModel.setAuthenticated(false)
                    biometricViewModel.setBiometricState(BiometricState.HardwareUnavailable)
                }
                is BiometricPromptManager.BiometricResult.FeatureUnavailable -> {
                    biometricViewModel.setAuthenticated(false)
                    biometricViewModel.setBiometricState(BiometricState.NoHardware)
                }
                is BiometricPromptManager.BiometricResult.AuthenticationNotSet -> {
                    biometricViewModel.setAuthenticated(false)
                    biometricViewModel.setBiometricState(BiometricState.NotEnrolled)
                }
            }
        }
    }

    // Handle biometric error states
    when (biometricState) {
        is BiometricState.HardwareUnavailable -> {
            BiometricErrorDialog(
                title = "Hardware Unavailable",
                message = "Biometric hardware is currently unavailable. Please try again later.",
                icon = Icons.Default.Warning,
                onDismiss = { biometricViewModel.resetBiometricState() }
            )
            Toast.makeText(context, "Biometric hardware unavailable.", Toast.LENGTH_SHORT).show()
            biometricViewModel.resetBiometricState()
        }
        is BiometricState.NoHardware -> {
            BiometricErrorDialog(
                title = "No Biometric Hardware",
                message = "This device doesn't have fingerprint or face recognition hardware.",
                icon = Icons.Default.Warning,
                onDismiss = { biometricViewModel.resetBiometricState() }
            )
        }
        is BiometricState.NotEnrolled -> {
            BiometricErrorDialog(
                title = "Biometric Not Set Up",
                message = "Please set up fingerprint recognition in your device settings first.",
                icon = Icons.Default.Fingerprint,
                onDismiss = { biometricViewModel.resetBiometricState() }
            )
        }
        is BiometricState.Error -> {
            val errorMessage = (biometricState as BiometricState.Error).message
            BiometricErrorDialog(
                title = "Authentication Error",
                message = errorMessage,
                icon = Icons.Default.Warning,
                onDismiss = { biometricViewModel.resetBiometricState() }
            )
        }
        else -> {}
    }

    var selectedIndex by remember { mutableStateOf(2) }
    val studentNavController = rememberNavController()

    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            navController.navigate("opening") {
                popUpTo("StudentHome") { inclusive = true }
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = { AppHeaderStudent(title = "Student Dashboard") { authViewModel.signOut() } },
        bottomBar = {
            if (authenticated) {
                StudentBottomNavigationBar(
                    selectedIndex = selectedIndex,
                    studentNavController = studentNavController,
                    onItemSelected = { index ->
                        selectedIndex = index
                        when (index) {
                            0 -> studentNavController.navigate("studentProfile") {
                                popUpTo("studentHome") { inclusive = false }
                            }
                            1 -> studentNavController.navigate("joinCourse") {
                                popUpTo("studentHome") { inclusive = false }
                            }
                            2 -> studentNavController.navigate("studentHome") {
                                popUpTo("studentHome") { inclusive = true }
                            }
                            3 -> studentNavController.navigate("scanAttendance") {
                                popUpTo("studentHome") { inclusive = false }
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            if (!authenticated) {
                // Biometric challenge screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Animated fingerprint icon
                        val pulseAnim = rememberInfiniteTransition(label = "pulse")
                        val scale by pulseAnim.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = EaseInOutCubic),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )

                        Box(
                            modifier = Modifier
                                .size((72 * scale).dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Neon_Cyan.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Authentication Required",
                                modifier = Modifier.size(56.dp),
                                tint = Neon_Cyan
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Authentication Required",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please authenticate to access your dashboard",
                            textAlign = TextAlign.Center,
                            color = TextSecondaryDark,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                biometricPromptManager?.showBiometricPrompt(
                                    title = "Authenticate",
                                    description = "Please authenticate to access your dashboard"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Neon_Cyan,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(48.dp)
                        ) {
                            Text("Try Again", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                StudentNavHost(
                    navController = studentNavController,
                    rootNavController = navController,
                    studentViewModel = studentViewModel,
                    onNavigateToHome = { selectedIndex = 2 }
                )
            }
        }
    }
}

@Composable
fun StudentNavHost(
    navController: NavHostController,
    rootNavController: NavController,
    studentViewModel: StudentViewModel,
    onNavigateToHome: () -> Unit
) {
    NavHost(navController = navController, startDestination = "studentHome") {
        composable("studentHome") { StudentHome(modifier = Modifier, navController) }
        composable("scanAttendance") { ScanAttendance(modifier = Modifier, navController, studentViewModel = studentViewModel) }
        composable(
            route = "studentViewAttendance/{courseId}/{courseBatch}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseBatch") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
            studentViewAttendance(navController = navController, courseId = courseId, courseBatch = courseBatch, modifier = Modifier)
        }
        composable("joinCourse") {
            StudentJoinCourse(navController = navController, modifier = Modifier, studentViewModel = studentViewModel, onJoinSuccess = onNavigateToHome)
        }
        composable("studentProfile") {
            StudentProfilePage(navController = navController, modifier = Modifier, studentViewModel = studentViewModel)
        }
        composable("changePassword") {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onRequireReauthentication = { navController.navigate("reauthenticate") }
            )
        }
        composable("reauthenticate") {
            ReauthenticationScreen(
                onNavigateBack = { navController.popBackStack() },
                onReauthenticationSuccess = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeaderStudent(title: String, onSignOut: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = DarkSurface,
            titleContentColor = TextPrimaryDark
        ),
        actions = {
            IconButton(onClick = onSignOut) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = ErrorCoral
                )
            }
        }
    )
}

@Composable
fun StudentBottomNavigationBar(
    selectedIndex: Int,
    studentNavController: NavHostController,
    onItemSelected: (Int) -> Unit
) {
    val navItemList = listOf(
        NavItem("Profile", Icons.Default.Person),
        NavItem("Join Course", Icons.Default.Add),
        NavItem("Home", Icons.Default.Home),
        NavItem("Attendance", Icons.AutoMirrored.Filled.List)
    )

    NavigationBar(
        containerColor = DarkSurface,
        contentColor = TextPrimaryDark,
        tonalElevation = 0.dp
    ) {
        navItemList.forEachIndexed { index, navItem ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = navItem.icon,
                        contentDescription = navItem.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = navItem.label,
                        fontSize = 10.sp,
                        maxLines = 1,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Neon_Cyan,
                    unselectedIconColor = TextSecondaryDark,
                    selectedTextColor = Neon_Cyan,
                    unselectedTextColor = TextSecondaryDark,
                    indicatorColor = Neon_Cyan.copy(alpha = 0.12f)
                )
            )
        }
    }
}


@Composable
fun BiometricErrorDialog(
    title: String,
    message: String,
    icon: ImageVector,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ErrorCoral.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ErrorCoral,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = TextPrimaryDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Neon_Cyan,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Understand", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun showBiometricPrompt(biometricPromptManager: BiometricPromptManager?) {
    biometricPromptManager?.showBiometricPrompt(
        title = "Authenticate",
        description = "Please authenticate to access your dashboard"
    )
}
