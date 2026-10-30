package de.focusontop.client.ui

import androidx.compose.foundation.layout.navigationBarsPadding
import de.focusontop.client.data.ClientFile
import de.focusontop.client.platform.rememberFilePicker
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.focusontop.client.data.AppState
import de.focusontop.client.data.Media
import de.focusontop.client.data.Screen
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.i18n.tr

private enum class FileTab { ALL, VIDEO, IMAGE, MINE, DOCS }

/** Tab.FILES in grid mode (from the reels header). */
@Composable
fun FilesTab(state: AppState, onUpload: () -> Unit) = FilesPage(state, onBack = { state.reelsMode = true }, onUpload = onUpload)

/** Figma 12 · Medien · Dateien  /  12b · Eigene Uploads  /  12c · Ladefehler. */
@Composable
fun FilesPage(state: AppState, onBack: (() -> Unit)? = null, onUpload: (() -> Unit)? = null) {
    val s = LocalStrings.current
    val home = state.home
    val picker = rememberFilePicker { state.upload(it) }
    var tab by remember { mutableStateOf(FileTab.ALL) }
    var month by remember { mutableStateOf<String?>(null) }
    var day by remember { mutableStateOf<String?>(null) }
    var monthMenu by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val copied = tr("Link kopiert", "Link copied", "تم نسخ الرابط")

    val all = home?.files.orEmpty()
    val byTab = all.filter {
        when (tab) {
            FileTab.ALL -> !it.mine
            FileTab.VIDEO -> !it.mine && it.type == "Video"
            FileTab.IMAGE -> !it.mine && it.type == "Image"
            FileTab.MINE -> it.mine
            FileTab.DOCS -> !it.mine && (it.type == "Document" || it.type == "Link")
        }
    }
    val list = byTab
        .filter {
            val d = fileDay(it.createdAt)
            when { day != null -> d == day; month != null -> d?.startsWith(month!!) == true; else -> true }
        }
        .filter { query.isBlank() || it.title().contains(query.trim(), ignoreCase = true) || (it.projectName ?: "").contains(query.trim(), ignoreCase = true) }
    val groups = list.groupBy { fileDay(it.createdAt)?.take(7) ?: "" }.toList().sortedByDescending { it.first }

    Column(Modifier.fillMaxSize().background(C.Bg)) {
        TopBar(tr("Dateien", "Files", "الملفات"), onBack = onBack ?: { state.back(); Unit })
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item { FtField("", query, { query = it }, placeholder = tr("Dateien durchsuchen", "Search files", "ابحث في الملفات"), trailing = Ic.Search) }
            item {
                SecondaryButton(tr("Fotos & Videos hochladen", "Upload photos & videos", "رفع صور وفيديو"), onClick = { if (onUpload != null) onUpload() else picker.pickMedia() },
                    modifier = Modifier.fillMaxWidth(), icon = Ic.Upload)
            }
            item { StorageMeter(state) }
            item {
                Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    GridTab(Ic.Layers, tab == FileTab.ALL) { tab = FileTab.ALL; month = null; day = null }
                    GridTab(Ic.Video, tab == FileTab.VIDEO) { tab = FileTab.VIDEO; month = null; day = null }
                    GridTab(Ic.Image, tab == FileTab.IMAGE) { tab = FileTab.IMAGE; month = null; day = null }
                    GridTab(Ic.Upload, tab == FileTab.MINE) { tab = FileTab.MINE; month = null; day = null }
                    GridTab(Ic.FileText, tab == FileTab.DOCS) { tab = FileTab.DOCS; month = null; day = null }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(Modifier.fillMaxWidth().height(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.weight(1f).clickable { monthMenu = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Ic.CalendarDays, null, tint = C.Ink, modifier = Modifier.size(18.dp))
                            Text(day?.let { fmtDate(it) } ?: month?.let { monthLabel(s.months, it) } ?: tr("Alle Daten", "All dates", "كل التواريخ"),
                                color = C.Ink, style = T.Footnote, modifier = Modifier.weight(1f), maxLines = 1)
                            if (day != null || month != null) Icon(Ic.X, null, tint = C.Muted, modifier = Modifier.size(16.dp).clickable { day = null; month = null })
                            else Icon(Ic.ChevronDownSm, null, tint = C.Muted, modifier = Modifier.size(16.dp))
                        }
                        Icon(Ic.Link, tr("Abschnittslink kopieren", "Copy section link", "نسخ رابط القسم"), tint = C.Ink,
                            modifier = Modifier.size(18.dp).clickable { clipboard.setText(AnnotatedString(Media.web("files"))); state.showToast(copied) })
                    }
                    Text(tr("Abschnittslink kopieren", "Copy section link", "نسخ رابط القسم"), color = C.GoldLabel, style = T.Micro,
                        modifier = Modifier.fillMaxWidth().clickable { clipboard.setText(AnnotatedString(Media.web("files"))); state.showToast(copied) })
                }
            }
            if (home == null && state.homeError != null) item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Icon(Ic.CloudAlert, null, tint = C.Muted, modifier = Modifier.size(36.dp))
                    Text(tr("Laden fehlgeschlagen. Bitte erneut versuchen.", "Loading failed. Please try again.", "فشل التحميل. يرجى المحاولة مرة أخرى."), color = C.Ink, style = T.Title3, textAlign = TextAlign.Center)
                    SecondaryButton(tr("Erneut versuchen", "Try again", "إعادة المحاولة"), onClick = { state.refresh() }, modifier = Modifier.fillMaxWidth())
                }
            } else if (tab == FileTab.MINE) item {
                UploadsCard(state, list)
            } else if (list.isEmpty()) item {
                EmptyState(s.noFiles, icon = Ic.Layers)
            } else groups.forEach { (ym, files) ->
                item(key = "h-$ym") {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text(
                            (if (ym.isNotEmpty()) monthLabel(s.months, ym) + " · " else "") + tr("${files.size} Dateien", "${files.size} files", "${files.size} ملفات"),
                            color = C.Muted, style = T.Footnote, fontWeight = FontWeight.SemiBold,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            files.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    row.forEach { f -> MediaTile(f, Modifier.weight(1f).aspectRatio(116f / 120f)) { state.go(Screen.Viewer(list.map { it.id }, list.indexOf(f))) } }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (monthMenu) {
        FileDatePicker(
            days = byTab.mapNotNull { fileDay(it.createdAt) }.groupingBy { it }.eachCount(),
            selectedDay = day,
            onDay = { d -> day = d; month = null; monthMenu = false },
            onMonth = { m -> month = m; day = null; monthMenu = false },
            onAll = { day = null; month = null; monthMenu = false },
            onDismiss = { monthMenu = false },
        )
    }
}

/** 12b "Uploads": bg/secondary radius 20; rows 72 (thumb 44 · name / status · progress), separators. */
@Composable
private fun UploadsCard(state: AppState, mine: List<ClientFile>) {
    val jobs = state.uploads
    if (jobs.isEmpty() && mine.isEmpty()) {
        EmptyState(tr("Noch nichts hochgeladen", "Nothing uploaded yet", "لم ترفع شيئًا بعد"), tr("Über + kannst du Dateien oder ganze Ordner ans Team schicken.", "Use + to send files or whole folders to the team.", "يمكنك عبر زر + إرسال ملفات أو مجلدات كاملة إلى الفريق."), icon = Ic.Upload)
        return
    }
    Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface)) {
        var first = true
        jobs.forEach { j ->
            if (!first) Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator)); first = false
            UploadRow(null, j.name,
                when { j.failed -> tr("Fehlgeschlagen", "Failed", "فشل"); j.done -> tr("Hochgeladen", "Uploaded", "تم الرفع")
                    else -> tr("Wird hochgeladen · ${j.sent / 1_000_000} von ${j.size / 1_000_000} MB", "Uploading · ${j.sent / 1_000_000} of ${j.size / 1_000_000} MB", "جارٍ الرفع · ${j.sent / 1_000_000} من ${j.size / 1_000_000} م.ب") },
                if (!j.done && !j.failed && j.size > 0) j.sent.toFloat() / j.size else null, onClick = {})
        }
        mine.forEach { f ->
            if (!first) Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator)); first = false
            UploadRow(f, f.fileName ?: f.title(),
                listOfNotNull(if (f.status == "approved" || f.status == "delivered") tr("Vom Team angenommen", "Accepted by the team", "قبله الفريق") else tr("Hochgeladen", "Uploaded", "تم الرفع"),
                    f.createdAt?.let { fmtDate(it.take(10)) }).joinToString(" · "),
                null, onClick = { state.go(Screen.FileDetail(f)) })
        }
    }
}

