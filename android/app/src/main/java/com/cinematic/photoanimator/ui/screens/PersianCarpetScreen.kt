package com.cinematic.photoanimator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinematic.photoanimator.data.model.CarpetType
import com.cinematic.photoanimator.data.model.MotionStyle
import com.cinematic.photoanimator.ui.theme.*
import com.cinematic.photoanimator.ui.viewmodel.AnimatorViewModel

@Composable
fun PersianCarpetScreen(
    viewModel: AnimatorViewModel,
    onNavigateBack: () -> Unit,
    onStartExport: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "نمایش لوکس فرش ایرانی",
                    style = MaterialTheme.typography.labelSmall,
                    color = LuxuryGold,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "استودیو عکاسی محصول",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Strict Color Preservation Notice Badge
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    LuxuryGold.copy(alpha = 0.6f),
                    RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = CharcoalSurface
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = LuxuryGold,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "حفظ 100 درصدی رنگ و دامنه رنگی",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 14.sp,
                        color = LuxuryGold,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "بدون تغییر رنگ یا افزایش غیرطبیعی اشباع رنگ. رنگ واقعی الیاف، درخشش ابریشم و تناژ طبیعی پشم بدون تغییر حفظ می شود.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Rug Type Selector
        Text(
            text = "انتخاب نوع و منشا فرش",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CarpetType.values().forEach { carpetType ->
                val isSelected = uiState.carpetProfile.carpetType == carpetType

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) LuxuryGold else BorderSubtle,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            viewModel.selectCarpetType(carpetType)
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            BrushedSlate
                        } else {
                            CharcoalSurface
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = carpetType.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 16.sp,
                                color = if (isSelected) {
                                    LuxuryGold
                                } else {
                                    TextPrimary
                                },
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = carpetType.originCity,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = carpetType.characteristic,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Macro Waypoints Focus Zones
        Text(
            text = "مراحل تمرکز روی جزئیات فرش",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "حرکت سینمایی با نرخ 60 فریم در ثانیه روی 5 بخش اصلی و فیزیکی فرش",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        uiState.carpetProfile.primaryFocusZones.forEachIndexed { index, zone ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(
                        CharcoalSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            LuxuryGold,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianBlack,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "${zone.description} (بزرگنمایی ${zone.zoomLevel} برابر)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Fiber Lighting Simulation Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = CharcoalSurface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "شبیه سازی نورپردازی سطح فرش",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "حرکت نرم نور استودیویی روی بافت پشم و ابریشم را شبیه سازی می کند، بدون تغییر رنگ اصلی فرش",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = uiState.carpetProfile.enableFiberLightingGleam,
                    onCheckedChange = {
                        viewModel.toggleFiberLighting(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LuxuryGold,
                        checkedTrackColor = BrushedSlate
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Action Button: Start Persian Carpet Showcase Export
        Button(
            onClick = {
                viewModel.selectMotionStyle(
                    MotionStyle.PERSIAN_CARPET_LUXURY
                )
                onStartExport()
            },
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
                Icons.Default.Flare,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "ساخت ویدیوی لوکس فرش",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
