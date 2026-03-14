package com.example.dale

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

class IntroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SharedPreferencesManager.getInstance(this)

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
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun IntroVideoScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var finishTriggered by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.app_entry_animation}")
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
            }

            override fun onResume(owner: LifecycleOwner) {
                player.playWhenReady = true
                player.play()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
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
            .background(Color(0xFF031C3C)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val playerView = LayoutInflater.from(it)
                    .inflate(R.layout.intro_player_view, null, false) as PlayerView
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
    }
}