@Composable
private fun UploadRow(f: ClientFile?, name: String, status: String, progress: Float?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(R.ThumbShape).background(C.Surface2), contentAlignment = Alignment.Center) {
            if (f != null) FileCover(f, Modifier.fillMaxSize(), 200) else Icon(Ic.Upload, null, tint = C.Muted, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(status, color = C.Muted, style = T.Caption, maxLines = 1)
            if (progress != null) Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(C.Surface2)) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).clip(RoundedCornerShape(2.dp)).background(C.Gold))
            }
        }
        Icon(Ic.ChevronRight, null, tint = C.Muted, modifier = Modifier.size(18.dp))
    }
}

fun monthLabel(months: List<String>, ym: String): String {
    val m = ym.substring(5, 7).toIntOrNull() ?: return ym
    return "${months[m - 1]} ${ym.take(4)}"
}

/** Figma "Card" 54: hard-drive 22 + Footnote (storage / plan line). */
@Composable
fun StorageMeter(state: AppState) {
    val u = state.home?.usage
    val used = u?.storageUsedBytes ?: 0.0
    val limit = u?.storageLimitGb
    val line = when {
        state.home?.subscription == null -> tr("Kein aktives Paket", "No active plan", "لا توجد باقة نشطة")
        limit != null -> tr("${gb(used)} von ${num(limit)} GB belegt", "${gb(used)} of ${num(limit)} GB used", "${gb(used)} من ${num(limit)} غ.ب مستخدمة") +
            (u.daysLeft?.let { " · " + tr("Verlängerung in $it Tagen", "renews in $it days", "تتجدد بعد ${arDays(it)}") } ?: "")
        else -> "${gb(used)} GB · " + planName(state, state.home)
    }
    Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.dp, C.StrokeGlass, R.CardShape).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Ic.HardDrive, null, tint = C.Muted, modifier = Modifier.size(22.dp))
            Text(line, color = C.Muted, style = T.Footnote, modifier = Modifier.weight(1f), maxLines = 2)
        }
        if (limit != null && limit > 0) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(R.Pill).background(C.Surface2)) {
                Box(Modifier.fillMaxWidth((used / (limit * 1e9)).toFloat().coerceIn(0.01f, 1f)).height(4.dp).clip(R.Pill).background(C.Gold))
            }
        }
    }
}


