package com.example.practice.StudentApp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.practice.RequestBodyApi.ScanAttendanceRequest
import com.example.practice.ResponsesModel.ScanAttendanceResponse
import com.example.practice.api.RetrofitInstance
import com.example.practice.data.AppDatabase
import com.example.practice.data.PendingAttendanceEntity
import com.example.practice.features.*
import com.example.practice.worker.AttendanceSyncWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

sealed class AttendanceMarkedState {
    object Idle : AttendanceMarkedState()
    object Loading : AttendanceMarkedState()
    data class Success(val scanAttendanceMarked: ScanAttendanceResponse) : AttendanceMarkedState()
    data class Error(val message: String) : AttendanceMarkedState()
}

class ScanningViewModel : ViewModel() {
    companion object {
        private const val TAG = "ScanningViewModel"
        private const val SCAN_PERIOD = 10000L // 10 seconds

        // ─── Security thresholds ───
        private const val MICRO_VERIFY_DURATION = 30000L // 30 seconds continuous verify
        private const val REQUIRED_TOKENS = 1           // 1 valid token required
        private const val FACE_RECHECK_INTERVAL = 10000L // Re-verify face every 10s
    }

    // Guard: prevent double attendance marking
    private val attendanceSubmitted = AtomicBoolean(false)


    // Bluetooth components
    var bluetoothAdapter: BluetoothAdapter? = null
    var advertiser: BluetoothLeAdvertiser? = null
    var scanner: BluetoothLeScanner? = null
    private val handler = Handler(Looper.getMainLooper())

    private val scanStopRequested = AtomicBoolean(false)
    private val scanTimeoutTask = Runnable { stopScanningInternal("Scan timeout reached") }

    private var scanCallback: ScanCallback? = null

    // ─── Security managers ───
    private val replayProtection = ReplayProtectionManager()
    private val auditLogger = AuditLogger()

    // ─── Security state ───
    private val _tokenVerified = MutableStateFlow(false)
    val tokenVerified: StateFlow<Boolean> = _tokenVerified

    private val _rssiOk = MutableStateFlow(false)
    val rssiOk: StateFlow<Boolean> = _rssiOk

    private val _geofenceVerified = MutableStateFlow(false)
    val geofenceVerified: StateFlow<Boolean> = _geofenceVerified

    private val _faceVerified = MutableStateFlow(false)
    val faceVerified: StateFlow<Boolean> = _faceVerified

    private val _integrityVerified = MutableStateFlow(false)
    val integrityVerified: StateFlow<Boolean> = _integrityVerified

    private val _microVerifyProgress = MutableStateFlow(0)
    val microVerifyProgress: StateFlow<Int> = _microVerifyProgress

    private val _currentToken = MutableStateFlow<String?>(null)
    val currentToken: StateFlow<String?> = _currentToken

    private val _tokenTimeRemaining = MutableStateFlow(0L)
    val tokenTimeRemaining: StateFlow<Long> = _tokenTimeRemaining

    // Stored face embedding from verification screen
    private var verifiedFaceEmbedding: FloatArray? = null

    // Session info (set before scanning)
    private var sessionId: String = ""
    private var sessionSecret: String = ""

    // Active session recordId resolved from backend
    private var activeRecordId: String = ""

    // ─── Pre-fetched resources (populated before BLE scan starts) ───
    private var cachedFirebaseToken: String? = null
    private var cachedLatitude: Double = 0.0
    private var cachedLongitude: Double = 0.0
    private var cachedIntegrityToken: String = "integrity_failed"
    private var cachedDeviceId: String = "unknown_device"

    // Micro-verification job
    private var microVerifyJob: Job? = null

    private val appApi = RetrofitInstance.studentApi

