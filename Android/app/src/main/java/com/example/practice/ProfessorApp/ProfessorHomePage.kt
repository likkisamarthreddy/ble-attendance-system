package com.example.practice.ProfessorApp

import com.example.practice.ProfessorApp.HomePages.CourseStudentDetailScreen
import com.example.practice.ProfessorApp.HomePages.CreateCourse
import com.example.practice.ProfessorApp.HomePages.GeofenceSettingsScreen
import com.example.practice.ProfessorApp.HomePages.ModifyStudentsInCourse
import com.example.practice.ProfessorApp.TakeAttendance.TakeManualAttendance
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.practice.ProfessorApp.HomePages.ProfessorHome
import com.example.practice.ProfessorApp.HomePages.TakeAttendance
import com.example.practice.ProfessorApp.TakeAttendance.BroadcastAttendance
import com.example.practice.ProfessorApp.ViewAttendance.ModifyStudentAttendance
import com.example.practice.ProfessorApp.ViewAttendance.ViewCourseAttendance
import com.example.practice.ProfessorApp.ViewAttendance.ViewRecordAttendance
import com.example.practice.StudentApp.ChangePasswordScreen
import com.example.practice.StudentApp.ReauthenticationScreen
import com.example.practice.auth.AuthState
import com.example.practice.auth.AuthViewModel
import com.example.practice.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorHomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    professorViewModel: ProfessorViewModel
) {
    val authState by authViewModel.authState.observeAsState()
    val courseTitle by professorViewModel.courseTitle.observeAsState()
    val batchName by professorViewModel.batchName.observeAsState()
    val joiningCode by professorViewModel.joiningCode.observeAsState()
    val courseExpiry by professorViewModel.courseExpiry.observeAsState()
    val isArchivedSelected by professorViewModel.isArchivedSelected.observeAsState()
    var selectedIndex by remember { mutableIntStateOf(1) }

    val professorNavController = rememberNavController()

    val encodedCourseName = java.net.URLEncoder.encode(courseTitle ?: "", "UTF-8")
    val encodedBatch = java.net.URLEncoder.encode(batchName ?: "", "UTF-8")
    val encodedJoiningCode = java.net.URLEncoder.encode(joiningCode ?: "", "UTF-8")
    val encodedCourseExpiry = java.net.URLEncoder.encode(courseExpiry ?: "", "UTF-8")

    val navItems = mutableListOf(
        NavigationItem("Profile", Icons.Default.Person, "professorProfile"),
        NavigationItem("Home", Icons.Filled.Home, "professorHome"),
        NavigationItem("Students", Icons.AutoMirrored.Filled.List, "courseStudentDetails/$encodedCourseName/$encodedBatch/$encodedJoiningCode"),
        NavigationItem("View Attendance", Icons.Filled.Check, "viewCourseAttendance/$encodedCourseName/$encodedBatch/$joiningCode")
    )

    if (isArchivedSelected == false) {
        navItems.add(NavigationItem("Take Attendance", Icons.Filled.Person, "takeAttendance/$encodedCourseName/$encodedBatch/$joiningCode/$encodedCourseExpiry"))
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            navController.navigate("opening") {
                popUpTo("ProfessorHome") { inclusive = true }
            }
        }
    }

    Scaffold(
        containerColor = Background_Deep,
        bottomBar = {
            ProfessorBottomNavigationBar(
                selectedIndex = selectedIndex,
                navItems = navItems,
                onItemSelected = { index ->
                    selectedIndex = index
                    professorNavController.navigate(navItems[index].route) {
                        popUpTo(navItems[index].route) { inclusive = false }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ProfessorNavHost(
                navController = professorNavController,
                rootNavController = navController,
                professorViewModel = professorViewModel,
                authViewModel = authViewModel
            )
        }
    }
}

@Composable
fun ProfessorNavHost(
    navController: NavHostController,
    rootNavController: NavController,
    professorViewModel: ProfessorViewModel,
    authViewModel: AuthViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "professorHome"
    ) {
        composable("professorHome") {
            ProfessorHome(
                navController = navController, 
                professorViewModel = professorViewModel
            )
        }

        composable(
            route = "viewCourseAttendance/{courseId}/{courseBatch}/{joiningCode}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseBatch") { type = NavType.StringType },
                navArgument("joiningCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
            val joiningCode = backStackEntry.arguments?.getString("joiningCode") ?: ""
            ViewCourseAttendance(
                navController = navController, 
                courseId = courseId,
                courseBatch = courseBatch,
                joiningCode = joiningCode,
                modifier = Modifier,
                professorViewModel = professorViewModel
            )
        }

        composable(
            route = "viewRecordAttendance/{recordId}/{recordDate}/{courseId}/{courseBatch}",
            arguments = listOf(
                navArgument("recordId") { type = NavType.StringType },
                navArgument("recordDate") { type = NavType.StringType },
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseBatch") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getString("recordId") ?: ""
            val recordDate = backStackEntry.arguments?.getString("recordDate") ?: ""
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""

            ViewRecordAttendance(
                navController = navController, 
                recordId = recordId,
                recordDate = recordDate,
                courseId = courseId,
                courseBatch = courseBatch,
                modifier = Modifier,
                professorViewModel = professorViewModel
            )
        }

        composable(route = "takeAttendance/{courseId}/{courseBatch}/{joiningCode}/{courseExpiry}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseBatch") { type = NavType.StringType },
                navArgument("joiningCode") { type = NavType.StringType },
                navArgument("courseExpiry") {type = NavType.StringType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
            val joiningCode = backStackEntry.arguments?.getString("joiningCode") ?: ""
            val courseEnpiry = backStackEntry.arguments?.getString("courseExpiry") ?: ""
            TakeAttendance(
                modifier = Modifier,
                navController = navController, 
                professorViewModel = professorViewModel,
                courseExpiry = courseEnpiry,
                joiningCode = joiningCode,
                courseTitle = courseId,
                batchName = courseBatch
            )
        }

        composable(
            route = "courseStudentDetails/{courseId}/{courseBatch}/{joiningCode}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseBatch") { type = NavType.StringType },
                navArgument("joiningCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
            val joiningCode = backStackEntry.arguments?.getString("joiningCode") ?: ""
            CourseStudentDetailScreen(
                navController = navController, 
                courseId = courseId,
                courseBatch = courseBatch,
                modifier = Modifier,
                joiningCode = joiningCode,
                professorViewModel = professorViewModel
            )
        }

        composable(
            route = "modifyStudentsInCourse/{courseId}/{courseBatch}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseBatch") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
            ModifyStudentsInCourse(
                navController = navController, 
                courseId = courseId,
                courseBatch = courseBatch,
                modifier = Modifier
            )
        }

        composable(
            route = "geofenceSettings/{joiningCode}/{courseName}",
            arguments = listOf(
                navArgument("joiningCode") { type = NavType.StringType },
                navArgument("courseName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val joiningCode = backStackEntry.arguments?.getString("joiningCode") ?: ""
            val courseName = backStackEntry.arguments?.getString("courseName") ?: ""
            GeofenceSettingsScreen(
                navController = navController,
                joiningCode = joiningCode,
                courseName = courseName,
                modifier = Modifier
            )
        }

        composable("createCourse") {
            CreateCourse(
                navController = navController,
                modifier = Modifier,
                professorViewModel = professorViewModel
            )
        }

        composable("professorProfile"){
            ProfessorProfilePage(
                navController = navController,
                modifier = Modifier,
                professorViewModel = professorViewModel,
                authViewModel = authViewModel
            )
        }

        composable( route = "takeManualAttendance/{courseId}/{courseBatch}/{attendanceId}/{joiningCode}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseBatch") { type = NavType.StringType },
                navArgument("attendanceId") { type = NavType.StringType },
                navArgument("joiningCode") { type = NavType.StringType }
            )
        ) {
            backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
            val attendanceId = backStackEntry.arguments?.getString("attendanceId") ?: ""
            val joiningCode = backStackEntry.arguments?.getString("joiningCode") ?: ""
            TakeManualAttendance(
                navController = navController, 
                courseId = courseId,
                courseBatch = courseBatch,
                attendanceId = attendanceId,
                joiningCode = joiningCode,
                modifier = Modifier,
                professorViewModel = professorViewModel
            )
        }

        composable( route = "broadcastAttendance/{courseId}/{courseBatch}/{attendanceId}/{sessionSecret}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseBatch") { type = NavType.StringType },
                navArgument("attendanceId") { type = NavType.StringType },
                navArgument("sessionSecret") { type = NavType.StringType }
            )
        ) {
                backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
            val attendanceId = backStackEntry.arguments?.getString("attendanceId") ?: ""
            val sessionSecret = backStackEntry.arguments?.getString("sessionSecret") ?: ""
            BroadcastAttendance(
                modifier = Modifier,
                navController = navController,
                courseId = courseId,
                courseBatch = courseBatch,
                attendanceId = attendanceId,
                sessionSecret = sessionSecret,
                professorViewModel = professorViewModel
            )
        }

        composable ( route = "modifyAttendance/{courseId}/{courseBatch}/{recordDate}/{id}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseBatch") {type = NavType.StringType },
                navArgument("recordDate") { type = NavType.StringType },
                navArgument("id") {type = NavType.StringType}
            )
        ) {
                backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
            val recordDate = backStackEntry.arguments?.getString("recordDate") ?: ""
            ModifyStudentAttendance(
                modifier = Modifier,
                professorViewModel = professorViewModel,
                navController = navController,
                courseId = courseId,
                courseBatch = courseBatch,
                recordDate = recordDate,
                id = id
            )
        }

        composable("changePassword") {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onRequireReauthentication = {
                    navController.navigate("reauthenticate")
                }
            )
        }

        composable("reauthenticate") {
            ReauthenticationScreen(
                onNavigateBack = { navController.popBackStack() },
                onReauthenticationSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun AppHeader(title: String, onSignOut: () -> Unit) {
    // Deprecated for premium look - we use headers inside screens now
}

@Composable
fun ProfessorBottomNavigationBar(
    selectedIndex: Int,
    navItems: List<NavigationItem>, 
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Surface_Elevated,
        contentColor = Text_Secondary,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        navItems.forEachIndexed { index, navItem ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = { 
                    Icon(
                        imageVector = navItem.icon, 
                        contentDescription = navItem.label,
                        tint = if (selectedIndex == index) Neon_Cyan else Text_Secondary
                    ) 
                },
                label = { 
                    Text(
                        text = navItem.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedIndex == index) Neon_Cyan else Text_Secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Neon_Cyan,
                    unselectedIconColor = Text_Secondary,
                    selectedTextColor = Neon_Cyan,
                    unselectedTextColor = Text_Secondary,
                    indicatorColor = Neon_Cyan.copy(alpha = 0.15f)
                )
            )
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)
