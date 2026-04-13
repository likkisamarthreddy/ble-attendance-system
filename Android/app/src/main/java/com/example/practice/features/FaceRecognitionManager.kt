package com.example.practice.features

import android.content.Context
import android.util.Log
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

private const val TAG = "FaceRecognition"

/**
 * Face Recognition Manager — TFLite MobileFaceNet for identity matching.
 * Auto-detects model output dimensions at initialization time.
 */
class FaceRecognitionManager(private val context: Context) {

    companion object {
        private const val MODEL_FILE = "mobilefacenet.tflite"
        private const val INPUT_SIZE = 112
        const val SIMILARITY_THRESHOLD = 0.65f
    }

    private var interpreter: Interpreter? = null
    private var inputShape: IntArray? = null   // e.g. [1, 112, 112, 3]
    private var outputShape: IntArray? = null  // e.g. [1, 192] or [1, 128]
    var embeddingSize: Int = 0                 // auto-detected from model
        private set
    var lastError: String? = null
        private set

    fun initialize(): Boolean {
        return try {
            val model = loadModelFile()
            val options = Interpreter.Options().apply { setNumThreads(4) }
            val interp = Interpreter(model, options)
            interpreter = interp

            // Auto-detect input/output shapes from the model
            inputShape = interp.getInputTensor(0).shape()
            outputShape = interp.getOutputTensor(0).shape()
            embeddingSize = outputShape!!.last()

            Log.d(TAG, "=== MODEL LOADED ===")
            Log.d(TAG, "  Input shape:  ${inputShape!!.toList()}")
            Log.d(TAG, "  Output shape: ${outputShape!!.toList()}")
            Log.d(TAG, "  Embedding size: $embeddingSize")
            Log.d(TAG, "  Input dtype: ${interp.getInputTensor(0).dataType()}")
            Log.d(TAG, "  Output dtype: ${interp.getOutputTensor(0).dataType()}")

            lastError = null
            true
        } catch (e: Exception) {
            lastError = e.message
            Log.e(TAG, "FAILED to load model: ${e.message}", e)
            false
        }
    }

    /**
     * Generate face embedding from a cropped face bitmap.
     * Output size is auto-detected from the model.
     */
    fun generateEmbedding(faceBitmap: Bitmap): FloatArray? {
        val interp = interpreter
        if (interp == null) {
            lastError = "Interpreter is NULL"
            return null
        }
        if (embeddingSize <= 0) {
            lastError = "Invalid embedding size: $embeddingSize"
            return null
        }

        return try {
            // Normalize lighting before embedding (histogram equalization)
            val normalized = normalizeLighting(faceBitmap)
            val resized = Bitmap.createScaledBitmap(normalized, INPUT_SIZE, INPUT_SIZE, true)
            val inputBuffer = preprocessBitmap(resized)

            // Use AUTO-DETECTED output size
            val outputArray = Array(1) { FloatArray(embeddingSize) }
            interp.run(inputBuffer, outputArray)

            val result = l2Normalize(outputArray[0])
            lastError = null
            Log.d(TAG, "Embedding OK: size=${result.size}, first3=[${result[0]}, ${result[1]}, ${result[2]}]")
            result
        } catch (e: Exception) {
            lastError = "Inference error: ${e.message}"
            Log.e(TAG, "generateEmbedding FAILED: ${e.message}", e)
            null
        }
    }

    fun compareFaces(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size) return 0f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            norm1 += embedding1[i] * embedding1[i]
            norm2 += embedding2[i] * embedding2[i]
        }

        val denominator = sqrt(norm1) * sqrt(norm2)
        if (denominator == 0f) return 0f

        return (dotProduct / denominator + 1f) / 2f
    }

    fun isSamePerson(embedding1: FloatArray, embedding2: FloatArray): Boolean {
        return compareFaces(embedding1, embedding2) >= SIMILARITY_THRESHOLD
    }

    fun averageEmbeddings(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) return FloatArray(embeddingSize)
        val size = embeddings[0].size
        val average = FloatArray(size)
        for (embedding in embeddings) {
            for (i in embedding.indices) average[i] += embedding[i]
        }
        for (i in average.indices) average[i] /= embeddings.size.toFloat()
        return l2Normalize(average)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    // ─── Private helpers ──────────────────────────

    private fun loadModelFile(): MappedByteBuffer {
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16 and 0xFF) / 127.5f) - 1f)
            buffer.putFloat(((pixel shr 8 and 0xFF) / 127.5f) - 1f)
            buffer.putFloat(((pixel and 0xFF) / 127.5f) - 1f)
        }

        buffer.rewind()
        return buffer
    }

    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sumSquared = 0f
        for (value in embedding) sumSquared += value * value
        val norm = sqrt(sumSquared)
        if (norm > 0) {
            for (i in embedding.indices) embedding[i] /= norm
        }
        return embedding
    }
}
