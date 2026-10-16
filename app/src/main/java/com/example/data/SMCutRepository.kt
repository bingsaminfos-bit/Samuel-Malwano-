package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class SMCutRepository(context: Context) {

    private val appContext: Context = context.applicationContext

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smcut_preferences", Context.MODE_PRIVATE)

    // Current user state
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    // Generated clips (initialized with 10 sample clips for immediate testing & search)
    private val _clips = MutableStateFlow<List<VideoClip>>(createInitialSampleClips())
    val clips: StateFlow<List<VideoClip>> = _clips.asStateFlow()

    // Generation state
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationProgress = MutableStateFlow(0)
    val generationProgress: StateFlow<Int> = _generationProgress.asStateFlow()

    private val _generationStatusMessage = MutableStateFlow("")
    val generationStatusMessage: StateFlow<String> = _generationStatusMessage.asStateFlow()

    // Current video settings
    private val _currentSettings = MutableStateFlow(VideoSettings())
    val currentSettings: StateFlow<VideoSettings> = _currentSettings.asStateFlow()

    // Admin Settings
    private val _adminSettings = MutableStateFlow(loadAdminSettings())
    val adminSettings: StateFlow<AdminSettings> = _adminSettings.asStateFlow()

    // Support tickets
    private val _supportTickets = MutableStateFlow(loadInitialTickets())
    val supportTickets: StateFlow<List<SupportTicket>> = _supportTickets.asStateFlow()

    // Payout records
    private val _payoutRecords = MutableStateFlow(loadInitialPayouts())
    val payoutRecords: StateFlow<List<PayoutRecord>> = _payoutRecords.asStateFlow()

    // Free Video Downloader State
    private val _downloadedVideos = MutableStateFlow<List<DownloadedVideoItem>>(loadInitialDownloadedVideos())
    val downloadedVideos: StateFlow<List<DownloadedVideoItem>> = _downloadedVideos.asStateFlow()

    // Downloaded clips history
    private val _clipHistory = MutableStateFlow<List<DownloadedClipRecord>>(loadInitialClipHistory())
    val clipHistory: StateFlow<List<DownloadedClipRecord>> = _clipHistory.asStateFlow()

    init {
        // Restore last logged user if present
        loadSavedUser()
    }

    private fun loadAdminSettings(): AdminSettings {
        val pass = prefs.getString("admin_pass", "@Bingsam01") ?: "@Bingsam01"
        val monthly = prefs.getFloat("admin_price_m", 5.0f).toDouble()
        val yearly = prefs.getFloat("admin_price_y", 50.0f).toDouble()
        val cur = prefs.getString("admin_currency", "$") ?: "$"
        val balance = prefs.getFloat("admin_balance", 345.0f).toDouble()
        val revenue = prefs.getFloat("admin_revenue", 490.0f).toDouble()
        val clipsCount = prefs.getInt("admin_clips_count", 1840)
        val usersCount = prefs.getInt("admin_users_count", 74)

        return AdminSettings(
            adminPasswordHash = pass,
            monthlyPrice = monthly,
            yearlyPrice = yearly,
            currency = cur,
            availableBalance = balance,
            totalRevenue = revenue,
            totalClipsCut = clipsCount,
            totalUsers = usersCount
        )
    }

    private fun loadSavedUser() {
        val userId = prefs.getString("user_id", null)
        if (userId != null) {
            val name = prefs.getString("user_name", "Utilisateur SMCut") ?: "Utilisateur SMCut"
            val ident = prefs.getString("user_ident", "bingsaminfos@gmail.com") ?: "bingsaminfos@gmail.com"
            val isGoogle = prefs.getBoolean("user_is_google", true)
            val isSub = prefs.getBoolean("user_is_sub", false)
            val plan = prefs.getString("user_plan", null)
            val endDate = prefs.getString("user_sub_end", null)
            val isAdmin = prefs.getBoolean("user_is_admin", false)

            _currentUser.value = UserAccount(
                id = userId,
                name = name,
                identifier = ident,
                isGoogle = isGoogle,
                isSubscribed = isSub,
                subscriptionPlan = plan,
                subscriptionEndDate = endDate,
                isAdmin = isAdmin
            )
        }
    }

    private fun saveUserToPrefs(user: UserAccount?) {
        if (user == null) {
            prefs.edit()
                .remove("user_id")
                .remove("user_name")
                .remove("user_ident")
                .remove("user_is_google")
                .remove("user_is_sub")
                .remove("user_plan")
                .remove("user_sub_end")
                .remove("user_is_admin")
                .apply()
        } else {
            prefs.edit()
                .putString("user_id", user.id)
                .putString("user_name", user.name)
                .putString("user_ident", user.identifier)
                .putBoolean("user_is_google", user.isGoogle)
                .putBoolean("user_is_sub", user.isSubscribed)
                .putString("user_plan", user.subscriptionPlan)
                .putString("user_sub_end", user.subscriptionEndDate)
                .putBoolean("user_is_admin", user.isAdmin)
                .apply()
        }
    }

    // AUTH METHODS
    fun loginWithGoogle(email: String = "bingsaminfos@gmail.com", name: String = "Bingsam Creator") {
        val user = UserAccount(
            id = "goog_" + UUID.randomUUID().toString().take(8),
            name = name,
            identifier = email,
            isGoogle = true,
            isSubscribed = _currentUser.value?.isSubscribed ?: false,
            subscriptionPlan = _currentUser.value?.subscriptionPlan,
            subscriptionEndDate = _currentUser.value?.subscriptionEndDate,
            isAdmin = false
        )
        _currentUser.value = user
        saveUserToPrefs(user)
    }

    fun loginWithPhone(phoneNumber: String) {
        val user = UserAccount(
            id = "tel_" + UUID.randomUUID().toString().take(8),
            name = "Abonné Mobile",
            identifier = phoneNumber,
            isGoogle = false,
            isSubscribed = _currentUser.value?.isSubscribed ?: false,
            subscriptionPlan = _currentUser.value?.subscriptionPlan,
            subscriptionEndDate = _currentUser.value?.subscriptionEndDate,
            isAdmin = false
        )
        _currentUser.value = user
        saveUserToPrefs(user)
    }

    fun loginAsAdmin(passwordAttempt: String): Boolean {
        if (passwordAttempt == _adminSettings.value.adminPasswordHash) {
            val user = UserAccount(
                id = "admin_master",
                name = "Administrateur Système",
                identifier = "admin@smcut.internal",
                isGoogle = true,
                isSubscribed = true, // Free VIP access for Admin!
                subscriptionPlan = "Accès Administrateur Illimité (VIP Gratuit)",
                subscriptionEndDate = "Permanent",
                isAdmin = true
            )
            _currentUser.value = user
            saveUserToPrefs(user)
            return true
        }
        return false
    }

    fun logout() {
        _currentUser.value = null
        saveUserToPrefs(null)
    }

    // SUBSCRIPTION METHODS
    fun activateSubscription(isYearly: Boolean) {
        val user = _currentUser.value ?: return
        val cal = Calendar.getInstance()
        if (isYearly) cal.add(Calendar.YEAR, 1) else cal.add(Calendar.MONTH, 1)
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val endDateStr = sdf.format(cal.time)
        val planName = if (isYearly) "Annuel (1 an - ${_adminSettings.value.yearlyPrice}${_adminSettings.value.currency})" else "Mensuel (1 mois - ${_adminSettings.value.monthlyPrice}${_adminSettings.value.currency})"

        val updatedUser = user.copy(
            isSubscribed = true,
            subscriptionPlan = planName,
            subscriptionEndDate = endDateStr
        )
        _currentUser.value = updatedUser
        saveUserToPrefs(updatedUser)

        // Increment admin revenue
        val amount = if (isYearly) _adminSettings.value.yearlyPrice else _adminSettings.value.monthlyPrice
        val newBalance = _adminSettings.value.availableBalance + amount
        val newRevenue = _adminSettings.value.totalRevenue + amount
        updateAdminFinance(newBalance, newRevenue)
    }

    // VIDEO SETTINGS
    fun updateVideoSettings(settings: VideoSettings) {
        _currentSettings.value = settings
    }

    // CLIP GENERATION PROCESS
    suspend fun generateClips(settings: VideoSettings) {
        _currentSettings.value = settings
        _isGenerating.value = true
        _generationProgress.value = 5
        _generationStatusMessage.value = "Analyse de la source vidéo (${extractDomain(settings.url)})..."

        delay(600)
        _generationProgress.value = 25
        _generationStatusMessage.value = "Téléchargement flux & extraction audio HQ..."

        delay(700)
        _generationProgress.value = 50
        _generationStatusMessage.value = "Whisper AI: Transcription et détection des moments forts..."

        delay(800)
        _generationProgress.value = 75
        val transMsg = if (settings.isTranslated) " & traduction en ${settings.targetTranslationLanguage}" else ""
        _generationStatusMessage.value = "FFmpeg: Découpage ${settings.format.label} à ${settings.quality.label}$transMsg..."

        delay(700)
        _generationProgress.value = 95
        _generationStatusMessage.value = "Génération des 10 clips viraux & titres d'accroche..."

        delay(400)
        _generationProgress.value = 100
        _generationStatusMessage.value = "Terminé ! 10 clips prêts à être exportés."

        // Create 10 distinct clips based on duration
        val d = settings.clipDurationSeconds
        val generated = create10Clips(d, settings.subtitleLanguage, settings.isTranslated, settings.targetTranslationLanguage, settings.quality)
        _clips.value = generated
        _isGenerating.value = false

        // Alert user via Firebase Cloud Messaging push notification
        val domain = extractDomain(settings.url)
        val titleSummary = if (domain.isNotBlank()) "$domain (${settings.format.label})" else "Vidéo source"
        com.example.notification.FCMManager.notifyVideoProcessingFinished(
            context = appContext,
            videoTitle = titleSummary,
            clipCount = generated.size,
            quality = settings.quality.label
        )

        // Increment admin clips stat
        val newCount = _adminSettings.value.totalClipsCut + 10
        prefs.edit().putInt("admin_clips_count", newCount).apply()
        _adminSettings.value = _adminSettings.value.copy(totalClipsCut = newCount)
    }

    private fun extractDomain(url: String): String {
        return when {
            url.contains("youtube") || url.contains("youtu.be") -> "YouTube"
            url.contains("tiktok") -> "TikTok"
            url.contains("instagram") -> "Instagram Reels"
            url.contains("twitter") || url.contains("x.com") -> "X / Twitter"
            url.contains("facebook") -> "Facebook Watch"
            else -> "Lien Vidéo Direct"
        }
    }

    private fun createInitialSampleClips(): List<VideoClip> {
        val titles = listOf(
            "Le moment que personne n'attendait !",
            "Ce qu'il a révélé change absolument tout",
            "3 erreurs fatales que 99% des gens font",
            "La vérité choquante dévoilée au grand jour",
            "Le passage le plus intense de toute la vidéo",
            "Ce détail crucial que tout le monde rate",
            "La réaction en direct qui a fait le buzz",
            "Le conseil d'expert qui vaut des millions",
            "Ils n'ont jamais vu ce retournement venir",
            "La fin surprenante qui laisse sans voix"
        )
        // Diverse durations for search testing: 30s, 45s, 60s, 90s, 120s, 180s, 240s, 300s, 600s, 720s
        val sampleDurations = listOf(30, 45, 60, 90, 120, 150, 180, 240, 300, 600)
        val sampleQualities = listOf(
            VideoQuality.P1080,
            VideoQuality.P4K,
            VideoQuality.P1080,
            VideoQuality.P720,
            VideoQuality.P1440,
            VideoQuality.P1080,
            VideoQuality.P4K,
            VideoQuality.P480,
            VideoQuality.P1080,
            VideoQuality.P4K
        )
        val sampleLanguages = listOf(
            "Français",
            "Français",
            "English",
            "Français",
            "Español",
            "Français",
            "English",
            "Français",
            "العربية",
            "Français"
        )

        var currentStart = 0
        return titles.mapIndexed { i, title ->
            val duration = sampleDurations[i]
            val start = currentStart
            val end = currentStart + duration
            currentStart = end
            val quality = sampleQualities[i]
            val lang = sampleLanguages[i]
            val score = 92 + (i % 8)

            VideoClip(
                id = "demo_clip_${i + 1}",
                title = title + " [ST: $lang]",
                startSeconds = start,
                endSeconds = end,
                viralScore = score,
                gradientIndex = i % 5,
                summary = "Extrait viral de ${duration}s optimisé pour algorithmes vidéo verticaux.",
                subtitleExcerpt = "« Attendez deux secondes, regardez bien ce qui se produit sous vos yeux... »",
                quality = quality,
                subtitleLanguage = lang,
                keyPoints = listOf("Hook percutant", "Rétention maximale", "Format adapté")
            )
        }
    }

    private fun create10Clips(
        clipDurationSec: Int,
        subLang: String,
        isTranslated: Boolean,
        transLang: String,
        quality: VideoQuality
    ): List<VideoClip> {
        val rawTitles = listOf(
            "Le moment que personne n'attendait !",
            "Ce qu'il a révélé change absolument tout",
            "3 erreurs fatales que 99% des gens font",
            "La vérité choquante dévoilée au grand jour",
            "Le passage le plus intense de toute la vidéo",
            "Ce détail crucial que tout le monde rate",
            "La réaction en direct qui a fait le buzz",
            "Le conseil d'expert qui vaut des millions",
            "Ils n'ont jamais vu ce retournement venir",
            "La fin surprenante qui laisse sans voix"
        )

        val subtitlesSamples = listOf(
            "« Attendez deux secondes, regardez bien ce qui va se passer sous vos yeux... »",
            "« Si vous appliquez cette seule astuce dès ce soir, tout devient 10x plus simple. »",
            "« Beaucoup pensent que c'est une question de chance, mais la réalité est différente. »",
            "« Voici précisément pourquoi la méthode habituelle ne fonctionne plus du tout. »",
            "« Le résultat a complètement dépassé toutes les espérances des experts. »",
            "« Ne faites surtout pas l'erreur de sous-estimer ce paramètre clé. »",
            "« Tout le monde dans la pièce est resté bouche bée en découvrant les chiffres. »",
            "« C'est le secret le mieux gardé de cette industrie depuis plus de 5 ans. »",
            "« En changeant cette habitude, j'ai divisé mes efforts par deux. »",
            "« Et c'est exactement pour cela qu'il faut toujours aller jusqu'au bout. »"
        )

        val finalLang = if (isTranslated) transLang else subLang

        return rawTitles.mapIndexed { i, title ->
            val start = i * clipDurationSec
            val end = (i + 1) * clipDurationSec
            val score = 91 + ((i * 7 + 3) % 9) // 91% to 99%
            val langNotice = if (isTranslated) " [Traduit en $transLang]" else " [ST: $subLang]"
            VideoClip(
                id = "clip_${i + 1}_${UUID.randomUUID().toString().take(6)}",
                title = title + langNotice,
                startSeconds = start,
                endSeconds = end,
                viralScore = score,
                gradientIndex = i % 5,
                summary = "Extrait de haute intensité détecté automatiquement entre ${formatTime(start)} et ${formatTime(end)}.",
                subtitleExcerpt = subtitlesSamples[i % subtitlesSamples.size],
                quality = quality,
                subtitleLanguage = finalLang,
                keyPoints = listOf("Accroche percutante < 3s", "Rétention maximale", "Format optimisé pour les algorithmes")
            )
        }
    }

    private fun formatTime(s: Int): String {
        val min = s / 60
        val sec = s % 60
        return String.format("%02d:%02d", min, sec)
    }

    // 5 PREDEFINED SOCIAL MEDIA DESCRIPTION TEMPLATES
    val defaultTemplates: List<DescriptionTemplate> = listOf(
        DescriptionTemplate(
            id = "storytelling",
            name = "Storytelling & Suspense",
            category = "Intrigue & Rétention",
            iconName = "AutoStories",
            templatePattern = """😱 Ce qui s'est passé ici dépasse tout ce qu'on imaginait...
👉 {TITRE}

Regarde bien jusqu'à la dernière seconde, le dénouement est totalement inattendu ! 👀
⏱️ Extrait de {DUREE} à ne surtout pas manquer.

💬 Tu aurais réagi comment à sa place ? Dis-le moi en commentaire ! 👇
{HASHTAGS}"""
        ),
        DescriptionTemplate(
            id = "educatif",
            name = "Éducatif & Astuce Express",
            category = "Conseil & Valeur",
            iconName = "Lightbulb",
            templatePattern = """💡 1 conseil majeur que tu dois absolument appliquer :
📌 {TITRE}

En seulement {DUREE}, voici la méthode résumée pour passer au niveau supérieur.
👉 Enregistre ce Reel pour ne pas oublier l'astuce ! ✨

Partage à quelqu'un qui a besoin d'entendre ça aujourd'hui 🤝
{HASHTAGS}"""
        ),
        DescriptionTemplate(
            id = "debat",
            name = "Débat & Question Ouverte",
            category = "Engagement & Avis",
            iconName = "Forum",
            templatePattern = """🔥 Est-ce que tu es 100% d'accord ou pas du tout avec cette phrase ?
🎙️ {TITRE}

Ce passage divise totalement internet. Certains crient au génie, d'autres trouvent ça excessif.
Tu te situes dans quel camp ? 👀

Donne ton avis franc et sincère dans les commentaires ! 👇
{HASHTAGS}"""
        ),
        DescriptionTemplate(
            id = "motivation",
            name = "Motivation & Inspiration",
            category = "Impact Mental",
            iconName = "Bolt",
            templatePattern = """⚡ La piqûre de rappel dont tu avais besoin aujourd'hui :
🚀 {TITRE}

Prends {DUREE} pour intégrer ce message puissant. Le succès n'est jamais le fruit du hasard.
Abonne-toi pour une dose quotidienne d'inspiration et de discipline ✨

Envoie ça à ton/ta partenaire d'ambition 💥
{HASHTAGS}"""
        ),
        DescriptionTemplate(
            id = "humour",
            name = "Humour & Moment Inattendu",
            category = "Divertissement & Fun",
            iconName = "SentimentVerySatisfied",
            templatePattern = """😂 J'étais tellement pas prêt pour cette réaction :
🎬 {TITRE}

Le passage le plus inattendu de toute la vidéo à {DUREE} 💀😭
Tag un pote qui réagirait exactement de la même manière mdr !

{HASHTAGS}"""
        )
    )

    fun applyTemplate(clip: VideoClip, template: DescriptionTemplate, platform: SocialPlatform): String {
        val cleanTitle = clip.title.substringBefore(" [")
        val tagWords = cleanTitle.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(" ")
            .filter { it.length > 3 }
            .take(3)
            .map { "#$it" }

        val allTags = (tagWords + platform.defaultHashtags).distinct().joinToString(" ")
        val durationStr = if (clip.durationSeconds < 60) "${clip.durationSeconds} secondes" else "${clip.durationMinutesApprox} min"

        return template.templatePattern
            .replace("{TITRE}", cleanTitle)
            .replace("{DUREE}", durationStr)
            .replace("{HASHTAGS}", allTags)
    }

    // SOCIAL MEDIA METADATA GENERATOR
    fun generateSocialMediaPost(clip: VideoClip, platform: SocialPlatform): String {
        val cleanTitle = clip.title.substringBefore(" [")
        val hook = when (platform) {
            SocialPlatform.TIKTOK -> "Regarde absolument jusqu'à la fin 👀 Tu n'es pas prêt pour ça !"
            SocialPlatform.YOUTUBE_SHORTS -> "Abonne-toi pour ne rien rater des prochains épisodes 🔔👇"
            SocialPlatform.INSTAGRAM -> "Enregistre ce post pour t'en souvenir plus tard ✨ Dis-moi ton avis en commentaire !"
            SocialPlatform.FACEBOOK -> "Partage cette vidéo avec un ami qui doit absolument voir ça aujourd'hui 🤝"
            SocialPlatform.X_TWITTER -> "À retenir : ce passage est capital. Déroule pour comprendre 👇"
        }

        val tagWords = cleanTitle.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(" ")
            .filter { it.length > 3 }
            .take(3)
            .map { "#$it" }

        val allTags = (tagWords + platform.defaultHashtags).distinct().joinToString(" ")

        return buildString {
            append("📌 Titre : ").append(cleanTitle).append("\n\n")
            append("💬 Description :\n")
            append(hook).append("\n")
            append(clip.summary).append("\n")
            append("⏱️ Timing : ").append(clip.formattedTimestamp).append(" (Score viral : ").append(clip.viralScore).append("%)\n\n")
            append("🏷️ Hashtags :\n")
            append(allTags)
        }
    }

    // ADMIN MANAGEMENT
    fun updateAdminPrices(monthly: Double, yearly: Double, currency: String) {
        val current = _adminSettings.value
        val updated = current.copy(
            monthlyPrice = monthly,
            yearlyPrice = yearly,
            currency = currency
        )
        _adminSettings.value = updated
        prefs.edit()
            .putFloat("admin_price_m", monthly.toFloat())
            .putFloat("admin_price_y", yearly.toFloat())
            .putString("admin_currency", currency)
            .apply()
    }

    fun updateAdminPassword(newPassword: String) {
        _adminSettings.value = _adminSettings.value.copy(adminPasswordHash = newPassword)
        prefs.edit().putString("admin_pass", newPassword).apply()
    }

    private fun updateAdminFinance(balance: Double, revenue: Double) {
        _adminSettings.value = _adminSettings.value.copy(
            availableBalance = balance,
            totalRevenue = revenue
        )
        prefs.edit()
            .putFloat("admin_balance", balance.toFloat())
            .putFloat("admin_revenue", revenue.toFloat())
            .apply()
    }

    fun requestPayout(amount: Double, method: String, destination: String): Boolean {
        val current = _adminSettings.value
        if (amount <= 0 || amount > current.availableBalance) {
            return false
        }
        val newBalance = current.availableBalance - amount
        updateAdminFinance(newBalance, current.totalRevenue)

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val newRecord = PayoutRecord(
            id = "PAY-" + UUID.randomUUID().toString().take(6).uppercase(),
            amount = amount,
            currency = current.currency,
            method = method,
            destination = destination,
            date = sdf.format(Date()),
            status = "Traité (Virement envoyé)"
        )
        _payoutRecords.value = listOf(newRecord) + _payoutRecords.value
        return true
    }

    fun resolveTicket(ticketId: String, resolutionNote: String) {
        _supportTickets.value = _supportTickets.value.map {
            if (it.id == ticketId) {
                it.copy(status = "Résolu", resolutionNote = resolutionNote)
            } else it
        }
    }

    private fun loadInitialTickets(): List<SupportTicket> {
        return listOf(
            SupportTicket(
                id = "TICK-101",
                title = "Lien vidéo YouTube Shorts refusé",
                category = "Technique",
                reportedAt = "Aujourd'hui à 14:20",
                status = "En cours",
                description = "Erreur 403 lors du téléchargement des streams audio haute définition sur certains liens restreints.",
                resolutionNote = "Mise à jour des cookies yt-dlp déployée sur le cluster."
            ),
            SupportTicket(
                id = "TICK-102",
                title = "Paiement annuel en attente de validation",
                category = "Abonnement",
                reportedAt = "Hier à 18:45",
                status = "En attente",
                description = "L'utilisateur signale que son abonnement 50$ par carte bancaire a été débité sans activation immédiate.",
                resolutionNote = ""
            ),
            SupportTicket(
                id = "TICK-103",
                title = "Sous-titres décalés de 1.5 seconde",
                category = "Qualité",
                reportedAt = "Il y a 2 jours",
                status = "Résolu",
                description = "Sur les vidéos à 60 FPS, les timestamps Whisper avaient un léger retard d'alignement avec les lèvres.",
                resolutionNote = "Correction du recalage automatique des timestamps dans FFmpeg filter graph."
            ),
            SupportTicket(
                id = "TICK-104",
                title = "Saturation des instances GPU FFmpeg",
                category = "Serveur",
                reportedAt = "Il y a 3 jours",
                status = "Résolu",
                description = "Pic de charge à 19h avec plus de 200 conversions simultanées en 4K.",
                resolutionNote = "Auto-scaling activé : 4 nœuds GPU supplémentaires ajoutés."
            )
        )
    }

    private fun loadInitialPayouts(): List<PayoutRecord> {
        return listOf(
            PayoutRecord(
                id = "PAY-849201",
                amount = 150.0,
                currency = "$",
                method = "Stripe Connect",
                destination = "Compte Pro FR76...8942",
                date = "04/10/2026",
                status = "Reçu"
            ),
            PayoutRecord(
                id = "PAY-732109",
                amount = 95.0,
                currency = "$",
                method = "PayPal Payouts",
                destination = "bingsaminfos@gmail.com",
                date = "28/09/2026",
                status = "Reçu"
            )
        )
    }

    // FREE VIDEO DOWNLOADER IMPLEMENTATION
    val supportedDownloadSites = listOf(
        "TikTok (Sans filigrane)",
        "YouTube & Shorts",
        "Instagram Reels & Posts",
        "Facebook Watch",
        "X / Twitter",
        "Pinterest Video",
        "Vimeo",
        "Dailymotion",
        "Reddit Video",
        "Snapchat",
        "Threads",
        "Tout lien MP4 / M3U8"
    )

    fun detectPlatformName(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains("tiktok") -> "TikTok"
            lower.contains("youtube") || lower.contains("youtu.be") -> "YouTube"
            lower.contains("instagram") -> "Instagram"
            lower.contains("twitter") || lower.contains("x.com") -> "X / Twitter"
            lower.contains("facebook") || lower.contains("fb.watch") -> "Facebook"
            lower.contains("pinterest") -> "Pinterest"
            lower.contains("vimeo") -> "Vimeo"
            lower.contains("dailymotion") -> "Dailymotion"
            lower.contains("reddit") -> "Reddit"
            lower.contains("threads") -> "Threads"
            lower.contains("snapchat") -> "Snapchat"
            else -> "Lien Web Direct"
        }
    }

    suspend fun executeFreeVideoDownload(
        url: String,
        quality: String,
        noWatermark: Boolean,
        onProgressUpdate: (Int, String) -> Unit
    ): DownloadedVideoItem {
        val platform = detectPlatformName(url)
        onProgressUpdate(15, "Connexion sécurisée et analyse du flux vidéo...")
        delay(400)
        onProgressUpdate(35, "Extraction du lien source direct (${platform})...")
        delay(450)
        val wmText = if (noWatermark && (platform == "TikTok" || platform == "Instagram")) " [Sans filigrane HD]" else ""
        onProgressUpdate(65, "Téléchargement haute vitesse $quality$wmText...")
        delay(550)
        onProgressUpdate(90, "Vérification de l'intégrité et sauvegarde MP4...")
        delay(350)
        onProgressUpdate(100, "Téléchargement terminé avec succès !")

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val generatedTitle = when (platform) {
            "TikTok" -> "Vidéo TikTok Tendance$wmText"
            "YouTube" -> "Vidéo YouTube HQ"
            "Instagram" -> "Reel Instagram Créateur$wmText"
            "Facebook" -> "Vidéo Publique Facebook"
            "X / Twitter" -> "Clip Média X / Twitter"
            else -> "Vidéo Web Téléchargée"
        }

        val size = when (quality) {
            "4K Ultra HD" -> 88.4
            "1080p Full HD" -> 34.2
            "720p HD" -> 19.6
            "480p SD" -> 9.8
            "MP3 Audio (HQ)" -> 4.5
            else -> 25.0
        }

        val newItem = DownloadedVideoItem(
            id = "dl_" + UUID.randomUUID().toString().take(8),
            title = "$generatedTitle (${quality})",
            sourceUrl = url,
            sourcePlatform = platform,
            formatQuality = quality,
            fileSizeMb = size,
            downloadDate = sdf.format(Date()),
            durationText = "02:45",
            noWatermark = noWatermark
        )

        _downloadedVideos.value = listOf(newItem) + _downloadedVideos.value

        // Also register in unified downloaded clips history
        val clipRecord = DownloadedClipRecord(
            id = "dl_clip_" + UUID.randomUUID().toString().take(8),
            title = newItem.title,
            formattedTimestamp = "00:00 – ${newItem.durationText}",
            durationSeconds = 165,
            formatQuality = quality,
            fileSizeMb = size,
            downloadDate = sdf.format(Date()),
            sourcePlatform = platform,
            viralScore = 95,
            subtitleExcerpt = "« Vidéo complète téléchargée sans filigrane depuis $platform »",
            gradientIndex = 2,
            isAiCutClip = false
        )
        _clipHistory.value = listOf(clipRecord) + _clipHistory.value

        return newItem
    }

    fun deleteDownloadedVideo(id: String) {
        _downloadedVideos.value = _downloadedVideos.value.filter { it.id != id }
    }

    private fun loadInitialDownloadedVideos(): List<DownloadedVideoItem> {
        return listOf(
            DownloadedVideoItem(
                id = "dl_demo_1",
                title = "TikTok Tendance Danse & Musique (Sans Filigrane)",
                sourceUrl = "https://www.tiktok.com/@creator/video/7291049281",
                sourcePlatform = "TikTok",
                formatQuality = "1080p Full HD",
                fileSizeMb = 14.8,
                downloadDate = "Aujourd'hui à 11:30",
                durationText = "00:48",
                noWatermark = true
            ),
            DownloadedVideoItem(
                id = "dl_demo_2",
                title = "YouTube Podcast Masterclass Résumé",
                sourceUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                sourcePlatform = "YouTube",
                formatQuality = "720p HD",
                fileSizeMb = 48.2,
                downloadDate = "Hier à 16:15",
                durationText = "06:12",
                noWatermark = false
            ),
            DownloadedVideoItem(
                id = "dl_demo_3",
                title = "Reel Instagram Voyage & Paysage Nature",
                sourceUrl = "https://www.instagram.com/reel/C8kPqZ/",
                sourcePlatform = "Instagram",
                formatQuality = "1080p Full HD",
                fileSizeMb = 21.5,
                downloadDate = "Il y a 2 jours",
                durationText = "01:15",
                noWatermark = true
            )
        )
    }

    // CLIP DOWNLOAD HISTORY METHODS
    fun recordDownloadedClip(
        clip: VideoClip,
        quality: VideoQuality = clip.quality,
        sourcePlatform: String = "YouTube / Web"
    ): DownloadedClipRecord {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sizeMb = when (quality) {
            VideoQuality.P4K -> 64.5
            VideoQuality.P1440 -> 42.0
            VideoQuality.P1080 -> 24.8
            VideoQuality.P720 -> 14.2
            VideoQuality.P480 -> 7.5
        }
        val record = DownloadedClipRecord(
            id = "clip_hist_" + UUID.randomUUID().toString().take(8),
            title = clip.title,
            formattedTimestamp = clip.formattedTimestamp,
            durationSeconds = clip.durationSeconds,
            formatQuality = quality.label,
            fileSizeMb = sizeMb,
            downloadDate = sdf.format(Date()),
            sourcePlatform = sourcePlatform,
            viralScore = clip.viralScore,
            subtitleExcerpt = clip.subtitleExcerpt,
            gradientIndex = clip.gradientIndex,
            isAiCutClip = true
        )
        _clipHistory.value = listOf(record) + _clipHistory.value
        return record
    }

    fun deleteFromClipHistory(id: String) {
        _clipHistory.value = _clipHistory.value.filter { it.id != id }
    }

    fun clearClipHistory() {
        _clipHistory.value = emptyList()
    }

    fun generateSocialTextForHistory(clip: DownloadedClipRecord, platform: SocialPlatform): String {
        val cleanTitle = clip.title.substringBefore(" [")
        val hook = when (platform) {
            SocialPlatform.TIKTOK -> "Regarde absolument jusqu'à la fin 👀 Tu n'es pas prêt pour ça !"
            SocialPlatform.YOUTUBE_SHORTS -> "Abonne-toi pour ne rien rater des prochains épisodes 🔔👇"
            SocialPlatform.INSTAGRAM -> "Enregistre ce post pour t'en souvenir plus tard ✨ Dis-moi ton avis en commentaire !"
            SocialPlatform.FACEBOOK -> "Partage cette vidéo avec un ami qui doit absolument voir ça aujourd'hui 🤝"
            SocialPlatform.X_TWITTER -> "À retenir : ce passage est capital. Déroule pour comprendre 👇"
        }

        val tagWords = cleanTitle.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(" ")
            .filter { it.length > 3 }
            .take(3)
            .map { "#$it" }

        val allTags = (tagWords + platform.defaultHashtags).distinct().joinToString(" ")

        return buildString {
            append("📌 Titre : ").append(cleanTitle).append("\n\n")
            append("💬 Description :\n")
            append(hook).append("\n")
            append(clip.subtitleExcerpt).append("\n")
            append("⏱️ Timing : ").append(clip.formattedTimestamp).append(" (Score viral : ").append(clip.viralScore).append("%)\n\n")
            append("🏷️ Hashtags :\n")
            append(allTags)
        }
    }

    private fun loadInitialClipHistory(): List<DownloadedClipRecord> {
        return listOf(
            DownloadedClipRecord(
                id = "hist_1",
                title = "Ce passage clé va changer votre vision [ST: Français]",
                formattedTimestamp = "00:00 – 01:00",
                durationSeconds = 60,
                formatQuality = "1080p",
                fileSizeMb = 23.4,
                downloadDate = "Aujourd'hui à 12:45",
                sourcePlatform = "YouTube",
                viralScore = 98,
                subtitleExcerpt = "« Attendez deux secondes, regardez bien ce qui va se passer sous vos yeux... »",
                gradientIndex = 0,
                isAiCutClip = true
            ),
            DownloadedClipRecord(
                id = "hist_2",
                title = "La révélation inattendue que personne n'attendait [Traduit en English]",
                formattedTimestamp = "01:00 – 02:00",
                durationSeconds = 60,
                formatQuality = "4K",
                fileSizeMb = 58.1,
                downloadDate = "Hier à 19:20",
                sourcePlatform = "TikTok",
                viralScore = 96,
                subtitleExcerpt = "« Si vous appliquez cette seule astuce dès ce soir, tout devient 10x plus simple. »",
                gradientIndex = 1,
                isAiCutClip = true
            ),
            DownloadedClipRecord(
                id = "hist_3",
                title = "Le secret dévoilé par le créateur [ST: Français]",
                formattedTimestamp = "02:00 – 03:00",
                durationSeconds = 60,
                formatQuality = "1080p",
                fileSizeMb = 24.2,
                downloadDate = "Hier à 14:10",
                sourcePlatform = "Instagram Reels",
                viralScore = 94,
                subtitleExcerpt = "« Beaucoup pensent que c'est une question de chance, mais la réalité est différente. »",
                gradientIndex = 2,
                isAiCutClip = true
            ),
            DownloadedClipRecord(
                id = "hist_4",
                title = "Le moment le plus drôle du direct [ST: Français]",
                formattedTimestamp = "04:00 – 05:00",
                durationSeconds = 60,
                formatQuality = "720p",
                fileSizeMb = 14.8,
                downloadDate = "Il y a 3 jours",
                sourcePlatform = "YouTube Shorts",
                viralScore = 92,
                subtitleExcerpt = "« Le résultat a complètement dépassé toutes les espérances des experts. »",
                gradientIndex = 4,
                isAiCutClip = true
            )
        )
    }
}
