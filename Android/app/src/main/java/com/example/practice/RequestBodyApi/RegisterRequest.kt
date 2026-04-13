package com.example.practice.RequestBodyApi

data class StudentRegisterRequest(
    val name: String,
    val rollno: Int,
    val email: String,
    val uid: String,
    val isProfessor: Boolean
)

data class ProfessorRegisterRequest(
    val name: String,
    val email: String,
    val uid: String
)