package com.example.practice.ResponsesModel

data class FaceVerifyResponse(
    val verified: Boolean,
    val similarity: Float,
    val threshold: Float
)

