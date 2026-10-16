package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testVideoClipTimestampAndDuration() {
        val clip = VideoClip(
            id = "c1",
            title = "Test Clip",
            startSeconds = 60,
            endSeconds = 120,
            viralScore = 96,
            gradientIndex = 0,
            summary = "Summary test",
            subtitleExcerpt = "Excerpt test",
            quality = VideoQuality.P1080,
            subtitleLanguage = "Français"
        )

        assertEquals("01:00 – 02:00", clip.formattedTimestamp)
        assertEquals(60, clip.durationSeconds)
        assertEquals(1.0, clip.durationMinutesApprox, 0.01)
        assertEquals(VideoQuality.P1080, clip.quality)
        assertEquals("Français", clip.subtitleLanguage)
    }

    @Test
    fun testSocialPlatformsPresence() {
        val platforms = SocialPlatform.values()
        assertEquals(5, platforms.size)
        assertTrue(platforms.any { it == SocialPlatform.TIKTOK })
        assertTrue(platforms.any { it == SocialPlatform.YOUTUBE_SHORTS })
        assertTrue(platforms.any { it == SocialPlatform.INSTAGRAM })
        assertTrue(platforms.any { it == SocialPlatform.FACEBOOK })
        assertTrue(platforms.any { it == SocialPlatform.X_TWITTER })
    }

    @Test
    fun testVideoSettingsDefaults() {
        val settings = VideoSettings()
        assertEquals(VideoFormat.VERTICAL_9_16, settings.format)
        assertEquals(VideoQuality.P1080, settings.quality)
        assertEquals(60, settings.clipDurationSeconds)
        assertEquals("Français", settings.subtitleLanguage)
    }

    @Test
    fun testAdminSettingsDefaults() {
        val admin = AdminSettings()
        assertEquals("@Bingsam01", admin.adminPasswordHash)
        assertEquals(5.0, admin.monthlyPrice, 0.001)
        assertEquals(50.0, admin.yearlyPrice, 0.001)
        assertEquals("$", admin.currency)
    }

    @Test
    fun testDescriptionTemplatePlaceholdersReplacement() {
        val tmpl = DescriptionTemplate(
            id = "test_tmpl",
            name = "Test Model",
            category = "Test",
            iconName = "Bolt",
            templatePattern = "Titre: {TITRE}\nDurée: {DUREE}\nTags: {HASHTAGS}"
        )

        val clip = VideoClip(
            id = "c2",
            title = "Secret Révélé [ST: Français]",
            startSeconds = 0,
            endSeconds = 45,
            viralScore = 95,
            gradientIndex = 0,
            summary = "Summary",
            subtitleExcerpt = "Excerpt"
        )

        val cleanTitle = clip.title.substringBefore(" [")
        val rendered = tmpl.templatePattern
            .replace("{TITRE}", cleanTitle)
            .replace("{DUREE}", "${clip.durationSeconds}s")
            .replace("{HASHTAGS}", "#fyp #viral")

        assertTrue(rendered.contains("Titre: Secret Révélé"))
        assertTrue(rendered.contains("Durée: 45s"))
        assertTrue(rendered.contains("Tags: #fyp #viral"))
    }

    @Test
    fun testClipsFilteringByQualityAndDuration() {
        val clips = listOf(
            VideoClip("1", "Short Clip", 0, 30, 95, 0, "", "", VideoQuality.P480, "Français"),
            VideoClip("2", "Medium Clip", 0, 90, 95, 0, "", "", VideoQuality.P1080, "English"),
            VideoClip("3", "Long Clip 4K", 0, 360, 95, 0, "", "", VideoQuality.P4K, "Français")
        )

        // Filter by duration < 60s
        val shortClips = clips.filter { it.durationSeconds < 60 }
        assertEquals(1, shortClips.size)
        assertEquals("Short Clip", shortClips.first().title)

        // Filter by 4K quality
        val fourKClips = clips.filter { it.quality == VideoQuality.P4K }
        assertEquals(1, fourKClips.size)
        assertEquals("Long Clip 4K", fourKClips.first().title)

        // Filter by English subtitle language
        val englishClips = clips.filter { it.subtitleLanguage == "English" }
        assertEquals(1, englishClips.size)
        assertEquals("Medium Clip", englishClips.first().title)
    }

    @Test
    fun testDownloadedVideoItemModel() {
        val item = DownloadedVideoItem(
            id = "d1",
            title = "TikTok Dance HD",
            sourceUrl = "https://www.tiktok.com/@user/video/123",
            sourcePlatform = "TikTok",
            formatQuality = "1080p Full HD",
            fileSizeMb = 15.4,
            downloadDate = "11/10/2026",
            durationText = "00:45",
            noWatermark = true
        )

        assertEquals("TikTok", item.sourcePlatform)
        assertEquals("1080p Full HD", item.formatQuality)
        assertTrue(item.noWatermark)
        assertEquals(15.4, item.fileSizeMb, 0.01)
    }

    @Test
    fun testDownloadedClipRecordAndHistoryFiltering() {
        val historyItem = DownloadedClipRecord(
            id = "h1",
            title = "Extrait Podcast Viral",
            formattedTimestamp = "01:00 – 02:00",
            durationSeconds = 60,
            formatQuality = "1080p",
            fileSizeMb = 24.5,
            downloadDate = "12/10/2026 10:30",
            sourcePlatform = "YouTube",
            viralScore = 98,
            subtitleExcerpt = "« Une astuce incroyable »",
            gradientIndex = 1,
            isAiCutClip = true
        )

        assertEquals("h1", historyItem.id)
        assertEquals(98, historyItem.viralScore)
        assertEquals("YouTube", historyItem.sourcePlatform)
        assertEquals(60, historyItem.durationSeconds)

        val list = listOf(
            historyItem,
            historyItem.copy(id = "h2", sourcePlatform = "TikTok", formatQuality = "4K")
        )

        val ytOnly = list.filter { it.sourcePlatform == "YouTube" }
        assertEquals(1, ytOnly.size)

        val fourKOnly = list.filter { it.formatQuality == "4K" }
        assertEquals(1, fourKOnly.size)
    }
}
