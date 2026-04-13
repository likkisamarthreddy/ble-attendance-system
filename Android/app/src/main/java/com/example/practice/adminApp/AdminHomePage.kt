package com.example.practice.adminApp


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.practice.ui.theme.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.auth.AuthState
import com.example.practice.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    professorViewModel: ProfessorViewModel
) {

    val authState by authViewModel.authState.observeAsState()

    val courseTitle by adminViewModel.courseTitle.observeAsState()
    val batchName by adminViewModel.batchName.observeAsState()
    val joiningCode by adminViewModel.joiningCode.observeAsState()
    val isArchivedSelected by adminViewModel.isArchivedSelected.observeAsState()
    var selectedIndex by remember { mutableIntStateOf(0) }



    // Create a nested NavController for professor screens
    val adminNavController = rememberNavController()

    val encodedCourseName = java.net.URLEncoder.encode(courseTitle, "UTF-8")
    val encodedBatch = java.net.URLEncoder.encode(batchName, "UTF-8")
    val encodedJoiningCode = java.net.URLEncoder.encode(joiningCode, "UTF-8")

    val navItems = mutableListOf(
        NavigationItem("adminHome", Icons.Filled.Home, "adminHome"),
        NavigationItem("Students",Icons.AutoMirrored.Filled.List, "courseStudentDetails/$encodedCourseName/$encodedBatch/$encodedJoiningCode"),
        NavigationItem("view Attendance", Icons.Filled.Check, "viewCourseAttendance/$encodedCourseName/$encodedBatch/$encodedJoiningCode"),
        NavigationItem("Create Account", Icons.Filled.Person, "createAccounts")
    )


    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            navController.navigate("opening") {
                popUpTo("adminHome") { inclusive = true }
            }
        }
    }

    Scaffold(
        containerColor = Background_Deep,
        topBar = { AppHeader(title = "Admin Dashboard") { authViewModel.signOut() } },
        bottomBar = {
            AdminBottomNavigationBar(
                selectedIndex = selectedIndex,
                navItems = navItems,
                onItemSelected = { index ->
                    selectedIndex = index // Update selectedIndex
                    // Navigate regardless of whether selectedIndex changes
                    adminNavController.navigate(navItems[index].route) {
                        popUpTo(navItems[index].route) { inclusive = false }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AdminNavHost(
                navController = adminNavController,
                rootNavController = navController,
                adminViewModel = adminViewModel,
                authViewModel = authViewModel
            )
        }
    }
}

@Composable
fun AdminNavHost(
    navController: NavHostController,
    rootNavController: NavController,
    adminViewModel: AdminViewModel,
    authViewModel: AuthViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "adminHome"
    ) {
        composable("adminHome") {
            AdminHome(
                modifier = Modifier,
                navController = navController, // Use nested navController
                adminViewModel = adminViewModel
            )
        }

                composable("createAccounts") {
            CreateAccounts(
                modifier = Modifier,
                navController = navController, // Use nested navController
                adminViewModel = adminViewModel
            )
        }
        composable("createStudentAccount") {
            CreateStudentAccounts(
                modifier = Modifier,
                navController = navController, // Use nested navController
                adminViewModel = adminViewModel,
                isProfessor = false
            )
        }

        composable("createProfessorAccount") {
            CreateStudentAccounts(
                modifier = Modifier,
                navController = navController, // Use nested navController
                adminViewModel = adminViewModel,
                isProfessor = true
            )
        }

//         View Attendance For a Particular Course
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
            AdminViewCourseAttendance(
                navController = navController, // Use nested navController
                courseId = courseId,
                courseBatch = courseBatch,
                joiningCode = joiningCode,
                modifier = Modifier,
                adminViewModel = adminViewModel
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

            AdminViewRecordAttendance(
                navController = navController, // Use nested navController
                recordId = recordId,
                recordDate = recordDate,
                courseId = courseId,
                courseBatch = courseBatch,
                modifier = Modifier,
                adminViewModel = adminViewModel
            )
        }

        composable("viewAllStudents") {
            ViewAllStudents(
                modifier = Modifier,
                navController = navController,
                adminViewModel = adminViewModel
            )
        }

        composable("viewAllProfessor") {
            ViewAllProfessors(
                modifier = Modifier,
                navController = navController,
                adminViewModel = adminViewModel
            )
        }


//         Detail screen that should appear within the professor navigation
        composable(
            route = "courseStudentDetails/{courseId}/{courseBatch}/{joiningCode}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseBatch") { type = NavType.StringType },
                navArgument("joiningCode") {type = NavType.StringType}
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
            val joiningCode = backStackEntry.arguments?.getString("joiningCode") ?: ""
            AdminViewCourseStudent(
                navController = navController, // Use nested navController
                courseId = courseId,
                courseBatch = courseBatch,
                joiningCode = joiningCode,
                modifier = Modifier,
                adminViewModel = adminViewModel
            )
        }

        composable(
            route = "ViewRecordAttendance/{Name}/{RollNo}",
            arguments = listOf(
                navArgument("Name") { type = NavType.StringType },
                navArgument("RollNo") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val Name = backStackEntry.arguments?.getString("Name") ?: ""
            val RollNo = backStackEntry.arguments?.getString("RollNo") ?: ""
            AdminViewStudentAttendance(
                navController = navController, // Use nested navController
                name = Name,
                rollno = RollNo,
                modifier = Modifier,
                adminViewModel = adminViewModel
            )
        }

//        composable(
//            route = "modifyStudentsInCourse/{courseId}/{courseBatch}",
//            arguments = listOf(
//                navArgument("courseId") { type = NavType.StringType },
//                navArgument("courseBatch") { type = NavType.StringType }
//            )
//        ) { backStackEntry ->
//            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
//            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
//            ModifyStudentsInCourse(
//                navController = navController, // Use nested navController
//                courseId = courseId,
//                courseBatch = courseBatch,
//                modifier = Modifier
//            )
//        }

        // Add other professor screens here
//        composable("createCourse") {
//            CreateCourse(
//                navController = navController,
//                modifier = Modifier,
//                professorViewModel = professorViewModel
//            )
//        }

//        composable("professorProfile"){
//            ProfessorProfilePage(
//                navController = navController,
//                modifier = Modifier,
//                professorViewModel = professorViewModel
//            )
//        }

//        composable( route = "takeManualAttendance/{courseId}/{courseBatch}/{attendanceId}",
//            arguments = listOf(
//                navArgument("courseId") { type = NavType.StringType },
//                navArgument("courseBatch") { type = NavType.StringType },
//                navArgument("attendanceId") { type = NavType.StringType }
//            )
//        ) {
//                backStackEntry ->
//            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
//            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
//            val attendanceId = backStackEntry.arguments?.getString("attendanceId") ?: ""
//            TakeManualAttendance(
//                navController = navController, // Use nested navController
//                courseId = courseId,
//                courseBatch = courseBatch,
//                attendanceId = attendanceId,
//                modifier = Modifier,
//                professorViewModel = professorViewModel
//            )
//        }

//        composable( route = "broadcastAttendance/{courseId}/{courseBatch}/{attendanceId}",
//            arguments = listOf(
//                navArgument("courseId") { type = NavType.StringType },
//                navArgument("courseBatch") { type = NavType.StringType },
//                navArgument("attendanceId") { type = NavType.StringType }
//            )
//        ) {
//                backStackEntry ->
//            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
//            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
//            val attendanceId = backStackEntry.arguments?.getString("attendanceId") ?: ""
//            BroadcastAttendance(
//                modifier = Modifier,
//                navController = navController,
//                courseId = courseId,
//                courseBatch = courseBatch,
//                attendanceId = attendanceId,
//                professorViewModel = professorViewModel
//            )
//        }

//        composable ( route = "modifyAttendance/{courseId}/{courseBatch}/{recordDate}/{id}",
//            arguments = listOf(
//                navArgument("courseId") { type = NavType.StringType },
//                navArgument("courseBatch") {type = NavType.StringType },
//                navArgument("recordDate") { type = NavType.StringType },
//                navArgument("id") {type = NavType.StringType}
//            )
//        ) {
//                backStackEntry ->
//            val id = backStackEntry.arguments?.getString("id") ?: ""
//            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
//            val courseBatch = backStackEntry.arguments?.getString("courseBatch") ?: ""
//            val recordDate = backStackEntry.arguments?.getString("recordDate") ?: ""
//            ModifyStudentAttendance(
//                modifier = Modifier,
//                professorViewModel = professorViewModel,
//                navController = navController,
//                courseId = courseId,
//                courseBatch = courseBatch,
//                recordDate = recordDate,
//                id = id
//            )
//        }

//        composable("changePassword") {
//            ChangePasswordScreen(
//                onNavigateBack = { navController.popBackStack() },
//                onRequireReauthentication = {
//                    navController.navigate("reauthenticate")
//                }
//            )
//        }

//        composable("reauthenticate") {
//            ReauthenticationScreen(
//                onNavigateBack = { navController.popBackStack() },
//                onReauthenticationSuccess = {
//                    // Navigate back to the change password screen
//                    navController.popBackStack()
//                }
//            )
//        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(title: String, onSignOut: () -> Unit) {
    TopAppBar(
        title = { 
            Text(
                text = title, 
                fontSize = 20.sp, 
                color = Text_Primary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 0.5.sp
            ) 
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = Surface_Elevated.copy(alpha = 0.95f),
            titleContentColor = Text_Primary
        ),
        actions = {
            IconButton(onClick = onSignOut, modifier = Modifier.padding(end = 8.dp)) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = Neon_Red
                )
            }
        }
    )
}

@Composable
fun AdminBottomNavigationBar(
    selectedIndex: Int,
    navItems: List<NavigationItem>,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Surface_Elevated.copy(alpha = 0.95f),
        contentColor = Text_Primary,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            navItems.forEachIndexed { index, navItem ->
                val isSelected = selectedIndex == index
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onItemSelected(index) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) Neon_Cyan.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = navItem.icon,
                            contentDescription = navItem.label,
                            tint = if (isSelected) Neon_Cyan else Text_Secondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = navItem.label,
                        color = if (isSelected) Neon_Cyan else Text_Secondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


data class NavItem(val label: String, val icon: ImageVector)

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

