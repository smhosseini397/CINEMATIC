package com.cinematic.photoanimator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinematic.photoanimator.data.model.MotionStyle
import com.cinematic.photoanimator.data.model.VideoFrameRate
import com.cinematic.photoanimator.data.model.VideoOrientation
import com.cinematic.photoanimator.data.model.VideoResolution
import com.cinematic.photoanimator.motion.CinematicMotionEngine
import com.cinematic.photoanimator.ui.theme.*
import com.cinematic.photoanimator.ui.viewmodel.AnimatorViewModel
import kotlinx.coroutines.isActive
import androidx.compose.runtime.withFrameNanos

@Composable
fun EditorScreen(
    viewModel: AnimatorViewModel,
    onNavigateBack: () -> Unit,
    onStartExport: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    /*
     * Time Engine Preview
     *
     * Instead of relying on an infinite transition, the preview clock
     * is driven directly from frame time. This guarantees that the
     * CinematicMotionEngine receives continuously changing progress.
     */
    var previewProgress by remember {
        mutableFloatStateOf(0f)
    }

    val previewDurationMs =
        (uiState.exportSettings.durationSeconds * 1000L)
            .coerceAtLeast(1000L)

    LaunchedEffect(
        uiState.currentPhoto?.id,
        uiState.selectedMotionStyle,
        uiState.exportSettings.durationSeconds,
        uiState.exportSettings.orientation
    ) {
        previewProgress = 0f

        var startTimeNanos = 0L

        while (isActive) {
            val frameTimeNanos = withFrameNanos { it }

            if (startTimeNanos == 0L) {
                startTimeNanos = frameTimeNanos
            }

            val elapsedMillis =
                (frameTimeNanos - startTimeNanos) / 1_000_000L

            previewProgress =
                ((elapsedMillis % previewDurationMs).toFloat() /
                        previewDurationMs.toFloat())
                    .coerceIn(0f, 1f)
        }
    }

    val transform =
        CinematicMotionEngine.calculateTransform(
            style = uiState.selectedMotionStyle,
            progress = previewProgress,
            aspectRatio = uiState.exportSettings.orientation.aspectRatio
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "استودیو حرکت سینمایی",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "پیش نمایش موتور زمان",
                    style = MaterialTheme.typography.labelSmall,
                    color = LuxuryGold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(
                    uiState.exportSettings.orientation.aspectRatio
                )
                .clip(RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    LuxuryGold.copy(alpha = 0.4f),
                    RoundedCornerShape(14.dp)
                )
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {

            val photo = uiState.currentPhoto

            if (photo != null) {

                AsyncImage(
                    model = photo.uri,
                    contentDescription = "پیش نمایش",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {

                            /*
                             * 2D cinematic movement
                             */
                            translationX =
                                transform.translationX * size.width

                            translationY =
                                transform.translationY * size.height

                            scaleX = transform.scale
                            scaleY = transform.scale

                            rotationZ = transform.rotationZ

                            /*
                             * 3D cinematic movement
                             */
                            rotationX = transform.tiltX
                            rotationY = transform.tiltY
                        },
                    contentScale = ContentScale.Crop
                )

            } else {

                Text(
                    text = "هیچ عکسی انتخاب نشده است",
                    color = TextMuted
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(
                        Color.Black.copy(alpha = 0.65f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
            ) {
                Text(
                    text =
                        uiState.exportSettings.orientation.shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = LuxuryGold,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(
                        Color.Black.copy(alpha = 0.65f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
            ) {
                Text(
                    text =
                        uiState.selectedMotionStyle.title +
                                " · بزرگنمایی: " +
                                String.format(
                                    "%.2f",
                                    transform.scale
                                ) +
                                "x · " +
                                "${uiState.exportSettings.frameRate.fps} FPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "سبک حرکت دوربین",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "انتخاب الگوی حرفه ای حرکت دوربین سینمایی",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MotionStyle.values().forEach { style ->

                val isSelected =
                    uiState.selectedMotionStyle == style

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width =
                                if (isSelected) 1.5.dp else 1.dp,
                            color =
                                if (isSelected)
                                    LuxuryGold
                                else
                                    BorderSubtle,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            viewModel.selectMotionStyle(style)
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (isSelected)
                                BrushedSlate
                            else
                                CharcoalSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectMotionStyle(style)
                            },
                            colors =
                                RadioButtonDefaults.colors(
                                    selectedColor = LuxuryGold
                                )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = style.title,
                                style =
                                    MaterialTheme.typography.titleLarge,
                                fontSize = 16.sp,
                                color =
                                    if (isSelected)
                                        LuxuryGold
                                    else
                                        TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = style.description,
                                style =
                                    MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "مدت زمان ویدیو",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            listOf(5, 10, 15).forEach { seconds ->

                val isSelected =
                    uiState.exportSettings.durationSeconds == seconds

                Button(
                    onClick = {
                        viewModel.setDuration(seconds)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (isSelected)
                                LuxuryGold
                            else
                                BrushedSlate,
                        contentColor =
                            if (isSelected)
                                ObsidianBlack
                            else
                                TextPrimary
                    )
                ) {
                    Text(
                        "$seconds ثانیه",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "جهت ویدیو",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "فرمت نهایی ویدیو را انتخاب کنید",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            VideoOrientation.values().forEach { orientation ->

                val isSelected =
                    uiState.exportSettings.orientation == orientation

                Button(
                    onClick = {
                        viewModel.setOrientation(orientation)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (isSelected)
                                LuxuryGold
                            else
                                BrushedSlate,
                        contentColor =
                            if (isSelected)
                                ObsidianBlack
                            else
                                TextPrimary
                    )
                ) {
                    Text(
                        text =
                            if (
                                orientation ==
                                VideoOrientation.PORTRAIT
                            ) {
                                "عمودی 9:16"
                            } else {
                                "افقی 16:9"
                            },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "کیفیت خروجی و انکودر سخت افزاری",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Button(
                onClick = {
                    viewModel.setResolution(
                        VideoResolution.FHD_1080P
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (
                            uiState.exportSettings.resolution ==
                            VideoResolution.FHD_1080P
                        )
                            LuxuryGold
                        else
                            BrushedSlate,
                    contentColor =
                        if (
                            uiState.exportSettings.resolution ==
                            VideoResolution.FHD_1080P
                        )
                            ObsidianBlack
                        else
                            TextPrimary
                )
            ) {
                Text("1080p FHD")
            }

            Button(
                onClick = {
                    viewModel.setResolution(
                        VideoResolution.UHD_4K
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (
                            uiState.exportSettings.resolution ==
                            VideoResolution.UHD_4K
                        )
                            LuxuryGold
                        else
                            BrushedSlate,
                    contentColor =
                        if (
                            uiState.exportSettings.resolution ==
                            VideoResolution.UHD_4K
                        )
                            ObsidianBlack
                        else
                            TextPrimary
                )
            ) {
                Text("4K UHD")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Button(
                onClick = {
                    viewModel.setFrameRate(
                        VideoFrameRate.FPS_30
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (
                            uiState.exportSettings.frameRate ==
                            VideoFrameRate.FPS_30
                        )
                            LuxuryGold
                        else
                            BrushedSlate,
                    contentColor =
                        if (
                            uiState.exportSettings.frameRate ==
                            VideoFrameRate.FPS_30
                        )
                            ObsidianBlack
                        else
                            TextPrimary
                )
            ) {
                Text("30 FPS")
            }

            Button(
                onClick = {
                    viewModel.setFrameRate(
                        VideoFrameRate.FPS_60
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (
                            uiState.exportSettings.frameRate ==
                            VideoFrameRate.FPS_60
                        )
                            LuxuryGold
                        else
                            BrushedSlate,
                    contentColor =
                        if (
                            uiState.exportSettings.frameRate ==
                            VideoFrameRate.FPS_60
                        )
                            ObsidianBlack
                        else
                            TextPrimary
                )
            ) {
                Text("60 FPS روان")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = CharcoalSurface
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = "خروجی نهایی",
                    style = MaterialTheme.typography.titleMedium,
                    color = LuxuryGold,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text =
                        "${uiState.exportSettings.outputWidth} × " +
                                "${uiState.exportSettings.outputHeight}  •  " +
                                uiState.exportSettings.orientation.shortLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onStartExport,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LuxuryGold,
                contentColor = ObsidianBlack
            )
        ) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "ساخت ویدیوی MP4",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
