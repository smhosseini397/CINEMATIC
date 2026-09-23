package com.cinematic.photoanimator.data.model

enum class MotionStyle(
    val title: String,
    val description: String,
    val iconName: String
) {
    SLOW_ZOOM_IN(
        "زوم سینمایی آرام به داخل",
        "حرکت نرم و سینمایی دوربین به سمت مرکز تصویر با شتابی ملایم و طبیعی",
        "zoom_in"
    ),
    SLOW_ZOOM_OUT(
        "زوم سینمایی آرام به بیرون",
        "عقب رفتن نرم دوربین برای نمایش کامل تصویر و حاشیه های آن",
        "zoom_out"
    ),
    KEN_BURNS(
        "حرکت پویا به سبک کن برنز",
        "حرکت مستندگونه دوربین همراه با زوم مورب و ظریف برای ایجاد حس طبیعی",
        "auto_awesome"
    ),
    SMOOTH_PAN_HORIZONTAL(
        "حرکت نرم افقی",
        "حرکت روان دوربین از یک سمت به سمت دیگر برای نمایش گستره تصویر بدون تغییر شکل",
        "swap_horiz"
    ),
    VERTICAL_MOVEMENT(
        "حرکت عمودی دوربین",
        "حرکت نرم دوربین از جزئیات پایین تصویر به سمت بخش بالایی",
        "swap_vert"
    ),
    PARALLAX_3D(
        "عمق سه بعدی پارالاکس",
        "ایجاد عمق چند لایه و حرکت فضایی برای شبیه سازی جلوه سینمایی حرفه ای",
        "layers"
    ),
    DOCUMENTARY_DRIFT(
        "حرکت طبیعی مستند",
        "حرکت بسیار ظریف و طبیعی دوربین با نوسان ملایم برای ایجاد حس تصویربرداری واقعی",
        "videocam"
    ),
    LUXURY_SHOWCASE(
        "نمایش لوکس محصول",
        "حرکت منحنی و گسترده دوربین با جلوه نورپردازی استودیویی برای نمایش حرفه ای محصول",
        "diamond"
    ),
    PERSIAN_CARPET_LUXURY(
        "نمایش لوکس فرش ایرانی",
        "حرکت سینمایی روی ترنج، گل های شاه عباسی، حاشیه و بافت ظریف فرش",
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
