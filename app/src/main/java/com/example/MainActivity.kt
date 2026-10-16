package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.SMCutRepository
import com.example.model.VideoClip
import com.example.notification.FCMManager
import com.example.ui.components.SMCutTopBar
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class AppScreen(val title: String, val icon: ImageVector) {
    STUDIO("Découper", Icons.Default.ContentCut),
    DOWNLOADER("Télécharger", Icons.Default.DownloadForOffline),
    HISTORY("Historique", Icons.Default.History),
    SUBSCRIPTION("Abonnement", Icons.Default.CreditCard),
    ADMIN("Admin", Icons.Default.AdminPanelSettings),
    ACCOUNT("Compte", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: SMCutRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = SMCutRepository(applicationContext)

        // Initialize Firebase Cloud Messaging & Notification Channels
        FCMManager.initFCM(applicationContext)

        val targetScreen = if (intent?.getStringExtra("OPEN_TARGET") == "HISTORY") {
            AppScreen.HISTORY
        } else {
            AppScreen.STUDIO
        }

        setContent {
            MyApplicationTheme {
                MainAppScaffold(repository = repository, initialScreen = targetScreen)
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    repository: SMCutRepository,
    initialScreen: AppScreen = AppScreen.STUDIO
) {
    val context = LocalContext.current
    val currentUser by repository.currentUser.collectAsState()
    var currentScreen by remember { mutableStateOf(initialScreen) }
    var selectedClipForDetail by remember { mutableStateOf<VideoClip?>(null) }

    // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        FCMManager.setPushEnabled(context, isGranted)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Predictive / Custom back handling
    BackHandler(enabled = currentScreen != AppScreen.STUDIO || selectedClipForDetail != null) {
        if (selectedClipForDetail != null) {
            selectedClipForDetail = null
        } else {
            currentScreen = AppScreen.STUDIO
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            SMCutTopBar(
                user = currentUser,
                onOpenProfile = { currentScreen = AppScreen.ACCOUNT },
                onAdminClick = { currentScreen = AppScreen.ADMIN }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = PanelBg,
                contentColor = TextInk,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = BorderLine,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            ) {
                // Always show Admin tab so administrator can access their tools at any time
                val screensToShow = listOf(
                    AppScreen.STUDIO,
                    AppScreen.DOWNLOADER,
                    AppScreen.HISTORY,
                    AppScreen.SUBSCRIPTION,
                    AppScreen.ADMIN,
                    AppScreen.ACCOUNT
                )

                screensToShow.forEach { screen ->
                    val isSelected = currentScreen == screen
                    val iconColor = when {
                        isSelected && screen == AppScreen.ADMIN -> GoldenAmber
                        isSelected && screen == AppScreen.DOWNLOADER -> MintCyan
                        isSelected && screen == AppScreen.HISTORY -> VioletPurple
                        isSelected -> CoralRed
                        else -> TextMuted
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = iconColor
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                color = if (isSelected) TextInk else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = when (screen) {
                                AppScreen.ADMIN -> GoldenAmber.copy(alpha = 0.2f)
                                AppScreen.DOWNLOADER -> MintCyan.copy(alpha = 0.2f)
                                AppScreen.HISTORY -> VioletPurple.copy(alpha = 0.2f)
                                else -> CoralRed.copy(alpha = 0.15f)
                            }
                        ),
                        modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.STUDIO -> {
                    CutStudioScreen(
                        repository = repository,
                        currentUser = currentUser,
                        onNavigateToSubscription = { currentScreen = AppScreen.SUBSCRIPTION },
                        onNavigateToDownloader = { currentScreen = AppScreen.DOWNLOADER },
                        onNavigateToHistory = { currentScreen = AppScreen.HISTORY },
                        onOpenClipDetail = { clip -> selectedClipForDetail = clip }
                    )
                }
                AppScreen.DOWNLOADER -> {
                    FreeDownloaderScreen(
                        repository = repository,
                        onNavigateToStudio = { currentScreen = AppScreen.STUDIO }
                    )
                }
                AppScreen.HISTORY -> {
                    HistoryScreen(
                        repository = repository,
                        onNavigateToStudio = { currentScreen = AppScreen.STUDIO }
                    )
                }
                AppScreen.SUBSCRIPTION -> {
                    SubscriptionScreen(
                        repository = repository,
                        currentUser = currentUser,
                        onNavigateToStudio = { currentScreen = AppScreen.STUDIO },
                        onNavigateToAuth = { currentScreen = AppScreen.ACCOUNT }
                    )
                }
                AppScreen.ADMIN -> {
                    AdminSpaceScreen(
                        repository = repository,
                        currentUser = currentUser,
                        onNavigateToStudio = { currentScreen = AppScreen.STUDIO },
                        onClipClick = { selectedClipForDetail = it }
                    )
                }
                AppScreen.ACCOUNT -> {
                    AuthScreen(
                        repository = repository,
                        currentUser = currentUser,
                        onLoginSuccess = { currentScreen = AppScreen.STUDIO },
                        onNavigateToAdmin = { currentScreen = AppScreen.ADMIN }
                    )
                }
            }

            // Clip Detail Studio Modal
            if (selectedClipForDetail != null) {
                ClipDetailModal(
                    clip = selectedClipForDetail!!,
                    repository = repository,
                    onDismiss = { selectedClipForDetail = null }
                )
            }
        }
    }
}
