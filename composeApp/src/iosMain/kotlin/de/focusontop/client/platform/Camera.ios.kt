@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package de.focusontop.client.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import platform.AVFoundation.*
import platform.CoreGraphics.*
import platform.CoreImage.*
import platform.Foundation.*
import platform.QuartzCore.CATransaction
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import kotlin.math.max

/**
 * iPhone story camera — the iOS twin of Camera.android.kt.
 *
 * - Live preview: AVCaptureVideoPreviewLayer (aspect-fill) in a custom UIView.
 * - Photo / HD video capture (with audio when the microphone is allowed),
 *   front/back switch, torch — the same CameraControl contract as Android.
 * - Filters: the exact same 3×3 colour matrix as Android is baked into the
 *   captured photo (Core Image CIColorMatrix) or video (AVVideoComposition).
 *   The live preview shows a light tint approximation of the filter; the
 *   exported story always carries the real matrix.
 */
actual val inAppCameraAvailable: Boolean = true

actual fun localFileUrl(path: String): String = "file://$path"

@OptIn(ExperimentalForeignApi::class)
private fun storyDir(): String {
    val dir = NSTemporaryDirectory() + "/story"
    NSFileManager.defaultManager.createDirectoryAtPath(
        dir, withIntermediateDirectories = true, attributes = null, error = null,
    )
    return dir
}

private fun newFile(ext: String): String {
    val stamp = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
    return "${storyDir()}/story_$stamp.$ext"
}

