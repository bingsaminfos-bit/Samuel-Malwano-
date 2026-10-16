package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Brush
import com.example.data.SMCutRepository
import com.example.model.*
import com.example.notification.FCMManager
import com.example.ui.components.GradientButton
import com.example.ui.components.SectionCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSpaceScreen(
    repository: SMCutRepository,
    currentUser: UserAccount?,
    onNavigateToStudio: () -> Unit,
    onClipClick: (VideoClip) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val adminSettings by repository.adminSettings.collectAsState()
    val tickets by repository.supportTickets.collectAsState()
    val payouts by repository.payoutRecords.collectAsState()

    // Video Cutter State (Admin VIP Gratuit)
    val isGenerating by repository.isGenerating.collectAsState()
    val progress by repository.generationProgress.collectAsState()
    val statusMsg by repository.generationStatusMessage.collectAsState()
    val clips by repository.clips.collectAsState()

    var adminVideoUrl by remember { mutableStateOf("https://www.youtube.com/watch?v=dQw4w9WgXcQ") }
    var adminSelectedFormat by remember { mutableStateOf(VideoFormat.VERTICAL_9_16) }
    var adminSelectedDurationSec by remember { mutableStateOf(30) }
    var adminSelectedQuality by remember { mutableStateOf(VideoQuality.P1080) }
    var adminSubtitleLang by remember { mutableStateOf("Français") }
    var adminTargetTranslateLang by remember { mutableStateOf("Anglais") }
    var adminIsTranslationEnabled by remember { mutableStateOf(false) }
    var adminAutoHookDetection by remember { mutableStateOf(true) }
    var adminSmartFaceTracking by remember { mutableStateOf(true) }
    var adminClipsSearchQuery by remember { mutableStateOf("") }
    var adminClipToShare by remember { mutableStateOf<VideoClip?>(null) }

    // 0: Découpeur Gratuit, 1: Prix, 2: Retrait, 3: Problèmes, 4: Sécurité, 5: FCM Push
    var selectedAdminTab by remember { mutableStateOf(0) }
    val tabs = listOf("✂️ Découpeur Gratuit", "💰 Prix", "💳 Retrait", "⚠️ Problèmes", "🔐 Sécurité", "🔔 FCM Push")

    // Price inputs
    var monthlyPriceInput by remember(adminSettings) { mutableStateOf(adminSettings.monthlyPrice.toInt().toString()) }
    var yearlyPriceInput by remember(adminSettings) { mutableStateOf(adminSettings.yearlyPrice.toInt().toString()) }
    var currencyInput by remember(adminSettings) { mutableStateOf(adminSettings.currency) }

    // Password input
    var newPasswordInput by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }

    // Payout modal state
    var showPayoutDialog by remember { mutableStateOf(false) }
    var payoutAmountInput by remember { mutableStateOf("100") }
    var payoutMethod by remember { mutableStateOf("Stripe Connect") }
    var payoutDestination by remember { mutableStateOf("FR76 3000 4000 5000 6000 78") }

    // Ticket resolution dialog state
    var selectedTicketToResolve by remember { mutableStateOf<SupportTicket?>(null) }
    var resolutionNoteInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    val clipGradients = listOf(
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
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Admin Header Banner
        Surface(
            color = GoldenAmber.copy(0.12f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAmber),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(GoldenAmber, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = DarkBg, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Panneau Administrateur",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldenAmber
                    )
                    Text(
                        text = "Statut : Découpeur Vidéo Gratuit & VIP Illimité Actifs",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextInk
                    )
                }
                IconButton(onClick = onNavigateToStudio) {
                    Icon(imageVector = Icons.Default.MovieFilter, contentDescription = "Studio", tint = GoldenAmber)
                }
            }
        }

        // Quick VIP activator if not yet logged in as admin
        if (currentUser?.isAdmin != true) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = GoldenAmber.copy(0.18f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Session Admin Recommandée", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = GoldenAmber)
                        Text("Cliquez pour activer votre session admin VIP complète.", style = MaterialTheme.typography.bodySmall, color = TextInk, fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            val ok = repository.loginAsAdmin("@Bingsam01")
                            if (ok) {
                                Toast.makeText(context, "Session Admin VIP activée avec succès !", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Activer VIP", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6 Tabs Scrollable Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, tabTitle ->
                val isSelected = selectedAdminTab == index
                Surface(
                    color = if (isSelected) GoldenAmber else PanelBg,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) GoldenAmber else BorderLine
                    ),
                    modifier = Modifier
                        .clickable { selectedAdminTab = index }
                        .testTag("admin_tab_$index")
                ) {
                    Text(
                        text = tabTitle,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) DarkBg else TextInk
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedAdminTab) {
            // TAB 0: DÉCOUPEUR VIDÉO GRATUIT VIP ADMIN
            0 -> {
                // Intro VIP Card
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(GoldenAmber, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = DarkBg,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Découpeur Vidéo Gratuit Admin",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextInk
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = GoldenAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "VIP ILLIMITÉ",
                                        color = GoldenAmber,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Collez un lien de n'importe quel site web. Vos découpages sont 100% gratuits, sans abonnement ni limite.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Video URL input
                    Text(
                        text = "Lien de la vidéo (YouTube, TikTok, Reels, X, Web...) :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = adminVideoUrl,
                        onValueChange = { adminVideoUrl = it },
                        placeholder = { Text("https://www.youtube.com/watch?v=... ou tiktok.com...", color = TextMuted) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = GoldenAmber)
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (adminVideoUrl.isNotBlank()) {
                                    IconButton(onClick = { adminVideoUrl = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer", tint = TextMuted)
                                    }
                                }
                                IconButton(onClick = {
                                    val clipService = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val text = clipService.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!text.isNullOrBlank()) {
                                        adminVideoUrl = text.trim()
                                        Toast.makeText(context, "Lien collé depuis le presse-papier !", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Presse-papier vide", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Coller", tint = MintCyan)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_video_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldenAmber,
                            unfocusedBorderColor = BorderLine,
                            focusedTextColor = TextInk,
                            unfocusedTextColor = TextInk,
                            cursorColor = GoldenAmber
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Test Preset Chips
                    Text(
                        text = "Liens d'exemple pour test immédiat :",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "🎬 YouTube Podcast" to "https://www.youtube.com/watch?v=tech_ai_interview_full_2025",
                            "⚡ TikTok Viral" to "https://www.tiktok.com/@creator/video/73918239019284",
                            "📸 Instagram Reel" to "https://www.instagram.com/reels/C9x81_sample_clip/",
                            "🐦 X / Twitter" to "https://x.com/tech_innovator/status/1892837492193",
                            "🌐 Direct MP4" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                        ).forEach { (label, url) ->
                            Surface(
                                color = PanelElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                                modifier = Modifier.clickable {
                                    adminVideoUrl = url
                                    Toast.makeText(context, "Lien $label sélectionné !", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MintCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Options: Format
                    Text(
                        text = "Format d'exportation vertical / horizontal :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextInk
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoFormat.values().forEach { format ->
                            val isSel = adminSelectedFormat == format
                            Surface(
                                color = if (isSel) GoldenAmber.copy(0.2f) else PanelElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) GoldenAmber else BorderLine
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { adminSelectedFormat = format }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = format.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) GoldenAmber else TextInk,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Text(
                                        text = format.ratioLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Options: Duration & Quality
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Duration column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Durée par clip :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextInk
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(15 to "15s", 30 to "30s", 60 to "60s", 90 to "90s").forEach { (sec, label) ->
                                    val isSel = adminSelectedDurationSec == sec
                                    Surface(
                                        color = if (isSel) GoldenAmber else PanelElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { adminSelectedDurationSec = sec }
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) DarkBg else TextInk
                                        )
                                    }
                                }
                            }
                        }

                        // Quality column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Résolution :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextInk
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(VideoQuality.P1080 to "1080p FHD", VideoQuality.P4K to "4K Ultra HD").forEach { (q, label) ->
                                    val isSel = adminSelectedQuality == q
                                    Surface(
                                        color = if (isSel) MintCyan else PanelElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { adminSelectedQuality = q }
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) DarkBg else TextInk
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subtitles & AI Viral Features
                    Text(
                        text = "Sous-titres & Intelligence Artificielle :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextInk
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Language chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Français", "Anglais", "Espagnol", "Allemand", "Portugais", "Italien").forEach { lang ->
                            val isSel = adminSubtitleLang == lang
                            Surface(
                                color = if (isSel) VioletPurple.copy(0.3f) else PanelElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) VioletPurple else BorderLine),
                                modifier = Modifier.clickable { adminSubtitleLang = lang }
                            ) {
                                Text(
                                    text = lang,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSel) Color.White else TextInk
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // AI Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Détection Virale IA (Hooks & Crochets)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextInk)
                            Text("Isole les 10 meilleures punchlines et pics d'attention", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = adminAutoHookDetection,
                            onCheckedChange = { adminAutoHookDetection = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldenAmber, checkedTrackColor = GoldenAmber.copy(0.3f))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recadrage Intelligent 9:16 (Face Tracking)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextInk)
                            Text("Suit automatiquement les visages au centre du clip", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = adminSmartFaceTracking,
                            onCheckedChange = { adminSmartFaceTracking = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MintCyan, checkedTrackColor = MintCyan.copy(0.3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Generate Button (100% Free Admin)
                    GradientButton(
                        text = if (isGenerating) "Découpage en cours..." else "✂️ Découper la vidéo gratuitement (VIP Admin)",
                        icon = Icons.Default.ContentCut,
                        colors = listOf(GoldenAmber, CoralRed),
                        enabled = !isGenerating,
                        onClick = {
                            if (adminVideoUrl.isBlank() || !adminVideoUrl.startsWith("http")) {
                                Toast.makeText(context, "Veuillez coller un lien valide (http...)", Toast.LENGTH_SHORT).show()
                                return@GradientButton
                            }

                            // Ensure admin user session
                            if (currentUser?.isAdmin != true) {
                                repository.loginAsAdmin("@Bingsam01")
                            }

                            val settings = VideoSettings(
                                url = adminVideoUrl.trim(),
                                format = adminSelectedFormat,
                                clipDurationSeconds = adminSelectedDurationSec,
                                quality = adminSelectedQuality,
                                subtitleLanguage = adminSubtitleLang,
                                targetTranslationLanguage = adminTargetTranslateLang,
                                isTranslated = adminIsTranslationEnabled
                            )

                            coroutineScope.launch {
                                repository.generateClips(settings)
                            }
                            Toast.makeText(context, "✂️ Découpage gratuit lancé ! Vos 10 clips sont en cours de génération.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_generate_clips_button")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚡ Gratuit & Illimité Admin : Aucun abonnement, paiement ou carte bancaire requis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldenAmber,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Progress indicator
                    if (isGenerating || (progress > 0 && progress < 100)) {
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = GoldenAmber,
                            trackColor = BorderLine
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = statusMsg.ifBlank { "Traitement de la vidéo..." },
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldenAmber
                            )
                            Text(
                                text = "$progress%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldenAmber
                            )
                        }

                        Surface(
                            color = GoldenAmber.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAmber.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Alerte push FCM active : Vous recevrez une notification push à la fin du découpage !",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldenAmber,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Generated Clips Section
                val filteredAdminClips = remember(clips, adminClipsSearchQuery) {
                    if (adminClipsSearchQuery.isBlank()) {
                        clips
                    } else {
                        val q = adminClipsSearchQuery.lowercase().trim()
                        clips.filter {
                            it.title.lowercase().contains(q) ||
                            it.subtitleExcerpt.lowercase().contains(q) ||
                            it.summary.lowercase().contains(q)
                        }
                    }
                }

                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Vos Clips Découpés (${filteredAdminClips.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextInk
                            )
                            Text(
                                text = "Téléchargez gratuitement ou partagez directement sur les réseaux sociaux.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        IconButton(onClick = onNavigateToStudio) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Studio", tint = GoldenAmber)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search inside clips
                    OutlinedTextField(
                        value = adminClipsSearchQuery,
                        onValueChange = { adminClipsSearchQuery = it },
                        placeholder = { Text("Rechercher dans les clips (titre, sujet, punchline)...", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        trailingIcon = {
                            if (adminClipsSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { adminClipsSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldenAmber,
                            unfocusedBorderColor = BorderLine,
                            focusedTextColor = TextInk,
                            unfocusedTextColor = TextInk
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (filteredAdminClips.isEmpty()) {
                        Text(
                            text = "Aucun clip ne correspond à votre recherche.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        filteredAdminClips.forEachIndexed { index, clip ->
                            val grad = clipGradients[clip.gradientIndex % clipGradients.size]
                            Surface(
                                color = PanelElevated,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .testTag("admin_clip_item_$index")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Gradient Preview thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp, 96.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Brush.verticalGradient(grad))
                                                .clickable { onClipClick(clip) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxSize().padding(6.dp)
                                            ) {
                                                Surface(
                                                    color = DarkBg.copy(0.7f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "🔥 ${clip.viralScore}",
                                                        color = GoldenAmber,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Icon(imageVector = Icons.Default.PlayCircleFilled, contentDescription = "Lire", tint = Color.White, modifier = Modifier.size(28.dp))
                                                Surface(
                                                    color = DarkBg.copy(0.7f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "${clip.durationSeconds}s",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Clip metadata
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = GoldenAmber.copy(0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Clip #${index + 1}",
                                                        color = GoldenAmber,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = clip.formattedTimestamp,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = clip.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextInk,
                                                maxLines = 2
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "« ${clip.subtitleExcerpt} »",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MintCyan,
                                                maxLines = 2,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action buttons row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Download Free Button
                                        Button(
                                            onClick = {
                                                repository.recordDownloadedClip(clip, adminSelectedQuality, "Admin Studio")
                                                Toast.makeText(context, "📥 Clip '${clip.title}' téléchargé gratuitement et ajouté à l'Historique !", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MintCyan),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = DarkBg, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Télécharger", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        // Share Button
                                        Button(
                                            onClick = { adminClipToShare = clip },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PanelBg),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAmber),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Partager", color = GoldenAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        // Details / Play Modal
                                        Button(
                                            onClick = { onClipClick(clip) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PanelBg),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = TextInk, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Lecteur", color = TextInk, fontWeight = FontWeight.Normal, fontSize = 11.sp)
                                        }

                                        // Copy Hashtags
                                        IconButton(
                                            onClick = {
                                                val clipService = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val copyText = "🔥 ${clip.title}\n${clip.subtitleExcerpt}\n\n#SMCut #Viral #Clips #TikTok #Reels #Shorts"
                                                clipService.setPrimaryClip(ClipData.newPlainText("Clip Copy", copyText))
                                                Toast.makeText(context, "Titre et hashtags copiés !", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copier", tint = TextMuted, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 1: TARIFICATION / PRIX
            1 -> {
                SectionCard {
                    Text(
                        text = "Gestion des Tarifs d'Abonnement",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextInk
                    )
                    Text(
                        text = "Modifiez les prix en direct. Les nouveaux tarifs seront immédiatement appliqués aux nouveaux abonnés.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = monthlyPriceInput,
                            onValueChange = { monthlyPriceInput = it },
                            label = { Text("Prix Mensuel (1 mois)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = { Text(currencyInput, color = GoldenAmber, modifier = Modifier.padding(end = 12.dp)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldenAmber,
                                unfocusedBorderColor = BorderLine,
                                focusedTextColor = TextInk,
                                unfocusedTextColor = TextInk
                            )
                        )

                        OutlinedTextField(
                            value = yearlyPriceInput,
                            onValueChange = { yearlyPriceInput = it },
                            label = { Text("Prix Annuel (1 an)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = { Text(currencyInput, color = GoldenAmber, modifier = Modifier.padding(end = 12.dp)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldenAmber,
                                unfocusedBorderColor = BorderLine,
                                focusedTextColor = TextInk,
                                unfocusedTextColor = TextInk
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "Devise appliquée :", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("$" to "Dollar USD ($)", "€" to "Euro EUR (€)").forEach { (symbol, label) ->
                            val isSel = currencyInput == symbol
                            Surface(
                                color = if (isSel) GoldenAmber.copy(0.2f) else PanelElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) GoldenAmber else BorderLine),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { currencyInput = symbol }
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) GoldenAmber else TextInk
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    GradientButton(
                        text = "Enregistrer les nouveaux prix",
                        icon = Icons.Default.Save,
                        colors = listOf(GoldenAmber, CoralRed),
                        onClick = {
                            val m = monthlyPriceInput.toDoubleOrNull() ?: 5.0
                            val y = yearlyPriceInput.toDoubleOrNull() ?: 50.0
                            repository.updateAdminPrices(m, y, currencyInput)
                            Toast.makeText(context, "Tarifs mis à jour : ${m.toInt()}$currencyInput/m et ${y.toInt()}$currencyInput/an !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // TAB 2: FINANCES & RETRAIT D'ARGENT
            2 -> {
                SectionCard {
                    Text(
                        text = "Revenus & Retrait Bancaire",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextInk
                    )
                    Text(
                        text = "Suivez les fonds collectés et virez l'argent vers votre compte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = PanelElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Solde Retirable", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${adminSettings.availableBalance.toInt()} ${adminSettings.currency}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MintCyan
                                )
                            }
                        }

                        Surface(
                            color = PanelElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Revenus Cumulés", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${adminSettings.totalRevenue.toInt()} ${adminSettings.currency}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldenAmber
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GradientButton(
                        text = "Retirer l'argent vers mon compte",
                        icon = Icons.Default.AccountBalance,
                        colors = listOf(MintCyan, VioletPurple),
                        onClick = { showPayoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_funds_button")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Historique des virements & retraits :",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextInk
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    payouts.forEach { payout ->
                        Surface(
                            color = PanelElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${payout.amount.toInt()} ${payout.currency} via ${payout.method}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextInk
                                    )
                                    Text(
                                        text = "${payout.destination} • ${payout.date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                                Surface(
                                    color = MintCyan.copy(0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = payout.status,
                                        color = MintCyan,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TAB 3: PROBLÈMES SIGNALÉS & TICKETS
            3 -> {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Problèmes & Support",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextInk
                        )
                        Surface(
                            color = CoralRed.copy(0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${tickets.size} tickets",
                                color = CoralRed,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Réglez les anomalies techniques, signalements utilisateurs et charge serveurs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    tickets.forEach { ticket ->
                        val isResolved = ticket.status == "Résolu"
                        Surface(
                            color = PanelElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isResolved) BorderLine else CoralRed.copy(0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clickable {
                                    selectedTicketToResolve = ticket
                                    resolutionNoteInput = ticket.resolutionNote
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = when (ticket.category) {
                                            "Technique" -> CoralRed.copy(0.2f)
                                            "Abonnement" -> MintCyan.copy(0.2f)
                                            "Qualité" -> VioletPurple.copy(0.2f)
                                            else -> GoldenAmber.copy(0.2f)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = ticket.category,
                                            color = TextInk,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }

                                    Surface(
                                        color = if (isResolved) MintCyan.copy(0.15f) else CoralRed.copy(0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = ticket.status,
                                            color = if (isResolved) MintCyan else CoralRed,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = ticket.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextInk
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = ticket.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )

                                if (ticket.resolutionNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = DarkBg,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Note de résolution : ${ticket.resolutionNote}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MintCyan,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 4: SÉCURITÉ & MOT DE PASSE ADMIN
            4 -> {
                SectionCard {
                    Text(
                        text = "Sécurité & Mot de Passe Admin",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextInk
                    )
                    Text(
                        text = "Changez votre mot de passe administrateur pour sécuriser votre espace VIP et vos retraits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("Nouveau mot de passe") },
                        placeholder = { Text("Ex: MonNouveauPassSecret99!") },
                        singleLine = true,
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldenAmber,
                            unfocusedBorderColor = BorderLine,
                            focusedTextColor = TextInk,
                            unfocusedTextColor = TextInk
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    GradientButton(
                        text = "Mettre à jour le mot de passe",
                        icon = Icons.Default.LockReset,
                        colors = listOf(GoldenAmber, CoralRed),
                        onClick = {
                            if (newPasswordInput.trim().length >= 4) {
                                repository.updateAdminPassword(newPasswordInput.trim())
                                Toast.makeText(context, "Mot de passe administrateur mis à jour avec succès !", Toast.LENGTH_LONG).show()
                                newPasswordInput = ""
                            } else {
                                Toast.makeText(context, "Le mot de passe doit contenir au moins 4 caractères", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Cluster Health Status
                    Text(text = "Statut Infrastructure Backend :", style = MaterialTheme.typography.titleMedium, color = TextInk)
                    Spacer(modifier = Modifier.height(10.dp))

                    val clusterNodes = listOf(
                        "Cluster yt-dlp Video Extractor" to "Opérationnel (v2026.10)",
                        "Cluster FFmpeg Transcode (4K GPU)" to "4 nœuds actifs (Charge 34%)",
                        "Moteur Whisper AI Transcription" to "Prêt (Latence 210ms)",
                        "Stockage S3 Clips CDN" to "Disponible (99.99%)"
                    )

                    clusterNodes.forEach { (name, state) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, style = MaterialTheme.typography.bodySmall, color = TextInk)
                            Surface(color = MintCyan.copy(0.15f), shape = RoundedCornerShape(4.dp)) {
                                Text(state, color = MintCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            // TAB 5: FIREBASE CLOUD MESSAGING (FCM) CONSOLE
            5 -> {
                val fcmToken by FCMManager.fcmToken.collectAsState()
                val isPushEnabled by FCMManager.isPushEnabled.collectAsState()
                val lastNotification by FCMManager.lastNotificationReceived.collectAsState()

                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Firebase Cloud Messaging (FCM)",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextInk
                            )
                            Text(
                                text = "Gestion des alertes push lors de la fin de découpe vidéo",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Surface(
                            color = MintCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MintCyan)
                        ) {
                            Text(
                                text = "FCM Actif",
                                color = MintCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // FCM Configuration Details
                    val fcmParams = listOf(
                        "Projet Firebase" to "smcut-ai-vizzard",
                        "Canal de notification" to "smcut_video_channel (Haute Priorité)",
                        "Topic de diffusion" to "video_processing_updates",
                        "Service Background" to "SMCutFirebaseMessagingService"
                    )

                    fcmParams.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Text(text = value, style = MaterialTheme.typography.bodySmall, color = TextInk, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // FCM Device Token Display
                    Text(
                        text = "Jeton FCM de l'appareil (Device Token) :",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextInk,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = DarkBg,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = fcmToken ?: "Récupération du jeton FCM en cours...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MintCyan,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            if (!fcmToken.isNullOrBlank()) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("FCM Token", fcmToken)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Jeton FCM copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copier le jeton", tint = TextInk, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Global Push Notifications Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Notifications Push Utilisateur", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextInk)
                            Text(text = if (isPushEnabled) "Activées pour toutes les découpes" else "Désactivées", style = MaterialTheme.typography.bodySmall, color = if (isPushEnabled) MintCyan else TextMuted)
                        }
                        Switch(
                            checked = isPushEnabled,
                            onCheckedChange = { FCMManager.setPushEnabled(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MintCyan,
                                uncheckedTrackColor = BorderLine
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test FCM Push Trigger Button
                    GradientButton(
                        text = "Simuler Push FCM : Découpe Terminée",
                        icon = Icons.Default.NotificationsActive,
                        colors = listOf(MintCyan, VioletPurple),
                        onClick = {
                            FCMManager.notifyVideoProcessingFinished(
                                context = context,
                                videoTitle = "Podcast Tech & Business #42",
                                clipCount = 10,
                                quality = "1080p Full HD"
                            )
                            Toast.makeText(context, "Push FCM envoyé ! Vérifiez votre barre de notifications Android.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (lastNotification != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Dernière alerte reçue : $lastNotification",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldenAmber,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    // Modal Retrait d'argent
    if (showPayoutDialog) {
        AlertDialog(
            onDismissRequest = { showPayoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = MintCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Demande de Virement", color = TextInk, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Solde disponible : ${adminSettings.availableBalance.toInt()} ${adminSettings.currency}",
                        color = MintCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = payoutAmountInput,
                        onValueChange = { payoutAmountInput = it },
                        label = { Text("Montant à retirer") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintCyan,
                            unfocusedBorderColor = BorderLine,
                            focusedTextColor = TextInk,
                            unfocusedTextColor = TextInk
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Méthode de virement :", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    val methods = listOf("Stripe Connect", "Virement IBAN", "PayPal", "Mobile Money")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        methods.forEach { m ->
                            val isSel = payoutMethod == m
                            Surface(
                                color = if (isSel) MintCyan.copy(0.2f) else DarkBg,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) MintCyan else BorderLine),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { payoutMethod = m }
                            ) {
                                Text(
                                    text = m.split(" ").first(),
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) MintCyan else TextInk
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = payoutDestination,
                        onValueChange = { payoutDestination = it },
                        label = { Text("Coordonnées de paiement (IBAN / email)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintCyan,
                            unfocusedBorderColor = BorderLine,
                            focusedTextColor = TextInk,
                            unfocusedTextColor = TextInk
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payoutAmountInput.toDoubleOrNull() ?: 0.0
                        val success = repository.requestPayout(amount, payoutMethod, payoutDestination)
                        if (success) {
                            showPayoutDialog = false
                            Toast.makeText(context, "Virement de ${amount.toInt()}${adminSettings.currency} ordonné avec succès !", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Montant invalide ou solde insuffisant", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintCyan)
                ) {
                    Text("Confirmer le virement", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayoutDialog = false }) {
                    Text("Annuler", color = TextMuted)
                }
            },
            containerColor = PanelBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Résolution de Ticket
    if (selectedTicketToResolve != null) {
        val ticket = selectedTicketToResolve!!
        AlertDialog(
            onDismissRequest = { selectedTicketToResolve = null },
            title = {
                Text("Traiter l'incident #${ticket.id}", color = TextInk, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(ticket.title, fontWeight = FontWeight.Bold, color = CoralRed)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(ticket.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = resolutionNoteInput,
                        onValueChange = { resolutionNoteInput = it },
                        label = { Text("Note technique / Correctif déployé") },
                        placeholder = { Text("Ex: Cache vidé, patch appliqué sur les nœuds FFmpeg...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintCyan,
                            unfocusedBorderColor = BorderLine,
                            focusedTextColor = TextInk,
                            unfocusedTextColor = TextInk
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.resolveTicket(ticket.id, resolutionNoteInput)
                        selectedTicketToResolve = null
                        Toast.makeText(context, "Ticket #${ticket.id} marqué comme résolu !", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintCyan)
                ) {
                    Text("Marquer comme résolu", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTicketToResolve = null }) {
                    Text("Fermer", color = TextMuted)
                }
            },
            containerColor = PanelBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Share Dialog for Admin Clips
    if (adminClipToShare != null) {
        val clip = adminClipToShare!!
        AlertDialog(
            onDismissRequest = { adminClipToShare = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = GoldenAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Partager le Clip (Admin)", color = TextInk, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(clip.title, fontWeight = FontWeight.Bold, color = TextInk)
                    Text(
                        "Exportez ce clip découpé directement vers vos réseaux sociaux préférés :",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    listOf(
                        Triple("TikTok", "com.zhiliaoapp.musically", CoralRed),
                        Triple("Instagram Reels", "com.instagram.android", VioletPurple),
                        Triple("YouTube Shorts", "com.google.android.youtube", CoralRed),
                        Triple("WhatsApp", "com.whatsapp", MintCyan),
                        Triple("X (Twitter)", "com.twitter.android", GoldenAmber),
                        Triple("Toutes les applications", "", TextInk)
                    ).forEach { (name, pkg, color) ->
                        Surface(
                            color = PanelElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val shareText = "🔥 Clip Viral découpé par SMCut AI : ${clip.title}\n« ${clip.subtitleExcerpt} »\n\n#SMCut #Viral #Clips #TikTok #Reels #Shorts"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, clip.title)
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        if (pkg.isNotEmpty()) {
                                            `package` = pkg
                                        }
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val fallback = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(fallback, "Partager avec..."))
                                    }
                                    adminClipToShare = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(name, color = TextInk, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { adminClipToShare = null }) {
                    Text("Fermer", color = TextMuted)
                }
            },
            containerColor = PanelBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
