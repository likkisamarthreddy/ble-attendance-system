package com.example.practice.ResponsesModel

data class Course(
    val _id: String? = null,
    val id: Int? = null,
    val __v: Int? = null,
    val name: String,
    val batch: String,
    val year: Int,
    val professor: String,
    val students: List<String>,
    val courseStatus: String,
    val courseExpiry: String,
    val joiningCode: String
)

