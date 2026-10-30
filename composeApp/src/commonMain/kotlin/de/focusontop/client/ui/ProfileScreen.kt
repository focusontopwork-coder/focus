package de.focusontop.client.ui

import androidx.compose.foundation.layout.fillMaxHeight
import de.focusontop.client.data.ClientFile
import de.focusontop.client.data.Task
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.focusontop.client.data.AppState
import de.focusontop.client.data.Home
import de.focusontop.client.data.Media
import de.focusontop.client.data.Screen
import de.focusontop.client.data.Usage
import de.focusontop.client.i18n.Lang
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.i18n.tr
import de.focusontop.client.platform.Prefs
import de.focusontop.client.platform.openUrl
import de.focusontop.client.platform.requestNotificationPermission
import de.focusontop.client.platform.todayIso
import kotlinx.coroutines.launch

private enum class ProfileGrid { POSTS, FILES, MINE }

/** Figma 14 · Konto · Profil. */
@Composable
fun ProfileTab(state: AppState) {
    val home = state.home
    val user = home?.user ?: state.user
    val clipboard = LocalClipboardManager.current
    val copied = tr("Link kopiert", "Link copied", "تم نسخ الرابط")
    var grid by remember { mutableStateOf(ProfileGrid.POSTS) }
    val files = home?.files.orEmpty().filter {
        when (grid) { ProfileGrid.POSTS -> false; ProfileGrid.FILES -> !it.mine; ProfileGrid.MINE -> it.mine }
    }.take(60)
    val display = user?.company?.ifBlank { null } ?: user?.name ?: ""
    val handle = profileHandle(user?.name ?: display)

    Column(Modifier.fillMaxSize()) {
        TopBar(
            tr("Profil", "Profile", "الملف الشخصي"),
            onBack = null,
            actions = { HeaderIcon(Ic.Settings, tr("Einstellungen", "Settings", "الإعدادات")) { state.go(Screen.Settings) } },
            leading = { HeaderIcon(Ic.Link, tr("Profillink kopieren", "Copy profile link", "نسخ رابط الملف")) { clipboard.setText(AnnotatedString(Media.web("profile"))); state.showToast(copied) } },
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(span = { GridItemSpan(3) }) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(64.dp).clip(R.Pill).border(1.dp, C.StrokeGlass, R.Pill)) { InitialTile(display, 64.dp, user?.avatar) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(display, color = C.Ink, style = T.Title3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("@$handle", color = C.Muted, style = T.Caption, maxLines = 1)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Stat(state.me?.posts?.size ?: 0, tr("Beiträge", "Posts", "منشورات"), Modifier.weight(1f)) { grid = ProfileGrid.POSTS }
                        Stat(state.me?.followers ?: 0, tr("Follower", "Followers", "متابِعون"), Modifier.weight(1f)) {}
                        Stat(home?.stats?.files ?: 0, tr("Dateien", "Files", "ملفات"), Modifier.weight(1f)) { state.go(Screen.Files) }
                    }
                    SecondaryButton(tr("Profil bearbeiten", "Edit profile", "تعديل الملف"), onClick = { state.go(Screen.EditProfile) }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileShortcut(Ic.Folder, tr("Projekte", "Projects", "المشاريع"), Modifier.weight(1f)) { state.go(Screen.Projects) }
                        ProfileShortcut(Ic.Layers, tr("Tarif", "Plan", "الباقة"), Modifier.weight(1f)) { state.go(Screen.Plans) }
                        ProfileShortcut(Ic.CalendarDays, tr("Kalender", "Calendar", "التقويم"), Modifier.weight(1f)) { state.tab = de.focusontop.client.data.Tab.CALENDAR }
                    }
                    PlanRow(state, home)
                    Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        GridTab(Ic.Grid3, grid == ProfileGrid.POSTS) { grid = ProfileGrid.POSTS }
                        GridTab(Ic.FolderCheck, grid == ProfileGrid.FILES) { grid = ProfileGrid.FILES }
                        GridTab(Ic.Upload, grid == ProfileGrid.MINE) { grid = ProfileGrid.MINE }
                    }
                }
            }
            if (grid == ProfileGrid.POSTS) {
                val posts = state.me?.posts.orEmpty()
                if (posts.isEmpty()) item(span = { GridItemSpan(3) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        EmptyState(tr("Noch keine Beiträge", "No posts yet", "لا توجد منشورات بعد"))
                        PrimaryButton(tr("Beitrag posten", "Create a post", "انشر منشورًا"), onClick = { state.go(Screen.Compose("post")) })
                    }
                }
                items(posts, key = { "pp-" + it.id }) { p ->
                    Box(Modifier.fillMaxWidth().aspectRatio(117f / 150f).clip(R.ThumbShape).background(C.Surface2).clickable { state.go(Screen.Post(p.id, p)) }) {
                        if (p.mediaType == "Image") AsyncImage(model = Media.image(p.mediaUrl, 400), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Icon(Ic.Play, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(24.dp))
                    }
                }
            } else if (files.isEmpty()) item(span = { GridItemSpan(3) }) { EmptyState(LocalStrings.current.noFiles) }
            else items(files, key = { "pf-" + it.id }) { f ->
                MediaTile(f, Modifier.fillMaxWidth().aspectRatio(117f / 150f)) { state.go(Screen.Viewer(files.map { it.id }, files.indexOf(f))) }
            }
        }
    }
}

