package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import com.example.data.SMCutRepository
import com.example.model.*
import com.example.notification.FCMManager
import com.example.ui.components.GradientButton
import com.example.ui.components.SectionCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class DurationFilter(val label: String) {
    ALL("Toutes"),
    UNDER_1_MIN("< 1 min"),
    FROM_1_TO_2_MIN("1 - 2 min"),
    FROM_2_TO_5_MIN("2 - 5 min"),
    OVER_5_MIN("> 5 min")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CutStudioScreen(
    repository: SMCutRepository,
    currentUser: UserAccount?,
    onNavigateToSubscription: () -> Unit,
    onNavigateToDownloader: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onOpenClipDetail: (VideoClip) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentSettings by repository.currentSettings.collectAsState()
    val clips by repository.clips.collectAsState()
    val isGenerating by repository.isGenerating.collectAsState()
    val progress by repository.generationProgress.collectAsState()
    val statusMsg by repository.generationStatusMessage.collectAsState()

    var videoUrl by remember { mutableStateOf(currentSettings.url) }
    var selectedFormat by remember { mutableStateOf(currentSettings.format) }
    var selectedDurationSec by remember { mutableStateOf(currentSettings.clipDurationSeconds) }
    var selectedQuality by remember { mutableStateOf(currentSettings.quality) }
    var subtitleLang by remember { mutableStateOf(currentSettings.subtitleLanguage) }
    var targetTranslateLang by remember { mutableStateOf(currentSettings.targetTranslationLanguage) }
    var isTranslationEnabled by remember { mutableStateOf(currentSettings.isTranslated) }

    val languages = listOf("Français", "English", "Español", "العربية", "Português", "Deutsch", "Italiano")
    val durationOptions = listOf(
        30 to "30 sec",
        60 to "1 min",
        120 to "2 min",
        180 to "3 min",
        300 to "5 min",
        600 to "10 min",
        900 to "15 min"
    )

    val gradients = listOf(
        GradientCoralViolet,
        GradientMintBlue,
        GradientAmberCoral,
        GradientVioletBlue,
        GradientMintLime
    )

    // Advanced Search & Filter States
    var searchQuery by remember { mutableStateOf("") }
    var selectedDurationFilter by remember { mutableStateOf(DurationFilter.ALL) }
    var selectedQualityFilter by remember { mutableStateOf<VideoQuality?>(null) }
    var selectedLanguageFilter by remember { mutableStateOf<String?>(null) }
    var isFilterPanelExpanded by remember { mutableStateOf(false) }

    val filteredClips = remember(clips, searchQuery, selectedDurationFilter, selectedQualityFilter, selectedLanguageFilter) {
        clips.filter { clip ->
            val matchesQuery = searchQuery.isBlank() ||
                    clip.title.contains(searchQuery, ignoreCase = true) ||
                    clip.summary.contains(searchQuery, ignoreCase = true)

            val matchesDuration = when (selectedDurationFilter) {
                DurationFilter.ALL -> true
                DurationFilter.UNDER_1_MIN -> clip.durationSeconds < 60
                DurationFilter.FROM_1_TO_2_MIN -> clip.durationSeconds in 60..120
                DurationFilter.FROM_2_TO_5_MIN -> clip.durationSeconds in 121..300
                DurationFilter.OVER_5_MIN -> clip.durationSeconds > 300
            }

            val matchesQuality = selectedQualityFilter == null || clip.quality == selectedQualityFilter

            val matchesLanguage = selectedLanguageFilter == null ||
                    clip.subtitleLanguage.equals(selectedLanguageFilter, ignoreCase = true) ||
                    clip.title.contains(selectedLanguageFilter!!, ignoreCase = true)

            matchesQuery && matchesDuration && matchesQuality && matchesLanguage
        }
    }

    val activeFiltersCount = (if (searchQuery.isNotBlank()) 1 else 0) +
            (if (selectedDurationFilter != DurationFilter.ALL) 1 else 0) +
            (if (selectedQualityFilter != null) 1 else 0) +
            (if (selectedLanguageFilter != null) 1 else 0)

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Hero Input Card
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ta vidéo source",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextInk
                )
                Surface(
                    color = MintCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintCyan)
                ) {
                    Text(
                        text = "10 clips générés",
                        color = MintCyan,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Quick Banners (Free Downloader + History)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MintCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintCyan),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToDownloader() }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.DownloadForOffline, contentDescription = null, tint = MintCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Téléchargeur Gratuit",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MintCyan
                            )
                            Text(
                                text = "Tout lien sans filigrane",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextInk,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Surface(
                    color = VioletPurple.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VioletPurple),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToHistory() }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = VioletPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Historique Clips",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = VioletPurple
                            )
                            Text(
                                text = "Repartager en 1 clic",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextInk,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // URL input with Paste & Clear buttons
            OutlinedTextField(
                value = videoUrl,
                onValueChange = { videoUrl = it },
                label = { Text("Lien de la vidéo (YouTube, TikTok, Reels, etc.)") },
                placeholder = { Text("https://www.youtube.com/watch?v=...") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = CoralRed)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (videoUrl.isNotEmpty()) {
                            IconButton(onClick = { videoUrl = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Effacer", tint = TextMuted)
                            }
                        }
                        IconButton(onClick = {
                            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipMgr.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    videoUrl = text
                                    Toast.makeText(context, "Lien collé depuis le presse-papiers !", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Coller", tint = MintCyan)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_url_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MintCyan,
                    unfocusedBorderColor = BorderLine,
                    focusedTextColor = TextInk,
                    unfocusedTextColor = TextInk
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Detected video thumbnail preview card
            Surface(
                color = PanelElevated,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp, 44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(GradientCoralViolet)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (videoUrl.contains("tiktok")) "Vidéo TikTok Détectée" else "Vidéo Longue Détectée",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextInk
                        )
                        Text(
                            text = "Durée totale estimée : 18 min 42 s • Stéréo HQ",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Format selector
            Text(
                text = "1. Format vidéo (Ratio)",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoFormat.values().forEach { fmt ->
                    val isSelected = selectedFormat == fmt
                    Surface(
                        color = if (isSelected) CoralRed.copy(alpha = 0.2f) else PanelElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CoralRed else BorderLine
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedFormat = fmt }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = fmt.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CoralRed else TextInk
                            )
                            Text(
                                text = fmt.ratioLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Durée de chaque clip (30s à 15 min)
            Text(
                text = "2. Durée de chaque clip découpé",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                durationOptions.take(4).forEach { (seconds, label) ->
                    val isSelected = selectedDurationSec == seconds
                    Surface(
                        color = if (isSelected) MintCyan.copy(alpha = 0.2f) else PanelElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MintCyan else BorderLine
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedDurationSec = seconds }
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MintCyan else TextInk
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                durationOptions.drop(4).forEach { (seconds, label) ->
                    val isSelected = selectedDurationSec == seconds
                    Surface(
                        color = if (isSelected) MintCyan.copy(alpha = 0.2f) else PanelElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MintCyan else BorderLine
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedDurationSec = seconds }
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MintCyan else TextInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Qualité vidéo (480p à 4K)
            Text(
                text = "3. Qualité de rendu (480p à 4K)",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VideoQuality.values().forEach { q ->
                    val isSelected = selectedQuality == q
                    Surface(
                        color = if (isSelected) VioletPurple.copy(alpha = 0.25f) else PanelElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) VioletPurple else BorderLine
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedQuality = q }
                    ) {
                        Text(
                            text = q.label,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Langue des sous-titres
            Text(
                text = "4. Langue des sous-titres animés",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                languages.take(4).forEach { lang ->
                    val isSelected = subtitleLang == lang
                    Surface(
                        color = if (isSelected) MintCyan.copy(alpha = 0.2f) else PanelElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MintCyan else BorderLine
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { subtitleLang = lang }
                    ) {
                        Text(
                            text = lang,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MintCyan else TextInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Traduction vidéo (Option demandée dans le brief)
            Surface(
                color = PanelElevated,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isTranslationEnabled) GoldenAmber else BorderLine
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = if (isTranslationEnabled) GoldenAmber else TextMuted
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Traduire la vidéo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextInk
                            )
                        }
                        Switch(
                            checked = isTranslationEnabled,
                            onCheckedChange = { isTranslationEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GoldenAmber,
                                checkedTrackColor = GoldenAmber.copy(0.3f)
                            )
                        )
                    }

                    if (isTranslationEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Traduire l'audio et les sous-titres en :",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            languages.filter { it != subtitleLang }.take(4).forEach { targetLang ->
                                val isSel = targetTranslateLang == targetLang
                                Surface(
                                    color = if (isSel) GoldenAmber.copy(0.2f) else DarkBg,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) GoldenAmber else BorderLine
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { targetTranslateLang = targetLang }
                                ) {
                                    Text(
                                        text = targetLang,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSel) GoldenAmber else TextInk
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bouton Générer
            GradientButton(
                text = if (isGenerating) "Génération en cours ($progress%)..." else "Générer les 10 clips",
                icon = if (isGenerating) Icons.Default.HourglassTop else Icons.Default.AutoAwesome,
                enabled = !isGenerating,
                onClick = {
                    if (videoUrl.isBlank() || !videoUrl.startsWith("http")) {
                        Toast.makeText(context, "Veuillez coller un lien valide (http...)", Toast.LENGTH_SHORT).show()
                        return@GradientButton
                    }
                    if (currentUser == null) {
                        Toast.makeText(context, "Veuillez vous connecter pour générer vos clips", Toast.LENGTH_SHORT).show()
                        return@GradientButton
                    }
                    if (!currentUser.isSubscribed && !currentUser.isAdmin) {
                        Toast.makeText(context, "Abonnez-vous (5$/mois ou 50$/an) pour générer des clips illimités.", Toast.LENGTH_LONG).show()
                        onNavigateToSubscription()
                        return@GradientButton
                    }

                    val updatedSettings = VideoSettings(
                        url = videoUrl,
                        format = selectedFormat,
                        clipDurationSeconds = selectedDurationSec,
                        quality = selectedQuality,
                        subtitleLanguage = subtitleLang,
                        targetTranslationLanguage = targetTranslateLang,
                        isTranslated = isTranslationEnabled
                    )

                    coroutineScope.launch {
                        repository.generateClips(updatedSettings)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_clips_button")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // FCM Push Notification Option Card
            val isPushEnabled by FCMManager.isPushEnabled.collectAsState()
            Surface(
                color = DarkBg.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (isPushEnabled) MintCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Alerte Push Firebase (FCM)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextInk
                            )
                            Text(
                                text = if (isPushEnabled) "Alerte sonore dès la fin de la découpe" else "Notifications désactivées",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPushEnabled) MintCyan else TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = CoralRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                FCMManager.notifyVideoProcessingFinished(
                                    context = context,
                                    videoTitle = if (videoUrl.isNotBlank()) videoUrl else "Démo YouTube Shorts",
                                    clipCount = 10,
                                    quality = selectedQuality.label
                                )
                                Toast.makeText(context, "Notification test envoyée !", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "Tester",
                                color = CoralRed,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = isPushEnabled,
                            onCheckedChange = { FCMManager.setPushEnabled(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MintCyan,
                                uncheckedTrackColor = BorderLine
                            ),
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                }
            }

            // Progress bar and feedback
            if (isGenerating || progress > 0 && progress < 100) {
                Spacer(modifier = Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MintCyan,
                    trackColor = BorderLine
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MintCyan
                )

                Surface(
                    color = MintCyan.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintCyan.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsNone, contentDescription = null, tint = MintCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Traitement en cours : vous recevrez une alerte push FCM dès que les 10 clips sont prêts !",
                            style = MaterialTheme.typography.bodySmall,
                            color = MintCyan,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Spacer(modifier = Modifier.height(24.dp))

        // Clips Section & Advanced Search Bar
        if (clips.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vos clips découpés",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextInk
                    )
                    Text(
                        text = "${filteredClips.size} sur ${clips.size} clips correspondent",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (filteredClips.isEmpty()) CoralRed else MintCyan
                    )
                }

                Surface(
                    color = if (activeFiltersCount > 0) MintCyan.copy(alpha = 0.2f) else PanelElevated,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (activeFiltersCount > 0) MintCyan else BorderLine
                    ),
                    modifier = Modifier.clickable { isFilterPanelExpanded = !isFilterPanelExpanded }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filtres",
                            tint = if (activeFiltersCount > 0) MintCyan else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeFiltersCount > 0) "Filtres ($activeFiltersCount)" else "Filtres",
                            color = if (activeFiltersCount > 0) MintCyan else TextInk,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher par mot-clé dans le titre...") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Recherche", tint = MintCyan)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Effacer", tint = TextMuted)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clip_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MintCyan,
                    unfocusedBorderColor = BorderLine,
                    focusedTextColor = TextInk,
                    unfocusedTextColor = TextInk,
                    focusedContainerColor = PanelBg,
                    unfocusedContainerColor = PanelBg
                )
            )

            // Advanced Filters Panel (Collapsible or toggleable)
            AnimatedVisibility(visible = isFilterPanelExpanded || activeFiltersCount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(PanelElevated, RoundedCornerShape(14.dp))
                        .border(1.dp, BorderLine, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtres avancés",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextInk
                        )

                        if (activeFiltersCount > 0) {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedDurationFilter = DurationFilter.ALL
                                    selectedQualityFilter = null
                                    selectedLanguageFilter = null
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = CoralRed, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Réinitialiser", color = CoralRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Filtre par Plage de durée
                    Text(
                        text = "Plage de durée :",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DurationFilter.values().forEach { dFilter ->
                            val isSel = selectedDurationFilter == dFilter
                            Surface(
                                color = if (isSel) MintCyan.copy(alpha = 0.25f) else DarkBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) MintCyan else BorderLine
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDurationFilter = dFilter }
                            ) {
                                Text(
                                    text = dFilter.label,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) MintCyan else TextInk,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Filtre par Résolution / Qualité
                    Text(
                        text = "Résolution vidéo :",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // "Toutes" option
                        val isAllQualities = selectedQualityFilter == null
                        Surface(
                            color = if (isAllQualities) VioletPurple.copy(alpha = 0.25f) else DarkBg,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAllQualities) VioletPurple else BorderLine
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedQualityFilter = null }
                        ) {
                            Text(
                                text = "Toutes",
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isAllQualities) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAllQualities) Color.White else TextInk,
                                fontSize = 10.sp
                            )
                        }

                        VideoQuality.values().forEach { q ->
                            val isSel = selectedQualityFilter == q
                            Surface(
                                color = if (isSel) VioletPurple.copy(alpha = 0.25f) else DarkBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) VioletPurple else BorderLine
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedQualityFilter = q }
                            ) {
                                Text(
                                    text = q.label,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else TextInk,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Filtre par Langue des sous-titres
                    Text(
                        text = "Langue des sous-titres :",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val filterLanguages = listOf(null to "Toutes") + languages.take(5).map { it to it }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filterLanguages.forEach { (langVal, label) ->
                            val isSel = selectedLanguageFilter == langVal
                            Surface(
                                color = if (isSel) CoralRed.copy(alpha = 0.25f) else DarkBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) CoralRed else BorderLine
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedLanguageFilter = langVal }
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) CoralRed else TextInk,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Empty state if no clips match filter
            if (filteredClips.isEmpty()) {
                Surface(
                    color = PanelBg,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = CoralRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aucun clip trouvé",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextInk
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aucun clip ne correspond à vos filtres de recherche actuels. Essayez avec un autre mot-clé ou réinitialisez les critères.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                searchQuery = ""
                                selectedDurationFilter = DurationFilter.ALL
                                selectedQualityFilter = null
                                selectedLanguageFilter = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PanelElevated),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = MintCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Réinitialiser les filtres", color = TextInk)
                        }
                    }
                }
            } else {
                // Clips list (Filtered)
                filteredClips.forEachIndexed { index, clip ->
                    val grad = gradients[clip.gradientIndex % gradients.size]
                    Surface(
                        color = PanelBg,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { onOpenClipDetail(clip) }
                            .testTag("clip_item_$index")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail gradient with clip time & play icon
                            Box(
                                modifier = Modifier
                                    .size(88.dp, 108.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.verticalGradient(grad)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                ) {
                                    // Viral score badge
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "🔥 ${clip.viralScore}%",
                                            color = MintCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.PlayCircleFilled,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )

                                    // Duration pill
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = clip.formattedTimestamp,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = BorderLine,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Clip #${index + 1}",
                                            color = TextMuted,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${clip.durationSeconds}s",
                                        color = MintCyan,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = VioletPurple.copy(0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = clip.quality.label,
                                            color = VioletPurple,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = clip.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextInk,
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = clip.subtitleExcerpt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = PanelElevated,
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = CoralRed, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Télécharger", color = TextInk, fontSize = 11.sp)
                                        }
                                    }

                                    Surface(
                                        color = PanelElevated,
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MintCyan, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Réseaux Sociaux", color = TextInk, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
