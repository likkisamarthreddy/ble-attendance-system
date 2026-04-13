package com.example.practice.ProfessorApp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practice.features.RollingTokenManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BroadcastingViewModel : ViewModel() {
    companion object {
        private const val TAG = "BroadcastingViewModel"
        private const val SCAN_PERIOD = 10000L // 10 seconds
        private const val BROADCAST_PERIOD = 120000L // 2 minutes
    }

    // Bluetooth components
    var bluetoothAdapter: BluetoothAdapter? = null
    var advertiser: BluetoothLeAdvertiser? = null
    var scanner: BluetoothLeScanner? = null
    private val handler = Handler(Looper.getMainLooper())

    // Callbacks
    private var scanCallback: ScanCallback? = null
    private var advertiseCallback: AdvertiseCallback? = null

    // UUIDs
    private val shortServiceUUID = ParcelUuid(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB"))

    // ─── Rolling token state ───
    private var sessionSecret: String = ""
    private var sessionId: String = ""
    private var tokenRotationJob: Job? = null

    private val _currentToken = MutableStateFlow<String>("")
    val currentToken: StateFlow<String> = _currentToken

    private val _currentTimeSlot = MutableStateFlow(0L)
    val currentTimeSlot: StateFlow<Long> = _currentTimeSlot

    private val _tokenTimeRemaining = MutableStateFlow(0L)
    val tokenTimeRemaining: StateFlow<Long> = _tokenTimeRemaining

    // Original data to broadcast (backward-compatible)
    private var customData: String = "NoData"

    // UI state flows
    private val _statusMessage = MutableStateFlow<String>("")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising

    private val _hasNetworkAccess = MutableStateFlow(false)
    val hasNetworkAccess: StateFlow<Boolean> = _hasNetworkAccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * Initialize the ViewModel with the Bluetooth adapter
     */
    fun initialize(adapter: BluetoothAdapter?) {
        bluetoothAdapter = adapter
        if (adapter != null) {
            advertiser = adapter.bluetoothLeAdvertiser
            scanner = adapter.bluetoothLeScanner
            _statusMessage.value = if (isBleSupported()) "BLE Ready" else "BLE Not Supported"
        } else {
            _statusMessage.value = "Bluetooth Not Available"
        }
        Log.i(TAG, "BLE Initialized: Adapter=${adapter != null}, Advertiser=${advertiser != null}")
    }

    /**
     * Set session details from backend (call before broadcasting).
     * In production, these come from POST /session/start API.
     */
    fun setSessionInfo(id: String, secret: String) {
        sessionId = id
        sessionSecret = secret
    }

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

    fun setCustomData(data: String, context: Context) {
        if (data.isNotEmpty() && data != customData) {
            customData = data
            if (_isAdvertising.value) {
                stopAdvertising()
                startAdvertising(context)
            }
        }
    }

    fun isBleSupported(): Boolean = bluetoothAdapter != null && advertiser != null && scanner != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /**
     * Start BLE advertising with rolling token rotation (every 7 seconds).
     */
    fun startAdvertising(context: Context) {
        if (!hasRequiredBluetoothPermissions(context)) {
            _statusMessage.value = "Missing required Bluetooth permissions"
            return
        }
        if (!isBluetoothEnabled()) {
            _statusMessage.value = "Please enable Bluetooth first"
            return
        }
        if (advertiser == null) {
            _statusMessage.value = "BLE Advertising not supported on this device"
            return
        }
        if (_isAdvertising.value) {
            _statusMessage.value = "Already advertising"
            return
        }

        // Start the actual advertising with current token
        startAdvertisingWithToken(context)

        // Start token rotation coroutine (re-advertise every 7s with new token)
        startTokenRotation(context)
    }

    /**
     * Start or restart BLE advertising with the current HMAC token.
     */
    private fun startAdvertisingWithToken(context: Context) {
        try {
            // Stop existing advertising if any
            if (advertiseCallback != null) {
                try { advertiser?.stopAdvertising(advertiseCallback) } catch (_: SecurityException) {}
                advertiseCallback = null
            }

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setTimeout(0)
                .build()

            // Generate 8-byte HMAC rolling token only (fits BLE service data limit)
            val tokenBytes = if (sessionSecret.isNotEmpty()) {
                RollingTokenManager.generateTokenBytes(sessionSecret)
            } else {
                hexStringToByteArray(customData)
            }

            val tokenHex = tokenBytes.joinToString("") { "%02x".format(it) }
            _currentToken.value = tokenHex
            _currentTimeSlot.value = RollingTokenManager.getCurrentTimeSlot()

            Log.d(TAG, "Broadcasting 8-byte token: $tokenHex")

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(shortServiceUUID)
                .addServiceData(shortServiceUUID, tokenBytes)
                .build()

            advertiseCallback = object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    super.onStartSuccess(settingsInEffect)
                    viewModelScope.launch {
                        _isAdvertising.value = true
                        _statusMessage.value = "Broadcasting token: $tokenHex"
                    }
                    Log.i(TAG, "Advertising started with token: $tokenHex")
                }

                override fun onStartFailure(errorCode: Int) {
                    super.onStartFailure(errorCode)
                    val errorMessage = when (errorCode) {
                        ADVERTISE_FAILED_ALREADY_STARTED -> "Already advertising"
                        ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertising data too large"
                        ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Advertising not supported"
                        ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                        ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                        else -> "Unknown error: $errorCode"
                    }

                    handler.post {
                        try {
                            Toast.makeText(context.applicationContext, "Advertising failed: $errorMessage", Toast.LENGTH_LONG).show()
                        } catch (_: Exception) {}
                    }

                    viewModelScope.launch {
                        _errorMessage.value = "Advertising failed: $errorMessage"
                        _isAdvertising.value = false
                        _statusMessage.value = "Advertising failed: $errorMessage"
                    }
                    Log.e(TAG, "Advertising failed: $errorMessage")
                }
            }

            try {
                advertiser?.startAdvertising(settings, data, advertiseCallback)
                _statusMessage.value = "Starting broadcast..."
            } catch (secEx: SecurityException) {
                _statusMessage.value = "Permission denied: ${secEx.message}"
            }
        } catch (e: Exception) {
            _statusMessage.value = "Error starting advertising: ${e.message}"
            Log.e(TAG, "Error starting advertising", e)
        }
    }

    /**
     * Token rotation coroutine — re-generates and re-advertises every 3 seconds.
     */
    private fun startTokenRotation(context: Context) {
        tokenRotationJob?.cancel()
        tokenRotationJob = viewModelScope.launch {
            while (_isAdvertising.value || !scanStopRequested()) {
                _tokenTimeRemaining.value = RollingTokenManager.getTimeRemainingInSlot()

                // Wait for current slot to expire
                val remaining = RollingTokenManager.getTimeRemainingInSlot()
                delay(remaining)

                // Re-advertise with new token
                if (_isAdvertising.value) {
                    startAdvertisingWithToken(context)
                    Log.d(TAG, "Token rotated to new slot: ${RollingTokenManager.getCurrentTimeSlot()}")
                } else {
                    break
                }
            }
        }

        // Also run a fast countdown updater
        viewModelScope.launch {
            while (_isAdvertising.value) {
                _tokenTimeRemaining.value = RollingTokenManager.getTimeRemainingInSlot()
                delay(100)
            }
        }
    }

    private fun scanStopRequested(): Boolean = false // Broadcasting doesn't use scan stop

    fun stopAdvertising() {
        tokenRotationJob?.cancel()
        tokenRotationJob = null

        if (!_isAdvertising.value) {
            _statusMessage.value = "Not currently advertising"
            return
        }

        try {
            advertiseCallback?.let { callback ->
                try { advertiser?.stopAdvertising(callback) }
                catch (_: SecurityException) {}
                viewModelScope.launch {
                    _isAdvertising.value = false
                    _statusMessage.value = "Broadcasting stopped"
                }
                Log.i(TAG, "Advertising stopped")
            }
            advertiseCallback = null
        } catch (e: Exception) {
            _statusMessage.value = "Error stopping advertising: ${e.message}"
            Log.e(TAG, "Error stopping advertising", e)
        }
    }

    fun checkBleCapabilities(context: Context): BleCapabilities {
        val packageManager = context.packageManager
        return BleCapabilities(
            hasBle = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            hasBluetoothAdmin = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
            hasBluetoothAdapter = bluetoothAdapter != null,
            canAdvertise = advertiser != null,
            canScan = scanner != null
        )
    }

    fun clearErrorMessage() {
        viewModelScope.launch { _errorMessage.value = null }
    }

    fun hexStringToByteArray(hexString: String): ByteArray {
        val len = hexString.length
        val result = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            val firstDigit = Character.digit(hexString[i], 16)
            val secondDigit = Character.digit(hexString[i + 1], 16)
            if (firstDigit == -1 || secondDigit == -1) {
                throw IllegalArgumentException("Invalid hex string")
            }
            result[i / 2] = ((firstDigit shl 4) + secondDigit).toByte()
        }
        return result
    }

    data class BleCapabilities(
        val hasBle: Boolean,
        val hasBluetoothAdmin: Boolean,
        val hasBluetoothAdapter: Boolean,
        val canAdvertise: Boolean,
        val canScan: Boolean
    ) {
        val isFullyCapable: Boolean
            get() = hasBle && hasBluetoothAdmin && hasBluetoothAdapter && canAdvertise && canScan
    }

    override fun onCleared() {
        super.onCleared()
        tokenRotationJob?.cancel()
        if (_isAdvertising.value) stopAdvertising()
        handler.removeCallbacksAndMessages(null)
        Log.i(TAG, "ViewModel cleared, resources released")
    }
}