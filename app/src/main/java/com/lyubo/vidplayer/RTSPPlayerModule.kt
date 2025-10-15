package com.lyubo.vidplayer

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer

@Module
@InstallIn(ViewModelComponent::class)
object RTSPPlayerModule {
    @Provides
    @ViewModelScoped
    fun provideLibVLC(app: Application): LibVLC {
        val options = arrayListOf(
            "--rtsp-tcp",
            "--rtsp-caching=3000",
            "--network-caching=3000",
            "--vout=android-display",
            "--sout-keep"

        )
        return LibVLC(app, options)
    }

    @Provides
    @ViewModelScoped
    fun provideMediaPlayer(libVLC: LibVLC): MediaPlayer {
        return MediaPlayer(libVLC)
    }
}