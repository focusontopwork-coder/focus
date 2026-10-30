package de.focusontop.client.ui

import de.focusontop.client.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.focusontop.client.data.ApiError
import de.focusontop.client.data.AppState
import de.focusontop.client.data.ClientFile
import de.focusontop.client.data.Comment
import de.focusontop.client.data.Media
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.platform.VideoPlayer
import kotlinx.coroutines.launch

/* ───────────── 13e · Freigabe · Versionen vergleichen ─────────────
 * Real data only: the two (or more) versions compared here are genuine
 * uploads that share one taskId (GET /api/client-app/files/:id/versions —
 * every re-upload after a revision request is its own row, so this is an
 * honest version history, not an invented one). The "what changed" list
 * is the real, resolved feedback left on the earlier version — no fake
 * diff is generated, because the server has no frame-level diff data. */

private enum class CompareMode { Slider, SideBySide, Toggle }

@Composable
fun VersionCompareScreen(state: AppState, initial: ClientFile) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var versions by remember(initial.id) { mutableStateOf(listOf(initial)) }
    var loading by remember(initial.id) { mutableStateOf(true) }
    var mode by remember { mutableStateOf(CompareMode.Toggle) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(initial.id) {
        runCatching { state.api.fileVersions(initial.id) }
            .onSuccess { if (it.size >= 2) versions = it }
        loading = false
    }

    val older = versions.firstOrNull() ?: initial
    val newer = versions.lastOrNull() ?: initial
    var showingNewer by remember { mutableStateOf(true) }
    val shown = if (showingNewer) newer else older

    // Real feedback that was left on the earlier version — the honest
    // substitute for a fabricated "what changed" diff.
    val changes = remember { mutableStateOf<List<Comment>>(emptyList()) }
    LaunchedEffect(older.id) {
        runCatching { state.api.comments(older.id) }
            .onSuccess { changes.value = it.filter { c -> c.timestampSeconds != null } }
    }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(55.dp).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Ic.Back, s.back, tint = C.Ink, modifier = Modifier.size(24.dp).clickable { state.back() })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    tr("${older.versionLabel ?: "V1"} und ${newer.versionLabel ?: "V2"} vergleichen",
                        "Compare ${older.versionLabel ?: "V1"} and ${newer.versionLabel ?: "V2"}",
                        "مقارنة ${older.versionLabel ?: "V1"} و ${newer.versionLabel ?: "V2"}"),
                    color = C.Ink, style = T.Title3, fontWeight = FontWeight.SemiBold, maxLines = 1,
                )
                Text(newer.projectName ?: newer.taskName ?: newer.title(), color = C.Muted, style = T.Caption, maxLines = 1)
            }
            Icon(Ic.Close, tr("Schließen", "Close", "إغلاق"), tint = C.Muted, modifier = Modifier.size(24.dp).clickable { state.back() })
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = C.Gold) }
            return@Column
        }
        if (versions.size < 2) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Ic.Compare, null, tint = C.Muted, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(10.dp))
                Text(tr("Keine weitere Version vorhanden", "No other version yet", "لا توجد نسخة أخرى حتى الآن"), color = C.Muted, style = T.Body, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            return@Column
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                ModeTabs(mode) { mode = it }
            }
            item {
                Box(Modifier.fillMaxWidth().height(219.dp).clip(RoundedCornerShape(20.dp)).background(C.Surface2)) {
                    when (mode) {
                        CompareMode.Toggle -> {
                            MediaBox(shown, Modifier.fillMaxSize())
                            VersionPill(shown.versionLabel ?: "", Modifier.align(Alignment.TopStart).padding(10.dp))
                            Row(
                                Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(R.Pill).background(C.GlassStrong)
                                    .clickable { showingNewer = !showingNewer }.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Ic.Compare, null, tint = C.Ink, modifier = Modifier.size(14.dp))
                                Text(
                                    tr("Zu ${(if (showingNewer) older else newer).versionLabel} wechseln",
                                        "Switch to ${(if (showingNewer) older else newer).versionLabel}",
                                        "التبديل إلى ${(if (showingNewer) older else newer).versionLabel}"),
                                    color = C.Ink, style = T.Caption, fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        CompareMode.SideBySide -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(Modifier.weight(1f).fillMaxWidth()) { MediaBox(older, Modifier.fillMaxSize()); VersionPill(older.versionLabel ?: "", Modifier.align(Alignment.TopStart).padding(8.dp)) }
                            Box(Modifier.weight(1f).fillMaxWidth()) { MediaBox(newer, Modifier.fillMaxSize()); VersionPill(newer.versionLabel ?: "", Modifier.align(Alignment.TopStart).padding(8.dp)) }
                        }
                        CompareMode.Slider -> androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                            var frac by remember { mutableStateOf(0.5f) }
                            val totalPx = constraints.maxWidth.toFloat()
                            MediaBox(newer, Modifier.fillMaxSize())
                            Box(Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0.04f, 0.96f)).align(Alignment.CenterStart)) { MediaBox(older, Modifier.fillMaxSize()) }
                            VersionPill(older.versionLabel ?: "", Modifier.align(Alignment.TopStart).padding(8.dp))
                            VersionPill(newer.versionLabel ?: "", Modifier.align(Alignment.TopEnd).padding(8.dp))
                            Box(
                                Modifier.fillMaxHeight().width(28.dp)
                                    .align(Alignment.TopStart)
                                    .offset { androidx.compose.ui.unit.IntOffset((frac.coerceIn(0.04f, 0.96f) * totalPx - 56f).toInt(), 0) }
                                    .pointerDragHandle { delta -> frac = (frac + delta).coerceIn(0.04f, 0.96f) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(Modifier.width(3.dp).fillMaxHeight().background(C.Ink.copy(alpha = 0.6f)))
                                Box(Modifier.size(28.dp).clip(CircleShape).background(C.Ink), contentAlignment = Alignment.Center) {
                                    Icon(Ic.Compare, null, tint = C.Bg, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    tr("Beide Versionen stehen einzeln zur Wiedergabe bereit — ohne gleichzeitige Tonspur.",
                        "Both versions are ready to play individually — not with simultaneous audio.",
                        "كل نسخة جاهزة للتشغيل بشكل منفصل — دون صوت متزامن للنسختين معاً."),
                    color = C.Muted, style = T.Caption,
                )
            }
            if (changes.value.isNotEmpty()) {
                item { Text(tr("Rückmeldungen zu ${older.versionLabel}", "Feedback on ${older.versionLabel}", "ملاحظات على ${older.versionLabel}"), color = C.Ink, style = T.Headline) }
                item {
                    Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.dp, C.StrokeGlass, R.CardShape)) {
                        changes.value.forEachIndexed { i, c ->
                            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(if (c.resolved != 0) Ic.CheckBold else Ic.ChatLines, null, tint = if (c.resolved != 0) C.Green else C.Muted, modifier = Modifier.size(16.dp))
                                Text(c.text ?: "", color = C.Ink, style = T.BodyS, modifier = Modifier.weight(1f), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                c.timestampSeconds?.let { ts ->
                                    val sec = ts.toInt()
                                    Text("${sec / 60}:${(sec % 60).toString().padStart(2, '0')}", color = C.GoldLabel, style = T.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1,
                                        modifier = Modifier.height(26.dp).clip(R.Pill).background(C.GoldTint).padding(horizontal = 10.dp, vertical = 5.dp))
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }

        if (!newer.mine) Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val keepOlderLabel = tr("Zurück zu ${older.versionLabel}", "Keep ${older.versionLabel}", "العودة إلى ${older.versionLabel}")
            val preferOlderNote = tr("Der Kunde bevorzugt ${older.versionLabel}.", "The client prefers ${older.versionLabel}.", "العميل يفضّل ${older.versionLabel}.")
            SecondaryButton(
                keepOlderLabel,
                onClick = {
                    if (busy) return@SecondaryButton
                    busy = true
                    scope.launch {
                        try {
                            state.api.requestRevision(newer.id, preferOlderNote)
                            state.updateFile(newer.id, "revision_requested")
                            state.showToast(s.revisionSent)
                            state.back()
                        } catch (_: ApiError) { } finally { busy = false }
                    }
                },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                tr("${newer.versionLabel} freigeben", "Approve ${newer.versionLabel}", "الموافقة على ${newer.versionLabel}"),
                onClick = {
                    if (busy) return@PrimaryButton
                    busy = true
                    scope.launch {
                        try {
                            state.api.approve(newer.id)
                            state.updateFile(newer.id, "approved")
                            state.showToast(s.approvedThanks)
                            state.refresh()
                            state.back()
                        } catch (_: ApiError) { } finally { busy = false }
                    }
                },
                modifier = Modifier.weight(1f), enabled = !busy, loading = busy,
            )
        }
    }
}

@Composable
private fun ModeTabs(mode: CompareMode, onChange: (CompareMode) -> Unit) {
    val items = listOf(
        CompareMode.Slider to tr("Schieberegler", "Slider", "شريط التمرير"),
        CompareMode.SideBySide to tr("Nebeneinander", "Side by side", "جنباً إلى جنب"),
        CompareMode.Toggle to tr("Umschalten", "Toggle", "تبديل"),
    )
    Row(Modifier.fillMaxWidth().height(42.dp).clip(R.Pill).background(C.Surface2).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (m, label) ->
            val sel = m == mode
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(R.Pill).background(if (sel) C.Surface else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onChange(m) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (sel) C.Ink else C.Muted, style = T.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun VersionPill(label: String, modifier: Modifier = Modifier) {
    if (label.isBlank()) return
    Text(label, color = C.Ink, style = T.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1,
        modifier = modifier.clip(R.Pill).background(C.GlassStrong).padding(horizontal = 10.dp, vertical = 5.dp))
}

@Composable
private fun MediaBox(f: ClientFile, modifier: Modifier = Modifier) {
    when (f.type) {
        "Video" -> {
            val src = Media.video(f)
            if (src != null) VideoPlayer(src, modifier) else FileCover(f, modifier, 800)
        }
        "Image" -> coil3.compose.AsyncImage(model = Media.image(f.url, 1200), contentDescription = f.title(), contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = modifier)
        else -> FileCover(f, modifier, 800)
    }
}

/** Minimal horizontal drag handle: reports the normalized delta it moved. */
private fun Modifier.pointerDragHandle(onDrag: (Float) -> Unit): Modifier = this.pointerInput(Unit) {
    detectDragGestures { change, dragAmount ->
        change.consume()
        onDrag(dragAmount.x / size.width.toFloat())
    }
}
