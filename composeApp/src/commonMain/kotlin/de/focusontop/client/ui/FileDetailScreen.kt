package de.focusontop.client.ui

import de.focusontop.client.i18n.tr
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.focusontop.client.data.ApiError
import de.focusontop.client.data.AppState
import de.focusontop.client.data.ClientFile
import de.focusontop.client.data.Comment
import de.focusontop.client.data.Media
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.platform.VideoPlayer
import de.focusontop.client.platform.openUrl
import kotlinx.coroutines.launch

/** One piece of work: watch / view it, approve or ask for a change, talk about it. */
@Composable
fun FileDetailScreen(state: AppState, initial: ClientFile) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var file by remember(initial.id) { mutableStateOf(initial) }
    val comments = remember(initial.id) { mutableStateListOf<Comment>() }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var askRevision by remember { mutableStateOf(false) }
    var confirmApprove by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    suspend fun loadComments() {
        runCatching { state.api.comments(file.id) }.onSuccess { comments.clear(); comments.addAll(it) }
    }

    LaunchedEffect(initial.id) {
        runCatching { state.api.file(initial.id) }.onSuccess { file = it }
        loadComments()
    }

    var playing by remember(initial.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopBar(tr("Dateien", "Files", "الملفات"), onBack = { state.back() })
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // media stage 358 × 410
            item {
                Box(Modifier.fillMaxWidth().height(410.dp).background(C.Surface2), contentAlignment = Alignment.Center) {
                    when (file.type) {
                        "Video" -> {
                            val src = Media.video(file)
                            if (playing && src != null) VideoPlayer(src, Modifier.fillMaxSize())
                            else {
                                FileCover(file, Modifier.fillMaxSize(), 1200)
                                Box(Modifier.size(60.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).clickable { playing = true }, contentAlignment = Alignment.Center) {
                                    Icon(Ic.Play, tr("Abspielen", "Play", "تشغيل"), tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        "Image" -> AsyncImage(model = Media.image(file.url, 1600), contentDescription = file.title(), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        else -> Column(Modifier.clickable { Media.abs(file.url)?.let(::openUrl) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Ic.FileText, null, tint = C.Muted, modifier = Modifier.size(44.dp))
                            Text(file.fileName ?: file.title(), color = C.Muted, style = T.Footnote, maxLines = 1)
                        }
                    }
                }
            }
            item {
                val label = when {
                    file.mine -> tr("Dein Upload", "Your upload", "رفعك")
                    file.status == "ready_for_review" -> tr("Wartet auf Freigabe", "Awaiting approval", "بانتظار الموافقة")
                    file.status == "revision_requested" -> tr("Änderung angefragt", "Change requested", "طُلب تعديل")
                    file.status == "approved" -> tr("Freigegeben", "Approved", "تمت الموافقة")
                    file.status == "delivered" || file.status == "published" -> tr("Geliefert", "Delivered", "تم التسليم")
                    else -> fileStatus(s, file.status).label
                }
                Row { Text(label, color = C.GoldLabel, style = T.Caption, maxLines = 1,
                    modifier = Modifier.height(30.dp).clip(R.Pill).background(C.GoldTint).border(1.dp, C.Gold, R.Pill).padding(horizontal = 10.dp, vertical = 7.dp)) }
            }
            item {
                Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.dp, C.StrokeGlass, R.CardShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Details", "Details", "التفاصيل"), color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        file.size?.let { Text(it, color = C.Muted, style = T.Footnote) }
                    }
                    Text(file.title(), color = C.Ink, style = T.Footnote)
                    Text(listOfNotNull(file.taskName ?: file.projectName, fmtDate(file.createdAt)).joinToString(" · "), color = C.Muted, style = T.Body)
                    if (!file.revisionNotes.isNullOrBlank()) {
                        Text(s.yourNotes, color = C.Ink, style = T.Footnote)
                        Text(file.revisionNotes ?: "", color = C.Muted, style = T.Body)
                    }
                }
            }
            toast?.let { msg -> item { Text(msg, color = C.Green, style = T.Footnote) } }
            if (!file.mine) item {
                when (file.status) {
                    "ready_for_review", "revision_requested" -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SecondaryButton(if (file.status == "ready_for_review") tr("Änderung", "Change", "تعديل") else tr("Weitere Änderung", "Another change", "تعديل آخر"),
                            onClick = { if (!busy) askRevision = true }, modifier = Modifier.weight(1f))
                        PrimaryButton(tr("Freigeben", "Approve", "موافقة"), onClick = { confirmApprove = true }, modifier = Modifier.weight(1f), enabled = !busy)
                    }
                    "approved" -> SecondaryButton(tr("Weitere Änderung", "Another change", "تعديل آخر"), onClick = { if (!busy) askRevision = true }, modifier = Modifier.fillMaxWidth())
                    "delivered", "published" -> PrimaryButton(tr("Öffnen", "Open", "فتح"), onClick = {
                        (if (file.type == "Link" || file.type == "Document") Media.abs(file.url) else Media.download(file.url, file.fileName ?: file.name))?.let(::openUrl)
                    }, icon = Ic.External)
                    else -> {}
                }
            }
            item { Text(s.comments, color = C.Ink, style = T.Headline) }
            items(comments, key = { it.id }) { c ->
                CommentRow(c, busy = busy, onToggleResolve = { cm ->
                    busy = true
                    scope.launch {
                        try { state.api.resolveComment(cm.id, cm.resolved == 0); loadComments() } catch (_: ApiError) { } finally { busy = false }
                    }
                })
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(48.dp).clip(R.Pill).background(C.GlassStrong).border(1.dp, C.StrokeGlass, R.Pill).padding(12.dp), contentAlignment = Alignment.CenterStart) {
                if (draft.isEmpty()) Text(s.writeComment, color = C.Muted, style = T.Footnote)
                androidx.compose.foundation.text.BasicTextField(draft, { draft = it }, maxLines = 3, textStyle = T.Footnote.copy(color = C.Ink),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(C.Gold), modifier = Modifier.fillMaxWidth())
            }
            Icon(Ic.ArrowUp, s.send, tint = if (draft.isBlank() || busy) C.Muted else C.Ink, modifier = Modifier.size(22.dp).clickable(enabled = draft.isNotBlank() && !busy) {
                val text = draft.trim()
                busy = true
                scope.launch {
                    try { state.api.addComment(file.id, text); draft = ""; loadComments() } catch (_: ApiError) { } finally { busy = false }
                }
            })
        }
    }

    if (confirmApprove) {
        ConfirmDialog(s.confirmApprove, onDismiss = { confirmApprove = false }) {
            confirmApprove = false
            busy = true
            scope.launch {
                try {
                    state.api.approve(file.id)
                    file = file.copy(status = "approved")
                    state.updateFile(file.id, "approved")
                    toast = s.approvedThanks
                    state.refresh()
                } catch (_: ApiError) { } finally { busy = false }
            }
        }
    }
    if (askRevision) {
        RevisionDialog(onDismiss = { askRevision = false }) { notes ->
            askRevision = false
            busy = true
            scope.launch {
                try {
                    state.api.requestRevision(file.id, notes)
                    file = file.copy(status = "revision_requested", revisionNotes = notes)
                    state.updateFile(file.id, "revision_requested", notes)
                    toast = s.revisionSent
                    state.refresh()
                } catch (_: ApiError) { } finally { busy = false }
            }
        }
    }
}

