package com.soren.airpodscompanion

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppErrorCenter {
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun report(message: String) {
        _message.value = message
    }

    fun clear() {
        _message.value = null
    }
}
