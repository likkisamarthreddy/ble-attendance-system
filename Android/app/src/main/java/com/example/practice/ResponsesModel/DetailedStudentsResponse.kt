package com.example.practice.ResponsesModel

data class DetailedStudentsResponse(
    val students: List<DetailedStudent>
)

data class DetailedStudent(
    val id: Int,
    val name: String,
    val rollno: Int,
    val email: String,
    val uid: String,
    val batch: List<String> = emptyList(),
    val branch: String? = null,
    val profilePicture: String? = null,
    val faceRegisteredAt: String? = null,
    val isDisabled: Boolean = false,
    val createdAt: String? = null,
    val attendancePercentage: Int? = null,
    val totalClasses: Int = 0,
    val presentCount: Int = 0,
    val hasRegisteredFace: Boolean = false,
    val courses: List<CourseIdOnly>? = null
)

data class CourseIdOnly(
    val id: Int
)
