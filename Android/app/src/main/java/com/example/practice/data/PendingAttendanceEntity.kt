package com.example.practice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching attendance payloads when the network is unavailable.
 * These records are synced to the backend by [AttendanceSyncWorker] when connectivity is restored.
 */
@Entity(tableName = "pending_attendance")
data class PendingAttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val token: String,              // BLE rolling token
    val latitude: Double,
    val longitude: Double,
    val faceEmbeddingJson: String,  // JSON-serialized List<Float>
    val deviceId: String,
    val integrityToken: String,
    val auditLogJson: String,       // JSON-serialized List<Map<String, Any>>
    val uid: String,                // Attendance record / session ID
    val createdAt: Long = System.currentTimeMillis()
)
