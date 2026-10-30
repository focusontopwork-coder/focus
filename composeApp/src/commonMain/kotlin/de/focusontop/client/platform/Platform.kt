package de.focusontop.client.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow

/** Tiny key–value store (SharedPreferences / NSUserDefaults). */
expect object Prefs {
    fun get(key: String): String?
    fun set(key: String, value: String?)
}

/** "android" or "ios" — sent with the push token. */
expect val platformName: String

/** Phone language ("ar", "de", "en", …) — picks the app language on first start. */
expect val deviceLanguage: String

/** 0–23, for "good morning / good evening". */
expect fun currentHour(): Int

/** Today as "YYYY-MM-DD" (local time) — overdue invoices. */
expect fun todayIso(): String

/** Opens a link in the browser / the right app. */
expect fun openUrl(url: String)

/** System share sheet with plain text (a link). */
expect fun shareText(text: String)

/** Native video player (ExoPlayer / AVPlayer). */
@Composable
expect fun VideoPlayer(url: String, modifier: Modifier = Modifier)

/** Full-screen looping video without controls (Reels); plays only while [playing]. */
@Composable
expect fun ReelPlayer(url: String, playing: Boolean, modifier: Modifier = Modifier)

/** Android system back. iOS: no-op (screens show a back button). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/** True where the native Google sign-in is built in (Android for now). */
expect val googleSignInAvailable: Boolean

/**
 * Shows the phone's Google account picker and returns a Google ID token for
 * [webClientId]. Returns null when the person closes the picker; throws on errors.
 */
expect suspend fun googleIdToken(webClientId: String): String?

/** Asks for permission to show notifications (Android 13+ / iOS). */
expect fun requestNotificationPermission()

/** Whether "Sign in with Apple" is offered on this platform (iOS only). */
expect val appleSignInAvailable: Boolean

/** Opens the Apple sign-in sheet and returns the Apple identity token (null when closed). */
expect suspend fun appleSignInToken(): String?

/**
 * The phone's push token arrives from native code (FCM on Android,
 * APNs on iOS via AppDelegate → PushBridge.onToken). The app sends it to the
 * server after login.
 */
object PushBridge {
    val token = MutableStateFlow<String?>(null)
    fun onToken(value: String) { token.value = value }
}

/** Milliseconds since 1970 (UTC) — for "2 hours ago". */
expect fun nowEpochMs(): Long

/** Reads a picked file piece by piece (uploads never hold a whole video in memory). */
interface ByteReader {
    /** Up to [max] bytes, or null at the end. */
    fun read(max: Int): ByteArray?
    fun close()
}

/** A file the client picked on the phone. [folder] = its path inside a picked folder ("Shooting/Raw"). */
class PickedFile(
    val name: String,
    val size: Long,
    val mimeType: String,
    val folder: String?,
    val open: () -> ByteReader,
)

interface FilePicker {
    fun pickFiles()
    /** The phone's photo gallery (photos + videos). */
    fun pickMedia()
    fun pickFolder()
}

/** The system file / folder picker (Storage Access Framework on Android, document picker on iOS). */
@Composable
expect fun rememberFilePicker(onPicked: (List<PickedFile>) -> Unit): FilePicker

/** Epoch ms → "YYYY-MM-DD HH:mm" in the phone's time zone. */
expect fun localDateTime(epochMs: Long): String
