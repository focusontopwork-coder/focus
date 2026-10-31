package de.focusontop.client.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

/* No in-app story camera on web yet (needs MediaDevices.getUserMedia through an
 * interop view) — people still post from gallery uploads. Same stopgap desktop uses. */
actual val inAppCameraAvailable: Boolean = false

@Composable
actual fun CameraPreview(filter: StoryFilter, modifier: Modifier, onControl: (CameraControl?) -> Unit) {
    Box(modifier)
    LaunchedEffect(Unit) { onControl(null) }
}

actual suspend fun copyToStoryCache(file: PickedFile): CameraShot? = null

actual fun localFileUrl(path: String): String = path

actual suspend fun renderStory(shot: CameraShot, filter: StoryFilter): PickedFile? = null
