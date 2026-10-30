package de.focusontop.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.focusontop.client.data.AppState
import de.focusontop.client.data.Screen
import de.focusontop.client.i18n.Lang
import de.focusontop.client.i18n.LocalLang
import de.focusontop.client.i18n.tr
import de.focusontop.client.platform.CameraControl
import de.focusontop.client.platform.CameraPreview
import de.focusontop.client.platform.CameraShot
import de.focusontop.client.platform.ReelPlayer
import de.focusontop.client.platform.StoryFilter
import de.focusontop.client.platform.localFileUrl
import de.focusontop.client.platform.renderStory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Instagram-style story camera: live colour effects, tap = photo, hold = video, then share. */
@Composable
fun StoryCameraScreen(state: AppState) {
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()
    var control by remember { mutableStateOf<CameraControl?>(null) }
    var denied by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(StoryFilter.NORMAL) }
    var torch by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableStateOf(0) }
    var shot by remember { mutableStateOf<CameraShot?>(null) }
    var text by remember { mutableStateOf("") }
    var editingText by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var showLabels by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // the effect whose circle is centred under the shutter
    val selectedIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - mid) }?.index ?: 0
        }
    }
    LaunchedEffect(selectedIndex) { filter = StoryFilter.entries[selectedIndex] }
    // let go mid-scroll → settle the nearest effect exactly under the shutter
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        val info = listState.layoutInfo
        val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
        val item = info.visibleItemsInfo.firstOrNull { it.index == selectedIndex } ?: return@LaunchedEffect
        if (abs(item.offset + item.size / 2 - mid) > 2) listState.animateScrollToItem(selectedIndex)
    }
    val failed = tr("Teilen fehlgeschlagen", "Could not share", "تعذّر النشر")
    val done = tr("Story geteilt", "Story shared", "نُشرت قصتك")
    // gallery: pick a photo / video and edit it right here (no extra screen)
    val picker = de.focusontop.client.platform.rememberFilePicker { files ->
        val f = files.firstOrNull() ?: return@rememberFilePicker
        scope.launch { de.focusontop.client.platform.copyToStoryCache(f)?.let { shot = it } }
    }
    fun label(f: StoryFilter) = when (lang) { Lang.DE -> f.de; Lang.EN -> f.en; Lang.AR -> f.ar }

    // recording timer (max 30 s like a story)
    LaunchedEffect(recording) {
        seconds = 0
        while (recording) {
            delay(1000); seconds++
            if (seconds >= 30) control?.stopVideo { s -> recording = false; shot = s }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val current = shot
        if (current == null) {
            // ── capture
            CameraPreview(filter, Modifier.fillMaxSize()) { c -> control = c; denied = c == null }
            if (denied) {
                Column(Modifier.align(Alignment.Center).padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr("Kamera-Zugriff fehlt", "Camera access needed", "يلزم السماح بالكاميرا"), color = Color.White, style = T.Headline, fontWeight = FontWeight.SemiBold)
                    Text(tr("Erlaube die Kamera in den Einstellungen deines Handys oder wähle ein Foto aus der Galerie.", "Allow the camera in your phone settings or choose from your gallery.", "اسمح بالكاميرا من إعدادات الجوال أو اختر من المعرض."), color = Color.White.copy(alpha = 0.7f), style = T.Footnote, textAlign = TextAlign.Center)
                    GoldButton(tr("Galerie", "Gallery", "المعرض"), { picker.pickMedia() }, small = true)
                }
            }
            // live text typed before the shot (Aa) — carried into the review
            if (text.isNotBlank() && !editingText) {
                Text(
                    text, color = Color.White, style = T.Title1, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.45f))
                        .clickable { editingText = true }.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            countdown?.let { n ->
                Text("$n", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
            }
            // top bar — bare icons like Instagram: close · flash · (space)
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                BareIcon(Ic.Close, size = 30) { state.back() }
                Spacer(Modifier.weight(1f))
                BareIcon(Ic.Zap, tint = if (torch) C.Gold else Color.White.copy(alpha = 0.75f), size = 28) { torch = !torch; control?.torch(torch) }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(44.dp))
            }
            // left tool rail — icons only, labels on demand (chevron)
            Column(Modifier.align(Alignment.CenterStart).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                RailItem(null, "Aa", tr("Text", "Text", "نص"), showLabels) { editingText = true }
                RailItem(Ic.Sparkles, null, label(filter), showLabels) {
                    val next = (filter.ordinal + 1) % StoryFilter.entries.size
                    scope.launch { listState.animateScrollToItem(next) }
                }
                RailItem(Ic.Clock, null, if (timer) tr("Timer 3 s", "Timer 3 s", "مؤقّت ٣ ث") else tr("Timer", "Timer", "مؤقّت"), showLabels, active = timer) { timer = !timer }
                RailItem(if (showLabels) Ic.Close else Ic.ChevronDown, null, tr("Weniger", "Less", "إخفاء"), showLabels) { showLabels = !showLabels }
            }
            // bottom: effect carousel running through the shutter, then gallery · mode · flip
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (recording) Text("● 0:${seconds.toString().padStart(2, '0')}", color = C.Red, style = T.Body, fontWeight = FontWeight.SemiBold)
                else Text(label(filter), color = Color.White, style = T.Footnote, fontWeight = FontWeight.SemiBold)
                BoxWithConstraints(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                    val side = (maxWidth - 64.dp) / 2
                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = side),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(StoryFilter.entries.size) { i ->
                            val f = StoryFilter.entries[i]
                            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                                if (i != selectedIndex) EffectSwatch(f, 50.dp) { scope.launch { listState.animateScrollToItem(i) } }
                            }
                        }
                    }
                    // the shutter sits over the centred effect
                    Box(
                        Modifier.size(84.dp).clip(CircleShape).border(5.dp, if (recording) C.Red else Color.White, CircleShape).padding(8.dp).clip(CircleShape)
                            .then(
                                if (filter == StoryFilter.NORMAL || recording) Modifier.background(if (recording) C.Red else Color.White.copy(alpha = 0.92f))
                                else Modifier.background(swatchBrush(filter))
                            )
                            .pointerInput(control, timer) {
                                detectTapGestures(
                                    onTap = {
                                        if (!recording && countdown == null) {
                                            if (timer) scope.launch {
                                                for (n in 3 downTo 1) { countdown = n; delay(1000) }
                                                countdown = null
                                                control?.photo { s -> shot = s }
                                            } else control?.photo { s -> shot = s }
                                        }
                                    },
                                    onLongPress = { if (control?.startVideo() == true) recording = true },
                                    onPress = {
                                        tryAwaitRelease()
                                        if (recording) control?.stopVideo { s -> recording = false; shot = s }
                                    },
                                )
                            },
                    )
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    // gallery
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).border(2.dp, Color.White, RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.12f)).clickable { picker.pickMedia() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Ic.Image, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                    Spacer(Modifier.weight(1f))
                    Text(
                        tr("BEITRAG", "POST", "منشور"), color = Color.White.copy(alpha = 0.55f), style = T.Footnote, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { state.back(); state.go(Screen.Compose("post")) }.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                    Text(tr("STORY", "STORY", "قصة"), color = Color.White, style = T.Footnote, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    Spacer(Modifier.weight(1f))
                    // flip
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).clickable { control?.flip(); torch = false },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Ic.Flip, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                }
            }
            if (editingText) TextEditorOverlay(text, { text = it }) { editingText = false }
        } else {
            // ── review: effect, text, share
            if (current.isVideo) ReelPlayer(localFileUrl(current.path), playing = true, modifier = Modifier.fillMaxSize())
            else AsyncImage(
                model = localFileUrl(current.path), contentDescription = null, contentScale = ContentScale.Crop,
                colorFilter = if (filter == StoryFilter.NORMAL) null else ColorFilter.colorMatrix(ColorMatrix(filter.matrix4x5())),
                modifier = Modifier.fillMaxSize(),
            )
            if (text.isNotBlank() && !editingText) {
                Text(
                    text, color = Color.White, style = T.Title1, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.45f))
                        .clickable { editingText = true }.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CamIcon(Ic.Close) { shot = null; text = ""; filter = StoryFilter.NORMAL }
                Spacer(Modifier.weight(1f))
                CamIcon(Ic.Pencil) { editingText = true }
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                    .navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (current.isVideo && filter != StoryFilter.NORMAL) Text(tr("Effekt „${label(filter)}“ wird beim Teilen angewendet", "Effect “${label(filter)}” is applied when sharing", "يُطبَّق تأثير «${label(filter)}» عند النشر"), color = Color.White.copy(alpha = 0.8f), style = T.Caption)
                FilterRow(filter, ::label) { filter = it }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(tr("Neu aufnehmen", "Retake", "إعادة"), { shot = null; text = "" }, Modifier.weight(1f))
                    GoldButton(tr("Deine Story", "Your story", "قصتك"), {
                        busy = true
                        scope.launch {
                            try {
                                val file = renderStory(current, filter) ?: throw IllegalStateException()
                                val blob = state.uploadBlob(file)
                                state.api.publish("story", blob.url ?: "", if (current.isVideo) "Video" else "Image", text.trim(), emptyList(), false)
                                state.showToast(done)
                                state.refreshCommunity()
                                state.back()
                            } catch (_: Exception) { state.showToast(failed) } finally { busy = false }
                        }
                    }, Modifier.weight(1.3f), icon = Ic.Send, enabled = !busy, loading = busy)
                }
            }
            if (editingText) TextEditorOverlay(text, { text = it }) { editingText = false }
        }
        if (busy) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = C.Gold)
                Text(tr("Story wird geteilt …", "Sharing your story …", "جارٍ نشر قصتك…"), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CamIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color = Color.White, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.3f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun BareIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color = Color.White, size: Int = 26, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.Black.copy(alpha = 0.35f), modifier = Modifier.size(size.dp).offset(x = 1.dp, y = 1.dp))
        Icon(icon, null, tint = tint, modifier = Modifier.size(size.dp))
    }
}

