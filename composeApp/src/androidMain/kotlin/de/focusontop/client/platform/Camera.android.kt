package de.focusontop.client.platform

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.effect.RgbMatrix
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

actual val inAppCameraAvailable: Boolean = true

actual fun localFileUrl(path: String): String = Uri.fromFile(File(path)).toString()

private fun newFile(ext: String): File {
    val dir = File(AndroidHost.app.cacheDir, "story").apply { mkdirs() }
    return File(dir, "story_${System.currentTimeMillis()}.$ext")
}

@Composable
actual fun CameraPreview(filter: StoryFilter, modifier: Modifier, onControl: (CameraControl?) -> Unit) {
    val ctx = LocalContext.current
    val owner = ctx as? LifecycleOwner
    val callback = rememberUpdatedState(onControl)
    fun has(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(has(Manifest.permission.CAMERA)) }
    var audio by remember { mutableStateOf(has(Manifest.permission.RECORD_AUDIO)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        granted = res[Manifest.permission.CAMERA] == true || has(Manifest.permission.CAMERA)
        audio = res[Manifest.permission.RECORD_AUDIO] == true || has(Manifest.permission.RECORD_AUDIO)
        if (!granted) callback.value(null)
    }
    LaunchedEffect(Unit) { if (!granted || !audio) ask.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }

    val previewView = remember {
        PreviewView(ctx).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE   // TextureView → filters can be drawn on it
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var front by remember { mutableStateOf(false) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val videoCapture = remember { VideoCapture.withOutput(Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var onStopped by remember { mutableStateOf<((CameraShot?) -> Unit)?>(null) }

    LaunchedEffect(granted, front) {
        if (!granted || owner == null) return@LaunchedEffect
        val p = provider ?: suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
            val f = ProcessCameraProvider.getInstance(ctx)
            f.addListener({ cont.resume(f.get()) }, ContextCompat.getMainExecutor(ctx))
        }.also { provider = it }
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        runCatching {
            p.unbindAll()
            camera = p.bindToLifecycle(owner, selector, preview, imageCapture, videoCapture)
        }
        callback.value(object : CameraControl {
            override fun flip() { front = !front }
            override fun torch(on: Boolean) { camera?.cameraControl?.enableTorch(on) }
            override fun photo(onDone: (CameraShot?) -> Unit) {
                val file = newFile("jpg")
                val meta = ImageCapture.Metadata().apply { isReversedHorizontal = front }
                val opts = ImageCapture.OutputFileOptions.Builder(file).setMetadata(meta).build()
                imageCapture.takePicture(opts, ContextCompat.getMainExecutor(ctx), object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) = onDone(CameraShot(file.absolutePath, false))
                    override fun onError(exception: ImageCaptureException) = onDone(null)
                })
            }
            @android.annotation.SuppressLint("MissingPermission")
            override fun startVideo(): Boolean {
                if (recording != null) return false
                val file = newFile("mp4")
                val pending = videoCapture.output.prepareRecording(ctx, FileOutputOptions.Builder(file).build())
                    .let { if (has(Manifest.permission.RECORD_AUDIO)) it.withAudioEnabled() else it }
                recording = pending.start(ContextCompat.getMainExecutor(ctx)) { e ->
                    if (e is VideoRecordEvent.Finalize) {
                        val ok = !e.hasError() || e.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
                        onStopped?.invoke(if (ok && file.length() > 0) CameraShot(file.absolutePath, true) else null)
                        onStopped = null
                        recording = null
                    }
                }
                return true
            }
            override fun stopVideo(onDone: (CameraShot?) -> Unit) {
                val r = recording ?: return onDone(null)
                onStopped = onDone
                r.stop()
            }
        })
    }
    DisposableEffect(Unit) { onDispose { recording?.stop(); provider?.unbindAll() } }

    // live colour look: the preview (a TextureView) is drawn through a colour-matrix layer
    LaunchedEffect(filter) {
        if (filter == StoryFilter.NORMAL) previewView.setLayerType(View.LAYER_TYPE_NONE, null)
        else previewView.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply { colorFilter = ColorMatrixColorFilter(filter.matrix4x5()) })
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

private class FileReader(path: String) : ByteReader {
    private val input = File(path).inputStream()
    override fun read(max: Int): ByteArray? {
        val buf = ByteArray(max); var n = 0
        while (n < max) { val r = input.read(buf, n, max - n); if (r < 0) break; n += r }
        return if (n == 0) null else if (n == max) buf else buf.copyOf(n)
    }
    override fun close() { runCatching { input.close() } }
}

private fun picked(file: File, video: Boolean) =
    PickedFile(file.name, file.length(), if (video) "video/mp4" else "image/jpeg", null) { FileReader(file.absolutePath) }

actual suspend fun renderStory(shot: CameraShot, filter: StoryFilter): PickedFile? {
    val src = File(shot.path)
    if (!src.exists()) return null
    return if (!shot.isVideo) withContext(Dispatchers.Default) { renderPhoto(src, filter) } else renderVideo(src, filter)
}

private fun renderPhoto(src: File, filter: StoryFilter): PickedFile {
    // respect the EXIF rotation, keep it at most 1920 px on the long side
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(src.absolutePath, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1920) sample *= 2
    val raw = BitmapFactory.decodeFile(src.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return picked(src, false)
    val rotation = when (android.media.ExifInterface(src.absolutePath).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val upright = if (rotation != 0f) Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation) }, true) else raw
    val out = Bitmap.createBitmap(upright.width, upright.height, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(upright, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply {
        if (filter != StoryFilter.NORMAL) colorFilter = ColorMatrixColorFilter(filter.matrix4x5())
    })
    val file = newFile("jpg")
    FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    return picked(file, false)
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private suspend fun renderVideo(src: File, filter: StoryFilter): PickedFile? {
    if (filter == StoryFilter.NORMAL) return picked(src, true)
    val out = newFile("mp4")
    // media3 colour matrix: 4×4, column-major
    val m = filter.m
    val rgb = FloatArray(16).also {
        for (r in 0 until 3) for (c in 0 until 3) it[c * 4 + r] = m[r * 3 + c]
        it[15] = 1f
    }
    val effect = object : RgbMatrix {
        override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = rgb
    }
    val ok = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Boolean> { cont ->
            val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(src)))
                .setEffects(Effects(listOf(), listOf(effect)))
                .build()
            val transformer = Transformer.Builder(AndroidHost.app)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) { if (cont.isActive) cont.resume(true) }
                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) { if (cont.isActive) cont.resume(false) }
                })
                .build()
            transformer.start(item, out.absolutePath)
            cont.invokeOnCancellation { transformer.cancel() }
        }
    }
    return if (ok && out.length() > 0) picked(out, true) else picked(src, true)
}

actual suspend fun copyToStoryCache(file: PickedFile): CameraShot? = withContext(Dispatchers.IO) {
    val video = file.mimeType.startsWith("video/")
    val out = newFile(if (video) "mp4" else "jpg")
    val reader = file.open()
    try {
        FileOutputStream(out).use { o -> while (true) { val b = reader.read(1 shl 16) ?: break; o.write(b) } }
    } finally { reader.close() }
    if (out.length() > 0) CameraShot(out.absolutePath, video) else null
}
