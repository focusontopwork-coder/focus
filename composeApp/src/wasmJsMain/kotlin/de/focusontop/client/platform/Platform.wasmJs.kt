package de.focusontop.client.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlin.js.Date

/** Browser's own localStorage — persists per-device, same spirit as SharedPreferences/NSUserDefaults. */
actual object Prefs {
    actual fun get(key: String): String? = localStorage.getItem(key)
    actual fun set(key: String, value: String?) {
        if (value == null) localStorage.removeItem(key) else localStorage.setItem(key, value)
    }
}

actual val platformName: String = "web"

actual val deviceLanguage: String get() = window.navigator.language.substringBefore("-")

actual fun currentHour(): Int = Date().getHours()

actual fun todayIso(): String {
    val d = Date()
    val m = (d.getMonth() + 1).toString().padStart(2, '0')
    val day = d.getDate().toString().padStart(2, '0')
    return "${d.getFullYear()}-$m-$day"
}

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}

actual fun shareText(text: String) {
    // Web Share API where available; clipboard as the fallback (no system share sheet on desktop browsers).
    val nav = window.navigator
    if (js("typeof nav.share === 'function'") as Boolean) {
        nav.asDynamic().share(js("({ text: text })"))
    } else {
        window.navigator.clipboard.writeText(text)
    }
}

/* Placeholder surface until a real <video> element is wired in through Compose
 * Multiplatform's web interop views — same stopgap desktop already uses. */
@Composable
actual fun VideoPlayer(url: String, modifier: Modifier) {
    Box(modifier.background(Color(0xFF1A1A1A)))
}

@Composable
actual fun ReelPlayer(url: String, playing: Boolean, modifier: Modifier) {
    Box(modifier.background(Color(0xFF1A1A1A)))
}

/** Browsers have no OS back button; screens already show their own back arrow. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) { /* no-op */ }

/* Native Google One Tap / account picker isn't wired up yet on web — email/password
 * login still works. Next step: Google Identity Services JS SDK. */
actual val googleSignInAvailable: Boolean = false

actual suspend fun googleIdToken(webClientId: String): String? = null

actual fun requestNotificationPermission() {
    val n = window.asDynamic().Notification
    if (n != null) n.requestPermission()
}

actual val appleSignInAvailable: Boolean = false

actual suspend fun appleSignInToken(): String? = null

actual fun nowEpochMs(): Long = Date.now().toLong()

@Composable
actual fun rememberFilePicker(onPicked: (List<PickedFile>) -> Unit): FilePicker =
    object : FilePicker {
        override fun pickFiles() { /* TODO: wire up <input type=file> */ }
        override fun pickMedia() { /* TODO */ }
        override fun pickFolder() { /* TODO */ }
    }

actual fun localDateTime(epochMs: Long): String {
    val d = Date(epochMs.toDouble())
    val y = d.getFullYear()
    val m = (d.getMonth() + 1).toString().padStart(2, '0')
    val day = d.getDate().toString().padStart(2, '0')
    val h = d.getHours().toString().padStart(2, '0')
    val min = d.getMinutes().toString().padStart(2, '0')
    return "$y-$m-$day $h:$min"
}

/* No mic capture yet (MediaRecorder API) — mic shows disabled, same as desktop/iOS today. */
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
    override fun toggle(url: String) { /* TODO: wire up <audio> */ }
}

/* Face ID/Touch ID have no browser equivalent without WebAuthn; off for now. */
actual suspend fun biometricAvailable(): Boolean = false
actual suspend fun biometricAuthenticate(reason: String): Boolean = false