@Composable
private fun MediaView(f: ClientFile) {
    val shape = RoundedCornerShape(20.dp)
    when (f.type) {
        "Video" -> {
            val src = Media.video(f)
            if (src != null) VideoPlayer(src, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape).background(C.Bg))
        }
        "Image" -> AsyncImage(
            model = Media.image(f.url, 1600),
            contentDescription = f.title(),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().clip(shape).background(C.Bg),
        )
        else -> Box(
            Modifier.fillMaxWidth().height(150.dp).clip(shape).background(C.Surface)
                .clickable { Media.abs(f.url)?.let(::openUrl) },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Description, null, tint = C.Gold, modifier = Modifier.size(44.dp))
                Text(f.fileName ?: f.title(), color = C.Muted, style = T.Footnote, maxLines = 1)
            }
        }
    }
}

private fun fmtTimecode(seconds: Double): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val m = total / 60; val sec = total % 60
    // String.format is JVM/Native-only, not available on Kotlin/Wasm — this
    // produces the identical "m:ss" output on every platform without it.
    return "$m:${sec.toString().padStart(2, '0')}"
}

@Composable
private fun CommentRow(c: Comment, onToggleResolve: (Comment) -> Unit = {}, busy: Boolean = false) {
    val s = LocalStrings.current
    val mine = c.userRole == "client"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (mine) InitialTile(c.userName, 36.dp, warm = true) else FtBadge(36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                c.timestampSeconds?.let { ts ->
                    Text(fmtTimecode(ts), color = C.GoldLabel, style = T.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        modifier = Modifier.height(22.dp).clip(R.Pill).background(C.GoldTint).padding(horizontal = 8.dp, vertical = 3.dp))
                }
                Text(if (mine) (c.userName ?: "—") else s.team, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(fmtDate(c.createdAt), color = C.Muted, style = T.Caption, maxLines = 1)
            }
            Text(c.text ?: "", color = C.Ink, style = T.BodyS)
        }
        if (c.timestampSeconds != null) {
            Box(
                Modifier.size(24.dp).clip(CircleShape)
                    .background(if (c.resolved != 0) C.Green else Color.Transparent)
                    .border(1.5.dp, if (c.resolved != 0) Color.Transparent else C.Muted, CircleShape)
                    .clickable(enabled = !busy) { onToggleResolve(c) },
                contentAlignment = Alignment.Center,
            ) {
                if (c.resolved != 0) Icon(Ic.CheckBold, null, tint = C.Bg, modifier = Modifier.size(13.dp))
            }
        }
    }
}
