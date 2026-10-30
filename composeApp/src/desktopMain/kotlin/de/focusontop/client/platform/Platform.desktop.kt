package de.focusontop.client.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** Desktop is visual-QA only (screenshot harness) — no real OS, so this is
 *  an in-memory map, not a real preferences store; nothing needs to persist
 *  between runs here. */
actual object Prefs {
    private val map = HashMap<String, String>()
    actual fun get(key: String): String? = map[key]
    actual fun set(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
    }
}

actual val platformName: String = "desktop"

actual val deviceLanguage: String get() = Locale.getDefault().language

actual fun currentHour(): Int = LocalTime.now().hour

actual fun todayIso(): String = LocalDate.now().toString()

actual fun openUrl(url: String) { /* no-op: screenshot harness only */ }

actual fun shareText(text: String) { /* no-op: screenshot harness only */ }

/** No real video decoding here — a flat placeholder is enough for layout/QA screenshots. */
@Composable
actual fun VideoPlayer(url: String, modifier: Modifier) {
    Box(modifier.background(Color(0xFF1A1A1A)))
}

@Composable
actual fun ReelPlayer(url: String, playing: Boolean, modifier: Modifier) {
    Box(modifier.background(Color(0xFF1A1A1A)))
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) { /* no-op */ }

actual val googleSignInAvailable: Boolean = false

actual suspend fun googleIdToken(webClientId: String): String? = null

actual fun requestNotificationPermission() { /* no-op */ }

actual val appleSignInAvailable: Boolean = false

actual suspend fun appleSignInToken(): String? = null

actual fun nowEpochMs(): Long = System.currentTimeMillis()

@Composable
actual fun rememberFilePicker(onPicked: (List<PickedFile>) -> Unit): FilePicker =
    object : FilePicker {
        override fun pickFiles() { /* no-op */ }
        override fun pickMedia() { /* no-op */ }
        override fun pickFolder() { /* no-op */ }
    }

actual fun localDateTime(epochMs: Long): String =
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .format(java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault()))

/* Screenshot harness only — no mic / audio playback needed for static QA renders. */
actual val voiceRecordingAvailable: Boolean = false

@Composable
actual fun rememberVoiceRecorder(): VoiceRecorder = object : VoiceRecorder {
    override val isRecording = false
    override val elapsedMs = 0L
    override fun start() { /* no-op */ }
    override fun stop(): RecordedVoice? = null
    override fun cancel() { /* no-op */ }
}

@Composable
actual fun rememberVoicePlayer(): VoicePlayer = object : VoicePlayer {
    override val isPlaying = false
    override val progress = 0f
    override val positionMs = 0L
    override fun toggle(url: String) { /* no-op */ }
}
