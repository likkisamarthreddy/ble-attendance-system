package com.example.practice.ResponsesModel

data class StudentCourseAttendanceResponse(
    val courses: List<CourseWithAttendance>
)

data class CourseWithAttendance(
    val _id: String? = null,
    val id: Int? = null,
    val name: String,
    val batch: String,
    val totalClasses: Int,
    val presentCount: Int,
    val absentCount: Int,
    val percentage: Int
)

