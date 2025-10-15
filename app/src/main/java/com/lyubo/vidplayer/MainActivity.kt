package com.lyubo.vidplayer

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp

import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Slider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lyubo.vidplayer.ui.theme.VidPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import org.videolan.libvlc.util.VLCVideoLayout

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            VidPlayerTheme {
                val viewModel = hiltViewModel<RTSPViewModel>()
                var areControlsVisible by rememberSaveable { mutableStateOf(true) }
                val activity = LocalActivity.current as ComponentActivity
                var isFullscreen by rememberSaveable {mutableStateOf(false)}
                val isBuffering by rememberSaveable { mutableStateOf(viewModel.isBuffering()) }
                LaunchedEffect(isFullscreen) {
                    val window = activity.window
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)

                    if(isFullscreen){
                        insetsController.hide(WindowInsetsCompat.Type.systemBars())
                        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }else{
                        insetsController.show(WindowInsetsCompat.Type.systemBars())
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
                LaunchedEffect(areControlsVisible) {
                    if(areControlsVisible){
                        delay(10000L)
                        areControlsVisible = false
                    }
                }
                val playerModifier = if(isFullscreen){
                    Modifier
                        .fillMaxSize()
                        .padding(top = 0.dp, start = 0.dp, end = 0.dp)

                }else{
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp, start = 25.dp, end = 25.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .aspectRatio(16 / 9f)

                }

                Column(modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                        verticalArrangement = Arrangement.spacedBy(
                            space = 8.dp,
                            alignment = Alignment.Top
                        )

                ) {
                    Box(modifier = playerModifier
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                areControlsVisible = !areControlsVisible
                            }
                            )
                        }
                    ){
                        VideoPlayer(viewModel, activity)
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        this@Column.AnimatedVisibility(
                            visible = areControlsVisible,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            ControlsRow(
                                viewModel = viewModel,
                                isFullscreen = isFullscreen,
                                onFullScreenClick = {
                                    isFullscreen = !isFullscreen
                                    areControlsVisible = true
                                },
                                activity = activity
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(viewModel: RTSPViewModel, activity: ComponentActivity){
    var lifecycle by remember {
        mutableStateOf(Lifecycle.Event.ON_CREATE)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver{ _, event ->
            lifecycle = event
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose{
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    AndroidView(
        factory = { context ->
            VLCVideoLayout(context).also { layout ->
                layout.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                viewModel.attachVideoLayout(layout)
            }
        }, update = { layout ->
            when (lifecycle) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.pause(activity.window)
                    viewModel.updateViews()
                }

                Lifecycle.Event.ON_RESUME -> {
                    viewModel.updateViews()
                    viewModel.play(activity.window)
                }

                else -> Unit
            }
        }, onRelease = {
            viewModel.detachVideoLayout()
        }, modifier = Modifier
            .fillMaxSize()
    )
}

@Composable
fun ControlsRow(
    viewModel: RTSPViewModel,
    isFullscreen: Boolean,
    onFullScreenClick: () -> Unit,
    activity: ComponentActivity
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ){
        var showVolumeSlider by remember { mutableStateOf(false) }
        var sliderPosition by rememberSaveable {mutableStateOf(viewModel.getVolume())}
        IconButton(onClick = { showVolumeSlider = !showVolumeSlider }) {
            Icon(
                // We can dynamically change the icon based on volume later
                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Volume",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        AnimatedVisibility(
            visible = showVolumeSlider,
            modifier = Modifier.weight(1f) // Use weight to fill space
        ) {
            Slider(
                modifier = Modifier.padding(horizontal = 8.dp),
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                    viewModel.setVolume(it.toInt())
                },
                valueRange = 0f..100f
            )
        }
        IconButton(onClick = {
            if(viewModel.isPlaying()){
                viewModel.pause(activity.window)
            }else{
                viewModel.play(activity.window)
            }
        }) {
            Icon(
                imageVector = if (viewModel.isPlaying()) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (viewModel.isPlaying()) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onFullScreenClick) {
            Icon(
                imageVector = if(isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                contentDescription = "Fullscreen",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}




