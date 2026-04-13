package com.example.practice.features

import android.util.Log
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.practice.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "FaceVerify"
private const val STABILITY_FRAMES = 3
private const val MAX_ATTEMPTS = 5
private const val VERIFICATION_SAMPLES = 3  // Best-of-3: capture 3 embeddings, send best

enum class LivenessChallenge { SMILE, BLINK, TURN_LEFT, TURN_RIGHT }

@Composable
fun FaceVerificationScreen(
    onVerificationComplete: (embedding: FloatArray) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var faceDetected by remember { mutableStateOf(false) }
    var verificationComplete by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Detecting face...") }
    var stateColor by remember { mutableStateOf(Neon_Red) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableIntStateOf(0) }

    val faceRecognitionManager = remember { FaceRecognitionManager(context) }
    var modelReady by remember { mutableStateOf(false) }

    val processing = remember { AtomicBoolean(false) }
    val done = remember { AtomicBoolean(false) }
    val stableFrames = remember { AtomicInteger(0) }
    val attemptCount = remember { AtomicInteger(0) }
    val capturedEmbeddings = remember { mutableListOf<FloatArray>() }
    var capturedCount by remember { mutableIntStateOf(0) }

    // Advanced Liveness states
    var livenessConfirmed by remember { mutableStateOf(false) }
    
    // Generate a sequence of 2 random challenges
    val livenessChallenges = remember {
        val allChallenges = LivenessChallenge.values().toList()
        val challenges = mutableListOf<LivenessChallenge>()
        // Pick 2 random unique challenges
        while (challenges.size < 2) {
            val randomChallenge = allChallenges.random()
            if (!challenges.contains(randomChallenge)) {
                challenges.add(randomChallenge)
            }
        }
        challenges
    }
    var currentChallengeIndex by remember { mutableIntStateOf(0) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.3f)
            .build()
        FaceDetection.getClient(options)
    }

    DisposableEffect(Unit) {
        val success = faceRecognitionManager.initialize()
        modelReady = success
        Log.d(TAG, "=== MODEL INIT: success=$success ===")
        onDispose {
            faceRecognitionManager.close()
            faceDetector.close()
            cameraExecutor.shutdown()
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = modifier.fillMaxSize().background(Background_Deep)) {

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp).align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Identity Verification", color = Text_Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, "Cancel", tint = Text_Primary)
            }
        }

        // Camera with glowing border
        Box(
            modifier = Modifier.align(Alignment.Center).size(340.dp).padding(bottom = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(320.dp).drawBehind {
                    rotate(degrees = angle) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(Color.Transparent, stateColor.copy(alpha = 0.5f), stateColor)
                            ),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            )

            Box(modifier = Modifier.size(300.dp).clip(CircleShape).background(Color.Black)) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        if (done.get() || !modelReady) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }
                                        if (processing.get()) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        try {
                                            // Step 1: Get full rotated bitmap
                                            val fullBitmap = getRotatedBitmap(imageProxy)
                                            if (fullBitmap == null) {
                                                imageProxy.close()
                                                return@setAnalyzer
                                            }

                                            // Step 2: Detect face
                                            @OptIn(ExperimentalGetImage::class)
                                            val mediaImage = imageProxy.image
                                            if (mediaImage == null) {
                                                imageProxy.close()
                                                return@setAnalyzer
                                            }
                                            val inputImage = InputImage.fromMediaImage(
                                                mediaImage, imageProxy.imageInfo.rotationDegrees
                                            )

                                            faceDetector.process(inputImage)
                                                .addOnSuccessListener { faces ->
                                                    // Bug #3: Multiple faces check
                                                    if (faces.size > 1) {
                                                        stableFrames.set(0)
                                                        coroutineScope.launch(Dispatchers.Main) {
                                                            faceDetected = false
                                                            livenessConfirmed = false
                                                            statusMessage = "Only one face allowed in frame"
                                                            stateColor = Neon_Red
                                                        }
                                                        return@addOnSuccessListener
                                                    }

                                                    if (faces.isNotEmpty()) {
                                                        val face = faces[0]

                                                        // Bug #2 + #4: Validate face quality (landmarks, distance)
                                                        val preValidation = validateFaceQuality(
                                                            faces = faces,
                                                            face = face,
                                                            imageWidth = fullBitmap.width,
                                                            imageHeight = fullBitmap.height
                                                        )
                                                        if (!preValidation.isValid) {
                                                            stableFrames.set(0)
                                                            coroutineScope.launch(Dispatchers.Main) {
                                                                faceDetected = true
                                                                livenessConfirmed = false
                                                                statusMessage = preValidation.message
                                                                stateColor = Neon_Red
                                                            }
                                                            return@addOnSuccessListener
                                                        }

                                                        // --- Advanced Liveness Detection ---
                                                        if (!livenessConfirmed) {
                                                            if (currentChallengeIndex < livenessChallenges.size) {
                                                                val currentChallenge = livenessChallenges[currentChallengeIndex]
                                                                
                                                                val leftEyeOpen = face.leftEyeOpenProbability ?: 1.0f
                                                                val rightEyeOpen = face.rightEyeOpenProbability ?: 1.0f
                                                                val smiling = face.smilingProbability ?: 0.0f
                                                                val headEulerY = face.headEulerAngleY
                                                                
                                                                var passedCurrent = false
                                                                when (currentChallenge) {
                                                                    LivenessChallenge.SMILE -> {
                                                                        if (smiling > 0.6f) passedCurrent = true
                                                                    }
                                                                    LivenessChallenge.BLINK -> {
                                                                        if (leftEyeOpen < 0.3f && rightEyeOpen < 0.3f) passedCurrent = true
                                                                    }
                                                                    LivenessChallenge.TURN_LEFT -> {
                                                                        if (headEulerY < -20f) passedCurrent = true
                                                                    }
                                                                    LivenessChallenge.TURN_RIGHT -> {
                                                                        if (headEulerY > 20f) passedCurrent = true
                                                                    }
                                                                }

                                                                if (passedCurrent) {
                                                                    Log.d(TAG, "Passed challenge: $currentChallenge")
                                                                    currentChallengeIndex++
                                                                    
                                                                    if (currentChallengeIndex >= livenessChallenges.size) {
                                                                        livenessConfirmed = true
                                                                        Log.d(TAG, "All liveness challenges passed!")
                                                                    } else {
                                                                        // Wait for next frame before evaluating next challenge
                                                                        stableFrames.set(0)
                                                                        return@addOnSuccessListener
                                                                    }
                                                                } else {
                                                                    // Challenge not yet met
                                                                    stableFrames.set(0)
                                                                    coroutineScope.launch(Dispatchers.Main) {
                                                                        faceDetected = true
                                                                        stateColor = Neon_Yellow
                                                                        statusMessage = "Liveness ${currentChallengeIndex + 1}/${livenessChallenges.size}: " + when(currentChallenge) {
                                                                            LivenessChallenge.SMILE -> "Please smile"
                                                                            LivenessChallenge.BLINK -> "Please blink"
                                                                            LivenessChallenge.TURN_LEFT -> "Turn head slightly Left"
                                                                            LivenessChallenge.TURN_RIGHT -> "Turn head slightly Right"
                                                                        }
                                                                    }
                                                                    return@addOnSuccessListener
                                                                }
                                                            }
                                                        } else {
                                                            // Liveness confirmed, ensure head is back to center before capturing
                                                            val headEulerY = face.headEulerAngleY
                                                            if (Math.abs(headEulerY) > 10f) {
                                                                stableFrames.set(0)
                                                                coroutineScope.launch(Dispatchers.Main) {
                                                                    statusMessage = "Please look straight ahead"
                                                                    stateColor = Neon_Yellow
                                                                }
                                                                return@addOnSuccessListener
                                                            }
                                                        }
                                                        // --- End Advanced Liveness Detection ---

                                                        val currentStable = stableFrames.incrementAndGet()

                                                        coroutineScope.launch(Dispatchers.Main) {
                                                            faceDetected = true
                                                            stateColor = Neon_Yellow
                                                            statusMessage = if (currentStable < STABILITY_FRAMES)
                                                                "Hold still... (${currentStable}/$STABILITY_FRAMES)"
                                                            else
                                                                "Scanning face..."
                                                        }

                                                        if (currentStable >= STABILITY_FRAMES &&
                                                            !processing.getAndSet(true) && !done.get()) {

                                                            cameraExecutor.execute {
                                                                try {
                                                                    // Step 3: CROP face
                                                                    val croppedFace = cropFace(fullBitmap, face, imageProxy.imageInfo.rotationDegrees)
                                                                    if (croppedFace == null) {
                                                                        Log.e(TAG, "Face crop failed")
                                                                        coroutineScope.launch(Dispatchers.Main) {
                                                                            retryCount++
                                                                            errorMessage = "Face crop failed"
                                                                        }
                                                                        return@execute
                                                                    }

                                                                    Log.d(TAG, "Cropped face: ${croppedFace.width}x${croppedFace.height}")

                                                                    // Bug #1: Brightness check on cropped face
                                                                    val brightnessCheck = validateFaceQuality(
                                                                        faces = faces,
                                                                        face = face,
                                                                        imageWidth = fullBitmap.width,
                                                                        imageHeight = fullBitmap.height,
                                                                        croppedFaceBitmap = croppedFace
                                                                    )
                                                                    if (!brightnessCheck.isValid) {
                                                                        Log.w(TAG, "Quality check failed: ${brightnessCheck.message}")
                                                                        stableFrames.set(0)
                                                                        coroutineScope.launch(Dispatchers.Main) {
                                                                            statusMessage = brightnessCheck.message
                                                                            stateColor = Neon_Red
                                                                        }
                                                                        return@execute
                                                                    }

                                                                    // Step 4: Generate embedding from CROPPED face
                                                                    val embedding = faceRecognitionManager.generateEmbedding(croppedFace)

                                                                    if (embedding != null && embedding.size == faceRecognitionManager.embeddingSize) {
                                                                        capturedEmbeddings.add(embedding.copyOf())
                                                                        val count = capturedEmbeddings.size
                                                                        Log.d(TAG, "=== EMBEDDING $count/$VERIFICATION_SAMPLES (attempt ${attemptCount.get() + 1}) ===")

                                                                        if (count >= VERIFICATION_SAMPLES) {
                                                                            // Best-of-3: pick the most consistent embedding
                                                                            val bestEmbedding = pickBestEmbedding(capturedEmbeddings, faceRecognitionManager)
                                                                            done.set(true)
                                                                            coroutineScope.launch(Dispatchers.Main) {
                                                                                verificationComplete = true
                                                                                statusMessage = "Face captured — verifying..."
                                                                                stateColor = Neon_Green
                                                                                errorMessage = null
                                                                                delay(300)
                                                                                onVerificationComplete(bestEmbedding)
                                                                            }
                                                                        } else {
                                                                            // Need more samples
                                                                            stableFrames.set(0)
                                                                            coroutineScope.launch(Dispatchers.Main) {
                                                                                capturedCount = count
                                                                                statusMessage = "Scanning... ($count/$VERIFICATION_SAMPLES)"
                                                                            }
                                                                        }
                                                                    } else {
                                                                        val attempt = attemptCount.incrementAndGet()
                                                                        Log.e(TAG, "Embedding failed (attempt $attempt/$MAX_ATTEMPTS)")
                                                                        if (attempt >= MAX_ATTEMPTS) {
                                                                            coroutineScope.launch(Dispatchers.Main) {
                                                                                retryCount++
                                                                                errorMessage = faceRecognitionManager.lastError ?: "Retry #$retryCount..."
                                                                            }
                                                                        } else {
                                                                            // Auto-retry: reset stability counter silently
                                                                            stableFrames.set(0)
                                                                            coroutineScope.launch(Dispatchers.Main) {
                                                                                statusMessage = "Scanning face... (attempt ${attempt + 1})"
                                                                            }
                                                                        }
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Log.e(TAG, "Exception: ${e.message}", e)
                                                                    coroutineScope.launch(Dispatchers.Main) {
                                                                        retryCount++
                                                                        errorMessage = "Error: ${e.message}"
                                                                    }
                                                                } finally {
                                                                    processing.set(false)
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        stableFrames.set(0)
                                                        coroutineScope.launch(Dispatchers.Main) {
                                                            faceDetected = false
                                                            livenessConfirmed = false
                                                            statusMessage = "Detecting face..."
                                                            stateColor = Neon_Red
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e(TAG, "Detection failed: ${e.message}")
                                                    stableFrames.set(0)
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Analyzer exception: ${e.message}", e)
                                            imageProxy.close()
                                        }
                                    }
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer
                                )
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Bottom status
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (verificationComplete) {
                Icon(Icons.Default.Check, "Done", tint = Neon_Green, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = statusMessage, color = stateColor,
                fontWeight = FontWeight.Bold, fontSize = 20.sp,
                textAlign = TextAlign.Center, letterSpacing = 1.sp
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            if (!modelReady) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("⚠️ Face model failed to load!", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Pick the most consistent embedding from a list.
 * Computes average similarity of each embedding to all others.
 * The one with the highest average is the most representative.
 */
private fun pickBestEmbedding(
    embeddings: List<FloatArray>,
    manager: FaceRecognitionManager
): FloatArray {
    if (embeddings.size <= 1) return embeddings.first()

    var bestIndex = 0
    var bestAvgSim = -1f

    for (i in embeddings.indices) {
        var totalSim = 0f
        var count = 0
        for (j in embeddings.indices) {
            if (i != j) {
                totalSim += manager.compareFaces(embeddings[i], embeddings[j])
                count++
            }
        }
        val avgSim = if (count > 0) totalSim / count else 0f
        if (avgSim > bestAvgSim) {
            bestAvgSim = avgSim
            bestIndex = i
        }
    }

    Log.d(TAG, "Best-of-${embeddings.size}: picked embedding #$bestIndex (avgSim=${"%.3f".format(bestAvgSim)})")
    return embeddings[bestIndex]
}
