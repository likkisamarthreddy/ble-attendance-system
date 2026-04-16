package com.example.practice.ResponsesModel

data class StudentProfileResponse(
    val __v: Int? = null,
    val _id: String? = null,
    val id: Int? = null,
    val batch: List<String>? = null,
    val courses: List<CourseDto>? = null,
    val email: String,
    val name: String,
    val rollno: Int,
    val uid: String,
    val hasRegisteredFace: Boolean = false
)

