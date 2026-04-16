package com.example.practice.ResponsesModel

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DashboardStatsDto(
    val totalStudents: Int? = null,
    val totalProfessors: Int? = null,
    val totalCourses: Int? = null,
    val overallAttendancePercentage: Double? = null,
    val criticalCount: Int? = null,
    val warningCount: Int? = null
)

fun DashboardStatsDto.toDomain(): DashboardStatsResponse {
    return DashboardStatsResponse(
        totalStudents = this.totalStudents ?: 0,
        totalProfessors = this.totalProfessors ?: 0,
        totalCourses = this.totalCourses ?: 0,
        overallAttendancePercentage = this.overallAttendancePercentage ?: 0.0,
        criticalCount = this.criticalCount ?: 0,
        warningCount = this.warningCount ?: 0
    )
}
