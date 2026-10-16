package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.data.SMCutRepository
import com.example.model.DownloadedVideoItem
import com.example.ui.components.GradientButton
import com.example.ui.components.SectionCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FreeDownloaderScreen(
    repository: SMCutRepository,
    onNavigateToStudio: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val downloads by repository.downloadedVideos.collectAsState()
    val supportedSites = repository.supportedDownloadSites

    var inputUrl by remember { mutableStateOf("https://www.tiktok.com/@creator/video/7298410293") }
    var selectedQuality by remember { mutableStateOf("1080p Full HD") }
    var noWatermarkChecked by remember { mutableStateOf(true) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadStatusText by remember { mutableStateOf("") }
    var lastDownloadedItem by remember { mutableStateOf<DownloadedVideoItem?>(null) }

    val qualityOptions = listOf(
        "1080p Full HD" to "Le plus populaire",
        "720p HD" to "Rapide & Léger",
        "4K Ultra HD" to "Qualité Maximale",
        "480p SD" to "Économie données",
        "MP3 Audio (HQ)" to "Audio / Musique seule"
    )

    val sampleLinks = listOf(
        "TikTok" to "https://www.tiktok.com/@trend/video/73821093",
        "YouTube" to "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "Instagram" to "https://www.instagram.com/reel/C9xLmQ3/",
        "X / Twitter" to "https://x.com/tech/status/183920194",
        "Facebook" to "https://www.facebook.com/watch/?v=928401928"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Hero Header
        Surface(
            color = MintCyan.copy(alpha = 0.12f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MintCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MintCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadForOffline,
                                contentDescription = null,
                                tint = DarkBg,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Téléchargeur Gratuit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MintCyan
                        )
                    }

                    Surface(
                        color = MintCyan,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "100% GRATUIT",
                            color = DarkBg,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Téléchargez gratuitement n'importe quelle vidéo à partir de son lien (TikTok sans filigrane, YouTube, Instagram Reels, Facebook, X, etc.) sans aucun abonnement ni limitation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextInk,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Input Card
        SectionCard {
            Text(
                text = "Colle le lien de la vidéo",
                style = MaterialTheme.typography.titleLarge,
                color = TextInk
            )
            Text(
                text = "Compatible avec tous les sites et réseaux sociaux.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            // URL input with Paste & Clear buttons
            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                placeholder = { Text("https://www.tiktok.com/@... ou lien web") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = MintCyan)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (inputUrl.isNotEmpty()) {
                            IconButton(onClick = { inputUrl = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Effacer", tint = TextMuted)
                            }
                        }
                        IconButton(onClick = {
                            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipMgr.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    inputUrl = text
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
                    .testTag("free_download_url_input"),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Sample links buttons for quick testing
            Text(text = "Exemples rapides :", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sampleLinks.take(4).forEach { (siteLabel, sampleUrl) ->
                    Surface(
                        color = PanelElevated,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { inputUrl = sampleUrl }
                    ) {
                        Text(
                            text = siteLabel,
                            modifier = Modifier.padding(vertical = 5.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = TextInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detected Platform Badge
            val detectedPlatform = remember(inputUrl) { repository.detectPlatformName(inputUrl) }
            Surface(
                color = PanelElevated,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = MintCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Plateforme détectée : $detectedPlatform",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextInk
                        )
                    }

                    Surface(
                        color = MintCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Flux vérifié",
                            color = MintCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quality Selection
            Text(
                text = "Format & Résolution de téléchargement :",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextInk
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                qualityOptions.forEach { (quality, desc) ->
                    val isSel = selectedQuality == quality
                    Surface(
                        color = if (isSel) MintCyan.copy(alpha = 0.18f) else PanelElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSel) MintCyan else BorderLine
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedQuality = quality }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSel,
                                    onClick = { selectedQuality = quality },
                                    colors = RadioButtonDefaults.colors(selectedColor = MintCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = quality,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) MintCyan else TextInk
                                    )
                                    Text(text = desc, style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                                }
                            }

                            Surface(
                                color = if (quality.contains("MP3")) VioletPurple.copy(0.2f) else CoralRed.copy(0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (quality.contains("MP3")) "AUDIO" else "MP4",
                                    color = TextInk,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // No Watermark Switch (for TikTok / Reels)
            Surface(
                color = PanelElevated,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.HighQuality, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Supprimer le filigrane (Watermark)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextInk
                            )
                            Text(
                                text = "Téléchargement propre sans logo TikTok / Instagram",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = noWatermarkChecked,
                        onCheckedChange = { noWatermarkChecked = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldenAmber,
                            checkedTrackColor = GoldenAmber.copy(0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Big Download CTA Button (Completely Free)
            GradientButton(
                text = if (isDownloading) "Téléchargement en cours ($downloadProgress%)..." else "Télécharger gratuitement ($selectedQuality)",
                icon = if (isDownloading) Icons.Default.HourglassTop else Icons.Default.Download,
                enabled = !isDownloading,
                colors = listOf(MintCyan, Color(0xFF0B6FA4)),
                onClick = {
                    if (inputUrl.isBlank() || !inputUrl.startsWith("http")) {
                        Toast.makeText(context, "Veuillez coller un lien vidéo valide (http...)", Toast.LENGTH_SHORT).show()
                        return@GradientButton
                    }

                    isDownloading = true
                    downloadProgress = 5
                    coroutineScope.launch {
                        val downloaded = repository.executeFreeVideoDownload(
                            url = inputUrl,
                            quality = selectedQuality,
                            noWatermark = noWatermarkChecked
                        ) { p, msg ->
                            downloadProgress = p
                            downloadStatusText = msg
                        }
                        isDownloading = false
                        lastDownloadedItem = downloaded
                        Toast.makeText(context, "Vidéo téléchargée avec succès dans Téléchargements / Galerie !", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("execute_free_download_button")
            )

            // Progress Feedback
            if (isDownloading) {
                Spacer(modifier = Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MintCyan,
                    trackColor = BorderLine
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = downloadStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MintCyan
                    )
                    Text(
                        text = "$downloadProgress%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MintCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Supported Websites Carousel Chips
        Text(
            text = "Sites & Réseaux Sociaux Compatibles :",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextInk
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            supportedSites.take(3).forEach { site ->
                Surface(
                    color = PanelElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = site,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextInk,
                        fontSize = 10.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            supportedSites.drop(3).take(3).forEach { site ->
                Surface(
                    color = PanelElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = site,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextInk,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Download History Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vidéos téléchargées (${downloads.size})",
                style = MaterialTheme.typography.headlineMedium,
                color = TextInk
            )
            Surface(
                color = MintCyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Stockage local",
                    color = MintCyan,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (downloads.isEmpty()) {
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
                    Icon(imageVector = Icons.Default.FileDownloadOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Aucun téléchargement pour le moment", style = MaterialTheme.typography.titleMedium, color = TextInk)
                    Text("Collez un lien ci-dessus pour télécharger votre première vidéo gratuitement.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        } else {
            downloads.forEachIndexed { index, item ->
                Surface(
                    color = PanelBg,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail box
                        Box(
                            modifier = Modifier
                                .size(72.dp, 84.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.verticalGradient(GradientMintBlue)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                            ) {
                                Surface(
                                    color = Color.Black.copy(0.7f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.sourcePlatform,
                                        color = MintCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                Icon(
                                    imageVector = if (item.formatQuality.contains("MP3")) Icons.Default.Audiotrack else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )

                                Surface(
                                    color = Color.Black.copy(0.7f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.durationText,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MintCyan.copy(0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.formatQuality,
                                        color = MintCyan,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (item.noWatermark) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = GoldenAmber.copy(0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Sans filigrane",
                                            color = GoldenAmber,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextInk,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${item.fileSizeMb} Mo • ${item.downloadDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = PanelElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                                    modifier = Modifier.clickable {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Regarde cette vidéo : ${item.sourceUrl}")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Partager le fichier"))
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MintCyan, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Partager", color = TextInk, fontSize = 11.sp)
                                    }
                                }

                                Surface(
                                    color = PanelElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                                    modifier = Modifier.clickable {
                                        repository.deleteDownloadedVideo(item.id)
                                        Toast.makeText(context, "Vidéo supprimée de l'historique", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = CoralRed, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Supprimer", color = CoralRed, fontSize = 11.sp)
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
