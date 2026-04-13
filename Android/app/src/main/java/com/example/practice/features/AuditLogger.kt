package com.example.practice.features

import android.util.Log

/**
 * Audit Logger — logs every verification event with timestamp, result, and metadata.
 *
 * Events are batched and sent to backend with the attendance request.
 * Backend stores audit trail per student per session for forensic review.
 */
class AuditLogger {

    data class AuditEvent(
        val timestamp: Long,
        val eventType: EventType,
        val result: Boolean,
        val metadata: Map<String, String> = emptyMap()
    )

    enum class EventType {
        // Token events
        TOKEN_SCANNED,
        TOKEN_VALID,
        TOKEN_EXPIRED,
        TOKEN_REPLAY,

        // Face events
        FACE_DETECTED,
        FACE_MATCHED,
        FACE_REJECTED,
        LIVENESS_PASSED,
        LIVENESS_FAILED,

        // Geofence events
        GEOFENCE_IN_RANGE,
        GEOFENCE_OUT_OF_RANGE,
        GEOFENCE_ERROR,

        // RSSI events
        RSSI_OK,
        RSSI_TOO_WEAK,

        // Device integrity events
        INTEGRITY_PASSED,
        INTEGRITY_FAILED,
        KEYSTORE_SIGN_SUCCESS,
        KEYSTORE_SIGN_FAILED,

        // Micro-verification events
        MICRO_VERIFY_TOKEN,
        MICRO_VERIFY_FACE_RECHECK,
        MICRO_VERIFY_COMPLETE,
        MICRO_VERIFY_FAILED,

        // Session events
        SESSION_STARTED,
        ATTENDANCE_SUBMITTED,
        ATTENDANCE_CONFIRMED,
        ATTENDANCE_REJECTED
    }

    private val events = mutableListOf<AuditEvent>()

    /**
     * Log a verification event.
     */
    fun log(eventType: EventType, result: Boolean, metadata: Map<String, String> = emptyMap()) {
        val event = AuditEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            result = result,
            metadata = metadata
        )
        events.add(event)
        Log.d("AuditLogger", "[$eventType] result=$result $metadata")
    }

    /**
     * Log with simple key-value metadata.
     */
    fun log(eventType: EventType, result: Boolean, vararg pairs: Pair<String, String>) {
        log(eventType, result, pairs.toMap())
    }

    /**
     * Get all events for the current session (to send with attendance request).
     */
    fun getEvents(): List<AuditEvent> = events.toList()

    /**
     * Get events as a list of maps (for JSON serialization).
     */
    fun getEventsAsMapList(): List<Map<String, Any>> {
        return events.map { event ->
            mapOf(
                "timestamp" to event.timestamp,
                "eventType" to event.eventType.name,
                "result" to event.result,
                "metadata" to event.metadata
            )
        }
    }

    /**
     * Get count of events by type.
     */
    fun getEventCount(eventType: EventType): Int {
        return events.count { it.eventType == eventType }
    }

    /**
     * Check if any event of a given type has succeeded.
     */
    fun hasSuccessful(eventType: EventType): Boolean {
        return events.any { it.eventType == eventType && it.result }
    }

    /**
     * Clear all events (when session ends or attendance confirmed).
     */
    fun clear() {
        events.clear()
    }

    /**
     * Get a summary string for debugging.
     */
    fun getSummary(): String {
        val grouped = events.groupBy { it.eventType }
        return grouped.entries.joinToString("\n") { (type, events) ->
            val passed = events.count { it.result }
            val failed = events.count { !it.result }
            "$type: $passed passed, $failed failed"
        }
    }
}
