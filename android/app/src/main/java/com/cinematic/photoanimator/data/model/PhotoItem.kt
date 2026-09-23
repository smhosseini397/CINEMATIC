package com.cinematic.photoanimator.data.model

import android.net.Uri

data class PhotoItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long = 0L,
    val colorSpaceName: String = "sRGB",
    val isPersianCarpet: Boolean = false,
    val carpetType: CarpetType = CarpetType.KASHAN
)

enum class CarpetType(val displayName: String, val originCity: String, val characteristic: String) {
    KASHAN("Kashan Classic", "Kashan, Iran", "Central medallion with Shah Abbasi floral spandrels and deep royal ruby"),
    TABRIZ("Tabriz Masterpiece", "Tabriz, Iran", "High KPSI silk highlights, intricate hunting & paisley motifs"),
    ISFAHAN("Isfahan Royal", "Isfahan, Iran", "Dome-inspired concentric harmony, pure silk warp and delicate arabesque"),
    NAIN("Nain Habibian 6La", "Nain, Iran", "Ivory, midnight blue, high silk density with botanical palmettes"),
    QOM("Qom Pure Silk", "Qom, Iran", "100% fine mulberry silk pile, luminescent sheen and razor-sharp detail")
}
