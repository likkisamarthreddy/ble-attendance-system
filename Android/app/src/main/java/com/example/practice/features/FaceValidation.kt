package com.example.practice.features

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.mlkit.vision.face.Face
import kotlin.math.max
import kotlin.math.min

private const val TAG = "FaceValidation"

// ── Thresholds ──
private const val MIN_BRIGHTNESS = 50       // Too dark below this (was 40)
private const val MAX_BRIGHTNESS = 200      // Too bright above this (was 220)
private const val MIN_FACE_RATIO = 0.15f    // Face too far (< 15% of image)
private const val MAX_FACE_RATIO = 0.85f    // Face too close (> 85% of image)
private const val OVEREXPOSED_THRESHOLD = 240   // Pixel considered overexposed
private const val OVEREXPOSED_RATIO_LIMIT = 0.25f  // Reject if > 25% pixels overexposed
private const val UNDEREXPOSED_THRESHOLD = 20    // Pixel considered underexposed
private const val UNDEREXPOSED_RATIO_LIMIT = 0.30f  // Reject if > 30% pixels underexposed

data class FaceValidationResult(
    val isValid: Boolean,
    val message: String
)

/**
 * Advanced lighting analysis result.
 * Contains average brightness + overexposed/underexposed pixel ratios.
 */
data class LightingAnalysis(
    val avgBrightness: Int,
    val overexposedRatio: Float,  // % of pixels with luminance > 240
    val underexposedRatio: Float  // % of pixels with luminance < 20
)

/**
 * Validates face quality before embedding generation.
 * Checks: multi-face, landmarks, distance, advanced lighting.
 * Returns a result with pass/fail + guidance message.
 */
fun validateFaceQuality(
    faces: List<Face>,
    face: Face,
    imageWidth: Int,
    imageHeight: Int,
    croppedFaceBitmap: Bitmap? = null
): FaceValidationResult {

    // ── Bug #3: Multiple faces in frame ──
    if (faces.size > 1) {
        Log.w(TAG, "Multiple faces detected: ${faces.size}")
        return FaceValidationResult(false, "Only one face allowed in frame")
    }

    // ── Bug #2: Partial face — require both eyes visible ──
    val leftEye = face.leftEyeOpenProbability
    val rightEye = face.rightEyeOpenProbability
    if (leftEye == null || rightEye == null) {
        Log.w(TAG, "Partial face: leftEye=$leftEye, rightEye=$rightEye")
        return FaceValidationResult(false, "Face your camera directly")
    }

    // ── Bug #4: Face distance — check bounding box ratio ──
    val faceWidth = face.boundingBox.width().toFloat()
    val referenceSize = max(imageWidth, imageHeight).toFloat()
    val faceRatio = faceWidth / referenceSize

    if (faceRatio < MIN_FACE_RATIO) {
        Log.w(TAG, "Face too far: ratio=${"%.2f".format(faceRatio)}")
        return FaceValidationResult(false, "Move closer to the camera")
    }
    if (faceRatio > MAX_FACE_RATIO) {
        Log.w(TAG, "Face too close: ratio=${"%.2f".format(faceRatio)}")
        return FaceValidationResult(false, "Move further from the camera")
    }

    // ── Bug #1: Advanced lighting — check on CROPPED face only ──
    if (croppedFaceBitmap != null) {
        val lighting = analyzeLighting(croppedFaceBitmap)

        // Check overexposed pixel ratio (blown-out face)
        if (lighting.overexposedRatio > OVEREXPOSED_RATIO_LIMIT) {
            Log.w(TAG, "Overexposed: ${(lighting.overexposedRatio * 100).toInt()}% pixels > $OVEREXPOSED_THRESHOLD")
            return FaceValidationResult(false, "Too much light on your face\nMove away from the light source")
        }

        // Check underexposed pixel ratio (face in shadow)
        if (lighting.underexposedRatio > UNDEREXPOSED_RATIO_LIMIT) {
            Log.w(TAG, "Underexposed: ${(lighting.underexposedRatio * 100).toInt()}% pixels < $UNDEREXPOSED_THRESHOLD")
            return FaceValidationResult(false, "Face too dark\nMove to a brighter spot")
        }

        // Check average brightness
        if (lighting.avgBrightness < MIN_BRIGHTNESS) {
            Log.w(TAG, "Too dark: avg=${lighting.avgBrightness}")
            return FaceValidationResult(false, "Not enough light\nFace a window or light source")
        }
        if (lighting.avgBrightness > MAX_BRIGHTNESS) {
            Log.w(TAG, "Too bright: avg=${lighting.avgBrightness}")
            return FaceValidationResult(false, "Avoid strong light behind you\nMove slightly to the side")
        }
    }

    return FaceValidationResult(true, "Face OK")
}

