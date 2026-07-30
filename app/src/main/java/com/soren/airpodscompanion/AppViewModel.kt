package com.soren.airpodscompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(application: Application, private val savedState: SavedStateHandle) :
    AndroidViewModel(application) {
    private val profiles = ListeningProfileStore(application)
    private val retention = HistoryRetentionPreferences(application)
    private val notifications = NotificationPreferences(application)

    private val _selectedProfile = MutableStateFlow(profiles.load())
    val selectedProfile = _selectedProfile.asStateFlow()
    private val _profileSettings = MutableStateFlow(profiles.settings(_selectedProfile.value))
    val profileSettings = _profileSettings.asStateFlow()
    private val _retentionSettings = MutableStateFlow(retention.load())
    val retentionSettings = _retentionSettings.asStateFlow()
    private val _notificationSettings = MutableStateFlow(notifications.load())
    val notificationSettings = _notificationSettings.asStateFlow()
    private val _error = MutableStateFlow<String?>(savedState["app_error"])
    val error = _error.asStateFlow()

    fun selectProfile(profile: ListeningProfile) {
        profiles.save(profile)
        _selectedProfile.value = profile
        _profileSettings.value = profiles.settings(profile)
    }

    fun updateProfile(settings: ListeningProfileSettings) {
        profiles.saveSettings(_selectedProfile.value, settings)
        _profileSettings.value = settings
    }

    fun updateNotifications(settings: NotificationSettings) {
        notifications.save(settings)
        _notificationSettings.value = settings
    }

    fun updateRetention(settings: HistoryRetentionSettings) {
        retention.save(settings)
        _retentionSettings.value = settings
    }

    fun reportError(message: String?) {
        savedState["app_error"] = message
        _error.value = message
    }

    fun reloadAfterDataDeletion() {
        _selectedProfile.value = profiles.load()
        _profileSettings.value = profiles.settings(_selectedProfile.value)
        _retentionSettings.value = retention.load()
        _notificationSettings.value = notifications.load()
        reportError(null)
    }
}
