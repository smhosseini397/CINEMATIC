package com.cinematic.photoanimator.encoder

import android.graphics.Bitmap
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
import com.cinematic.photoanimator.data.model.ExportSettings
import com.cinematic.photoanimator.data.model.MotionStyle
import com.cinematic.photoanimator.data.model.RenderProgress
import com.cinematic.photoanimator.motion.CinematicMotionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class MediaCodecVideoEncoder {

    companion object {

        private const val MIME_TYPE =
            MediaFormat.MIMETYPE_VIDEO_AVC

        private const val I_FRAME_INTERVAL = 1

        fun isVideoSupported(
            width: Int,
            height: Int,
            fps: Int
        ): Boolean {

            val codecList =
                MediaCodecList(
                    MediaCodecList.REGULAR_CODECS
                )

            for (info in codecList.codecInfos) {

                if (!info.isEncoder) continue

                try {

                    val caps =
                        info.getCapabilitiesForType(
                            MIME_TYPE
                        )

                    val videoCaps =
                        caps.videoCapabilities
                            ?: continue

                    if (
                        videoCaps.areSizeAndRateSupported(
                            width,
                            height,
                            fps.toDouble()
                        ) ||
                        videoCaps.areSizeAndRateSupported(
                            height,
                            width,
                            fps.toDouble()
                        )
                    ) {
                        return true
                    }

                } catch (ignored: Exception) {
                }
            }

            return false
        }

        fun is4KSupported(): Boolean {
            return isVideoSupported(
                3840,
                2160,
                30
            )
        }

        fun is4K60Supported(): Boolean {
            return isVideoSupported(
                3840,
                2160,
                60
            )
        }
    }

    suspend fun encodeVideo(
        sourceBitmap: Bitmap,
        style: MotionStyle,
        settings: ExportSettings,
        outputFile: File,
        onProgress: (RenderProgress) -> Unit
    ) = withContext(Dispatchers.Default) {

        val width = settings.outputWidth
        val height = settings.outputHeight
        val fps = settings.frameRate.fps
        val totalFrames = settings.totalFrames
        val bitrate = settings.resolution.defaultBitrate

        if (
            !isVideoSupported(
                width,
                height,
                fps
            )
        ) {
            throw IllegalArgumentException(
                "این دستگاه از خروجی ${width}x${height} با نرخ $fps فریم بر ثانیه پشتیبانی نمی کند."
            )
        }

        val format =
            MediaFormat.createVideoFormat(
                MIME_TYPE,
                width,
                height
            ).apply {

                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities
                        .COLOR_FormatSurface
                )

                setInteger(
                    MediaFormat.KEY_BIT_RATE,
                    bitrate
                )

                setInteger(
                    MediaFormat.KEY_FRAME_RATE,
                    fps
                )

                setInteger(
                    MediaFormat.KEY_I_FRAME_INTERVAL,
                    I_FRAME_INTERVAL
                )
            }

        val encoder =
            try {
                MediaCodec.createEncoderByType(
                    MIME_TYPE
                )
            } catch (e: Exception) {
                throw RuntimeException(
                    "امکان ایجاد Encoder ویدیو وجود ندارد.",
                    e
                )
            }

        var inputSurface: android.view.Surface? = null

        val muxer =
            MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

        var trackIndex = -1
        var muxerStarted = false

        val bufferInfo =
            MediaCodec.BufferInfo()

        try {

            try {
                encoder.configure(
                    format,
                    null,
                    null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE
                )
            } catch (e: Exception) {
                throw RuntimeException(
                    "Encoder برای خروجی ${width}x${height} با $fps FPS قابل تنظیم نیست.",
                    e
                )
            }

            inputSurface =
                try {
                    encoder.createInputSurface()
                } catch (e: Exception) {
                    throw RuntimeException(
                        "امکان ایجاد سطح ورودی Encoder وجود ندارد.",
                        e
                    )
                }

            val surface =
                inputSurface
                    ?: throw RuntimeException(
                        "سطح ورودی Encoder ایجاد نشد."
                    )

            try {
                encoder.start()
            } catch (e: Exception) {
                throw RuntimeException(
                    "شروع Encoder برای خروجی ${width}x${height} با $fps FPS ناموفق بود.",
                    e
                )
            }

            val startTimeMs =
                System.currentTimeMillis()

            val bitmapPaint =
                Paint(
                    Paint.ANTI_ALIAS_FLAG or
                            Paint.FILTER_BITMAP_FLAG
                ).apply {
                    isDither = true
                }

            val lightingPaint =
                Paint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    setStyle(Paint.Style.FILL)
                }

            for (
                frameIndex in 0 until totalFrames
            ) {

                val progressFraction =
                    frameIndex.toFloat() /
                            (totalFrames - 1)
                                .coerceAtLeast(1)

                val transform =
                    CinematicMotionEngine
                        .calculateTransform(
                            style,
                            progressFraction
                        )

                val canvas =
                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.M
                    ) {
                        surface.lockHardwareCanvas()
                    } else {
                        surface.lockCanvas(null)
                    }

                try {

                    canvas.drawColor(
                        Color.BLACK
                    )

                    val matrix =
                        Matrix()

                    val centerX =
                        width / 2f

                    val centerY =
                        height / 2f

                    val photoWidth =
                        sourceBitmap.width.toFloat()

                    val photoHeight =
                        sourceBitmap.height.toFloat()

                    val scaleFit =
                        max(
                            width / photoWidth,
                            height / photoHeight
                        )

                    matrix.postTranslate(
                        -photoWidth / 2f,
                        -photoHeight / 2f
                    )

                    matrix.postScale(
                        scaleFit *
                                transform.scale,
                        scaleFit *
                                transform.scale
                    )

                    matrix.postRotate(
                        transform.rotationZ
                    )

                    matrix.postTranslate(
                        centerX +
                                (
                                    transform.translationX *
                                            width
                                    ),
                        centerY +
                                (
                                    transform.translationY *
                                            height
                                    )
                    )

                    canvas.drawBitmap(
                        sourceBitmap,
                        matrix,
                        bitmapPaint
                    )

                    if (
                        transform.lightIntensity >
                        0.01f
                    ) {

                        val rad =
                            Math.toRadians(
                                transform.lightAngle
                                    .toDouble()
                            )

                        val lx =
                            centerX +
                                    (
                                        cos(rad).toFloat() *
                                                centerX *
                                                0.8f
                                        )

                        val ly =
                            centerY +
                                    (
                                        sin(rad).toFloat() *
                                                centerY *
                                                0.8f
                                        )

                        val lightRadius =
                            max(
                                width,
                                height
                            ) * 0.75f

                        val alpha =
                            (
                                transform.lightIntensity *
                                        255
                                )
                                .toInt()
                                .coerceIn(
                                    0,
                                    45
                                )

                        lightingPaint.shader =
                            RadialGradient(
                                lx,
                                ly,
                                lightRadius,
                                Color.argb(
                                    alpha,
                                    255,
                                    250,
                                    240
                                ),
                                Color.TRANSPARENT,
                                Shader.TileMode.CLAMP
                            )

                        canvas.drawRect(
                            0f,
                            0f,
                            width.toFloat(),
                            height.toFloat(),
                            lightingPaint
                        )
                    }

                } finally {

                    surface.unlockCanvasAndPost(
                        canvas
                    )
                }

                drainEncoder(
                    encoder,
                    muxer,
                    bufferInfo,
                    false,
                    { trackIndex },
                    { index ->
                        trackIndex = index
                    },
                    { muxerStarted },
                    { started ->
                        muxerStarted = started
                    }
                )

                val elapsed =
                    System.currentTimeMillis() -
                            startTimeMs

                val framesRemaining =
                    totalFrames -
                            (frameIndex + 1)

                val avgTimePerFrame =
                    if (frameIndex > 0) {
                        elapsed.toFloat() /
                                (frameIndex + 1)
                    } else {
                        25f
                    }

                val remainingMs =
                    (
                        framesRemaining *
                                avgTimePerFrame
                        ).toLong()

                onProgress(
                    RenderProgress(
                        isRendering = true,
                        isCompleted = false,
                        currentFrame =
                            frameIndex + 1,
                        totalFrames =
                            totalFrames,
                        percentage =
                            (
                                (frameIndex + 1)
                                    .toFloat() /
                                        totalFrames
                                ) * 100f,
                        elapsedMillis =
                            elapsed,
                        estimatedRemainingMillis =
                            remainingMs
                    )
                )
            }

            encoder.signalEndOfInputStream()

            drainEncoder(
                encoder,
                muxer,
                bufferInfo,
                true,
                { trackIndex },
                { index ->
                    trackIndex = index
                },
                { muxerStarted },
                { started ->
                    muxerStarted = started
                }
            )

            val totalElapsed =
                System.currentTimeMillis() -
                        startTimeMs

            onProgress(
                RenderProgress(
                    isRendering = false,
                    isCompleted = true,
                    currentFrame =
                        totalFrames,
                    totalFrames =
                        totalFrames,
                    percentage = 100f,
                    elapsedMillis =
                        totalElapsed,
                    estimatedRemainingMillis =
                        0L,
                    outputPath =
                        outputFile.absolutePath
                )
            )

        } finally {

            try {
                encoder.stop()
            } catch (ignored: Exception) {
            }

            try {
                encoder.release()
            } catch (ignored: Exception) {
            }

            if (muxerStarted) {
                try {
                    muxer.stop()
                } catch (ignored: Exception) {
                }
            }

            try {
                muxer.release()
            } catch (ignored: Exception) {
            }

            try {
                inputSurface?.release()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        getTrackIndex: () -> Int,
        setTrackIndex: (Int) -> Unit,
        isMuxerStarted: () -> Boolean,
        setMuxerStarted: (Boolean) -> Unit
    ) {

        val timeoutUs = 10_000L

        var noOutputCount = 0

        while (true) {

            val encoderStatus =
                encoder.dequeueOutputBuffer(
                    bufferInfo,
                    timeoutUs
                )

            if (
                encoderStatus ==
                MediaCodec.INFO_TRY_AGAIN_LATER
            ) {

                if (!endOfStream) {
                    break
                }

                noOutputCount++

                if (noOutputCount > 1000) {
                    throw RuntimeException(
                        "Encoder در پایان رندر متوقف شد و سیگنال پایان دریافت نشد."
                    )
                }

            } else if (
                encoderStatus ==
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
            ) {

                if (isMuxerStarted()) {
                    throw RuntimeException(
                        "فرمت Encoder بعد از شروع Muxer تغییر کرد."
                    )
                }

                val newFormat =
                    encoder.outputFormat

                val track =
                    muxer.addTrack(
                        newFormat
                    )

                setTrackIndex(track)

                muxer.start()

                setMuxerStarted(true)

                noOutputCount = 0

            } else if (
                encoderStatus >= 0
            ) {

                noOutputCount = 0

                val encodedData =
                    encoder.getOutputBuffer(
                        encoderStatus
                    ) ?: throw RuntimeException(
                        "بافر خروجی Encoder خالی است."
                    )

                if (
                    (
                        bufferInfo.flags and
                                MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                        ) != 0
                ) {
                    bufferInfo.size = 0
                }

                if (
                    bufferInfo.size != 0 &&
                    isMuxerStarted()
                ) {

                    encodedData.position(
                        bufferInfo.offset
                    )

                    encodedData.limit(
                        bufferInfo.offset +
                                bufferInfo.size
                    )

                    muxer.writeSampleData(
                        getTrackIndex(),
                        encodedData,
                        bufferInfo
                    )
                }

                encoder.releaseOutputBuffer(
                    encoderStatus,
                    false
                )

                if (
                    (
                        bufferInfo.flags and
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        ) != 0
                ) {
                    break
                }
            }
        }
    }
}