/** One entry of the left tool rail: a bare icon (or "Aa"), its label shown when expanded. */
@Composable
private fun RailItem(icon: androidx.compose.ui.graphics.vector.ImageVector?, glyph: String?, label: String, showLabel: Boolean, active: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            val tint = if (active) C.Gold else Color.White
            if (glyph != null) Text(glyph, color = tint, style = T.Title2, fontWeight = FontWeight.Normal)
            else if (icon != null) Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
        }
        if (showLabel) {
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color.White, style = T.BodyS, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun swatchBrush(f: StoryFilter): Brush {
    val m = f.m
    fun px(r: Float, g: Float, b: Float) = Color(
        (m[0] * r + m[1] * g + m[2] * b).coerceIn(0f, 1f),
        (m[3] * r + m[4] * g + m[5] * b).coerceIn(0f, 1f),
        (m[6] * r + m[7] * g + m[8] * b).coerceIn(0f, 1f),
    )
    return Brush.linearGradient(listOf(px(0.91f, 0.66f, 0.42f), px(0.18f, 0.43f, 0.62f), px(0.55f, 0.77f, 0.54f)))
}

@Composable
private fun EffectSwatch(f: StoryFilter, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape).padding(2.dp).clip(CircleShape)
            .background(swatchBrush(f)).clickable(onClick = onClick),
    )
}

