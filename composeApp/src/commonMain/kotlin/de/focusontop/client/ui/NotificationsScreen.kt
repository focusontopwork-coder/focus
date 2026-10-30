@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.focusontop.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.focusontop.client.data.ApiError
import de.focusontop.client.data.AppNotification
import de.focusontop.client.data.AppState
import de.focusontop.client.data.Media
import de.focusontop.client.data.Screen
import de.focusontop.client.data.Tab
import de.focusontop.client.data.asBool
import de.focusontop.client.i18n.LocalLang
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.i18n.tr
import de.focusontop.client.platform.nowEpochMs
import de.focusontop.client.platform.openUrl
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

@Composable
fun NotificationsScreen(state: AppState) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<AppNotification>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("all") }

    suspend fun load() {
        try { items = state.api.clientNotifications(); error = null }
        catch (_: ApiError) {
            // older server: fall back to the plain list
            runCatching { state.api.notifications() }.onSuccess { items = it; error = null }.onFailure { error = s.loadError }
        }
    }
    LaunchedEffect(Unit) { load() }

    fun markRead(n: AppNotification) {
        if (!n.isRead.asBool()) scope.launch { runCatching { state.api.markRead(n.id) } }
        items = items?.map { if (it.id == n.id) it.copy(isRead = JsonPrimitive(true)) else it }
    }

    fun open(n: AppNotification) {
        markRead(n)
        when (n.relatedType) {
            "file" -> n.relatedId?.let { id -> state.go(Screen.Viewer(listOf(id), 0)) }
            "invoice" -> state.go(Screen.Invoices)
            "task", "project" -> state.go(Screen.Projects)
            "appointment", "calendar" -> { state.tab = Tab.CALENDAR; state.back() }
            "chat" -> state.go(Screen.Chat())
            "subscription" -> { state.tab = Tab.PROFILE; state.back() }
            "community" -> n.relatedId?.let { id -> if (n.type == "community_follow") state.go(Screen.ClientProfile(id)) else state.go(Screen.Post(id)) }
        }
        state.refresh()
    }

    val requested = state.home?.appointments.orEmpty().filter { it.status == "requested" }
    val pending = state.home?.files.orEmpty().filter { !it.mine && it.status == "ready_for_review" }
    val now = nowEpochMs()
    val list = items.orEmpty().filter {
        when (filter) {
            "files" -> it.kind == "files"
            "appointments" -> it.kind == "appointments"
            "invoices" -> it.kind == "invoices"
            "messages" -> it.kind == "messages"
            else -> true
        }
    }
    val chips = listOf(
        "all" to tr("Alle", "All", "الكل"), "files" to tr("Freigaben", "Approvals", "موافقات"),
        "appointments" to tr("Termine", "Appointments", "مواعيد"), "invoices" to tr("Rechnungen", "Invoices", "فواتير"),
        "messages" to tr("Nachrichten", "Messages", "رسائل"),
    )
    val tToday = tr("Heute", "Today", "اليوم")
    val tWeek = tr("Diese Woche", "This week", "هذا الأسبوع")
    val tOlder = tr("Älter", "Older", "أقدم")
    val dayMs = 86_400_000L
    val today = list.filter { (now - (epochOf(it.createdAt) ?: 0)) < dayMs }
    val week = list.filter { (now - (epochOf(it.createdAt) ?: 0)) in dayMs until 7 * dayMs }
    val older = list.filter { (now - (epochOf(it.createdAt) ?: 0)) >= 7 * dayMs }

    fun markAll() {
        scope.launch {
            runCatching { state.api.markAllRead() }
            items = items?.map { it.copy(isRead = JsonPrimitive(true)) }
            state.clearUnread()
        }
    }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(s.notifications, onBack = { state.back() }, actions = { HeaderIcon(Ic.CheckCheck, s.markAllRead) { markAll() } })
        when {
            items == null && error == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = C.Gold) }
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().clickable { markAll() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(tr("Alle gelesen", "Mark all read", "تعيين الكل كمقروء"), color = C.GoldLabel, style = T.Caption, modifier = Modifier.weight(1f))
                        Icon(Ic.CheckCheck, null, tint = C.Ink, modifier = Modifier.size(18.dp))
                    }
                }
                item {
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chips.forEach { (k, label) ->
                            val on = filter == k
                            Text(label, color = if (on) C.GoldLabel else C.Ink, style = T.Caption, maxLines = 1,
                                modifier = Modifier.height(30.dp).clip(R.Pill).background(if (on) C.GoldTint else C.GlassStrong)
                                    .border(1.dp, if (on) C.Gold else C.StrokeGlass, R.Pill).clickable { filter = k }.padding(horizontal = 10.dp, vertical = 7.dp))
                        }
                    }
                }
                error?.let { item { ErrorBanner(it) { scope.launch { load() } } } }
                if ((pending.isNotEmpty() || requested.isNotEmpty()) && filter in listOf("all", "files", "appointments")) item {
                    Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.dp, C.StrokeGlass, R.CardShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(tr("Wartet auf deine Freigabe", "Awaiting your approval", "بانتظار موافقتك"), color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(tr("${pending.size} Dateien", "${pending.size} files", "${pending.size} ملفات"), color = C.Muted, style = T.Body, modifier = Modifier.weight(1f))
                            PrimaryButton(tr("Prüfen", "Review", "راجع"), onClick = { if (pending.isNotEmpty()) state.go(Screen.Viewer(pending.map { it.id }, 0)) },
                                modifier = Modifier.weight(1.23f), enabled = pending.isNotEmpty())
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
                        Row(Modifier.fillMaxWidth().clickable { state.go(Screen.AppointmentRequest) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(tr("Ihre Anfragen", "Your requests", "طلباتك"), color = C.Muted, style = T.Footnote, modifier = Modifier.weight(1f))
                            Text(tr("${requested.size} offen", "${requested.size} open", "${requested.size} مفتوح"), color = C.Muted, style = T.Body)
                        }
                    }
                }
                if (list.isEmpty() && items != null) item { EmptyState(s.noNotifications, icon = Ic.Bell) }
                group(tToday, today, lang, now, true, ::open)
                group(tWeek, week, lang, now, false, ::open)
                group(tOlder, older, lang, now, false, ::open)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.group(
    title: String, list: List<AppNotification>, lang: de.focusontop.client.i18n.Lang, now: Long, first: Boolean, open: (AppNotification) -> Unit,
) {
    if (list.isEmpty()) return
    item(key = "h-$title") { Text(title, color = C.Ink, style = if (first) T.BodyS else T.Headline, fontWeight = FontWeight.SemiBold) }
    item(key = "g-$title") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { list.forEach { n -> NotificationRow(n, lang, now) { open(n) } } }
    }
}

