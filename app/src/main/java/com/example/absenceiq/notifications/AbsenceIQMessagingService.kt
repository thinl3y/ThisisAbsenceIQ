package com.example.absenceiq.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.absenceiq.MainActivity
import com.example.absenceiq.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AbsenceIQMessagingService :
    FirebaseMessagingService() {

    override fun onNewToken(
        token: String
    ) {
        super.onNewToken(token)

        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {
        super.onMessageReceived(remoteMessage)

        val title =
            remoteMessage.notification?.title
                ?: remoteMessage.data["title"]
                ?: "AbsenceIQ"

        val message =
            remoteMessage.notification?.body
                ?: remoteMessage.data["message"]
                ?: "You have a new update."

        showNotification(
            title = title,
            message = message
        )
    }

    private fun saveTokenToFirestore(
        token: String
    ) {

        val uid =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid
                ?: return

        FirebaseFirestore
            .getInstance()
            .collection("users")
            .document(uid)
            .update(
                "fcmToken",
                token
            )
    }

    private fun showNotification(
        title: String,
        message: String
    ) {

        val channelId =
            "absenceiq_leave_updates"

        val notificationManager =
            getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    channelId,
                    "Leave Updates",
                    NotificationManager
                        .IMPORTANCE_HIGH
                ).apply {

                    description =
                        "Notifications about leave applications"
                }

            notificationManager
                .createNotificationChannel(
                    channel
                )
        }

        val intent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or
                        PendingIntent.FLAG_UPDATE_CURRENT
            )

        val notification =
            NotificationCompat
                .Builder(
                    this,
                    channelId
                )
                .setSmallIcon(
                    R.drawable.ic_launcher_foreground
                )
                .setContentTitle(
                    title
                )
                .setContentText(
                    message
                )
                .setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .bigText(message)
                )
                .setPriority(
                    NotificationCompat
                        .PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .setContentIntent(
                    pendingIntent
                )
                .build()

        notificationManager.notify(
            System.currentTimeMillis()
                .toInt(),
            notification
        )
    }
}