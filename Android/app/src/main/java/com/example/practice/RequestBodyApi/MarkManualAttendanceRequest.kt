package com.example.practice.RequestBodyApi

data class MarkManualAttendanceRequest(
    val uid: String,
    val students: List<String>
)