@OptIn(ExperimentalForeignApi::class)
private fun fileSize(path: String): Long {
    val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
    return (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
}

private fun exists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val out = ByteArray(length.toInt())
    out.usePinned { p -> platform.posix.memcpy(p.addressOf(0), bytes, length) }
    return out
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { p ->
    NSData.create(bytes = p.addressOf(0), length = size.toULong())
} ?: NSData()

/** Reads a local file piece by piece (same contract as the Android FileReader). */
private class FileReader(path: String) : ByteReader {
    private val handle = NSFileHandle.fileHandleForReadingAtPath(path)
    override fun read(max: Int): ByteArray? {
        val data = handle?.readDataOfLength(max.toULong()) ?: return null
        if (data.length.toInt() == 0) return null
        return data.toByteArray()
    }
    override fun close() { runCatching { handle?.closeFile() } }
}

private fun pickedFile(path: String, video: Boolean): PickedFile {
    val size = fileSize(path)
    val mime = when (path.substringAfterLast('.').lowercase()) {
        "mov" -> "video/quicktime"
        "mp4" -> "video/mp4"
        else -> if (video) "video/mp4" else "image/jpeg"
    }
    return PickedFile(path.substringAfterLast('/'), size, mime, null) { FileReader(path) }
}

/** UIView whose sublayer always fills the view (the camera preview). */
@OptIn(ExperimentalForeignApi::class)
private class PreviewView : UIView(frame = CGRectZero.readValue()) {
    val previewLayer = AVCaptureVideoPreviewLayer()

    init {
        backgroundColor = UIColor.blackColor
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        previewLayer.frame = bounds
        CATransaction.commit()
    }
}

/** Core Image context shared by photo and video rendering. */
private object CIRenderer {
    val context: CIContext? by lazy { runCatching { CIContext() }.getOrNull() }
}

/** Applies the same row-major 3×3 matrix as Android's ColorMatrixColorFilter. */
private fun applyMatrix(image: CIImage, filter: StoryFilter): CIImage? {
    if (filter == StoryFilter.NORMAL) return image
    val f = CIFilter.filterWithName("CIColorMatrix") ?: return null
    val m = filter.m
    fun vec(a: Float, b: Float, c: Float): CIVector =
        CIVector.vectorWithX(a.toDouble(), Y = b.toDouble(), Z = c.toDouble(), W = 0.0)
    f.setValue(vec(m[0], m[1], m[2]), forKey = "inputRVector")
    f.setValue(vec(m[3], m[4], m[5]), forKey = "inputGVector")
    f.setValue(vec(m[6], m[7], m[8]), forKey = "inputBVector")
    f.setValue(image, forKey = "inputImage")
    return f.outputImage
}

private class IosCamera {
    val container = PreviewView()
    private val session = AVCaptureSession()
    private val photoOutput = AVCapturePhotoOutput()
    private val movieOutput = AVCaptureMovieFileOutput()

    private var videoDevice: AVCaptureDevice? = null
    private var photoDelegate: NSObject? = null
    private var moviePath: String? = null
    private var stopCallback: ((CameraShot?) -> Unit)? = null
    private var recording = false
    private var torchOn = false

    var front = false
        private set

    private fun deviceFor(front: Boolean): AVCaptureDevice? =
        AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
            AVMediaTypeVideo,
            if (front) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack,
        ).devices.firstOrNull() as? AVCaptureDevice
            ?: AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)

    /** Builds/starts the capture session on a background queue, then reports readiness on the main queue. */
    fun start(front: Boolean, withAudio: Boolean, onReady: (CameraControl?) -> Unit) {
        this.front = front
        dispatch_async(dispatch_get_global_queue(0, 0u)) {
            val ok = runCatching {
                session.beginConfiguration()
                session.sessionPreset = AVCaptureSessionPresetHigh
                session.inputs.forEach { session.removeInput(it as AVCaptureDeviceInput) }
                val device = deviceFor(front) ?: throw IllegalStateException("no camera device")
                val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = null) as? AVCaptureDeviceInput
                    ?: throw IllegalStateException("no camera input")
                if (!session.canAddInput(input)) throw IllegalStateException("cannot add input")
                session.addInput(input)
                videoDevice = device
                if (session.outputs.isEmpty()) {
                    if (session.canAddOutput(photoOutput)) session.addOutput(photoOutput)
                    if (session.canAddOutput(movieOutput)) session.addOutput(movieOutput)
                }
                if (withAudio) {
                    val mic = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeAudio)
                    val micInput = mic?.let {
                        AVCaptureDeviceInput.deviceInputWithDevice(it, error = null) as? AVCaptureDeviceInput
                    }
                    if (micInput != null && session.canAddInput(micInput)) session.addInput(micInput)
                }
                fixConnections()
                session.commitConfiguration()
                session.startRunning()
            }.isSuccess
            dispatch_async(dispatch_get_main_queue()) { onReady(if (ok) control() else null) }
        }
    }

    fun stop() {
        dispatch_async(dispatch_get_global_queue(0, 0u)) {
            runCatching { session.stopRunning() }
        }
    }

    private fun fixConnections() {
        fun orient(c: AVCaptureConnection?) {
            c?.let {
                if (it.isVideoOrientationSupported()) it.videoOrientation = AVCaptureVideoOrientationPortrait
                if (front && it.isVideoMirroringSupported()) it.videoMirrored = true
            }
        }
        orient(photoOutput.connectionWithMediaType(AVMediaTypeVideo))
        orient(movieOutput.connectionWithMediaType(AVMediaTypeVideo))
    }

    fun flip(onDone: () -> Unit) {
        start(!front, false) { onDone() }
    }

    fun torch(on: Boolean) {
        val d = videoDevice ?: return
        runCatching {
            if (!d.hasTorch) return
            d.lockForConfiguration(null)
            d.torchMode = if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff
            d.unlockForConfiguration()
            torchOn = on
        }
    }

    fun photo(onDone: (CameraShot?) -> Unit) {
        val path = newFile("jpg")
        val delegate = object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
            override fun captureOutput(
                output: AVCapturePhotoOutput,
                didFinishProcessingPhoto: AVCapturePhoto,
                error: NSError?,
            ) {
                val data = runCatching { didFinishProcessingPhoto.fileDataRepresentation() }.getOrNull()
                val saved = if (data != null) {
                    runCatching { data.writeToFile(path, atomically = true) }.getOrDefault(false)
                } else false
                photoDelegate = null
                dispatch_async(dispatch_get_main_queue()) {
                    onDone(if (saved && fileSize(path) > 0) CameraShot(path, false) else null)
                }
            }
        }
        photoDelegate = delegate
        val settings = AVCapturePhotoSettings().apply {
            flashMode = if (torchOn) AVCaptureFlashModeOn else AVCaptureFlashModeOff
        }
        runCatching { photoOutput.capturePhotoWithSettings(settings, delegate) }
            .onFailure { photoDelegate = null; dispatch_async(dispatch_get_main_queue()) { onDone(null) } }
    }

    fun startVideo(): Boolean {
        if (recording) return false
        val path = newFile("mp4")
        moviePath = path
        recording = true
        return runCatching {
            movieOutput.startRecordingToOutputFileURL(NSURL.fileURLWithPath(path), recordingDelegate = movieDelegate())
            true
        }.getOrDefault(false)
    }

    fun stopVideo(onDone: (CameraShot?) -> Unit) {
        if (!recording) { onDone(null); return }
        stopCallback = onDone
        runCatching { movieOutput.stopRecording() }.onFailure {
            recording = false
            dispatch_async(dispatch_get_main_queue()) { onDone(null) }
        }
    }

    private fun movieDelegate() = object : NSObject(), AVCaptureFileOutputRecordingDelegateProtocol {
        override fun captureOutput(
            output: AVCaptureFileOutput,
            didFinishRecordingToOutputFileAtURL: NSURL,
            fromConnections: List<*>,
            error: NSError?,
        ) {
            recording = false
            val path = didFinishRecordingToOutputFileAtURL.path ?: moviePath
            val ok = path != null && exists(path) && fileSize(path) > 0
            val cb = stopCallback
            stopCallback = null
            dispatch_async(dispatch_get_main_queue()) {
                cb?.invoke(if (ok && path != null) CameraShot(path, true) else null)
            }
        }
    }

    private fun control() = object : CameraControl {
        override fun flip() { this@IosCamera.flip { } }
        override fun torch(on: Boolean) = this@IosCamera.torch(on)
        override fun photo(onDone: (CameraShot?) -> Unit) = this@IosCamera.photo(onDone)
        override fun startVideo(): Boolean = this@IosCamera.startVideo()
        override fun stopVideo(onDone: (CameraShot?) -> Unit) = this@IosCamera.stopVideo(onDone)
    }
}

