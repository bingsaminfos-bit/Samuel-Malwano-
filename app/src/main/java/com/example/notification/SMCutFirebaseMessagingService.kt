package com.example.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SMCutFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nouveau jeton FCM généré: $token")
        FCMManager.updateToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message FCM reçu de: ${remoteMessage.from}")

        // 1. Données du message (data payload)
        val data = remoteMessage.data
        val type = data["type"] ?: "video_cut_finished"
        val videoTitle = data["video_title"] ?: data["title"] ?: "Vidéo traitée"
        val clipsCount = data["clips_count"]?.toIntOrNull() ?: 10
        val quality = data["quality"] ?: "1080p Full HD"

        // 2. Notification payload si présent
        val notificationTitle = remoteMessage.notification?.title
        val notificationBody = remoteMessage.notification?.body

        if (notificationTitle != null && notificationBody != null) {
            NotificationHelper.showCustomNotification(
                context = applicationContext,
                title = notificationTitle,
                body = notificationBody,
                dataPayload = data
            )
            FCMManager.recordNotificationReceived("$notificationTitle - $notificationBody")
        } else {
            // Découpe vidéo terminée reçue par data payload
            if (type == "video_cut_finished" || type == "cut_completed") {
                NotificationHelper.showVideoProcessingCompleteNotification(
                    context = applicationContext,
                    videoTitle = videoTitle,
                    clipCount = clipsCount,
                    quality = quality
                )
                FCMManager.recordNotificationReceived("Découpe terminée: $videoTitle")
            } else {
                NotificationHelper.showCustomNotification(
                    context = applicationContext,
                    title = "SMCut AI Studio",
                    body = "Nouveau message: $videoTitle",
                    dataPayload = data
                )
            }
        }
    }

    companion object {
        private const val TAG = "SMCutFCMService"
    }
}
