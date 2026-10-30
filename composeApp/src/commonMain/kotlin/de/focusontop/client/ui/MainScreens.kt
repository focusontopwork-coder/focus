@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.focusontop.client.ui

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.draw.shadow
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import de.focusontop.client.data.AppState
import de.focusontop.client.data.ClientFile
import de.focusontop.client.data.Media
import de.focusontop.client.data.Screen
import de.focusontop.client.data.Tab
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.i18n.tr
import de.focusontop.client.platform.requestNotificationPermission
import de.focusontop.client.platform.rememberFilePicker
import kotlinx.coroutines.delay

/** Room under the floating navigation bar so the last item is never hidden. */
val bottomSpace = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 130.dp)

@Composable
fun MainScreen(state: AppState) {
    var createOpen by remember { mutableStateOf(false) }
    val picker = rememberFilePicker { state.upload(it) }
    val changeDraft = tr("Änderungswunsch: ", "Change request: ", "طلب تعديل: ")
    LaunchedEffect(Unit) {
        requestNotificationPermission()
        // keep the home data fresh while the app is open
        while (true) { delay(60_000); state.refresh() }
    }
    Box(Modifier.fillMaxSize().screenBg()) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            when (state.tab) {
                Tab.HOME -> HomeTab(state, onUpload = { picker.pickMedia() })
                Tab.SEARCH -> SearchScreen(state)
                Tab.FILES -> if (state.reelsMode) ReelsScreen(state, onGrid = { state.reelsMode = false }, onUpload = { picker.pickMedia() }) else FilesTab(state, onUpload = { picker.pickMedia() })
                Tab.CALENDAR -> CalendarTab(state)
                Tab.PROFILE -> ProfileTab(state)
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            UploadPill(state)
            BottomNav(state) { createOpen = true }
        }
        CreateSheet(
            open = createOpen,
            onClose = { createOpen = false },
            onAppointment = { createOpen = false; state.go(Screen.AppointmentRequest) },
            onFile = { createOpen = false; picker.pickFiles() },
            onFolder = { createOpen = false; picker.pickFolder() },
            onChange = { createOpen = false; state.go(Screen.Chat(draft = changeDraft)) },
            onMessage = { createOpen = false; state.go(Screen.Chat()) },
            onStory = { createOpen = false; state.go(if (de.focusontop.client.platform.inAppCameraAvailable) Screen.StoryCamera else Screen.Compose("story")) },
            onPost = { createOpen = false; state.go(Screen.Compose("post")) },
        )
    }
}

/* ───────────── navigation ───────────── */

/**
 * Figma component "Tab bar": floating glass capsule 300×60, centred, 24 above the
 * home indicator. Five icon-only destinations; the active one turns gold-label with a
 * heavier stroke; Create is a bare gold "+". Calendar lives under Profile → Kalender.
 */
@Composable
private fun BottomNav(state: AppState, onCreate: () -> Unit) {
    Row(
        Modifier.width(300.dp).height(60.dp)
            .shadow(18.dp, R.Pill, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(R.Pill).background(C.NavGlass)
            .border(1.dp, C.StrokeGlass, R.Pill).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        NavItem(tr("Start", "Home", "الرئيسية"), state.tab == Tab.HOME, { state.tab = Tab.HOME }) { sel ->
            Icon(if (sel) Ic.HomeFill else Ic.Home, null, tint = if (sel) C.GoldLabel else C.Ink, modifier = Modifier.size(24.dp))
        }
        NavItem(tr("Suche", "Search", "بحث"), state.tab == Tab.SEARCH, { state.tab = Tab.SEARCH }) { sel ->
            Icon(if (sel) Ic.SearchActive else Ic.Search, null, tint = if (sel) C.GoldLabel else C.Ink, modifier = Modifier.size(24.dp))
        }
        Box(
            Modifier.size(width = 40.dp, height = 40.dp).clip(R.Pill)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = tr("Erstellen", "Create", "إنشاء"), onClick = onCreate),
            contentAlignment = Alignment.Center,
        ) { Icon(Ic.PlusBold, tr("Erstellen", "Create", "إنشاء"), tint = C.GoldLabel, modifier = Modifier.size(26.dp)) }
        NavItem(tr("Reels", "Reels", "ريلز"), state.tab == Tab.FILES, { state.tab = Tab.FILES }) { sel ->
            Icon(if (sel) Ic.ClapperActive else Ic.Clapper, null, tint = if (sel) C.GoldLabel else C.Ink, modifier = Modifier.size(24.dp))
        }
        NavItem(tr("Profil", "Profile", "حسابي"), state.tab == Tab.PROFILE || state.tab == Tab.CALENDAR, { state.tab = Tab.PROFILE }) { sel ->
            Icon(if (sel) Ic.UserActive else Ic.User, null, tint = if (sel) C.GoldLabel else C.Ink, modifier = Modifier.size(24.dp))
        }
    }
}

