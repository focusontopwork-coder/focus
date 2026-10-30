package de.focusontop.client.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.focusontop.client.data.ApiError
import de.focusontop.client.data.AppState
import de.focusontop.client.data.Screen
import de.focusontop.client.data.ClientFile
import de.focusontop.client.data.Comment
import de.focusontop.client.data.Media
import de.focusontop.client.i18n.LocalLang
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.i18n.tr
import de.focusontop.client.platform.VideoPlayer
import de.focusontop.client.platform.ReelPlayer
import de.focusontop.client.platform.nowEpochMs
import de.focusontop.client.platform.openUrl
import kotlinx.coroutines.launch

/** Full-screen viewer: swipe through work, approve or ask for a change, comment. */
@Composable
fun ViewerScreen(state: AppState, ids: List<String>, start: Int, title: String?, fallback: ClientFile? = null) {
    val pager = rememberPagerState(initialPage = start.coerceIn(0, (ids.size - 1).coerceAtLeast(0))) { ids.size }
    Box(Modifier.fillMaxSize().background(Color(0xFF07131E))) {
        // swipe up / down like Reels
        androidx.compose.foundation.pager.VerticalPager(pager, Modifier.fillMaxSize(), key = { ids[it] }, beyondViewportPageCount = 1) { page ->
            ViewerPage(state, ids[page], title, if (page == start) fallback else null, active = pager.currentPage == page)
        }
    }
}

