package com.example.practice.StudentApp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practice.RequestBodyApi.JoinCourseRequest
import com.example.practice.ResponsesModel.StudentCourseAttendanceResponse
import com.example.practice.ResponsesModel.StudentCourseResponse
import com.example.practice.ResponsesModel.StudentProfileResponse
import com.example.practice.ResponsesModel.StudentViewAttendanceResponse
import com.example.practice.api.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.practice.utils.await

class StudentViewModel: ViewModel() {

    private val appApi = RetrofitInstance.studentApi

    private val _studentCoursesData = MutableLiveData<StudentCourseState>()
    val studentCoursesData: LiveData<StudentCourseState> = _studentCoursesData

    private val _studentViewAttendanceData = MutableLiveData<StudentViewAttendanceState>()
    val studentViewAttendanceData: LiveData<StudentViewAttendanceState> = _studentViewAttendanceData

    private val _studentJoiningCodeData = MutableLiveData<StudentJoiningCodeState>()
    val studentJoiningCodeData: LiveData<StudentJoiningCodeState> = _studentJoiningCodeData

    private val _studentProfileDetails = MutableLiveData<StudentProfileDetailsState>()
    val studentProfileDetails: LiveData<StudentProfileDetailsState> = _studentProfileDetails

    private val _coursesWithAttendance = MutableLiveData<CoursesAttendanceState>()
    val coursesWithAttendance: LiveData<CoursesAttendanceState> = _coursesWithAttendance

