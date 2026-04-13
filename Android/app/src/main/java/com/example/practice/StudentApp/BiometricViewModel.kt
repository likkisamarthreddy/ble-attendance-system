package com.example.practice.StudentApp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BiometricViewModel : ViewModel() {
    private val _authenticated = MutableStateFlow(false)
    val authenticated: StateFlow<Boolean> = _authenticated.asStateFlow()

    private val _biometricState = MutableStateFlow<BiometricState>(BiometricState.Initial)
    val biometricState: StateFlow<BiometricState> = _biometricState.asStateFlow()

    private val _wasInBackground = MutableStateFlow(false)
    val wasInBackground = _wasInBackground.asStateFlow()

    fun setAuthenticated(value: Boolean) {
        _authenticated.value = value
    }

    fun setBiometricState(state: BiometricState) {
        viewModelScope.launch {
            _biometricState.emit(state)
        }
    }

    fun resetBiometricState() {
        viewModelScope.launch {
            _biometricState.emit(BiometricState.Initial)
        }
    }

    // Add these new functions
    fun setWasInBackground(value: Boolean) {
        _wasInBackground.value = value
    }

    // Function to handle app going to background
    fun handleAppBackground() {
        // Set the background flag
        _wasInBackground.value = true
        // Invalidate authentication when app goes to background
        _authenticated.value = false
    }

    // Function to handle app coming to foreground
    fun handleAppForeground(): Boolean {
        val needsAuthentication = _wasInBackground.value || !_authenticated.value
        // Reset the background flag
        _wasInBackground.value = false
        return needsAuthentication
    }

}

sealed class BiometricState {
    object Initial : BiometricState()
    object Success : BiometricState()
    object Failed : BiometricState()
    object HardwareUnavailable : BiometricState()
    object NoHardware : BiometricState()
    object NotEnrolled : BiometricState()
    data class Error(val message: String) : BiometricState()
}
