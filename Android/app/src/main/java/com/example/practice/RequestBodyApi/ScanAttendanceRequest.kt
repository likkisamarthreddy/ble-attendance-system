package com.example.practice.RequestBodyApi

data class ScanAttendanceRequest(
    val token: String,
    val latitude: Double,
    val longitude: Double,
    val faceEmbedding: List<Float>,
    val deviceId: String,
    val integrityToken: String,
    val auditLog: List<Map<String, Any>>,
    val uid: String // Matches 'uid' in backend (Attendance/Session ID)
)
