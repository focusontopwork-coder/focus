package de.focusontop.client.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.*
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AVKit.*
import platform.Foundation.*
import platform.UIKit.*
import platform.UserNotifications.*
import platform.darwin.dispatch_async
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

actual object Prefs {
    private val d get() = NSUserDefaults.standardUserDefaults
    actual fun get(key: String): String? = d.stringForKey(key)
    actual fun set(key: String, value: String?) {
        if (value == null) d.removeObjectForKey(key) else d.setObject(value, forKey = key)
    }
}

actual val platformName: String = "ios"

actual val deviceLanguage: String
    get() = (NSLocale.preferredLanguages.firstOrNull() as? String)?.substringBefore("-") ?: "de"

actual fun currentHour(): Int =
    NSCalendar.currentCalendar.component(NSCalendarUnitHour, fromDate = NSDate()).toInt()

actual fun todayIso(): String = NSDateFormatter().apply {
    dateFormat = "yyyy-MM-dd"
    locale = NSLocale(localeIdentifier = "en_US_POSIX")
}.stringFromDate(NSDate())

actual fun openUrl(url: String) {
    val u = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(u, options = emptyMap<Any?, Any>(), completionHandler = null)
}

actual fun shareText(text: String) {
    val root = platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    var top = root
    while (top.presentedViewController != null) top = top.presentedViewController!!
    val vc = platform.UIKit.UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    top.presentViewController(vc, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(url: String, modifier: Modifier) {
    val player = remember(url) { NSURL.URLWithString(url)?.let { AVPlayer(uRL = it) } } ?: return
    DisposableEffect(player) { onDispose { player.pause() } }
    UIKitViewController(
        factory = {
            AVPlayerViewController().apply {
                this.player = player
                showsPlaybackControls = true
                player.play()
            }
        },
        modifier = modifier,
    )
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system back button; every inner screen shows its own back arrow.
}

// iPhone: Google sign-in runs as an OAuth web-session with PKCE (GoogleOAuth),
// and Apple sign-in comes with it (App Store rule: both or none).
actual val googleSignInAvailable: Boolean = true

actual suspend fun googleIdToken(webClientId: String): String? = GoogleOAuth.idToken(webClientId)

actual fun requestNotificationPermission() {
    UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
        UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
    ) { granted, _ ->
        if (granted) dispatch_async(dispatch_get_main_queue()) {
            UIApplication.sharedApplication.registerForRemoteNotifications()
        }
    }
}

actual val appleSignInAvailable: Boolean = true

actual suspend fun appleSignInToken(): String? = AppleSignInIos.idToken()

actual fun nowEpochMs(): Long = (platform.Foundation.NSDate().timeIntervalSince1970 * 1000.0).toLong()

@OptIn(ExperimentalForeignApi::class)
private class HandleReader(private val url: NSURL, private val scoped: Boolean) : ByteReader {
    private val handle = platform.Foundation.NSFileHandle.fileHandleForReadingFromURL(url, error = null)
    override fun read(max: Int): ByteArray? {
        val data = handle?.readDataOfLength(max.toULong()) ?: return null
        val n = data.length.toInt()
        if (n == 0) return null
        val out = ByteArray(n)
        out.usePinned { pinned -> platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length) }
        return out
    }
    override fun close() {
        handle?.closeFile()
        if (scoped) url.stopAccessingSecurityScopedResource()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun sizeOf(url: NSURL): Long {
    val attrs = platform.Foundation.NSFileManager.defaultManager.attributesOfItemAtPath(url.path ?: return 0, error = null)
    return (attrs?.get(platform.Foundation.NSFileSize) as? platform.Foundation.NSNumber)?.longLongValue ?: 0L
}

private fun mimeOf(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "mp4", "m4v" -> "video/mp4"; "mov" -> "video/quicktime"
    "jpg", "jpeg" -> "image/jpeg"; "png" -> "image/png"; "heic" -> "image/heic"; "webp" -> "image/webp"
    "pdf" -> "application/pdf"; "zip" -> "application/zip"
    else -> "application/octet-stream"
}

/** Kept alive while the picker is open (UIKit only holds the delegate weakly). */
private var pickerDelegate: platform.darwin.NSObject? = null

@OptIn(ExperimentalForeignApi::class)
private fun presentPicker(folder: Boolean, onPicked: (List<PickedFile>) -> Unit, mediaOnly: Boolean = false) {
    val types = when {
        folder -> listOf(platform.UniformTypeIdentifiers.UTTypeFolder)
        mediaOnly -> listOf(platform.UniformTypeIdentifiers.UTTypeImage, platform.UniformTypeIdentifiers.UTTypeMovie)
        else -> listOf(platform.UniformTypeIdentifiers.UTTypeItem)
    }
    val picker = platform.UIKit.UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = !folder)
    picker.allowsMultipleSelection = !folder
    val delegate = object : platform.darwin.NSObject(), platform.UIKit.UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: platform.UIKit.UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            val urls = didPickDocumentsAtURLs.filterIsInstance<NSURL>()
            val out = mutableListOf<PickedFile>()
            if (!folder) {
                urls.forEach { u ->
                    val name = u.lastPathComponent ?: "datei"
                    val size = sizeOf(u)
                    if (size > 0) out += PickedFile(name, size, mimeOf(name), null) { HandleReader(u, false) }
                }
            } else urls.firstOrNull()?.let { root ->
                val scoped = root.startAccessingSecurityScopedResource()
                val rootName = root.lastPathComponent ?: "Ordner"
                val en = platform.Foundation.NSFileManager.defaultManager.enumeratorAtPath(root.path ?: "")
                while (out.size < 500) {
                    val rel = en?.nextObject() as? String ?: break
                    val name = rel.substringAfterLast('/')
                    if (name.startsWith(".")) continue
                    val u = root.URLByAppendingPathComponent(rel) ?: continue
                    val size = sizeOf(u)
                    var isDir: Boolean = false
                    val attrs = platform.Foundation.NSFileManager.defaultManager.attributesOfItemAtPath(u.path ?: "", error = null)
                    isDir = attrs?.get(platform.Foundation.NSFileType) == platform.Foundation.NSFileTypeDirectory
                    if (isDir || size <= 0) continue
                    val sub = rel.substringBeforeLast('/', "")
                    out += PickedFile(name, size, mimeOf(name), if (sub.isEmpty()) rootName else "$rootName/$sub") { HandleReader(u, false) }
                }
                // keep access open for the upload; released when the app goes away
                if (!scoped) Unit
            }
            pickerDelegate = null
            if (out.isNotEmpty()) onPicked(out)
        }
        override fun documentPickerWasCancelled(controller: platform.UIKit.UIDocumentPickerViewController) { pickerDelegate = null }
    }
    pickerDelegate = delegate
    picker.delegate = delegate
    UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(picker, animated = true, completion = null)
}

