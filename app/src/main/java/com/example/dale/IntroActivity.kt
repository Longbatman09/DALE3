package com.example.dale

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.dale.ui.theme.DALETheme
import com.example.dale.utils.SharedPreferencesManager
import kotlinx.coroutines.delay
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.view.LayoutInflater
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@UnstableApi
class IntroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SharedPreferencesManager.getInstance(this)

        val currentTime = System.currentTimeMillis()
        val lastSplashTime = prefs.getLastSplashVideoTime()
        val cooldownMillis = 30 * 60 * 1000L // 30 minutes

        if (lastSplashTime > 0 && (currentTime - lastSplashTime) in 0 until cooldownMillis) {
            val destination = if (prefs.isSetupCompleted()) {
                MainActivity::class.java
            } else {
                WelcomeActivity::class.java
            }
            startActivity(Intent(this, destination))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
            return
        }

        prefs.setLastSplashVideoTime(currentTime)

        setContent {
            DALETheme {
                IntroVideoScreen(
                    onFinished = {
                        val destination = if (prefs.isSetupCompleted()) {
                            MainActivity::class.java
                        } else {
                            // Show intro/welcome screen first, then user can choose to start setup.
                            WelcomeActivity::class.java
                        }
                        startActivity(Intent(this, destination))
                        @Suppress("DEPRECATION")
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
@UnstableApi
private fun IntroVideoScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var finishTriggered by remember { mutableStateOf(false) }

    // Main player for app entry animation
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = "android.resource://${context.packageName}/${R.raw.app_entry_animation}".toUri()
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_OFF
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            prepare()
            playWhenReady = true
        }
    }

    // Secondary player for branding animation
    val brandingPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = "android.resource://${context.packageName}/${R.raw.app_branding_animation}".toUri()
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_OFF
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                player.playWhenReady = false
                player.pause()
                brandingPlayer.playWhenReady = false
                brandingPlayer.pause()
            }

            override fun onResume(owner: LifecycleOwner) {
                player.playWhenReady = true
                player.play()
                brandingPlayer.playWhenReady = true
                brandingPlayer.play()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
            brandingPlayer.release()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && !finishTriggered) {
                    finishTriggered = true
                    onFinished()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (!finishTriggered) {
                    finishTriggered = true
                    onFinished()
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    DisposableEffect(brandingPlayer) {
        val brandingListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (!finishTriggered) {
                    finishTriggered = true
                    onFinished()
                }
            }
        }
        brandingPlayer.addListener(brandingListener)
        onDispose { brandingPlayer.removeListener(brandingListener) }
    }

    // Fallback so app is never stuck on intro if a device fails to report end-of-playback.
    LaunchedEffect(Unit) {
        delay(15_000)
        if (!finishTriggered) {
            finishTriggered = true
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.dale_system_bar)),
        contentAlignment = Alignment.Center
    ) {
        // Main video view
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val playerView = LayoutInflater.from(it)
                    .inflate(R.layout.intro_player_view, null) as PlayerView
                playerView.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                playerView.setKeepContentOnPlayerReset(true)
                playerView.player = player
                playerView
            },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
            }
        )

        // Branding video in bottom right corner at 20% size
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .offset(y = (-40).dp)
                .size(90.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    val playerView = LayoutInflater.from(it)
                        .inflate(R.layout.intro_player_view, null) as PlayerView
                    playerView.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    playerView.setKeepContentOnPlayerReset(true)
                    playerView.player = brandingPlayer
                    playerView
                },
                update = { view ->
                    if (view.player !== brandingPlayer) {
                        view.player = brandingPlayer
                    }
                }
            )
        }
    }
}