    // UUIDs
    private val shortServiceUUID = ParcelUuid(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB"))

    // UI state flows
    private val _statusMessage = MutableStateFlow<String>("")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _scanResultMessage = MutableStateFlow<String>("")
    val scanResultMessage: StateFlow<String> = _scanResultMessage

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scannedData = MutableStateFlow<List<AlreadyScannedData>>(emptyList())
    val scannedData: StateFlow<List<AlreadyScannedData>> = _scannedData

    private val _attendanceMarked = MutableStateFlow<AttendanceMarkedState>(AttendanceMarkedState.Idle)
    val attendanceMarked: StateFlow<AttendanceMarkedState> = _attendanceMarked.asStateFlow()

    private val _hasNetworkAccess = MutableStateFlow(false)
    val hasNetworkAccess: StateFlow<Boolean> = _hasNetworkAccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val processedData = mutableSetOf<String>()

    data class AlreadyScannedData(
        val data: String?,
        val timestamp: Long = System.currentTimeMillis()
    )

    // ═══════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════

    fun setVerifiedFaceEmbedding(embedding: FloatArray) {
        verifiedFaceEmbedding = embedding
        _faceVerified.value = true
        Log.d(TAG, "Face embedding set, faceVerified=true")
    }

    fun initialize(adapter: BluetoothAdapter?, context: Context) {
        bluetoothAdapter = adapter
        if (adapter != null) {
            advertiser = adapter.bluetoothLeAdvertiser
            scanner = adapter.bluetoothLeScanner
            _statusMessage.value = if (isBleSupported()) "BLE Ready" else "BLE Not Supported"
        } else {
            _statusMessage.value = "Bluetooth Not Available"
        }

        // Initialize device security key — may fail on some devices
        try {
            DeviceSecurityManager.getOrCreateKeyPair()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize device security key", e)
        }

        Log.i(TAG, "BLE Initialized: Adapter=${adapter != null}")
    }

    /**
     * Set session details (called before scanning).
     * In production, these come from the backend via API.
     */
    fun setSessionInfo(id: String, secret: String) {
        sessionId = id
        sessionSecret = secret
        replayProtection.clearSession(id)
        auditLogger.clear()
        auditLogger.log(AuditLogger.EventType.SESSION_STARTED, true, "sessionId" to id)
    }

    /**
     * Set verified face embedding from FaceVerificationScreen.
     */
    fun setFaceEmbedding(embedding: FloatArray) {
        verifiedFaceEmbedding = embedding
        _faceVerified.value = true
        auditLogger.log(AuditLogger.EventType.FACE_MATCHED, true)
    }

    // ═══════════════════════════════════════
    // PERMISSIONS & CAPABILITIES
    // ═══════════════════════════════════════

    fun hasRequiredBluetoothPermissions(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            return context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBleSupported(): Boolean = bluetoothAdapter != null && advertiser != null && scanner != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun checkNetworkAccess(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
        viewModelScope.launch { _hasNetworkAccess.value = isConnected }
    }

    // ═══════════════════════════════════════
    // SCANNING WITH SECURITY LAYERS
    // ═══════════════════════════════════════

    fun startScanning(context: Context) {
        if (!hasRequiredBluetoothPermissions(context)) {
            _scanResultMessage.value = "Missing required Bluetooth permissions"
            return
        }
        if (!isBluetoothEnabled()) {
            _scanResultMessage.value = "Please enable Bluetooth first"
            return
        }
        if (scanner == null) {
            _scanResultMessage.value = "BLE Scanning not supported on this device"
            return
        }
        if (_isScanning.value) {
            _scanResultMessage.value = "Already scanning"
            return
        }

        try {
            scanStopRequested.set(false)
            attendanceSubmitted.set(false)

            viewModelScope.launch {
                _scannedData.value = emptyList()
                _scanResultMessage.value = "Preparing..."

                // ── STEP 1: Pre-fetch Firebase token ──
                try {
                    val fbToken = getFirebaseToken()
                    if (fbToken != null) {
                        cachedFirebaseToken = fbToken
                    } else {
                        _scanResultMessage.value = "Authentication error"
                        return@launch
                    }
                } catch (e: Exception) {
                    _scanResultMessage.value = "Auth error: ${e.message}"
                    return@launch
                }

                // ── STEP 2: Fetch active session ──
                _scanResultMessage.value = "Fetching active session..."
                try {
                    val response = appApi.getActiveSessions("Bearer ${cachedFirebaseToken!!}")
                    if (response.isSuccessful && response.body() != null) {
                        val sessions = response.body()!!.sessions
                        if (sessions.isNotEmpty()) {
                            val session = sessions.first()
                            activeRecordId = session.recordId
                            sessionSecret = session.sessionSecret
                            sessionId = session.recordId
                            Log.d(TAG, "Active session found: recordId=${session.recordId}, course=${session.courseName}")
                        } else {
                            Log.w(TAG, "No active sessions found for enrolled courses")
                            _scanResultMessage.value = "No active attendance session found"
                            return@launch
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch active sessions: ${response.code()}")
                        _scanResultMessage.value = "Failed to fetch session"
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching active sessions", e)
                    _scanResultMessage.value = "Error: ${e.message}"
                    return@launch
                }

                // ── STEP 3: Pre-fetch location ──
                _scanResultMessage.value = "Getting location..."
                try {
                    val geofenceManager = GeofenceManager(context)
                    val location = geofenceManager.getCurrentLocation()
                    cachedLatitude = location?.latitude ?: 0.0
                    cachedLongitude = location?.longitude ?: 0.0
                } catch (e: Exception) {
                    Log.w(TAG, "Location pre-fetch failed, continuing", e)
                }

                // ── STEP 4: Pre-fetch integrity token ──
                try {
                    val nonce = java.util.UUID.randomUUID().toString()
                    cachedIntegrityToken = DeviceSecurityManager.requestIntegrityVerdict(context, nonce) ?: "integrity_failed"
                    cachedDeviceId = DeviceSecurityManager.getPublicKeyBase64() ?: "unknown_device"
                } catch (e: Exception) {
                    Log.w(TAG, "Integrity pre-fetch failed, continuing", e)
                }

                Log.i(TAG, "All resources pre-fetched — starting BLE scan")

                // ── STEP 5: Session + resources resolved — NOW start BLE scanning ──
                _scanResultMessage.value = "Scanning for attendance token..."
                startBleScanInternal(context)
            }

        } catch (e: Exception) {
            _scanResultMessage.value = "Error starting scan: ${e.message}"
        }
    }

    /**
     * Internal: Start BLE scanning AFTER session is resolved.
     * Called from the coroutine inside startScanning once activeRecordId is set.
     */
    @android.annotation.SuppressLint("MissingPermission")
    private fun startBleScanInternal(context: Context) {
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(shortServiceUUID)
                .build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Log.d(TAG, "BLE onScanResult triggered, attendanceSubmitted=${attendanceSubmitted.get()}, scanStopRequested=${scanStopRequested.get()}")
                if (scanStopRequested.get() || !_isScanning.value) return
                // Guard: already submitted — ignore all further tokens
                if (attendanceSubmitted.get()) return

                try {
                    super.onScanResult(callbackType, result)

                    val rssi = result.rssi
                    val scanRecord = result.scanRecord
                    val serviceData = scanRecord?.getServiceData(shortServiceUUID) ?: return

                    // Extract 8-byte HMAC token from BLE service data
                    val tokenHex = RollingTokenManager.bytesToHexToken(serviceData)
                    _currentToken.value = tokenHex
                    _tokenVerified.value = true

                    Log.i(TAG, "✅ Token found: $tokenHex — marking attendance immediately")

                    // ═══ FIRST TOKEN → MARK ATTENDANCE & STOP ═══
                    if (!attendanceSubmitted.compareAndSet(false, true)) {
                        return  // Another callback beat us
                    }

                    // Stop scanning FIRST, then mark attendance
                    stopScanningInternal("Token found — submitting attendance...")
                    // Mark attendance with the SPECIFIC token we found
                    markScanAttendance(activeRecordId, tokenHex, context)

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing scan result", e)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                try {
                    super.onScanFailed(errorCode)
                    val errorMessage = when (errorCode) {
                        ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Scanning not supported"
                        ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Internal scanning error"
                        else -> "Unknown scanning error: $errorCode"
                    }
                    stopScanningInternal("Scan failed: $errorMessage")
                } catch (e: Exception) {
                    stopScanningInternal("Error in scan failure handler")
                }
            }
        }

        // Start scanning
        try {
            _isScanning.value = true
            scanner?.startScan(filters, settings, scanCallback)
            Log.i(TAG, "BLE scanning started (session: $activeRecordId)")

            // Timeout for scan
            handler.removeCallbacks(scanTimeoutTask)
            handler.postDelayed(scanTimeoutTask, MICRO_VERIFY_DURATION + SCAN_PERIOD)

            // Start countdown timer
            startTokenCountdown()

        } catch (secEx: SecurityException) {
            stopScanningInternal("Permission denied: ${secEx.message}")
        } catch (e: Exception) {
            stopScanningInternal("Error starting scan: ${e.message}")
        }
    }

    /**
     * Continuously update token countdown timer.
     */
    private fun startTokenCountdown() {
        microVerifyJob?.cancel()
        microVerifyJob = viewModelScope.launch {
            while (_isScanning.value) {
                _tokenTimeRemaining.value = RollingTokenManager.getTimeRemainingInSlot()
                delay(100)
            }
        }
    }

    // ═══════════════════════════════════════
    // ATTENDANCE MARKING
    // ═══════════════════════════════════════

    fun markScanAttendance(courseUid: String, tokenStr: String, context: Context) {
        if (_attendanceMarked.value is AttendanceMarkedState.Loading) return

        _attendanceMarked.value = AttendanceMarkedState.Loading
        auditLogger.log(AuditLogger.EventType.ATTENDANCE_SUBMITTED, true, "token" to tokenStr)

        viewModelScope.launch {
            // Use pre-fetched Firebase token (avoid re-fetching delay)
            val token = cachedFirebaseToken ?: getFirebaseToken()
            if (token != null) {
                try {
                    markScanAttendanceFromApi(courseUid, tokenStr, token, context)
                } catch (e: Exception) {
                    _attendanceMarked.value = AttendanceMarkedState.Error("Exception: ${e.message}")
                    auditLogger.log(AuditLogger.EventType.ATTENDANCE_REJECTED, false, "reason" to (e.message ?: "unknown"))
                    delay(3000)
                    resetMarkScanAttendanceState()
                }
            } else {
                _attendanceMarked.value = AttendanceMarkedState.Error("Failed to fetch Firebase Token")
                delay(3000)
                resetMarkScanAttendanceState()
            }
        }
    }

    private suspend fun markScanAttendanceFromApi(courseUid: String, bToken: String, fbToken: String, context: Context) {
        try {
            // Use PRE-FETCHED location (already cached before BLE scan started)
            val lat = cachedLatitude
            val lon = cachedLongitude
            
            if (lat == 0.0 && lon == 0.0) {
                 auditLogger.log(AuditLogger.EventType.GEOFENCE_OUT_OF_RANGE, false, "reason" to "Location unavailable")
            } else {
                 auditLogger.log(AuditLogger.EventType.GEOFENCE_IN_RANGE, true, "lat" to lat.toString(), "lon" to lon.toString())
            }

            // Use PRE-FETCHED face embedding
            val embeddingList = verifiedFaceEmbedding?.toList() ?: emptyList()

            // Use PRE-FETCHED integrity token and device ID
            val request = ScanAttendanceRequest(
                token = bToken,
                latitude = lat,
                longitude = lon,
                faceEmbedding = embeddingList,
                deviceId = cachedDeviceId,
                integrityToken = cachedIntegrityToken,
                auditLog = auditLogger.getEventsAsMapList(),
                uid = courseUid
            )

            Log.d(TAG, "Submitting attendance IMMEDIATELY: course=$courseUid, token=$bToken")
            val response = appApi.markScannedAttendance("Bearer $fbToken", request)

            if (response.isSuccessful && response.body() != null) {
                _attendanceMarked.value = AttendanceMarkedState.Success(response.body()!!)
                auditLogger.log(AuditLogger.EventType.ATTENDANCE_CONFIRMED, true)
                Toast.makeText(context, "Attendance marked successfully", Toast.LENGTH_SHORT).show()
                processedData.add(courseUid)

                // NO automatic reset here! UI will stay on success screen.
            } else {
                val errorBody = response.errorBody()?.string()
                var errorMessage = "Failed to mark attendance"
                try {
                    errorBody?.let {
                        val jsonObject = JSONObject(it)
                        if (jsonObject.has("message")) {
                            errorMessage = jsonObject.getString("message")
                        }
                    }
                } catch (_: Exception) {}

                _attendanceMarked.value = AttendanceMarkedState.Error(errorMessage)
                auditLogger.log(AuditLogger.EventType.ATTENDANCE_REJECTED, false, "reason" to errorMessage)
                Toast.makeText(context, "ERROR: $errorMessage", Toast.LENGTH_SHORT).show()
                delay(3000)
                resetMarkScanAttendanceState()
            }
        } catch (e: Exception) {
            // ─── Offline caching: save locally on network failure ───
            if (e is IOException || e.cause is IOException) {
                Log.w(TAG, "Network error — caching attendance offline", e)
                try {
                    val gson = Gson()
                    val embeddingList = verifiedFaceEmbedding?.toList() ?: emptyList<Float>()
                    val pending = PendingAttendanceEntity(
                        token = _currentToken.value ?: "",
                        latitude = cachedLatitude,
                        longitude = cachedLongitude,
                        faceEmbeddingJson = gson.toJson(embeddingList),
                        deviceId = cachedDeviceId,
                        integrityToken = cachedIntegrityToken,
                        auditLogJson = gson.toJson(auditLogger.getEventsAsMapList()),
                        uid = courseUid
                    )
                    val dao = AppDatabase.getInstance(context).pendingAttendanceDao()
                    dao.insert(pending)

                    // Enqueue WorkManager to sync when connectivity is restored
                    val syncRequest = OneTimeWorkRequestBuilder<AttendanceSyncWorker>()
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                    WorkManager.getInstance(context).enqueueUniqueWork(
                        "attendance_sync",
                        ExistingWorkPolicy.KEEP,
                        syncRequest
                    )

                    _attendanceMarked.value = AttendanceMarkedState.Error("Saved offline — will sync when online")
                    auditLogger.log(AuditLogger.EventType.ATTENDANCE_REJECTED, false, "reason" to "offline_cached")
                    Toast.makeText(context, "Attendance saved offline. Will sync when online.", Toast.LENGTH_LONG).show()
                } catch (dbErr: Exception) {
                    Log.e(TAG, "Failed to cache attendance offline", dbErr)
                    _attendanceMarked.value = AttendanceMarkedState.Error(e.message ?: "Network error")
                    Toast.makeText(context, "Network error and offline save failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                _attendanceMarked.value = AttendanceMarkedState.Error(e.message ?: "Unknown Error")
                auditLogger.log(AuditLogger.EventType.ATTENDANCE_REJECTED, false, "reason" to (e.message ?: "unknown"))
                Toast.makeText(context, "ERROR: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            delay(3000)
            resetMarkScanAttendanceState()
        }
    }

    fun resetMarkScanAttendanceState() {
        _attendanceMarked.value = AttendanceMarkedState.Idle
    }



    // ═══════════════════════════════════════
    // SCANNING CONTROL
    // ═══════════════════════════════════════

    @SuppressLint("MissingPermission")
    private fun stopScanningInternal(reason: String) {
        Log.i(TAG, "Stopping scanning: $reason")
        if (!scanStopRequested.compareAndSet(false, true)) return

        handler.removeCallbacks(scanTimeoutTask)
        microVerifyJob?.cancel()

        try {
            if (scanCallback != null) {
                try {
                    scanner?.stopScan(scanCallback)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
        } finally {
            scanCallback = null
            viewModelScope.launch {
                _isScanning.value = false
                // Only update scanResultMessage if attendance was NOT submitted.
                // When attendance IS submitted, the attendanceMarked StateFlow
                // drives the UI state (Success/Error) — writing here would
                // trigger the LaunchedEffect(scanResultMessage) and clobber it.
                if (!attendanceSubmitted.get()) {
                    _scanResultMessage.value = reason
                }
            }
        }
    }

    fun stopScanning() {
        stopScanningInternal("Scanning stopped by user")
    }

    // ═══════════════════════════════════════
    // SECURITY HELPERS
    // ═══════════════════════════════════════

    /**
     * Reset all security layer states (for a new attendance attempt).
     */
    fun resetSecurityState() {
        _tokenVerified.value = false
        _geofenceVerified.value = false
        _faceVerified.value = false
        _integrityVerified.value = false
        _microVerifyProgress.value = 0
        _currentToken.value = null
        verifiedFaceEmbedding = null
        replayProtection.clear()
        auditLogger.clear()
        attendanceSubmitted.set(false)
        scanStopRequested.set(false)
        // Clear cached pre-fetched resources
        cachedFirebaseToken = null
        cachedLatitude = 0.0
        cachedLongitude = 0.0
        cachedIntegrityToken = "integrity_failed"
        cachedDeviceId = "unknown_device"
    }

    /**
     * Get the audit log summary for debugging.
     */
    fun getAuditSummary(): String = auditLogger.getSummary()

    /**
     * Get audit events for backend submission.
     */
    fun getAuditEvents(): List<Map<String, Any>> = auditLogger.getEventsAsMapList()

    // ═══════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════

    private suspend fun getFirebaseToken(): String? {
        return try {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            firebaseUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Firebase token", e)
            null
        }
    }

    fun checkBleCapabilities(context: Context): BleCapabilities {
        val packageManager = context.packageManager
        return BleCapabilities(
            hasBle = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            hasBluetoothAdmin = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
            hasBluetoothAdapter = bluetoothAdapter != null,
            canScan = scanner != null
        )
    }

    fun clearErrorMessage() {
        viewModelScope.launch { _errorMessage.value = null }
    }

    data class BleCapabilities(
        val hasBle: Boolean,
        val hasBluetoothAdmin: Boolean,
        val hasBluetoothAdapter: Boolean,
        val canScan: Boolean
    ) {
        val isFullyCapable: Boolean
            get() = hasBle && hasBluetoothAdmin && hasBluetoothAdapter && canScan
    }

    // AttendanceMarkedState is defined at the top level of this file

    fun byteArrayToHexString(byteArray: ByteArray): String {
        return byteArray.joinToString("") { "%02X".format(it) }
    }

    override fun onCleared() {
        super.onCleared()
        if (_isScanning.value) stopScanning()
        microVerifyJob?.cancel()
        handler.removeCallbacksAndMessages(null)
        auditLogger.clear()
        Log.i(TAG, "ViewModel cleared, resources released")
    }
}