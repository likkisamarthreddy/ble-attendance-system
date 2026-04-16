package com.example.practice.ResponsesModel

data class Course(
    val _id: String,
    val name: String,
    val batch: String,
    val year: Int,
    val professor: String,
    val students: List<String>,
    val courseStatus: String,
    val courseExpiry: String,
    val joiningCode: String
)