/** Figma "Profile shortcut": icon 22 + Micro label, gap 8. */
@Composable
private fun ProfileShortcut(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = C.Ink, modifier = Modifier.size(22.dp))
        Text(label, color = C.Ink, style = T.Micro, textAlign = TextAlign.Center)
    }
}

/** "@karim.films"-style handle from the account name. */
fun profileHandle(name: String): String =
    name.lowercase().replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        .replace(Regex("[^a-z0-9]+"), ".").trim('.').ifBlank { "kunde" }

/** Figma "Plan row": 66 high, bg/secondary + separator, radius 20, padding 12/14, gold-tint icon tile 40. */
@Composable
private fun PlanRow(state: AppState, home: Home?) {
    val sub = home?.subscription
    val u = home?.usage
    Row(
        Modifier.fillMaxWidth().height(66.dp).clip(R.CardShape).background(C.Surface).border(1.dp, C.Separator, R.CardShape)
            .clickable { state.go(Screen.Plans) }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(C.GoldTint), contentAlignment = Alignment.Center) {
            Icon(Ic.Sparkles, null, tint = C.GoldLabel, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (sub == null) {
                Text(tr("Kein aktiver Tarif", "No active plan", "لا توجد باقة نشطة"), color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(tr("Wähle einen Tarif für dein erstes Projekt", "Choose a plan for your first project", "اختر باقة لمشروعك الأول"), color = C.Muted, style = T.Caption, maxLines = 1)
            } else {
                Text(planName(state, home), color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
                val line = listOfNotNull(
                    u?.daysLeft?.let { tr("Noch $it Tage", "$it days left", "متبقٍ $it يومًا") },
                    if (u?.monthIndex != null && u.monthsTotal != null) tr("Monat ${u.monthIndex} von ${u.monthsTotal}", "Month ${u.monthIndex} of ${u.monthsTotal}", "الشهر ${u.monthIndex} من ${u.monthsTotal}") else null,
                ).joinToString(" · ")
                if (line.isNotEmpty()) Text(line, color = C.Muted, style = T.Caption, maxLines = 1)
            }
        }
        Icon(Ic.ChevronRight, null, tint = C.Muted, modifier = Modifier.size(18.dp))
    }
}

/** Figma 14b · Profil bearbeiten — changes are sent to the team for approval. */
@Composable
fun EditProfileScreen(state: AppState) {
    val user = state.home?.user ?: state.user
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(user?.name ?: "") }
    var handle by remember { mutableStateOf("@" + profileHandle(user?.name ?: "")) }
    var busy by remember { mutableStateOf(false) }
    val sent = tr("Gesendet – das Team prüft deine Änderung.", "Sent – the team will review your change.", "تم الإرسال — سيراجع الفريق التعديل.")
    val failed = tr("Senden fehlgeschlagen.", "Couldn't send.", "تعذر الإرسال.")
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopBar(tr("Profil bearbeiten", "Edit profile", "تعديل الملف"), onBack = { state.back() })
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(96.dp)) {
                    InitialTile(user?.company?.ifBlank { null } ?: user?.name, 96.dp, user?.avatar)
                    Box(Modifier.align(Alignment.BottomEnd).size(32.dp).clip(R.Pill).background(C.Gold).border(3.dp, C.Bg, R.Pill), contentAlignment = Alignment.Center) {
                        Icon(Ic.CameraBold, null, tint = C.OnAccent, modifier = Modifier.size(16.dp))
                    }
                }
                val photoDraft = tr("Bitte mein Profilbild ändern.", "Please change my profile photo.", "يرجى تغيير صورة ملفي.")
                Text(tr("Profilbild ändern", "Change profile photo", "تغيير صورة الملف"), color = C.GoldLabel, style = T.Footnote, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { state.go(Screen.Chat(draft = photoDraft)) })
            }
            EditField(tr("Name", "Name", "الاسم"), name) { name = it }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EditField(tr("Benutzername", "Username", "اسم المستخدم"), handle) { handle = "@" + it.removePrefix("@").lowercase().filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' } }
                Text(tr("Dein Profil: focusontop.de/$handle", "Your profile: focusontop.de/$handle", "ملفك: focusontop.de/$handle"), color = C.Muted, style = T.Caption)
            }
            Box(
                Modifier.fillMaxWidth().height(50.dp).clip(R.Pill).background(C.Gold).clickable(enabled = !busy) {
                    busy = true
                    scope.launch {
                        runCatching { state.api.requestProfileUpdate(mapOf("name" to name.trim(), "username" to handle.removePrefix("@"))) }
                            .onSuccess { state.showToast(sent); state.back() }.onFailure { state.showToast(failed) }
                        busy = false
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = C.OnAccent, strokeWidth = 2.dp)
                else Text(tr("Speichern", "Save", "حفظ"), color = C.OnAccent, style = T.Body, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** 14b input: label Footnote semibold, 52 high, glass-strong, radius 14, padding 0/14, Body. */
@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth().height(52.dp).clip(R.FieldShape).background(C.GlassStrong).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
            androidx.compose.foundation.text.BasicTextField(value, onChange, singleLine = true, textStyle = T.Body.copy(color = C.Ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(C.Gold), modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Figma "Profile statistic": Title 2 semibold + Micro, centred, gap 4. */
@Composable
private fun Stat(n: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(n.toString(), color = C.Ink, style = T.Title2, textAlign = TextAlign.Center)
        Text(label, color = C.Muted, style = T.Micro, textAlign = TextAlign.Center)
    }
}

/** Figma "Type filter" 54 × 44: icon 22, selected = label/primary + 28 × 2 gold line at the bottom. */
@Composable
fun GridTab(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(54.dp, 44.dp).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (selected) C.Ink else C.Muted, modifier = Modifier.size(22.dp))
        if (selected) Box(Modifier.align(Alignment.BottomCenter).width(28.dp).height(2.dp).background(C.Gold))
    }
}

/** Plan with the three usage meters (storage, reels, shooting hours). */
@Composable
fun PlanUsageCard(state: AppState, home: Home?) {
    val sub = home?.subscription
    val u = home?.usage ?: Usage()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Surface.copy(alpha = 0.85f))
            .border(1.dp, C.Border.copy(alpha = 0.6f), RoundedCornerShape(22.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).border(1.dp, C.Border, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(Ic.Shield, null, tint = C.Gold, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(planName(state, home), color = C.Ink, style = T.Headline, fontWeight = FontWeight.SemiBold, maxLines = 1)
                val line = listOfNotNull(
                    u.monthsTotal?.let { tr("$it-Monats-Abo", "$it-month plan", "اشتراك $it أشهر") },
                    u.monthIndex?.let { tr("Monat $it", "Month $it", "الشهر $it") },
                    sub?.endDate?.let { tr("verlängert ${fmtDate(it)}", "renews ${fmtDate(it)}", "يتجدد ${fmtDate(it)}") },
                ).joinToString(" · ")
                if (line.isNotEmpty()) Text(line, color = C.Muted, style = T.Caption, maxLines = 2)
            }
            if (sub?.status == "active") OutlinePill(tr("Aktiv", "Active", "نشطة"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            UsageMeter(tr("Speicher", "Storage", "التخزين"), "${gb(u.storageUsedBytes)} GB", u.storageLimitGb?.let { "/ ${num(it)}" }, u.storageLimitGb?.let { (u.storageUsedBytes / (it * 1e9)).toFloat() }, Modifier.weight(1f))
            UsageMeter(tr("Reels/Monat", "Reels/month", "ريلز الشهر"), u.reelsThisMonth.toString(), u.reelsLimit?.let { "/ ${num(it)}" }, u.reelsLimit?.let { (u.reelsThisMonth / it).toFloat() }, Modifier.weight(1f))
            UsageMeter(tr("Drehstunden", "Shoot hours", "ساعات التصوير"), "${num(u.shootHoursThisMonth)} h", u.shootHoursLimit?.let { "/ ${num(it)} h" }, u.shootHoursLimit?.let { (u.shootHoursThisMonth / it).toFloat() }, Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clickable { openUrl(Media.web("subscriptions")) }, verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Tarife vergleichen", "Compare plans", "مقارنة الباقات"), color = C.Gold, style = T.BodyS, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Icon(Ic.Forward, null, tint = C.Gold, modifier = Modifier.size(16.dp))
            }
            val draft = tr("Ich interessiere mich für ein Upgrade meines Tarifs.", "I'm interested in upgrading my plan.", "أرغب في ترقية باقتي.")
            GoldButton("Upgrade", { state.go(Screen.Chat(draft = draft)) }, small = true)
        }
    }
}

@Composable
private fun UsageMeter(label: String, value: String, limit: String?, fraction: Float?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = C.Muted, style = T.Micro, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = C.Ink, style = T.Headline, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (limit != null) Text(" $limit", color = C.Muted, style = T.Caption, maxLines = 1)
        }
        SegmentMeter(fraction ?: 0f, segments = 8, height = 4.dp)
    }
}

/* ───────────── settings ───────────── */

/** Figma 28 · Konto · Einstellungen. */
@Composable
fun SettingsScreen(state: AppState) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteDone by remember { mutableStateOf(false) }
    var langMenu by remember { mutableStateOf(false) }
    var notifOn by remember { mutableStateOf(Prefs.get("notif_off") != "1") }
    val priv = state.me?.private ?: true

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(tr("Einstellungen", "Settings", "الإعدادات"), onBack = { state.back() })
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item { SettingsLabel(tr("Dein Konto", "Your account", "حسابك")) }
            item {
                SettingsCard {
                    SettingsRow(Ic.UserRound, tr("Persönliche Daten", "Personal details", "المعلومات الشخصية")) { state.go(Screen.EditProfile) }
                    SettingsRow(Ic.ShieldCheck15, tr("Sicherheit & 2-Faktor", "Security & 2-factor", "الأمان والتحقق بخطوتين"), trailing = Ic.External) { openUrl(Media.web("profile")) }
                    SettingsRow(Ic.Card, tr("Tarif, Rechnungen & Zahlung", "Plan, invoices & payment", "الباقة والفواتير والدفع")) { state.go(Screen.Invoices) }
                }
            }
            item { SettingsLabel(tr("Community", "Community", "المجتمع")) }
            item {
                SettingsCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Ic.LockKeyhole, null, tint = C.Ink, modifier = Modifier.size(22.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(tr("Privates Konto", "Private account", "حساب خاص"), color = C.Ink, style = T.BodyS)
                            Text(if (priv) tr("Nur du und das Team sehen deine Beiträge.", "Only you and the team see your posts.", "منشوراتك تظهر لك وللفريق فقط.")
                                else tr("Alle Kunden können deinen Arbeiten folgen.", "Every client can follow your work.", "يمكن لكل العملاء متابعة أعمالك."), color = C.Muted, style = T.Caption)
                        }
                        FtToggle(priv) { state.setPrivate(it) }
                    }
                }
            }
            item { SettingsLabel(tr("Einstellungen", "Preferences", "التفضيلات")) }
            item {
                SettingsCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Ic.Bell, null, tint = C.Ink, modifier = Modifier.size(22.dp))
                        Text(tr("Mitteilungen", "Notifications", "الإشعارات"), color = C.Ink, style = T.BodyS, modifier = Modifier.weight(1f))
                        FtToggle(notifOn) { on -> notifOn = on; Prefs.set("notif_off", if (on) null else "1"); if (on) requestNotificationPermission() }
                    }
                    Box {
                        SettingsRow(Ic.Languages, s.language, sub = state.lang.label, trailing = Ic.ChevronDown) { langMenu = true }
                        DropdownMenu(langMenu, { langMenu = false }, containerColor = C.Surface, shape = RoundedCornerShape(R.Menu), border = androidx.compose.foundation.BorderStroke(1.dp, C.Separator)) {
                            Lang.entries.forEach { l ->
                                DropdownMenuItem(text = { Text(l.label, color = if (l == state.lang) C.GoldLabel else C.Ink, style = T.Body) },
                                    trailingIcon = { if (l == state.lang) Icon(Ic.Check, null, tint = C.Ink, modifier = Modifier.size(18.dp)) },
                                    onClick = { state.setLanguage(l); langMenu = false })
                            }
                        }
                    }
                    SettingsRow(Ic.Moon, tr("Darstellung", "Appearance", "المظهر"),
                        sub = if (ThemeState.dark) tr("Dunkel", "Dark", "داكن") else tr("Hell", "Light", "فاتح"),
                        trailing = if (ThemeState.dark) Ic.Moon else Ic.Sun) {
                        ThemeState.dark = !ThemeState.dark; Prefs.set("theme", if (ThemeState.dark) "dark" else "light")
                    }
                }
            }
            item { SettingsLabel(tr("Tools nach Berechtigung", "Tools", "الأدوات حسب صلاحياتك")) }
            item {
                SettingsCard {
                    SettingsRow(Ic.Sparkles, "Trendo AI", trailing = Ic.External) { openUrl(Media.web("trendo")) }
                    SettingsRow(Ic.FolderCheck, tr("Projekte & Aufgaben", "Projects & tasks", "المشاريع والمهام")) { state.go(Screen.Projects) }
                    SettingsRow(Ic.Receipt, tr("Rechnungen", "Invoices", "الفواتير")) { state.go(Screen.Invoices) }
                }
            }
            item { SettingsLabel(tr("Support", "Support", "الدعم")) }
            item {
                SettingsCard {
                    SettingsRow(Ic.Help, tr("Hilfe & Support kontaktieren", "Help & contact support", "المساعدة والتواصل مع الدعم")) { state.go(Screen.Chat()) }
                    SettingsRow(Ic.Shield, tr("Rechtliches & Datenschutz", "Legal & privacy", "القانونية والخصوصية"), trailing = Ic.External) { state.go(Screen.Legal) }
                    SettingsRow(Ic.Logout, s.logout) { confirmLogout = true }
                    SettingsRow(Ic.Trash, if (deleteDone) s.deleteRequested else tr("Konto löschen anfragen", "Request account deletion", "طلب حذف الحساب"), danger = true) { if (!deleteDone) confirmDelete = true }
                }
            }
        }
    }
    if (confirmLogout) ConfirmDialog(tr("Möchtest du dich abmelden?", "Do you want to sign out?", "هل تريد تسجيل الخروج؟"), onDismiss = { confirmLogout = false }) { confirmLogout = false; state.signOut() }
    if (confirmDelete) ConfirmDialog(tr("Deine Anfrage geht zur Bestätigung an uns. Sicher?", "Your request goes to us for confirmation. Are you sure?", "سيصلنا طلبك للتأكيد. هل أنت متأكد؟"), onDismiss = { confirmDelete = false }) {
        confirmDelete = false
        scope.launch { runCatching { state.api.requestDelete() }.onSuccess { deleteDone = true } }
    }
}

