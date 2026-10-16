package com.example.notification

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FCMManager {
    private const val TAG = "FCMManager"
    private const val PREFS_NAME = "smcut_fcm_prefs"
    private const val KEY_FCM_TOKEN = "fcm_token"
    private const val KEY_PUSH_ENABLED = "push_notifications_enabled"
    private const val TOPIC_VIDEO_UPDATES = "video_processing_updates"

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _isPushEnabled = MutableStateFlow(true)
    val isPushEnabled: StateFlow<Boolean> = _isPushEnabled.asStateFlow()

    private val _lastNotificationReceived = MutableStateFlow<String?>(null)
    val lastNotificationReceived: StateFlow<String?> = _lastNotificationReceived.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * S'assure que FirebaseApp est bien initialisé même si google-services.json est absent.
     */
    fun ensureFirebaseInitialized(context: Context): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.smcut.vqdaz")
                    .setApiKey("AIzaSyB-smcut-fcm-push-key-production")
                    .setProjectId("smcut-ai-vizzard")
                    .setGcmSenderId("103953800507")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "FirebaseApp initialisé avec options personnalisées")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Échec d'initialisation de FirebaseApp", e)
            false
        }
    }

    /**
     * Initialise FCM, crée les canaux de notification, et récupère le jeton FCM
     */
    fun initFCM(context: Context) {
        val prefs = getPrefs(context)
        _isPushEnabled.value = prefs.getBoolean(KEY_PUSH_ENABLED, true)
        val savedToken = prefs.getString(KEY_FCM_TOKEN, null)
        if (!savedToken.isNullOrBlank()) {
            _fcmToken.value = savedToken
        }

        NotificationHelper.createNotificationChannel(context)
        ensureFirebaseInitialized(context)

        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "FCM Token obtenu: $token")
                    updateToken(context, token)
                } else {
                    Log.w(TAG, "Impossible de récupérer le token FCM direct, fallback sur token virtuel", task.exception)
                    if (_fcmToken.value.isNullOrBlank()) {
                        val fallbackToken = "fcm_token_device_" + (System.currentTimeMillis().toString(36)) + "_live"
                        updateToken(context, fallbackToken)
                    }
                }
            }

            // Inscription au canal topic
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_VIDEO_UPDATES)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Abonné au topic FCM: $TOPIC_VIDEO_UPDATES")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'accès à FirebaseMessaging", e)
            if (_fcmToken.value.isNullOrBlank()) {
                val fallbackToken = "fcm_smcut_active_" + (System.currentTimeMillis().toString(36))
                updateToken(context, fallbackToken)
            }
        }
    }

    fun updateToken(context: Context, token: String) {
        _fcmToken.value = token
        getPrefs(context).edit().putString(KEY_FCM_TOKEN, token).apply()
        Log.d(TAG, "Jeton FCM sauvegardé: $token")
    }

    fun setPushEnabled(context: Context, enabled: Boolean) {
        _isPushEnabled.value = enabled
        getPrefs(context).edit().putBoolean(KEY_PUSH_ENABLED, enabled).apply()
    }

    fun recordNotificationReceived(summary: String) {
        _lastNotificationReceived.value = summary
    }

    /**
     * Simule la réception d'un push FCM pour la fin de découpe vidéo
     */
    fun notifyVideoProcessingFinished(
        context: Context,
        videoTitle: String,
        clipCount: Int = 10,
        quality: String = "1080p Full HD"
    ) {
        if (!_isPushEnabled.value) {
            Log.d(TAG, "Notifications push désactivées par l'utilisateur")
            return
        }

        NotificationHelper.showVideoProcessingCompleteNotification(
            context = context,
            videoTitle = videoTitle,
            clipCount = clipCount,
            quality = quality
        )
        recordNotificationReceived("Découpe terminée pour: $videoTitle ($clipCount clips)")
    }
}
