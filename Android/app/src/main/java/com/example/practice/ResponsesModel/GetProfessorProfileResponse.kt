package com.example.practice.ResponsesModel

data class GetProfessorProfileResponse(
    val _id: String? = null,
    val id: Int? = null,
    val name: String,
    val email: String,
    val courses: List<Course>,
    val uid: String,
    val __v: Int? = null,
)

