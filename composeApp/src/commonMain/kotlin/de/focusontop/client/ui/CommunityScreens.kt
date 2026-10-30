package de.focusontop.client.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.focusontop.client.data.ApiError
import de.focusontop.client.data.AppState
import de.focusontop.client.data.CAuthor
import de.focusontop.client.data.CComment
import de.focusontop.client.data.CPerson
import de.focusontop.client.data.CPost
import de.focusontop.client.data.CProfile
import de.focusontop.client.data.CStoryGroup
import de.focusontop.client.data.Media
import de.focusontop.client.data.Screen
import de.focusontop.client.i18n.LocalLang
import de.focusontop.client.i18n.tr
import de.focusontop.client.platform.PickedFile
import de.focusontop.client.platform.VideoPlayer
import de.focusontop.client.platform.nowEpochMs
import de.focusontop.client.platform.rememberFilePicker
import kotlinx.coroutines.launch

/* ───────────── shared pieces ───────────── */

/** Avatar of a post author: the FT badge for the team, the client's picture / initial otherwise. */
@Composable
fun AuthorAvatar(a: CAuthor, size: Dp) {
    if (a.team) FtBadge(size) else InitialTile(a.name, size, a.avatar, warm = true)
}

/** The photo, or the video (poster first, plays on tap / when [autoPlay]). */
@Composable
fun PostMedia(p: CPost, modifier: Modifier, autoPlay: Boolean = false, fit: Boolean = false) {
    var playing by remember(p.id) { mutableStateOf(autoPlay) }
    Box(modifier.background(placeholderBrush(p.id)), contentAlignment = Alignment.Center) {
        if (p.mediaType == "Video") {
            if (playing) Media.abs(p.mediaUrl)?.let { VideoPlayer(it, Modifier.fillMaxSize()) }
            else Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)).clickable { playing = true },
                contentAlignment = Alignment.Center,
            ) { Icon(Ic.Play, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        } else {
            AsyncImage(
                model = Media.image(p.mediaUrl, 1200), contentDescription = p.caption,
                contentScale = if (fit) ContentScale.Fit else ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Figma component "Post card": full-bleed 390, header (avatar 34 · name + badge · meta · ⋯),
 * media 4:5, actions (heart / comment / send 26 with counts, bookmark at the end),
 * caption with bold author, comments link, relative time.
 */
@Composable
fun PostCard(state: AppState, p: CPost, onOpen: () -> Unit) {
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var saved by remember(p.id) { mutableStateOf(false) }
    val deleted = tr("Beitrag gelöscht", "Post deleted", "حُذف المنشور")
    val author = if (p.author.team) "focusontop" else p.author.name
    Column(Modifier.fillMaxWidth().background(C.Surface)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(R.Pill).clickable { state.go(Screen.ClientProfile(p.author.id ?: "team")) }) { AuthorAvatar(p.author, 34.dp) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clickable { state.go(Screen.ClientProfile(p.author.id ?: "team")) }, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(author, color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    if (p.author.team) Icon(Ic.BadgeCheck, null, tint = C.GoldLabel, modifier = Modifier.size(14.dp))
                }
                Text(
                    listOfNotNull(relativeTime(lang, p.createdAt, nowEpochMs()), if (p.visibility == "public" && !p.author.team) tr("Öffentlich", "Public", "عام") else null).joinToString(" · "),
                    color = C.Muted, style = T.Caption, maxLines = 1,
                )
            }
            Box {
                Icon(Ic.More, tr("Mehr", "More", "المزيد"), tint = C.Ink, modifier = Modifier.size(36.dp).clip(R.Pill).clickable { menu = true }.padding(7.dp))
                DropdownMenu(menu, { menu = false }, containerColor = C.Surface, shape = RoundedCornerShape(R.Menu), border = androidx.compose.foundation.BorderStroke(1.dp, C.Separator)) {
                    if (p.mine) DropdownMenuItem(leadingIcon = { Icon(Ic.Trash, null, tint = C.Red, modifier = Modifier.size(22.dp)) }, text = { Text(tr("Beitrag löschen", "Delete post", "حذف المنشور"), color = C.Red, style = T.BodyS) }, onClick = {
                        menu = false
                        scope.launch { runCatching { state.api.deletePost(p.id) }.onSuccess { state.removePost(p.id); state.showToast(deleted) } }
                    })
                    DropdownMenuItem(leadingIcon = { Icon(Ic.External, null, tint = C.Ink, modifier = Modifier.size(22.dp)) }, text = { Text(tr("Öffnen", "Open", "فتح"), color = C.Ink, style = T.BodyS) }, onClick = { menu = false; onOpen() })
                }
            }
        }
        PostMedia(p, Modifier.fillMaxWidth().aspectRatio(390f / 488f).clickable(onClick = onOpen))
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            PostAction(if (p.liked) Ic.HeartFill else Ic.Heart, p.likes, if (p.liked) C.Gold else C.Ink) { state.toggleLike(p) }
            PostAction(Ic.Chat, p.comments, C.Ink, onOpen)
            PostAction(Ic.Send, 0, C.Ink, onOpen)
            Spacer(Modifier.weight(1f))
            Icon(Ic.Bookmark, tr("Speichern", "Save", "حفظ"), tint = if (saved) C.GoldLabel else C.Ink,
                modifier = Modifier.size(26.dp).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { saved = !saved })
        }
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (p.caption.isNotBlank()) Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.SemiBold)); append(author); pop()
                    append(" "); append(p.caption)
                },
                color = C.Ink, style = T.BodyS, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable(onClick = onOpen),
            )
            if (p.tags.isNotEmpty()) TagLine(state, p)
            if (p.comments > 0) Text(
                tr("Alle ${p.comments} Kommentare ansehen", "View all ${p.comments} comments", "عرض كل التعليقات (${p.comments})"),
                color = C.Muted, style = T.BodyS, modifier = Modifier.clickable(onClick = onOpen),
            )
            Text(relativeTime(lang, p.createdAt, nowEpochMs()), color = C.Faint, style = T.Caption)
        }
    }
}

