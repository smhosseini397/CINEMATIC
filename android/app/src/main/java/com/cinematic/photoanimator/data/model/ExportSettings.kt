package com.cinematic.photoanimator.data.model

enum class VideoResolution(
    val label: String,
    val width: Int,
    val height: Int,
    val defaultBitrate: Int
) {
    FHD_1080P("1080p Full HD", 1920, 1080, 24_000_000),
    UHD_4K("4K Ultra HD", 3840, 2160, 50_000_000);

    val isSupportedOnDevice: Boolean
        get() = true // Checked dynamically in VideoExportManager against MediaCodecList
}

enum class VideoFrameRate(val fps: Int, val label: String) {
    FPS_30(30, "30 FPS (Standard Cinematic)"),
    FPS_60(60, "60 FPS (Ultra Smooth)")
}

data class ExportSettings(
    val resolution: VideoResolution = VideoResolution.FHD_1080P,
    val frameRate: VideoFrameRate = VideoFrameRate.FPS_60,
    val durationSeconds: Int = 10,
    val preserveExactColors: Boolean = true,
    val enableStudioLightingSimulation: Boolean = false,
    val carpetFocusMacro: Boolean = false
) {
    val totalFrames: Int
        get() = durationSeconds * frameRate.fps

    val estimatedFileSizeMb: Float
        get() = (resolution.defaultBitrate.toFloat() * durationSeconds) / (8 * 1024 * 1024)
}

data class RenderProgress(
    val isRendering: Boolean = false,
    val isCompleted: Boolean = false,
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val percentage: Float = 0f,
    val elapsedMillis: Long = 0L,
    val estimatedRemainingMillis: Long = 0L,
    val outputPath: String? = null,
    val errorMessage: String? = null
) {
    val formattedRemainingTime: String
        get() {
            if (estimatedRemainingMillis <= 0L) return "Calculating..."
            val totalSeconds = (estimatedRemainingMillis / 1000).toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
