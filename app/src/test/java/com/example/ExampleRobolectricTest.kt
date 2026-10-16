package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SMCutRepository
import com.example.notification.FCMManager
import com.example.notification.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SMCut", appName)
  }

  @Test
  fun `notification channel is created properly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    NotificationHelper.createNotificationChannel(context)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = nm.getNotificationChannel(NotificationHelper.CHANNEL_ID)
    assertNotNull(channel)
    assertEquals(NotificationHelper.CHANNEL_NAME, channel.name)
  }

  @Test
  fun `fcm manager notifies and tracks status`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    FCMManager.setPushEnabled(context, true)
    assertTrue(FCMManager.isPushEnabled.value)

    FCMManager.notifyVideoProcessingFinished(
      context = context,
      videoTitle = "Test Video",
      clipCount = 10,
      quality = "1080p Full HD"
    )

    assertNotNull(FCMManager.lastNotificationReceived.value)
    assertTrue(FCMManager.lastNotificationReceived.value!!.contains("Test Video"))
  }

  @Test
  fun `repository loads initial clip history`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = SMCutRepository(context)
    val history = repository.clipHistory.value
    assertTrue(history.isNotEmpty())
  }

  @Test
  fun `admin login activates unlimited free VIP status`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = SMCutRepository(context)
    val success = repository.loginAsAdmin("@Bingsam01")
    assertTrue(success)

    val admin = repository.currentUser.value
    assertNotNull(admin)
    assertTrue(admin!!.isAdmin)
    assertTrue(admin.isSubscribed)
  }

  @Test
  fun `admin can record clip download to history without payment`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = SMCutRepository(context)
    val clips = repository.clips.value
    assertTrue(clips.isNotEmpty())

    val firstClip = clips.first()
    val record = repository.recordDownloadedClip(firstClip, sourcePlatform = "Admin Studio VIP")
    assertNotNull(record)
    assertEquals(firstClip.title, record.title)
    assertTrue(repository.clipHistory.value.any { it.title == firstClip.title })
  }
}
