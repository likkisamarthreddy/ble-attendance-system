package com.example.practice.ResponsesModel

data class CreateAttendanceResponse(
    val record: Record,
    val sessionSecret: String? = null
)

