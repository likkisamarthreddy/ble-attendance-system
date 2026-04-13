package com.example.practice.auth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practice.DataStore.SimPrefStore
import com.example.practice.RequestBodyApi.ProfessorRegisterRequest
import com.example.practice.RequestBodyApi.StudentRegisterRequest
import com.example.practice.ResponsesModel.UserRoleResponses
import com.example.practice.api.NetworkResponse
import com.example.practice.api.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val TAG = "AuthViewModel" // Consistent tag for logging

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val appApi = RetrofitInstance.authApi

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _roleResult = MutableLiveData<NetworkResponse<UserRoleResponses>>()
    val roleResult: LiveData<NetworkResponse<UserRoleResponses>> = _roleResult

    private val _simBindResult = MutableLiveData<NetworkResponse<SimBindState>>()
    val simBindResult: LiveData<NetworkResponse<SimBindState>> = _simBindResult

    private val _requestSimSelection = MutableLiveData<List<SubscriptionInfo>>()
    val requestSimSelection: LiveData<List<SubscriptionInfo>> = _requestSimSelection

    // Flag to track role fetching status
    private var isFetchingRole = false

    // Context reference for Android ID
    private lateinit var appContext: Context

    init {
        Log.d(TAG, "AuthViewModel initialized")
    }

    fun verifySubscriptionId(context: Context) {
        Log.d(TAG, "verifySubscriptionId() called")
        viewModelScope.launch {
            _simBindResult.value = NetworkResponse.Loading

            val savedId = SimPrefStore.getSimId(context)
            Log.d(TAG, "savedId: ${savedId}")
            var selectedSim = savedId
            if (savedId != null) {
                selectedSim = savedId
            } else{
                val simList = getSimList(context)
                if (simList.isEmpty()) {
                    _simBindResult.value = NetworkResponse.Error("No active SIMs found, must have an active present")
                    return@launch
                }


                selectedSim = simList.first().subscriptionId
            }

            val androidId = getAndroidId(context)
            val user = auth.currentUser
            val idToken = try {
                user?.getIdToken(false)?.await()?.token
            } catch (e: Exception) { null }

            if (idToken == null) {
                _simBindResult.value = NetworkResponse.Error("Failed to get auth token")
                return@launch
            }

            try {
                val response = appApi.bindSim("Bearer $idToken", androidId, selectedSim)
                if (response.isSuccessful) {
                    SimPrefStore.saveSimId(context, selectedSim)
                    _simBindResult.value = NetworkResponse.Success(SimBindState.Connected)
                } else {
                    _simBindResult.value = NetworkResponse.Error("Incorrect sim present")
                }
            } catch (e: Exception) {
                _simBindResult.value = NetworkResponse.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun getSimList(context: Context): List<SubscriptionInfo> {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        return subscriptionManager.activeSubscriptionInfoList ?: emptyList()
    }

    // Set context for the ViewModel to use
    fun setContext(context: Context) {
        appContext = context.applicationContext // Use application context to avoid leaks
    }

    fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    // check auth status and if authenticated then verify role
    fun checkAuthStatus(context: Context) {
        Log.d(TAG, "checkAuthStatus() called")

        // Only skip for WrongRole and Error states, but allow checking
        // even if current state is Unauthenticated (which is now important for app restart)
        val currentState = _authState.value
        if (currentState is AuthState.WrongRole || currentState is AuthState.Error) {
            Log.d(TAG, "checkAuthStatus: Already in a definitive state: ${_authState.value}, skipping check")
            return
        }

        // Set to loading while we check
        _authState.value = AuthState.Loading

        val user = auth.currentUser
        if (user == null) {
            Log.e(TAG, "checkAuthStatus: No current user found, setting Unauthenticated")
            _authState.value = AuthState.Unauthenticated
            // Reset role result to ensure nothing is loading
            _roleResult.value = NetworkResponse.Error("No authenticated user")
            isFetchingRole = false
        } else {
            Log.d(TAG, "checkAuthStatus: Current user found with UID: ${user.uid}")
            // Fetch user role to complete authentication
            fetchUserRole(user.uid, context)
        }
    }

    // Login specifically for students
    fun loginStudent(email: String, password: String) {
        Log.d(TAG, "loginStudent() called with email: $email")
        if (email.isEmpty() || password.isEmpty()) {
            Log.e(TAG, "loginStudent: Empty email or password")
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }

        Log.d(TAG, "loginStudent: Setting AuthState to Loading")
        _authState.value = AuthState.Loading

        Log.d(TAG, "loginStudent: Attempting Firebase signInWithEmailAndPassword")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                Log.d(TAG, "loginStudent: Firebase authentication task completed")
                if (task.isSuccessful) {
                    Log.d(TAG, "loginStudent: Firebase authentication successful")
                    auth.currentUser?.let { user ->
                        Log.d(TAG, "loginStudent: Current user UID: ${user.uid}")
                        verifyStudentRole(user.uid)
                    } ?: run {
                        Log.e(TAG, "loginStudent: Authentication successful but user is null")
                        _authState.value = AuthState.Error("Authentication failed, user not found")
                        resetState()
                    }
                } else {
                    Log.e(TAG, "loginStudent: Firebase authentication failed")
                    val errorMessage = task.exception?.localizedMessage ?: "Unknown Firebase Error"
                    Log.e(TAG, "loginStudent: Error details: $errorMessage", task.exception)
                    _authState.value = AuthState.Error(errorMessage)
                    resetState()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "loginStudent: Firebase authentication failure callback", exception)
                _authState.value = AuthState.Error("Authentication failed: ${exception.message}")
                resetState()
            }
    }

    // Login specifically for professors
    fun loginProfessor(email: String, password: String) {
        Log.d(TAG, "loginProfessor() called with email: $email")
        if (email.isEmpty() || password.isEmpty()) {
            Log.e(TAG, "loginProfessor: Empty email or password")
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }

        Log.d(TAG, "loginProfessor: Setting AuthState to Loading")
        _authState.value = AuthState.Loading

        Log.d(TAG, "loginProfessor: Attempting Firebase signInWithEmailAndPassword")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                Log.d(TAG, "loginProfessor: Firebase authentication task completed")
                if (task.isSuccessful) {
                    Log.d(TAG, "loginProfessor: Firebase authentication successful")
                    auth.currentUser?.let { user ->
                        Log.d(TAG, "loginProfessor: Current user UID: ${user.uid}")
                        verifyProfessorRole(user.uid)
                    } ?: run {
                        Log.e(TAG, "loginProfessor: Authentication successful but user is null")
                        _authState.value = AuthState.Error("Authentication failed, user not found")
                        resetState()
                    }
                } else {
                    Log.e(TAG, "loginProfessor: Firebase authentication failed")
                    val errorMessage = task.exception?.localizedMessage ?: "Unknown Firebase Error"
                    Log.e(TAG, "loginProfessor: Error details: $errorMessage", task.exception)
                    _authState.value = AuthState.Error(errorMessage)
                    resetState()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "loginProfessor: Firebase authentication failure callback", exception)
                _authState.value = AuthState.Error("Authentication failed: ${exception.message}")
                resetState()
            }
    }

    fun loginAdmin(email: String, password: String) {
        Log.d(TAG, "loginAdmin() called with email: $email")
        if (email.isEmpty() || password.isEmpty()) {
            Log.e(TAG, "loginAdmin: Empty email or password")
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }

        Log.d(TAG, "loginAdmin: Setting AuthState to Loading")
        _authState.value = AuthState.Loading

        Log.d(TAG, "loginAdmin: Attempting Firebase signInWithEmailAndPassword")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                Log.d(TAG, "loginAdmin: Firebase authentication task completed")
                if (task.isSuccessful) {
                    Log.d(TAG, "loginAdmin: Firebase authentication successful")
                    auth.currentUser?.let { user ->
                        Log.d(TAG, "loginAdmin: Current user UID: ${user.uid}")
                        verifyAdminRole(user.uid)
                    } ?: run {
                        Log.e(TAG, "loginAdmin: Authentication successful but user is null")
                        _authState.value = AuthState.Error("Authentication failed, user not found")
                        resetState()
                    }
                } else {
                    Log.e(TAG, "loginAdmin: Firebase authentication failed")
                    val errorMessage = task.exception?.localizedMessage ?: "Unknown Firebase Error"
                    Log.e(TAG, "loginAdmin: Error details: $errorMessage", task.exception)
                    _authState.value = AuthState.Error(errorMessage)
                    resetState()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "loginAdmin: Firebase authentication failure callback", exception)
                _authState.value = AuthState.Error("Authentication failed: ${exception.message}")
                resetState()
            }
    }

    // Handle wrong role with a delay before signout
    private fun handleWrongRole(message: String) {
        Log.e(TAG, "handleWrongRole: $message")
        _authState.value = AuthState.WrongRole(message)
        _roleResult.value = NetworkResponse.Error("Wrong user role")
        isFetchingRole = false

        // Sign out after a short delay to allow UI to process the WrongRole state
        viewModelScope.launch {
            delay(1500) // Increased delay to 1.5 seconds to ensure UI catches the state
            signOut(preserveWrongRoleState = true)
        }
    }

    // New method to handle API errors with delayed signout
    private fun handleApiError(errorMessage: String) {
        Log.e(TAG, "handleApiError: $errorMessage")
        _roleResult.value = NetworkResponse.Error(errorMessage)
        _authState.value = AuthState.Error(errorMessage)
        isFetchingRole = false

        // Sign out after a delay to ensure the error state is observed by UI
        viewModelScope.launch {
            delay(1500) // 1.5 second delay to ensure UI has time to show the toast
            signOut()
        }
    }

    // Get Firebase ID Token (JWT) for API calls
    private suspend fun getFirebaseIdToken(): String? {
        return try {
            auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firebase ID token", e)
            null
        }
    }

    // Verify that user has student role
    private fun verifyStudentRole(uid: String) {
        Log.d(TAG, "verifyStudentRole() called with UID: $uid")
        viewModelScope.launch {
            Log.d(TAG, "verifyStudentRole: Starting coroutine to fetch role")
            _roleResult.value = NetworkResponse.Loading
            isFetchingRole = true

            try {
                if (!::appContext.isInitialized) {
                    Log.e(TAG, "verifyStudentRole: Context not initialized")
                    handleApiError("Application context not initialized")
                    return@launch
                }

                val idToken = getFirebaseIdToken()
                if (idToken == null) {
                    handleApiError("Failed to get Firebase ID token")
                    return@launch
                }

                Log.d(TAG, "verifyStudentRole: Calling API with Firebase ID token")
                val request = getAndroidId(appContext)
                val response = appApi.checkUserRole("Bearer $idToken", request)

                Log.d(TAG, "verifyStudentRole: Response received, code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val userRole = response.body()!!
                    Log.d(TAG, "verifyStudentRole: Success response with role: ${userRole.role}")

                    if (userRole.role.equals("student", ignoreCase = true)) {
                        Log.d(TAG, "verifyStudentRole: Verified student role")
                        _roleResult.value = NetworkResponse.Success(userRole)
                        _authState.value = AuthState.Authenticated
                    } else {
                        Log.e(TAG, "verifyStudentRole: User is not a student (role: ${userRole.role})")
                        handleWrongRole("This account is not registered as a student")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e(TAG, "verifyStudentRole: Failed response - Code: ${response.code()}, Error body: $errorBody")
                    handleApiError("Error: $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "verifyStudentRole: Exception during API call", e)
                val error = "Network error: ${e.message}"
                Log.e(TAG, "verifyStudentRole: Setting error state: $error")
                handleApiError(error)
            }
        }
    }

    // Verify that user has professor role
    private fun verifyProfessorRole(uid: String) {
        Log.d(TAG, "verifyProfessorRole() called with UID: $uid")
        viewModelScope.launch {
            Log.d(TAG, "verifyProfessorRole: Starting coroutine to fetch role")
            _roleResult.value = NetworkResponse.Loading
            isFetchingRole = true

            try {
                if (!::appContext.isInitialized) {
                    Log.e(TAG, "verifyProfessorRole: Context not initialized")
                    handleApiError("Application context not initialized")
                    return@launch
                }

                val idToken = getFirebaseIdToken()
                if (idToken == null) {
                    handleApiError("Failed to get Firebase ID token")
                    return@launch
                }

                Log.d(TAG, "verifyProfessorRole: Calling API with Firebase ID token")
                val request = getAndroidId(appContext)
                val response = appApi.checkUserRole("Bearer $idToken", request)

                Log.d(TAG, "verifyProfessorRole: Response received, code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val userRole = response.body()!!
                    Log.d(TAG, "verifyProfessorRole: Success response with role: ${userRole.role}")

                    if (userRole.role.equals("professor", ignoreCase = true)) {
                        Log.d(TAG, "verifyProfessorRole: Verified professor role")
                        _roleResult.value = NetworkResponse.Success(userRole)
                        _authState.value = AuthState.Authenticated
                    } else {
                        Log.e(TAG, "verifyProfessorRole: User is not a professor (role: ${userRole.role})")
                        handleWrongRole("This account is not registered as a professor")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e(TAG, "verifyProfessorRole: Failed response - Code: ${response.code()}, Error body: $errorBody")
                    handleApiError("Role verification failed: $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "verifyProfessorRole: Exception during API call", e)
                val error = "Network error: ${e.message}"
                Log.e(TAG, "verifyProfessorRole: Setting error state: $error")
                handleApiError(error)
            }
        }
    }

    // Verify that user has admin role
    private fun verifyAdminRole(uid: String) {
        Log.d(TAG, "verifyAdminRole() called with UID: $uid")
        viewModelScope.launch {
            Log.d(TAG, "verifyAdminRole: Starting coroutine to fetch role")
            _roleResult.value = NetworkResponse.Loading
            isFetchingRole = true

            try {
                if (!::appContext.isInitialized) {
                    Log.e(TAG, "verifyAdminRole: Context not initialized")
                    handleApiError("Application context not initialized")
                    return@launch
                }

                val idToken = getFirebaseIdToken()
                if (idToken == null) {
                    handleApiError("Failed to get Firebase ID token")
                    return@launch
                }

                Log.d(TAG, "verifyAdminRole: Calling API with Firebase ID token")
                val request = getAndroidId(appContext)
                val response = appApi.checkUserRole("Bearer $idToken", request)

                Log.d(TAG, "verifyAdminRole: Response received, code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val userRole = response.body()!!
                    Log.d(TAG, "verifyAdminRole: Success response with role: ${userRole.role}")

                    if (userRole.role.equals("admin", ignoreCase = true)) {
                        Log.d(TAG, "verifyAdminRole: Verified admin role")
                        _roleResult.value = NetworkResponse.Success(userRole)
                        _authState.value = AuthState.Authenticated
                    } else {
                        Log.e(TAG, "verifyAdminRole: User is not a admin (role: ${userRole.role})")
                        handleWrongRole("This account is not registered as a admin")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e(TAG, "verifyAdminRole: Failed response - Code: ${response.code()}, Error body: $errorBody")
                    handleApiError("Role verification failed: $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "verifyAdminRole: Exception during API call", e)
                val error = "Network error: ${e.message}"
                Log.e(TAG, "verifyAdminRole: Setting error state: $error")
                handleApiError(error)
            }
        }
    }

    // For general role fetching (e.g., after app restart)
    private fun fetchUserRole(uid: String, context: Context) {
        Log.d(TAG, "fetchUserRole() called with UID: $uid")
        if (isFetchingRole) {
            Log.e(TAG, "fetchUserRole: Already fetching role, skipping duplicate request")
            return
        }

        Log.d(TAG, "fetchUserRole: Setting isFetchingRole = true")
        isFetchingRole = true
        _authState.value = AuthState.Loading // Set to loading while fetching role

        viewModelScope.launch {
            Log.d(TAG, "fetchUserRole: Starting coroutine to fetch role")
            _roleResult.value = NetworkResponse.Loading
            try {
                if (!::appContext.isInitialized) {
                    Log.e(TAG, "fetchUserRole: Context not initialized")
                    handleApiError("Application context not initialized")
                    return@launch
                }

                val idToken = getFirebaseIdToken()
                if (idToken == null) {
                    handleApiError("Failed to get Firebase ID token")
                    return@launch
                }

                Log.d(TAG, "fetchUserRole: Calling API with Firebase ID token")
                val request = getAndroidId(appContext)
                val response = appApi.checkUserRole("Bearer $idToken", request)

                Log.d(TAG, "fetchUserRole: Response received, code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "fetchUserRole: Success response with body: ${response.body()}")
                    _roleResult.value = NetworkResponse.Success(response.body()!!)
                    _authState.value = AuthState.Authenticated
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e(TAG, "fetchUserRole: Failed response - Code: ${response.code()}, Error body: $errorBody")
                    handleApiError("Role fetch failed: $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchUserRole: Exception during API call", e)
                val error = "Network error: ${e.message}"
                Log.e(TAG, "fetchUserRole: Setting error state: $error")
                handleApiError(error)
            } finally {
                Log.d(TAG, "fetchUserRole: Setting isFetchingRole = false")
                isFetchingRole = false
            }
        }
    }

    fun signup(email: String, password: String, name: String, roll: Int?, isProfessor: Boolean) {
        Log.d(TAG, "signup() called - Email: $email, Name: $name, IsProfessor: $isProfessor, Roll: $roll")

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Log.e(TAG, "signup: Empty required fields")
            _authState.value = AuthState.Error("Fields can't be empty")
            return
        }

        Log.d(TAG, "signup: Setting AuthState to Loading")
        _authState.value = AuthState.Loading

        Log.d(TAG, "signup: Attempting Firebase createUserWithEmailAndPassword")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                Log.d(TAG, "signup: Firebase registration task completed")
                if (task.isSuccessful) {
                    Log.d(TAG, "signup: Firebase registration successful")
                    auth.currentUser?.let { user ->
                        Log.d(TAG, "signup: Current user UID: ${user.uid}")
                        registerUserInBackend(user.uid, name, email, roll, isProfessor)
                    } ?: run {
                        Log.e(TAG, "signup: Registration successful but user is null")
                        _authState.value = AuthState.Error("Firebase registration failed")
                        resetState()
                    }
                } else {
                    Log.e(TAG, "signup: Firebase registration failed")
                    val errorMessage = task.exception?.localizedMessage ?: "Unknown Firebase Error"
                    Log.e(TAG, "signup: Error details: $errorMessage", task.exception)
                    _authState.value = AuthState.Error(errorMessage)
                    resetState()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "signup: Firebase registration failure callback", exception)
                _authState.value = AuthState.Error("Registration failed: ${exception.message}")
                resetState()
            }
    }

    private fun registerUserInBackend(uid: String, name: String, email: String, roll: Int?, isProfessor: Boolean) {
        Log.d(TAG, "registerUserInBackend() called - UID: $uid, Name: $name, Email: $email, Roll: $roll, IsProfessor: $isProfessor")

        viewModelScope.launch {
            Log.d(TAG, "registerUserInBackend: Starting coroutine to register in backend")

            if (!::appContext.isInitialized) {
                Log.e(TAG, "registerUserInBackend: Context not initialized")
                handleApiError("Application context not initialized")
                return@launch
            }

            val androidId = getAndroidId(appContext)
            val request = if (isProfessor) {
                Log.d(TAG, "registerUserInBackend: Creating ProfessorRegisterRequest")
                ProfessorRegisterRequest(name, email, uid)
            } else {
                Log.d(TAG, "registerUserInBackend: Creating StudentRegisterRequest with roll: ${roll ?: 0}")
                StudentRegisterRequest(name, roll ?: 0, email, uid, false)
            }

            try {
                val idToken = getFirebaseIdToken()
                if (idToken == null) {
                    handleApiError("Failed to get Firebase ID token")
                    return@launch
                }

                Log.d(TAG, "registerUserInBackend: Calling API with Firebase ID token")
                val response = if (isProfessor) {
                    Log.d(TAG, "registerUserInBackend: Calling registerProfessor API")
                    appApi.registerProfessor(request as ProfessorRegisterRequest, "Bearer $idToken")
                } else {
                    Log.d(TAG, "registerUserInBackend: Calling registerStudent API")
                    appApi.registerStudent(request as StudentRegisterRequest, "Bearer $idToken")
                }

                Log.d(TAG, "registerUserInBackend: Response received, code: ${response.code()}")
                Log.d(TAG, "registerUserInBackend: Response body: ${response.body()}")

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "registerUserInBackend: Success response")
                    _authState.value = AuthState.Authenticated
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e(TAG, "registerUserInBackend: Failed response - Code: ${response.code()}, Error body: $errorBody")
                    handleApiError("Backend registration failed: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "registerUserInBackend: Exception during API call", e)
                val error = "Network error: ${e.message}"
                Log.e(TAG, "registerUserInBackend: Setting error state: $error")
                handleApiError(error)
            }
        }
    }

    // Helper method to reset all state variables
    private fun resetState() {
        isFetchingRole = false
    }

    // Modified to optionally preserve WrongRole state
    fun signOut(preserveWrongRoleState: Boolean = false) {
        Log.d(TAG, "signOut() called, preserveWrongRoleState=$preserveWrongRoleState")

        // Check if we're in WrongRole state and should preserve it
        val currentState = _authState.value
        val isWrongRoleState = currentState is AuthState.WrongRole

        auth.signOut()
        Log.d(TAG, "signOut: User signed out from Firebase")

        // Reset state variables, but preserve WrongRole if requested
        if (!(preserveWrongRoleState && isWrongRoleState)) {
            _authState.value = AuthState.Unauthenticated
            Log.d(TAG, "signOut: Reset to Unauthenticated state")
        } else {
            Log.d(TAG, "signOut: Preserving WrongRole state: $currentState")
        }

        _roleResult.value = NetworkResponse.Error("User signed out")
        _simBindResult.value = NetworkResponse.Error("User signed out")
        isFetchingRole = false

        Log.d(TAG, "signOut: All states reset")
    }

    // Called when WrongRole or another error occurs to ensure clean state
    fun resetAfterError() {
        Log.d(TAG, "resetAfterError() called")
        _authState.value = AuthState.Unauthenticated
        _roleResult.value = NetworkResponse.Error("Reset after error")
        isFetchingRole = false
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "AuthViewModel onCleared() called")
    }
}

// Authentication States
sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
    data class WrongRole(val message: String) : AuthState() // State for wrong role attempts
}

sealed class SimBindState {
    object idle : SimBindState()
    object Connected : SimBindState()
    object Loading : SimBindState()
    data class Error(val message: String) : SimBindState()
}