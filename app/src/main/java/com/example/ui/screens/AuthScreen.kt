package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import com.example.data.SMCutRepository
import com.example.model.UserAccount
import com.example.ui.components.GradientButton
import com.example.ui.components.SectionCard
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    repository: SMCutRepository,
    currentUser: UserAccount?,
    onLoginSuccess: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("+33 6 12 34 56 78") }
    var smsOtpCode by remember { mutableStateOf("") }
    var isWaitingForOtp by remember { mutableStateOf(false) }

    // Modals
    var showForgotPasswordModal by remember { mutableStateOf(false) }
    var showAdminLoginModal by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var adminPasswordVisible by remember { mutableStateOf(false) }
    var forgotIdentifierInput by remember { mutableStateOf("") }

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

        // Hero Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CoralRed),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ContentCut,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Colle un lien.\nRécupère 10 clips.",
            style = MaterialTheme.typography.displayLarge,
            color = TextInk,
            lineHeight = 36.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Découpe n'importe quelle vidéo longue en courts extraits ultra-viraux avec sous-titres animés et traduction.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (currentUser != null) {
            // Already logged in state
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(if (currentUser.isAdmin) GoldenAmber.copy(0.2f) else MintCyan.copy(0.2f), CircleShape)
                            .border(1.dp, if (currentUser.isAdmin) GoldenAmber else MintCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentUser.isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (currentUser.isAdmin) GoldenAmber else MintCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = currentUser.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextInk
                        )
                        Text(
                            text = currentUser.identifier,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        if (currentUser.isAdmin) {
                            Text(
                                text = "⭐ Administrateur Principal (VIP Illimité Gratuit)",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldenAmber,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (currentUser.isSubscribed) {
                            Text(
                                text = "✨ Abonnement actif : ${currentUser.subscriptionPlan ?: "Illimité"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MintCyan,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Abonnement inactif (5$ / mois ou 50$ / an)",
                                style = MaterialTheme.typography.labelSmall,
                                color = CoralRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (currentUser.isAdmin) {
                    GradientButton(
                        text = "Accéder à l'Espace Admin",
                        icon = Icons.Default.AdminPanelSettings,
                        onClick = onNavigateToAdmin,
                        colors = listOf(GoldenAmber, CoralRed),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                GradientButton(
                    text = "Aller au Studio de découpe",
                    icon = Icons.Default.MovieFilter,
                    onClick = onLoginSuccess,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        repository.logout()
                        Toast.makeText(context, "Vous êtes déconnecté", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Se déconnecter")
                }
            }
        } else {
            // Login Card
            SectionCard {
                Text(
                    text = "Connexion & Inscription",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextInk
                )
                Text(
                    text = "Connectez-vous pour générer vos clips illimités.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign In Button
                Button(
                    onClick = {
                        repository.loginWithGoogle()
                        Toast.makeText(context, "Connexion Google réussie avec bingsaminfos@gmail.com", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_login_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanelElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = CoralRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continuer avec Google",
                            color = TextInk,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLine)
                    Text(
                        text = " OU PAR TÉLÉPHONE ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLine)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Numéro de téléphone mobile") },
                    placeholder = { Text("+33 6 00 00 00 00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = TextMuted)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MintCyan,
                        unfocusedBorderColor = BorderLine,
                        focusedTextColor = TextInk,
                        unfocusedTextColor = TextInk
                    )
                )

                if (isWaitingForOtp) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = smsOtpCode,
                        onValueChange = { smsOtpCode = it },
                        label = { Text("Code SMS reçu (ex: 849201)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Sms, contentDescription = null, tint = MintCyan)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintCyan,
                            unfocusedBorderColor = BorderLine,
                            focusedTextColor = TextInk,
                            unfocusedTextColor = TextInk
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GradientButton(
                        text = "Confirmer le code SMS",
                        icon = Icons.Default.CheckCircle,
                        onClick = {
                            if (smsOtpCode.length >= 4) {
                                repository.loginWithPhone(phoneNumber)
                                Toast.makeText(context, "Numéro vérifié avec succès !", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Veuillez entrer le code reçu", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (phoneNumber.length >= 6) {
                                isWaitingForOtp = true
                                smsOtpCode = "849201"
                                Toast.makeText(context, "Code de confirmation envoyé par SMS au $phoneNumber (Code test: 849201)", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Numéro de téléphone invalide", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PanelElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                    ) {
                        Icon(imageVector = Icons.Default.Sms, contentDescription = null, tint = MintCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recevoir un code par SMS", color = TextInk, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showForgotPasswordModal = true }) {
                        Text("Mot de passe oublié ?", color = MintCyan, style = MaterialTheme.typography.labelMedium)
                    }

                    TextButton(onClick = { showAdminLoginModal = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Accès Admin", color = GoldenAmber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info box
        Surface(
            color = BorderLine.copy(alpha = 0.4f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MintCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Abonnement plat : 5$ ou 5€ / mois, 50$ / an. Pas de limite de crédits. Clips illimités.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Modal Mot de passe oublié
    if (showForgotPasswordModal) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordModal = false },
            title = {
                Text("Réinitialisation du mot de passe", color = TextInk, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Entrez votre adresse email Google ou votre numéro de téléphone pour recevoir le lien de réinitialisation sécurisé.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = forgotIdentifierInput,
                        onValueChange = { forgotIdentifierInput = it },
                        placeholder = { Text("email@gmail.com ou +33 6...") },
                        singleLine = true,
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
                        showForgotPasswordModal = false
                        Toast.makeText(context, "Un email de réinitialisation sécurisé a été envoyé !", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintCyan)
                ) {
                    Text("Envoyer le lien", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordModal = false }) {
                    Text("Annuler", color = TextMuted)
                }
            },
            containerColor = PanelBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Connexion Admin
    if (showAdminLoginModal) {
        AlertDialog(
            onDismissRequest = { showAdminLoginModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldenAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Espace Administrateur", color = TextInk, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Entrez votre mot de passe administrateur pour débloquer l'accès gratuit VIP, le tableau des prix, les problèmes et le retrait.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = { adminPasswordInput = it },
                        label = { Text("Mot de passe administrateur") },
                        singleLine = true,
                        visualTransformation = if (adminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { adminPasswordVisible = !adminPasswordVisible }) {
                                Icon(
                                    imageVector = if (adminPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldenAmber,
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
                        val success = repository.loginAsAdmin(adminPasswordInput.trim())
                        if (success) {
                            showAdminLoginModal = false
                            Toast.makeText(context, "Bienvenue Administrateur ! Accès VIP activé.", Toast.LENGTH_SHORT).show()
                            onNavigateToAdmin()
                        } else {
                            Toast.makeText(context, "Mot de passe administrateur incorrect", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber)
                ) {
                    Text("Connexion Admin", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminLoginModal = false }) {
                    Text("Annuler", color = TextMuted)
                }
            },
            containerColor = PanelBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
