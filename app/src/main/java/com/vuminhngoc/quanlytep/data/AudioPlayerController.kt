package com.vuminhngoc.quanlytep.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import java.io.File
import java.util.Locale

class AudioPlayerController(private val appContext: Context? = null) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSourceKey: String? = null

    fun resolveAudioFile(
        realFilePath: String?,
        fallbackDir: String,
        name: String
    ): File? {
        val candidates = buildList {
            realFilePath?.takeIf { it.isNotBlank() && !it.startsWith("content://") }?.let { raw ->
                add(File(raw))
                if (raw.startsWith("/sdcard/")) {
                    add(File(raw.replaceFirst("/sdcard/", "/storage/emulated/0/")))
                }
            }
            if (fallbackDir.isNotBlank() && !fallbackDir.startsWith("content://")) {
                add(File(fallbackDir, name))
            }
        }
        return candidates.firstOrNull { it.exists() && it.isFile }
    }

    fun readDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L) ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun readDurationMs(uri: Uri): Long {
        val ctx = appContext ?: return 0L
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(ctx, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L) ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSec = (ms.coerceAtLeast(0L) / 1000L)
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        return String.format(Locale.ROOT, "%02d:%02d", mins, secs)
    }

    fun togglePlayFile(
        file: File,
        onStateChanged: (
            isPlaying: Boolean,
            positionMs: Long,
            durationMs: Long,
            error: String?
        ) -> Unit
    ) {
        val key = "file:${file.absolutePath}"
        try {
            if (currentSourceKey != key || mediaPlayer == null) {
                stopAndRelease()
                val player = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener { mp ->
                        val dur = mp.duration.coerceAtLeast(0).toLong()
                        onStateChanged(false, 0L, dur, null)
                    }
                    start()
                }
                mediaPlayer = player
                currentSourceKey = key
                onStateChanged(
                    true,
                    player.currentPosition.toLong().coerceAtLeast(0L),
                    player.duration.coerceAtLeast(0).toLong(),
                    null
                )
            } else {
                val player = mediaPlayer!!
                val dur = player.duration.coerceAtLeast(0).toLong()
                if (player.isPlaying) {
                    player.pause()
                    onStateChanged(
                        false,
                        player.currentPosition.toLong().coerceAtLeast(0L),
                        dur,
                        null
                    )
                } else {
                    player.start()
                    onStateChanged(
                        true,
                        player.currentPosition.toLong().coerceAtLeast(0L),
                        dur,
                        null
                    )
                }
            }
        } catch (e: Exception) {
            stopAndRelease()
            onStateChanged(
                false,
                0L,
                0L,
                "Không thể phát tệp âm thanh: ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }

    fun togglePlayUri(
        uri: Uri,
        onStateChanged: (
            isPlaying: Boolean,
            positionMs: Long,
            durationMs: Long,
            error: String?
        ) -> Unit
    ) {
        val ctx = appContext
        if (ctx == null) {
            onStateChanged(false, 0L, 0L, "Thiếu Context để phát tệp từ URI.")
            return
        }
        val key = "uri:$uri"
        try {
            if (currentSourceKey != key || mediaPlayer == null) {
                stopAndRelease()
                val player = MediaPlayer().apply {
                    setDataSource(ctx, uri)
                    prepare()
                    setOnCompletionListener { mp ->
                        val dur = mp.duration.coerceAtLeast(0).toLong()
                        onStateChanged(false, 0L, dur, null)
                    }
                    start()
                }
                mediaPlayer = player
                currentSourceKey = key
                onStateChanged(
                    true,
                    player.currentPosition.toLong().coerceAtLeast(0L),
                    player.duration.coerceAtLeast(0).toLong(),
                    null
                )
            } else {
                val player = mediaPlayer!!
                val dur = player.duration.coerceAtLeast(0).toLong()
                if (player.isPlaying) {
                    player.pause()
                    onStateChanged(
                        false,
                        player.currentPosition.toLong().coerceAtLeast(0L),
                        dur,
                        null
                    )
                } else {
                    player.start()
                    onStateChanged(
                        true,
                        player.currentPosition.toLong().coerceAtLeast(0L),
                        dur,
                        null
                    )
                }
            }
        } catch (e: Exception) {
            stopAndRelease()
            onStateChanged(
                false,
                0L,
                0L,
                "Không thể phát tệp âm thanh: ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }

    fun seekTo(positionMs: Long): Long {
        val player = mediaPlayer ?: return 0L
        return runCatching {
            val clamped = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0).toLong())
            player.seekTo(clamped.toInt())
            clamped
        }.getOrDefault(0L)
    }

    fun currentPositionMs(): Long = runCatching {
        mediaPlayer?.currentPosition?.toLong()?.coerceAtLeast(0L) ?: 0L
    }.getOrDefault(0L)

    fun isPlaying(): Boolean = runCatching {
        mediaPlayer?.isPlaying == true
    }.getOrDefault(false)

    fun stopAndRelease() {
        runCatching {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
        }
        runCatching {
            mediaPlayer?.release()
        }
        mediaPlayer = null
        currentSourceKey = null
    }
}
