package com.soren.airpodscompanion

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

class AirPodsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, it, manager) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        update(context, appWidgetId, manager)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val preferences = WidgetPreferences(context)
        appWidgetIds.forEach(preferences::remove)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, AirPodsWidget::class.java))
                .forEach { update(context, it, manager) }
        }

        fun update(
            context: Context,
            widgetId: Int,
            manager: AppWidgetManager = AppWidgetManager.getInstance(context)
        ) {
            manager.updateAppWidget(widgetId, views(context, widgetId, manager))
        }

        private fun views(
            context: Context,
            widgetId: Int,
            manager: AppWidgetManager
        ): RemoteViews {
            val snapshot = DeviceSnapshotStore(context).load()
            val permissionsGranted = hasBluetoothPermission(context)
            val bluetoothEnabled = if (permissionsGranted) {
                runCatching {
                    context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
                }.getOrDefault(false)
            } else false
            val presentation = WidgetPreferences(context).load(widgetId)
            val minHeight = manager.getAppWidgetOptions(widgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 72)
            val detailed = presentation == WidgetPresentation.DETAILED ||
                (presentation == WidgetPresentation.AUTOMATIC && minHeight >= 110)
            val canReconnect = permissionsGranted && bluetoothEnabled &&
                !snapshot.connected && snapshot.address != null

            return RemoteViews(context.packageName, R.layout.airpods_widget).apply {
                setTextViewText(R.id.widget_title, snapshot.name ?: "AirPods")
                setTextViewText(
                    R.id.widget_status,
                    when {
                        !permissionsGranted -> "Permiso de dispositivos cercanos pendiente"
                        !bluetoothEnabled -> "Bluetooth apagado"
                        snapshot.reconnecting -> "Reconectando…"
                        snapshot.connected && snapshot.batteryFresh ->
                            snapshot.batteryPercent?.let { "Conectados · $it%" } ?: "Conectados"
                        snapshot.connected && snapshot.batteryPercent != null ->
                            "Conectados · última lectura ${snapshot.batteryPercent}%"
                        snapshot.connected -> "Conectados"
                        snapshot.address != null -> "Sin conexión"
                        else -> "Sin AirPods reconocidos"
                    }
                )

                val components = listOfNotNull(
                    snapshot.leftBatteryPercent?.let { "I $it%" },
                    snapshot.rightBatteryPercent?.let { "D $it%" },
                    snapshot.caseBatteryPercent?.let { "Estuche $it%" }
                ).joinToString("  ·  ")
                val showComponents = detailed && components.isNotEmpty()
                setViewVisibility(R.id.widget_components, if (showComponents) View.VISIBLE else View.GONE)
                if (showComponents) setTextViewText(R.id.widget_components, components)

                setViewVisibility(
                    R.id.widget_action,
                    if (canReconnect && !snapshot.reconnecting) View.VISIBLE else View.GONE
                )
                if (canReconnect) {
                    setOnClickPendingIntent(
                        R.id.widget_action,
                        PendingIntent.getBroadcast(
                            context,
                            widgetId,
                            Intent(context, NotificationActionReceiver::class.java)
                                .setAction(NotificationActionReceiver.ACTION_RECONNECT)
                                .putExtra(NotificationActionReceiver.EXTRA_ADDRESS, snapshot.address),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }

                setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context,
                        widgetId,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        }

        private fun hasBluetoothPermission(context: Context): Boolean {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Manifest.permission.BLUETOOTH_CONNECT
            } else {
                Manifest.permission.BLUETOOTH
            }
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
