package com.soren.airpodscompanion

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object AirPodsNotifications {
    const val SERVICE_CHANNEL = "airpods_monitor"
    const val EVENTS_CHANNEL = "airpods_events"
    const val SERVICE_ID = 1001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(
            listOf(
                NotificationChannel(SERVICE_CHANNEL, "Supervisión de AirPods", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(EVENTS_CHANNEL, "Eventos de AirPods", NotificationManager.IMPORTANCE_HIGH)
            )
        )
    }

    fun monitoring(context: Context, snapshot: DeviceSnapshot): Notification =
        base(context, SERVICE_CHANNEL)
            .setContentTitle(if (snapshot.connected) snapshot.name ?: "AirPods conectados" else "Supervisión activa")
            .setContentText(
                if (snapshot.connected) {
                    if (snapshot.batteryFresh) "Conectados · ${snapshot.batteryPercent}%"
                    else snapshot.batteryPercent?.let { "Conectados · última lectura $it%" } ?: "Conectados"
                } else "Esperando una conexión"
            )
            .setOngoing(true).setOnlyAlertOnce(true).build()

    fun event(context: Context, type: AirPodsNotificationType, title: String, detail: String) {
        if (!NotificationPreferences(context).enabled(type)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val builder = base(context, EVENTS_CHANNEL).setContentTitle(title).setContentText(detail)
            .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup("airpods_events")
        if (type == AirPodsNotificationType.DISCONNECTED) {
            DeviceSnapshotStore(context).load().address?.let { address ->
                val reconnectIntent = Intent(context, NotificationActionReceiver::class.java)
                    .setAction(NotificationActionReceiver.ACTION_RECONNECT)
                    .putExtra(NotificationActionReceiver.EXTRA_ADDRESS, address)
                val reconnectPending = PendingIntent.getBroadcast(
                    context,
                    21,
                    reconnectIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_airpods_notification, "Reconectar", reconnectPending)
            }
        }
        NotificationManagerCompat.from(context).notify(
            1100,
            builder.build()
        )
    }

    private fun base(context: Context, channel: String): NotificationCompat.Builder {
        val pending = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_airpods_notification)
            .setContentIntent(pending)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }
}