/**
 * Advanced lighting analysis on a cropped face bitmap.
 * Returns average brightness AND overexposed/underexposed pixel ratios.
 * Samples the center 60% of the face for accuracy (avoids hair/background edges).
 */
fun analyzeLighting(bitmap: Bitmap): LightingAnalysis {
    val w = bitmap.width
    val h = bitmap.height

    // Sample from center 60% of the face
    val startX = (w * 0.2f).toInt()
    val endX = (w * 0.8f).toInt()
    val startY = (h * 0.2f).toInt()
    val endY = (h * 0.8f).toInt()

    val step = 4  // Sample every 4th pixel for speed
    var totalLuminance = 0L
    var totalPixels = 0
    var brightPixels = 0
    var darkPixels = 0

    var y = startY
    while (y < endY) {
        var x = startX
        while (x < endX) {
            val pixel = bitmap.getPixel(x, y)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            // ITU-R BT.601 luminance
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            totalLuminance += luminance
            totalPixels++

            if (luminance > OVEREXPOSED_THRESHOLD) brightPixels++
            if (luminance < UNDEREXPOSED_THRESHOLD) darkPixels++

            x += step
        }
        y += step
    }

    val avgBrightness = if (totalPixels > 0) (totalLuminance / totalPixels).toInt() else 128
    val overexposedRatio = if (totalPixels > 0) brightPixels.toFloat() / totalPixels else 0f
    val underexposedRatio = if (totalPixels > 0) darkPixels.toFloat() / totalPixels else 0f

    Log.d(TAG, "Lighting: avg=$avgBrightness, overexposed=${(overexposedRatio * 100).toInt()}%, underexposed=${(underexposedRatio * 100).toInt()}%")

    return LightingAnalysis(avgBrightness, overexposedRatio, underexposedRatio)
}

/**
 * Backward-compatible wrapper — returns just average brightness.
 */
fun calculateBrightness(bitmap: Bitmap): Int {
    return analyzeLighting(bitmap).avgBrightness
}

/**
 * Apply histogram equalization to a face bitmap for lighting normalization.
 * This makes embeddings more stable under varying lighting conditions.
 * Works on the luminance (Y) channel while preserving color.
 */
fun normalizeLighting(bitmap: Bitmap): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    // Step 1: Build luminance histogram
    val histogram = IntArray(256)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val pixel = bitmap.getPixel(x, y)
            val lum = (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)).toInt()
            histogram[lum.coerceIn(0, 255)]++
        }
    }

    // Step 2: Build cumulative distribution function (CDF)
    val cdf = IntArray(256)
    cdf[0] = histogram[0]
    for (i in 1..255) {
        cdf[i] = cdf[i - 1] + histogram[i]
    }

    // Step 3: Normalize CDF to 0-255
    val totalPixels = w * h
    val cdfMin = cdf.first { it > 0 }
    val lookup = IntArray(256) { i ->
        if (totalPixels > cdfMin) {
            ((cdf[i] - cdfMin).toFloat() / (totalPixels - cdfMin) * 255).toInt().coerceIn(0, 255)
        } else {
            i
        }
    }

    // Step 4: Apply equalization — adjust each pixel's brightness while preserving hue
    for (y in 0 until h) {
        for (x in 0 until w) {
            val pixel = bitmap.getPixel(x, y)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val a = Color.alpha(pixel)

            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
            val newLum = lookup[lum]

            // Scale RGB by the same factor to preserve color
            val scale = if (lum > 0) newLum.toFloat() / lum else 1f
            val newR = (r * scale).toInt().coerceIn(0, 255)
            val newG = (g * scale).toInt().coerceIn(0, 255)
            val newB = (b * scale).toInt().coerceIn(0, 255)

            result.setPixel(x, y, Color.argb(a, newR, newG, newB))
        }
    }

    return result
}
