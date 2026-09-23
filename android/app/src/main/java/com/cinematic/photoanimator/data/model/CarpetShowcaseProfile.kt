package com.cinematic.photoanimator.data.model

data class CarpetFocusZone(
    val name: String,
    val description: String,
    val normalizedX: Float, // 0.0 to 1.0 (center = 0.5)
    val normalizedY: Float,
    val zoomLevel: Float,   // e.g. 2.4f for macro
    val holdRatio: Float    // proportion of stage duration
)

data class CarpetShowcaseProfile(
    val carpetType: CarpetType,
    val primaryFocusZones: List<CarpetFocusZone> = listOf(
        CarpetFocusZone(
            name = "Center Medallion (Toranj)",
            description = "Central floral celestial sunburst medallion, symbol of Persian eternity",
            normalizedX = 0.50f,
            normalizedY = 0.50f,
            zoomLevel = 1.95f,
            holdRatio = 0.25f
        ),
        CarpetFocusZone(
            name = "Floral Palmette Details (Eslimi)",
            description = "Intricate Shah Abbasi spiraling vines, lotus buds and arabesques",
            normalizedX = 0.35f,
            normalizedY = 0.38f,
            zoomLevel = 2.40f,
            holdRatio = 0.20f
        ),
        CarpetFocusZone(
            name = "Master Border Guard (Hasheeyeh)",
            description = "Triple guard border with traditional cartouche inscriptions and floral guards",
            normalizedX = 0.18f,
            normalizedY = 0.82f,
            zoomLevel = 2.20f,
            holdRatio = 0.20f
        ),
        CarpetFocusZone(
            name = "Wool & Silk Pile Texture",
            description = "Raking studio light across hand-spun high-KPSI pile fibers showing natural luster",
            normalizedX = 0.62f,
            normalizedY = 0.65f,
            zoomLevel = 2.85f,
            holdRatio = 0.20f
        ),
        CarpetFocusZone(
            name = "Hand-Knotted Fringe (Risheh)",
            description = "Authentic cotton/silk foundation fringes confirming master weaver provenance",
            normalizedX = 0.50f,
            normalizedY = 0.95f,
            zoomLevel = 2.10f,
            holdRatio = 0.15f
        )
    ),
    val enableFiberLightingGleam: Boolean = true,
    val strictColorIntegrityVerification: Boolean = true
)