/** Post action: icon 26, count beside it (Body S semibold) — no pill, no label. */
@Composable
fun PostAction(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
        if (count > 0) Text(formatCount(count), color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold)
    }
}

/** 1248 → "1.248", 12400 → "12,4 Tsd." style compact counts (German grouping, as in Figma). */
fun formatCount(n: Int): String = when {
    n >= 10_000 -> { val k = n / 100; "${k / 10},${k % 10} Tsd." }
    n >= 1000 -> "${n / 1000}.${(n % 1000).toString().padStart(3, '0')}"
    else -> n.toString()
}

@Composable
private fun TagLine(state: AppState, p: CPost) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(p.tags, key = { it.id }) { t ->
            Text(
                "@${t.name}", color = C.GoldLabel, style = T.Footnote, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { state.go(Screen.ClientProfile(t.id)) }.padding(horizontal = 2.dp, vertical = 2.dp),
            )
        }
    }
}

/**
 * Stories row (Figma "Stories"): circle ring 70 — gold 2 pt when unseen, separator 1.5 pt when seen —
 * avatar 60 inside, caption label. "Deine Story" carries a gold + badge 22.
 */
@Composable
fun CommunityStoriesRow(state: AppState) {
    val groups = state.stories
    val myId = state.user?.id ?: state.home?.user?.id
    val mine = groups.indexOfFirst { it.author.id != null && it.author.id == myId }
    val camera = { state.go(if (de.focusontop.client.platform.inAppCameraAvailable) Screen.StoryCamera else Screen.Compose("story")) }
    LazyRow(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StoryItem(tr("Deine Story", "Your story", "قصتك"), ring = if (mine >= 0) StoryRing.Unseen else StoryRing.None, muted = true,
                onClick = { if (mine >= 0) state.go(Screen.Stories(groups, mine)) else camera() },
                badge = { Box(Modifier.size(22.dp).clip(R.Pill).background(C.Gold).border(2.5.dp, C.Surface, R.Pill).clickable(onClick = camera), contentAlignment = Alignment.Center) { Icon(Ic.Plus, null, tint = C.OnAccent, modifier = Modifier.size(12.dp)) } },
            ) { if (mine >= 0) StoryCover(groups[mine]) else InitialTile(state.home?.user?.company?.ifBlank { null } ?: state.user?.name, 60.dp, state.home?.user?.avatar) }
        }
        if (groups.none { it.author.team }) item(key = "team-profile") {
            StoryItem("focusontop", ring = StoryRing.Seen, onClick = { state.go(Screen.ClientProfile("team")) }) { FtBadge(60.dp) }
        }
        groups.forEachIndexed { i, g ->
            if (i == mine) return@forEachIndexed
            item(key = "sg-" + (g.author.id ?: "team")) {
                StoryItem(if (g.author.team) "focusontop" else g.author.name, ring = if (g.allSeen) StoryRing.Seen else StoryRing.Unseen, onClick = { state.go(Screen.Stories(groups, i)) }) { StoryCover(g) }
            }
        }
    }
}

