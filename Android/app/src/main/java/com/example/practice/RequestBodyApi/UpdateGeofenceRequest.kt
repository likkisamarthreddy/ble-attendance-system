package com.example.practice.RequestBodyApi

data class UpdateGeofenceRequest(
    val joiningCode: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 80
)
