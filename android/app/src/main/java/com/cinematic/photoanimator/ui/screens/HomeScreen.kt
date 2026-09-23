package com.cinematic.photoanimator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinematic.photoanimator.ui.theme.*
import com.cinematic.photoanimator.ui.viewmodel.AnimatorViewModel

@Composable
fun HomeScreen(
    viewModel: AnimatorViewModel,
    onLaunchSinglePicker: () -> Unit,
    onLaunchMultiPicker: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onNavigateToCarpetShowcase: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "سینماتیک",
                    style = MaterialTheme.typography.labelSmall,
                    color = LuxuryGold,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "انیمیشن عکس",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
            }

            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = LuxuryGold,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    LuxuryGold.copy(alpha = 0.5f),
                    RoundedCornerShape(14.dp)
                )
                .clickable {
                    onNavigateToCarpetShowcase()
                },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = CharcoalSurface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            LuxuryGold.copy(alpha = 0.15f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = LuxuryGold,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "نمایش لوکس فرش ایرانی",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "کاشان، تبریز، اصفهان · نمایش بافت و طرح فرش",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = LuxuryGold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "انتخاب عکس از گالری",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Text(
            text = "حفظ رنگ و کیفیت اصلی عکس بدون فشرده سازی",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onLaunchSinglePicker,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrushedSlate,
                    contentColor = TextPrimary
                )
            ) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("انتخاب عکس")
            }

            Button(
                onClick = onLaunchMultiPicker,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrushedSlate,
                    contentColor = TextPrimary
                )
            ) {
                Icon(
                    Icons.Default.Collections,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("انتخاب چند عکس")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.selectedPhotos.isNotEmpty()) {
            Text(
                text = "عکس های انتخاب شده (${uiState.selectedPhotos.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.selectedPhotos) { photo ->

                    val isSelected =
                        uiState.currentPhoto?.id == photo.id

                    Card(
                        modifier = Modifier
                            .size(130.dp, 160.dp)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color =
                                    if (isSelected)
                                        LuxuryGold
                                    else
                                        BorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.selectPhoto(photo)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CharcoalSurface
                        )
                    ) {
                        Column {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(105.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = photo.name,
                                    style =
                                        MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    maxLines = 1
                                )

                                Text(
                                    text =
                                        "${photo.width} × ${photo.height} · " +
                                                photo.colorSpaceName,
                                    style =
                                        MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = LuxuryGold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onNavigateToEditor,
            enabled = uiState.currentPhoto != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LuxuryGold,
                contentColor = ObsidianBlack,
                disabledContainerColor = BrushedSlate
            )
        ) {
            Icon(
                Icons.Default.MovieFilter,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "باز کردن ویرایشگر حرکت و پیش نمایش",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
