package com.example.practice.ResponsesModel

data class StudentRegisterCsvResponse(
    val errors: List<Error>,
    val message: String,
    val success: Int,
    val total: Int
)

