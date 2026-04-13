package com.example.practice.ResponsesModel

data class DashboardStatsResponse(
    val totalStudents: Int = 0,
    val totalProfessors: Int = 0,
    val totalCourses: Int = 0,
    val overallAttendancePercentage: Double = 0.0,
    val criticalCount: Int = 0,
    val warningCount: Int = 0
)
