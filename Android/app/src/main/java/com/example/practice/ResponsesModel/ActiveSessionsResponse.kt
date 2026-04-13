package com.example.practice.ResponsesModel

data class ActiveSessionsResponse(
    val sessions: List<ActiveSession>
)

data class ActiveSession(
    val recordId: String,
    val courseName: String,
    val batch: String,
    val sessionSecret: String
)

