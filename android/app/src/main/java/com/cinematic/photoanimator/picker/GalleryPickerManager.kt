package com.cinematic.photoanimator.picker

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.cinematic.photoanimator.data.model.PhotoItem
import com.cinematic.photoanimator.data.repository.PhotoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GalleryPickerManager(
    private val activity: ComponentActivity,
    private val photoRepository: PhotoRepository,
    private val scope: CoroutineScope,
    private val onPhotosSelected: (List<PhotoItem>) -> Unit
) {

    // Modern Android Photo Picker for single image
    private val singlePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    val item = photoRepository.loadPhotoMetadata(it)
                    launch(Dispatchers.Main) {
                        onPhotosSelected(listOf(item))
                    }
                }
            }
        }

    // Modern Android Photo Picker for multiple high-resolution images
    private val multiPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        activity.registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                scope.launch(Dispatchers.IO) {
                    val items = uris.map { photoRepository.loadPhotoMetadata(it) }
                    launch(Dispatchers.Main) {
                        onPhotosSelected(items)
                    }
                }
            }
        }

    fun launchSinglePicker() {
        singlePickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun launchMultiPicker() {
        multiPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}
