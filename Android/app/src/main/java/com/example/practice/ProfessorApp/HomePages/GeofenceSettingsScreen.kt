package com.example.practice.ProfessorApp.HomePages

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.practice.RequestBodyApi.UpdateGeofenceRequest
import com.example.practice.api.RetrofitInstance
import com.example.practice.features.GeofenceManager
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "GeofenceSettings"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceSettingsScreen(
    navController: NavController,
    joiningCode: String,
    courseName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appApi = RetrofitInstance.professorApi

    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var radiusMeters by remember { mutableStateOf("80") }

    var isLoading by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Location services check
    var isLocationEnabled by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-check location status when screen resumes (e.g. user returns from settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
                isLocationEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                        locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Initial check
    LaunchedEffect(Unit) {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
        isLocationEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    val statusColor by animateColorAsState(
        targetValue = if (isError) Neon_Red else Neon_Green,
        label = "statusColor"
    )

    AnimatedGradientBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // ── Top Bar ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Text_Primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Geofence Settings",
                    color = Text_Primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Course name badge
            Text(
                text = courseName,
                color = Neon_Cyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Location Services Warning ──
            if (!isLocationEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Neon_Red.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Neon_Red.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = "Location Off",
                            tint = Neon_Red,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Location is OFF",
                                color = Neon_Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Please turn on your device location to use geofence features.",
                                color = Text_Secondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }
                        ) {
                            Text("TURN ON", color = Neon_Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Geofence Fields ──
            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Geofence Center Coordinates",
                        color = Text_Primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        placeholder = { Text("e.g. 26.081154") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Neon_Cyan,
                            unfocusedBorderColor = Text_Secondary,
                            focusedLabelColor = Neon_Cyan,
                            unfocusedLabelColor = Text_Secondary,
                            cursorColor = Neon_Cyan,
                            focusedTextColor = Text_Primary,
                            unfocusedTextColor = Text_Primary,
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        placeholder = { Text("e.g. 91.561969") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Neon_Cyan,
                            unfocusedBorderColor = Text_Secondary,
                            focusedLabelColor = Neon_Cyan,
                            unfocusedLabelColor = Text_Secondary,
                            cursorColor = Neon_Cyan,
                            focusedTextColor = Text_Primary,
                            unfocusedTextColor = Text_Primary,
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = radiusMeters,
                        onValueChange = { radiusMeters = it },
                        label = { Text("Radius (meters)") },
                        placeholder = { Text("80") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Neon_Cyan,
                            unfocusedBorderColor = Text_Secondary,
                            focusedLabelColor = Neon_Cyan,
                            unfocusedLabelColor = Text_Secondary,
                            cursorColor = Neon_Cyan,
                            focusedTextColor = Text_Primary,
                            unfocusedTextColor = Text_Primary,
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Use Current Location Button ──
            OutlinedButton(
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {
                        statusMessage = "Location permission not granted"
                        isError = true
                        return@OutlinedButton
                    }

                    isFetchingLocation = true
                    coroutineScope.launch {
                        val geofenceManager = GeofenceManager(context)
                        val location = geofenceManager.getCurrentLocation()
                        isFetchingLocation = false

                        if (location != null) {
                            latitude = String.format("%.6f", location.latitude)
                            longitude = String.format("%.6f", location.longitude)
                            statusMessage = "Location fetched successfully"
                            isError = false
                        } else {
                            statusMessage = "Failed to get location. Try again."
                            isError = true
                        }
                    }
                },
                enabled = !isFetchingLocation && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Neon_Cyan,
                )
            ) {
                if (isFetchingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Neon_Cyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetching Location...")
                } else {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Current Location", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Save Button ──
            GradientButton(
                text = if (isLoading) "Saving..." else "Save Geofence",
                onClick = {
                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()
                    val radius = radiusMeters.toIntOrNull() ?: 80

                    if (lat == null || lng == null) {
                        statusMessage = "Invalid latitude or longitude"
                        isError = true
                        return@GradientButton
                    }
                    if (lat < -90 || lat > 90) {
                        statusMessage = "Latitude must be between -90 and 90"
                        isError = true
                        return@GradientButton
                    }
                    if (lng < -180 || lng > 180) {
                        statusMessage = "Longitude must be between -180 and 180"
                        isError = true
                        return@GradientButton
                    }

                    isLoading = true
                    statusMessage = null

                    coroutineScope.launch {
                        try {
                            val firebaseUser = FirebaseAuth.getInstance().currentUser
                            val token = firebaseUser?.getIdToken(true)?.await()?.token

                            if (token == null) {
                                statusMessage = "Authentication failed"
                                isError = true
                                isLoading = false
                                return@launch
                            }

                            val request = UpdateGeofenceRequest(
                                joiningCode = joiningCode,
                                latitude = lat,
                                longitude = lng,
                                radiusMeters = radius
                            )

                            val response = appApi.updateGeofence("Bearer $token", request)

                            if (response.isSuccessful) {
                                val geofence = response.body()?.geofence
                                statusMessage = "Geofence saved! Center: (${geofence?.latitude}, ${geofence?.longitude}), Radius: ${geofence?.radiusMeters}m"
                                isError = false
                                Toast.makeText(context, "Geofence updated!", Toast.LENGTH_SHORT).show()
                            } else {
                                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                                statusMessage = "Failed: $errorMsg"
                                isError = true
                                Log.e(TAG, "Geofence update failed: ${response.code()} $errorMsg")
                            }
                        } catch (e: Exception) {
                            statusMessage = "Error: ${e.message}"
                            isError = true
                            Log.e(TAG, "Geofence update exception", e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && latitude.isNotBlank() && longitude.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Status Message ──
            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = statusMessage!!,
                        color = statusColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Info Card ──
            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "ℹ️ How Geofencing Works",
                        color = Neon_Yellow,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Students must be within the specified radius of the center point to mark attendance. The system uses GPS to verify their location.",
                        color = Text_Secondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