@Composable
actual fun rememberFilePicker(onPicked: (List<PickedFile>) -> Unit): FilePicker {
    val callback = androidx.compose.runtime.rememberUpdatedState(onPicked)
    return remember {
        object : FilePicker {
            override fun pickFiles() = presentPicker(false, { callback.value(it) })
            override fun pickMedia() = presentPicker(false, { callback.value(it) }, mediaOnly = true)
            override fun pickFolder() = presentPicker(true, { callback.value(it) })
        }
    }
}

actual fun localDateTime(epochMs: Long): String = NSDateFormatter().apply {
    dateFormat = "yyyy-MM-dd HH:mm"
    locale = NSLocale(localeIdentifier = "en_US_POSIX")
}.stringFromDate(platform.Foundation.NSDate.dateWithTimeIntervalSince1970(epochMs / 1000.0))

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ReelPlayer(url: String, playing: Boolean, modifier: Modifier) {
    val player = remember(url) { NSURL.URLWithString(url)?.let { AVPlayer(uRL = it) } } ?: return
    DisposableEffect(player) {
        // loop: jump back to the start when the clip ends
        val observer = platform.Foundation.NSNotificationCenter.defaultCenter.addObserverForName(
            platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification, player.currentItem, null,
        ) { _ -> player.seekToTime(platform.CoreMedia.CMTimeMake(0, 1)); player.play() }
        onDispose { platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(observer); player.pause() }
    }
    androidx.compose.runtime.LaunchedEffect(playing) { if (playing) player.play() else player.pause() }
    UIKitViewController(
        factory = {
            AVPlayerViewController().apply {
                this.player = player
                showsPlaybackControls = false
                videoGravity = platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
            }
        },
        modifier = modifier,
    )
}

/**
 * "Sign in with Apple" — Apple's App Store rule: whenever Google sign-in is
 * offered, Apple sign-in must be offered too. Returns the Apple identity
 * token (a signed JWT the server verifies against Apple's public keys).
 */
private object AppleSignInIos {
    private var controller: ASAuthorizationController? = null
    private var holder: NSObject? = null

    suspend fun idToken(): String? = suspendCancellableCoroutine { cont ->
        val request = ASAuthorizationAppleIDProvider().createRequest()
        val ctrl = ASAuthorizationController(authorizationRequests = listOf(request))
        val delegateHolder = object : NSObject(),
            ASAuthorizationControllerDelegateProtocol,
            ASAuthorizationControllerPresentationContextProvidingProtocol {

            override fun authorizationController(
                controller: ASAuthorizationController,
                didCompleteWithAuthorization: ASAuthorization,
            ) {
                val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
                val jwt = credential?.identityToken?.toByteArray()?.decodeToString()
                if (cont.isActive) cont.resume(jwt, null)
            }

            override fun authorizationController(
                controller: ASAuthorizationController,
                didCompleteWithError: NSError,
            ) {
                if (cont.isActive) cont.resume(null, null)
            }

            override fun presentationAnchorForAuthorizationController(
                controller: ASAuthorizationController,
            ): UIWindow = UIApplication.sharedApplication.keyWindow ?: UIWindow()
        }
        ctrl.delegate = delegateHolder
        ctrl.presentationContextProvider = delegateHolder
        this.controller = ctrl
        this.holder = delegateHolder
        ctrl.performRequests()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val out = ByteArray(length.toInt())
    out.usePinned { p -> platform.posix.memcpy(p.addressOf(0), bytes, length) }
    return out
}