    /** Fetch Firebase Token (Now a Suspend Function) */
    private suspend fun getFirebaseToken(): String? {
        return try {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            firebaseUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }

    fun fetchStudentCourses() {
        _studentCoursesData.value = StudentCourseState.Loading

        viewModelScope.launch{
            val token = getFirebaseToken()
            if(token != null){
                fetchStudentCoursesFromApi(token)
            } else{
                _studentCoursesData.value = StudentCourseState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchStudentCoursesFromApi(token: String) {
        try {
            val response = appApi.getStudentCourses("Bearer $token")

            if(response.isSuccessful && response.body() != null){
                _studentCoursesData.value = StudentCourseState.Success(response.body()!!)
            } else{
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                _studentCoursesData.value = StudentCourseState.Error("Failed to fetch courses: ${response.code()} - $errorMsg")
            }
        } catch(e: Exception){
            _studentCoursesData.value = StudentCourseState.Error("Error: ${e.message}")
        }
    }

    fun fetchStudentAttendance(courseName: String, batch: String){
        _studentViewAttendanceData.value = StudentViewAttendanceState.Loading

        viewModelScope.launch{
            val token = getFirebaseToken()
            if(token != null){
                fetchStudentAttendanceFromApi(token, courseName, batch)
            } else{
                _studentViewAttendanceData.value = StudentViewAttendanceState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchStudentAttendanceFromApi(token: String, courseName: String, batch: String) {
        try{
            val response = appApi.getStudentAttendance("Bearer $token", batch, courseName)

            if(response.isSuccessful && response.body() != null){
                _studentViewAttendanceData.value = StudentViewAttendanceState.Success(response.body()!!)
            } else{
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                _studentViewAttendanceData.value = StudentViewAttendanceState.Error("Failed to fetch courses: ${response.code()} - $errorMsg")
            }

        } catch(e: Exception){
            _studentViewAttendanceData.value = StudentViewAttendanceState.Error("Error: ${e.message}")
        }
    }

    fun studentJoinCourse(joiningCode: String){
        _studentJoiningCodeData.value = StudentJoiningCodeState.Loading

        viewModelScope.launch{
            val token = getFirebaseToken()
            if(token != null){
                studentJoinCourseFromApi(token, joiningCode)
            } else{
                _studentJoiningCodeData.value = StudentJoiningCodeState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun studentJoinCourseFromApi(token: String, joiningCode: String){
        val request = JoinCourseRequest(joiningCode)
        try{
            val response = appApi.StudentJoiningCode("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                _studentJoiningCodeData.value = StudentJoiningCodeState.Success(response.body()!!.message)
                // Refresh the course list
                fetchStudentCourses()
                kotlinx.coroutines.delay(500)
            } else {
                _studentJoiningCodeData.value = StudentJoiningCodeState.Error(
                    response.errorBody()?.string() ?: "Failed to create course"
                )
            }

        } catch(e: Exception){
            _studentJoiningCodeData.value = StudentJoiningCodeState.Error("Error: ${e.message}")
        }
    }

    fun resetJoiningCodeState() {
        _studentJoiningCodeData.value = StudentJoiningCodeState.Idle
    }

    fun fetchStudentProfileDetails(){
        _studentProfileDetails.value = StudentProfileDetailsState.Loading

        viewModelScope.launch {
            val token = getFirebaseToken()
            if(token != null){
                fetchStudentProfileDetailsFromApi(token)
            } else{
                _studentProfileDetails.value = StudentProfileDetailsState.Error("Failed to get Firebase token")
            }
        }
    }

    private suspend fun fetchStudentProfileDetailsFromApi(token: String){
        try{
            val response = appApi.getStudentProfile("Bearer $token")
            if(response.isSuccessful && response.body() != null){
                _studentProfileDetails.value = StudentProfileDetailsState.Success(response.body()!!)
            } else{
                val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                _studentProfileDetails.value = StudentProfileDetailsState.Error("Failed to fetch courses: ${response.code()} - $errorMsg")
            }
        } catch (e: Exception){
            _studentProfileDetails.value = StudentProfileDetailsState.Error("Error: ${e.message}")
        }
    }

    fun markFaceRegisteredLocally() {
        val currentState = _studentProfileDetails.value
        if (currentState is StudentProfileDetailsState.Success) {
            val updatedProfile = currentState.data.copy(hasRegisteredFace = true)
            _studentProfileDetails.value = StudentProfileDetailsState.Success(updatedProfile)
        }
    }

    // ── Face Registration: send embedding to backend ──
    private val _faceRegisterState = MutableLiveData<FaceRegisterState>()
    val faceRegisterState: LiveData<FaceRegisterState> = _faceRegisterState

    fun registerFace(embeddings: List<FloatArray>, profilePicture: String? = null) {
        _faceRegisterState.value = FaceRegisterState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token == null) {
                _faceRegisterState.value = FaceRegisterState.Error("Failed to get Firebase token")
                return@launch
            }
            try {
                val request = com.example.practice.RequestBodyApi.FaceMultiEmbeddingRequest(
                    faceEmbeddings = embeddings.map { it.toList() },
                    profilePicture = profilePicture
                )
                val response = appApi.registerFaceEmbedding("Bearer $token", request)
                if (response.isSuccessful) {
                    markFaceRegisteredLocally()
                    _faceRegisterState.value = FaceRegisterState.Success
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                    _faceRegisterState.value = FaceRegisterState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _faceRegisterState.value = FaceRegisterState.Error("Error: ${e.message}")
            }
        }
    }

    // ── Face Verification: compare embedding with DB ──
    private val _faceVerifyState = MutableLiveData<FaceVerifyState>()
    val faceVerifyState: LiveData<FaceVerifyState> = _faceVerifyState

    fun verifyFace(embedding: FloatArray) {
        _faceVerifyState.value = FaceVerifyState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token == null) {
                _faceVerifyState.value = FaceVerifyState.Error("Failed to get Firebase token")
                return@launch
            }
            try {
                val request = com.example.practice.RequestBodyApi.FaceEmbeddingRequest(embedding.toList())
                val response = appApi.verifyFaceEmbedding("Bearer $token", request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.verified) {
                        _faceVerifyState.value = FaceVerifyState.Success(body.similarity)
                    } else {
                        _faceVerifyState.value = FaceVerifyState.Failed(body.similarity, body.threshold)
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Verification failed"
                    _faceVerifyState.value = FaceVerifyState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _faceVerifyState.value = FaceVerifyState.Error("Error: ${e.message}")
            }
        }
    }

    fun resetFaceVerifyState() { _faceVerifyState.value = FaceVerifyState.Idle }
    fun resetFaceRegisterState() { _faceRegisterState.value = FaceRegisterState.Idle }

    sealed class FaceRegisterState {
        object Idle : FaceRegisterState()
        object Loading : FaceRegisterState()
        object Success : FaceRegisterState()
        data class Error(val message: String) : FaceRegisterState()
    }

    sealed class FaceVerifyState {
        object Idle : FaceVerifyState()
        object Loading : FaceVerifyState()
        data class Success(val similarity: Float) : FaceVerifyState()
        data class Failed(val similarity: Float, val threshold: Float) : FaceVerifyState()
        data class Error(val message: String) : FaceVerifyState()
    }

    sealed class StudentCourseState{
        object Loading : StudentCourseState()
        data class Success(val data: StudentCourseResponse) : StudentCourseState()
        data class Error(val message: String) : StudentCourseState()
    }

    sealed class StudentViewAttendanceState{
        object Loading : StudentViewAttendanceState()
        data class Success(val data: StudentViewAttendanceResponse) : StudentViewAttendanceState()
        data class Error(val message: String) : StudentViewAttendanceState()
    }

    sealed class StudentJoiningCodeState{
        object Idle : StudentJoiningCodeState()
        object Loading: StudentJoiningCodeState()
        data class Success(val data: String) : StudentJoiningCodeState()
        data class Error(val message: String): StudentJoiningCodeState()
    }

    sealed class StudentProfileDetailsState{
        object Loading: StudentProfileDetailsState()
        data class Success(val data: StudentProfileResponse) : StudentProfileDetailsState()
        data class Error(val message: String) : StudentProfileDetailsState()
    }

    fun fetchCoursesWithAttendance() {
        _coursesWithAttendance.value = CoursesAttendanceState.Loading
        viewModelScope.launch {
            val token = getFirebaseToken()
            if (token == null) {
                _coursesWithAttendance.value = CoursesAttendanceState.Error("Failed to get token")
                return@launch
            }
            try {
                val response = appApi.getCoursesWithAttendance("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _coursesWithAttendance.value = CoursesAttendanceState.Success(response.body()!!)
                } else {
                    _coursesWithAttendance.value = CoursesAttendanceState.Error("Failed to fetch attendance")
                }
            } catch (e: Exception) {
                _coursesWithAttendance.value = CoursesAttendanceState.Error("Error: ${e.message}")
            }
        }
    }

    sealed class CoursesAttendanceState {
        object Loading : CoursesAttendanceState()
        data class Success(val data: StudentCourseAttendanceResponse) : CoursesAttendanceState()
        data class Error(val message: String) : CoursesAttendanceState()
    }

}