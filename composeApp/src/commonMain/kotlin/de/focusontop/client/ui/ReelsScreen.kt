package de.focusontop.client.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.focusontop.client.data.AppState
import de.focusontop.client.data.CPerson
import de.focusontop.client.data.CPost
import de.focusontop.client.data.Media
import de.focusontop.client.data.Screen
import de.focusontop.client.i18n.tr
import de.focusontop.client.platform.ReelPlayer
import de.focusontop.client.platform.openUrl
import de.focusontop.client.platform.shareText
import kotlinx.coroutines.launch

private val shade = Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 6f)

/**
 * Figma 11 · Medien · Reels: full-screen vertical pager of community videos — top / bottom shades,
 * "Reels" Title 2 + camera, action rail (heart / comment / send 30 + Caption semibold counts),
 * author row with outlined follow pill, caption, original audio, 2 pt progress line.
 */
@Composable
fun ReelsScreen(state: AppState, onGrid: () -> Unit, onUpload: () -> Unit) {
    val reels = state.feed.orEmpty().filter { it.kind == "post" && it.mediaType == "Video" && it.mediaUrl != null }
    var share by remember { mutableStateOf<CPost?>(null) }
    var insights by remember { mutableStateOf<CPost?>(null) }
    val camera = { state.go(if (de.focusontop.client.platform.inAppCameraAvailable) Screen.StoryCamera else Screen.Compose("story")) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (reels.isEmpty()) {
            Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding()) {
                ReelsHeader(camera, onGrid)
                EmptyState(tr("Noch keine Reels", "No reels yet", "لا توجد ريلز بعد"),
                    tr("Videos aus der Community erscheinen hier.", "Videos from the community appear here.", "تظهر هنا فيديوهات المجتمع."), icon = Ic.Clapper)
            }
            return@Box
        }
        val pager = rememberLazyListState()
        val current by remember { derivedStateOf { pager.firstVisibleItemIndex } }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val pageH = maxHeight
            LazyColumn(state = pager, modifier = Modifier.fillMaxSize(), flingBehavior = rememberSnapFlingBehavior(lazyListState = pager)) {
                items(reels.size, key = { reels[it].id }) { i ->
                    Box(Modifier.fillMaxWidth().height(pageH)) {
                        ReelPage(state, reels[i], playing = i == current && share == null && insights == null,
                            onShare = { share = reels[i] }, onInsights = { insights = reels[i] })
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().statusBarsPadding()) { ReelsHeader(camera, onGrid, dark = true) }
        share?.let { p -> ShareSheet(state, p) { share = null } }
        insights?.let { p -> ReelInsightsSheet(state, p) { insights = null } }
    }
}

@Composable
private fun ReelsHeader(onCamera: () -> Unit, onGrid: () -> Unit, dark: Boolean = false) {
    val fg = if (dark) Color.White else C.Ink
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp).height(32.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(tr("Reels", "Reels", "ريلز"), color = fg, style = T.Title2.copy(shadow = if (dark) shade else null),
            modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onGrid))
        Icon(Ic.Camera, tr("Kamera", "Camera", "الكاميرا"), tint = fg,
            modifier = Modifier.size(26.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCamera))
    }
}

@Composable
private fun ReelPage(state: AppState, p: CPost, playing: Boolean, onShare: () -> Unit, onInsights: () -> Unit) {
    val scope = rememberCoroutineScope()
    var following by remember(p.id) { mutableStateOf<Boolean?>(null) }
    var progress by remember(p.id) { mutableStateOf(0f) }
    LaunchedEffect(playing) { if (!playing) progress = 0f }
    // Count a view once the reel has actually held the screen for a moment (not just a quick scroll-past).
    LaunchedEffect(playing, p.id) {
        if (playing) {
            kotlinx.coroutines.delay(1_500)
            runCatching { state.api.recordView(p.id) }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Media.abs(p.mediaUrl)?.let { ReelPlayer(it, playing = playing, modifier = Modifier.fillMaxSize()) }
            ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = C.Gold) }
        // shades: top 140 (45% → 0), bottom 420 (0 → 75%)
        Box(Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent))))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(420.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 92.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
                // Reel info 296: author · caption · audio, gap 10
                Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(34.dp).clip(R.Pill).border(1.5.dp, Color.White, R.Pill).clickable { state.go(Screen.ClientProfile(p.author.id ?: "team")) }) { AuthorAvatar(p.author, 34.dp) }
                        Text(if (p.author.team) "focusontop" else p.author.name, color = Color.White, style = T.BodyS.copy(shadow = shade), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        if (!p.mine && !p.author.team && p.author.id != null) {
                            val f = following ?: false
                            Box(
                                Modifier.height(30.dp).clip(R.Pill).border(1.dp, Color.White.copy(alpha = 0.7f), R.Pill)
                                    .clickable { scope.launch { runCatching { state.api.follow(p.author.id) }.onSuccess { following = it.following; state.refreshCommunity() } } }
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(if (f) tr("Gefolgt", "Following", "تتابعه") else tr("Folgen", "Follow", "متابعة"), color = Color.White, style = T.Footnote, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                    if (p.caption.isNotBlank()) Text(p.caption, color = Color.White, style = T.BodyS.copy(shadow = shade), maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { state.go(Screen.Post(p.id, p)) })
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Ic.Music, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(tr("Originalton", "Original audio", "الصوت الأصلي") + " · " + (if (p.author.team) "focusontop" else p.author.name),
                            color = Color.White, style = T.Caption.copy(shadow = shade), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                // Action rail: gap 20
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    RailAction(if (p.liked) Ic.HeartFill else Ic.Heart, formatCount(p.likes), if (p.liked) C.Gold else Color.White) { state.toggleLike(p) }
                    RailAction(Ic.Chat, formatCount(p.comments), Color.White) { state.go(Screen.Post(p.id, p)) }
                    RailAction(Ic.Bookmark, tr("Speichern", "Save", "حفظ"), if (p.saved) C.Gold else Color.White) { state.toggleSave(p) }
                    RailAction(Ic.Send, tr("Teilen", "Share", "مشاركة"), Color.White, onShare)
                    if (p.mine) RailAction(Ic.ChartBar, tr("Statistik", "Insights", "الإحصائيات"), Color.White, onInsights)
                }
            }
            Spacer(Modifier.height(16.dp))
            // 2 pt progress line
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.25f))) {
                Box(Modifier.fillMaxWidth(if (playing) 0.36f else 0f).height(2.dp).background(Color.White))
            }
        }
    }
}

