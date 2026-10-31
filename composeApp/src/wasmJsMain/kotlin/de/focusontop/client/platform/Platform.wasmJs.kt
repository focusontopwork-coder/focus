package de.focusontop.client.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/* ───────────── raw JS interop ─────────────
 * Kotlin/Wasm's browser target has no `kotlin.js.Date` / `dynamic` (those are
 * legacy Kotlin/JS-only APIs) — plain `js("...")` single-expression bodies,
 * with Kotlin String/Int/Double/Boolean crossing the boundary automatically,
 * is the supported way to touch the DOM/browser APIs here. */

private fun jsDateHours(): Int = js("new Date().getHours()")
private fun jsDateIso(): String = js("new Date().toISOString().slice(0, 10)")
private fun jsNowMs(): Double = js("Date.now()")
private fun jsLocalDateTime(epochMs: Double): String = js("""
    (function (ms) {
        var d = new Date(ms);
        var p = function (n) { return n < 10 ? '0' + n : '' + n; };
        return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
    })(epochMs)
""")

private fun jsLocalStorageGet(key: String): String? = js("localStorage.getItem(key)")
private fun jsLocalStorageSet(key: String, value: String): Unit = js("localStorage.setItem(key, value)")
private fun jsLocalStorageRemove(key: String): Unit = js("localStorage.removeItem(key)")

private fun jsNavigatorLanguage(): String = js("navigator.language")
private fun jsWindowOpen(url: String): Unit = js("window.open(url, '_blank')")
private fun jsClipboardWriteText(text: String): Unit = js("navigator.clipboard.writeText(text)")
private fun jsShareOrCopy(text: String): Unit = js("""
    (function (t) {
        if (navigator.share) { navigator.share({ text: t }); } else { navigator.clipboard.writeText(t); }
    })(text)
""")
private fun jsRequestNotificationPermission(): Unit = js("""
    (function () { if (window.Notification) { Notification.requestPermission(); } })()
""")

/** Browser's own localStorage — persists per-device, same spirit as SharedPreferences/NSUserDefaults. */
actual object Prefs {
    actual fun get(key: String): String? = jsLocalStorageGet(key)
    actual fun set(key: String, value: String?) {
        if (value == null) jsLocalStorageRemove(key) else jsLocalStorageSet(key, value)
    }
}

actual val platformName: String = "web"

actual val deviceLanguage: String get() = jsNavigatorLanguage().substringBefore("-")

actual fun currentHour(): Int = jsDateHours()

actual fun todayIso(): String = jsDateIso()

actual fun openUrl(url: String) {
    jsWindowOpen(url)
}

actual fun shareText(text: String) {
    jsShareOrCopy(text)
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
    jsRequestNotificationPermission()
}

actual val appleSignInAvailable: Boolean = false

actual suspend fun appleSignInToken(): String? = null

actual fun nowEpochMs(): Long = jsNowMs().toLong()

@Composable
actual fun rememberFilePicker(onPicked: (List<PickedFile>) -> Unit): FilePicker =
    object : FilePicker {
        override fun pickFiles() { /* TODO: wire up <input type=file> */ }
        override fun pickMedia() { /* TODO */ }
        override fun pickFolder() { /* TODO */ }
    }

actual fun localDateTime(epochMs: Long): String = jsLocalDateTime(epochMs.toDouble())

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
