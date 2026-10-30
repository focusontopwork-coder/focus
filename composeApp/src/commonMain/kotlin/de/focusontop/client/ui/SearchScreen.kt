package de.focusontop.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.focusontop.client.data.AppState
import de.focusontop.client.data.CPerson
import de.focusontop.client.data.Media
import de.focusontop.client.data.Screen
import de.focusontop.client.data.Tab
import de.focusontop.client.i18n.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Recent searches for this app session: an account (by person) or a plain term. */
private sealed class Recent {
    data class Person(val p: CPerson) : Recent()
    data class Term(val q: String) : Recent()
}
private val recents = mutableStateListOf<Recent>()
private fun addRecent(r: Recent) {
    recents.removeAll { it == r || (it is Recent.Person && r is Recent.Person && it.p.id == r.p.id) }
    recents.add(0, r)
    while (recents.size > 8) recents.removeAt(recents.lastIndex)
}

/** Figma 10 · Entdecken · Suche  /  10b · Keine Ergebnisse. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(state: AppState) {
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var people by remember { mutableStateOf<List<CPerson>?>(null) }
    var suggested by remember { mutableStateOf<List<CPerson>>(emptyList()) }
    LaunchedEffect(Unit) { suggested = runCatching { state.api.people("") }.getOrElse { emptyList() } }
    LaunchedEffect(q) {
        if (q.isBlank()) { people = null; return@LaunchedEffect }
        delay(250)
        people = runCatching { state.api.people(q.trim()) }.getOrElse { emptyList() }
    }
    fun follow(p: CPerson) {
        scope.launch {
            runCatching { state.api.follow(p.id) }.onSuccess { f ->
                people = people?.map { if (it.id == p.id) it.copy(following = f.following) else it }
                suggested = suggested.map { if (it.id == p.id) it.copy(following = f.following) else it }
                state.refreshCommunity()
            }
        }
    }
    fun openPerson(p: CPerson) { addRecent(Recent.Person(p)); state.go(Screen.ClientProfile(p.id)) }

    val posts = state.feed.orEmpty().filter {
        it.kind == "post" && q.isNotBlank() && (it.caption.contains(q.trim(), true) || it.author.name?.contains(q.trim(), true) == true)
    }
    val categories = listOf(
        tr("Hochzeiten", "Weddings", "أعراس"), tr("Produkte", "Products", "منتجات"),
        tr("Events", "Events", "فعاليات"), tr("Gastronomie", "Food & drink", "مطاعم"),
    )
    val covers = state.feed.orEmpty().filter { it.kind == "post" && it.mediaType == "Image" }.mapNotNull { it.mediaUrl }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(tr("Suche", "Search", "بحث"), onBack = { state.tab = Tab.HOME })
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                FtField("", q, { q = it }, placeholder = tr("Konten, Videos, Themen …", "Accounts, videos, topics …", "حسابات، فيديوهات، مواضيع…"),
                    trailing = if (q.isEmpty()) Ic.Search else null)
            }
            if (q.isBlank()) {
                if (recents.isNotEmpty()) item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SectionHeader(tr("Zuletzt gesucht", "Recent", "عمليات البحث الأخيرة"), tr("Alle löschen", "Clear all", "مسح الكل")) { recents.clear() }
                        recents.toList().forEach { r ->
                            Row(Modifier.fillMaxWidth().height(40.dp).clickable {
                                when (r) { is Recent.Person -> openPerson(r.p); is Recent.Term -> q = r.q }
                            }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                when (r) {
                                    is Recent.Person -> {
                                        InitialTile(r.p.name, 40.dp, r.p.avatar, warm = true)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(r.p.name, color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    is Recent.Term -> {
                                        Box(Modifier.size(40.dp).clip(R.Pill).background(C.GlassStrong), contentAlignment = Alignment.Center) {
                                            Icon(Ic.Clock, null, tint = C.Muted, modifier = Modifier.size(18.dp))
                                        }
                                        Text(r.q, color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                }
                                Icon(Ic.Close, null, tint = C.Faint, modifier = Modifier.size(18.dp).clickable { recents.remove(r) })
                            }
                        }
                    }
                }
                if (suggested.isNotEmpty()) item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SectionHeader(tr("Vorschläge für dich", "Suggested for you", "مقترحات لك"), null) {}
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(suggested, key = { it.id }) { p -> AccountCard(p, onOpen = { openPerson(p) }, onFollow = { follow(p) }) }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SectionHeader(tr("Kategorien", "Categories", "الفئات"), null) {}
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            categories.chunked(2).forEachIndexed { row, pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pair.forEachIndexed { i, name ->
                                        CategoryTile(name, covers.getOrNull(row * 2 + i), Modifier.weight(1f)) { q = name; addRecent(Recent.Term(name)) }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val pl = people
                val nothing = pl != null && pl.isEmpty() && posts.isEmpty()
                if (nothing) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(64.dp).clip(R.Pill).background(C.GlassStrong), contentAlignment = Alignment.Center) {
                                Icon(Ic.Search, null, tint = C.Muted, modifier = Modifier.size(28.dp))
                            }
                            Text(tr("Keine Ergebnisse für „${q.trim()}“", "No results for “${q.trim()}”", "لا نتائج لـ «${q.trim()}»"), color = C.Ink, style = T.Headline, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Text(tr("Prüfe die Schreibweise oder probiere einen dieser Begriffe:", "Check the spelling or try one of these:", "تحقق من الكتابة أو جرّب أحد هذه المصطلحات:"), color = C.Muted, style = T.BodyS, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    item {
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEach { c ->
                                Row(
                                    Modifier.height(34.dp).clip(R.Pill).background(C.GlassStrong).clickable { q = c }.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(Ic.Search, null, tint = C.GoldLabel, modifier = Modifier.size(14.dp))
                                    Text(c, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                            }
                        }
                    }
                    if (suggested.isNotEmpty()) item {
                        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionHeader(tr("Beliebte Konten", "Popular accounts", "حسابات شائعة"), null) {}
                            suggested.take(5).forEach { p -> AccountRow(p, onOpen = { openPerson(p) }, onFollow = { follow(p) }) }
                        }
                    }
                } else {
                    if (!pl.isNullOrEmpty()) item {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionHeader(tr("Konten", "Accounts", "الحسابات"), null) {}
                            pl.forEach { p -> AccountRow(p, onOpen = { openPerson(p) }, onFollow = { follow(p) }) }
                        }
                    }
                    if (posts.isNotEmpty()) item {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionHeader(tr("Beiträge", "Posts", "المنشورات"), null) {}
                            posts.chunked(3).forEach { row ->
                                Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    row.forEach { p ->
                                        Box(Modifier.weight(1f).aspectRatio(1f).clip(R.ThumbShape).background(C.Surface2).clickable { addRecent(Recent.Term(q.trim())); state.go(Screen.Post(p.id, p)) }) {
                                            Media.image(p.mediaUrl, 600)?.let { AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                            if (p.mediaType == "Video") Icon(Ic.Play, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(15.dp))
                                        }
                                    }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Section header: Headline + optional Footnote-semibold gold action, space-between. */
