package com.example.practice.ResponsesModel

data class SearchStudentResponse(
    val students: List<SearchStudent>
)

data class SearchStudent(
    val id: Int,
    val name: String,
    val rollno: Int,
    val email: String,
    val branch: String? = null,
    val section: String? = null,
    val profilePicture: String? = null,
    val attendancePercentage: Int? = null,
    val totalClasses: Int = 0,
    val presentCount: Int = 0,
    val hasRegisteredFace: Boolean = false,
    val courseAttendance: List<CourseAttendance>? = null
)

data class CourseAttendance(
    val courseId: Int,
    val courseName: String,
    val year: Int,
    val batch: String,
    val present: Int,
    val total: Int,
    val percentage: Int? = null
)
