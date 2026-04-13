package com.example.practice.RequestBodyApi

data class FaceEmbeddingRequest(
    val faceEmbedding: List<Float>
)

data class FaceMultiEmbeddingRequest(
    val faceEmbeddings: List<List<Float>>,
    val profilePicture: String? = null
)
