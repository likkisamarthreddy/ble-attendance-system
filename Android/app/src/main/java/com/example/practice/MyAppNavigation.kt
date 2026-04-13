package com.example.practice

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.practice.ProfessorApp.ProfessorHomePage
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.StudentApp.BiometricViewModel
import com.example.practice.StudentApp.StudentHomePage
import com.example.practice.StudentApp.StudentViewModel
import com.example.practice.adminApp.AdminHomePage
import com.example.practice.adminApp.AdminViewModel
import com.example.practice.auth.AdminLoginPage
import com.example.practice.auth.AuthViewModel
import com.example.practice.auth.Opening
import com.example.practice.auth.ProfessorLoginPage
import com.example.practice.auth.ProfessorSignupPage
import com.example.practice.auth.StudentLoginPage
import com.example.practice.auth.StudentSignupPage

@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel, navController: NavController, professorViewModel: ProfessorViewModel, studentViewModel: StudentViewModel, biometricViewModel: BiometricViewModel, activity: AppCompatActivity, adminViewModel: AdminViewModel){
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "opening", builder = {
        composable("StudentLogin"){
            StudentLoginPage(modifier, navController, authViewModel)
        }
        composable("StudentSignup"){
            StudentSignupPage(modifier, navController, authViewModel)
        }
        composable("StudentHome"){
            StudentHomePage(modifier, navController, authViewModel, studentViewModel, biometricViewModel,
                activity)
        }
        composable("ProfessorLogin"){
            ProfessorLoginPage(modifier, navController, authViewModel)
        }
        composable("ProfessorSignup"){
            ProfessorSignupPage(modifier, navController, authViewModel)
        }
        composable("ProfessorHome"){
            ProfessorHomePage(modifier, navController, authViewModel, professorViewModel)
        }
        composable("AdminLogin"){
            AdminLoginPage(modifier, navController, authViewModel)
        }
        composable("AdminHome"){
            AdminHomePage(modifier, navController, authViewModel, adminViewModel, professorViewModel)
        }
        composable("opening"){
            Opening(modifier, activity, navController, authViewModel)
        }


    })
}

