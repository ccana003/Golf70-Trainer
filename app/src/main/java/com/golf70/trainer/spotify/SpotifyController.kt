package com.golf70.trainer.spotify

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Starter abstraction for Spotify Android SDK integration.
 * In production wire this with SpotifyAppRemote connection callbacks.
 */
class SpotifyController {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun connect() {
        _connected.value = true
    }

    fun playPlaylist(_playlistUri: String = "spotify:playlist:37i9dQZF1DX6VdMW310YC7") {
        if (_connected.value) _isPlaying.value = true
    }

    fun pause() {
        _isPlaying.value = false
    }

    fun skip() {
        // Delegate to SDK's playerApi.skipNext() in full implementation.
    }
}