@Composable
private fun RailAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(30.dp))
        Text(label, color = Color.White, style = T.Caption.copy(shadow = shade), fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Figma 11b share sheet: handle, search + add people, contacts grid (68), divider, share actions (56 circles), Senden. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareSheet(state: AppState, p: CPost, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val link = Media.abs(p.mediaUrl) ?: "https://focusontop.de"
    fun recordShare() { scope.launch { runCatching { state.api.recordShare(p.id) } } }
    var q by remember { mutableStateOf("") }
    var people by remember { mutableStateOf<List<CPerson>>(emptyList()) }
    val picked = remember { mutableStateListOf<String>() }
    LaunchedEffect(q) { people = runCatching { state.api.people(q.trim()) }.getOrElse { emptyList() } }
    val copied = tr("Link kopiert", "Link copied", "تم نسخ الرابط")
    Box(Modifier.fillMaxSize().background(C.Scrim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(R.SheetShape).background(C.Surface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding().padding(top = 8.dp, bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.width(36.dp).height(5.dp).clip(R.Pill).background(C.Separator))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.weight(1f).height(40.dp).clip(R.Pill).background(C.GlassStrong).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Ic.Search, null, tint = C.Muted, modifier = Modifier.size(18.dp))
                    Box(Modifier.weight(1f)) {
                        if (q.isEmpty()) Text(tr("Suchen", "Search", "بحث"), color = C.Muted, style = T.Body)
                        androidx.compose.foundation.text.BasicTextField(q, { q = it }, singleLine = true, textStyle = T.Body.copy(color = C.Ink),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(C.Gold), modifier = Modifier.fillMaxWidth())
                    }
                }
                Box(Modifier.size(40.dp).clip(R.Pill).background(C.GlassStrong).clickable { onClose(); state.tab = de.focusontop.client.data.Tab.SEARCH }, contentAlignment = Alignment.Center) {
                    Icon(Ic.UserPlus, null, tint = C.Ink, modifier = Modifier.size(20.dp))
                }
            }
            if (people.isNotEmpty()) FlowRow(Modifier.fillMaxWidth().padding(horizontal = 9.dp), maxItemsInEachRow = 3) {
                people.take(6).forEach { c ->
                    val on = c.id in picked
                    Column(Modifier.weight(1f).padding(bottom = 18.dp).clickable { if (on) picked.remove(c.id) else picked.add(c.id) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(68.dp)) {
                            InitialTile(c.name, 68.dp, c.avatar, warm = true)
                            if (on) Box(Modifier.offset(46.dp, 46.dp).size(24.dp).clip(R.Pill).background(C.Gold).border(2.5.dp, C.Surface, R.Pill), contentAlignment = Alignment.Center) {
                                Icon(Ic.CheckBold, null, tint = C.OnAccent, modifier = Modifier.size(14.dp))
                            }
                        }
                        Text(c.name, color = C.Ink, style = T.Caption, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(C.Separator))
            val lStory = tr("In Story posten", "Post to story", "نشر في القصة"); val lCopy = tr("Link kopieren", "Copy link", "نسخ الرابط")
            val lVia = tr("Teilen über", "Share via", "مشاركة عبر"); val lSave = tr("Speichern", "Save", "حفظ")
            LazyRow(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val enc = link.replace(":", "%3A").replace("/", "%2F")
                val actions = listOf<Triple<ImageVector, String, () -> Unit>>(
                    Triple(Ic.Chat, "WhatsApp") { recordShare(); openUrl("https://wa.me/?text=$enc") },
                    Triple(Ic.Instagram, "Instagram") { recordShare(); shareText(link) },
                    Triple(Ic.Facebook, "Facebook") { recordShare(); openUrl("https://www.facebook.com/sharer/sharer.php?u=$enc") },
                    Triple(Ic.Music, "TikTok") { recordShare(); shareText(link) },
                    Triple(Ic.PlusCircle, lStory) { recordShare(); onClose(); state.go(Screen.Compose("story")) },
                    Triple(Ic.Link, lCopy) { recordShare(); clipboard.setText(AnnotatedString(link)); state.showToast(copied); onClose() },
                    Triple(Ic.Share, lVia) { recordShare(); shareText(link) },
                    Triple(Ic.Download, lSave) { openUrl(link) },
                )
                items(actions.size) { i ->
                    val (icon, label, act) = actions[i]
                    val brand = i < 4
                    Column(Modifier.width(62.dp).clickable(onClick = act), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(56.dp).clip(R.Pill).background(C.Surface2), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = if (brand) C.Faint else C.GoldLabel, modifier = Modifier.size(24.dp))
                        }
                        Text(label, color = C.Muted, style = T.Caption, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
            Box(Modifier.padding(horizontal = 16.dp)) {
                Box(Modifier.fillMaxWidth().height(50.dp).clip(R.Pill).background(C.Gold).clickable { recordShare(); shareText(link); onClose() }, contentAlignment = Alignment.Center) {
                    Text(tr("Senden", "Send", "إرسال"), color = C.OnAccent, style = T.Body, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Figma "F2 · Medien · Statistik und Zuschauer": 4 stat tiles (save/share/like/view) + "Gesehen von"/"شاهده" viewer list.
 *  Author-only — opened from the reel's own action rail. */
@Composable
private fun ReelInsightsSheet(state: AppState, p: CPost, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var insights by remember(p.id) { mutableStateOf<de.focusontop.client.data.CInsights?>(null) }
    var loading by remember(p.id) { mutableStateOf(true) }
    val follow = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(p.id) {
        insights = runCatching { state.api.insights(p.id) }.getOrNull()
        loading = false
    }
    Box(Modifier.fillMaxSize().background(C.Scrim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.78f).clip(R.SheetShape).background(C.Surface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding().padding(top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width(36.dp).height(5.dp).clip(R.Pill).background(C.Separator))
            Spacer(Modifier.height(16.dp))
            Text(tr("Statistik", "Insights", "الإحصائيات"), color = C.Ink, style = T.Title3, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            if (loading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = C.Gold) }
            } else {
                val ins = insights ?: de.focusontop.client.data.CInsights()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InsightTile(Ic.Bookmark, formatCount(ins.saves), tr("Speicherungen", "Saves", "حفظ"), Modifier.weight(1f))
                    InsightTile(Ic.Send, formatCount(ins.shares), tr("Geteilt", "Shares", "مشاركات"), Modifier.weight(1f))
                    InsightTile(Ic.HeartFill, formatCount(ins.likes), tr("Gefällt mir", "Likes", "إعجابات"), Modifier.weight(1f))
                    InsightTile(Ic.Eye, formatCount(ins.views), tr("Aufrufe", "Views", "مشاهدات"), Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(formatCount(ins.views), color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold)
                    Text(tr("Gesehen von", "Watched by", "شاهده"), color = C.Muted, style = T.Footnote, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                if (ins.viewers.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(tr("Noch keine Aufrufe", "No views yet", "لا توجد مشاهدات بعد"), color = C.Muted, style = T.BodyS)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        items(ins.viewers, key = { it.id }) { v ->
                            val following = follow[v.id] ?: v.following
                            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    Modifier.height(32.dp).clip(R.Pill)
                                        .background(if (following) C.GlassStrong else C.Gold)
                                        .clickable {
                                            scope.launch { runCatching { state.api.follow(v.id) }.onSuccess { follow[v.id] = it.following } }
                                        }.padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        if (following) tr("Nachricht", "Message", "رسالة") else tr("Folgen", "Follow", "متابعة"),
                                        color = if (following) C.Ink else C.OnAccent, style = T.Footnote, fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                if (v.liked) Icon(Ic.HeartFill, null, tint = C.Gold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.weight(1f))
                                Text(v.name, color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                InitialTile(v.name, 44.dp, v.avatar, warm = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(R.CardShape).background(C.Surface2).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = C.GoldLabel, modifier = Modifier.size(18.dp))
        Text(value, color = C.Ink, style = T.BodyS, fontWeight = FontWeight.Bold)
        Text(label, color = C.Muted, style = T.Micro)
    }
}