@Composable
private fun SideItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White, style = T.BodyS, fontWeight = FontWeight.SemiBold)
    }
}

/** Effect circles: each previews its colour look on a little gradient. */
@Composable
private fun FilterRow(selected: StoryFilter, label: (StoryFilter) -> String, onPick: (StoryFilter) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(StoryFilter.entries) { f ->
            val sel = f == selected
            Column(Modifier.clickable { onPick(f) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier.size(54.dp).clip(CircleShape).border(2.dp, if (sel) C.Gold else Color.White.copy(alpha = 0.8f), CircleShape)
                        .padding(3.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFE9A86A), Color(0xFF2F6E9E), Color(0xFF8BC48A)))),
                ) {
                    // tint the swatch with the filter's dominant hue
                    val m = f.m
                    val r = (m[0] + m[1] + m[2]) / 3f; val g = (m[3] + m[4] + m[5]) / 3f; val b = (m[6] + m[7] + m[8]) / 3f
                    if (f != StoryFilter.NORMAL) Box(Modifier.fillMaxSize().background(Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), 0.45f)))
                }
                Text(label(f), color = if (sel) C.Gold else Color.White, style = T.Micro, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TextEditorOverlay(value: String, onChange: (String) -> Unit, onDone: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onDone).imePadding()) {
        Text(
            tr("Fertig", "Done", "تم"), color = Color.White, style = T.Headline, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(20.dp).clickable(onClick = onDone),
        )
        BasicTextField(
            value, { if (it.length <= 120) onChange(it) },
            textStyle = T.Title1.copy(color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(C.Gold),
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(28.dp).focusRequester(focus),
        )
        if (value.isEmpty()) Text(tr("Text eingeben …", "Type something …", "اكتب نصًا…"), color = Color.White.copy(alpha = 0.6f), style = T.Title1, modifier = Modifier.align(Alignment.Center))
    }
}
