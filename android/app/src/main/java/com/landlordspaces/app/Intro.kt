package com.landlordspaces.app

import android.app.Activity
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

/** Remembers that the opening film has already played in this process, so rotating the phone or returning from recents never replays it. */
object Launch {
    var played = false
    /** True once the person has signed in during this run of the app. */
    var signedIn = false
    /** A short message for the sign-in screen, such as "Your session has ended". */
    var notice = ""
}

/** People who turned animations off in Android's settings get no film. */
fun reducedMotion(context: android.content.Context): Boolean = try {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
} catch (e: Exception) { false }

private const val VIDEO_W = 1080f
private const val VIDEO_H = 1920f

/**
 * The cinematic opening, played full screen from a bundled file (no internet). The app underneath is already
 * loading while it plays; when it ends the film fades away into the app. Tap to skip.
 */
@Composable
fun IntroFilm(onFinished: () -> Unit) {
    val context = LocalContext.current
    var started by remember { mutableStateOf(false) }
    var ending by remember { mutableStateOf(false) }
    var canSkip by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (ending) 0f else 1f, tween(450), label = "introFade", finishedListener = { if (ending) onFinished() })

    // Immersive full screen while the film plays.
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    // Safety nets: a film that cannot start in 3 seconds, or runs past 9, never holds the app back.
    LaunchedEffect(Unit) { delay(3000); if (!started) ending = true }
    LaunchedEffect(Unit) { delay(1500); canSkip = true }
    LaunchedEffect(Unit) { delay(9000); ending = true }

    Box(
        Modifier.fillMaxSize().alpha(alpha).background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = canSkip && !ending) { ending = true },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val maxWpx = with(density) { maxWidth.toPx() }
            val maxHpx = with(density) { maxHeight.toPx() }
            val scale = minOf(maxWpx / VIDEO_W, maxHpx / VIDEO_H)
            val wDp = with(density) { (VIDEO_W * scale).toDp() }
            val hDp = with(density) { (VIDEO_H * scale).toDp() }
            AndroidView(
                modifier = Modifier.align(Alignment.Center).size(wDp, hDp),
                factory = { ctx ->
                    var player: MediaPlayer? = null
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                try {
                                    val mp = MediaPlayer()
                                    player = mp
                                    mp.setSurface(Surface(st))
                                    mp.setDataSource(ctx, Uri.parse("android.resource://" + ctx.packageName + "/" + R.raw.startup))
                                    mp.setVolume(0f, 0f)
                                    mp.isLooping = false
                                    mp.setOnPreparedListener { started = true; it.start() }
                                    mp.setOnCompletionListener { ending = true }
                                    mp.setOnErrorListener { _, _, _ -> ending = true; true }
                                    mp.prepareAsync()
                                } catch (e: Exception) {
                                    ending = true
                                }
                            }
                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                try { player?.release() } catch (e: Exception) { }
                                player = null
                                return true
                            }
                        }
                    }
                },
            )
        }
        if (canSkip && !ending) Text("Tap to skip", color = Color(0x66FFFFFF), fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp))
    }
}
