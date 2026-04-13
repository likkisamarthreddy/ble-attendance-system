package com.example.practice.ResponsesModel

data class UpdateGeofenceResponse(
    val message: String,
    val geofence: GeofenceData?
)

data class GeofenceData(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int
)

