package com.example.practice.features

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * Liveness Detector — uses ML Kit FaceDetector to verify a live person.
 *
 * Liveness signals:
 * - Eye blink detection (probability drops below threshold)
 * - Head rotation (Euler angle Y or Z > 10°)
 *
 * Prevents: photo/video attacks, printed photo tricks.
 */
class LivenessDetector {

    data class LivenessResult(
        val isLive: Boolean,
        val faceDetected: Boolean,
        val blinkDetected: Boolean,
        val headMoved: Boolean,
        val confidence: Float,
        val leftEyeOpen: Float = 1f,
        val rightEyeOpen: Float = 1f,
        val headYAngle: Float = 0f,
        val headZAngle: Float = 0f
    )

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Enables eye open + smile
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    // State tracking for liveness over multiple frames
    private var blinkDetectedCount = 0
    private var headMovedCount = 0
    private var framesProcessed = 0
    private var eyeWasOpen = true  // Track eye state transitions
    private var headWasTurned = false // Track head state transitions

    companion object {
        private const val BLINK_THRESHOLD = 0.4f       // Eye considered closed below this
        private const val EYE_OPEN_THRESHOLD = 0.7f     // Eye considered open above this
        private const val HEAD_ROTATION_THRESHOLD = 10f  // Degrees
        private const val REQUIRED_BLINKS = 1
        private const val REQUIRED_HEAD_MOVES = 1
    }

    /**
     * Process a single frame and update liveness state.
     *
     * @return LivenessResult with current state
     */
    suspend fun processFrame(image: InputImage): LivenessResult {
        val faces = detectFaces(image)

        if (faces.isEmpty()) {
            return LivenessResult(
                isLive = false,
                faceDetected = false,
                blinkDetected = blinkDetectedCount >= REQUIRED_BLINKS,
                headMoved = headMovedCount >= REQUIRED_HEAD_MOVES,
                confidence = 0f
            )
        }

        val face = faces.first()
        framesProcessed++

        val leftEyeOpen = face.leftEyeOpenProbability ?: 1f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 1f
        val headY = face.headEulerAngleY  // Left-right rotation
        val headZ = face.headEulerAngleZ  // Tilt

        // Detect blink: eye was open → now closed → back to open
        val eyesClosed = leftEyeOpen < BLINK_THRESHOLD && rightEyeOpen < BLINK_THRESHOLD
        val eyesOpen = leftEyeOpen > EYE_OPEN_THRESHOLD && rightEyeOpen > EYE_OPEN_THRESHOLD

        if (eyeWasOpen && eyesClosed) {
            // Eyes just closed — blink in progress
            eyeWasOpen = false
        } else if (!eyeWasOpen && eyesOpen) {
            // Eyes reopened — blink complete
            blinkDetectedCount++
            eyeWasOpen = true
        } else if (eyesOpen) {
            eyeWasOpen = true
        }

        // Detect head movement
        val currentHeadTurned = abs(headY) > HEAD_ROTATION_THRESHOLD || abs(headZ) > HEAD_ROTATION_THRESHOLD
        if (!headWasTurned && currentHeadTurned) {
            headMovedCount++
            headWasTurned = true
        } else if (!currentHeadTurned) {
            headWasTurned = false
        }

        val blinkOk = blinkDetectedCount >= REQUIRED_BLINKS
        val headOk = headMovedCount >= REQUIRED_HEAD_MOVES
        val isLive = blinkOk && headOk

        // Confidence based on how many checks passed
        val confidence = when {
            isLive -> 0.9f + (framesProcessed.coerceAtMost(10) / 100f)
            blinkOk || headOk -> 0.5f
            else -> 0.2f
        }

        return LivenessResult(
            isLive = isLive,
            faceDetected = true,
            blinkDetected = blinkOk,
            headMoved = headOk,
            confidence = confidence.coerceAtMost(1f),
            leftEyeOpen = leftEyeOpen,
            rightEyeOpen = rightEyeOpen,
            headYAngle = headY,
            headZAngle = headZ
        )
    }

    /**
     * Reset liveness tracking state (for new verification attempt).
     */
    fun reset() {
        blinkDetectedCount = 0
        headMovedCount = 0
        framesProcessed = 0
        eyeWasOpen = true
    }

    /**
     * Release detector resources.
     */
    fun close() {
        detector.close()
    }

    // ─── Private helpers ──────────────────────────

    private suspend fun detectFaces(image: InputImage): List<Face> {
        return suspendCancellableCoroutine { continuation ->
            detector.process(image)
                .addOnSuccessListener { faces ->
                    continuation.resume(faces)
                }
                .addOnFailureListener {
                    continuation.resume(emptyList())
                }
        }
    }
}
