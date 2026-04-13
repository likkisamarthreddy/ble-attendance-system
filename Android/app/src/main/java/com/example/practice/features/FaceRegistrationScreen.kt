package com.example.practice.features

import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.practice.ui.components.*
import com.example.practice.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val TAG = "FaceReg"
private const val STABILITY_FRAMES = 3

// ── Guided Registration Steps ──
data class RegistrationStep(
    val instruction: String,
    val emoji: String,
    val validatePose: (Face) -> Boolean
)

private val REGISTRATION_STEPS = listOf(
    RegistrationStep("Look straight at the camera", "😐") { face ->
        abs(face.headEulerAngleY) < 15f && abs(face.headEulerAngleZ) < 15f
    },
    RegistrationStep("Turn your head slightly left", "👈") { face ->
        face.headEulerAngleY > 12f && face.headEulerAngleY < 45f
    },
    RegistrationStep("Turn your head slightly right", "👉") { face ->
        face.headEulerAngleY < -12f && face.headEulerAngleY > -45f
    },
    RegistrationStep("Look straight again", "😐") { face ->
        abs(face.headEulerAngleY) < 15f && abs(face.headEulerAngleZ) < 15f
    },
    RegistrationStep("Blink your eyes", "😉") { face ->
        val leftEye = face.leftEyeOpenProbability ?: 1f
        val rightEye = face.rightEyeOpenProbability ?: 1f
        // Detect blink: at least one eye closed
        leftEye < 0.3f || rightEye < 0.3f
    }
)

