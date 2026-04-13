package com.example.practice.ResponsesModel

data class Student(
    val _id: String? = null,
    val id: Int? = null,
    val name: String,
    val rollno: Int,
    val email: String,
    val courses: List<String>,
    val uid: String,
    val batch: List<Any>,
    val __v: Int? = null,
    val attendancePercentage: String
)

