package com.wiva.android.ui.screens.idle

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.wiva.android.R
import com.wiva.android.ui.screens.customer.WivaElectronAssets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG = "IdleVideoOverlay"
private const val CROSSFADE_MS = 500

/**
 * За сколько мс до конца начинаем кроссфейд (.
 * Должно быть >= времени буферизации на реальном железе (~2 с).
 * Следующий ролик начинает буферизоваться в самом начале текущего — к этому моменту он готов.
 */
private const val CROSSFADE_TRIGGER_MS = 1_500L

/** Интервал опроса позиции плеера. */
private const val POLL_MS = 100L

/** Максимальное время ожидания готовности следующего плеера (запасной сценарий). */
private const val READY_WAIT_TIMEOUT_MS = 3_000L

/**
 * Полноэкранный скринсейвер из включённых видео [enabledVideoIds].
 *
 * Архитектура
 * - Два ExoPlayer (A/B ping-pong): пока A играет, B загружает следующий ролик.
 * - Кроссфейд стартует за CROSSFADE_TRIGGER_MS до конца текущего ролика,
 * только если следующий уже в STATE_READY — нет чёрного экрана даже при 2с задержке буфера.
 * - Запасной сценарий: ролик кончился, следующий не готов → ждём до READY_WAIT_TIMEOUT_MS.
 * - PlayerView в режиме TextureView: обязателен для alpha-прозрачности.
 */
@UnstableApi
@Composable
fun IdleVideoOverlay(
    enabledVideoIds: List<String>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
 // Преобразуем id → fileName; только включённые видео из IDLE_VIDEOS
    val files = remember(enabledVideoIds) {
        enabledVideoIds.map { id -> "$id.mp4" }.ifEmpty { return@remember emptyList() }
    }
    if (files.isEmpty()) return

    val playerA = remember(context) { ExoPlayer.Builder(context).build().apply { volume = 0f } }
    val playerB = remember(context) { ExoPlayer.Builder(context).build().apply { volume = 0f } }

    val alphaA = remember { Animatable(0f) }
    val alphaB = remember { Animatable(0f) }

    var activeIsA by remember { mutableStateOf(true) }
    val counter = remember { intArrayOf(0) }

    DisposableEffect(playerA, playerB) {
        onDispose {
            playerA.release()
            playerB.release()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> { playerA.pause(); playerB.pause() }
                Lifecycle.Event.ON_START -> {
                    val active = if (activeIsA) playerA else playerB
                    if (active.playbackState == Player.STATE_READY ||
                        active.playbackState == Player.STATE_BUFFERING
                    ) active.play()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    fun uri(idx: Int) = Uri.parse(
        "${WivaElectronAssets.ASSET_URI_PREFIX}/video/${files[idx % files.size]}"
    )

    fun preload(player: ExoPlayer, idx: Int) {
        val name = files[idx % files.size]
        Timber.tag(TAG).d("preload %d (%s) → %s", idx % files.size, name, if (player === playerA) "A" else "B")
        player.setMediaItem(MediaItem.fromUri(uri(idx)))
        player.prepare()
    }

 /**
 * Ждёт, пока нужно начать кроссфейд:
 * - Плановый: позиция >= длительность - CROSSFADE_TRIGGER_MS И следующий STATE_READY.
 * - Запасной: ролик закончился → ждём готовности до READY_WAIT_TIMEOUT_MS, потом всё равно переходим.
 */
    suspend fun waitForCrossfadeWindow(active: ExoPlayer, next: ExoPlayer) {
        val start = System.currentTimeMillis()
        while (true) {
            when (active.playbackState) {
                Player.STATE_ENDED -> {
 // Ролик кончился, но следующий может ещё не буфернуть → ждём
                    val elapsed = System.currentTimeMillis() - start
                    val waited = System.currentTimeMillis()
                    while (next.playbackState != Player.STATE_READY &&
                        System.currentTimeMillis() - waited < READY_WAIT_TIMEOUT_MS
                    ) {
                        Timber.tag(TAG).w("video ended but next not ready yet, waiting... elapsed=%dms", elapsed)
                        delay(POLL_MS)
                    }
                    return
                }
                Player.STATE_READY, Player.STATE_BUFFERING -> {
                    val dur = active.duration
                    val pos = active.currentPosition
                    if (dur != C.TIME_UNSET && dur > 0) {
                        val remaining = dur - pos
                        if (remaining <= CROSSFADE_TRIGGER_MS &&
                            next.playbackState == Player.STATE_READY
                        ) {
                            Timber.tag(TAG).d("crossfade window: remaining=%dms", remaining)
                            return
                        }
                    }
                    delay(POLL_MS)
                }
                else -> delay(POLL_MS)
            }
        }
    }

    LaunchedEffect(Unit) {
 // A: видео 0, сразу играть
        preload(playerA, 0)
        playerA.playWhenReady = true

 // B: видео 1, только буферизовать (не играть)
        preload(playerB, 1)

 // Плавное появление
        alphaA.animateTo(1f, tween(CROSSFADE_MS, easing = LinearEasing))

 // Основной цикл: ждём окна → кроссфейд → preload следующего
        while (true) {
            val isA = activeIsA
            val active = if (isA) playerA else playerB
            val next   = if (isA) playerB else playerA
            val fromAlpha = if (isA) alphaA else alphaB
            val toAlpha   = if (isA) alphaB else alphaA

 // Ждём: за CROSSFADE_TRIGGER_MS до конца И следующий готов
            waitForCrossfadeWindow(active, next)

 // Запустить следующий (буферизован) с самого начала
            next.seekTo(0)
            next.playWhenReady = true

 // Параллельный кроссфейд: оба слоя анимируются одновременно
            launch { fromAlpha.animateTo(0f, tween(CROSSFADE_MS, easing = LinearEasing)) }
            toAlpha.animateTo(1f, tween(CROSSFADE_MS, easing = LinearEasing))

            activeIsA = !isA
            counter[0]++

 // Начать буферизовать следующий ролик в теперь неактивный плеер
            val preloadTarget = if (activeIsA) playerB else playerA
            preload(preloadTarget, counter[0] + 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = alphaA.value },
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.idle_player_view, null) as PlayerView)
                    .apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        player = playerA
                    }
            },
            update = { it.player = playerA },
        )

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = alphaB.value },
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.idle_player_view, null) as PlayerView)
                    .apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        player = playerB
                    }
            },
            update = { it.player = playerB },
        )
    }
}