private fun StoryFilter.previewTint(): Color? = when (this) {
    StoryFilter.NORMAL -> null
    StoryFilter.GOLD -> Color(0xFFD9A94A)
    StoryFilter.WARM -> Color(0xFFD98A4A)
    StoryFilter.COOL -> Color(0xFF4A8AD9)
    StoryFilter.MONO -> Color(0xFF808080)
    StoryFilter.VINTAGE -> Color(0xFFB08A5A)
    StoryFilter.DRAMA -> Color(0xFF2A2F6B)
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(filter: StoryFilter, modifier: Modifier, onControl: (CameraControl?) -> Unit) {
    val callback = rememberUpdatedState(onControl)
    val camera = remember { IosCamera() }
    DisposableEffect(Unit) {
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { videoOk ->
            if (!videoOk) {
                dispatch_async(dispatch_get_main_queue()) { callback.value(null) }
            } else {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeAudio) { audioOk ->
                    dispatch_async(dispatch_get_main_queue()) {
                        camera.start(front = false, withAudio = audioOk) { control -> callback.value(control) }
                    }
                }
            }
        }
        onDispose { camera.stop() }
    }
    Box(modifier) {
        UIKitView(factory = { camera.container }, modifier = Modifier.fillMaxSize())
        // light preview approximation — the exported story gets the exact matrix
        filter.previewTint()?.let { tint ->
            Box(Modifier.fillMaxSize().background(tint.copy(alpha = 0.25f)))
        }
    }
}

actual suspend fun copyToStoryCache(file: PickedFile): CameraShot? = withContext(Dispatchers.IO) {
    val video = file.mimeType.startsWith("video/")
    val out = newFile(if (video) "mp4" else "jpg")
    val reader = file.open()
    try {
        val handle = NSFileHandle.fileHandleForWritingAtPath(out)
        while (handle != null) {
            val chunk = reader.read(1 shl 16) ?: break
            if (chunk.isEmpty()) break
            handle.writeData(chunk.toNSData(), error = null)
        }
        handle?.closeFile()
    } catch (_: Exception) {
        runCatching { reader.close() }
        return@withContext null
    } finally {
        runCatching { reader.close() }
    }
    if (exists(out) && fileSize(out) > 0) CameraShot(out, video) else null
}

