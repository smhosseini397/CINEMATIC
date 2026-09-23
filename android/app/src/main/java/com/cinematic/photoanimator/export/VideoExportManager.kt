package com.cinematic.photoanimator.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.cinematic.photoanimator.data.model.ExportSettings
import com.cinematic.photoanimator.data.model.MotionStyle
import com.cinematic.photoanimator.data.model.PhotoItem
import com.cinematic.photoanimator.data.model.RenderProgress
import com.cinematic.photoanimator.data.repository.VideoExportRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

class VideoExportManager(
    private val videoExportRepository: VideoExportRepository
) {
    val progressFlow: Flow<RenderProgress> = videoExportRepository.renderProgressFlow

    suspend fun exportVideo(
        photo: PhotoItem,
        style: MotionStyle,
        settings: ExportSettings
    ): Result<File> {
        return try {
            val file = videoExportRepository.startExport(photo, style, settings)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveToGallery(file: File): Uri? {
        return videoExportRepository.saveToGallery(file)
    }

    fun getShareIntent(file: File): Intent {
        return videoExportRepository.createShareIntent(file)
    }
}
