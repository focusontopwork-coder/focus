package de.focusontop.client.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.focusontop.client.data.AppState
import de.focusontop.client.data.ClientFile
import de.focusontop.client.data.Home
import de.focusontop.client.data.Media
import de.focusontop.client.data.Screen
import de.focusontop.client.data.Story
import de.focusontop.client.data.Tab
import de.focusontop.client.i18n.Lang
import de.focusontop.client.i18n.LocalLang
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.i18n.tr
import de.focusontop.client.platform.nowEpochMs
import de.focusontop.client.platform.openUrl
import de.focusontop.client.platform.todayIso
import de.focusontop.client.res.Res
import de.focusontop.client.res.logo_white
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeTab(state: AppState, onUpload: () -> Unit) {
    val s = LocalStrings.current
    val home = state.home
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    // swipe towards the end (→ in German/English, ← in Arabic) opens the story camera, like Instagram
    val swipe = Modifier.pointerInput(rtl) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                if ((if (rtl) -total else total) > 160f && de.focusontop.client.platform.inAppCameraAvailable) state.go(Screen.StoryCamera)
            },
        ) { _, dx -> total += dx }
    }
    LazyColumn(swipe, contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            // Figma "Page header" (72): bell 22 · wordmark 22 + feed scope (Micro) · message-circle 22, separator below
            Box(Modifier.fillMaxWidth().height(72.dp)) {
                Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    HeaderIcon(Ic.Bell, s.notifications, dot = (home?.unreadNotifications ?: 0) > 0) { state.go(Screen.Notifications) }
                    Column(
                        Modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { state.toggleFeedScope() },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Wordmark(22.dp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(if (state.discover) tr("Für dich", "For you", "لك") else tr("Gefolgt", "Following", "أتابعهم"), color = C.Muted, style = T.Micro)
                            Icon(Ic.ChevronDownSm, null, tint = C.Muted, modifier = Modifier.size(10.dp))
                        }
                    }
                    HeaderIcon(Ic.Chat, tr("Nachrichten", "Messages", "الرسائل"), badge = home?.unreadMessages ?: 0) { state.go(Screen.Messages) }
                }
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp).background(C.Separator))
            }
        }
        if (state.homeError != null && home == null) item { Box(Modifier.padding(horizontal = 16.dp)) { ErrorBanner(s.loadError) { state.refresh() } } }
        if (home == null && state.homeError == null) {
            item { Box(Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = C.Gold) } }
        }
        if (home != null) {
            item { Box(Modifier.padding(top = 4.dp)) { CommunityStoriesRow(state) } }
            val feed = state.feed
            if (feed == null) item { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = C.Gold) } }
            else if (feed.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EmptyState(
                        tr("Bald gibt es hier Neues", "New posts coming soon", "قريبًا منشورات جديدة"),
                        tr("Hier erscheinen die Beiträge von Focus On Top und von Kunden, denen du folgst.", "Posts from Focus On Top and the clients you follow appear here.", "هنا تظهر منشورات Focus On Top والعملاء الذين تتابعهم."),
                        icon = UploadIcon,
                    )
                    GoldButton(tr("Beitrag posten", "Create a post", "انشر منشورًا"), { state.go(Screen.Compose("post")) }, Modifier.fillMaxWidth(), icon = Ic.Plus)
                }
            }
            else items(feed, key = { "post-" + it.id }) { p ->
                PostCard(state, p) { state.go(Screen.Post(p.id, p)) }
            }
        }
    }
}

/* ── stories ── */

