package com.cinematic.photoanimator.motion

import com.cinematic.photoanimator.data.model.CameraTransform
import com.cinematic.photoanimator.data.model.CarpetFocusZone
import com.cinematic.photoanimator.data.model.CarpetShowcaseProfile
import com.cinematic.photoanimator.data.model.CarpetType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object PersianCarpetShowcaseEngine {

    private val defaultProfile = CarpetShowcaseProfile(CarpetType.KASHAN)

    /**
     * Calculates smooth multi-stage cinematic camera motion specifically tailored
     * for high-end Persian rugs, navigating between:
     * 1. Overview and zoom to Center Medallion (Toranj)
     * 2. Slow glide to Floral Details & Spandrels (Eslimi / Shah Abbasi motifs)
     * 3. Macro pan along Master Border Guard Patterns (Hasheeyeh)
     * 4. Ultra-close focus on hand-knotted pile texture with glancing light sweep
     * 5. Final descent and hold on Master Fringe & Kilims (Risheh)
     *
     * Colors, chromatic values, and tonal curves remain 100% untouched.
     */
    fun calculateCarpetTransform(
        progress: Float,
        profile: CarpetShowcaseProfile = defaultProfile
    ): CameraTransform {
        val zones = profile.primaryFocusZones
        val n = zones.size
        if (n == 0) return CameraTransform(1.0f, 0f, 0f)

        // Segment progress across zones with smooth inter-zone easing
        val totalStages = n
        val scaledProgress = (progress.coerceIn(0f, 1f) * (totalStages - 1))
        val currentStageIndex = scaledProgress.toInt().coerceIn(0, totalStages - 2)
        val stageFraction = (scaledProgress - currentStageIndex).coerceIn(0f, 1f)

        // Smooth hermite / smoothstep curve between waypoints
        val smoothT = smoothStep(stageFraction)

        val zoneA = zones[currentStageIndex]
        val zoneB = zones[currentStageIndex + 1]

        // Interpolate target focus position
        val targetX = zoneA.normalizedX + (zoneB.normalizedX - zoneA.normalizedX) * smoothT
        val targetY = zoneA.normalizedY + (zoneB.normalizedY - zoneA.normalizedY) * smoothT
        val scale = zoneA.zoomLevel + (zoneB.zoomLevel - zoneA.zoomLevel) * smoothT

        // Transform normalized carpet coordinates (0..1, center 0.5) to translation offsets
        val transX = (0.5f - targetX) * (scale - 1f)
        val transY = (0.5f - targetY) * (scale - 1f)

        // Studio directional lighting gleam angle simulating physical softbox traverse across wool/silk pile
        val lightAngle = progress * 180f
        val lightIntensity = if (profile.enableFiberLightingGleam) {
            // Very subtle grazing specular highlight (max 0.12) to reveal weave texture without washing color
            (0.08f * sin(progress * PI.toFloat())).coerceIn(0f, 0.15f)
        } else 0f

        val subtleTiltX = sin(progress * 2f * PI.toFloat()) * 1.2f
        val subtleTiltY = cos(progress * 2f * PI.toFloat()) * 1.5f

        return CameraTransform(
            scale = scale,
            translationX = transX,
            translationY = transY,
            rotationZ = sin(progress * PI.toFloat()) * 0.2f,
            tiltX = subtleTiltX,
            tiltY = subtleTiltY,
            lightAngle = lightAngle,
            lightIntensity = lightIntensity
        )
    }

    private fun smoothStep(t: Float): Float {
        return t * t * (3f - 2f * t)
    }
}
