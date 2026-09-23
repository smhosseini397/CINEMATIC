package com.cinematic.photoanimator.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinematic.photoanimator.data.model.VideoOrientation
import com.cinematic.photoanimator.ui.theme.*
import com.cinematic.photoanimator.ui.viewmodel.AnimatorViewModel

@Composable
fun ExportScreen(
    viewModel: AnimatorViewModel,
    onNavigateHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val progress by viewModel.renderProgress.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.startRender {
            // Render completed callback
        }
    }

    val settings = uiState.exportSettings
    val orientation = settings.orientation
    val width = settings.outputWidth
    val height = settings.outputHeight
    val fps = settings.frameRate.fps

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateHome) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "بستن",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (progress.isCompleted) {
                    "ویدیو آماده است"
                } else {
                    "در حال ساخت ویدیو"
                },
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (!progress.isCompleted) {

                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress.percentage / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = LuxuryGold,
                        trackColor = BrushedSlate,
                        strokeWidth = 10.dp
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${progress.percentage.toInt()}%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = LuxuryGold
                        )

                        Text(
                            text = "فریم ${progress.currentFrame} از ${progress.totalFrames}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CharcoalSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "زمان تقریبی باقی مانده",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )

                            Text(
                                progress.formattedRemainingTime,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress.percentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = LuxuryGold,
                            trackColor = BrushedSlate
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "انکودر",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )

                            Text(
                                "MediaCodec H.264 با شتاب سخت افزاری",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "جهت ویدیو",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )

                            Text(
                                when (orientation) {
                                    VideoOrientation.PORTRAIT -> "عمودی 9:16"
                                    VideoOrientation.LANDSCAPE -> "افقی 16:9"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = LuxuryGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "مشخصات خروجی",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )

                            Text(
                                "${width} × ${height}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "فرمت",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )

                            Text(
                                "MP4 · H.264 · $fps FPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

            } else {

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            SuccessGreen.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "ویدیوی سینمایی آماده شد!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "خروجی با حفظ رنگ و کیفیت اصلی تصویر تکمیل شد",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CharcoalSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Text(
                            text = "جزئیات خروجی",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 14.sp,
                            color = LuxuryGold
                        )

                        Text(
                            text = "فرمت: MP4 (H.264 High Profile / $fps FPS)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )

                        Text(
                            text = "جهت: ${
                                when (orientation) {
                                    VideoOrientation.PORTRAIT -> "عمودی 9:16"
                                    VideoOrientation.LANDSCAPE -> "افقی 16:9"
                                }
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )

                        Text(
                            text = "رزولوشن: $width × $height",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )

                        Text(
                            text = "مدت زمان: ${settings.durationSeconds} ثانیه",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )

                        Text(
                            text = "فایل: ${uiState.lastExportedFile?.name ?: "video.mp4"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (progress.isCompleted) {

                Button(
                    onClick = {
                        viewModel.saveExportedVideoToGallery()

                        Toast.makeText(
                            context,
                            "ویدیو در گالری ذخیره شد",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isSavedToGallery) {
                            SuccessGreen
                        } else {
                            LuxuryGold
                        },
                        contentColor = ObsidianBlack
                    )
                ) {

                    Icon(
                        imageVector = if (uiState.isSavedToGallery) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.SaveAlt
                        },
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (uiState.isSavedToGallery) {
                            "در گالری ذخیره شد"
                        } else {
                            "ذخیره در گالری گوشی"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        val shareIntent = viewModel.getShareIntent()

                        if (shareIntent != null) {
                            context.startActivity(
                                android.content.Intent.createChooser(
                                    shareIntent,
                                    "اشتراک گذاری ویدیوی سینمایی"
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            LuxuryGold
                        )
                    )
                ) {

                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = LuxuryGold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("اشتراک گذاری فایل ویدیو")
                }

                TextButton(
                    onClick = onNavigateHome,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "پایان / بازگشت به پروژه ها",
                        color = TextSecondary
                    )
                }

            } else {

                Text(
                    text = "لطفا تا پایان ساخت فریم ها، برنامه را باز نگه دارید",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
