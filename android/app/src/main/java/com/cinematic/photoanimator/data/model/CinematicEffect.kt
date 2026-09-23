package com.cinematic.photoanimator.data.model

enum class MotionStyle(
    val title: String,
    val description: String,
    val iconName: String
) {
    SLOW_ZOOM_IN(
        "Slow Cinematic Zoom In",
        "Gentle, filmic acceleration focusing into the emotional core at 60 FPS",
        "zoom_in"
    ),
    SLOW_ZOOM_OUT(
        "Slow Cinematic Zoom Out",
        "Reveals the grand context and full borders with smooth cinematic deceleration",
        "zoom_out"
    ),
    KEN_BURNS(
        "Ken Burns Dynamic",
        "Signature documentary pan combined with subtle organic diagonal zoom",
        "auto_awesome"
    ),
    SMOOTH_PAN_HORIZONTAL(
        "Smooth Lateral Pan",
        "Gliding horizontal camera dolly showcasing panoramic span without distortion",
        "swap_horiz"
    ),
    VERTICAL_MOVEMENT(
        "Vertical Camera Glide",
        "Elevating camera movement from foreground texture to top crown",
        "swap_vert"
    ),
    PARALLAX_3D(
        "3D Parallax Depth",
        "Multi-plane spatial depth displacement simulating high-end cinema prime lens",
        "layers"
    ),
    DOCUMENTARY_DRIFT(
        "Documentary Handheld Drift",
        "Organic subtle camera breathe and natural harmonic movement",
        "videocam"
    ),
    LUXURY_SHOWCASE(
        "Luxury Product Showcase",
        "Sweeping orbital curve with dynamic studio rim lighting shimmer",
        "diamond"
    ),
    PERSIAN_CARPET_LUXURY(
        "Persian Carpet Showcase",
        "Macro focus across medallion, floral palmettes, border knotting & silk sheen",
        "flare"
    )
}

data class CameraTransform(
    val scale: Float,
    val translationX: Float,
    val translationY: Float,
    val rotationZ: Float = 0f,
    val tiltX: Float = 0f,
    val tiltY: Float = 0f,
    val lightAngle: Float = 0f,
    val lightIntensity: Float = 0f
)