enum class StoryRing { None, Seen, Unseen }

@Composable
private fun StoryItem(label: String, ring: StoryRing, muted: Boolean = false, onClick: () -> Unit, badge: (@Composable () -> Unit)? = null, cover: @Composable () -> Unit) {
    Column(Modifier.width(70.dp).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(70.dp)) {
            Box(
                Modifier.fillMaxSize().then(when (ring) {
                    StoryRing.Unseen -> Modifier.border(2.dp, C.Gold, R.Pill)
                    StoryRing.Seen -> Modifier.border(1.5.dp, C.Separator, R.Pill)
                    StoryRing.None -> Modifier
                }).padding(5.dp).clip(R.Pill),
                contentAlignment = Alignment.Center,
            ) { cover() }
            if (badge != null) Box(Modifier.align(Alignment.BottomEnd).offset(x = (-2).dp, y = (-2).dp)) { badge() }
        }
        Text(label, color = if (muted) C.Muted else C.Ink, style = T.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StoryCover(g: CStoryGroup) {
    val first = g.items.firstOrNull { !it.seen } ?: g.items.firstOrNull()
    Box(Modifier.fillMaxSize().background(placeholderBrush(first?.id ?: g.author.name)), contentAlignment = Alignment.Center) {
        if (first?.mediaType == "Image") AsyncImage(model = Media.image(first.mediaUrl, 300), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else if (g.author.team) FtBadge(60.dp)
        else Icon(Ic.Play, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
    }
}

/* ───────────── story viewer ───────────── */

@Composable
fun StoriesScreen(state: AppState, groups: List<CStoryGroup>, start: Int) {
    var g by remember { mutableStateOf(start.coerceIn(0, (groups.size - 1).coerceAtLeast(0))) }
    val group = groups.getOrNull(g)
    var i by remember(g) { mutableStateOf(group?.items?.indexOfFirst { !it.seen }?.takeIf { it >= 0 } ?: 0) }
    val progress = remember { Animatable(0f) }
    val lang = LocalLang.current

    fun next() {
        val items = groups.getOrNull(g)?.items.orEmpty()
        if (i < items.lastIndex) i++ else if (g < groups.lastIndex) g++ else state.back()
    }
    fun prev() { if (i > 0) i-- else if (g > 0) g-- }

    val item = group?.items?.getOrNull(i)
    LaunchedEffect(g, i) {
        if (item == null) { state.back(); return@LaunchedEffect }
        if (!item.seen) state.markStorySeen(item.id)
        progress.snapTo(0f)
        progress.animateTo(1f, tween(if (item.mediaType == "Video") 20_000 else 6_000, easing = LinearEasing))
        next()
    }
    // Figma 23 · Story-Viewer: progress segments · author row (avatar 64, name, time, ×) · story stage
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (item != null && group != null) {
            Row(Modifier.fillMaxWidth().height(2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                group.items.forEachIndexed { k, _ ->
                    Box(Modifier.weight(1f).fillMaxHeight().background(C.Surface2)) {
                        val f = when { k < i -> 1f; k == i -> progress.value; else -> 0f }
                        Box(Modifier.fillMaxHeight().fillMaxWidth(f).background(C.Gold))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(64.dp).clip(R.Pill).border(1.dp, C.StrokeGlass, R.Pill).clickable { state.go(Screen.ClientProfile(group.author.id ?: "team")) }) { AuthorAvatar(group.author, 64.dp) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (group.author.team) "Focus On Top" else group.author.name, color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(relativeTime(lang, item.createdAt, nowEpochMs()), color = C.Muted, style = T.Micro)
                }
                Icon(Ic.X, tr("Schließen", "Close", "إغلاق"), tint = C.Ink, modifier = Modifier.size(22.dp).clickable { state.back() })
            }
            Box(Modifier.fillMaxWidth().weight(1f).background(C.Surface2)) {
                androidx.compose.runtime.key(item.id) { PostMedia(item, Modifier.fillMaxSize(), autoPlay = true) }
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().pointerInput(g, i) { detectTapGestures { prev() } })
                    Box(Modifier.weight(2f).fillMaxHeight().pointerInput(g, i) { detectTapGestures { next() } })
                }
            }
            if (item.caption.isNotBlank()) Text(item.caption, color = C.Ink, style = T.BodyS, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (item.tags.isNotEmpty()) TagLine(state, item)
        }
    }
}

/* ───────────── compose a post / story ───────────── */

@Composable
fun ComposeScreen(state: AppState, kind: String) {
    val scope = rememberCoroutineScope()
    val story = kind == "story"
    var mediaUrl by remember { mutableStateOf<String?>(null) }
    var mediaType by remember { mutableStateOf("Image") }
    var uploading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var caption by remember { mutableStateOf("") }
    var public by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var tagQuery by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf<List<CPerson>>(emptyList()) }
    val tags = remember { mutableStateListOf<CPerson>() }
    val failed = tr("Hochladen fehlgeschlagen", "Upload failed", "تعذّر الرفع")
    val done = if (story) tr("Story veröffentlicht", "Story published", "نُشرت قصتك") else tr("Beitrag veröffentlicht", "Post published", "نُشر منشورك")
    val accountPrivate = state.me?.private ?: true

    val picker = rememberFilePicker { files: List<PickedFile> ->
        val f = files.firstOrNull { it.mimeType.startsWith("image/") || it.mimeType.startsWith("video/") } ?: files.firstOrNull() ?: return@rememberFilePicker
        uploading = true; progress = 0f
        scope.launch {
            try {
                val blob = state.uploadBlob(f) { sent -> progress = sent.toFloat() / f.size.coerceAtLeast(1) }
                mediaUrl = blob.url
                mediaType = if (f.mimeType.startsWith("video/") || (blob.mimeType ?: "").startsWith("video/")) "Video" else "Image"
            } catch (_: Exception) { state.showToast(failed) } finally { uploading = false }
        }
    }
    LaunchedEffect(tagQuery) {
        matches = if (tagQuery.trim().length < 2) emptyList()
        else runCatching { state.api.people(tagQuery.trim()) }.getOrElse { emptyList() }.filter { p -> tags.none { it.id == p.id } }
    }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopBar(if (story) tr("Neue Story", "New story", "قصة جديدة") else tr("Neuer Beitrag", "New post", "منشور جديد"), onBack = { state.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // "Media selection": bg/secondary + separator, radius 20, padding 24, gap 16
            Box(
                Modifier.fillMaxWidth().then(if (mediaUrl == null && !uploading) Modifier.height(188.dp) else Modifier.aspectRatio(if (story) 0.75f else 390f / 488f))
                    .clip(R.CardShape).background(C.Surface).border(1.dp, C.Separator, R.CardShape).clickable(enabled = !uploading) { picker.pickMedia() },
                contentAlignment = Alignment.Center,
            ) {
                val url = mediaUrl
                when {
                    uploading -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(progress = { progress }, color = C.Gold, trackColor = C.Surface2)
                        Text("${(progress * 100).toInt()}%", color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold)
                    }
                    url == null -> Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Ic.ImagePlus, null, tint = C.Ink, modifier = Modifier.size(36.dp))
                        Text(tr("Foto oder Video wählen", "Choose a photo or video", "اختر صورة أو فيديو"), color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        Text(tr("z. B. vom Dreh, hinter den Kulissen", "e.g. from the shoot, behind the scenes", "مثلًا من التصوير أو الكواليس"), color = C.Muted, style = T.Caption, textAlign = TextAlign.Center)
                    }
                    else -> PostMedia(CPost(id = url, mediaUrl = url, mediaType = mediaType), Modifier.fillMaxSize())
                }
            }
            Box(Modifier.fillMaxWidth().height(112.dp).clip(R.FieldShape).background(C.GlassStrong).border(1.dp, C.StrokeGlass, R.FieldShape).padding(16.dp)) {
                if (caption.isEmpty()) Text(if (story) tr("Kurzer Text (optional)", "Short text (optional)", "نص قصير (اختياري)") else tr("Erzähl etwas über das Projekt oder deine Erfahrung …", "Say something about the project or your experience …", "اكتب شيئًا عن المشروع أو تجربتك…"), color = C.Muted, style = T.BodyS)
                BasicTextField(caption, { if (it.length <= 2200) caption = it }, textStyle = T.BodyS.copy(color = C.Ink), cursorBrush = SolidColor(C.Gold), modifier = Modifier.fillMaxSize())
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FtField(tr("Kunden markieren", "Tag clients", "الإشارة إلى عملاء"), tagQuery, { tagQuery = it },
                    placeholder = tr("Name suchen (nur öffentliche Konten)", "Search a name (public accounts)", "ابحث بالاسم (الحسابات العامة فقط)"), trailing = Ic.Search)
                matches.take(5).forEach { p ->
                    Row(Modifier.fillMaxWidth().clip(R.FieldShape).clickable { tags.add(p); tagQuery = "" }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InitialTile(p.name, 36.dp, p.avatar, warm = true)
                        Text(p.name, color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (tags.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tags.toList(), key = { it.id }) { t ->
                        Row(Modifier.height(30.dp).clip(R.Pill).background(C.GoldTint).border(1.dp, C.Gold, R.Pill).clickable { tags.remove(t) }.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("@${t.name}", color = C.GoldLabel, style = T.Caption)
                            Icon(Ic.X, null, tint = C.GoldLabel, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.dp, C.StrokeGlass, R.CardShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Ic.Globe, null, tint = C.Ink, modifier = Modifier.size(22.dp))
                    Text(tr("Für alle sichtbar", "Visible to everyone", "مرئي للجميع"), color = C.Ink, style = T.BodyS, modifier = Modifier.weight(1f))
                    if (accountPrivate) FtToggle(public) { public = it } else FtToggle(true) {}
                }
                Text(
                    if (accountPrivate) tr("Dein Konto ist privat. Nur dieser Beitrag wird für alle Kunden sichtbar.", "Your account is private. Only this post becomes visible to all clients.", "حسابك خاص. هذا المنشور وحده سيظهر لكل العملاء.")
                    else tr("Dein Konto ist öffentlich – alle Kunden können diesen Beitrag sehen.", "Your account is public – every client can see this.", "حسابك عام، وكل العملاء يمكنهم رؤية هذا."),
                    color = C.Muted, style = T.Caption,
                )
            }
            PrimaryButton(if (story) tr("Story teilen", "Share story", "نشر القصة") else tr("Veröffentlichen", "Publish", "نشر"), onClick = {
                val url = mediaUrl ?: return@PrimaryButton
                busy = true
                scope.launch {
                    try {
                        state.api.publish(kind, url, mediaType, caption.trim(), tags.map { it.id }, public)
                        state.showToast(done); state.refreshCommunity(); state.back()
                    } catch (_: ApiError) { state.showToast(failed) } finally { busy = false }
                }
            }, enabled = mediaUrl != null && !uploading, loading = busy)
        }
    }
}

/* ───────────── one post + comments ───────────── */

/** Figma 24 · Community · Beitrag: post card, comment list (avatar 36 · name / time · text · Antworten), composer. */
@Composable
fun PostScreen(state: AppState, id: String, initial: CPost?) {
    val scope = rememberCoroutineScope()
    val lang = LocalLang.current
    var post by remember { mutableStateOf(initial) }
    val comments = remember { mutableStateListOf<CComment>() }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var gone by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val live = state.feed?.firstOrNull { it.id == id } ?: state.me?.posts?.firstOrNull { it.id == id }
    LaunchedEffect(id) {
        runCatching { state.api.communityPost(id) }.onSuccess { post = it }.onFailure { if ((it as? ApiError)?.status == 404) gone = true }
        runCatching { state.api.postComments(id) }.onSuccess { comments.clear(); comments.addAll(it) }
    }
    val p = live ?: post
    fun send() {
        val text = draft.trim(); if (text.isEmpty() || busy) return
        busy = true
        scope.launch {
            try {
                val c = state.api.addPostComment(id, text)
                comments.add(c); draft = ""
                p?.let { state.replacePost(it.copy(comments = it.comments + 1)) }
            } catch (_: ApiError) { } finally { busy = false }
        }
    }
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopBar(tr("Beitrag", "Post", "منشور"), onBack = { state.back() }, actions = {
            if (p?.mine == true) Box {
                HeaderIcon(Ic.MoreH, tr("Mehr", "More", "المزيد")) { menu = true }
                DropdownMenu(menu, { menu = false }, containerColor = C.Surface, shape = RoundedCornerShape(R.Menu), border = androidx.compose.foundation.BorderStroke(1.dp, C.Separator)) {
                    DropdownMenuItem(leadingIcon = { Icon(Ic.Trash, null, tint = C.Red, modifier = Modifier.size(22.dp)) }, text = { Text(tr("Beitrag löschen", "Delete post", "حذف المنشور"), color = C.Red, style = T.BodyS) }, onClick = {
                        menu = false
                        scope.launch { runCatching { state.api.deletePost(id) }.onSuccess { state.removePost(id); state.back() } }
                    })
                }
            }
        })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (gone) item { EmptyState(tr("Dieser Beitrag ist nicht mehr verfügbar.", "This post is no longer available.", "هذا المنشور لم يعد متاحًا.")) }
            if (p != null) item { PostCard(state, p) {} }
            if (p != null && comments.isEmpty()) item { Text(tr("Noch keine Kommentare.", "No comments yet.", "لا توجد تعليقات بعد."), color = C.Muted, style = T.BodyS, modifier = Modifier.padding(horizontal = 16.dp)) }
            if (comments.isNotEmpty()) item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    comments.toList().forEach { c ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(36.dp).clip(R.Pill).clickable { c.author.id?.let { state.go(Screen.ClientProfile(it)) } }) { AuthorAvatar(c.author, 36.dp) }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (c.author.team) "focusontop" else c.author.name, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(relativeTime(lang, c.createdAt, nowEpochMs()), color = C.Muted, style = T.Caption, maxLines = 1)
                                }
                                Text(c.text, color = C.Ink, style = T.BodyS)
                                Text(tr("Antworten", "Reply", "رد"), color = C.Muted, style = T.Caption, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { draft = "@" + (if (c.author.team) "focusontop" else c.author.name) + " " })
                            }
                            if (c.mine || p?.mine == true) Icon(Ic.X, tr("Löschen", "Delete", "حذف"), tint = C.Muted, modifier = Modifier.padding(top = 4.dp).size(16.dp).clickable {
                                scope.launch { runCatching { state.api.deletePostComment(c.id) }.onSuccess { comments.remove(c); post = post?.copy(comments = (post?.comments ?: 1) - 1) } }
                            })
                        }
                    }
                }
            }
        }
        if (p != null) Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(48.dp).clip(R.Pill).background(C.GlassStrong).border(1.dp, C.StrokeGlass, R.Pill).padding(12.dp), contentAlignment = Alignment.CenterStart) {
                if (draft.isEmpty()) Text(tr("Kommentar hinzufügen …", "Add a comment …", "أضف تعليقًا…"), color = C.Muted, style = T.Footnote)
                androidx.compose.foundation.text.BasicTextField(draft, { draft = it }, maxLines = 3, textStyle = T.Footnote.copy(color = C.Ink),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(C.Gold), modifier = Modifier.fillMaxWidth())
            }
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = C.Gold, strokeWidth = 2.dp)
            else Icon(Ic.ArrowUp, tr("Senden", "Send", "إرسال"), tint = if (draft.isBlank()) C.Muted else C.Ink, modifier = Modifier.size(22.dp).clickable { send() })
        }
    }
}