@Composable
private fun ViewerPage(state: AppState, id: String, title: String?, fallback: ClientFile?, active: Boolean) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var file by remember(id) { mutableStateOf(state.fileById(id) ?: fallback) }
    var paused by remember(id) { mutableStateOf(false) }
    var showComments by remember(id) { mutableStateOf(false) }
    var askRevision by remember { mutableStateOf(false) }
    var confirmApprove by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val copied = tr("Link kopiert", "Link copied", "تم نسخ الرابط")

    LaunchedEffect(id) { runCatching { state.api.file(id) }.onSuccess { file = it } }
    var versions by remember(id) { mutableStateOf<List<ClientFile>>(emptyList()) }
    LaunchedEffect(id) { runCatching { state.api.fileVersions(id) }.onSuccess { versions = it } }
    LaunchedEffect(active) { if (active) paused = false }
    val playing = active && !paused

    val f = file
    Box(Modifier.fillMaxSize()) {
        if (f == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = C.Gold) }
        } else {
            // ── media
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (f.type) {
                    "Video" -> {
                        // poster underneath while the video loads
                        FileCover(f, Modifier.fillMaxSize(), 1200)
                        Media.video(f)?.let { src ->
                            ReelPlayer(src, playing, Modifier.fillMaxSize())
                        }
                        // tap anywhere to pause / resume
                        Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { paused = !paused })
                        if (paused) Box(
                            Modifier.size(78.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Ic.Play, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
                    }
                    "Image" -> AsyncImage(model = Media.image(f.url, 1600), contentDescription = f.title(), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    else -> {
                        FileCover(f, Modifier.fillMaxSize(), 1200)
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                    }
                }
                if (f.type == "Document" || f.type == "Link") {
                    GoldButton(s.open, { Media.abs(f.url)?.let(::openUrl) }, icon = Ic.External, small = true)
                }
            }
            // shade for text legibility
            if (true) {
                Box(Modifier.fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE07131E)))))
                Box(Modifier.fillMaxWidth().height(140.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color(0x9907131E), Color.Transparent))))
            }

            // ── top bar
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                RailButton(Ic.Back, s.back) { state.back() }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon: ImageVector = when (f.type) { "Video" -> Ic.Video; "Image" -> Ic.Image; else -> Ic.File }
                    Icon(icon, null, tint = C.Ink, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(title ?: when (f.type) { "Video" -> "Reels"; "Image" -> tr("Fotos", "Photos", "صور"); else -> tr("Dokument", "Document", "مستند") }, color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(52.dp))
            }

            // ── side rail
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 14.dp, top = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                val fav = f.id in state.favourites
                RailButton(if (fav) Ic.HeartFill else Ic.Heart, tr("Gefällt", "Like", "إعجاب"), if (fav) C.Gold else Color.White) { state.toggleFavourite(f.id) }
                RailButton(Ic.ChatLines, f.commentCount.takeIf { it > 0 }?.toString() ?: tr("Kommentar", "Comment", "تعليق")) { showComments = true }
                RailButton(Ic.Send, tr("Teilen", "Share", "مشاركة")) { Media.share(f)?.let { clipboard.setText(AnnotatedString(it)); state.showToast(copied) } }
                if (f.type != "Link") RailButton(Ic.Download, tr("Laden", "Save", "تنزيل")) { Media.download(f.url, f.fileName ?: f.name)?.let(::openUrl) }
            }

            // ── bottom info + answer
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 70.dp)) {
                    when (f.status) {
                        "ready_for_review" -> StatusFilter(tr("Wartet auf Freigabe", "Awaiting approval", "بانتظار الموافقة"))
                        "revision_requested" -> StatusFilter(tr("Änderung angefragt", "Change requested", "طُلب تعديل"))
                        "approved" -> StatusFilter(tr("Freigegeben", "Approved", "تمت الموافقة"))
                        else -> StatusFilter(if (f.mine) tr("Dein Upload", "Your upload", "رفعك") else tr("Geliefert", "Delivered", "تم التسليم"))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(relativeTime(LocalLang.current, f.createdAt, nowEpochMs()), color = C.Ink.copy(alpha = 0.8f), style = T.Footnote, maxLines = 1)
                }
                Text(f.title(), color = Color.White, style = T.Headline, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 70.dp))
                f.revisionNotes?.takeIf { it.isNotBlank() }?.let {
                    Text("${s.yourNotes}: $it", color = C.Ink.copy(alpha = 0.85f), style = T.BodyS, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Text(f.size ?: "", color = C.Muted, style = T.Caption)
                // Every delivered photo / video can be answered: approve it, or ask for a change.
                if (!f.mine && f.type != "Link") {
                    Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(1.dp).background(Color.White.copy(alpha = 0.12f)))
                    if (versions.size > 1) {
                        val myLabel = versions.find { it.id == f.id }?.versionLabel ?: ""
                        val otherLabel = versions.lastOrNull { it.versionLabel != myLabel }?.versionLabel
                            ?: versions.firstOrNull()?.versionLabel ?: ""
                        Row(
                            Modifier.padding(top = 8.dp).clickable { state.go(Screen.CompareVersions(f)) },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Ic.Compare, null, tint = C.Gold, modifier = Modifier.size(16.dp))
                            Text(
                                tr("Mit $otherLabel vergleichen", "Compare with $otherLabel", "مقارنة مع $otherLabel"),
                                color = C.Gold, style = T.Footnote, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                        SecondaryButton(
                            if (f.status == "revision_requested" || f.status == "approved") tr("Weitere Änderung", "Another change", "تعديل إضافي") else tr("Änderung", "Change", "تعديل"),
                            onClick = { if (!busy) { showComments = true; askRevision = true } }, modifier = Modifier.weight(1f),
                        )
                        if (f.status == "approved") {
                            Row(
                                Modifier.weight(1f).height(48.dp).clip(R.Pill).background(C.Green.copy(alpha = 0.14f)),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(Ic.CheckCircle, null, tint = C.GreenText, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tr("Freigegeben", "Approved", "تمت الموافقة"), color = C.GreenText, style = T.BodyS, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            PrimaryButton(tr("Freigeben", "Approve", "موافقة"), onClick = { confirmApprove = true }, modifier = Modifier.weight(1f), enabled = !busy, loading = busy)
                        }
                    }
                }
            }
        }

        if (f != null) CommentsSheet(state, f, showComments, revisionMode = askRevision, onSent = { askRevision = false }, onClose = { showComments = false; askRevision = false; scope.launch { runCatching { state.api.file(f.id) }.onSuccess { file = it } } })
    }

    if (confirmApprove && f != null) {
        ConfirmDialog(s.confirmApprove, onDismiss = { confirmApprove = false }) {
            confirmApprove = false
            busy = true
            scope.launch {
                try {
                    state.api.approve(f.id)
                    file = f.copy(status = "approved")
                    state.updateFile(f.id, "approved")
                    state.showToast(s.approvedThanks)
                    state.refresh()
                } catch (_: ApiError) { } finally { busy = false }
            }
        }
    }
}

