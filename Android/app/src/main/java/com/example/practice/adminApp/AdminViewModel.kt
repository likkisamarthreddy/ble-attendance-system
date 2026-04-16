package com.example.practice.adminApp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practice.RequestBodyApi.StudentRegisterRequest
import com.example.practice.ResponsesModel.AllStudentsData
import com.example.practice.ResponsesModel.AttendanceRecord
import com.example.practice.ResponsesModel.AttendanceXX
import com.example.practice.ResponsesModel.CourseStudentView
import com.example.practice.ResponsesModel.FetchAllAttendanceRecord
import com.example.practice.ResponsesModel.DashboardStatsResponse
import com.example.practice.ResponsesModel.DetailedStudentsResponse
import com.example.practice.ResponsesModel.GetRecordDataResponse
import com.example.practice.ResponsesModel.ListOfProfessor
import com.example.practice.ResponsesModel.Professor
import com.example.practice.ResponsesModel.RecordXX
import com.example.practice.ResponsesModel.Student
import com.example.practice.ResponsesModel.StudentAttendanceByCourse
import com.example.practice.ResponsesModel.StudentRegisterCsvResponse
import com.example.practice.ResponsesModel.ViewAllAttendanceRecords
import com.example.practice.ResponsesModel.ViewCourses
import com.example.practice.ResponsesModel.toDomain
import com.example.practice.api.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.practice.utils.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AdminViewModel : ViewModel() {

    private val TAG = "AdminViewModel"
    private val appApi = RetrofitInstance.adminApi

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _studentRegisterCsv = MutableLiveData<StudentRegisterCsvState>()
    val studentRegisterCsv: LiveData<StudentRegisterCsvState> = _studentRegisterCsv

    private val _adminViewCurrentData = MutableLiveData<AdminViewCurrrentCourseState>()
    val adminViewCurrentData: LiveData<AdminViewCurrrentCourseState> = _adminViewCurrentData

    private val _courseTitle = MutableLiveData<String>("")
    val courseTitle: LiveData<String> = _courseTitle

    private val _batchName = MutableLiveData<String>("")
    val batchName: LiveData<String> = _batchName

    private val _joiningCode = MutableLiveData<String>("")
    val joiningCode: LiveData<String> = _joiningCode

    private val _courseYear = MutableLiveData<Int>()
    val courseYear: LiveData<Int> = _courseYear

    private val _courseExpiry = MutableLiveData<String>("")
    val courseExpiry: LiveData<String> = _courseExpiry

    private val _isArchivedSelected = MutableLiveData<Boolean>(false)
    val isArchivedSelected: LiveData<Boolean> = _isArchivedSelected

    private val _studentAccountCreated = MutableLiveData<CreateStudentAccountState>()
    val studentAccountCreated: LiveData<CreateStudentAccountState> = _studentAccountCreated

    private val _courseStudentsData = MutableLiveData<CourseStudentsState>()
    val courseStudentsData: LiveData<CourseStudentsState> = _courseStudentsData

    private val _viewAllAttendanceRecords = MutableLiveData<ViewAllAttendanceRecordsState>()
    val viewAllAttendanceRecords: LiveData<ViewAllAttendanceRecordsState> = _viewAllAttendanceRecords

    private val _fullAttendanceData = MutableLiveData<FullAttendanceDataState>()
    val fullAttendanceData: LiveData<FullAttendanceDataState> = _fullAttendanceData

    private val _viewRecordData = MutableLiveData<ViewRecordDataState>()
    val viewRecordData: LiveData<ViewRecordDataState> = _viewRecordData

    private val _currentRecords = MutableLiveData<List<AttendanceXX>>()
    val currentRecords: LiveData<List<AttendanceXX>> = _currentRecords

    private val _allStudentsData = MutableLiveData<AllStudentsState>()
    val allStudentsData: LiveData<AllStudentsState> = _allStudentsData

    private val _allProfessorData = MutableLiveData<AllProfessorState>()
    val allProfessorData: LiveData<AllProfessorState> = _allProfessorData

    private val _studentAttendanceByCourseData = MutableLiveData<StudentAttendanceByCourseState>()
    val studentAttendanceByCourseData: LiveData<StudentAttendanceByCourseState> = _studentAttendanceByCourseData

    // ── New UI States for Dashboard Redesign ──

    private val _dashboardStats = MutableLiveData<DashboardStatsState>()
    val dashboardStats: LiveData<DashboardStatsState> = _dashboardStats

    private val _detailedStudentsData = MutableLiveData<DetailedStudentsState>()
    val detailedStudentsData: LiveData<DetailedStudentsState> = _detailedStudentsData

    private val _deleteStudentState = MutableLiveData<DeleteStudentState>()
    val deleteStudentState: LiveData<DeleteStudentState> = _deleteStudentState

    private val _auditLogs = MutableLiveData<AuditLogsState>()
    val auditLogs: LiveData<AuditLogsState> = _auditLogs

    private val _securityStats = MutableLiveData<SecurityStatsState>()
    val securityStats: LiveData<SecurityStatsState> = _securityStats

    private val _searchStudentsData = MutableLiveData<SearchStudentState>()
    val searchStudentsData: LiveData<SearchStudentState> = _searchStudentsData

    fun fetchDashboardStats() {
        _dashboardStats.value = DashboardStatsState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token == null) {
                _dashboardStats.value = DashboardStatsState.Error("Failed to get Firebase token")
                return@launch
            }
            when (val result = com.example.practice.utils.safeApiCall { appApi.getDashboardStats("Bearer $token") }) {
                is com.example.practice.utils.ApiResult.Success -> {
                    try {
                        val mappedStats = result.data.toDomain()
                        _dashboardStats.value = DashboardStatsState.Success(mappedStats)
                    } catch (e: Exception) {
                        _dashboardStats.value = DashboardStatsState.Error("Mapping error: ${e.message}")
                    }
                }
                is com.example.practice.utils.ApiResult.Error -> {
                    _dashboardStats.value = DashboardStatsState.Error(result.message)
                }
            }
        }
    }

    fun fetchAuditLogs(page: Int, limit: Int = 20, action: String? = null) {
        _auditLogs.value = AuditLogsState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token == null) {
                _auditLogs.value = AuditLogsState.Error("Failed to get Firebase token")
                return@launch
            }
            try {
                val actionParam = if (action.isNullOrEmpty()) null else action
                val response = appApi.getAuditLogs("Bearer $token", page, limit, actionParam)
                if (response.isSuccessful && response.body() != null) {
                    _auditLogs.value = AuditLogsState.Success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    _auditLogs.value = AuditLogsState.Error("Failed: $errorMsg")
                }
            } catch (e: Exception) {
                _auditLogs.value = AuditLogsState.Error("Error: ${e.message}")
            }
        }
    }

    fun fetchSecurityStats() {
        _securityStats.value = SecurityStatsState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token == null) {
                _securityStats.value = SecurityStatsState.Error("Failed to get Firebase token")
                return@launch
            }
            try {
                val response = appApi.getSecurityStats("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _securityStats.value = SecurityStatsState.Success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    _securityStats.value = SecurityStatsState.Error("Failed: $errorMsg")
                }
            } catch (e: Exception) {
                _securityStats.value = SecurityStatsState.Error("Error: ${e.message}")
            }
        }
    }

    fun searchStudents(branch: String? = null, section: String? = null, year: String? = null, rollno: String? = null, name: String? = null) {
        _searchStudentsData.value = SearchStudentState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token == null) {
                _searchStudentsData.value = SearchStudentState.Error("Failed to get Firebase token")
                return@launch
            }
            try {
                val response = appApi.searchStudents(
                    "Bearer $token", 
                    branch?.takeIf { it.isNotEmpty() }, 
                    section?.takeIf { it.isNotEmpty() }, 
                    year?.takeIf { it.isNotEmpty() }, 
                    rollno?.takeIf { it.isNotEmpty() }, 
                    name?.takeIf { it.isNotEmpty() }
                )
                if (response.isSuccessful && response.body() != null) {
                    _searchStudentsData.value = SearchStudentState.Success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    _searchStudentsData.value = SearchStudentState.Error("Failed: $errorMsg")
                }
            } catch (e: Exception) {
                _searchStudentsData.value = SearchStudentState.Error("Error: ${e.message}")
            }
        }
    }

    fun fetchDetailedStudents() {
        _detailedStudentsData.value = DetailedStudentsState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token == null) {
                _detailedStudentsData.value = DetailedStudentsState.Error("Failed to get Firebase token")
                return@launch
            }
            try {
                val response = appApi.getDetailedStudents("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _detailedStudentsData.value = DetailedStudentsState.Success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    _detailedStudentsData.value = DetailedStudentsState.Error("Failed: $errorMsg")
                }
            } catch (e: Exception) {
                _detailedStudentsData.value = DetailedStudentsState.Error("Error: ${e.message}")
            }
        }
    }

    fun deleteStudent(studentId: Int) {
        _deleteStudentState.value = DeleteStudentState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token == null) {
                _deleteStudentState.value = DeleteStudentState.Error("Failed to get Firebase token")
                return@launch
            }
            try {
                val response = appApi.deleteStudent("Bearer $token", studentId)
                if (response.isSuccessful) {
                    _deleteStudentState.value = DeleteStudentState.Success
                    // Refresh data after deletion
                    fetchDetailedStudents()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    _deleteStudentState.value = DeleteStudentState.Error("Failed: $errorMsg")
                }
            } catch (e: Exception) {
                _deleteStudentState.value = DeleteStudentState.Error("Error: ${e.message}")
            }
        }
    }

    fun resetDeleteStudentState() {
        _deleteStudentState.value = DeleteStudentState.Idle
    }


    fun selectCourse(course: String, batch: String, courseExpiry: String, year: Int, joiningCode: String) {
        Log.d("adminViewModel", "Select course Called")
        _courseTitle.value = course
        _batchName.value = batch
        _courseExpiry.value = courseExpiry
        _courseYear.value = year
        _joiningCode.value = joiningCode
    }

    fun resetSelectedCourse(){
        _courseTitle.value = ""
        _batchName.value = ""
        _courseExpiry.value = ""
        _courseYear.value = 0
        _joiningCode.value = ""
    }

    private fun createCsvPart(file: File): MultipartBody.Part {
        val requestBody = file
            .asRequestBody("text/csv".toMediaTypeOrNull())

        return MultipartBody.Part.createFormData(
            "file", // must match `req.file` on backend
            file.name,
            requestBody
        )
    }

    /** Fetch Firebase Token (Now a Suspend Function) */
    private suspend fun getFirebaseToken(): String? {
        return try {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            firebaseUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firebase token: ${e.message}", e)
            null
        }
    }

    fun registerStudentsCsv(file: File, isProfessor: Boolean, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        if (!file.exists()) {
            val errorMsg = "File does not exist"
            Log.e(TAG, errorMsg)
            _studentRegisterCsv.value = StudentRegisterCsvState.Error(errorMsg)
            onError(errorMsg)
            return
        }

        Log.d(TAG, "Starting CSV registration with file: ${file.absolutePath}, size: ${file.length()} bytes")
        _studentRegisterCsv.value = StudentRegisterCsvState.Loading

        viewModelScope.launch {
            try {
                val token = getFirebaseToken()
                if (token != null) {
                    Log.d(TAG, "Successfully retrieved Firebase token")
                    registerStudentsCsvFromApi(file, token, isProfessor, onSuccess, onError)
                } else {
                    val errorMsg = "Failed to get Firebase token"
                    Log.e(TAG, errorMsg)
                    _studentRegisterCsv.value = StudentRegisterCsvState.Error(errorMsg)
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Error in registration process: ${e.message}"
                Log.e(TAG, errorMsg, e)
                _studentRegisterCsv.value = StudentRegisterCsvState.Error(errorMsg)
                onError(errorMsg)
            }
        }
    }

    private suspend fun registerStudentsCsvFromApi(
        file: File,
        token: String,
        isProfessor: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            Log.d(TAG, "Creating CSV part for file upload")
            val csvPart = createCsvPart(file)

            Log.d(TAG, "Sending CSV to API")

            val response: Response<StudentRegisterCsvResponse> = appApi.studentRegisterCsv("Bearer $token", isProfessor, csvPart)

            if (response.isSuccessful && response.body() != null) {
                val responseData = response.body()!!
                Log.d(TAG, "CSV upload response received: $responseData")

                // Check if there were any errors in the response
                if (responseData.errors.isNotEmpty()) {
                    // There were errors in the processing
                    val errorCount = responseData.errors.size
                    val successCount = responseData.success
                    val totalCount = responseData.total

                    // Format a user-friendly message with error details
                    val errorSummary = if (errorCount <= 3) {
                        // Show specific errors if there are just a few
                        responseData.errors.joinToString(", ") {
                            "Email: ${it.email}, Error: ${it.error}"
                        }
                    } else {
                        // Just show the count for many errors
                        "$errorCount errors occurred during processing"
                    }

                    val message = "Processed $totalCount records with $successCount successful and $errorCount failed. $errorSummary"
                    Log.w(TAG, "CSV processing completed with errors: $message")

                    // We'll still consider this a mixed success
                    _studentRegisterCsv.value = StudentRegisterCsvState.PartialSuccess(responseData, message)

                    // If all records failed, treat as error
                    if (successCount == 0) {
                        onError(message)
                    } else {
                        onSuccess()
                    }
                } else {
                    // No errors, complete success
                    val message = "Successfully processed all ${responseData.total} student records"
                    Log.d(TAG, message)
                    _studentRegisterCsv.value = StudentRegisterCsvState.Success(responseData)
                    onSuccess()
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                Log.e(TAG, "API error: ${response.code()} - $errorMsg")
                _studentRegisterCsv.value = StudentRegisterCsvState.Error("Failed to register students: ${response.code()} - $errorMsg")
                onError("Failed to register students: ${response.code()} - $errorMsg")
            }
        } catch (e: Exception) {
            val errorMsg = "Error during API call: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _studentRegisterCsv.value = StudentRegisterCsvState.Error(errorMsg)
            onError(errorMsg)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun createStudentAccount(
        email: String, name: String, roll: Int, isProfessor: Boolean
    ) {
        if (email.isEmpty() || name.isEmpty()) {
            if(!isProfessor && roll <= 0)
            Log.e(TAG, "All fields are required")
            _studentAccountCreated.value = CreateStudentAccountState.Error("Fields can't be empty")
            return
        }

        _studentAccountCreated.value = CreateStudentAccountState.Loading

        viewModelScope.launch {
            try {
                // Step 1: Create Firebase account
                val firebaseUser = createFirebaseAccount(email)

                if (firebaseUser != null) {
                    Log.d(TAG, "Firebase account created successfully: ${firebaseUser.uid}")

                    // Step 2: Register in backend
                    val backendSuccess = registerUserInBackend(firebaseUser.uid, name, email, roll, isProfessor)

                    if (backendSuccess) {
                        Log.d(TAG, "Both Firebase and backend registration successful")
                        // Success handled in registerUserInBackend
                    } else {
                        Log.e(TAG, "Backend registration failed, rolling back Firebase account")
                        // Rollback: Delete the Firebase account
                        rollbackFirebaseAccount(firebaseUser)
                        // Error state already set in registerUserInBackend
                    }
                } else {
                    Log.e(TAG, "Firebase account creation failed")
                    // Error state already set in createFirebaseAccount
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in createStudentAccount", e)
                _studentAccountCreated.value = CreateStudentAccountState.Error("Unexpected error: ${e.message}")
            }
        }
    }

    private suspend fun createFirebaseAccount(email: String): FirebaseUser? {
        return try {
            val password = email.split("@")[0]
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()

            val user = authResult.user
            if (user != null) {
                Log.d(TAG, "Firebase account created successfully for: $email")
                user
            } else {
                Log.e(TAG, "Firebase account creation returned null user")
                _studentAccountCreated.value = CreateStudentAccountState.Error("Firebase account creation failed")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase account creation failed", e)
            val errorMessage = e.localizedMessage ?: "Unknown Firebase Error"
            _studentAccountCreated.value = CreateStudentAccountState.Error("Firebase registration failed: $errorMessage")
            null
        }
    }

    private suspend fun registerUserInBackend(uid: String, name: String, email: String, roll: Int, isProfessor: Boolean): Boolean {
        Log.d(TAG, "registerUserInBackend() called - UID: $uid, Name: $name, Email: $email, Roll: $roll")

        return try {
            Log.d(TAG, "registerUserInBackend: Creating StudentRegisterRequest with roll: $roll")
            val request = StudentRegisterRequest(name, roll, email, uid, isProfessor)

            Log.d(TAG, "registerUserInBackend: Calling API with request: $request")
            val response = appApi.createStudentAccount("Bearer $uid", request)

            Log.d(TAG, "registerUserInBackend: Response received, code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "registerUserInBackend: Success response")
                _studentAccountCreated.value = CreateStudentAccountState.Success(response.body()!!.toDomain())
                true
            } else {
                val errorBody = response.errorBody()?.string() ?: "No error body"
                Log.e(TAG, "registerUserInBackend: Failed - Code: ${response.code()}, Error: $errorBody")
                _studentAccountCreated.value = CreateStudentAccountState.Error("Backend registration failed: ${response.code()} - $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerUserInBackend: Exception during API call", e)
            val error = "Network error: ${e.message}"
            _studentAccountCreated.value = CreateStudentAccountState.Error("Backend registration failed: $error")
            false
        }
    }

    private suspend fun rollbackFirebaseAccount(user: FirebaseUser) {
        try {
            Log.d(TAG, "Rolling back Firebase account for user: ${user.uid}")
            user.delete().await()
            Log.d(TAG, "Successfully deleted Firebase account: ${user.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rollback Firebase account: ${user.uid}", e)
            // Note: Even if rollback fails, we've already set the error state
            // You might want to log this for manual cleanup
        }
    }

    fun resetCreateStudentAccountState() {
        _studentAccountCreated.value = CreateStudentAccountState.Idle
    }

    fun fetchAllActiveCourses(isArchivedSelected: Boolean) {
        _adminViewCurrentData.value = AdminViewCurrrentCourseState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                if(isArchivedSelected == false) {
                    fetchCurrentCoursesFromApi(token)
                }
                else{
                    fetchArchivedCoursesFromApi(token)
                }
            } else {
                _adminViewCurrentData.value = AdminViewCurrrentCourseState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchArchivedCoursesFromApi(token: String) {
        when (val result = com.example.practice.utils.safeApiCall { appApi.viewArchivedCourses("Bearer $token") }) {
            is com.example.practice.utils.ApiResult.Success -> {
                try {
                    val mappedCourses = result.data.courses.map { it.toDomain() }
                    _adminViewCurrentData.value = AdminViewCurrrentCourseState.Success(mappedCourses)
                } catch (e: Exception) {
                    _adminViewCurrentData.value = AdminViewCurrrentCourseState.Error("Mapping error: ${e.message}")
                }
            }
            is com.example.practice.utils.ApiResult.Error -> {
                _adminViewCurrentData.value = AdminViewCurrrentCourseState.Error(result.message)
            }
        }
    }

    private suspend fun fetchCurrentCoursesFromApi(token: String) {
        when (val result = com.example.practice.utils.safeApiCall { appApi.viewCurrentCourses("Bearer $token") }) {
            is com.example.practice.utils.ApiResult.Success -> {
                try {
                    val mappedCourses = result.data.courses.map { it.toDomain() }
                    _adminViewCurrentData.value = AdminViewCurrrentCourseState.Success(mappedCourses)
                } catch (e: Exception) {
                    _adminViewCurrentData.value = AdminViewCurrrentCourseState.Error("Mapping error: ${e.message}")
                }
            }
            is com.example.practice.utils.ApiResult.Error -> {
                _adminViewCurrentData.value = AdminViewCurrrentCourseState.Error(result.message)
            }
        }
    }

    fun ArchiveSection(){
        _isArchivedSelected.value = !(_isArchivedSelected.value ?: false)
    }

    fun fetchStudentsInCourse(joiningCode: String,  batch: String, courseName: String, isArchived: Boolean, year: Int) {
        Log.d("adminViewModel", "fetchStudentsInCourse() called")
        _courseStudentsData.value = CourseStudentsState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                Log.d("adminViewModel", "Fetch student from api called")
                fetchStudentsFromApi(token, batch, courseName, isArchived, year, joiningCode)
            } else {
                _courseStudentsData.value = CourseStudentsState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchStudentsFromApi(token: String, batch: String, courseName: String, isArchived: Boolean, year: Int, joiningCode: String) {

        try {
            Log.d("adminViewModel", "Fetching students for course: $courseName")
            val response: Response<CourseStudentView> =
                appApi.getStudentsInCourse("Bearer $token", batch, courseName, isArchived, year, joiningCode)

            if (response.isSuccessful && response.body() != null) {
                Log.d("adminViewModel", "response success")
                val mappedStudents = response.body()!!.students.map { it.toDomain() }
                _courseStudentsData.value = CourseStudentsState.Success(mappedStudents)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                _courseStudentsData.value = CourseStudentsState.Error("Failed to fetch students: ${response.code()} - $errorMsg")
            }
        } catch (e: Exception) {
            _courseStudentsData.value = CourseStudentsState.Error("Error: ${e.message}")
        }
    }

    fun fetchAllAttendanceRecords(joiningCode: String, courseName: String, batch: String, year: Int, isArchived: Boolean) {
        _viewAllAttendanceRecords.value = ViewAllAttendanceRecordsState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                fetchAllAttendanceRecordsFromApi(token, joiningCode, courseName, batch, year, isArchived)
            } else {
                _viewAllAttendanceRecords.value = ViewAllAttendanceRecordsState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchAllAttendanceRecordsFromApi(token: String, joiningCode: String, courseName: String, batch: String, year: Int, isArchived: Boolean) {
        try {
            val response: Response<ViewAllAttendanceRecords> =
                appApi.getAllAttendanceRecords("Bearer $token", joiningCode, batch, courseName, year, isArchived)

            if (response.isSuccessful && response.body() != null) {
                _viewAllAttendanceRecords.value = ViewAllAttendanceRecordsState.Success(response.body()!!.records)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                _viewAllAttendanceRecords.value = ViewAllAttendanceRecordsState.Error("Failed to fetch students: ${response.code()} - $errorMsg")
            }
        } catch (e: Exception) {
            _viewAllAttendanceRecords.value = ViewAllAttendanceRecordsState.Error("Error: ${e.message}")
        }
    }

    suspend fun createExcelFile(
        context: Context,
        attendanceData: List<AttendanceRecord>,
        courseName: String,
        courseBatch: String,
        year: Int
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Attendance Data - ${courseName}(${courseBatch}) - (${year})")

                var rowIndex = 0

                // Extract Unique Student Roll Numbers (Sorted)
                val studentRollNumbers = attendanceData
                    .flatMap { it.attendance.keys }
                    .toSet()
                    .sorted()

                // Create a pair of (date, index) to preserve the original order
                // This ensures same dates maintain their sequence
                val dateColumns = attendanceData.mapIndexed { index, record ->
                    Pair(record.date, index)
                }

                // Create Header Row: "Student Roll No" + Each Attendance Record's Date
                val headerRow = sheet.createRow(rowIndex++)
                headerRow.createCell(0).setCellValue("Student Roll No") // First column (Roll No)

                // Create date columns in order of the original attendance data
                dateColumns.forEachIndexed { colIndex, (date, _) ->
                    headerRow.createCell(colIndex + 1).setCellValue(date)
                }

                // Populate Attendance Data for Each Student
                studentRollNumbers.forEach { rollNo ->
                    val row = sheet.createRow(rowIndex++)
                    row.createCell(0).setCellValue(rollNo) // Student Roll No in first column

                    // For each date column, find the corresponding attendance record
                    dateColumns.forEachIndexed { colIndex, (date, originalIndex) ->
                        // Get this specific attendance record
                        val record = attendanceData[originalIndex]
                        // Get status for this student from this specific record
                        val status = if (record.date == date) {
                            record.attendance[rollNo] ?: "Absent"
                        } else {
                            "Absent" // Should never happen, just a safeguard
                        }

                        Log.d("ExcelProfessorView", "Status for $rollNo on $date: $status")
                        row.createCell(colIndex + 1).setCellValue(status)
                    }
                }

                // Save the File
                val fileName = "Attendance_${System.currentTimeMillis()}.xlsx"
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
                FileOutputStream(file).use { workbook.write(it) }
                workbook.close()

                Log.d("ExcelProfessorView", "Excel file saved: ${file.absolutePath}")
                return@withContext file
            } catch (e: IOException) {
                Log.e("ExcelError", "Error creating Excel file", e)
                return@withContext null
            }
        }
    }

    /** Fetches all Attendance Record present given course */
    fun fetchFullAttendanceData(joiningCode: String, courseName: String, batch: String, year: Int, isArchived: Boolean) {
        _fullAttendanceData.value = FullAttendanceDataState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                fetchFullAttendanceDataFromApi(token, joiningCode, courseName, batch, year, isArchived)
            } else {
                _fullAttendanceData.value = FullAttendanceDataState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchFullAttendanceDataFromApi(token: String, joiningCode: String, courseName: String, batch: String, year: Int, isArchived: Boolean) {
        try {
            val response: Response<FetchAllAttendanceRecord> =
                appApi.getFullAttendanceRecord("Bearer $token", joiningCode, batch, courseName, year, isArchived)

            if (response.isSuccessful && response.body() != null) {
                _fullAttendanceData.value = FullAttendanceDataState.Success(response.body()!!.attendanceSheet)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                _fullAttendanceData.value = FullAttendanceDataState.Error("Failed to fetch students: ${response.code()} - $errorMsg")
            }
        } catch (e: Exception) {
            _fullAttendanceData.value = FullAttendanceDataState.Error("Error: ${e.message}")
        }
    }

    fun resetViewRecordData() {
        _viewRecordData.value = ViewRecordDataState.Idle
    }

    fun fetchRecordData(recordId: String, isArchived: Boolean) {
        _viewRecordData.value = ViewRecordDataState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                fetchRecordDataFromApi(token, recordId, isArchived)
            } else {
                _viewRecordData.value = ViewRecordDataState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchRecordDataFromApi(token: String, recordId: String, isArchived: Boolean) {
        try {
            val response: Response<GetRecordDataResponse> =
                appApi.getRecordData("Bearer $token", recordId, isArchived)

            if (response.isSuccessful && response.body() != null) {
                _viewRecordData.value = ViewRecordDataState.Success(response.body()!!.attendance)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                _viewRecordData.value = ViewRecordDataState.Error("Failed to fetch students: ${response.code()} - $errorMsg")
            }
        } catch (e: Exception) {
            _viewRecordData.value = ViewRecordDataState.Error("Error: ${e.message}")
        }
    }

    fun setCurrentRecords(records: List<AttendanceXX>) {
        _currentRecords.value = records
    }

    fun fetchAllStudents(){
        _allStudentsData.value = AllStudentsState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                fetchAllStudentsFromApi(token)
            } else {
                _allStudentsData.value = AllStudentsState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchAllStudentsFromApi(token: String) {
        when (val result = com.example.practice.utils.safeApiCall { appApi.getDetailedStudents("Bearer $token") }) {
            is com.example.practice.utils.ApiResult.Success -> {
                try {
                    val mappedStudents = result.data.students.map { detailedStudent ->
                        Student(
                            _id = detailedStudent.id.toString(),
                            id = detailedStudent.id,
                            name = detailedStudent.name,
                            rollno = detailedStudent.rollno,
                            email = detailedStudent.email,
                            courses = emptyList(),
                            uid = detailedStudent.uid,
                            batch = detailedStudent.batch,
                            attendancePercentage = (detailedStudent.attendancePercentage ?: 0).toString()
                        )
                    }
                    _allStudentsData.value = AllStudentsState.Success(mappedStudents)
                } catch (e: Exception) {
                    _allStudentsData.value = AllStudentsState.Error("Mapping error: ${e.message}")
                }
            }
            is com.example.practice.utils.ApiResult.Error -> {
                _allStudentsData.value = AllStudentsState.Error(result.message)
            }
        }
    }

    fun fetchAllProfessors(){
        _allProfessorData.value = AllProfessorState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if(token != null){
                fetchAllProfessorFromApi(token)
            } else{
                _allProfessorData.value = AllProfessorState.Error("Failed to get Firebase token")
            }

        }
    }

    private suspend fun fetchAllProfessorFromApi(token: String) {

        try {
            val response: Response<ListOfProfessor> =
                appApi.getAllProfessorData("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                _allProfessorData.value = AllProfessorState.Success(response.body()!!.professor)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                _allProfessorData.value = AllProfessorState.Error("Failed to fetch professor: ${response.code()} - $errorMsg")
            }
        } catch (e: Exception) {
            _allProfessorData.value = AllProfessorState.Error("Error: ${e.message}")
        }
    }

    fun fetchStudentAttendance(name: String, rollno: String){
        _studentAttendanceByCourseData.value = StudentAttendanceByCourseState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if(token != null){
                fetchStudentAttendanceFromApi(name, rollno, token)
            } else{
                _studentAttendanceByCourseData.value = StudentAttendanceByCourseState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchStudentAttendanceFromApi(name: String, rollno: String, token: String){
        try {
            val response: Response<StudentAttendanceByCourse> =
                appApi.getStudentAttendance("Bearer $token", name, rollno)
            Log.d("AdminViewStudentAttendance", "Api call response ${response}")
            if(response.isSuccessful){
                _studentAttendanceByCourseData.value = StudentAttendanceByCourseState.Success(response.body()!!)
            } else{
                Log.d("AdminViewStudentAttendance", "${response.errorBody()}")
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                _studentAttendanceByCourseData.value = StudentAttendanceByCourseState.Error("Failed to fetch attendance: ${response.code()} - $errorMsg")
            }
        } catch(e: Exception){
            Log.d("AdminViewStudentAttendance", "Exception: ${e.message}")
            _studentAttendanceByCourseData.value = StudentAttendanceByCourseState.Error("Error: ${e.message}")
        }
    }

    sealed class StudentRegisterCsvState {
        data object Loading : StudentRegisterCsvState()
        data class Success(val data: StudentRegisterCsvResponse) : StudentRegisterCsvState()
        data class PartialSuccess(val data: StudentRegisterCsvResponse, val message: String) : StudentRegisterCsvState()
        data class Error(val message: String) : StudentRegisterCsvState()
    }

    sealed class AdminViewCurrrentCourseState {
        data object Loading : AdminViewCurrrentCourseState()
        data class Success(val data: List<com.example.practice.ResponsesModel.Course>) : AdminViewCurrrentCourseState()
        data class Error(val message: String) : AdminViewCurrrentCourseState()
    }

    sealed class CreateStudentAccountState {
        object Idle : CreateStudentAccountState()
        object Loading : CreateStudentAccountState()
        data class Success(val data: Student) : CreateStudentAccountState()
        data class Error(val message: String) : CreateStudentAccountState()
    }

    sealed class CourseStudentsState {
        data object Loading : CourseStudentsState()
        data class Success(val students: List<Student>) : CourseStudentsState()
        data class Error(val message: String) : CourseStudentsState()
    }

    sealed class FullAttendanceDataState {
        data object Loading : FullAttendanceDataState()
        data class Success(val data: List<AttendanceRecord>): FullAttendanceDataState()
        data class Error(val message: String) : FullAttendanceDataState()
    }

    sealed class ViewAllAttendanceRecordsState {
        data object Loading : ViewAllAttendanceRecordsState()
        data class Success(val data: List<RecordXX>): ViewAllAttendanceRecordsState()
        data class Error(val message: String) : ViewAllAttendanceRecordsState()
    }

    sealed class ViewRecordDataState {
        data object Idle : ViewRecordDataState()
        data object Loading : ViewRecordDataState()
        data class Success(val data: List<AttendanceXX>): ViewRecordDataState()
        data class Error(val message: String) : ViewRecordDataState()
    }

    sealed class AllStudentsState {
        data object Loading : AllStudentsState()
        data class Success(val students: List<Student>) : AllStudentsState()
        data class Error(val message: String) : AllStudentsState()
    }

    sealed class AllProfessorState {
        data object Loading : AllProfessorState()
        data class Success(val professor: List<Professor>) : AllProfessorState()
        data class Error(val message: String) : AllProfessorState()
    }

    sealed class StudentAttendanceByCourseState{
        data object Loading : StudentAttendanceByCourseState()
        data class Success(val data: StudentAttendanceByCourse) : StudentAttendanceByCourseState()
        data class Error(val message: String) : StudentAttendanceByCourseState()
    }

    // ── New Sealed Classes for Dashboard Redesign ──

    sealed class DashboardStatsState {
        object Loading : DashboardStatsState()
        data class Success(val data: DashboardStatsResponse) : DashboardStatsState()
        data class Error(val message: String) : DashboardStatsState()
    }

    sealed class DetailedStudentsState {
        object Loading : DetailedStudentsState()
        data class Success(val data: DetailedStudentsResponse) : DetailedStudentsState()
        data class Error(val message: String) : DetailedStudentsState()
    }

    sealed class DeleteStudentState {
        object Idle : DeleteStudentState()
        object Loading : DeleteStudentState()
        object Success : DeleteStudentState()
        data class Error(val message: String) : DeleteStudentState()
    }

    sealed class AuditLogsState {
        object Loading : AuditLogsState()
        data class Success(val data: com.example.practice.ResponsesModel.AuditLogsResponse) : AuditLogsState()
        data class Error(val message: String) : AuditLogsState()
    }

    sealed class SecurityStatsState {
        object Loading : SecurityStatsState()
        data class Success(val data: com.example.practice.ResponsesModel.SecurityStatsResponse) : SecurityStatsState()
        data class Error(val message: String) : SecurityStatsState()
    }

    sealed class SearchStudentState {
        object Loading : SearchStudentState()
        data class Success(val data: com.example.practice.ResponsesModel.SearchStudentResponse) : SearchStudentState()
        data class Error(val message: String) : SearchStudentState()
    }
}