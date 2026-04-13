package com.example.practice.RequestBodyApi

data class ModifyStudentsInCourse(
    val courseName: String,
    val batch: String,
    val rollno: List<Int>,
)
