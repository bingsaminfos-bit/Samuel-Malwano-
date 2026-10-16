package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SMCutRepository
import com.example.model.UserAccount
import com.example.ui.components.GradientButton
import com.example.ui.components.SectionCard
import com.example.ui.theme.*

@Composable
fun SubscriptionScreen(
    repository: SMCutRepository,
    currentUser: UserAccount?,
    onNavigateToStudio: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    val context = LocalContext.current
    val adminSettings by repository.adminSettings.collectAsState()

    var selectedIsYearly by remember { mutableStateOf(false) }
    var currency by remember { mutableStateOf(adminSettings.currency) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Badge
        Surface(
            color = MintCyan.copy(alpha = 0.15f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MintCyan)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = MintCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SANS SYSTÈME DE CRÉDITS",
                    color = MintCyan,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Un prix simple.\nClips illimités.",
            style = MaterialTheme.typography.displayLarge,
            color = TextInk,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Générez et téléchargez autant de vidéos que vous le souhaitez sans compter de jetons ou de crédits.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Currency Selector Toggle ($ / €)
        Row(
            modifier = Modifier
                .background(PanelBg, RoundedCornerShape(24.dp))
                .border(1.dp, BorderLine, RoundedCornerShape(24.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (currency == "$") CoralRed else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable { currency = "$" }
            ) {
                Text(
                    text = "USD ($)",
                    color = if (currency == "$") Color.White else TextMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Surface(
                color = if (currency == "€") CoralRed else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable { currency = "€" }
            ) {
                Text(
                    text = "EUR (€)",
                    color = if (currency == "€") Color.White else TextMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // VIP Admin banner if user is admin
        if (currentUser?.isAdmin == true) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(GoldenAmber.copy(0.2f), CircleShape)
                            .border(1.dp, GoldenAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldenAmber)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Compte Administrateur Actif",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldenAmber
                        )
                        Text(
                            text = "Vous bénéficiez d'un accès VIP permanent et gratuit sans aucun paiement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else if (currentUser?.isSubscribed == true) {
            // Already Subscribed Banner
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MintCyan.copy(0.2f), CircleShape)
                            .border(1.dp, MintCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MintCyan)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Votre abonnement est actif !",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MintCyan
                        )
                        Text(
                            text = "${currentUser.subscriptionPlan} • Valable jusqu'au ${currentUser.subscriptionEndDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Two Plan Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Plan Mensuel (5$ / 5€)
            Surface(
                color = if (!selectedIsYearly) PanelElevated else PanelBg,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (!selectedIsYearly) CoralRed else BorderLine
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedIsYearly = false }
                    .testTag("plan_monthly_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "1 Mois",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${adminSettings.monthlyPrice.toInt()} $currency",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = CoralRed
                        )
                    }
                    Text(
                        text = "par mois",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "• Clips illimités\n• Rendu 4K & 1080p\n• Whisper & Traduction",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Start,
                        lineHeight = 18.sp
                    )
                }
            }

            // Plan Annuel (50$ / 50€)
            Surface(
                color = if (selectedIsYearly) PanelElevated else PanelBg,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (selectedIsYearly) MintCyan else BorderLine
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedIsYearly = true }
                    .testTag("plan_yearly_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = MintCyan.copy(0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "2 MOIS OFFERTS",
                            color = MintCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1 An",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextInk
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${adminSettings.yearlyPrice.toInt()} $currency",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MintCyan
                        )
                    }
                    Text(
                        text = "par an (~${(adminSettings.yearlyPrice / 12).toInt()} $currency/m)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Accès VIP 365 jours\n• Priorité serveurs FFmpeg\n• Support prioritaire",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Start,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Big Activate CTA
        GradientButton(
            text = if (selectedIsYearly) "Choisir l'offre 1 An (${adminSettings.yearlyPrice.toInt()} $currency)"
            else "Choisir l'offre 1 Mois (${adminSettings.monthlyPrice.toInt()} $currency)",
            icon = Icons.Default.CreditCard,
            colors = if (selectedIsYearly) listOf(MintCyan, VioletPurple) else GradientCoralViolet,
            onClick = {
                if (currentUser == null) {
                    Toast.makeText(context, "Veuillez d'abord vous connecter ou créer un compte", Toast.LENGTH_SHORT).show()
                    onNavigateToAuth()
                } else if (currentUser.isAdmin) {
                    Toast.makeText(context, "Vous êtes Administrateur : accès gratuit déjà actif !", Toast.LENGTH_SHORT).show()
                    onNavigateToStudio()
                } else {
                    repository.activateSubscription(selectedIsYearly)
                    Toast.makeText(context, "Abonnement activé avec succès ! Profitez de vos clips illimités.", Toast.LENGTH_LONG).show()
                    onNavigateToStudio()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("subscribe_cta_button")
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Feature Comparison Checklist
        SectionCard {
            Text(
                text = "Ce qui est inclus dans votre formule :",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextInk
            )
            Spacer(modifier = Modifier.height(12.dp))

            val benefits = listOf(
                "Découpage illimité de vidéos courtes et longues",
                "Génération automatique de 10 clips prêts à poster",
                "Durées paramétrables de 30 secondes à 15 minutes",
                "Exportation en ultra haute définition de 480p à 4K",
                "Sous-titres animés avec synchronisation labiale",
                "Module de traduction multilingue IA",
                "Titres, descriptions et hashtags pour TikTok, Shorts, Insta & X",
                "Sans engagement, renouvellement automatique révocable"
            )

            benefits.forEach { benefit ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MintCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = benefit, style = MaterialTheme.typography.bodyMedium, color = TextInk)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
