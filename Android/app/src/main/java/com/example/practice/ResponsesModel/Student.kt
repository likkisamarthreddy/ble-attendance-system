package com.example.practice.ResponsesModel

data class Student(
    val _id: String,
    val id: Int?, // Retained nullable since the API might vary on this field without consequence to DB mapping
    val name: String,
    val rollno: Int,
    val email: String,
    val courses: List<String>,
    val uid: String,
    val batch: List<Any>,
    val attendancePercentage: String
)
