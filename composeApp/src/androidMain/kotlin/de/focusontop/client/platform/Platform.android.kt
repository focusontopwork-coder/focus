package de.focusontop.client.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.lang.ref.WeakReference
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** Holds the application context and the current activity for the platform helpers. */
@SuppressLint("StaticFieldLeak")
object AndroidHost {
    lateinit var app: Context
        private set
    var activity: WeakReference<Activity>? = null
    internal lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        app = context.applicationContext
        prefs = app.getSharedPreferences("focusontop_client", Context.MODE_PRIVATE)
    }
}

actual object Prefs {
    actual fun get(key: String): String? = AndroidHost.prefs.getString(key, null)
    actual fun set(key: String, value: String?) {
        AndroidHost.prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}

actual val platformName: String = "android"

actual val deviceLanguage: String get() = Locale.getDefault().language

actual fun currentHour(): Int = LocalTime.now().hour

actual fun todayIso(): String = LocalDate.now().toString()

actual fun openUrl(url: String) {
    val ctx: Context = AndroidHost.activity?.get() ?: AndroidHost.app
    val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (ctx !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(i) }
}

actual fun shareText(text: String) {
    val ctx: Context = AndroidHost.activity?.get() ?: AndroidHost.app
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    val chooser = Intent.createChooser(send, null)
    if (ctx !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(chooser) }
}

@Composable
actual fun VideoPlayer(url: String, modifier: Modifier) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        modifier = modifier,
        factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
        update = { it.player = player },
    )
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)

actual val googleSignInAvailable: Boolean = true

actual suspend fun googleIdToken(webClientId: String): String? {
    val activity = AndroidHost.activity?.get() ?: return null
    val option = GetSignInWithGoogleOption.Builder(webClientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    return try {
        val result = CredentialManager.create(activity).getCredential(activity, request)
        val cred = result.credential
        if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleIdTokenCredential.createFrom(cred.data).idToken
        } else null
    } catch (_: GetCredentialCancellationException) {
        null
    }
}

actual fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val a = AndroidHost.activity?.get() ?: return
    if (a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        a.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 41)
    }
}

actual val appleSignInAvailable: Boolean = false

actual suspend fun appleSignInToken(): String? = null

actual fun nowEpochMs(): Long = System.currentTimeMillis()

private class StreamReader(private val input: java.io.InputStream?) : ByteReader {
    override fun read(max: Int): ByteArray? {
        val s = input ?: return null
        val buf = ByteArray(max)
        var n = 0
        while (n < max) {
            val r = s.read(buf, n, max - n)
            if (r < 0) break
            n += r
        }
        return if (n == 0) null else if (n == max) buf else buf.copyOf(n)
    }
    override fun close() { runCatching { input?.close() } }
}

private fun pickedFromUri(ctx: Context, uri: Uri, folder: String?): PickedFile? {
    var name = "datei"
    var size = -1L
    runCatching {
        ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getString(0)?.let { name = it }
                if (!c.isNull(1)) size = c.getLong(1)
            }
        }
    }
    if (size <= 0) return null
    val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
    return PickedFile(name, size, mime, folder) { StreamReader(ctx.contentResolver.openInputStream(uri)) }
}

/** Every file below a picked folder (keeps the sub-folder path; hidden files and > 500 files skipped). */
private fun walkTree(ctx: Context, tree: Uri): List<PickedFile> {
    val out = mutableListOf<PickedFile>()
    val rootId = android.provider.DocumentsContract.getTreeDocumentId(tree)
    var rootName = "Ordner"
    runCatching {
        ctx.contentResolver.query(
            android.provider.DocumentsContract.buildDocumentUriUsingTree(tree, rootId),
            arrayOf(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null,
        )?.use { c -> if (c.moveToFirst()) c.getString(0)?.let { rootName = it } }
    }
    fun walk(docId: String, path: String, depth: Int) {
        if (depth > 6 || out.size >= 500) return
        val children = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val cols = arrayOf(
            android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
            android.provider.DocumentsContract.Document.COLUMN_SIZE,
        )
        runCatching {
            ctx.contentResolver.query(children, cols, null, null, null)?.use { c ->
                while (c.moveToNext() && out.size < 500) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    if (name.startsWith(".")) continue
                    val mime = c.getString(2) ?: "application/octet-stream"
                    if (mime == android.provider.DocumentsContract.Document.MIME_TYPE_DIR) {
                        walk(id, "$path/$name", depth + 1)
                    } else {
                        val size = if (c.isNull(3)) 0L else c.getLong(3)
                        if (size <= 0) continue
                        val uri = android.provider.DocumentsContract.buildDocumentUriUsingTree(tree, id)
                        out += PickedFile(name, size, mime, path) { StreamReader(ctx.contentResolver.openInputStream(uri)) }
                    }
                }
            }
        }
    }
    walk(rootId, rootName, 0)
    return out
}

@Composable
actual fun rememberFilePicker(onPicked: (List<PickedFile>) -> Unit): FilePicker {
    val ctx = LocalContext.current
    val callback = androidx.compose.runtime.rememberUpdatedState(onPicked)
    val files = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> if (uris.isNotEmpty()) callback.value(uris.mapNotNull { pickedFromUri(ctx, it, null) }) }
    val media = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(50),
    ) { uris -> if (uris.isNotEmpty()) callback.value(uris.mapNotNull { pickedFromUri(ctx, it, null) }) }
    val folder = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree(),
    ) { tree -> if (tree != null) callback.value(walkTree(ctx, tree)) }
    return remember(files, media, folder) {
        object : FilePicker {
            override fun pickFiles() = files.launch(arrayOf("*/*"))
            override fun pickMedia() = media.launch(
                androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo),
            )
            override fun pickFolder() = folder.launch(null)
        }
    }
}

actual fun localDateTime(epochMs: Long): String =
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .format(java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault()))

@Composable
actual fun ReelPlayer(url: String, playing: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            prepare()
        }
    }
    androidx.compose.runtime.LaunchedEffect(playing) { player.playWhenReady = playing }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { it.player = player },
    )
}
