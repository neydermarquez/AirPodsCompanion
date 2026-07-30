package com.soren.airpodscompanion

import android.annotation.SuppressLint
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import java.util.regex.Pattern

class CompanionCoordinator(private val context: Context) {
    fun supported(): Boolean =
        Build.VERSION.SDK_INT >= 26 &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP) &&
            context.getSystemService(CompanionDeviceManager::class.java) != null

    fun associated(): Boolean {
        if (Build.VERSION.SDK_INT < 26 || !supported()) return false
        return associatedApi26()
    }

    @RequiresApi(26)
    @Suppress("DEPRECATION")
    private fun associatedApi26(): Boolean {
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        return if (Build.VERSION.SDK_INT >= 33) {
            manager.myAssociations.any { it.displayName?.contains("airpods", true) == true }
        } else {
            manager.associations.isNotEmpty()
        }
    }

    fun requestAssociation(onChooser: (IntentSender) -> Unit, onFailure: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < 26 || !supported()) {
            return onFailure("Este teléfono no admite dispositivos complementarios.")
        }
        requestAssociationApi26(onChooser, onFailure)
    }

    @RequiresApi(26)
    @Suppress("DEPRECATION")
    private fun requestAssociationApi26(onChooser: (IntentSender) -> Unit, onFailure: (String) -> Unit) {
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        val filter = BluetoothDeviceFilter.Builder()
            .setNamePattern(Pattern.compile(".*AirPods.*", Pattern.CASE_INSENSITIVE))
            .build()
        val request = AssociationRequest.Builder()
            .addDeviceFilter(filter)
            .setSingleDevice(false)
            .build()
        manager.associate(
            request,
            object : CompanionDeviceManager.Callback() {
                override fun onDeviceFound(chooserLauncher: IntentSender) = onChooser(chooserLauncher)
                override fun onFailure(error: CharSequence?) = onFailure(error?.toString() ?: "Asociación cancelada")
            },
            Handler(Looper.getMainLooper())
        )
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun startPresenceObservation(): Boolean {
        if (Build.VERSION.SDK_INT < 31 || !associated()) return false
        return startPresenceObservationApi31()
    }

    @RequiresApi(31)
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun startPresenceObservationApi31(): Boolean {
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        return runCatching {
            val addresses = if (Build.VERSION.SDK_INT >= 33) {
                manager.myAssociations.mapNotNull { it.deviceMacAddress?.toString() }
            } else {
                manager.associations
            }
            addresses.forEach { manager.startObservingDevicePresence(it) }
            addresses.isNotEmpty()
        }.getOrDefault(false)
    }
}