actual suspend fun renderStory(shot: CameraShot, filter: StoryFilter): PickedFile? = withContext(Dispatchers.Default) {
    val src = shot.path
    if (!exists(src)) return@withContext null
    if (filter == StoryFilter.NORMAL) return@withContext pickedFile(src, shot.isVideo)
    try {
        if (shot.isVideo) renderVideo(src, filter) else renderPhoto(src, filter)
    } catch (_: Exception) {
        pickedFile(src, shot.isVideo)
    }
}

/** Bakes the colour matrix into a photo (EXIF-safe, max 1920 px on the long side) and writes a JPEG. */
private fun renderPhoto(src: String, filter: StoryFilter): PickedFile {
    val ui = UIImage(contentsOfFile = src) ?: return pickedFile(src, false)
    var image = CIImage.imageWithContentsOfURL(NSURL.fileURLWithPath(src))
        ?: return pickedFile(src, false)
    // orientation: map the UIImage orientation value to its EXIF number before filtering
    val exif = when (ui.imageOrientation.value) {
        1L -> 3; 2L -> 8; 3L -> 6; 4L -> 2; 5L -> 4; 6L -> 5; 7L -> 7; else -> 1
    }
    runCatching { image = image.imageByApplyingOrientation(exif) }
    val longSide = image.extent.useContents { max(size.width, size.height) }
    if (longSide > 1920.0) {
        val s = 1920.0 / longSide
        runCatching { image = image.imageByApplyingTransform(CGAffineTransformMakeScale(s, s)) }
    }
    val filtered = applyMatrix(image, filter) ?: image
    val ctx = CIRenderer.context ?: return pickedFile(src, false)
    val cg = runCatching { ctx.createCGImage(filtered, fromRect = filtered.extent) }.getOrNull()
        ?: return pickedFile(src, false)
    val out = newFile("jpg")
    val encoded = runCatching {
        val uiOut = UIImage.imageWithCGImage(cg)
        val data = UIImageJPEGRepresentation(uiOut, 0.9)
        platform.CoreFoundation.CFRelease(cg)
        data?.writeToFile(out, atomically = true) == true
    }.getOrDefault(false)
    return if (encoded && exists(out) && fileSize(out) > 0) pickedFile(out, false) else pickedFile(src, false)
}

/** Bakes the colour matrix into a video via AVVideoComposition + Core Image. */
private suspend fun renderVideo(src: String, filter: StoryFilter): PickedFile {
    val asset = AVURLAsset.URLAssetWithURL(NSURL.fileURLWithPath(src), null)
    val composition = runCatching {
        AVMutableVideoComposition.videoCompositionWithAsset(asset) { request ->
            request?.let { r ->
                val f = CIFilter.filterWithName("CIColorMatrix")
                f?.setValue(r.sourceImage, forKey = "inputImage")
                val m = filter.m
                fun vec(a: Float, b: Float, c: Float): CIVector =
                    CIVector.vectorWithX(a.toDouble(), Y = b.toDouble(), Z = c.toDouble(), W = 0.0)
                f?.setValue(vec(m[0], m[1], m[2]), forKey = "inputRVector")
                f?.setValue(vec(m[3], m[4], m[5]), forKey = "inputGVector")
                f?.setValue(vec(m[6], m[7], m[8]), forKey = "inputBVector")
                val filtered = f?.outputImage
                if (filtered != null) {
                    r.finishWithImage(filtered as objcnames.classes.CIImage, context = null)
                } else {
                    r.finishWithImage(r.sourceImage, context = null)
                }
            }
        }
    }.getOrNull() ?: return pickedFile(src, true)

    val export = runCatching {
        AVAssetExportSession(asset = asset, presetName = AVAssetExportPresetHighestQuality)
    }.getOrNull() ?: return pickedFile(src, true)
    val out = newFile("mp4")
    export.outputURL = NSURL.fileURLWithPath(out)
    export.outputFileType = AVFileTypeMPEG4
    export.videoComposition = composition
    runCatching { export.exportAsynchronouslyWithCompletionHandler { } }
    var waited = 0
    while (export.status == AVAssetExportSessionStatusExporting && waited++ < 300) delay(100)
    return if (export.status == AVAssetExportSessionStatusCompleted && exists(out) && fileSize(out) > 0) {
        pickedFile(out, true)
    } else {
        pickedFile(src, true)
    }
}
