package com.example.practice.ResponsesModel

data class AuditLogsResponse(
    val logs: List<AuditLog> = emptyList(),
    val total: Int = 0,
    val totalPages: Int = 0,
    val page: Int = 1
)

data class AuditLog(
    val id: Int = 0,
    val userId: Int = 0,
    val action: String = "",
    val role: String = "",
    val courseId: Int? = null,
    val details: Any? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val timestamp: String = "",
    val status: String = ""
)

data class SecurityStatsResponse(
    val faceMismatchCount: Int,
    val replayAttempts: Int,
    val geofenceFailures: Int,
    val tokenInvalid: Int,
    val timingRejected: Int,
    val recentEvents: List<AuditLog>
)
