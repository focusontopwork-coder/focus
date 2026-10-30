package de.focusontop.client.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.focusontop.client.data.ApiError
import de.focusontop.client.data.AppState
import de.focusontop.client.data.ChatMessage
import de.focusontop.client.data.ChatThread
import de.focusontop.client.data.Media
import de.focusontop.client.data.Screen
import de.focusontop.client.data.Tab
import de.focusontop.client.i18n.LocalLang
import de.focusontop.client.i18n.tr
import de.focusontop.client.platform.localDateTime
import de.focusontop.client.platform.nowEpochMs
import de.focusontop.client.platform.openUrl
import de.focusontop.client.platform.rememberFilePicker
import de.focusontop.client.platform.todayIso
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ChatStyle { MAIN, TEAM }

/** Figma 20 · Nachrichten · Hauptchat — the conversation with the team, opened from the home header. */
@Composable
fun MessagesScreen(state: AppState) = ChatView(state, "", ChatStyle.MAIN)

/** Figma 21 · Nachrichten · Teamchat. */
@Composable
fun ChatScreen(state: AppState, initialDraft: String) = ChatView(state, initialDraft, ChatStyle.TEAM)

@Composable
private fun ChatView(state: AppState, initialDraft: String, style: ChatStyle) {
    val scope = rememberCoroutineScope()
    val lang = LocalLang.current
    var messages by remember { mutableStateOf<List<ChatMessage>?>(null) }
    var draft by remember { mutableStateOf(initialDraft) }
    var busy by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val failed = tr("Senden fehlgeschlagen", "Could not send", "تعذّر الإرسال")

    suspend fun load() {
        runCatching { state.api.chat() }.onSuccess { messages = it.messages; state.clearUnreadMessages() }
    }
    LaunchedEffect(Unit) { while (true) { load(); delay(8_000) } }
    LaunchedEffect(messages?.size) { messages?.let { if (it.isNotEmpty()) listState.animateScrollToItem(it.lastIndex) } }

    val picker = rememberFilePicker { files ->
        val f = files.firstOrNull() ?: return@rememberFilePicker
        uploading = true
        scope.launch {
            try {
                val blob = state.uploadBlob(f)
                state.api.sendChat(draft.trim(), blob.url, f.name, f.mimeType)
                draft = ""
                load()
            } catch (_: Exception) { state.showToast(failed) } finally { uploading = false }
        }
    }
    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || busy) return
        busy = true
        scope.launch {
            try { state.api.sendChat(text); draft = ""; load() }
            catch (_: ApiError) { state.showToast(failed) } finally { busy = false }
        }
    }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        if (style == ChatStyle.MAIN) TopBar(tr("Nachrichten", "Messages", "الرسائل"), onBack = { state.back() },
            actions = { HeaderIcon(Ic.SquarePen, tr("Neue Nachricht", "New message", "رسالة جديدة")) { state.go(Screen.Chat()) } })
        else TopBar("Team Focus On Top", onBack = { state.back() })

        if (style == ChatStyle.TEAM) Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).border(1.5.dp, C.Green, CircleShape))
            Text(tr("Antwortet meist innerhalb weniger Stunden", "Usually replies within a few hours", "يرد عادةً خلال ساعات قليلة"), color = C.Muted, style = T.Caption, maxLines = 1)
        }
        val list = messages
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                list == null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = C.Gold)
                list.isEmpty() -> EmptyState(tr("Schreib dem Team", "Write to the team", "اكتب إلى الفريق"), tr("Fragen, Wünsche, Feedback – hier landet alles direkt beim Team.", "Questions, wishes, feedback – everything lands with the team.", "أسئلتك وطلباتك وملاحظاتك تصل مباشرة إلى الفريق."))
                else -> LazyColumn(
                    state = listState,
                    contentPadding = if (style == ChatStyle.MAIN) PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp) else PaddingValues(start = 16.dp, end = 16.dp, top = 34.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    var lastDay = ""
                    list.forEach { m ->
                        val day = epochOf(m.createdAt)?.let { localDateTime(it).take(10) } ?: ""
                        if (day != lastDay && day.isNotEmpty()) {
                            lastDay = day
                            item(key = "d-$day") {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(dayHeader(de.focusontop.client.i18n.LocalStrings.current, day, todayIso()), color = C.Muted, style = T.Caption, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clip(R.Pill).background(C.GlassStrong).padding(horizontal = 10.dp, vertical = 4.dp))
                                }
                            }
                        }
                        item(key = m.id) { Bubble(m, style) }
                    }
                }
            }
        }
        if (style == ChatStyle.MAIN) {
            // Figma "Composer": padding 8/16/30/16, gap 8 — attach 40 · field 42 · send 40 gold
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(40.dp).clip(R.Pill).background(C.GlassStrong).clickable(enabled = !uploading) { picker.pickFiles() }, contentAlignment = Alignment.Center) {
                    if (uploading) CircularProgressIndicator(Modifier.size(18.dp), color = C.Gold, strokeWidth = 2.dp)
                    else Icon(Ic.Paperclip, tr("Datei anhängen", "Attach file", "إرفاق ملف"), tint = C.Ink, modifier = Modifier.size(20.dp))
                }
                Box(Modifier.weight(1f).height(42.dp).clip(R.Pill).background(C.GlassStrong).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    if (draft.isEmpty()) Text(tr("Nachricht …", "Message …", "رسالة…"), color = C.Muted, style = T.BodyS)
                    BasicTextField(draft, { draft = it }, maxLines = 4, textStyle = T.BodyS.copy(color = C.Ink), cursorBrush = SolidColor(C.Gold), modifier = Modifier.fillMaxWidth())
                }
                Box(Modifier.size(40.dp).clip(R.Pill).background(C.Gold).clickable(enabled = !busy) { send() }, contentAlignment = Alignment.Center) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = C.OnAccent, strokeWidth = 2.dp)
                    else Icon(Ic.Send, tr("Senden", "Send", "إرسال"), tint = C.OnAccent, modifier = Modifier.size(20.dp))
                }
            }
        } else {
            // Figma 21: plus 22 · field 48 (glass-strong + hairline, Footnote) · arrow-up 22
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uploading) CircularProgressIndicator(Modifier.size(22.dp), color = C.Gold, strokeWidth = 2.dp)
                else Icon(Ic.Plus, tr("Datei anhängen", "Attach file", "إرفاق ملف"), tint = C.Ink, modifier = Modifier.size(22.dp).clickable { picker.pickFiles() })
                Box(Modifier.weight(1f).height(48.dp).clip(R.Pill).background(C.GlassStrong).border(1.dp, C.StrokeGlass, R.Pill).padding(12.dp), contentAlignment = Alignment.CenterStart) {
                    if (draft.isEmpty()) Text(tr("Nachricht schreiben …", "Write a message …", "اكتب رسالة…"), color = C.Muted, style = T.Footnote)
                    BasicTextField(draft, { draft = it }, maxLines = 3, textStyle = T.Footnote.copy(color = C.Ink), cursorBrush = SolidColor(C.Gold), modifier = Modifier.fillMaxWidth())
                }
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = C.Gold, strokeWidth = 2.dp)
                else Icon(Ic.ArrowUp, tr("Senden", "Send", "إرسال"), tint = if (draft.isBlank()) C.Muted else C.Ink, modifier = Modifier.size(22.dp).clickable { send() })
            }
        }
    }
}

