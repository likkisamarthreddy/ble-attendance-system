package com.example.practice.ProfessorApp

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practice.RequestBodyApi.CourseInfo
import com.example.practice.RequestBodyApi.CreateCourseRequest
import com.example.practice.RequestBodyApi.EndSessionRequest
import com.example.practice.RequestBodyApi.MarkManualAttendanceRequest
import com.example.practice.RequestBodyApi.ModifyAttendanceRequest
import com.example.practice.ResponsesModel.AllStudentsData
import com.example.practice.ResponsesModel.AttendanceRecord
import com.example.practice.ResponsesModel.AttendanceXX
import com.example.practice.ResponsesModel.CourseStudentView
import com.example.practice.ResponsesModel.FetchAllAttendanceRecord
import com.example.practice.ResponsesModel.GetProfessorProfileResponse
import com.example.practice.ResponsesModel.GetRecordDataResponse
import com.example.practice.ResponsesModel.MarkManualAttendanceResponse
import com.example.practice.ResponsesModel.MessageResponse
import com.example.practice.ResponsesModel.RecordXX
import com.example.practice.ResponsesModel.Student
import com.example.practice.ResponsesModel.ViewAllAttendanceRecords
import com.example.practice.ResponsesModel.toDomain
import com.example.practice.ResponsesModel.ViewCourses
import com.example.practice.api.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ProfessorViewModel : ViewModel() {

    private val appApi = RetrofitInstance.professorApi

    private val _professorData = MutableLiveData<ProfessorState>()
    val professorData: LiveData<ProfessorState> = _professorData

    private val _courseTitle = MutableLiveData<String>("")
    val courseTitle: LiveData<String> = _courseTitle

    private val _isArchivedSelected = MutableLiveData<Boolean>(false)
    val isArchivedSelected: LiveData<Boolean> = _isArchivedSelected

    private val _batchName = MutableLiveData<String>("")
    val batchName: LiveData<String> = _batchName

    private val _joiningCode = MutableLiveData<String>("")
    val joiningCode: LiveData<String> = _joiningCode

    private val _courseYear = MutableLiveData<Int>()
    val courseYear: LiveData<Int> = _courseYear

    private val _courseExpiry = MutableLiveData<String>("")
    val courseExpiry: LiveData<String> = _courseExpiry

    private val _viewAllAttendanceRecords = MutableLiveData<ViewAllAttendanceRecordsState>()
    val viewAllAttendanceRecords: LiveData<ViewAllAttendanceRecordsState> = _viewAllAttendanceRecords

    private val _viewRecordData = MutableLiveData<ViewRecordDataState>()
    val viewRecordData: LiveData<ViewRecordDataState> = _viewRecordData

    private val _fullAttendanceData = MutableLiveData<FullAttendanceDataState>()
    val fullAttendanceData: LiveData<FullAttendanceDataState> = _fullAttendanceData

    private val _courseStudentsData = MutableLiveData<CourseStudentsState>()
    val courseStudentsData: LiveData<CourseStudentsState> = _courseStudentsData

    private val _AllStudentsData = MutableLiveData<AllStudentsState>()
    val AllStudentsData: LiveData<AllStudentsState> = _AllStudentsData

    private val _createCourseState = MutableLiveData<CreateCourseState>(CreateCourseState.Idle)
    val createCourseState: LiveData<CreateCourseState> = _createCourseState

    private val _createAttendanceState = MutableLiveData<CreateAttendanceState>(CreateAttendanceState.Idle)
    val createAttendanceState: LiveData<CreateAttendanceState> = _createAttendanceState

    private val _markManualAttendanceState = MutableLiveData<MarkManualAttendanceState>(MarkManualAttendanceState.Idle)
    val markManualAttendanceState: LiveData<MarkManualAttendanceState> = _markManualAttendanceState

    private val _modifyAttendance = MutableLiveData<ModifyAttendanceState>(ModifyAttendanceState.Idle)
    val modifyAttendanceState: LiveData<ModifyAttendanceState> = _modifyAttendance

    private val _modifyStudents = MutableLiveData<ModifyStudentsState>(ModifyStudentsState.Idle)
    val modifyStudents: LiveData<ModifyStudentsState> = _modifyStudents

    private val _modifyLiveAttendance = MutableLiveData<ModifyLiveAttendanceState>(ModifyLiveAttendanceState.Idle)
    val modifyLiveAttendanceState: LiveData<ModifyLiveAttendanceState> = _modifyLiveAttendance

    private val _currentRecords = MutableLiveData<List<AttendanceXX>>()
    val currentRecords: LiveData<List<AttendanceXX>> = _currentRecords

    private val _professorProfileDetails = MutableLiveData<ProfessorProfileDeatilsState>()
    val professorProfileDetails: LiveData<ProfessorProfileDeatilsState> = _professorProfileDetails

    private val _endSessionState = MutableLiveData<EndSessionState>(EndSessionState.Idle)
    val endSessionState: LiveData<EndSessionState> = _endSessionState

    fun selectCourse(course: String, batch: String, courseExpiry: String, year: Int, joiningCode: String) {
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

    fun ArchiveSection(){
        _isArchivedSelected.value = !(_isArchivedSelected.value ?: false)
    }

    /** Fetch Firebase Token (Now a Suspend Function) */
    private suspend fun getFirebaseToken(): String? {
        return try {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            firebaseUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }

    private fun resetModifyAttendanceState() {
        _modifyAttendance.value = ModifyAttendanceState.Idle
    }

    fun modifyStudentsAttendance(rollno: List<Int>, recordId: String) {
        _modifyAttendance.value = ModifyAttendanceState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                modifyStudentsAttendanceFromApi(rollno, recordId, token)
            } else {
                _modifyAttendance.value = ModifyAttendanceState.Error("Failed to get Firebase token")
                kotlinx.coroutines.delay(500)
                resetModifyAttendanceState()
            }
        }
    }

    private suspend fun modifyStudentsAttendanceFromApi(rollno: List<Int>, recordId: String, token: String) {
        val request = ModifyAttendanceRequest(rollno, recordId)

        try {
            val response: Response<MessageResponse> = appApi.modifyAttendance("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                _modifyAttendance.value = ModifyAttendanceState.Success(response.body()!!)

                // After a successful update, refresh the record data
                // Small delay to ensure the backend has processed the changes
                kotlinx.coroutines.delay(300)
                fetchRecordData(recordId, true)

                // Reset the state after a delay to avoid UI issues
                kotlinx.coroutines.delay(1000)
                resetModifyAttendanceState()
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                _modifyAttendance.value = ModifyAttendanceState.Error("Failed to update attendance: ${response.code()} - $errorMsg")

                // Reset the state after showing error
                kotlinx.coroutines.delay(3000)
                resetModifyAttendanceState()
            }
        } catch (e: Exception) {
            _modifyAttendance.value = ModifyAttendanceState.Error("Network error: ${e.message}")

            // Reset the state after showing error
            kotlinx.coroutines.delay(3000)
            resetModifyAttendanceState()
        }
    }

    fun modifyLiveAttendace(rollno: List<Int>, recordId: String){
        _modifyLiveAttendance.value = ModifyLiveAttendanceState.Loading

        viewModelScope.launch{
            val token = getFirebaseToken()
            if(token != null){
                modifyLiveAttendanceFromApi(rollno, recordId, token)
            } else{
                _modifyLiveAttendance.value = ModifyLiveAttendanceState.Error("Failed to update Attendance")
                kotlinx.coroutines.delay(500)
                resetModifyLiveAttendanceState()
            }
        }
    }

    private suspend fun modifyLiveAttendanceFromApi(rollno: List<Int>, recordId: String, token: String){
        val request = ModifyAttendanceRequest(rollno, recordId)
        try {
            val response: Response<MessageResponse> = appApi.modifyLiveAttendance("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                _modifyLiveAttendance.value = ModifyLiveAttendanceState.Success(response.body()!!)

                // After a successful update, refresh the record data
                // Small delay to ensure the backend has processed the changes
                kotlinx.coroutines.delay(300)
                fetchRecordData(recordId, false)

                // Reset the state after a delay to avoid UI issues
                kotlinx.coroutines.delay(1000)
                resetModifyLiveAttendanceState()
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                _modifyLiveAttendance.value = ModifyLiveAttendanceState.Error("Failed to update attendance: ${response.code()} - $errorMsg")

                // Reset the state after showing error
                kotlinx.coroutines.delay(3000)
                resetModifyLiveAttendanceState()
            }
        } catch (e: Exception) {
            _modifyLiveAttendance.value = ModifyLiveAttendanceState.Error("Network error: ${e.message}")

            // Reset the state after showing error
            kotlinx.coroutines.delay(3000)
            resetModifyLiveAttendanceState()
        }

    }

    fun resetModifyLiveAttendanceState(){
        _modifyLiveAttendance.value = ModifyLiveAttendanceState.Idle
    }

    /** Fetches all courses assigned to the professor */
    fun fetchProfessorCourses(isArchivedSelected: Boolean) {
        _professorData.value = ProfessorState.Loading

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
                _professorData.value = ProfessorState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchArchivedCoursesFromApi(token: String) {
        when (val result = com.example.practice.utils.safeApiCall { appApi.getProfessorArchivedCourseData("Bearer $token") }) {
            is com.example.practice.utils.ApiResult.Success -> {
                try {
                    val mappedCourses = result.data.courses.map { it.toDomain() }
                    _professorData.value = ProfessorState.Success(mappedCourses)
                } catch (e: Exception) {
                    _professorData.value = ProfessorState.Error("Mapping error: ${e.message}")
                }
            }
            is com.example.practice.utils.ApiResult.Error -> {
                _professorData.value = ProfessorState.Error(result.message)
            }
        }
    }

    private suspend fun fetchCurrentCoursesFromApi(token: String) {
        when (val result = com.example.practice.utils.safeApiCall { appApi.getProfessorCurrentCourseData("Bearer $token") }) {
            is com.example.practice.utils.ApiResult.Success -> {
                try {
                    val mappedCourses = result.data.courses.map { it.toDomain() }
                    _professorData.value = ProfessorState.Success(mappedCourses)
                } catch (e: Exception) {
                    _professorData.value = ProfessorState.Error("Mapping error: ${e.message}")
                }
            }
            is com.example.practice.utils.ApiResult.Error -> {
                _professorData.value = ProfessorState.Error(result.message)
            }
        }
    }

    fun setCurrentRecords(records: List<AttendanceXX>) {
        _currentRecords.value = records
    }

    /** Fetches all Attendance Record present given course */
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
    ): android.net.Uri? {
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
                val dateColumns = attendanceData.mapIndexed { index, record ->
                    Pair(record.date, index)
                }

                Log.d("ExcelProfessorView", "Date Columns: $dateColumns")
                Log.d("ExcelProfessorView", "Student Roll Numbers: $studentRollNumbers")

                // Create Header Row: "Student Roll No" + Each Attendance Record's Date
                val headerRow = sheet.createRow(rowIndex++)
                headerRow.createCell(0).setCellValue("Student Roll No")

                dateColumns.forEachIndexed { colIndex, (date, _) ->
                    headerRow.createCell(colIndex + 1).setCellValue(date)
                }

                // Populate Attendance Data for Each Student
                studentRollNumbers.forEach { rollNo ->
                    val row = sheet.createRow(rowIndex++)
                    row.createCell(0).setCellValue(rollNo)

                    dateColumns.forEachIndexed { colIndex, (date, originalIndex) ->
                        val record = attendanceData[originalIndex]
                        val status = if (record.date == date) {
                            record.attendance[rollNo] ?: "Absent"
                        } else {
                            "Absent"
                        }
                        row.createCell(colIndex + 1).setCellValue(status)
                    }
                }

                // Save to public Downloads folder via MediaStore (Android Q+)
                val fileName = "Attendance_${courseName}_${courseBatch}_${System.currentTimeMillis()}.xlsx"
                val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType)
                        put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { workbook.write(it) }
                        workbook.close()
                        Log.d("ExcelProfessorView", "Excel saved to Downloads via MediaStore: $uri")
                        return@withContext uri
                    } else {
                        workbook.close()
                        Log.e("ExcelError", "MediaStore insert returned null")
                        return@withContext null
                    }
                } else {
                    // Fallback for older devices
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { workbook.write(it) }
                    workbook.close()
                    Log.d("ExcelProfessorView", "Excel saved to Downloads: ${file.absolutePath}")
                    return@withContext android.net.Uri.fromFile(file)
                }
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

    fun resetViewRecordData() {
        _viewRecordData.value = ViewRecordDataState.Idle
    }

    /** Fetch students in a specific course */
    fun fetchStudentsInCourse(joiningCode: String,  batch: String, courseName: String, isArchived: Boolean, year: Int) {
        _courseStudentsData.value = CourseStudentsState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                fetchStudentsFromApi(token, batch, courseName, isArchived, year, joiningCode)
            } else {
                _courseStudentsData.value = CourseStudentsState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchStudentsFromApi(token: String, batch: String, courseName: String, isArchived: Boolean, year: Int, joiningCode: String) {

        try {
            val response: Response<CourseStudentView> =
                appApi.getStudentsInCourse("Bearer $token", batch, courseName, isArchived, year, joiningCode)

            if (response.isSuccessful && response.body() != null) {
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

    fun fetchAllStudents(){
        _courseStudentsData.value = CourseStudentsState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                fetchAllStudentsFromApi(token)
            } else {
                _AllStudentsData.value = AllStudentsState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchAllStudentsFromApi(token: String) {
        when (val result = com.example.practice.utils.safeApiCall { appApi.getAllStudentsData("Bearer $token") }) {
            is com.example.practice.utils.ApiResult.Success -> {
                try {
                    val mappedStudents = result.data.student.map { it.toDomain() }
                    _AllStudentsData.value = AllStudentsState.Success(mappedStudents)
                } catch (e: Exception) {
                    _AllStudentsData.value = AllStudentsState.Error("Mapping error: ${e.message}")
                }
            }
            is com.example.practice.utils.ApiResult.Error -> {
                _AllStudentsData.value = AllStudentsState.Error(result.message)
            }
        }
    }

    fun createCourse(courseName: String, courseBatch: String, courseExpiry: String, year: Int) {
        if (courseBatch.isEmpty() || courseName.isEmpty() || courseExpiry.isEmpty()) {
            _createCourseState.value = CreateCourseState.Error("Fields can't be empty")
            resetCreateCourseState()
            return
        }

        _createCourseState.value = CreateCourseState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                createCourseFromApi(token, courseBatch, courseName, courseExpiry, year)
            } else {
                _createCourseState.value = CreateCourseState.Error("Failed to get Firebase token")
                resetCreateCourseState()
            }
        }
    }

    private suspend fun createCourseFromApi(token: String, batch: String, courseName: String, courseExpiry: String, year: Int) {
        val request = CreateCourseRequest(
            name = courseName,
            batch = batch,
            year = year,
            courseExpiry = courseExpiry
        )

        try {
            val response = appApi.createCourse(request, "Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                _createCourseState.value = CreateCourseState.Success
                // Refresh the course list
                fetchProfessorCourses(false)
                kotlinx.coroutines.delay(500)
                resetCreateCourseState()
            } else {
                _createCourseState.value = CreateCourseState.Error(
                    response.errorBody()?.string() ?: "Failed to create course"
                )
            }
        } catch (e: Exception) {
            _createCourseState.value = CreateCourseState.Error(
                e.message ?: "Unknown error occurred"
            )
        }
    }

    // Reset the create course state
    fun resetCreateCourseState() {
        _createCourseState.value = CreateCourseState.Idle
    }

//    fun modifyStudents(courseName: String, courseBatch: String, students: List<Int>, year: Int) {
//        if (courseBatch.isEmpty() || courseName.isEmpty()) {
//            _modifyStudents.value = ModifyStudentsState.Error("Fields can't be empty")
//            resetModifyStudentsState()
//            return
//        }
//
//        _modifyStudents.value = ModifyStudentsState.Loading
//
//        viewModelScope.launch {
//            modifyStudentsFromApi(courseBatch, courseName, students, year)
//        }
//    }

//    private suspend fun modifyStudentsFromApi(batch: String, courseName: String, students: List<Int>, year: Int) {
//        Log.d("ModifyStudentsInCourse", "Checking in function: $students")
//        val request = ModifyStudentsInCourse(courseName, batch, students)
//
//        try {
//            val response = appApi.modifyStudentsInCourse(request)
//
//            if (response.isSuccessful && response.body() != null) {
//                _modifyStudents.value = ModifyStudentsState.Success
//                // Refresh the course list
//                kotlinx.coroutines.delay(500)
//                fetchStudentsInCourse(batch, courseName, false, year)
//                kotlinx.coroutines.delay(500)
//                resetModifyStudentsState()
//            } else {
//                _modifyStudents.value = ModifyStudentsState.Error(
//                    response.errorBody()?.string() ?: "Failed to modify students"
//                )
//            }
//        } catch (e: Exception) {
//            _modifyStudents.value = ModifyStudentsState.Error(
//                e.message ?: "Unknown error occurred"
//            )
//        }
//    }

    // Reset the create course state
//    fun resetModifyStudentsState() {
//        _modifyStudents.value = ModifyStudentsState.Idle
//    }

    fun createAttendance(courseName: String, courseBatch: String, courseExpiry: String, joiningCode: String) {
        if (courseBatch.isEmpty() || courseName.isEmpty() || courseExpiry.isEmpty()) {
            _createAttendanceState.value = CreateAttendanceState.Error("Fields can't be empty")
            return
        }

        _createAttendanceState.value = CreateAttendanceState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if(token != null){
                createAttendanceFromApi(token, courseBatch, courseName, courseExpiry, joiningCode)
            } else{
                _createAttendanceState.value = CreateAttendanceState.Error("Failed to get Firebase token")
                resetCreateCourseState()
            }

        }
    }

    private suspend fun createAttendanceFromApi(token: String, batch: String, courseName: String, courseExpiry: String, joiningCode: String) {
        val request = CourseInfo(courseName, batch, courseExpiry, joiningCode)
        try {
            val response = appApi.createAttendance("Bearer $token", request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val attendanceId = body.record._id ?: body.record.id?.toString() ?: ""
                val sessionSecret = body.sessionSecret ?: ""
                Log.d("ProfessorViewModel", "Attendance created with ID: $attendanceId")
                _createAttendanceState.value = CreateAttendanceState.Success(attendanceId, sessionSecret)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to create Attendance"
                Log.e("ProfessorViewModel", "API error: $errorMsg")
                _createAttendanceState.value = CreateAttendanceState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("ProfessorViewModel", "Exception: ${e.message}")
            _createAttendanceState.value = CreateAttendanceState.Error(e.message ?: "Unknown error")
        }
    }

    fun endSession(recordId: String) {
        _endSessionState.value = EndSessionState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                endSessionFromApi(token, recordId)
            } else {
                _endSessionState.value = EndSessionState.Error("Failed to get Firebase token")
                resetEndSessionState()
            }
        }
    }

    private suspend fun endSessionFromApi(token: String, recordId: String) {
        val request = EndSessionRequest(recordId)
        try {
            val response = appApi.endSession("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                _endSessionState.value = EndSessionState.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to end session"
                _endSessionState.value = EndSessionState.Error(errorMsg)
            }
        } catch (e: Exception) {
            _endSessionState.value = EndSessionState.Error(e.message ?: "Unknown error")
        }
    }

    fun resetEndSessionState() {
        _endSessionState.value = EndSessionState.Idle
    }

    fun fetchProfessorProfileDetails(){
        _professorProfileDetails.value = ProfessorProfileDeatilsState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                fetchProfessorProfileDetailsFromApi(token)
            } else {
                _professorProfileDetails.value = ProfessorProfileDeatilsState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchProfessorProfileDetailsFromApi(token: String){
        try {
            val response: Response<GetProfessorProfileResponse> =
                appApi.getProfessorProfile("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Log.d("ProfessorViewModel", "${response.body()!!}")
                _professorProfileDetails.value = ProfessorProfileDeatilsState.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                _professorProfileDetails.value = ProfessorProfileDeatilsState.Error("Failed to fetch students: ${response.code()} - $errorMsg")
            }
        } catch (e: Exception) {
            _professorProfileDetails.value = ProfessorProfileDeatilsState.Error("Error: ${e.message}")
        }
    }

    fun resetCreateAttendanceState() {
        _createAttendanceState.value = CreateAttendanceState.Idle
        Log.d("ProfessorViewModel", "Reset state to Idle")
    }

    fun MarkManualAttendance(uid: String, students: List<String>) {
        if (uid.isEmpty()) {
            _markManualAttendanceState.value = MarkManualAttendanceState.Error("Fields can't be empty")
            return
        }
        _markManualAttendanceState.value = MarkManualAttendanceState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token != null) {
                MarkManualAttendanceFromApi(token, uid, students)
            } else {
                _markManualAttendanceState.value = MarkManualAttendanceState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun MarkManualAttendanceFromApi(token: String, uid: String, students: List<String>) {
        val request = MarkManualAttendanceRequest(uid, students)
        try {
            val response = appApi.markManualAttendance("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                val attendanceId = response.body()!!
                Log.d("ProfessorViewModel", "Attendance created with ID: $attendanceId")
                _markManualAttendanceState.value = MarkManualAttendanceState.Success(attendanceId)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to create Attendance"
                Log.e("ProfessorViewModel", "API error: $errorMsg")
                _markManualAttendanceState.value = MarkManualAttendanceState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("ProfessorViewModel", "Exception: ${e.message}")
            _markManualAttendanceState.value = MarkManualAttendanceState.Error(e.message ?: "Unknown error")
        }
    }

    fun resetMarkManualAttendanceState() {
        _markManualAttendanceState.value = MarkManualAttendanceState.Idle
        Log.d("ProfessorViewModel", "Reset state to Idle")
    }

    /** State management for professor courses */
    sealed class ProfessorState {
        data object Loading : ProfessorState()
        data class Success(val data: List<com.example.practice.ResponsesModel.Course>) : ProfessorState()
        data class Error(val message: String) : ProfessorState()
    }

    /** State management for students in a course */
    sealed class CourseStudentsState {
        data object Loading : CourseStudentsState()
        data class Success(val students: List<Student>) : CourseStudentsState()
        data class Error(val message: String) : CourseStudentsState()
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

    sealed class FullAttendanceDataState {
        data object Loading : FullAttendanceDataState()
        data class Success(val data: List<AttendanceRecord>): FullAttendanceDataState()
        data class Error(val message: String) : FullAttendanceDataState()
    }

    sealed class AllStudentsState {
        data object Loading : AllStudentsState()
        data class Success(val student: List<Student>) : AllStudentsState()
        data class Error(val message: String) : AllStudentsState()
    }

    sealed class CreateCourseState {
        object Idle : CreateCourseState()
        object Loading : CreateCourseState()
        object Success : CreateCourseState()
        data class Error(val message: String) : CreateCourseState()
    }

    sealed class CreateAttendanceState {
        data object Idle : CreateAttendanceState()
        data object Loading : CreateAttendanceState()
        data class Success(val attendanceId: String, val sessionSecret: String) : CreateAttendanceState()
        data class Error(val message: String) : CreateAttendanceState()
    }

    sealed class ModifyStudentsState {
        object Idle : ModifyStudentsState()
        object Loading : ModifyStudentsState()
        object Success : ModifyStudentsState()
        data class Error(val message: String) : ModifyStudentsState()
    }

    sealed class MarkManualAttendanceState {
        object Idle : MarkManualAttendanceState()
        object Loading : MarkManualAttendanceState()
        data class Success(val attendanceMarked: MarkManualAttendanceResponse) : MarkManualAttendanceState()
        data class Error(val message: String) : MarkManualAttendanceState()
    }

    sealed class ModifyAttendanceState {
        object Idle : ModifyAttendanceState()
        object Loading : ModifyAttendanceState()
        data class Success(val data : MessageResponse) : ModifyAttendanceState()
        data class Error(val message: String): ModifyAttendanceState()
    }

    sealed class ModifyLiveAttendanceState {
        object Idle : ModifyLiveAttendanceState()
        object Loading : ModifyLiveAttendanceState()
        data class Success(val data : MessageResponse) : ModifyLiveAttendanceState()
        data class Error(val message: String): ModifyLiveAttendanceState()
    }

    sealed class ProfessorProfileDeatilsState{
        object Loading : ProfessorProfileDeatilsState()
        data class Success(val data : GetProfessorProfileResponse) : ProfessorProfileDeatilsState()
        data class Error(val message: String): ProfessorProfileDeatilsState()
    }

    sealed class EndSessionState {
        object Idle : EndSessionState()
        object Loading : EndSessionState()
        data class Success(val data : MessageResponse) : EndSessionState()
        data class Error(val message: String): EndSessionState()
    }

}
