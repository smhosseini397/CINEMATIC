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
            name = "ترنج مرکزی",
            description = "ترنج مرکزی با طرح گل و نقش های ظریف که نمادی از زیبایی و اصالت هنر فرش ایرانی است",
            normalizedX = 0.50f,
            normalizedY = 0.50f,
            zoomLevel = 1.95f,
            holdRatio = 0.25f
        ),
        CarpetFocusZone(
            name = "جزئیات گل و اسلیمی",
            description = "جزئیات ظریف گل های شاه عباسی، پیچش اسلیمی ها، غنچه ها و نقش های تزئینی",
            normalizedX = 0.35f,
            normalizedY = 0.38f,
            zoomLevel = 2.40f,
            holdRatio = 0.20f
        ),
        CarpetFocusZone(
            name = "حاشیه اصلی فرش",
            description = "نمای نزدیک از حاشیه های سنتی فرش و جزئیات دقیق گل و نقش های تزئینی",
            normalizedX = 0.18f,
            normalizedY = 0.82f,
            zoomLevel = 2.20f,
            holdRatio = 0.20f
        ),
        CarpetFocusZone(
            name = "بافت پرز و الیاف",
            description = "نورپردازی نرم استودیویی روی بافت پرزهای فرش برای نمایش ظرافت، تراکم و درخشش طبیعی الیاف",
            normalizedX = 0.62f,
            normalizedY = 0.65f,
            zoomLevel = 2.85f,
            holdRatio = 0.20f
        ),
        CarpetFocusZone(
            name = "ریشه های دستبافت",
            description = "نمای نزدیک از ریشه های فرش و جزئیات اتصال آنها که ظرافت و کیفیت بافت فرش را نشان می دهد",
            normalizedX = 0.50f,
            normalizedY = 0.95f,
            zoomLevel = 2.10f,
            holdRatio = 0.15f
        )
    ),
    val enableFiberLightingGleam: Boolean = true,
    val strictColorIntegrityVerification: Boolean = true
)
