package com.cinematic.photoanimator.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.cinematic.photoanimator.data.model.ExportSettings
import com.cinematic.photoanimator.data.model.MotionStyle
import com.cinematic.photoanimator.data.model.PhotoItem
import com.cinematic.photoanimator.data.model.RenderProgress
import com.cinematic.photoanimator.encoder.MediaCodecVideoEncoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream

interface VideoExportRepository {

    val renderProgressFlow: Flow<RenderProgress>

    suspend fun startExport(
        photo: PhotoItem,
        style: MotionStyle,
        settings: ExportSettings
    ): File

    suspend fun saveToGallery(
        videoFile: File
    ): Uri?

    fun createShareIntent(
        videoFile: File
    ): Intent
}

class VideoExportRepositoryImpl(
    private val context: Context,
    private val photoRepository: PhotoRepository,
    private val encoder: MediaCodecVideoEncoder
) : VideoExportRepository {

    private val _progress =
        MutableStateFlow(RenderProgress())

    override val renderProgressFlow: Flow<RenderProgress> =
        _progress.asStateFlow()

    override suspend fun startExport(
        photo: PhotoItem,
        style: MotionStyle,
        settings: ExportSettings
    ): File {

        _progress.value = RenderProgress(
            isRendering = true,
            isCompleted = false,
            currentFrame = 0,
            totalFrames = settings.totalFrames,
            percentage = 0f,
            elapsedMillis = 0L,
            estimatedRemainingMillis = 0L,
            outputPath = null,
            errorMessage = null
        )

        val targetWidth = settings.outputWidth
        val targetHeight = settings.outputHeight

        val bitmap =
            photoRepository.loadHighResBitmap(
                photo.uri,
                targetWidth,
                targetHeight
            )

        val outputDir =
            File(
                context.getExternalFilesDir(
                    Environment.DIRECTORY_MOVIES
                ),
                "CinematicExports"
            ).apply {
                if (!exists()) {
                    mkdirs()
                }
            }

        val fileName =
            "CINEMATIC_${System.currentTimeMillis()}_${settings.resolution.label.replace(" ", "_")}_${settings.orientation.shortLabel.replace(":", "x")}.mp4"

        val outputFile =
            File(
                outputDir,
                fileName
            )

        if (outputFile.exists()) {
            outputFile.delete()
        }

        encoder.encodeVideo(
            sourceBitmap = bitmap,
            style = style,
            settings = settings,
            outputFile = outputFile,
            onProgress = { progress ->
                _progress.value = progress
            }
        )

        return outputFile
    }

    override suspend fun saveToGallery(
        videoFile: File
    ): Uri? {

        if (!videoFile.exists() || videoFile.length() == 0L) {
            return null
        }

        val resolver = context.contentResolver

        val contentValues =
            ContentValues().apply {

                put(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    videoFile.name
                )

                put(
                    MediaStore.Video.Media.MIME_TYPE,
                    "video/mp4"
                )

                put(
                    MediaStore.Video.Media.DATE_ADDED,
                    System.currentTimeMillis() / 1000
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/CinematicAnimator"
                    )

                    put(
                        MediaStore.Video.Media.IS_PENDING,
                        1
                    )
                }
            }

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )

            } else {

                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

        val itemUri =
            resolver.insert(
                collection,
                contentValues
            ) ?: return null

        try {

            resolver.openOutputStream(
                itemUri
            )?.use { outStream ->

                FileInputStream(
                    videoFile
                ).use { inStream ->

                    inStream.copyTo(
                        outStream
                    )
                }
            } ?: run {

                resolver.delete(
                    itemUri,
                    null,
                    null
                )

                return null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                val completedValues =
                    ContentValues().apply {
                        put(
                            MediaStore.Video.Media.IS_PENDING,
                            0
                        )
                    }

                resolver.update(
                    itemUri,
                    completedValues,
                    null,
                    null
                )
            }

            return itemUri

        } catch (e: Exception) {

            resolver.delete(
                itemUri,
                null,
                null
            )

            throw e
        }
    }

    override fun createShareIntent(
        videoFile: File
    ): Intent {

        val authority =
            "${context.packageName}.fileprovider"

        val contentUri =
            FileProvider.getUriForFile(
                context,
                authority,
                videoFile
            )

        return Intent(
            Intent.ACTION_SEND
        ).apply {

            type = "video/mp4"

            putExtra(
                Intent.EXTRA_STREAM,
                contentUri
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
}
