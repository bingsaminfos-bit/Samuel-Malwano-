package com.example.model

enum class VideoFormat(val label: String, val ratioLabel: String) {
    VERTICAL_9_16("9:16 Vertical", "Shorts, TikTok, Reels"),
    SQUARE_1_1("1:1 Carré", "Instagram, Post"),
    LANDSCAPE_16_9("16:9 Horizontal", "YouTube, Web")
}

enum class VideoQuality(val label: String, val resolution: String) {
    P480("480p", "SD Standard"),
    P720("720p", "HD"),
    P1080("1080p", "Full HD (Recommandé)"),
    P1440("1440p", "2K QHD"),
    P4K("4K", "Ultra HD")
}

enum class SocialPlatform(val displayName: String, val defaultHashtags: List<String>) {
    TIKTOK("TikTok", listOf("#fyp", "#pourtoi", "#viral", "#foryou", "#trend")),
    YOUTUBE_SHORTS("YouTube Shorts", listOf("#shorts", "#viral", "#trending", "#subscribers")),
    INSTAGRAM("Instagram", listOf("#reels", "#explore", "#viral", "#instareels", "#trend")),
    FACEBOOK("Facebook", listOf("#video", "#viral", "#reelsfb", "#partage")),
    X_TWITTER("X (Twitter)", listOf("#viral", "#trending", "#thread", "#media"))
}

data class VideoClip(
    val id: String,
    val title: String,
    val startSeconds: Int,
    val endSeconds: Int,
    val viralScore: Int, // e.g. 97%
    val gradientIndex: Int,
    val summary: String,
    val subtitleExcerpt: String,
    val quality: VideoQuality = VideoQuality.P1080,
    val subtitleLanguage: String = "Français",
    val keyPoints: List<String> = emptyList()
) {
    val formattedTimestamp: String
        get() {
            val startMin = startSeconds / 60
            val startSec = startSeconds % 60
            val endMin = endSeconds / 60
            val endSec = endSeconds % 60
            return String.format("%02d:%02d – %02d:%02d", startMin, startSec, endMin, endSec)
        }

    val durationSeconds: Int
        get() = (endSeconds - startSeconds).coerceAtLeast(1)

    val durationMinutesApprox: Double
        get() = Math.round((durationSeconds / 60.0) * 10.0) / 10.0
}

data class DescriptionTemplate(
    val id: String,
    val name: String,
    val category: String,
    val iconName: String,
    val templatePattern: String
)

data class VideoSettings(
    val url: String = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    val format: VideoFormat = VideoFormat.VERTICAL_9_16,
    val clipDurationSeconds: Int = 60, // 30s to 900s
    val quality: VideoQuality = VideoQuality.P1080,
    val subtitleLanguage: String = "Français",
    val targetTranslationLanguage: String = "English",
    val isTranslated: Boolean = false
)

data class UserAccount(
    val id: String,
    val name: String,
    val identifier: String, // email or phone
    val isGoogle: Boolean,
    val isSubscribed: Boolean,
    val subscriptionPlan: String? = null, // "Mensuel (1 mois)" or "Annuel (1 an)"
    val subscriptionEndDate: String? = null,
    val isAdmin: Boolean = false
)

data class AdminSettings(
    val adminPasswordHash: String = "@Bingsam01",
    val monthlyPrice: Double = 5.0,
    val yearlyPrice: Double = 50.0,
    val currency: String = "$", // "$" or "€"
    val availableBalance: Double = 345.0,
    val totalRevenue: Double = 490.0,
    val totalClipsCut: Int = 1840,
    val totalUsers: Int = 74
)

data class SupportTicket(
    val id: String,
    val title: String,
    val category: String, // "Technique", "Abonnement", "Qualité", "Serveur"
    val reportedAt: String,
    val status: String, // "En attente", "En cours", "Résolu"
    val description: String,
    val resolutionNote: String = ""
)

data class PayoutRecord(
    val id: String,
    val amount: Double,
    val currency: String,
    val method: String, // "Stripe Connect", "Virement Bancaire (IBAN)", "PayPal", "Mobile Money"
    val destination: String,
    val date: String,
    val status: String
)

data class DownloadedVideoItem(
    val id: String,
    val title: String,
    val sourceUrl: String,
    val sourcePlatform: String,
    val formatQuality: String, // "1080p Full HD", "720p HD", "4K Ultra HD", "MP3 Audio"
    val fileSizeMb: Double,
    val downloadDate: String,
    val durationText: String,
    val noWatermark: Boolean = true
)

data class DownloadedClipRecord(
    val id: String,
    val title: String,
    val formattedTimestamp: String,
    val durationSeconds: Int,
    val formatQuality: String,
    val fileSizeMb: Double,
    val downloadDate: String,
    val sourcePlatform: String,
    val viralScore: Int,
    val subtitleExcerpt: String,
    val gradientIndex: Int,
    val isAiCutClip: Boolean = true
)
