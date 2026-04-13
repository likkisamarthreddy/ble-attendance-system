package com.example.practice.features

/**
 * Replay Protection Manager — prevents token reuse.
 *
 * Client-side: tracks used {token, timeSlot} pairs in memory.
 * Each student can only submit one token per timeSlot per session.
 * Backend should also enforce this server-side.
 */
class ReplayProtectionManager {

    // Key: "sessionId:timeSlot", Value: submitted token
    private val usedTokens = mutableMapOf<String, String>()

    /**
     * Check if a token has already been submitted for this session + timeSlot.
     *
     * @param sessionId The session identifier
     * @param timeSlot The time slot index
     * @param token The token hex string
     * @return true if token is fresh (not a replay), false if already used
     */
    fun isTokenFresh(sessionId: String, timeSlot: Long, token: String): Boolean {
        val key = "$sessionId:$timeSlot"
        return !usedTokens.containsKey(key)
    }

    /**
     * Record that a token has been submitted for this session + timeSlot.
     */
    fun recordToken(sessionId: String, timeSlot: Long, token: String) {
        val key = "$sessionId:$timeSlot"
        usedTokens[key] = token
    }

    /**
     * Check and record in one atomic operation.
     *
     * @return true if token was fresh and has been recorded, false if replay detected
     */
    fun checkAndRecord(sessionId: String, timeSlot: Long, token: String): Boolean {
        val key = "$sessionId:$timeSlot"
        if (usedTokens.containsKey(key)) return false
        usedTokens[key] = token
        return true
    }

    /**
     * Get the number of unique time slots with successful token submissions.
     * Used for continuous micro-verification (need ≥3).
     */
    fun getVerifiedSlotCount(sessionId: String): Int {
        return usedTokens.keys.count { it.startsWith("$sessionId:") }
    }

    /**
     * Clear all recorded tokens (e.g., when session ends).
     */
    fun clear() {
        usedTokens.clear()
    }

    /**
     * Clear tokens for a specific session.
     */
    fun clearSession(sessionId: String) {
        usedTokens.keys.removeAll { it.startsWith("$sessionId:") }
    }
}