/** Bubbles: mine = gold, radius 18/18/6/18; team = bg/secondary, radius 18/18/18/6 with a 28 avatar; max 274. */
@Composable
private fun Bubble(m: ChatMessage, style: ChatStyle) {
    val mine = m.mine
    val shape = if (mine) RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 6.dp, bottomStart = 18.dp)
        else RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp)
    val fg = if (mine) C.OnAccent else C.Ink
    val meta = if (mine) C.OnAccent else C.Muted
    val img = m.imageUrl
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        if (!mine) { FtBadge(28.dp); Spacer(Modifier.width(8.dp)) }
        Column(
            Modifier.widthIn(max = if (style == ChatStyle.MAIN) 274.dp else 264.dp).clip(shape).background(if (mine) C.Gold else C.Surface)
                .padding(if (img != null) PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 9.dp) else PaddingValues(horizontal = 12.dp, vertical = 9.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!mine && style == ChatStyle.TEAM && !m.senderName.isNullOrBlank() && img == null) Text(m.senderName, color = C.GoldLabel, style = T.Caption, fontWeight = FontWeight.SemiBold)
            if (img != null) {
                Box(Modifier.width(220.dp).height(124.dp).clip(RoundedCornerShape(12.dp)).clickable { Media.abs(img)?.let(::openUrl) }) {
                    AsyncImage(model = Media.image(img, 800), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            val fileUrl = m.fileData?.takeIf { it.startsWith("/uploads/") || it.startsWith("http") }
            if (fileUrl != null) {
                Row(Modifier.padding(horizontal = if (img != null) 8.dp else 0.dp).clickable { Media.download(fileUrl, m.fileName)?.let(::openUrl) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Ic.Paperclip, null, tint = if (mine) C.OnAccent else C.GoldLabel, modifier = Modifier.size(16.dp))
                    Text(m.fileName ?: "Datei", color = fg, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (!m.text.isNullOrBlank()) Text(m.text, color = fg, style = T.BodyS, modifier = Modifier.padding(horizontal = if (img != null) 8.dp else 0.dp, vertical = if (img != null) 4.dp else 0.dp))
            Row(Modifier.padding(horizontal = if (img != null) 8.dp else 0.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(epochOf(m.createdAt)?.let { localDateTime(it).substring(11, 16) } ?: "", color = meta, style = T.Caption)
                if (mine) Icon(Ic.CheckAllBold, null, tint = C.OnAccent, modifier = Modifier.size(14.dp))
            }
        }
    }
}