/** 56×48 hit area, no background — matches "Tab · <name>" in Figma. */
@Composable
private fun NavItem(label: String, selected: Boolean, onClick: () -> Unit, icon: @Composable (Boolean) -> Unit) {
    Box(
        Modifier.size(width = 56.dp, height = 48.dp).clip(R.Pill)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { icon(selected) }
}

/** Running uploads, shown just above the navigation. */
@Composable
private fun UploadPill(state: AppState) {
    val jobs = state.uploads
    if (jobs.isEmpty()) return
    val total = jobs.sumOf { it.size }.coerceAtLeast(1)
    val sent = jobs.sumOf { if (it.done) it.size else it.sent }
    val failed = jobs.count { it.failed }
    val done = jobs.count { it.done }
    val running = state.uploading
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(C.Surface2)
            .border(1.dp, C.Gold.copy(alpha = 0.5f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            running -> CircularProgressIndicator(progress = { sent.toFloat() / total }, modifier = Modifier.size(22.dp), color = C.Gold, strokeWidth = 3.dp, trackColor = C.Border)
            failed > 0 -> Icon(Ic.Alert, null, tint = C.Red, modifier = Modifier.size(22.dp))
            else -> Icon(Ic.CheckCircle, null, tint = C.GreenText, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (running) tr("Wird hochgeladen", "Uploading", "جارٍ الرفع") + " · ${done + 1}/${jobs.size}"
                else if (failed > 0) tr("$failed fehlgeschlagen", "$failed failed", "تعذّر رفع $failed")
                else tr("Hochgeladen – das Team ist informiert", "Uploaded – the team has been told", "تم الرفع وأُبلغ الفريق"),
                color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1,
            )
            if (running) Text(jobs.firstOrNull { !it.done && !it.failed }?.name ?: "", color = C.Muted, style = T.Micro, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (running) Text("${(sent * 100 / total)}%", color = C.Gold, style = T.Footnote, fontWeight = FontWeight.SemiBold)
    }
}

/* ───────────── create sheet (+) ───────────── */

@Composable
private fun CreateSheet(
    open: Boolean,
    onClose: () -> Unit,
    onAppointment: () -> Unit,
    onFile: () -> Unit,
    onFolder: () -> Unit,
    onChange: () -> Unit,
    onMessage: () -> Unit,
    onStory: () -> Unit,
    onPost: () -> Unit,
) {
    AnimatedVisibility(open, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(C.Scrim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose))
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(open, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            // Figma B · "Bottom sheet": bg/secondary, separator 1, padding 40/16/32/16, gap 24
            Box(
                Modifier.fillMaxWidth().clip(R.SheetShape).background(C.Surface).border(1.dp, C.Separator, R.SheetShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            ) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 12.dp).width(36.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(C.Muted))
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(tr("Erstellen", "Create", "إنشاء"), color = C.Ink, style = T.Title3, modifier = Modifier.weight(1f))
                        Icon(Ic.X, tr("Schließen", "Close", "إغلاق"), tint = C.Muted, modifier = Modifier.size(22.dp).clickable(onClick = onClose))
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.dp, C.StrokeGlass, R.CardShape)
                            .clickable(onClick = onAppointment).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Ic.CalendarPlus, null, tint = C.Ink, modifier = Modifier.size(30.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(tr("Termin anfragen", "Request appointment", "طلب موعد"), color = C.GoldLabel, style = T.Headline)
                            Text(tr("Meeting, Dreh oder Call mit dem Team", "Meeting, shoot or call with the team", "اجتماع أو تصوير أو مكالمة مع الفريق"), color = C.Muted, style = T.Footnote)
                        }
                        Icon(Ic.ChevronRight, null, tint = C.Muted, modifier = Modifier.size(22.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CreateTile(Ic.PlusCircle, tr("Story teilen", "Share a story", "نشر قصة"), onStory, Modifier.weight(1f))
                        CreateTile(Ic.SquarePen, tr("Beitrag posten", "Create a post", "نشر منشور"), onPost, Modifier.weight(1f))
                        CreateTile(Ic.Upload, tr("Datei hochladen", "Upload file", "رفع ملف"), onFile, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CreateTile(Ic.ListChecks, tr("Änderung / Aufgabe", "Change / task", "طلب تعديل أو مهمة"), onChange, Modifier.weight(1f))
                        CreateTile(Ic.Chat, tr("Nachricht ans Team", "Message the team", "رسالة إلى الفريق"), onMessage, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Figma "Create destination": icon 26 + Caption, padding 12 vertical, gap 12 — no tile background. */
@Composable
private fun CreateTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(R.CardShape).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = C.Ink, modifier = Modifier.size(26.dp))
        Text(label, color = C.Ink, style = T.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SheetRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = C.Gold, modifier = Modifier.size(23.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, color = C.Ink, style = T.Headline, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Ic.ChevronRight, null, tint = C.Muted)
    }
}

/* ───────────── shared bits used by several screens ───────────── */

@Composable
fun Avatar(url: String?, name: String?, sizeDp: Int) {
    Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(C.Gold.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        val src = Media.image(url, 256)
        if (src != null) {
            AsyncImage(model = src, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text((name ?: "?").trim().take(1).uppercase(), color = C.Gold, fontSize = (sizeDp / 2.4).sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun Thumb(f: ClientFile, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        FileCover(f, Modifier.fillMaxSize())
        if (f.type == "Video") Box(Modifier.size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
            Icon(Ic.Play, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        if (f.type == "Document") Icon(Ic.File, null, tint = C.Gold)
    }
}

@Composable
fun AnswerButton(text: String, icon: ImageVector, filled: Boolean, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    if (filled) GoldButton(text, onClick, modifier, icon = icon, enabled = enabled)
    else GhostButton(text, { if (enabled) onClick() }, modifier)
}

/** Figma G · "Change request sheet": title + privacy line + close, change type chips, text area, gold send. */
@Composable
fun RevisionDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    val s = LocalStrings.current
    var notes by remember { mutableStateOf("") }
    val types = listOf(tr("Schnitt", "Edit", "المونتاج"), tr("Musik", "Music", "الموسيقى"), tr("Farbe", "Colour", "الألوان"), tr("Text & Titel", "Text & titles", "النصوص والعناوين"), tr("Sonstiges", "Other", "أخرى"))
    var type by remember { mutableStateOf(types.first()) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(C.Scrim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier.fillMaxWidth().clip(R.SheetShape).background(C.Surface).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding().imePadding().padding(top = 8.dp, bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.width(36.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(C.Separator))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tr("Änderung anfordern", "Request a change", "طلب تعديل"), color = C.Ink, style = T.Title3)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Ic.LockSm, null, tint = C.Muted, modifier = Modifier.size(14.dp))
                            Text(tr("Nur für dich und das Team sichtbar", "Only visible to you and the team", "مرئي لك وللفريق فقط"), color = C.Muted, style = T.Caption)
                        }
                    }
                    Box(Modifier.size(32.dp).clip(R.Pill).background(C.GlassStrong).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                        Icon(Ic.XSm, s.cancel, tint = C.Ink, modifier = Modifier.size(16.dp))
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(tr("Was soll geändert werden?", "What should change?", "ما الذي يجب تعديله؟"), color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEach { t ->
                            val on = t == type
                            Text(t, color = if (on) C.GoldLabel else C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1,
                                modifier = Modifier.height(if (on) 36.dp else 34.dp).clip(R.Pill).background(if (on) C.GoldTint else C.GlassStrong)
                                    .then(if (on) Modifier.border(1.dp, C.Gold, R.Pill) else Modifier).clickable { type = t }.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(96.dp).clip(R.FieldShape).background(C.GlassStrong).padding(horizontal = 14.dp, vertical = 12.dp)) {
                        if (notes.isEmpty()) Text(s.whatToChangeHint, color = C.Muted, style = T.BodyS)
                        BasicTextField(notes, { notes = it }, textStyle = T.BodyS.copy(color = C.Ink), cursorBrush = SolidColor(C.Gold), modifier = Modifier.fillMaxSize())
                    }
                }
                Box(Modifier.padding(horizontal = 16.dp)) {
                    Box(Modifier.fillMaxWidth().height(50.dp).clip(R.Pill).background(if (notes.isNotBlank()) C.Gold else C.Surface2)
                        .clickable(enabled = notes.isNotBlank()) { onSend("[$type] " + notes.trim()) }, contentAlignment = Alignment.Center) {
                        Text(tr("An Team senden", "Send to team", "إرسال إلى الفريق"), color = if (notes.isNotBlank()) C.OnAccent else C.Faint, style = T.Body, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Figma E / H / I · "Confirmation dialog": 358 wide, bg/secondary + separator, radius 20, padding 24, gap 24. */
@Composable
fun ConfirmDialog(text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val s = LocalStrings.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(C.Scrim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.dp, C.Separator, R.CardShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Icon(Ic.CircleHelp, null, tint = C.Ink, modifier = Modifier.size(28.dp))
                Text(text, color = C.Ink, style = T.Title3)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton(s.cancel, onClick = onDismiss, modifier = Modifier.weight(1f))
                    PrimaryButton(s.confirm, onClick = onConfirm, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Settings-style row: icon, text, value / chevron. */
@Composable
fun MenuRow(icon: ImageVector, text: String, value: String? = null, danger: Boolean = false, chevron: Boolean = true, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (danger) C.Red else C.Ink.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, color = if (danger) C.Red else C.Ink, style = T.Headline, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (trailing != null) trailing()
        else {
            if (value != null) Text(value, color = C.Muted, style = T.BodyS)
            if (chevron && value == null) Icon(Ic.ChevronRight, null, tint = C.Muted)
        }
    }
}

/** A rounded group of rows with thin dividers. */
@Composable
fun MenuGroup(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Surface.copy(alpha = 0.85f))
            .border(1.dp, C.Border.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
    ) { content() }
}

internal fun String.encodeForUrl(): String = buildString {
    for (b in this@encodeForUrl.encodeToByteArray()) {
        val c = b.toInt() and 0xff
        if ((c in 'a'.code..'z'.code) || (c in 'A'.code..'Z'.code) || (c in '0'.code..'9'.code) || c == '-'.code || c == '.'.code || c == '_'.code) append(c.toChar())
        else append('%').append(c.toString(16).uppercase().padStart(2, '0'))
    }
}

/** Unused-icon guard for the upload illustration in empty states. */
internal val UploadIcon = Ic.Cloud
