package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import com.example.model.DownloadedClipRecord
import com.example.model.SocialPlatform
import com.example.ui.components.GradientButton
import com.example.ui.theme.*

@Composable
fun HistoryScreen(
    repository: SMCutRepository,
    onNavigateToStudio: () -> Unit
) {
    val context = LocalContext.current
    val clipHistory by repository.clipHistory.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatformFilter by remember { mutableStateOf("Tous") }
    var selectedQualityFilter by remember { mutableStateOf("Tous") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Quick Reshare Modal state
    var selectedClipForShare by remember { mutableStateOf<DownloadedClipRecord?>(null) }
    var activeSharePlatform by remember { mutableStateOf(SocialPlatform.TIKTOK) }

    val platformOptions = listOf("Tous", "YouTube", "TikTok", "Instagram Reels", "YouTube Shorts")
    val qualityOptions = listOf("Tous", "1080p", "4K", "720p")

    val filteredList = remember(clipHistory, searchQuery, selectedPlatformFilter, selectedQualityFilter) {
        clipHistory.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.subtitleExcerpt.contains(searchQuery, ignoreCase = true)
            val matchesPlatform = selectedPlatformFilter == "Tous" || item.sourcePlatform.contains(selectedPlatformFilter, ignoreCase = true)
            val matchesQuality = selectedQualityFilter == "Tous" || item.formatQuality.contains(selectedQualityFilter, ignoreCase = true)
            matchesQuery && matchesPlatform && matchesQuality
        }
    }

    val totalSizeMb = remember(clipHistory) {
        Math.round(clipHistory.sumOf { it.fileSizeMb } * 10.0) / 10.0
    }
    val bestViralScore = remember(clipHistory) {
        clipHistory.maxOfOrNull { it.viralScore } ?: 0
    }

    val gradients = listOf(
        GradientCoralViolet,
        GradientMintBlue,
        GradientAmberCoral,
        GradientVioletBlue,
        GradientMintLime
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Sticky Header & Stats
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Historique des Clips",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextInk
                    )
                    Text(
                        text = "Tous vos clips téléchargés prêts à repartager",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                if (clipHistory.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier
                            .background(PanelElevated, RoundedCornerShape(10.dp))
                            .border(1.dp, BorderLine, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Effacer l'historique",
                            tint = CoralRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stat 1: Total Clips
                Surface(
                    color = PanelElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Clips sauvés",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${clipHistory.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = CoralRed
                        )
                    }
                }

                // Stat 2: Total Storage
                Surface(
                    color = PanelElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Stockage MP4",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${totalSizeMb} Mo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MintCyan
                        )
                    }
                }

                // Stat 3: Best Viral Score
                Surface(
                    color = PanelElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Score viral max",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (bestViralScore > 0) "${bestViralScore}%" else "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldenAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher dans l'historique...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Effacer", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MintCyan,
                    unfocusedBorderColor = BorderLine,
                    focusedContainerColor = PanelBg,
                    unfocusedContainerColor = PanelBg,
                    focusedTextColor = TextInk,
                    unfocusedTextColor = TextInk
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("history_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips (Platforms)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                platformOptions.forEach { plat ->
                    val isSel = selectedPlatformFilter == plat
                    Surface(
                        color = if (isSel) CoralRed.copy(alpha = 0.2f) else PanelElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CoralRed else BorderLine),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPlatformFilter = plat }
                    ) {
                        Text(
                            text = plat,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) CoralRed else TextInk,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // List of clips or Empty State
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HistoryEdu,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedPlatformFilter != "Tous") "Aucun clip trouvé pour ce filtre" else "Aucun clip téléchargé",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextInk,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Essayez un autre mot-clé ou réinitialisez les filtres." else "Téléchargez des clips depuis le Studio ou le Téléchargeur gratuit pour les retrouver ici et les repartager en 1 clic.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    GradientButton(
                        text = "Aller au Studio de découpe",
                        icon = Icons.Default.ContentCut,
                        onClick = onNavigateToStudio,
                        modifier = Modifier.widthIn(max = 280.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredList, key = { it.id }) { record ->
                    HistoryClipCard(
                        record = record,
                        gradientColors = gradients[record.gradientIndex % gradients.size],
                        onQuickShare = { platform ->
                            selectedClipForShare = record
                            activeSharePlatform = platform
                        },
                        onDelete = {
                            repository.deleteFromClipHistory(record.id)
                            Toast.makeText(context, "Clip retiré de l'historique", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Modal Confirmation Clear All
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text("Effacer tout l'historique ?", color = TextInk, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Voulez-vous supprimer tous les clips enregistrés dans l'historique ? Les fichiers physiques déjà sauvegardés dans votre galerie ne seront pas supprimés.",
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.clearClipHistory()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Historique vidé", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Oui, effacer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Annuler", color = TextMuted)
                }
            },
            containerColor = PanelBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Quick Reshare Bottom Sheet / Dialog
    if (selectedClipForShare != null) {
        val clip = selectedClipForShare!!
        val postText = remember(clip, activeSharePlatform) {
            repository.generateSocialTextForHistory(clip, activeSharePlatform)
        }

        AlertDialog(
            onDismissRequest = { selectedClipForShare = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MintCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Repartager sur ${activeSharePlatform.displayName}", color = TextInk, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { selectedClipForShare = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = TextMuted)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Clip : ${clip.title}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CoralRed
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Social platform switcher chips inside dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SocialPlatform.values().forEach { plat ->
                            val isSel = activeSharePlatform == plat
                            Surface(
                                color = if (isSel) MintCyan.copy(alpha = 0.2f) else PanelElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) MintCyan else BorderLine),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeSharePlatform = plat }
                            ) {
                                Text(
                                    text = plat.displayName.split(" ").first(),
                                    modifier = Modifier.padding(vertical = 5.dp),
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

                    // Post content container
                    Surface(
                        color = PanelElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                text = postText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextInk,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, postText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Repartager ${clip.title}")
                        context.startActivity(shareIntent)
                        selectedClipForShare = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintCyan)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = DarkBg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partager l'application", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("SMCut Quick Post", postText)
                        clipMgr.setPrimaryClip(clipData)
                        Toast.makeText(context, "Texte et hashtags copiés dans le presse-papier !", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = TextInk, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copier", color = TextInk)
                }
            },
            containerColor = PanelBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun HistoryClipCard(
    record: DownloadedClipRecord,
    gradientColors: List<Color>,
    onQuickShare: (SocialPlatform) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        color = PanelBg,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_card_${record.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Visual Banner Preview with metadata overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(Brush.linearGradient(gradientColors))
                    .padding(10.dp)
            ) {
                // Top badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = record.formattedTimestamp, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        color = CoralRed,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${record.viralScore}% VIRAL",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Bottom tags in visual banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "🎬 ${record.formatQuality} • ${record.fileSizeMb} Mo",
                            color = MintCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = record.sourcePlatform,
                            color = GoldenAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Card Body
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = record.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextInk,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = record.subtitleExcerpt,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            maxLines = 2
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Supprimer", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 Téléchargé le ${record.downloadDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Surface(
                        color = MintCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Prêt à republier",
                            color = MintCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderLine, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // QUICK RESHARE BAR (1-Click actions)
                Text(
                    text = "⚡ Repartager en 1 clic :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextInk
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickPlatforms = listOf(
                        SocialPlatform.TIKTOK to "TikTok",
                        SocialPlatform.INSTAGRAM to "Reels",
                        SocialPlatform.YOUTUBE_SHORTS to "Shorts",
                        SocialPlatform.X_TWITTER to "X"
                    )

                    quickPlatforms.forEach { (platform, label) ->
                        Button(
                            onClick = { onQuickShare(platform) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (platform) {
                                    SocialPlatform.TIKTOK -> Color(0xFF1E1E2E)
                                    SocialPlatform.INSTAGRAM -> Color(0xFF381A2D)
                                    SocialPlatform.YOUTUBE_SHORTS -> Color(0xFF321A1A)
                                    else -> Color(0xFF15222E)
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (platform) {
                                    SocialPlatform.TIKTOK -> MintCyan.copy(alpha = 0.5f)
                                    SocialPlatform.INSTAGRAM -> CoralRed.copy(alpha = 0.5f)
                                    SocialPlatform.YOUTUBE_SHORTS -> Color(0xFFFF4444).copy(alpha = 0.5f)
                                    else -> Color(0xFF1DA1F2).copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (platform) {
                                    SocialPlatform.TIKTOK -> MintCyan
                                    SocialPlatform.INSTAGRAM -> CoralRed
                                    SocialPlatform.YOUTUBE_SHORTS -> Color(0xFFFF6666)
                                    else -> Color(0xFF70C4F7)
                                }
                            )
                        }
                    }

                    // Direct Share Intent Icon Button
                    IconButton(
                        onClick = { onQuickShare(SocialPlatform.TIKTOK) },
                        modifier = Modifier
                            .size(38.dp)
                            .background(PanelElevated, RoundedCornerShape(10.dp))
                            .border(1.dp, BorderLine, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Partager",
                            tint = TextInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
