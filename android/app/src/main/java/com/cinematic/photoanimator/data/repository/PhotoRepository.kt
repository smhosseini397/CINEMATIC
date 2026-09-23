package com.cinematic.photoanimator.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.cinematic.photoanimator.data.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

interface PhotoRepository {
    suspend fun loadPhotoMetadata(uri: Uri): PhotoItem
    suspend fun loadHighResBitmap(uri: Uri, targetWidth: Int, targetHeight: Int): Bitmap
    suspend fun getSamplePersianCarpets(): List<PhotoItem>
}

class PhotoRepositoryImpl(private val context: Context) : PhotoRepository {

    override suspend fun loadPhotoMetadata(uri: Uri): PhotoItem = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val name = uri.lastPathSegment ?: "Photo_${System.currentTimeMillis()}"
        val width = options.outWidth
        val height = options.outHeight
        val colorSpace = options.outColorSpace?.name ?: "sRGB"

        PhotoItem(
            id = UUID.randomUUID().toString(),
            uri = uri,
            name = name,
            width = width,
            height = height,
            colorSpaceName = colorSpace
        )
    }

    override suspend fun loadHighResBitmap(
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        // Calculate sample size preserving maximum fidelity without memory exhaustion
        var sampleSize = 1
        val rawWidth = options.outWidth
        val rawHeight = options.outHeight

        if (rawWidth > targetWidth || rawHeight > targetHeight) {
            val halfWidth = rawWidth / 2
            val halfHeight = rawHeight / 2
            while ((halfWidth / sampleSize) >= targetWidth && (halfHeight / sampleSize) >= targetHeight) {
                sampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            // Strictly preserve original color gamut without automatic clipping or tone curve alteration
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            }
            inPremultiplied = true
        }

        val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalArgumentException("Could not decode bitmap from URI: $uri")

        // Read EXIF orientation to ensure correct portrait/landscape alignment without distorting pixels
        val rotation = getExifOrientation(uri)
        if (rotation != 0) {
            val matrix = android.graphics.Matrix().apply {
                postRotate(rotation.toFloat())
            }
            Bitmap.createBitmap(
                rawBitmap,
                0,
                0,
                rawBitmap.width,
                rawBitmap.height,
                matrix,
                true
            )
        } else {
            rawBitmap
        }
    }

    private fun getExifOrientation(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun getSamplePersianCarpets(): List<PhotoItem> = withContext(Dispatchers.IO) {
        listOf(
            PhotoItem(
                id = "sample_kashan",
                uri = Uri.parse("android.resource://${context.packageName}/drawable/sample_kashan"),
                name = "Kashan Royal Medallion",
                width = 3840,
                height = 2560,
                colorSpaceName = "sRGB Authentic",
                isPersianCarpet = true,
                carpetType = com.cinematic.photoanimator.data.model.CarpetType.KASHAN
            ),
            PhotoItem(
                id = "sample_tabriz",
                uri = Uri.parse("android.resource://${context.packageName}/drawable/sample_tabriz"),
                name = "Tabriz Silk 70-Raj Masterpiece",
                width = 3840,
                height = 2560,
                colorSpaceName = "sRGB Authentic",
                isPersianCarpet = true,
                carpetType = com.cinematic.photoanimator.data.model.CarpetType.TABRIZ
            ),
            PhotoItem(
                id = "sample_isnfahan",
                uri = Uri.parse("android.resource://${context.packageName}/drawable/sample_isnfahan"),
                name = "Isfahan Sheikh Lotfollah Ceiling Motif",
                width = 3840,
                height = 2560,
                colorSpaceName = "sRGB Authentic",
                isPersianCarpet = true,
                carpetType = com.cinematic.photoanimator.data.model.CarpetType.ISFAHAN
            ),
            PhotoItem(
                id = "sample_nain",
                uri = Uri.parse("android.resource://${context.packageName}/drawable/sample_nain"),
                name = "Nain Habibian Ivory & Sapphire",
                width = 3840,
                height = 2560,
                colorSpaceName = "sRGB Authentic",
                isPersianCarpet = true,
                carpetType = com.cinematic.photoanimator.data.model.CarpetType.NAIN
            ),
            PhotoItem(
                id = "sample_qom",
                uri = Uri.parse("android.resource://${context.packageName}/drawable/sample_qom"),
                name = "Qom 100% Pure Mulberry Silk Tree of Life",
                width = 3840,
                height = 2560,
                colorSpaceName = "sRGB Authentic",
                isPersianCarpet = true,
                carpetType = com.cinematic.photoanimator.data.model.CarpetType.QOM
            )
        )
    }
}
