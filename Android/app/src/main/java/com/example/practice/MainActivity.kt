package com.example.practice

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.practice.ProfessorApp.ProfessorViewModel
import com.example.practice.StudentApp.BiometricViewModel
import com.example.practice.StudentApp.StudentViewModel
import com.example.practice.adminApp.AdminViewModel
import com.example.practice.auth.AuthViewModel
import com.example.practice.ui.theme.PracticeTheme
import com.example.practice.worker.AttendanceSyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ─── Schedule periodic offline attendance sync ───
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodicSync = PeriodicWorkRequestBuilder<AttendanceSyncWorker>(
            15, TimeUnit.MINUTES  // Minimum interval for periodic work
        ).setConstraints(syncConstraints).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "attendance_periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSync
        )

        val authViewModel : AuthViewModel by viewModels()
        val professorViewModel : ProfessorViewModel by viewModels()
        val studentViewModel : StudentViewModel by viewModels()
        val biometricViewModel: BiometricViewModel by viewModels()
        val adminViewModel: AdminViewModel by viewModels()
        authViewModel.setContext(applicationContext)


        setContent {
            val navController = rememberNavController()
            PracticeTheme {
                Scaffold { innerPadding ->
                    Surface(

                        modifier = Modifier.fillMaxSize().systemBarsPadding(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MyAppNavigation(
                            modifier = Modifier.padding(innerPadding),
                            authViewModel = authViewModel,
                            navController = navController,
                            professorViewModel = professorViewModel,
                            studentViewModel = studentViewModel,
                            biometricViewModel = biometricViewModel,
                            activity = this,
                            adminViewModel = adminViewModel
                        )
                    }
                }
            }
        }
    }
}

