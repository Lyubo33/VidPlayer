package com.lyubo.vidplayer

import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import androidx.core.net.toUri
import org.videolan.libvlc.util.VLCVideoLayout

@HiltViewModel
class RTSPViewModel
@Inject constructor(
    val libVLC: LibVLC,
    val player: MediaPlayer
): ViewModel(), MediaPlayer.EventListener {
    private val VIDEO_URI = "rtsp://dev.gradotech.eu:8554/stream"
    private val _isPlaying = mutableStateOf(false)

    private fun setKeepScreenOn(keepScreenOn: Boolean, window: Window) {
        if(keepScreenOn){
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }else{
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    val isPlaying: () -> Boolean = { _isPlaying.value }

    init {
        setupPlayer()
    }
    override fun onEvent(event: MediaPlayer.Event){
        when(event.type){
            MediaPlayer.Event.EndReached -> {
                player.stop()
                player.play()
            }
            MediaPlayer.Event.Playing -> _isPlaying.value = true
            MediaPlayer.Event.Paused -> _isPlaying.value = false
            MediaPlayer.Event.Stopped -> _isPlaying.value = false

        }

    }
    fun setupPlayer(){
        val media = Media(libVLC, VIDEO_URI.toUri())
        player.media = media
        player.setEventListener(this)
    }
    fun attachVideoLayout(layout: VLCVideoLayout) {
        player.attachViews(layout, null, false, false)
    }

    fun detachVideoLayout() {
        player.detachViews()
    }

    fun pause(window:Window) {
        player.pause()
        setKeepScreenOn(false, window)
    }
    fun play(window:Window) {
        player.play()
        setKeepScreenOn(true, window)

    }

    fun updateViews(){
        player.updateVideoSurfaces()
    }
    fun stop() {
        player.stop()
    }
    fun setVolume(volume: Int) {player.volume = volume} // 0-100
    fun getVolume(): Float { return player.volume.toFloat()}
    override fun onCleared() {
        super.onCleared()
        player.setEventListener(null)
        player.stop()
        _isPlaying.value = false
        player.detachViews()
        player.media?.release()
        player.release()
    }

}