/* ───────────── 25 · offizielles Profil  /  25b · privates Kundenprofil ───────────── */

@Composable
fun ClientProfileScreen(state: AppState, id: String) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<CProfile?>(null) }
    var error by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    LaunchedEffect(id) { runCatching { state.api.profile(id) }.onSuccess { profile = it }.onFailure { error = true } }
    val pr = profile
    val team = id == "team" || pr?.id == "team"
    val locked = pr != null && !pr.me && !team && pr.private && !pr.isFollowing
    fun toggleFollow() {
        val cur = pr ?: return
        scope.launch {
            runCatching { state.api.follow(cur.id) }.onSuccess { f ->
                profile = cur.copy(isFollowing = f.following, followers = cur.followers + if (f.following) 1 else -1); state.refreshCommunity()
            }
        }
    }
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(when { team -> "Focus On Top"; locked -> tr("Privates Konto", "Private account", "حساب خاص"); else -> pr?.name ?: "" }, onBack = { state.back() })
        when {
            error -> EmptyState(tr("Profil nicht gefunden", "Profile not found", "الحساب غير موجود"))
            pr == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = C.Gold) }
            else -> {
                val posts = when (tab) { 1 -> pr.posts.filter { it.mediaType == "Video" }; 2 -> pr.posts.filter { it.tags.isNotEmpty() }; else -> pr.posts }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item(span = { GridItemSpan(3) }) {
                        Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(if (team) 16.dp else 24.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                when {
                                    team -> Box(Modifier.size(64.dp).clip(R.Pill).background(C.Surface).border(2.dp, C.Gold, R.Pill), contentAlignment = Alignment.Center) { Wordmark(13.dp) }
                                    locked -> Box(Modifier.size(64.dp).clip(R.Pill).background(C.Surface2).border(1.dp, C.StrokeGlass, R.Pill), contentAlignment = Alignment.Center) {
                                        Icon(Ic.UserRound, null, tint = C.Ink, modifier = Modifier.size(22.dp))
                                    }
                                    else -> Box(Modifier.size(64.dp).clip(R.Pill).border(1.dp, C.StrokeGlass, R.Pill)) { InitialTile(pr.name, 64.dp, pr.avatar) }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(when { team -> "Focus On Top"; locked -> tr("Privates Konto", "Private account", "حساب خاص"); else -> pr.name }, color = C.Ink, style = T.Title3,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                        if (team) Icon(Ic.BadgeCheckT, null, tint = C.Turquoise, modifier = Modifier.size(20.dp))
                                    }
                                    if (team) Text(tr("Kreativagentur für Foto, Video & Social Media", "Creative agency for photo, video & social media", "وكالة إبداعية للتصوير والفيديو والسوشيال ميديا"), color = C.Muted, style = T.Footnote, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    else if (!locked) pr.since?.take(4)?.let { Text(tr("Kunde seit $it", "Client since $it", "عميل منذ $it"), color = C.Muted, style = T.Footnote) }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ProfileStat(if (locked) null else pr.posts.size, tr("Beiträge", "Posts", "منشورات"), Modifier.weight(1f))
                                ProfileStat(if (locked) null else pr.followers, tr("Follower", "Followers", "متابِعون"), Modifier.weight(1f))
                                ProfileStat(if (locked) null else pr.following, tr("Gefolgt", "Following", "يتابع"), Modifier.weight(1f))
                            }
                            if (!pr.me) {
                                if (team) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (pr.isFollowing) SecondaryButton(tr("Gefolgt", "Following", "تتابعه"), onClick = { toggleFollow() }, modifier = Modifier.weight(1f))
                                    else PrimaryButton(tr("Folgen", "Follow", "متابعة"), onClick = { toggleFollow() }, modifier = Modifier.weight(1f))
                                    SecondaryButton(tr("Nachricht", "Message", "رسالة"), onClick = { state.go(Screen.Chat()) }, modifier = Modifier.weight(1f))
                                } else if (pr.isFollowing) SecondaryButton(tr("Gefolgt", "Following", "تتابعه"), onClick = { toggleFollow() }, modifier = Modifier.fillMaxWidth())
                                else PrimaryButton(tr("Folgen", "Follow", "متابعة"), onClick = { toggleFollow() })
                            }
                            if (locked) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
                                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Icon(Ic.LockKeyholeThin, null, tint = C.Muted, modifier = Modifier.size(36.dp))
                                    Text(tr("Dieses Konto ist privat.", "This account is private.", "هذا الحساب خاص."), color = C.Ink, style = T.Title3, textAlign = TextAlign.Center)
                                }
                                Text(tr("Kunden folgen nur den Arbeiten – Nachrichten gibt es nur mit dem Team.", "Clients only follow each other's work – messages are with the team only.", "يتابع العملاء أعمال بعضهم فقط، والمراسلة مع الفريق وحده."),
                                    color = C.Muted, style = T.Caption, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            } else Row(Modifier.fillMaxWidth().height(42.dp)) {
                                listOf(Ic.GridL, Ic.ClapperL, Ic.Tag).forEachIndexed { i, ic ->
                                    Column(Modifier.weight(1f).fillMaxHeight().clickable { tab = i }.padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(ic, null, tint = if (tab == i) C.Ink else C.Muted, modifier = Modifier.size(24.dp))
                                        Box(Modifier.width(40.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(if (tab == i) C.Ink else Color.Transparent))
                                    }
                                }
                            }
                        }
                    }
                    if (!locked) {
                        if (posts.isEmpty()) item(span = { GridItemSpan(3) }) { EmptyState(tr("Noch keine Beiträge", "No posts yet", "لا توجد منشورات بعد")) }
                        items(posts.size) { k -> val p = posts[k]; PostTile(p) { state.go(Screen.Post(p.id, p)) } }
                    }
                }
            }
        }
    }
}