@Composable
fun FaceRegistrationScreen(
    onRegistrationComplete: (embeddings: List<FloatArray>, profilePicture: String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val faceRecognitionManager = remember { FaceRecognitionManager(context) }

    var faceDetected by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Position your face in the frame") }
    var registrationDone by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableIntStateOf(0) }
    var modelReady by remember { mutableStateOf(false) }

    val processing = remember { AtomicBoolean(false) }
    val done = remember { AtomicBoolean(false) }
    val stableFrames = remember { AtomicInteger(0) }
    val capturedEmbeddings = remember { mutableListOf<FloatArray>() }
    var profilePictureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Guided step tracking
    var currentStep by remember { mutableIntStateOf(0) }
    var poseReady by remember { mutableStateOf(false) }
    var blinkDetected by remember { mutableStateOf(false) }
    // For blink step: need to see eyes open first, then closed
    var sawEyesOpen by remember { mutableStateOf(false) }

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

    Box(modifier = modifier.fillMaxSize()) {
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
                                    val fullBitmap = getRotatedBitmap(imageProxy)
                                    if (fullBitmap == null) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }

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
                                            // Bug #3: Multiple faces
                                            if (faces.size > 1) {
                                                stableFrames.set(0)
                                                coroutineScope.launch(Dispatchers.Main) {
                                                    faceDetected = false
                                                    statusMessage = "Only one face allowed in frame"
                                                }
                                                return@addOnSuccessListener
                                            }

                                            if (faces.isNotEmpty()) {
                                                val face = faces[0]

                                                // Basic quality checks (distance, landmarks)
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
                                                        statusMessage = preValidation.message
                                                        poseReady = false
                                                    }
                                                    return@addOnSuccessListener
                                                }

                                                // ── Guided step: check pose ──
                                                val step = REGISTRATION_STEPS[currentStep]
                                                val isPoseValid = step.validatePose(face)

                                                // Special handling for blink step
                                                if (currentStep == 4) {
                                                    val leftEye = face.leftEyeOpenProbability ?: 1f
                                                    val rightEye = face.rightEyeOpenProbability ?: 1f
                                                    if (leftEye > 0.7f && rightEye > 0.7f) {
                                                        sawEyesOpen = true
                                                    }
                                                }

                                                val blinkOk = if (currentStep == 4) sawEyesOpen && isPoseValid else isPoseValid

                                                if (!blinkOk) {
                                                    stableFrames.set(0)
                                                    coroutineScope.launch(Dispatchers.Main) {
                                                        faceDetected = true
                                                        poseReady = false
                                                        statusMessage = step.emoji + " " + step.instruction
                                                    }
                                                    return@addOnSuccessListener
                                                }

                                                // Pose is correct — count stable frames
                                                val currentStable = stableFrames.incrementAndGet()

                                                coroutineScope.launch(Dispatchers.Main) {
                                                    faceDetected = true
                                                    poseReady = true
                                                    statusMessage = if (currentStable < STABILITY_FRAMES)
                                                        "✓ Hold still... (${currentStable}/$STABILITY_FRAMES)"
                                                    else
                                                        "Capturing..."
                                                }

                                                // Capture after stable frames
                                                if (currentStable >= STABILITY_FRAMES &&
                                                    !processing.getAndSet(true) && !done.get()) {

                                                    cameraExecutor.execute {
                                                        try {
                                                            val croppedFace = cropFace(fullBitmap, face, imageProxy.imageInfo.rotationDegrees)
                                                            if (croppedFace == null) {
                                                                Log.e(TAG, "Face crop failed")
                                                                coroutineScope.launch(Dispatchers.Main) {
                                                                    retryCount++
                                                                    errorMessage = "Face crop failed"
                                                                }
                                                                return@execute
                                                            }

                                                            // Brightness check
                                                            val brightnessCheck = validateFaceQuality(
                                                                faces = faces,
                                                                face = face,
                                                                imageWidth = fullBitmap.width,
                                                                imageHeight = fullBitmap.height,
                                                                croppedFaceBitmap = croppedFace
                                                            )
                                                            if (!brightnessCheck.isValid) {
                                                                stableFrames.set(0)
                                                                coroutineScope.launch(Dispatchers.Main) {
                                                                    statusMessage = brightnessCheck.message
                                                                    poseReady = false
                                                                }
                                                                return@execute
                                                            }

                                                            // Generate embedding (skip for blink step — no embedding needed)
                                                            if (currentStep == 4) {
                                                                // Blink confirmed — we're done! Send ALL individual embeddings
                                                                done.set(true)
                                                                Log.d(TAG, "=== ALL 5 STEPS DONE — ${capturedEmbeddings.size} EMBEDDINGS READY ===")
                                                                // Convert profile picture to Base64
                                                                val profilePicBase64 = profilePictureBitmap?.let { bmp ->
                                                                    try {
                                                                        val baos = ByteArrayOutputStream()
                                                                        bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                                                        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                                                                    } catch (e: Exception) {
                                                                        Log.e(TAG, "Failed to encode profile picture: ${e.message}")
                                                                        null
                                                                    }
                                                                }
                                                                coroutineScope.launch(Dispatchers.Main) {
                                                                    registrationDone = true
                                                                    statusMessage = "✅ Registration complete!"
                                                                    errorMessage = null
                                                                    delay(500)
                                                                    onRegistrationComplete(capturedEmbeddings.toList(), profilePicBase64)
                                                                }
                                                                return@execute
                                                            }

                                                            val embedding = faceRecognitionManager.generateEmbedding(croppedFace)

                                                            if (embedding != null && embedding.size == faceRecognitionManager.embeddingSize) {
                                                                capturedEmbeddings.add(embedding.copyOf())
                                                                // Capture profile picture during step 0 ("look straight")
                                                                if (currentStep == 0 && profilePictureBitmap == null) {
                                                                    profilePictureBitmap = croppedFace.copy(Bitmap.Config.ARGB_8888, false)
                                                                }
                                                                val count = capturedEmbeddings.size
                                                                Log.d(TAG, "=== STEP ${currentStep + 1}/5 CAPTURED (embedding $count) ===")

                                                                coroutineScope.launch(Dispatchers.Main) {
                                                                    // Move to next step
                                                                    currentStep++
                                                                    poseReady = false
                                                                    sawEyesOpen = false
                                                                    stableFrames.set(0)
                                                                    val nextStep = REGISTRATION_STEPS[currentStep]
                                                                    statusMessage = nextStep.emoji + " " + nextStep.instruction
                                                                }
                                                            } else {
                                                                Log.e(TAG, "Embedding failed")
                                                                stableFrames.set(0)
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e(TAG, "Processing exception: ${e.message}", e)
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
                                                    poseReady = false
                                                    if (!registrationDone) {
                                                        statusMessage = "Position your face in the frame"
                                                    }
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Face detection failed: ${e.message}")
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

        // Gradients
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(DarkBackground.copy(alpha = 0.85f), Color.Transparent))))
        Box(modifier = Modifier.fillMaxWidth().height(280.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, DarkBackground.copy(alpha = 0.95f)))))

        // Face frame
        Box(
            modifier = Modifier.size(250.dp).align(Alignment.Center)
                .border(3.dp, when {
                    registrationDone -> SuccessGreen
                    poseReady -> Color(0xFF4CAF50)
                    faceDetected -> PrimaryIndigo
                    else -> Color.White.copy(alpha = 0.4f)
                }, RoundedCornerShape(40.dp))
        )

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 8.dp, end = 16.dp).align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Face Registration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        // ── Step progress indicator ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 64.dp, start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            REGISTRATION_STEPS.forEachIndexed { index, step ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                index < currentStep -> Color(0xFF4CAF50)       // Completed
                                index == currentStep -> PrimaryIndigo          // Current
                                else -> Color.White.copy(alpha = 0.2f)         // Future
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentStep) {
                        Icon(Icons.Default.Check, "Done", tint = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            text = "${index + 1}",
                            color = if (index == currentStep) Color.White else Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (index < REGISTRATION_STEPS.size - 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 4.dp)
                            .background(
                                if (index < currentStep) Color(0xFF4CAF50)
                                else Color.White.copy(alpha = 0.15f)
                            )
                    )
                }
            }
        }

        // Bottom status
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (registrationDone) {
                Icon(Icons.Default.Check, "Done", tint = SuccessGreen, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Step counter
            if (!registrationDone) {
                Text(
                    text = "Step ${currentStep + 1} of ${REGISTRATION_STEPS.size}",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = statusMessage,
                color = when {
                    registrationDone -> SuccessGreen
                    poseReady -> Color(0xFF4CAF50)
                    faceDetected -> PrimaryIndigo
                    else -> Color.White
                },
                fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp)
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp))
            }
            if (!modelReady) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("⚠️ Face model failed to load!", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ════════════════════════════════════════════════
// BITMAP UTILITIES — shared by Registration & Verification
// ════════════════════════════════════════════════

/**
 * Get a properly ROTATED full-frame bitmap from ImageProxy.
 * Always applies rotation — never returns an un-rotated bitmap.
 */
fun getRotatedBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val raw = try {
            imageProxy.toBitmap()
        } catch (e: Exception) {
            yuvToBitmapRaw(imageProxy)
        } ?: return null

        val rotation = imageProxy.imageInfo.rotationDegrees
        if (rotation == 0) return raw

        val matrix = android.graphics.Matrix()
        matrix.postRotate(rotation.toFloat())
        Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    } catch (e: Exception) {
        Log.e("FaceUtils", "getRotatedBitmap failed: ${e.message}")
        null
    }
}

