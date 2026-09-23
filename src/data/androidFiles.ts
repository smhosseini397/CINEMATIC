export interface AndroidFileEntry {
  path: string;
  category: 'Kotlin Architecture' | 'MediaCodec & Motion' | 'Compose UI & Theme' | 'Gradle & Config';
  description: string;
  language: string;
  content: string;
}

export const ANDROID_FILES: AndroidFileEntry[] = [
  {
    path: 'android/app/src/main/java/com/cinematic/photoanimator/encoder/MediaCodecVideoEncoder.kt',
    category: 'MediaCodec & Motion',
    description: 'Hardware video encoder using Android MediaCodec (H.264 High Profile), Surface input, 60 FPS frame pump, and MediaMuxer MP4 output.',
    language: 'kotlin',
    content: `package com.cinematic.photoanimator.encoder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.view.Surface
import com.cinematic.photoanimator.data.model.ExportSettings
import com.cinematic.photoanimator.data.model.MotionStyle
import com.cinematic.photoanimator.data.model.RenderProgress
import com.cinematic.photoanimator.motion.CinematicMotionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

class MediaCodecVideoEncoder {

    companion object {
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC // H.264
        private const val I_FRAME_INTERVAL = 1

        fun is4KSupported(): Boolean {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (info in codecList.codecInfos) {
                if (!info.isEncoder) continue
                try {
                    val caps = info.getCapabilitiesForType(MIME_TYPE)
                    val videoCaps = caps.videoCapabilities ?: continue
                    if (videoCaps.isSizeSupported(3840, 2160)) return true
                } catch (ignored: Exception) {}
            }
            return false
        }
    }

    suspend fun encodeVideo(
        sourceBitmap: Bitmap,
        style: MotionStyle,
        settings: ExportSettings,
        outputFile: File,
        onProgress: (RenderProgress) -> Unit
    ) = withContext(Dispatchers.Default) {
        val width = settings.resolution.width
        val height = settings.resolution.height
        val fps = settings.frameRate.fps
        val totalFrames = settings.totalFrames
        val bitrate = settings.resolution.defaultBitrate

        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
                setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel51)
            }
        }

        val encoder = MediaCodec.createEncoderByType(MIME_TYPE)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface: Surface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()
        val startTimeMs = System.currentTimeMillis()

        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { isDither = true }
        val lightingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        try {
            for (frameIndex in 0 until totalFrames) {
                val progressFraction = frameIndex.toFloat() / (totalFrames - 1).coerceAtLeast(1)
                val transform = CinematicMotionEngine.calculateTransform(style, progressFraction)

                val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }

                try {
                    canvas.drawColor(Color.BLACK)
                    val matrix = Matrix()
                    val centerX = width / 2f
                    val centerY = height / 2f

                    val scaleFit = maxOf(width / sourceBitmap.width.toFloat(), height / sourceBitmap.height.toFloat())
                    matrix.postTranslate(-sourceBitmap.width / 2f, -sourceBitmap.height / 2f)
                    matrix.postScale(scaleFit * transform.scale, scaleFit * transform.scale)
                    matrix.postRotate(transform.rotationZ)
                    matrix.postTranslate(centerX + (transform.translationX * width), centerY + (transform.translationY * height))

                    canvas.drawBitmap(sourceBitmap, matrix, bitmapPaint)

                    if (transform.lightIntensity > 0.01f) {
                        val rad = Math.toRadians(transform.lightAngle.toDouble())
                        val lx = centerX + (cos(rad).toFloat() * centerX * 0.8f)
                        val ly = centerY + (sin(rad).toFloat() * centerY * 0.8f)
                        val lightRadius = maxOf(width, height) * 0.75f
                        val alpha = (transform.lightIntensity * 255).toInt().coerceIn(0, 45)
                        lightingPaint.shader = RadialGradient(lx, ly, lightRadius, Color.argb(alpha, 255, 250, 240), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), lightingPaint)
                    }
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                drainEncoder(encoder, muxer, bufferInfo, false, { trackIndex }, { trackIndex = it }, { muxerStarted }, { muxerStarted = it })

                val elapsed = System.currentTimeMillis() - startTimeMs
                val framesRemaining = totalFrames - (frameIndex + 1)
                val avgTimePerFrame = if (frameIndex > 0) elapsed.toFloat() / (frameIndex + 1) else 25f
                val remainingMs = (framesRemaining * avgTimePerFrame).toLong()

                onProgress(
                    RenderProgress(
                        isRendering = true,
                        isCompleted = false,
                        currentFrame = frameIndex + 1,
                        totalFrames = totalFrames,
                        percentage = ((frameIndex + 1).toFloat() / totalFrames) * 100f,
                        elapsedMillis = elapsed,
                        estimatedRemainingMillis = remainingMs
                    )
                )
            }

            encoder.signalEndOfInputStream()
            drainEncoder(encoder, muxer, bufferInfo, true, { trackIndex }, { trackIndex = it }, { muxerStarted }, { muxerStarted = it })

            onProgress(RenderProgress(isRendering = false, isCompleted = true, percentage = 100f, outputPath = outputFile.absolutePath))
        } finally {
            try { encoder.stop(); encoder.release() } catch (ignored: Exception) {}
            if (muxerStarted) { try { muxer.stop(); muxer.release() } catch (ignored: Exception) {} }
            inputSurface.release()
        }
    }
}`
  },
  {
    path: 'android/app/src/main/java/com/cinematic/photoanimator/motion/CinematicMotionEngine.kt',
    category: 'MediaCodec & Motion',
    description: '60 FPS camera motion mathematics: Ken Burns, Slow Zoom In/Out, 3D Parallax, Handheld Drift, and Luxury Product curves.',
    language: 'kotlin',
    content: `package com.cinematic.photoanimator.motion

import com.cinematic.photoanimator.data.model.CameraTransform
import com.cinematic.photoanimator.data.model.MotionStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object CinematicMotionEngine {

    fun calculateTransform(style: MotionStyle, progress: Float): CameraTransform {
        val clampedProgress = progress.coerceIn(0f, 1f)
        val easedProgress = cubicEaseInOut(clampedProgress)

        return when (style) {
            MotionStyle.SLOW_ZOOM_IN -> CameraTransform(scale = 1.0f + (0.35f * easedProgress), translationX = 0f, translationY = 0f)
            MotionStyle.SLOW_ZOOM_OUT -> CameraTransform(scale = 1.40f - (0.35f * easedProgress), translationX = 0f, translationY = 0f)
            MotionStyle.KEN_BURNS -> CameraTransform(
                scale = 1.15f + (0.25f * easedProgress),
                translationX = -0.12f + (0.22f * easedProgress),
                translationY = -0.08f + (0.14f * easedProgress),
                rotationZ = sin(easedProgress * PI.toFloat()) * 0.4f
            )
            MotionStyle.SMOOTH_PAN_HORIZONTAL -> CameraTransform(scale = 1.25f, translationX = -0.22f + (0.44f * easedProgress), translationY = 0f)
            MotionStyle.VERTICAL_MOVEMENT -> CameraTransform(scale = 1.25f, translationX = 0f, translationY = 0.20f - (0.40f * easedProgress), tiltX = sin(easedProgress * PI.toFloat()) * 1.5f)
            MotionStyle.PARALLAX_3D -> {
                val angle = easedProgress * 2f * PI.toFloat()
                CameraTransform(scale = 1.28f + (0.10f * sin(easedProgress * PI.toFloat())), translationX = cos(angle) * 0.08f, translationY = sin(angle) * 0.05f, tiltX = sin(angle) * 3.5f, tiltY = cos(angle) * 4.0f)
            }
            MotionStyle.DOCUMENTARY_DRIFT -> {
                val t = easedProgress * 4f * PI.toFloat()
                CameraTransform(scale = 1.18f + (sin(t * 0.5f) * 0.04f), translationX = (sin(t) * 0.035f), translationY = (cos(t * 0.8f) * 0.030f), rotationZ = sin(t * 0.6f) * 0.75f)
            }
            MotionStyle.LUXURY_SHOWCASE -> CameraTransform(
                scale = 1.30f + (sin(easedProgress * PI.toFloat()) * 0.15f),
                translationX = sin((easedProgress - 0.5f) * PI.toFloat()) * 0.18f,
                translationY = cos((easedProgress - 0.5f) * PI.toFloat()) * 0.08f - 0.05f,
                lightAngle = easedProgress * 180f,
                lightIntensity = sin(easedProgress * PI.toFloat()) * 0.25f
            )
            MotionStyle.PERSIAN_CARPET_LUXURY -> PersianCarpetShowcaseEngine.calculateCarpetTransform(clampedProgress)
        }
    }

    private fun cubicEaseInOut(t: Float): Float =
        if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).let { it * it * it } / 2f
}`
  },
  {
    path: 'android/app/src/main/java/com/cinematic/photoanimator/motion/PersianCarpetShowcaseEngine.kt',
    category: 'MediaCodec & Motion',
    description: 'Dedicated multi-stage macro trajectory for Persian rugs: Medallion, Floral Palmettes, Borders, Pile Texture, and Hand-Knotted Fringes.',
    language: 'kotlin',
    content: `package com.cinematic.photoanimator.motion

import com.cinematic.photoanimator.data.model.CameraTransform
import com.cinematic.photoanimator.data.model.CarpetShowcaseProfile
import com.cinematic.photoanimator.data.model.CarpetType
import kotlin.math.PI
import kotlin.math.sin

object PersianCarpetShowcaseEngine {
    private val defaultProfile = CarpetShowcaseProfile(CarpetType.KASHAN)

    fun calculateCarpetTransform(progress: Float, profile: CarpetShowcaseProfile = defaultProfile): CameraTransform {
        val zones = profile.primaryFocusZones
        val n = zones.size
        val totalStages = n
        val scaledProgress = (progress.coerceIn(0f, 1f) * (totalStages - 1))
        val currentStageIndex = scaledProgress.toInt().coerceIn(0, totalStages - 2)
        val stageFraction = (scaledProgress - currentStageIndex).coerceIn(0f, 1f)
        val smoothT = stageFraction * stageFraction * (3f - 2f * stageFraction)

        val zoneA = zones[currentStageIndex]
        val zoneB = zones[currentStageIndex + 1]

        val targetX = zoneA.normalizedX + (zoneB.normalizedX - zoneA.normalizedX) * smoothT
        val targetY = zoneA.normalizedY + (zoneB.normalizedY - zoneA.normalizedY) * smoothT
        val scale = zoneA.zoomLevel + (zoneB.zoomLevel - zoneA.zoomLevel) * smoothT

        val transX = (0.5f - targetX) * (scale - 1f)
        val transY = (0.5f - targetY) * (scale - 1f)
        val lightAngle = progress * 180f
        val lightIntensity = if (profile.enableFiberLightingGleam) (0.08f * sin(progress * PI.toFloat())).coerceIn(0f, 0.15f) else 0f

        return CameraTransform(
            scale = scale,
            translationX = transX,
            translationY = transY,
            rotationZ = sin(progress * PI.toFloat()) * 0.2f,
            lightAngle = lightAngle,
            lightIntensity = lightIntensity
        )
    }
}`
  },
  {
    path: 'android/app/src/main/java/com/cinematic/photoanimator/MainActivity.kt',
    category: 'Kotlin Architecture',
    description: 'Main activity with Jetpack Compose edge-to-edge layout, ViewModel lifecycle scope, and Navigation Compose.',
    language: 'kotlin',
    content: `package com.cinematic.photoanimator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cinematic.photoanimator.picker.GalleryPickerManager
import com.cinematic.photoanimator.ui.screens.*
import com.cinematic.photoanimator.ui.theme.CinematicTheme
import com.cinematic.photoanimator.ui.theme.ObsidianBlack
import com.cinematic.photoanimator.ui.viewmodel.AnimatorViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AnimatorViewModel by viewModels()
    private lateinit var galleryPickerManager: GalleryPickerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        galleryPickerManager = GalleryPickerManager(
            activity = this,
            photoRepository = com.cinematic.photoanimator.data.repository.PhotoRepositoryImpl(this),
            scope = lifecycleScope,
            onPhotosSelected = { photos -> viewModel.onPhotosSelected(photos) }
        )

        setContent {
            CinematicTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = ObsidianBlack) {
                    AppNavigation(
                        viewModel = viewModel,
                        onLaunchSinglePicker = { galleryPickerManager.launchSinglePicker() },
                        onLaunchMultiPicker = { galleryPickerManager.launchMultiPicker() }
                    )
                }
            }
        }
    }
}`
  },
  {
    path: 'android/app/src/main/java/com/cinematic/photoanimator/ui/viewmodel/AnimatorViewModel.kt',
    category: 'Kotlin Architecture',
    description: 'MVVM ViewModel using StateFlow, Coroutines, MediaCodec hardware capability checks, and export orchestration.',
    language: 'kotlin',
    content: `package com.cinematic.photoanimator.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cinematic.photoanimator.data.model.*
import com.cinematic.photoanimator.data.repository.*
import com.cinematic.photoanimator.encoder.MediaCodecVideoEncoder
import com.cinematic.photoanimator.export.VideoExportManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class AnimatorViewModel(application: Application) : AndroidViewModel(application) {
    private val photoRepository: PhotoRepository = PhotoRepositoryImpl(application)
    private val videoEncoder: MediaCodecVideoEncoder = MediaCodecVideoEncoder()
    private val exportRepository: VideoExportRepository = VideoExportRepositoryImpl(application, photoRepository, videoEncoder)
    private val exportManager: VideoExportManager = VideoExportManager(exportRepository)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val renderProgress: StateFlow<RenderProgress> = exportManager.progressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RenderProgress())

    fun selectMotionStyle(style: MotionStyle) { _uiState.update { it.copy(selectedMotionStyle = style) } }
    fun setDuration(seconds: Int) { _uiState.update { it.copy(exportSettings = it.exportSettings.copy(durationSeconds = seconds)) } }
    fun setResolution(res: VideoResolution) { _uiState.update { it.copy(exportSettings = it.exportSettings.copy(resolution = res)) } }
    fun setFrameRate(fps: VideoFrameRate) { _uiState.update { it.copy(exportSettings = it.exportSettings.copy(frameRate = fps)) } }
    fun startRender(onCompleted: (File) -> Unit) {
        val photo = _uiState.value.currentPhoto ?: return
        viewModelScope.launch {
            exportManager.exportVideo(photo, _uiState.value.selectedMotionStyle, _uiState.value.exportSettings)
                .onSuccess { file ->
                    _uiState.update { it.copy(lastExportedFile = file) }
                    onCompleted(file)
                }
        }
    }
}`
  },
  {
    path: 'android/app/build.gradle.kts',
    category: 'Gradle & Config',
    description: 'Gradle configuration for Android targetSdk 34, minSdk 26, Jetpack Compose Material 3 BOM, and MediaCodec dependencies.',
    language: 'groovy',
    content: `plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cinematic.photoanimator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cinematic.photoanimator"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}`
  },
  {
    path: 'android/app/src/main/AndroidManifest.xml',
    category: 'Gradle & Config',
    description: 'Application manifest with high-res photo permissions, GLES hardware acceleration, largeHeap, and FileProvider.',
    language: 'xml',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-feature android:glEsVersion="0x00020000" android:required="true" />

    <application
        android:label="@string/app_name"
        android:largeHeap="true"
        android:hardwareAccelerated="true"
        android:theme="@style/Theme.CinematicPhotoAnimator">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="\${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>
</manifest>`
  },
  {
    path: 'android/gradle/wrapper/gradle-wrapper.properties',
    category: 'Gradle & Config',
    description: 'Official Gradle Wrapper configuration pointing to Gradle 8.4 binary distribution.',
    language: 'properties',
    content: `distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.4-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists`
  }
];