@Composable
private fun SectionHeader(title: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = C.Ink, style = T.Headline)
        if (action != null) Text(action, color = C.GoldLabel, style = T.Footnote, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAction))
    }
}

/** Follow pill 30 high: gold "Folgen" / glass-strong "Gefolgt". */
@Composable
private fun FollowPill(following: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(30.dp).clip(R.Pill).background(if (following) C.GlassStrong else C.Gold).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (following) tr("Gefolgt", "Following", "تتابعه") else tr("Folgen", "Follow", "متابعة"),
            color = if (following) C.Ink else C.OnAccent, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** "Account card" 140 × 182: bg/secondary, separator 1, radius 20, padding 16/10, gap 8. */
@Composable
private fun AccountCard(p: CPerson, onOpen: () -> Unit, onFollow: () -> Unit) {
    Column(
        Modifier.width(140.dp).height(182.dp).clip(R.CardShape).background(C.Surface).border(1.dp, C.Separator, R.CardShape)
            .clickable(onClick = onOpen).padding(horizontal = 10.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InitialTile(p.name, 60.dp, p.avatar, warm = true)
        Text(p.name, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(tr("Kunde", "Client", "عميل"), color = C.Muted, style = T.Caption, maxLines = 1)
        FollowPill(p.following, Modifier.fillMaxWidth(), onFollow)
    }
}

/** "Account row" 44: avatar 44 · name (+ badge) / caption · follow pill. */
@Composable
private fun AccountRow(p: CPerson, onOpen: () -> Unit, onFollow: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp).clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InitialTile(p.name, 44.dp, p.avatar, warm = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(p.name, color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tr("Kunde", "Client", "عميل"), color = C.Muted, style = T.Caption, maxLines = 1)
        }
        FollowPill(p.following, onClick = onFollow)
    }
}

/** "Category" tile 174 × 96, radius 20, image + bottom shade (30% → 70% black), label Body S semibold white at 12/64. */
@Composable
private fun CategoryTile(name: String, cover: String?, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(96.dp).clip(R.CardShape).background(C.Surface2).clickable(onClick = onClick)) {
        Media.image(cover, 500)?.let { AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
        Text(name, color = Color.White, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1,
            modifier = Modifier.padding(start = 12.dp, top = 64.dp))
    }
}