/** Figma "Profile statistic": Title 2 semibold + Micro; "—" while hidden. */
@Composable
fun ProfileStat(n: Int?, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(n?.let { formatCount(it) } ?: "—", color = C.Ink, style = T.Title2, textAlign = TextAlign.Center, maxLines = 1)
        Text(label, color = C.Muted, style = T.Micro, maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Post tile 116 × 150, radius 10; videos carry a 16 play glyph top-right. */
@Composable
fun PostTile(p: CPost, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(116f / 150f).clip(R.ThumbShape).background(C.Surface2).clickable(onClick = onClick)) {
        if (p.mediaType == "Image") AsyncImage(model = Media.image(p.mediaUrl, 400), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Ic.Play, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).size(16.dp))
    }
}

/* ───────────── discover clients ───────────── */

@Composable
fun DiscoverScreen(state: AppState) {
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var people by remember { mutableStateOf<List<CPerson>?>(null) }
    LaunchedEffect(q) { people = runCatching { state.api.people(q.trim()) }.getOrElse { emptyList() } }
    Column(Modifier.fillMaxSize().screenBg().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(tr("Kunden entdecken", "Discover clients", "اكتشف العملاء"), onBack = { state.back() })
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(C.Surface.copy(alpha = 0.7f)).border(1.dp, C.Border, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Ic.Search, null, tint = C.Muted, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (q.isEmpty()) Text(tr("Name suchen", "Search a name", "ابحث بالاسم"), color = C.Faint, style = T.Body)
                BasicTextField(q, { q = it }, singleLine = true, textStyle = T.Body.copy(color = C.Ink), cursorBrush = SolidColor(C.Gold), modifier = Modifier.fillMaxWidth())
            }
        }
        val list = people
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (list == null) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = C.Gold) } }
            else if (list.isEmpty()) item { EmptyState(tr("Keine öffentlichen Konten gefunden", "No public accounts found", "لا توجد حسابات عامة"), tr("Nur Kunden mit öffentlichem Konto erscheinen hier.", "Only clients with a public account appear here.", "تظهر هنا فقط حسابات العملاء العامة.")) }
            else items(list, key = { it.id }) { p ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { state.go(Screen.ClientProfile(p.id)) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    InitialTile(p.name, 50.dp, p.avatar, warm = true)
                    Spacer(Modifier.width(12.dp))
                    Text(p.name, color = C.Ink, style = T.Headline, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (p.following) GhostButton(tr("Gefolgt", "Following", "تتابعه"), {
                        scope.launch { runCatching { state.api.follow(p.id) }.onSuccess { f -> people = people?.map { if (it.id == p.id) it.copy(following = f.following) else it }; state.refreshCommunity() } }
                    }, small = true)
                    else GoldButton(tr("Folgen", "Follow", "متابعة"), {
                        scope.launch { runCatching { state.api.follow(p.id) }.onSuccess { f -> people = people?.map { if (it.id == p.id) it.copy(following = f.following) else it }; state.refreshCommunity() } }
                    }, small = true)
                }
            }
        }
    }
}

