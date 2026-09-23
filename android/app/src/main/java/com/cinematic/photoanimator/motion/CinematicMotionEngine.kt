package com.cinematic.photoanimator.motion

import com.cinematic.photoanimator.data.model.CameraTransform
import com.cinematic.photoanimator.data.model.MotionStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object CinematicMotionEngine {

    /**
     * Calculates the camera transformation for any given normalized progress (0.0 to 1.0).
     * Smooth 60 FPS interpolations use cubic easing and trigonometric harmonics.
     */
    fun calculateTransform(
        style: MotionStyle,
        progress: Float,
        aspectRatio: Float = 16f / 9f
    ): CameraTransform {
        val clampedProgress = progress.coerceIn(0f, 1f)
        val easedProgress = cubicEaseInOut(clampedProgress)

        return when (style) {
            MotionStyle.SLOW_ZOOM_IN -> {
                val scale = 1.0f + (0.35f * easedProgress)
                CameraTransform(
                    scale = scale,
                    translationX = 0f,
                    translationY = 0f,
                    rotationZ = 0f
                )
            }

            MotionStyle.SLOW_ZOOM_OUT -> {
                val scale = 1.40f - (0.35f * easedProgress)
                CameraTransform(
                    scale = scale,
                    translationX = 0f,
                    translationY = 0f,
                    rotationZ = 0f
                )
            }

            MotionStyle.KEN_BURNS -> {
                // Diagonal pan from top-left (-0.12, -0.08) to bottom-right (0.10, 0.06) with zoom
                val scale = 1.15f + (0.25f * easedProgress)
                val transX = -0.12f + (0.22f * easedProgress)
                val transY = -0.08f + (0.14f * easedProgress)
                val subtleRotation = sin(easedProgress * PI.toFloat()) * 0.4f

                CameraTransform(
                    scale = scale,
                    translationX = transX,
                    translationY = transY,
                    rotationZ = subtleRotation
                )
            }

            MotionStyle.SMOOTH_PAN_HORIZONTAL -> {
                val scale = 1.25f
                val transX = -0.22f + (0.44f * easedProgress)
                CameraTransform(
                    scale = scale,
                    translationX = transX,
                    translationY = 0f
                )
            }

            MotionStyle.VERTICAL_MOVEMENT -> {
                val scale = 1.25f
                val transY = 0.20f - (0.40f * easedProgress)
                val tiltX = sin(easedProgress * PI.toFloat()) * 1.5f
                CameraTransform(
                    scale = scale,
                    translationX = 0f,
                    translationY = transY,
                    tiltX = tiltX
                )
            }

            MotionStyle.PARALLAX_3D -> {
                val scale = 1.28f + (0.10f * sin(easedProgress * PI.toFloat()))
                val angle = easedProgress * 2f * PI.toFloat()
                val transX = cos(angle) * 0.08f
                val transY = sin(angle) * 0.05f
                val tiltX = sin(angle) * 3.5f
                val tiltY = cos(angle) * 4.0f
                val lightAngle = easedProgress * 360f

                CameraTransform(
                    scale = scale,
                    translationX = transX,
                    translationY = transY,
                    tiltX = tiltX,
                    tiltY = tiltY,
                    lightAngle = lightAngle,
                    lightIntensity = 0.15f
                )
            }

            MotionStyle.DOCUMENTARY_DRIFT -> {
                // Organic harmonic oscillation simulating handheld precision rig
                val t = easedProgress * 4f * PI.toFloat()
                val transX = (sin(t) * 0.035f) + (cos(t * 0.5f) * 0.02f)
                val transY = (cos(t * 0.8f) * 0.030f) + (sin(t * 0.3f) * 0.015f)
                val scale = 1.18f + (sin(t * 0.5f) * 0.04f)
                val rot = sin(t * 0.6f) * 0.75f

                CameraTransform(
                    scale = scale,
                    translationX = transX,
                    translationY = transY,
                    rotationZ = rot
                )
            }

            MotionStyle.LUXURY_SHOWCASE -> {
                // Sweeping orbital arc with subtle lighting angle change
                val scale = 1.30f + (sin(easedProgress * PI.toFloat()) * 0.15f)
                val transX = sin((easedProgress - 0.5f) * PI.toFloat()) * 0.18f
                val transY = cos((easedProgress - 0.5f) * PI.toFloat()) * 0.08f - 0.05f
                val lightAngle = easedProgress * 180f
                val lightIntensity = sin(easedProgress * PI.toFloat()) * 0.25f

                CameraTransform(
                    scale = scale,
                    translationX = transX,
                    translationY = transY,
                    tiltX = sin(easedProgress * PI.toFloat()) * 2.5f,
                    tiltY = cos(easedProgress * PI.toFloat()) * 3.0f,
                    lightAngle = lightAngle,
                    lightIntensity = lightIntensity
                )
            }

            MotionStyle.PERSIAN_CARPET_LUXURY -> {
                // Delegates to specialized multi-zone carpet engine
                PersianCarpetShowcaseEngine.calculateCarpetTransform(clampedProgress)
            }
        }
    }

    private fun cubicEaseInOut(t: Float): Float {
        return if (t < 0.5f) {
            4f * t * t * t
        } else {
            1f - (-2f * t + 2f).let { it * it * it } / 2f
        }
    }
}