/** The day a file arrived, in the phone's time zone ("YYYY-MM-DD"). */
fun fileDay(createdAt: String?): String? = epochOf(createdAt)?.let { de.focusontop.client.platform.localDateTime(it).take(10) }

/** Calendar to pick a day (or a whole month) — days with files carry a gold dot and their count. */
@Composable
private fun FileDatePicker(
    days: Map<String, Int>,
    selectedDay: String?,
    onDay: (String) -> Unit,
    onMonth: (String) -> Unit,
    onAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    val today = de.focusontop.client.platform.todayIso()
    val start = selectedDay?.take(7) ?: days.keys.maxOrNull()?.take(7) ?: today.take(7)
    var ym by remember { mutableStateOf(start) }
    val y = ym.take(4).toInt(); val m = ym.substring(5, 7).toInt()
    val lead = weekdayIndex(iso(y, m, 1))
    val count = daysInMonth(y, m)
    val inMonth = days.filterKeys { it.startsWith(ym) }.values.sum()
    // Figma D · "Bottom sheet": title + ×, month switcher, month grid, empty hint, actions
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(C.Scrim).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxWidth().clip(R.SheetShape).background(C.Surface).border(1.dp, C.Separator, R.SheetShape)
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 12.dp).size(36.dp, 3.dp).clip(RoundedCornerShape(2.dp)).background(C.Muted))
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(tr("Datum wählen", "Choose a date", "اختر التاريخ"), color = C.Ink, style = T.Title3, modifier = Modifier.weight(1f))
                        Icon(Ic.X, null, tint = C.Muted, modifier = Modifier.size(22.dp).clickable(onClick = onDismiss))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${s.months[m - 1]} $y", color = C.Ink, style = T.Headline, modifier = Modifier.weight(1f))
                        Icon(Ic.ChevronLeft, null, tint = C.Muted, modifier = Modifier.size(22.dp).clickable { ym = if (m == 1) "${y - 1}-12" else "$y-${(m - 1).toString().padStart(2, '0')}" })
                        Icon(Ic.ChevronRight, null, tint = C.Muted, modifier = Modifier.size(22.dp).clickable { ym = if (m == 12) "${y + 1}-01" else "$y-${(m + 1).toString().padStart(2, '0')}" })
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            shortWeekdays().forEach { Text(it, color = C.Muted, style = T.Micro, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
                        }
                        val cells = 7 * ((lead + count + 6) / 7)
                        for (row in 0 until cells / 7) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                for (col in 0 until 7) {
                                    val d = row * 7 + col - lead + 1
                                    if (d in 1..count) {
                                        val date = iso(y, m, d); val n = days[date] ?: 0; val sel = date == selectedDay
                                        Box(Modifier.weight(1f).height(if (sel) 36.dp else 18.dp).clip(R.Pill).then(if (sel) Modifier.background(C.Gold) else Modifier).clickable(enabled = n > 0) { onDay(date) },
                                            contentAlignment = Alignment.Center) {
                                            Text(d.toString(), color = when { sel -> C.OnAccent; n > 0 -> C.Ink; else -> C.Muted }, style = T.Footnote,
                                                fontWeight = if (n > 0) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center)
                                        }
                                    } else Spacer(Modifier.weight(1f).height(18.dp))
                                }
                            }
                        }
                    }
                    if (inMonth == 0) Text(tr("Hier ist noch nichts", "Nothing here yet", "لا يوجد شيء هنا بعد"), color = C.Muted, style = T.Caption, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SecondaryButton(tr("Alle Daten", "All dates", "كل التواريخ"), onClick = onAll, modifier = Modifier.weight(1f))
                        PrimaryButton(tr("Ganzer Monat ($inMonth)", "Whole month ($inMonth)", "الشهر كامل ($inMonth)"), onClick = { onMonth(ym) }, modifier = Modifier.weight(1f), enabled = inMonth > 0)
                    }
                }
            }
        }
    }

}