/** Public / private switch for the client's own account (profile + settings). */
@Composable
fun PrivacyRow(state: AppState) {
    val priv = state.me?.private ?: true
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Surface.copy(alpha = 0.85f)).border(1.dp, C.Border.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (priv) Ic.Lock else Ic.Globe, null, tint = C.Gold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (priv) tr("Privates Konto", "Private account", "حساب خاص") else tr("Öffentliches Konto", "Public account", "حساب عام"), color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold)
            Text(
                if (priv) tr("Nur du und das Team sehen deine Beiträge.", "Only you and the team see your posts.", "منشوراتك تظهر لك وللفريق فقط.")
                else tr("Alle Kunden können deinen Arbeiten folgen.", "Every client can follow your work.", "يمكن لكل العملاء متابعة أعمالك."),
                color = C.Muted, style = T.Caption,
            )
        }
        Switch(!priv, { state.setPrivate(!it) }, colors = SwitchDefaults.colors(checkedThumbColor = C.Bg, checkedTrackColor = C.Gold, uncheckedTrackColor = C.Border))
    }
}

/** "Following / Discover" switch above the feed. */
@Composable
fun FeedScopeTabs(state: AppState) {
    var open by remember { mutableStateOf(false) }
    val forYou = tr("Für dich", "For you", "لك")
    val following = tr("Folge ich", "Following", "أتابعهم")
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable { open = true }.padding(vertical = 4.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (state.discover) forYou else following, color = C.Ink, style = T.Title2, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Icon(Ic.ChevronDown, null, tint = C.Ink, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(open, { open = false }, containerColor = C.Surface) {
                listOf(true to forYou, false to following).forEach { (d, label) ->
                    DropdownMenuItem(
                        text = { Text(label, color = if (state.discover == d) C.Gold else C.Ink, fontWeight = FontWeight.SemiBold) },
                        trailingIcon = { if (state.discover == d) Icon(Ic.Check, null, tint = C.Gold, modifier = Modifier.size(18.dp)) },
                        onClick = { state.setFeedScope(d); open = false },
                    )
                }
            }
        }
    }
}
