package com.cinematic.photoanimator.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cinematic.photoanimator.data.model.CarpetShowcaseProfile
import com.cinematic.photoanimator.data.model.CarpetType
import com.cinematic.photoanimator.data.model.ExportSettings
import com.cinematic.photoanimator.data.model.MotionStyle
import com.cinematic.photoanimator.data.model.PhotoItem
import com.cinematic.photoanimator.data.model.RenderProgress
import com.cinematic.photoanimator.data.model.VideoFrameRate
import com.cinematic.photoanimator.data.model.VideoResolution
import com.cinematic.photoanimator.data.repository.PhotoRepository
import com.cinematic.photoanimator.data.repository.PhotoRepositoryImpl
import com.cinematic.photoanimator.data.repository.VideoExportRepository
import com.cinematic.photoanimator.data.repository.VideoExportRepositoryImpl
import com.cinematic.photoanimator.encoder.MediaCodecVideoEncoder
import com.cinematic.photoanimator.export.VideoExportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UiState(
    val selectedPhotos: List<PhotoItem> = emptyList(),
    val currentPhoto: PhotoItem? = null,
    val selectedMotionStyle: MotionStyle = MotionStyle.KEN_BURNS,
    val exportSettings: ExportSettings = ExportSettings(),
    val is4KHardwareSupported: Boolean = false,
    val carpetProfile: CarpetShowcaseProfile = CarpetShowcaseProfile(CarpetType.KASHAN),
    val lastExportedFile: File? = null,
    val isSavedToGallery: Boolean = false,
    val errorMessage: String? = null
)

class AnimatorViewModel(application: Application) : AndroidViewModel(application) {

    private val photoRepository: PhotoRepository = PhotoRepositoryImpl(application)
    private val videoEncoder: MediaCodecVideoEncoder = MediaCodecVideoEncoder()
    private val exportRepository: VideoExportRepository = VideoExportRepositoryImpl(application, photoRepository, videoEncoder)
    private val exportManager: VideoExportManager = VideoExportManager(exportRepository)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val renderProgress: StateFlow<RenderProgress> = exportManager.progressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RenderProgress())

    init {
        checkHardwareCapabilities()
        loadDefaultSamplePhotos()
    }

    private fun checkHardwareCapabilities() {
        val can4K = MediaCodecVideoEncoder.is4KSupported()
        _uiState.update { it.copy(is4KHardwareSupported = can4K) }
    }

    fun loadDefaultSamplePhotos() {
        viewModelScope.launch {
            val samples = photoRepository.getSamplePersianCarpets()
            _uiState.update { state ->
                state.copy(
                    selectedPhotos = samples,
                    currentPhoto = samples.firstOrNull()
                )
            }
        }
    }

    fun onPhotosSelected(photos: List<PhotoItem>) {
        if (photos.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                selectedPhotos = photos,
                currentPhoto = photos.first()
            )
        }
    }

    fun selectPhoto(photo: PhotoItem) {
        _uiState.update { it.copy(currentPhoto = photo) }
    }

    fun selectMotionStyle(style: MotionStyle) {
        _uiState.update { it.copy(selectedMotionStyle = style) }
    }

    fun setDuration(seconds: Int) {
        _uiState.update { state ->
            state.copy(exportSettings = state.exportSettings.copy(durationSeconds = seconds))
        }
    }

    fun setResolution(resolution: VideoResolution) {
        _uiState.update { state ->
            state.copy(exportSettings = state.exportSettings.copy(resolution = resolution))
        }
    }

    fun setFrameRate(frameRate: VideoFrameRate) {
        _uiState.update { state ->
            state.copy(exportSettings = state.exportSettings.copy(frameRate = frameRate))
        }
    }

    fun selectCarpetType(type: CarpetType) {
        _uiState.update { state ->
            state.copy(carpetProfile = CarpetShowcaseProfile(carpetType = type))
        }
    }

    fun toggleFiberLighting(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(carpetProfile = state.carpetProfile.copy(enableFiberLightingGleam = enabled))
        }
    }

    fun startRender(onCompleted: (File) -> Unit) {
        val photo = _uiState.value.currentPhoto ?: return
        val style = _uiState.value.selectedMotionStyle
        val settings = _uiState.value.exportSettings

        viewModelScope.launch {
            val result = exportManager.exportVideo(photo, style, settings)
            result.onSuccess { file ->
                _uiState.update { it.copy(lastExportedFile = file, isSavedToGallery = false) }
                onCompleted(file)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.localizedMessage) }
            }
        }
    }

    fun saveExportedVideoToGallery() {
        val file = _uiState.value.lastExportedFile ?: return
        viewModelScope.launch {
            val uri = exportManager.saveToGallery(file)
            if (uri != null) {
                _uiState.update { it.copy(isSavedToGallery = true) }
            }
        }
    }

    fun getShareIntent(): Intent? {
        val file = _uiState.value.lastExportedFile ?: return null
        return exportManager.getShareIntent(file)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
