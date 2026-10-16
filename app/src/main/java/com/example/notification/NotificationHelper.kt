package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "smcut_video_channel"
    const val CHANNEL_NAME = "Traitement des vidéos SMCut"
    const val CHANNEL_DESC = "Notifications push lorsque la découpe et le traitement de vos vidéos sont terminés"
    private const val NOTIFICATION_ID_BASE = 4001

    /**
     * Crée le canal de notification avec haute importance, son et vibration pour Android 8.0+
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                enableLights(true)
                lightColor = 0xFF6366F1.toInt() // Indigo / Violet
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
            Log.d("NotificationHelper", "Canal de notification créé: $CHANNEL_ID")
        }
    }

    /**
     * Vérifie si les notifications sont activées pour l'application
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED && NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Déclenche une notification push de fin de découpe vidéo
     */
    fun showVideoProcessingCompleteNotification(
        context: Context,
        videoTitle: String,
        clipCount: Int = 10,
        quality: String = "1080p Full HD"
    ) {
        createNotificationChannel(context)

        // PendingIntent pour ouvrir l'application directement
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TARGET", "HISTORY")
            putExtra("NOTIFICATION_SOURCE", "VIDEO_CUT_COMPLETE")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val cleanTitle = videoTitle.take(50).ifBlank { "Votre vidéo" }
        val titleText = "Découpe vidéo terminée ! 🎉"
        val bodyText = "Votre vidéo \"$cleanTitle\" a été découpée avec succès en $clipCount clips viraux ($quality)."
        val bigText = "🎉 Découpe terminée avec succès !\n\n" +
                "• Source: \"$cleanTitle\"\n" +
                "• Clips générés: $clipCount moments forts viraux\n" +
                "• Résolution: $quality\n" +
                "• Sous-titres IA & scores d'accroche inclus.\n\n" +
                "Appuyez pour prévisualiser, télécharger et repartager sur TikTok & Reels !"

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setContentIntent(pendingIntent)

        val notificationId = NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 1000).toInt()

        try {
            if (areNotificationsEnabled(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
                Log.d("NotificationHelper", "Notification envoyée id=$notificationId pour $videoTitle")
            } else {
                Log.w("NotificationHelper", "Permission notifications non accordée")
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Erreur de sécurité lors de l'envoi de la notification", e)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Erreur lors de l'envoi de la notification", e)
        }
    }

    /**
     * Déclenche une notification personnalisée (reçue depuis FCM ou test)
     */
    fun showCustomNotification(
        context: Context,
        title: String,
        body: String,
        dataPayload: Map<String, String> = emptyMap()
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            for ((k, v) in dataPayload) {
                putExtra(k, v)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 1000).toInt()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Erreur affichage notification", e)
        }
    }
}
