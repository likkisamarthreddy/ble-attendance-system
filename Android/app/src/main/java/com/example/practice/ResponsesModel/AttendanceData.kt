package com.example.practice.ResponsesModel

data class AttendanceData(
    val batch: String,
    val course: String,
    val courseYear: Int,
    val presentCount: Int,
    val totalTaken: Int
)

