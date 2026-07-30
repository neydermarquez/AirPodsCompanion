package com.soren.airpodscompanion

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

class MediaControls(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    fun playPause() = send(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun previous() = send(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    fun next() = send(KeyEvent.KEYCODE_MEDIA_NEXT)

    fun volumeUp() {
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun volumeDown() {
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun setMusicVolumePercent(percent: Int): Boolean {
        val manager = audioManager ?: return false
        val maximum = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maximum <= 0) return false
        val target = (maximum * percent.coerceIn(0, 100) / 100f).toInt().coerceIn(0, maximum)
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return true
    }

    private fun send(keyCode: Int) {
        val manager = audioManager ?: return
        manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