@Composable
private fun SettingsLabel(text: String) { Text(text, color = C.GoldLabel, style = T.Footnote, fontWeight = FontWeight.SemiBold) }

/** Figma settings "Card": bg/secondary + glass hairline, radius 20, padding 16, gap 16 with separators between rows. */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.dp, C.StrokeGlass, R.CardShape).padding(16.dp)) {
        androidx.compose.ui.layout.Layout(content = content) { measurables, constraints ->
            val gap = 16.dp.roundToPx(); val line = 1.dp.roundToPx()
            val ps = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
            val h = ps.sumOf { it.height } + (ps.size - 1).coerceAtLeast(0) * (gap * 2 + line)
            layout(constraints.maxWidth, h) { var y = 0; ps.forEachIndexed { k, p -> if (k > 0) y += gap * 2 + line; p.place(0, y); y += p.height } }
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, sub: String? = null, trailing: ImageVector = Ic.ChevronRight, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = if (danger) C.Red else C.Ink, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = if (danger) C.Red else C.Ink, style = T.BodyS)
            if (sub != null) Text(sub, color = C.Muted, style = T.Caption)
        }
        Icon(trailing, null, tint = C.Muted, modifier = Modifier.size(18.dp))
    }
}

/** Figma 28c · Rechtliches & Datenschutz. */
@Composable
fun LegalScreen(state: AppState) {
    val rows = listOf(
        tr("Impressum", "Imprint", "بيانات الناشر") to "https://www.focusontop.de",
        tr("Datenschutzerklärung", "Privacy policy", "سياسة الخصوصية") to Media.PRIVACY,
        tr("Allgemeine Geschäftsbedingungen", "Terms & conditions", "الشروط والأحكام") to "https://www.focusontop.de",
        tr("Widerrufsbelehrung", "Cancellation policy", "سياسة الإلغاء") to "https://www.focusontop.de",
    )
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(tr("Rechtliches & Datenschutz", "Legal & privacy", "القانونية والخصوصية"), onBack = { state.back() })
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsLabel(tr("Rechtliches", "Legal", "قانوني"))
            Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface)) {
                rows.forEachIndexed { k, (label, url) ->
                    if (k > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
                    Row(Modifier.fillMaxWidth().clickable { openUrl(url) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Ic.FileText, null, tint = C.Ink, modifier = Modifier.size(20.dp))
                        Text(label, color = C.Ink, style = T.BodyS, modifier = Modifier.weight(1f))
                        Icon(Ic.External, null, tint = C.Muted, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(tr("Version 2.0.0", "Version 2.0.0", "الإصدار 2.0.0"), color = C.Faint, style = T.Caption, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

/* ───────────── 29 · Rechnungen ───────────── */

/** "832,00 €" (German grouping). */
fun euro(v: Double?, currency: String?): String {
    if (v == null) return "—"
    val whole = v.toLong(); val cents = kotlin.math.round((v - whole) * 100).toLong()
    val grouped = whole.toString().reversed().chunked(3).joinToString(".").reversed()
    val c = when (currency?.uppercase()) { null, "", "EUR", "€" -> "€"; else -> currency }
    return "$grouped,${cents.toString().padStart(2, '0')} $c"
}

@Composable
fun InvoicesScreen(state: AppState) {
    val s = LocalStrings.current
    val home = state.home
    val list = home?.invoices.orEmpty().sortedByDescending { it.issuedDate ?: it.dueDate ?: "" }
    val today = todayIso()
    val open = list.filter { it.status != "paid" && it.status != "bezahlt" }
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(s.tabInvoices, onBack = { state.back() })
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val sub = home?.subscription
            if (sub?.contractStatus == "sent" && !sub.contractToken.isNullOrBlank()) item { PrimaryButton(s.signContract, onClick = { openUrl(Media.contract(sub.contractToken)) }) }
            open.firstOrNull()?.let { inv ->
                item {
                    val sum = open.sumOf { invoiceTotal(it) ?: 0.0 }
                    Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.5.dp, C.Gold, R.CardShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Ic.Wallet, null, tint = C.GoldLabel, modifier = Modifier.size(16.dp))
                            Text(tr("Offener Betrag", "Amount due", "المبلغ المستحق"), color = C.GoldLabel, style = T.Footnote, fontWeight = FontWeight.SemiBold)
                        }
                        Text(euro(sum, inv.currency), color = C.Ink, style = T.Title1)
                        Text(listOfNotNull(inv.dueDate?.let { tr("Fällig am ${fmtDate(it)}", "Due ${fmtDate(it)}", "الاستحقاق ${fmtDate(it)}") }, inv.invoiceNumber).joinToString(" · "), color = C.Muted, style = T.Caption)
                        Box(Modifier.fillMaxWidth().height(50.dp).clip(R.Pill).background(C.Gold).clickable {
                            (Media.download(inv.attachmentUrl, "${s.invoice}-${inv.invoiceNumber ?: inv.id}.pdf") ?: Media.web("invoices")).let(::openUrl)
                        }, contentAlignment = Alignment.Center) {
                            Text(tr("Jetzt bezahlen", "Pay now", "ادفع الآن"), color = C.OnAccent, style = T.Body, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (list.isEmpty()) item { EmptyState(s.noInvoices, icon = Ic.Receipt) }
            list.groupBy { (it.issuedDate ?: it.dueDate ?: "").take(7) }.forEach { (ym, invs) ->
                item(key = "m-$ym") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (ym.length == 7) Text(monthLabel(s.months, ym), color = C.Muted, style = T.Footnote, fontWeight = FontWeight.SemiBold)
                        Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface)) {
                            invs.forEachIndexed { k, inv ->
                                if (k > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
                                val st = invoiceStatus(s, inv, today)
                                val paid = st.label == s.paid
                                val pdf = Media.download(inv.attachmentUrl, "${s.invoice}-${inv.invoiceNumber ?: inv.id}.pdf")
                                Row(Modifier.fillMaxWidth().clickable(enabled = pdf != null) { pdf?.let(::openUrl) }.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Ic.FileText175, null, tint = C.Muted, modifier = Modifier.size(22.dp))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(s.invoice, color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(inv.invoiceNumber ?: "—", color = C.Muted, style = T.Caption, maxLines = 1)
                                    }
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(euro(invoiceTotal(inv), inv.currency), color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold)
                                        Text(st.label, color = if (paid) C.Green else if (st.fg == C.Red) C.Red else C.GoldLabel, style = T.Caption, fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.height(22.dp).clip(R.Pill).background(if (paid) C.Green.copy(alpha = 0.14f) else if (st.fg == C.Red) C.Red.copy(alpha = 0.14f) else C.GoldTint).padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ───────────── 30 · Projekte  /  30b · Projekt-Details ───────────── */

private val STAGES: List<String> @Composable get() = listOf(tr("Briefing", "Briefing", "الموجز"), tr("Dreh", "Shoot", "التصوير"), tr("Schnitt", "Edit", "المونتاج"), tr("Freigabe", "Approval", "الموافقة"), tr("Lieferung", "Delivery", "التسليم"))

/** Stage reached by a project: 0 Briefing … 4 Lieferung, 5 = done. */
private fun projectStage(tasks: List<Task>, files: List<ClientFile>): Int {
    if (tasks.isNotEmpty() && tasks.all { it.status == "completed" }) return 5
    if (files.any { it.status == "ready_for_review" } || tasks.any { it.status == "review" || it.status == "revision_requested" }) return 3
    if (tasks.any { it.status == "in_progress" }) return 2
    return if (files.isNotEmpty()) 2 else 1
}

@Composable
fun ProjectsScreen(state: AppState) {
    val s = LocalStrings.current
    val home = state.home
    val tasks = home?.tasks.orEmpty()
    val other = tr("Weitere", "Other", "أخرى")
    val projects = tasks.groupBy { it.projectName ?: other }.map { (name, list) -> Triple(name, list, home?.files.orEmpty().filter { it.projectName == name }) }
    val active = projects.filter { projectStage(it.second, it.third) < 5 }
    val done = projects.filter { projectStage(it.second, it.third) >= 5 }
    var tab by remember { mutableStateOf(0) }
    val today = todayIso()
    val next = home?.appointments.orEmpty().filter { it.date >= today }.minByOrNull { it.date }
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(tr("Projekte", "Projects", "المشاريع"), onBack = { state.back() }, actions = {
            HeaderIcon(Ic.Plus175, tr("Neues Projekt", "New project", "مشروع جديد")) { state.go(Screen.NewProject) }
        })
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth().height(42.dp).clip(R.Pill).background(C.GlassStrong).padding(4.dp)) {
                    listOf(tr("Aktiv (${active.size})", "Active (${active.size})", "نشطة (${active.size})"), tr("Abgeschlossen (${done.size})", "Completed (${done.size})", "مكتملة (${done.size})")).forEachIndexed { i, l ->
                        Box(Modifier.weight(1f).fillMaxHeight().clip(R.Pill).then(if (tab == i) Modifier.background(C.Surface2) else Modifier).clickable { tab = i }, contentAlignment = Alignment.Center) {
                            Text(l, color = if (tab == i) C.Ink else C.Muted, style = T.Footnote, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectStat(Ic.Clapper175, active.count { projectStage(it.second, it.third) in 1..2 }.toString(), tr("In Produktion", "In production", "قيد الإنتاج"), Modifier.weight(1f))
                    ProjectStat(Ic.Eye175, (home?.files.orEmpty().count { !it.mine && it.status == "ready_for_review" }).toString(), tr("Zur Freigabe", "To approve", "للموافقة"), Modifier.weight(1f))
                    ProjectStat(Ic.Calendar175, next?.let { "${it.date.substring(8, 10).toInt()}. ${s.months[it.date.substring(5, 7).toInt() - 1].take(3)}." } ?: "—", tr("Nächster Termin", "Next appointment", "الموعد القادم"), Modifier.weight(1f))
                }
            }
            val shown = if (tab == 0) active else done
            if (shown.isEmpty()) item { EmptyState(s.noTasks, icon = Ic.FolderCheck) }
            items(shown, key = { it.first }) { (name, list, files) -> ProjectCard(name, list, files) { state.go(Screen.ProjectDetail(name)) } }
        }
    }
}

@Composable
private fun ProjectStat(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Column(modifier.height(92.dp).clip(R.CardShape).background(C.Surface).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = C.GoldLabel, modifier = Modifier.size(18.dp))
        Text(value, color = C.Ink, style = T.Headline, maxLines = 1)
        Text(label, color = C.Muted, style = T.Caption, maxLines = 1)
    }
}

@Composable
private fun ProjectCard(name: String, tasks: List<Task>, files: List<ClientFile>, onClick: () -> Unit) {
    val stages = STAGES
    val stage = projectStage(tasks, files)
    val review = stage == 3
    val due = tasks.mapNotNull { it.deliveryDate }.minOrNull()
    Column(
        Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).then(if (review) Modifier.border(1.5.dp, C.Gold, R.CardShape) else Modifier)
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).clip(R.ThumbShape).background(C.Surface2)) { files.firstOrNull()?.let { FileCover(it, Modifier.fillMaxSize(), 200) } }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tasks.mapNotNull { it.title }.take(2).joinToString(" · ").ifBlank { tr("${tasks.size} Aufgaben", "${tasks.size} tasks", "${tasks.size} مهام") }, color = C.Muted, style = T.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Ic.ChevronRight175, null, tint = C.Muted, modifier = Modifier.size(18.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (k in 0 until 5) Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(when { k < stage -> C.Green; k == stage -> C.Gold; else -> C.Surface2 }))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stages[stage.coerceAtMost(4)], color = C.OnAccent, style = T.Caption, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.height(24.dp).clip(R.Pill).background(C.Gold).padding(horizontal = 10.dp, vertical = 4.dp))
            Text("${(stage.coerceAtMost(5) * 20)} %", color = C.Muted, style = T.Caption, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (due != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Ic.Calendar175, null, tint = C.Muted, modifier = Modifier.size(13.dp))
                Text(tr("Abgabe ${fmtDate(due)}", "Due ${fmtDate(due)}", "التسليم ${fmtDate(due)}"), color = C.Muted, style = T.Caption)
            }
        }
    }
}

@Composable
fun ProjectDetailScreen(state: AppState, name: String) {
    val s = LocalStrings.current
    val home = state.home
    val tasks = home?.tasks.orEmpty().filter { (it.projectName ?: tr("Weitere", "Other", "أخرى")) == name }
    val files = home?.files.orEmpty().filter { it.projectName == name }
    val stage = projectStage(tasks, files)
    val stages = STAGES
    val pending = files.filter { !it.mine && it.status == "ready_for_review" }
    Column(Modifier.fillMaxSize().background(C.Bg).navigationBarsPadding()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(210.dp).background(C.Surface2)) {
                    files.firstOrNull()?.let { FileCover(it, Modifier.fillMaxSize(), 1000) }
                    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
                    Box(Modifier.statusBarsPadding().padding(16.dp).size(36.dp).clip(R.Pill).background(Color.Black.copy(alpha = 0.40f)).clickable { state.back() }, contentAlignment = Alignment.Center) {
                        Icon(Ic.Back, s.back, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (stage == 3) tr("Zur Freigabe", "To approve", "للموافقة") else stages[stage.coerceAtMost(4)], color = C.OnAccent, style = T.Caption, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.height(24.dp).clip(R.Pill).background(C.Gold).padding(horizontal = 10.dp, vertical = 4.dp))
                        Text(name, color = Color.White, style = T.Title2, maxLines = 2)
                        Text(tr("${tasks.size} Aufgaben · ${files.size} Dateien", "${tasks.size} tasks · ${files.size} files", "${tasks.size} مهام · ${files.size} ملفات"), color = Color.White.copy(alpha = 0.8f), style = T.Footnote)
                    }
                }
            }
            val delivered = files.filter { !it.mine && (it.status == "approved" || it.status == "delivered" || it.status == "published") }
            if (delivered.isNotEmpty()) item {
                Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(50.dp).clip(R.Pill).background(C.Gold).clickable { state.go(Screen.Delivery(name)) },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    Icon(Ic.FolderCheck, null, tint = C.OnAccent, modifier = Modifier.size(18.dp))
                    Text(tr("Lieferung ansehen", "View delivery", "عرض التسليم"), color = C.OnAccent, style = T.Body, fontWeight = FontWeight.SemiBold)
                }
            }
            if (pending.isNotEmpty()) item {
                Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(R.CardShape).background(C.GoldTint).border(1.dp, C.Gold, R.CardShape).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(pending.first().title() + " " + tr("ist bereit", "is ready", "جاهز"), color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(tr("Prüfe das Video und gib es frei.", "Review and approve it.", "راجعه ووافق عليه."), color = C.Muted, style = T.Caption)
                    }
                    Text(tr("Prüfen", "Review", "راجع"), color = C.OnAccent, style = T.Footnote, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.height(36.dp).clip(R.Pill).background(C.Gold).clickable { state.go(Screen.Viewer(pending.map { it.id }, 0)) }.padding(horizontal = 16.dp, vertical = 9.dp))
                }
            }
            item { Text(tr("Fortschritt", "Progress", "التقدم"), color = C.Ink, style = T.Headline, modifier = Modifier.padding(horizontal = 16.dp)) }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    stages.forEachIndexed { k, label ->
                        val doneK = k < stage; val now = k == stage
                        Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.width(24.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(24.dp).clip(R.Pill).background(when { doneK -> C.Green; now -> C.Gold; else -> C.Surface2 }), contentAlignment = Alignment.Center) {
                                    if (doneK) Icon(Ic.CheckBold, null, tint = C.Bg, modifier = Modifier.size(13.dp))
                                }
                                if (k < 4) Box(Modifier.width(2.dp).weight(1f).background(if (doneK) C.Green else C.Surface2))
                            }
                            Column(Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(label, color = if (k > stage) C.Muted else C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold)
                                Text(when { doneK -> tr("Abgeschlossen", "Completed", "مكتمل"); now && k == 3 -> tr("Wartet auf dich", "Waiting for you", "بانتظارك"); now -> tr("In Arbeit", "In progress", "قيد العمل"); else -> tr("Geplant", "Planned", "مخطط") }
                                    + (if (k == 4) tasks.mapNotNull { it.deliveryDate }.minOrNull()?.let { " · " + fmtDate(it) } ?: "" else ""), color = C.Muted, style = T.Caption)
                            }
                        }
                    }
                }
            }
            if (files.isNotEmpty()) item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Dateien", "Files", "الملفات"), color = C.Ink, style = T.Headline, modifier = Modifier.weight(1f))
                        Text(tr("Alle (${files.size})", "All (${files.size})", "الكل (${files.size})"), color = C.GoldLabel, style = T.Footnote, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { state.go(Screen.Viewer(files.map { it.id }, 0)) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        files.take(3).forEachIndexed { k, f -> MediaTile(f, Modifier.weight(1f).height(110.dp)) { state.go(Screen.Viewer(files.map { it.id }, k)) } }
                        repeat(3 - files.take(3).size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    Row(Modifier.fillMaxWidth().height(50.dp).clip(R.Pill).background(C.GlassStrong).clickable { state.go(Screen.Chat(draft = "$name: ")) },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        Icon(Ic.Chat, null, tint = C.Ink, modifier = Modifier.size(18.dp))
                        Text(tr("Nachricht ans Team", "Message the team", "رسالة إلى الفريق"), color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
