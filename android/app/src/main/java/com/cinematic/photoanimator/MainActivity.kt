package com.cinematic.photoanimator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cinematic.photoanimator.picker.GalleryPickerManager
import com.cinematic.photoanimator.ui.screens.EditorScreen
import com.cinematic.photoanimator.ui.screens.ExportScreen
import com.cinematic.photoanimator.ui.screens.HomeScreen
import com.cinematic.photoanimator.ui.screens.PersianCarpetScreen
import com.cinematic.photoanimator.ui.theme.CinematicTheme
import com.cinematic.photoanimator.ui.theme.ObsidianBlack
import com.cinematic.photoanimator.ui.viewmodel.AnimatorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AnimatorViewModel by viewModels()

    private lateinit var galleryPickerManager: GalleryPickerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize modern Android Photo Picker
        galleryPickerManager = GalleryPickerManager(
            activity = this,
            photoRepository = com.cinematic.photoanimator.data.repository.PhotoRepositoryImpl(this),
            scope = lifecycleScope,
            onPhotosSelected = { photos ->
                viewModel.onPhotosSelected(photos)
            }
        )

        setContent {
            CinematicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ObsidianBlack
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        onLaunchSinglePicker = { galleryPickerManager.launchSinglePicker() },
                        onLaunchMultiPicker = { galleryPickerManager.launchMultiPicker() }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: AnimatorViewModel,
    onLaunchSinglePicker: () -> Unit,
    onLaunchMultiPicker: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onLaunchSinglePicker = onLaunchSinglePicker,
                onLaunchMultiPicker = onLaunchMultiPicker,
                onNavigateToEditor = {
                    navController.navigate("editor")
                },
                onNavigateToCarpetShowcase = {
                    navController.navigate("carpet_showcase")
                }
            )
        }

        composable("editor") {
            EditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onStartExport = { navController.navigate("export") }
            )
        }

        composable("carpet_showcase") {
            PersianCarpetScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onStartExport = { navController.navigate("export") }
            )
        }

        composable("export") {
            ExportScreen(
                viewModel = viewModel,
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
