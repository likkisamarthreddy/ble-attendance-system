package com.example.practice.ResponsesModel

data class FetchAllAttendanceRecord(
    val attendanceSheet: List<AttendanceRecord>
)

data class AttendanceRecord(
    val date: String,
    val attendance: Map<String, String> // Roll number as key, status as value
)

