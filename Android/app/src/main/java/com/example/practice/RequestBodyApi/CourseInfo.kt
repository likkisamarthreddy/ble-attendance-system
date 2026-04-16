package com.example.practice.RequestBodyApi

import com.squareup.moshi.Json

data class CourseInfo(
    @Json(name = "courseName") val courseName: String,
    val batch: String,
    val courseExpiry: String,
    val joiningCode: String
)

data class CreateCourseRequest(
    val name: String,
    val batch: String,
    val year: Int,
    val courseExpiry: String? = null
)
