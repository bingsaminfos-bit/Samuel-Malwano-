package com.example.ui.screens

import android.content.ClipData
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
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.SMCutRepository
import com.example.model.SocialPlatform
import com.example.model.VideoClip
import com.example.model.VideoQuality
import com.example.ui.components.GradientButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ClipDetailModal(
    clip: VideoClip,
    repository: SMCutRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentSettings by repository.currentSettings.collectAsState()

    var selectedPlatform by remember { mutableStateOf(SocialPlatform.TIKTOK) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackPositionSec by remember { mutableStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var hasCopied by remember { mutableStateOf(false) }

    val clipTotalDuration = clip.durationSeconds.toFloat()

    // Simulated playback loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(100)
            playbackPositionSec = (playbackPositionSec + 0.1f) % clipTotalDuration
        }
    }

    var selectedTemplate by remember { mutableStateOf<com.example.model.DescriptionTemplate?>(null) }
    val templates = repository.defaultTemplates

    // Generated social content
    val socialContent = remember(selectedPlatform, clip, selectedTemplate) {
        if (selectedTemplate != null) {
            repository.applyTemplate(clip, selectedTemplate!!, selectedPlatform)
        } else {
            repository.generateSocialMediaPost(clip, selectedPlatform)
        }
    }

    val gradients = listOf(
        GradientCoralViolet,
        GradientMintBlue,
        GradientAmberCoral,
        GradientVioletBlue,
        GradientMintLime
    )
    val grad = gradients[clip.gradientIndex % gradients.size]

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp),
            color = DarkBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = clip.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextInk,
                            maxLines = 1
                        )
                        Text(
                            text = "Extrait : ${clip.formattedTimestamp} • Score Viral : ${clip.viralScore}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MintCyan
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(PanelElevated, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = TextInk)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Simulated Video Player Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(grad))
                        .border(1.dp, BorderLine, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Playback controls overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.Black.copy(0.7f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Qualité : ${currentSettings.quality.label}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                color = CoralRed,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "EN LECTURE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Centered animated subtitles simulation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = Color.Black.copy(0.75f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = clip.subtitleExcerpt,
                                    color = GoldenAmber,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Bottom playback controller bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { isPlaying = !isPlaying },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.Black.copy(0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = "${String.format("%.1f", playbackPositionSec)}s / ${clip.durationSeconds}s",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = playbackPositionSec,
                                onValueChange = {
                                    playbackPositionSec = it
                                },
                                valueRange = 0f..clipTotalDuration,
                                colors = SliderDefaults.colors(
                                    thumbColor = MintCyan,
                                    activeTrackColor = MintCyan,
                                    inactiveTrackColor = Color.White.copy(0.4f)
                                ),
                                modifier = Modifier.height(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bouton Télécharger
                if (isDownloading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PanelElevated, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Exportation MP4 ${currentSettings.quality.label}...", color = TextInk, fontWeight = FontWeight.Bold)
                            Text("$downloadProgress%", color = MintCyan, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            color = CoralRed,
                            trackColor = BorderLine,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                } else {
                    GradientButton(
                        text = "Télécharger le clip (${currentSettings.quality.label})",
                        icon = Icons.Default.Download,
                        colors = listOf(CoralRed, CoralRedDark),
                        onClick = {
                            isDownloading = true
                            downloadProgress = 10
                            // Simulate download in background coroutine
                            // After completed:
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_clip_button")
                    )
                }

                // Handle simulated download progression
                LaunchedEffect(isDownloading) {
                    if (isDownloading) {
                        for (p in 10..100 step 15) {
                            delay(250)
                            downloadProgress = p
                        }
                        isDownloading = false
                        repository.recordDownloadedClip(clip, currentSettings.quality)
                        Toast.makeText(context, "Clip MP4 enregistré dans la galerie & ajouté à l'Historique !", Toast.LENGTH_LONG).show()
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section Générateur Réseaux Sociaux
                Text(
                    text = "Générateur Réseaux Sociaux IA",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextInk
                )
                Text(
                    text = "Titres accrocheurs, descriptions virales et hashtags optimisés par plateforme.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Platform selection chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SocialPlatform.values().forEach { platform ->
                        val isSel = selectedPlatform == platform
                        Surface(
                            color = if (isSel) MintCyan.copy(0.2f) else PanelElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) MintCyan else BorderLine
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedPlatform = platform
                                    hasCopied = false
                                }
                        ) {
                            Text(
                                text = platform.displayName.split(" ").first(),
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) MintCyan else TextInk
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Predefined Description Templates
                Text(
                    text = "Modèles de descriptions prédéfinis :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextInk
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isAutoSel = selectedTemplate == null
                    Surface(
                        color = if (isAutoSel) CoralRed.copy(0.2f) else PanelElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAutoSel) CoralRed else BorderLine),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTemplate = null
                                hasCopied = false
                            }
                    ) {
                        Text(
                            text = "✨ Auto IA",
                            modifier = Modifier.padding(vertical = 7.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isAutoSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAutoSel) CoralRed else TextInk,
                            fontSize = 10.sp
                        )
                    }

                    templates.take(2).forEach { tmpl ->
                        val isSel = selectedTemplate == tmpl
                        Surface(
                            color = if (isSel) CoralRed.copy(0.2f) else PanelElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CoralRed else BorderLine),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedTemplate = tmpl
                                    hasCopied = false
                                }
                        ) {
                            Text(
                                text = when (tmpl.id) {
                                    "storytelling" -> "📖 Récit"
                                    "educatif" -> "💡 Astuce"
                                    else -> tmpl.name.split(" ").first()
                                },
                                modifier = Modifier.padding(vertical = 7.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) CoralRed else TextInk,
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
                    templates.drop(2).forEach { tmpl ->
                        val isSel = selectedTemplate == tmpl
                        Surface(
                            color = if (isSel) CoralRed.copy(0.2f) else PanelElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CoralRed else BorderLine),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedTemplate = tmpl
                                    hasCopied = false
                                }
                        ) {
                            Text(
                                text = when (tmpl.id) {
                                    "debat" -> "💬 Débat"
                                    "motivation" -> "⚡ Motivation"
                                    "humour" -> "😂 Humour"
                                    else -> tmpl.name.split(" ").first()
                                },
                                modifier = Modifier.padding(vertical = 7.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) CoralRed else TextInk,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Social Post Preview Box
                Surface(
                    color = PanelElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Post prêt pour ${selectedPlatform.displayName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MintCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (selectedTemplate != null) "Modèle : ${selectedTemplate!!.name}" else "Génération Automatique IA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, socialContent)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Partager le clip")
                                context.startActivity(shareIntent)
                            }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Partager", tint = TextInk, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        SelectionContainer {
                            Text(
                                text = socialContent,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextInk,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Copier le texte
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = ClipData.newPlainText("SMCut Post", socialContent)
                            clipMgr.setPrimaryClip(clipData)
                            hasCopied = true
                            Toast.makeText(context, "Titre, description et hashtags copiés !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasCopied) MintCyan else CoralRed
                        )
                    ) {
                        Icon(
                            imageVector = if (hasCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = if (hasCopied) DarkBg else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasCopied) "Copié !" else "Copier le texte",
                            color = if (hasCopied) DarkBg else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.7f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextInk),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                    ) {
                        Text("Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
