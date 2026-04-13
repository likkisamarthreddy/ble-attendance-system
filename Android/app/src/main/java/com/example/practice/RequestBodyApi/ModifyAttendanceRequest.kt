package com.example.practice.RequestBodyApi

data class ModifyAttendanceRequest(
    val rollno: List<Int>,
    val id: String,
)
