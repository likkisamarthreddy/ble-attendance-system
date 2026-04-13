package com.example.practice.features

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Rolling Token Manager — generates and validates HMAC-SHA256 tokens
 * that rotate every 7 seconds.
 *
 * BLE broadcasts ONLY the 8-byte truncated token.
 * timeSlot is derived independently: System.currentTimeMillis() / 7000
 */
object RollingTokenManager {

    private const val SLOT_DURATION_MS = 3000L
    private const val TOKEN_BYTE_LENGTH = 8  // 8 bytes = 16 hex chars, fits BLE payload

    /**
     * Get the current time slot index (changes every 7 seconds).
     */
    fun getCurrentTimeSlot(): Long {
        return System.currentTimeMillis() / SLOT_DURATION_MS
    }

    /**
     * Get milliseconds remaining in the current slot.
     */
    fun getTimeRemainingInSlot(): Long {
        return SLOT_DURATION_MS - (System.currentTimeMillis() % SLOT_DURATION_MS)
    }

    /**
     * Generate an 8-byte HMAC-SHA256 token for the given session secret and time slot.
     *
     * @param sessionSecret The shared secret from backend (Base64 or hex string)
     * @param timeSlot The time slot index (default: current)
     * @return 8-byte token as hex string (16 chars)
     */
    fun generateToken(sessionSecret: String, timeSlot: Long = getCurrentTimeSlot()): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(sessionSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)

        // HMAC input: timeSlot as 8-byte big-endian
        val slotBytes = ByteArray(8)
        var slot = timeSlot
        for (i in 7 downTo 0) {
            slotBytes[i] = (slot and 0xFF).toByte()
            slot = slot shr 8
        }

        val fullHash = mac.doFinal(slotBytes)

        // Truncate to 8 bytes
        return fullHash.take(TOKEN_BYTE_LENGTH).joinToString("") {
            "%02x".format(it)
        }
    }

    /**
     * Generate the raw 8-byte token as ByteArray for BLE advertising.
     */
    fun generateTokenBytes(sessionSecret: String, timeSlot: Long = getCurrentTimeSlot()): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(sessionSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)

        val slotBytes = ByteArray(8)
        var slot = timeSlot
        for (i in 7 downTo 0) {
            slotBytes[i] = (slot and 0xFF).toByte()
            slot = slot shr 8
        }

        val fullHash = mac.doFinal(slotBytes)
        return fullHash.copyOfRange(0, TOKEN_BYTE_LENGTH)
    }

    /**
     * Convert scanned BLE service data bytes back to hex token string.
     */
    fun bytesToHexToken(bytes: ByteArray): String {
        return bytes.take(TOKEN_BYTE_LENGTH).joinToString("") {
            "%02x".format(it)
        }
    }

    /**
     * Validate a token hex string (client-side pre-check, final validation on backend).
     * Allows ±1 slot tolerance for clock skew.
     *
     * @return true if token matches any of the 3 valid slots
     */
    fun validateToken(
        token: String,
        sessionSecret: String,
        timeSlot: Long = getCurrentTimeSlot(),
        tolerance: Int = 1
    ): Boolean {
        for (offset in -tolerance..tolerance) {
            val expected = generateToken(sessionSecret, timeSlot + offset)
            if (token == expected) return true
        }
        return false
    }

    /**
     * Get the slot duration in milliseconds.
     */
    fun getSlotDurationMs(): Long = SLOT_DURATION_MS
}
