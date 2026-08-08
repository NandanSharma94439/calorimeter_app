@file:OptIn(ExperimentalPermissionsApi::class)

package com.nandan.calorimeterapp.ui.camera

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.nandan.calorimeterapp.data.model.AnalyzeResult
import com.nandan.calorimeterapp.ui.theme.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
fun CameraAnalyzeScreen(
    onDismiss: () -> Unit,
    onResult: (AnalyzeResult) -> Unit,
    cameraViewModel: CameraViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by cameraViewModel.state.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    // When result comes in, pass it back
    LaunchedEffect(state) {
        if (state is CameraUiState.Result) {
            onResult((state as CameraUiState.Result).data)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (!cameraPermission.status.isGranted) {
            // Permission denied UI
            PermissionDeniedContent(
                onRequestAgain = { cameraPermission.launchPermissionRequest() },
                onDismiss = onDismiss,
            )
        } else {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    val preview = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        val previewUseCase = Preview.Builder().build().also {
                            it.setSurfaceProvider(preview.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                previewUseCase,
                                capture,
                            )
                        } catch (e: Exception) {
                            Log.e("CameraAnalyzeScreen", "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    preview
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Overlay gradient (bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Scanning frame
            ScanningFrame(isScanning = state is CameraUiState.Analyzing)

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                ) {
                    Text(
                        "AI Calorie Analyzer",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                    )
                }
                Spacer(Modifier.size(44.dp))
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AnimatedContent(targetState = state, label = "camera_state") { s ->
                    when (s) {
                        is CameraUiState.Error -> {
                            ErrorBanner(s.message) { cameraViewModel.reset() }
                        }
                        is CameraUiState.Analyzing -> {
                            AnalyzingBanner()
                        }
                        else -> {
                            Text(
                                "Point at your food and tap the button",
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                // Shutter button
                val isAnalyzing = state is CameraUiState.Analyzing
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAnalyzing) Color.White.copy(alpha = 0.3f) else Color.White
                        )
                        .border(4.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = {
                            if (!isAnalyzing) {
                                imageCapture?.let { capture ->
                                    capture.takePicture(
                                        executor,
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            @androidx.annotation.OptIn(ExperimentalGetImage::class)
                                            override fun onCaptureSuccess(proxy: ImageProxy) {
                                                try {
                                                    val bytes = proxyToCompressedBytes(proxy)
                                                    cameraViewModel.analyzeImage(bytes)
                                                } catch (e: Exception) {
                                                    Log.e("Camera", "Capture processing failed", e)
                                                } finally {
                                                    proxy.close()
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                Log.e("Camera", "Capture failed", exception)
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.size(72.dp),
                        enabled = !isAnalyzing,
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            null,
                            tint = if (isAnalyzing) Color.White.copy(alpha = 0.4f) else Color(0xFF0D1117),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        }
    }
}

// ── Helper: convert ImageProxy to compressed JPEG bytes ──────────────────────

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun proxyToCompressedBytes(proxy: ImageProxy): ByteArray {
    val mediaImage = proxy.image!!
    val planes = mediaImage.planes
    val buffer = planes[0].buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)

    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        ?: run {
            // YUV fallback — use ImageProxy directly
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = android.graphics.YuvImage(
                nv21, android.graphics.ImageFormat.NV21,
                mediaImage.width, mediaImage.height, null
            )
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, mediaImage.width, mediaImage.height),
                85, out
            )
            return out.toByteArray()
        }

    // Rotate if needed
    val matrix = Matrix().apply { postRotate(proxy.imageInfo.rotationDegrees.toFloat()) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    // Scale down to max 1024px to reduce upload size
    val maxDim = 1024
    val scaled = if (rotated.width > maxDim || rotated.height > maxDim) {
        val scale = maxDim.toFloat() / maxOf(rotated.width, rotated.height)
        Bitmap.createScaledBitmap(rotated, (rotated.width * scale).toInt(), (rotated.height * scale).toInt(), true)
    } else rotated

    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
    return out.toByteArray()
}

// ── Scanning Frame ────────────────────────────────────────────────────────────

@Composable
private fun ScanningFrame(isScanning: Boolean) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isScanning) 1f else 0.7f,
        animationSpec = if (isScanning) {
            infiniteRepeatable<Float>(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            )
        } else {
            tween(durationMillis = 0)
        },
        label = "scan_alpha",
    )
    val color = if (isScanning) Emerald else Color.White.copy(alpha = 0.5f)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val size = 240.dp
        val cornerSize = 24.dp
        val strokeWidth = 3.dp

        Box(modifier = Modifier.size(size)) {
            // Top-left
            CornerBracket(Modifier.align(Alignment.TopStart), color.copy(alpha = animatedAlpha))
            // Top-right
            CornerBracket(Modifier.align(Alignment.TopEnd).then(Modifier), color.copy(alpha = animatedAlpha), flipH = true)
            // Bottom-left
            CornerBracket(Modifier.align(Alignment.BottomStart), color.copy(alpha = animatedAlpha), flipV = true)
            // Bottom-right
            CornerBracket(Modifier.align(Alignment.BottomEnd), color.copy(alpha = animatedAlpha), flipH = true, flipV = true)
        }
    }
}

@Composable
private fun CornerBracket(
    modifier: Modifier,
    color: Color,
    flipH: Boolean = false,
    flipV: Boolean = false,
) {
    val scaleX = if (flipH) -1f else 1f
    val scaleY = if (flipV) -1f else 1f
    Box(
        modifier = modifier
            .size(36.dp)
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(3.dp)
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(color)
        )
    }
}

// ── Analyzing Banner ──────────────────────────────────────────────────────────

@Composable
private fun AnalyzingBanner() {
    val dots = remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(400)
            dots.value = when (dots.value.length) {
                0 -> "."
                1 -> ".."
                2 -> "..."
                else -> ""
            }
        }
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Emerald.copy(alpha = 0.2f),
        modifier = Modifier.border(1.dp, Emerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Emerald, strokeWidth = 2.dp)
            Text(
                "Analyzing food${dots.value}",
                color = Emerald,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

// ── Error Banner ──────────────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AccentRed.copy(alpha = 0.15f),
        modifier = Modifier.border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Error, null, tint = AccentRed, modifier = Modifier.size(18.dp))
            Text(message, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text("Retry", color = Emerald, fontSize = 13.sp) }
        }
    }
}

// ── Permission Denied ─────────────────────────────────────────────────────────

@Composable
private fun PermissionDeniedContent(onRequestAgain: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.CameraAlt,
            null,
            tint = OnSurfaceMuted,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Camera Permission Required",
            style = MaterialTheme.typography.titleMedium,
            color = OnBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Camera access is needed to analyze your food and estimate calories using AI.",
            color = OnSurfaceMuted,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequestAgain,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color(0xFF0D1117)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Grant Permission", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onDismiss) {
            Text("Cancel", color = OnSurfaceMuted)
        }
    }
}