/**
 * Crop face region from bitmap using ML Kit's bounding box.
 * Adds 20% margin around the face for better embedding quality.
 */
fun cropFace(fullBitmap: Bitmap, face: Face, rotationDegrees: Int): Bitmap? {
    return try {
        val bounds = face.boundingBox

        // ML Kit returns bounding box relative to the original (unrotated) image.
        // Since we already rotated the bitmap, we need to transform the bbox.
        val bitmapW = fullBitmap.width
        val bitmapH = fullBitmap.height

        // Add 20% margin for better embeddings
        val marginX = (bounds.width() * 0.2f).toInt()
        val marginY = (bounds.height() * 0.2f).toInt()

        val left = max(0, bounds.left - marginX)
        val top = max(0, bounds.top - marginY)
        val right = min(bitmapW, bounds.right + marginX)
        val bottom = min(bitmapH, bounds.bottom + marginY)

        val cropW = right - left
        val cropH = bottom - top

        if (cropW <= 0 || cropH <= 0) {
            Log.w("FaceUtils", "Invalid crop dimensions: ${cropW}x${cropH}")
            return null
        }

        Bitmap.createBitmap(fullBitmap, left, top, cropW, cropH)
    } catch (e: Exception) {
        Log.e("FaceUtils", "cropFace failed: ${e.message}")
        null
    }
}

/**
 * YUV to Bitmap fallback (no rotation applied — raw pixels only)
 */
private fun yuvToBitmapRaw(imageProxy: ImageProxy): Bitmap? {
    return try {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer
        yBuffer.rewind(); uBuffer.rewind(); vBuffer.rewind()
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 90, out)
        val bytes = out.toByteArray()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        Log.e("FaceUtils", "YUV conversion failed: ${e.message}")
        null
    }
}