@Composable
private fun StoriesRow(state: AppState, stories: List<Story>, onUpload: () -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StoryBubble(tr("Deine Story", "Your story", "قصتك"), onClick = onUpload) {
                Box(Modifier.fillMaxSize().border(1.5.dp, C.Border, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                    Icon(Ic.Plus, null, tint = C.Gold, modifier = Modifier.size(30.dp))
                }
            }
        }
        items(stories, key = { it.id }) { st ->
            StoryBubble(st.title, ring = st.fresh, onClick = { if (st.fileIds.isNotEmpty()) state.go(Screen.Viewer(st.fileIds, 0, st.title)) }) {
                Box(Modifier.fillMaxSize().background(placeholderBrush(st.id))) {
                    val src = Media.image(st.cover, 300)
                    if (src != null) AsyncImage(model = src, contentDescription = st.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun StoryBubble(label: String, ring: Boolean = false, onClick: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.width(84.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            Modifier.size(84.dp).then(if (ring) Modifier.dashedBorder(C.Gold, 26.dp, 2.5.dp) else Modifier).padding(if (ring) 5.dp else 0.dp)
                .clip(RoundedCornerShape(if (ring) 20.dp else 24.dp)),
        ) { content() }
        Text(label, color = C.Ink.copy(alpha = 0.85f), style = T.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/* ── plan card ── */

@Composable
fun planName(state: AppState, home: Home?): String {
    val sub = home?.subscription ?: return tr("Kein aktiver Tarif", "No active plan", "لا توجد باقة نشطة")
    return when (state.lang) { Lang.AR -> sub.nameAr; Lang.DE -> sub.nameDe; Lang.EN -> sub.nameEn } ?: sub.nameDe ?: sub.nameEn ?: sub.nameAr ?: "—"
}

/** "01.11." — the next open invoice's due date. */
fun nextPayment(home: Home?): String? {
    val today = todayIso()
    return home?.invoices.orEmpty()
        .filter { it.status != "paid" && !it.dueDate.isNullOrBlank() && it.dueDate.take(10) >= today }
        .minByOrNull { it.dueDate!!.take(10) }?.dueDate?.let { fmtDate(it).take(6) }
}

@Composable
private fun PlanCard(state: AppState, home: Home) {
    val sub = home.subscription
    val u = home.usage
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Surface.copy(alpha = 0.85f))
            .border(1.dp, C.Border.copy(alpha = 0.6f), RoundedCornerShape(22.dp)).clickable { state.tab = Tab.PROFILE }.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).border(1.dp, C.Border, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(Ic.Shield, null, tint = C.Gold, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(planName(state, home), color = C.Ink, style = T.Headline, fontWeight = FontWeight.SemiBold, maxLines = 1)
                val months = u?.monthsTotal
                val line = listOfNotNull(
                    months?.let { tr("$it-Monats-Abo", "$it-month plan", "اشتراك $it أشهر") },
                    sub?.endDate?.let { tr("endet ${fmtDate(it)}", "ends ${fmtDate(it)}", "ينتهي ${fmtDate(it)}") },
                ).joinToString(" · ")
                if (line.isNotEmpty()) Text(line, color = C.Muted, style = T.Caption, maxLines = 1)
            }
            if (sub?.status == "active") OutlinePill(tr("Aktiv", "Active", "نشطة"))
        }
        val days = u?.daysLeft
        if (days != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                days.toString().forEach { d ->
                    Box(Modifier.padding(end = 6.dp).width(36.dp).height(44.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, C.Border, RoundedCornerShape(12.dp)).background(C.Bg.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                        Text(d.toString(), color = C.Gold, style = T.Title2, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Tage übrig", "days left", "يومًا للتجديد"), color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold)
                    nextPayment(home)?.let { Text(tr("Zahlung $it", "Payment $it", "الدفعة القادمة $it"), color = C.Muted, style = T.Caption) }
                }
                val mi = u?.monthIndex
                val mt = u?.monthsTotal
                if (mi != null && mt != null) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tr("Monat $mi von $mt", "Month $mi of $mt", "الشهر $mi من $mt"), color = C.Muted, style = T.Micro)
                        MonthDots(mi, mt)
                    }
                }
            }
        }
    }
}

/* ── waiting for approval ── */

@Composable
private fun PendingCard(state: AppState, pending: List<ClientFile>) {
    val n = pending.size
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Surface.copy(alpha = 0.85f))
            .border(1.dp, C.Border.copy(alpha = 0.6f), RoundedCornerShape(22.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(30.dp + 22.dp * (pending.take(3).size - 1).coerceAtLeast(0) + 16.dp).height(46.dp)) {
            pending.take(3).forEachIndexed { i, f ->
                Box(Modifier.offset(x = (i * 22).dp).size(width = 38.dp, height = 46.dp).clip(RoundedCornerShape(12.dp)).border(1.5.dp, C.Surface, RoundedCornerShape(12.dp))) {
                    FileCover(f, Modifier.fillMaxSize(), 160)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tr(if (n == 1) "1 Datei wartet auf Freigabe" else "$n Dateien warten auf Freigabe", if (n == 1) "1 file awaits your approval" else "$n files await your approval",
                    if (n == 1) "ملف واحد بانتظار موافقتك" else if (n == 2) "ملفان بانتظار موافقتك" else "$n ملفات بانتظار موافقتك"),
                color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold, maxLines = 2,
            )
            Text(pending.first().title(), color = C.Muted, style = T.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        GoldButton(tr("Prüfen", "Review", "راجع"), { state.go(Screen.Viewer(pending.map { it.id }, 0)) }, small = true)
    }
}

/* ── feed post ── */

@Composable
private fun FeedPost(state: AppState, f: ClientFile, open: () -> Unit) {
    val lang = LocalLang.current
    val clipboard = LocalClipboardManager.current
    val copied = tr("Link kopiert", "Link copied", "تم نسخ الرابط")
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FtBadge(42.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Focus On Top", color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold)
                Text(listOfNotNull(relativeTime(lang, f.createdAt, nowEpochMs()), f.projectName ?: f.taskName).joinToString(" · "), color = C.Muted, style = T.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            fileStatus(LocalStrings.current, f.status).let { if (f.status == "ready_for_review") GoldPill(it.label) }
        }
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(24.dp)).border(1.dp, C.Border.copy(alpha = 0.5f), RoundedCornerShape(24.dp)).clickable(onClick = open),
            contentAlignment = Alignment.Center,
        ) {
            FileCover(f, Modifier.fillMaxSize(), 1000)
            if (f.type == "Video") {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Icon(Ic.Play, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val fav = f.id in state.favourites
            ActionPill(if (fav) Ic.HeartFill else Ic.Heart, null, tint = if (fav) C.Gold else C.Ink) { state.toggleFavourite(f.id) }
            ActionPill(Ic.ChatLines, f.commentCount.takeIf { it > 0 }?.toString(), onClick = open)
            ActionPill(Ic.Link, null) { Media.share(f)?.let { clipboard.setText(AnnotatedString(it)); state.showToast(copied) } }
            Spacer(Modifier.weight(1f))
            ActionPill(Ic.Download, null) { Media.download(f.url, f.fileName ?: f.name)?.let(::openUrl) }
        }
    }
}

@Composable
fun ActionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, count: String?, tint: Color = C.Ink, onClick: () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(14.dp)).background(C.Surface.copy(alpha = 0.7f)).border(1.dp, C.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        if (count != null) { Spacer(Modifier.width(7.dp)); Text(count, color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold) }
    }
}
