package de.focusontop.client.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Colour looks for stories (3×3 RGB matrix, row-major) — like Instagram's effects. */
enum class StoryFilter(val de: String, val en: String, val ar: String, val m: FloatArray) {
    NORMAL("Normal", "Normal", "عادي", floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)),
    GOLD("Gold", "Gold", "ذهبي", floatArrayOf(1.15f, 0.05f, 0f, 0.02f, 1.05f, 0f, -0.05f, 0f, 0.78f)),
    WARM("Warm", "Warm", "دافئ", floatArrayOf(1.12f, 0f, 0f, 0f, 1.0f, 0f, 0f, 0f, 0.85f)),
    COOL("Kühl", "Cool", "بارد", floatArrayOf(0.88f, 0f, 0f, 0f, 1.0f, 0f, 0f, 0f, 1.15f)),
    MONO("S/W", "B&W", "أبيض وأسود", floatArrayOf(0.299f, 0.587f, 0.114f, 0.299f, 0.587f, 0.114f, 0.299f, 0.587f, 0.114f)),
    VINTAGE("Vintage", "Vintage", "قديم", floatArrayOf(0.393f, 0.769f, 0.189f, 0.349f, 0.686f, 0.168f, 0.272f, 0.534f, 0.131f)),
    DRAMA("Drama", "Drama", "درامي", floatArrayOf(1.56f, -0.47f, -0.09f, -0.12f, 1.21f, -0.09f, -0.12f, -0.47f, 1.59f));

    /** 4×5 colour matrix (Android / Compose ColorMatrix layout). */
    fun matrix4x5(): FloatArray = floatArrayOf(
        m[0], m[1], m[2], 0f, 0f,
        m[3], m[4], m[5], 0f, 0f,
        m[6], m[7], m[8], 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

/** A photo or video just taken with the in-app camera (a local file). */
class CameraShot(val path: String, val isVideo: Boolean)

interface CameraControl {
    fun flip()
    fun torch(on: Boolean)
    fun photo(onDone: (CameraShot?) -> Unit)
    /** Returns false if recording could not start. */
    fun startVideo(): Boolean
    fun stopVideo(onDone: (CameraShot?) -> Unit)
}

/** True where the in-app story camera exists (Android for now). */
expect val inAppCameraAvailable: Boolean

/** Live camera preview with the [filter] applied; hands out the controls once ready (null = no permission). */
@Composable
expect fun CameraPreview(filter: StoryFilter, modifier: Modifier, onControl: (CameraControl?) -> Unit)

/** Copies a picked gallery file into the app's cache so the story editor can use it (null where not supported). */
expect suspend fun copyToStoryCache(file: PickedFile): CameraShot?

/** "file:///…" for a local path (for Coil / the video player). */
expect fun localFileUrl(path: String): String

/** Bakes the filter into the shot and returns it ready for upload. */
expect suspend fun renderStory(shot: CameraShot, filter: StoryFilter): PickedFile?