/** Figma "Notification": padding 8/0, gap 12 — unread dot 8 · avatar / icon circle 44 · text (Body S + Caption time) · action / thumbnail. */
@Composable
private fun NotificationRow(n: AppNotification, lang: de.focusontop.client.i18n.Lang, now: Long, onClick: () -> Unit) {
    val unread = !n.isRead.asBool()
    val kindIcon: ImageVector = when (n.kind) {
        "appointments" -> Ic.CalendarPlus
        "invoices" -> Ic.Receipt
        "messages" -> Ic.Chat
        "files" -> Ic.Upload
        else -> Ic.Bell
    }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (unread) C.Gold else Color.Transparent))
        val who = n.senderName
        if (who.isNullOrBlank() || n.kind == "invoices" || n.kind == "other") {
            Box(Modifier.size(44.dp).clip(CircleShape).background(C.GoldTint), contentAlignment = Alignment.Center) {
                Icon(kindIcon, null, tint = C.GoldLabel, modifier = Modifier.size(20.dp))
            }
        } else if (who.contains("Focus On Top", true)) FtBadge(44.dp) else InitialTile(who, 44.dp, warm = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    val t = n.title ?: ""
                    if (!who.isNullOrBlank() && t.startsWith(who)) {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.SemiBold)); append(who); pop(); append(t.removePrefix(who))
                    } else append(t)
                    if (!n.message.isNullOrBlank()) { append(if (t.isNotEmpty()) " " else ""); append(n.message) }
                },
                color = C.Ink, style = T.BodyS, maxLines = 4, overflow = TextOverflow.Ellipsis,
            )
            Text(relativeTime(lang, n.createdAt, now), color = C.Muted, style = T.Caption)
        }
        val thumb = Media.image(n.thumbnail, 200)
        when {
            n.kind == "files" && n.fileStatus == "ready_for_review" -> Text(tr("Prüfen", "Review", "راجع"), color = C.OnAccent, style = T.Footnote, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.height(32.dp).clip(R.Pill).background(C.Gold).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 7.dp))
            thumb != null -> AsyncImage(model = thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(R.ThumbShape))
        }
    }
}
