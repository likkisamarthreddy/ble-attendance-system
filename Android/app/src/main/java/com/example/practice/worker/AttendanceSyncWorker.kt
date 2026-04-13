package com.example.practice.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.practice.RequestBodyApi.ScanAttendanceRequest
import com.example.practice.api.RetrofitInstance
import com.example.practice.data.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await

private const val TAG = "AttendanceSyncWorker"

/**
 * WorkManager CoroutineWorker that syncs offline-cached attendance records
 * to the backend when connectivity is restored.
 *
 * Each pending record is retried individually; successfully synced records
 * are deleted from Room. If any record fails, the worker returns [Result.retry()]
 * so WorkManager will re-run with exponential backoff.
 */
class AttendanceSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val gson = Gson()

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting offline attendance sync...")

        val dao = AppDatabase.getInstance(applicationContext).pendingAttendanceDao()
        val pendingRecords = dao.getAll()

        if (pendingRecords.isEmpty()) {
            Log.d(TAG, "No pending records to sync.")
            return Result.success()
        }

        Log.d(TAG, "Found ${pendingRecords.size} pending record(s) to sync.")

        // Get fresh Firebase token
        val firebaseToken = try {
            val user = FirebaseAuth.getInstance().currentUser
            user?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            Log.e(TAG, "Cannot get Firebase token, will retry later.", e)
            return Result.retry()
        }

        if (firebaseToken == null) {
            Log.e(TAG, "Firebase token is null (user not logged in?), will retry later.")
            return Result.retry()
        }

        var allSynced = true

        for (record in pendingRecords) {
            try {
                // Deserialize JSON fields back to their original types
                val faceEmbedding: List<Float> = gson.fromJson(
                    record.faceEmbeddingJson,
                    object : TypeToken<List<Float>>() {}.type
                )
                val auditLog: List<Map<String, Any>> = gson.fromJson(
                    record.auditLogJson,
                    object : TypeToken<List<Map<String, Any>>>() {}.type
                )

                val request = ScanAttendanceRequest(
                    token = record.token,
                    latitude = record.latitude,
                    longitude = record.longitude,
                    faceEmbedding = faceEmbedding,
                    deviceId = record.deviceId,
                    integrityToken = record.integrityToken,
                    auditLog = auditLog,
                    uid = record.uid
                )

                val response = RetrofitInstance.studentApi.markScannedAttendance(
                    "Bearer $firebaseToken",
                    request
                )

                if (response.isSuccessful) {
                    // Synced successfully — remove from local DB
                    dao.deleteById(record.id)
                    Log.d(TAG, "✅ Synced record id=${record.id} (uid=${record.uid})")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.w(TAG, "❌ Failed to sync record id=${record.id}: $errorBody")
                    // If server says "already marked" or similar client error, remove the record
                    if (response.code() in 400..499) {
                        Log.w(TAG, "   Client error (${response.code()}), removing stale record.")
                        dao.deleteById(record.id)
                    } else {
                        allSynced = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Network error syncing record id=${record.id}: ${e.message}")
                allSynced = false
            }
        }

        return if (allSynced) {
            Log.d(TAG, "All pending records synced successfully.")
            Result.success()
        } else {
            Log.d(TAG, "Some records failed to sync, will retry later.")
            Result.retry()
        }
    }
}
