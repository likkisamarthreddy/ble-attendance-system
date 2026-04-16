package com.example.practice.utils

import android.util.Log
import java.net.URLEncoder

object EncoderHelper {
    private const val TAG = "EncoderHelper"

    fun safeEncode(value: String?, fallback: String = ""): String {
        if (value == null) {
            Log.e(TAG, "Missing value for encoding. Returning fallback value.")
            return fallback
        }
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding value: ${e.message}")
            fallback
        }
    }
}