/** Rail action like Figma 11 (Reels): icon 30, Caption semibold count / label, soft shadow, gap 4. */
@Composable
private fun RailButton(icon: ImageVector, label: String?, tint: Color = Color.White, onClick: () -> Unit) {
    Column(
        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(30.dp))
        if (!label.isNullOrEmpty()) Text(
            label, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1,
            style = T.Caption.copy(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 6f)),
        )
    }
}

/** The conversation between the client and the team about this one reel / photo. */
@Composable
private fun CommentsSheet(state: AppState, f: ClientFile, open: Boolean, revisionMode: Boolean = false, onSent: () -> Unit = {}, onClose: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()
    val comments = remember(f.id) { mutableStateListOf<Comment>() }
    var loaded by remember(f.id) { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(open, f.id) {
        while (open) {
            runCatching { state.api.comments(f.id) }.onSuccess { if (it.size != comments.size || it.lastOrNull()?.id != comments.lastOrNull()?.id) { comments.clear(); comments.addAll(it) } }
            loaded = true
            kotlinx.coroutines.delay(8_000)
        }
    }
    LaunchedEffect(comments.size, open) { if (open && comments.isNotEmpty()) listState.animateScrollToItem(comments.lastIndex) }
    fun send(text: String) {
        if (text.isBlank() || busy) return
        busy = true
        scope.launch {
            try {
                if (revisionMode) {
                    state.api.requestRevision(f.id, text.trim())
                    state.updateFile(f.id, "revision_requested", text.trim())
                    state.showToast(s.revisionSent)
                    state.refresh()
                    onSent()
                } else {
                    state.api.addComment(f.id, text.trim())
                    comments.clear(); comments.addAll(state.api.comments(f.id))
                }
                draft = ""
            } catch (_: ApiError) { } finally { busy = false }
        }
    }
    val quick = listOf(
        tr("Sieht super aus 👍", "Looks great 👍", "رائع جدًا 👍"),
        tr("Kleine Änderung bitte", "A small change, please", "تعديل بسيط من فضلك"),
        tr("Wann geht es online?", "When does it go live?", "متى سيُنشر؟"),
    )
    AnimatedVisibility(open, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose))
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(open, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            // Figma F · "Comments sheet": bg/secondary, radius 34 top, padding 8/0/30/0, gap 16
            Column(
                Modifier.fillMaxWidth().fillMaxHeight(0.72f).clip(R.SheetShape).background(C.Surface)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding().imePadding().padding(top = 8.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(C.Separator))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (revisionMode) tr("Änderung anfordern", "Request change", "طلب تعديل") else tr("Kommentare", "Comments", "التعليقات"), color = C.Ink, style = T.Headline)
                    if (!revisionMode && comments.isNotEmpty()) Text(comments.size.toString(), color = C.Muted, style = T.Headline, fontWeight = FontWeight.Normal)
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                    if (!loaded) CircularProgressIndicator(Modifier.align(Alignment.Center), color = C.Gold)
                    else if (comments.isEmpty()) Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FtBadge(52.dp, dashed = true)
                        Text(
                            if (revisionMode) tr("Beschreibe die Änderungen", "Describe the changes", "صف التغييرات المطلوبة")
                            else tr("Sei der Erste und kommentiere", "Be the first to comment", "كن أول من يعلّق"),
                            color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Text(
                            if (revisionMode) tr("Dein Wunsch geht direkt ans Team.", "Your request goes straight to the team.", "طلبك يصل إلى الفريق فوراً.")
                            else tr("Kommentare sind für alle sichtbar.", "Comments are visible to everyone.", "التعليقات مرئية لجميع المستخدمين."),
                            color = C.Muted, style = T.Footnote, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    else LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)) {
                        items(comments, key = { it.id }) { c ->
                            val isTeam = c.userRole != "client"
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (isTeam) FtBadge(36.dp) else InitialTile(c.userName, 36.dp, warm = true)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(c.userName ?: s.team, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        c.timestampSeconds?.let { t ->
                                            val sec = t.toInt()
                                            Text("${sec / 60}:${(sec % 60).toString().padStart(2, '0')}", color = C.GoldLabel, style = T.Caption, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(relativeTime(lang, c.createdAt, nowEpochMs()), color = C.Muted, style = T.Caption, maxLines = 1)
                                    }
                                    Text(c.text ?: "", color = C.Ink, style = T.BodyS)
                                    Text(tr("Antworten", "Reply", "رد"), color = C.Muted, style = T.Caption, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { draft = "@" + (c.userName ?: s.team) + " " })
                                }
                                if (c.timestampSeconds != null) {
                                    var resolveBusy by remember(c.id) { mutableStateOf(false) }
                                    Box(
                                        Modifier.size(24.dp).clip(CircleShape)
                                            .background(if (c.resolved != 0) C.Green else Color.Transparent)
                                            .border(1.5.dp, if (c.resolved != 0) Color.Transparent else C.Muted, CircleShape)
                                            .clickable(enabled = !resolveBusy) {
                                                resolveBusy = true
                                                scope.launch {
                                                    try { state.api.resolveComment(c.id, c.resolved == 0); comments.clear(); comments.addAll(state.api.comments(f.id)) }
                                                    catch (_: ApiError) { } finally { resolveBusy = false }
                                                }
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (c.resolved != 0) Icon(Ic.CheckBold, null, tint = C.Bg, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
                Box(Modifier.padding(horizontal = 16.dp)) {
                    ComposerBar(draft, { draft = it }, if (revisionMode) s.whatToChangeHint else tr("Kommentar hinzufügen …", "Add a comment …", "أضف تعليقًا…"), busy,
                        leading = { InitialTile(state.user?.name, 32.dp, state.home?.user?.avatar, warm = true) }) { send(draft) }
                }
            }
        }
    }
}

/** Figma F · "Composer": [leading 32] · field 42 (pill, separator hairline, Body S) · gold send 42. */
@Composable
fun ComposerBar(value: String, onChange: (String) -> Unit, placeholder: String, busy: Boolean, leading: (@Composable () -> Unit)? = null, onSend: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (leading != null) leading()
        Box(
            Modifier.weight(1f).height(42.dp).clip(R.Pill).border(1.dp, C.Separator, R.Pill).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) Text(placeholder, color = C.Muted, style = T.BodyS, maxLines = 1)
            BasicTextField(value, onChange, textStyle = T.BodyS.copy(color = C.Ink), cursorBrush = SolidColor(C.Gold), maxLines = 3, modifier = Modifier.fillMaxWidth())
        }
        Box(
            Modifier.size(42.dp).clip(R.Pill).background(if (value.isBlank() || busy) C.Gold.copy(alpha = 0.4f) else C.Gold)
                .clickable(enabled = value.isNotBlank() && !busy, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = C.OnAccent, strokeWidth = 2.dp)
            else Icon(Ic.ArrowUpBold, null, tint = C.OnAccent, modifier = Modifier.size(20.dp))
        }
    }
}

/** Status "Filter" chip on media: gold tint + gold hairline, Caption gold-label. */
@Composable
private fun StatusFilter(text: String) {
    Text(text, color = C.GoldLabel, style = T.Caption, maxLines = 1,
        modifier = Modifier.height(30.dp).clip(R.Pill).background(C.GoldTint).border(1.dp, C.Gold, R.Pill).padding(horizontal = 10.dp, vertical = 7.dp))
}
