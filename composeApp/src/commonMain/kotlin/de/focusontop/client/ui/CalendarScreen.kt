package de.focusontop.client.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.alpha
import de.focusontop.client.data.Tab
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.focusontop.client.data.ApiError
import de.focusontop.client.data.AppState
import de.focusontop.client.data.Appointment
import de.focusontop.client.data.Screen
import de.focusontop.client.data.Slot
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.i18n.tr
import de.focusontop.client.platform.openUrl
import de.focusontop.client.platform.todayIso
import kotlinx.coroutines.launch

@Composable
fun shortWeekdays(): List<String> = listOf(
    tr("Mo", "Mon", "ن"), tr("Di", "Tue", "ث"), tr("Mi", "Wed", "ر"), tr("Do", "Thu", "خ"),
    tr("Fr", "Fri", "ج"), tr("Sa", "Sat", "س"), tr("So", "Sun", "ح"),
)

private enum class CalView { DAY, WEEK, MONTH, YEAR }

/** Figma 15–18 · Kalender (Tag / Woche / Monat / Jahr). */
@Composable
fun CalendarTab(state: AppState) {
    val s = LocalStrings.current
    val today = todayIso()
    var view by remember { mutableStateOf(CalView.DAY) }
    var focus by remember { mutableStateOf(today) }
    val all = state.home?.appointments.orEmpty()
    val y = focus.take(4).toInt(); val m = focus.substring(5, 7).toInt()

    fun shift(dir: Int) {
        focus = when (view) {
            CalView.DAY -> addDaysIso(focus, dir)
            CalView.WEEK -> addDaysIso(focus, 7 * dir)
            CalView.MONTH -> { val nm = m + dir; val ny = y + if (nm < 1) -1 else if (nm > 12) 1 else 0; iso(ny, ((nm - 1 + 12) % 12) + 1, 1) }
            CalView.YEAR -> iso(y + dir, m, 1)
        }
    }
    val listed: List<Appointment> = when (view) {
        CalView.DAY -> all.filter { it.date == focus }
        CalView.WEEK -> { val ws = weekStart(focus); val we = addDaysIso(ws, 6); all.filter { it.date in ws..we } }
        CalView.MONTH -> all.filter { it.date.startsWith(focus.take(7)) }
        CalView.YEAR -> all.filter { it.date >= today }.take(20)
    }.sortedWith(compareBy({ it.date }, { it.time ?: "" }))

    Column(Modifier.fillMaxSize().background(C.Bg)) {
        TopBar(tr("Kalender", "Calendar", "التقويم"), onBack = { state.tab = Tab.PROFILE },
            actions = { HeaderIcon(Ic.CalendarPlus, tr("Termin anfragen", "Request appointment", "طلب موعد")) { state.go(Screen.AppointmentRequest) } })
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(if (view == CalView.YEAR) "$y" else "${s.months[m - 1]} $y", color = C.Ink, style = T.Title1, maxLines = 1, modifier = Modifier.weight(1f))
                    Text(tr("Heute", "Today", "اليوم"), color = C.Ink, style = T.Caption, maxLines = 1,
                        modifier = Modifier.height(30.dp).clip(R.Pill).background(C.GlassStrong).border(1.dp, C.StrokeGlass, R.Pill).clickable { focus = today }.padding(horizontal = 10.dp, vertical = 7.dp))
                    Icon(Ic.ChevronLeft, null, tint = C.Muted, modifier = Modifier.size(22.dp).clickable { shift(-1) })
                    Icon(Ic.ChevronRight, null, tint = C.Muted, modifier = Modifier.size(22.dp).clickable { shift(1) })
                }
            }
            item {
                // "View tabs": 44 high, glass-strong + hairline pill, padding 4; selected = glass-strong pill, gold-label Caption semibold
                Row(Modifier.fillMaxWidth().height(44.dp).clip(R.Pill).background(C.GlassStrong).border(1.dp, C.StrokeGlass, R.Pill).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(CalView.DAY to tr("Tag", "Day", "يوم"), CalView.WEEK to tr("Woche", "Week", "أسبوع"), CalView.MONTH to tr("Monat", "Month", "شهر"), CalView.YEAR to tr("Jahr", "Year", "سنة")).forEach { (v, label) ->
                        val sel = view == v
                        Box(Modifier.weight(1f).fillMaxHeight().clip(R.Pill).then(if (sel) Modifier.background(C.GlassStrong) else Modifier).clickable { view = v }, contentAlignment = Alignment.Center) {
                            Text(label, color = if (sel) C.GoldLabel else C.Ink, style = T.Caption, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            item {
                when (view) {
                    CalView.DAY, CalView.WEEK -> WeekStrip(weekStart(focus), focus) { focus = it }
                    CalView.MONTH -> MonthGrid(y, m, focus) { focus = it }
                    CalView.YEAR -> YearGrid(y, today) { mm -> focus = iso(y, mm, 1); view = CalView.MONTH }
                }
            }
            if (view == CalView.DAY) item { Text(longDay(s, focus), color = C.Muted, style = T.BodyS) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Ic.CircleCheck, null, tint = C.Green, modifier = Modifier.size(12.dp))
                    Text(tr("Bestätigt", "Confirmed", "مؤكد"), color = C.Muted, style = T.Micro, modifier = Modifier.weight(1f))
                    Icon(Ic.Clock, null, tint = C.Ink, modifier = Modifier.size(12.dp))
                    Text(tr("Ausstehend", "Pending", "بانتظار التأكيد"), color = C.Muted, style = T.Micro, modifier = Modifier.weight(1f))
                }
            }
            item {
                Text(when (view) {
                    CalView.DAY -> tr("Termine an diesem Tag", "Appointments this day", "مواعيد هذا اليوم")
                    CalView.WEEK -> tr("Diese Woche", "This week", "هذا الأسبوع")
                    CalView.MONTH -> tr("In diesem Monat", "This month", "هذا الشهر")
                    CalView.YEAR -> tr("Demnächst", "Upcoming", "القادم")
                }, color = C.Ink, style = T.Headline)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (listed.isEmpty()) Text(tr("Keine Termine", "No appointments", "لا توجد مواعيد"), color = C.Muted, style = T.Footnote, modifier = Modifier.padding(vertical = 8.dp))
                    listed.forEach { a -> AppointmentRow(a, view) }
                    Box(Modifier.fillMaxWidth().height(48.dp).clip(R.Pill).background(C.GlassStrong).clickable { state.go(Screen.AppointmentRequest) }, contentAlignment = Alignment.Center) {
                        Text(tr("Termin anfragen", "Request appointment", "طلب موعد"), color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** "Mittwoch, 28. Oktober". */
@Composable
private fun longDay(s: de.focusontop.client.i18n.Strings, d: String): String {
    val names = listOf(tr("Montag", "Monday", "الاثنين"), tr("Dienstag", "Tuesday", "الثلاثاء"), tr("Mittwoch", "Wednesday", "الأربعاء"), tr("Donnerstag", "Thursday", "الخميس"),
        tr("Freitag", "Friday", "الجمعة"), tr("Samstag", "Saturday", "السبت"), tr("Sonntag", "Sunday", "الأحد"))
    return "${names[weekdayIndex(d)]}, ${d.substring(8, 10).toInt()}. ${s.months[d.substring(5, 7).toInt() - 1]}"
}

/** Two-line date cell: weekday 11 secondary / number 15 (gold when selected), line height 190 %. */
@Composable
private fun DateCell(wd: String, num: String, selected: Boolean, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.height(52.dp).clip(R.Pill)
            .then(if (selected) Modifier.background(C.Surface2).border(1.dp, C.Gold, R.Pill) else Modifier)
            .alpha(if (enabled) 1f else 0.4f).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 11.sp, color = C.Muted)) { append(wd + "\n") }
            withStyle(SpanStyle(fontSize = 15.sp, color = if (selected) C.Gold else C.Ink)) { append(num) }
        }, style = T.Body.copy(lineHeight = 21.sp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun WeekStrip(start: String, selected: String, onPick: (String) -> Unit) {
    val wd = shortWeekdays()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (k in 0 until 7) {
            val d = addDaysIso(start, k)
            DateCell(wd[k], d.substring(8, 10).toInt().toString(), d == selected, Modifier.weight(1f)) { onPick(d) }
        }
    }
}

/** Month grid: weekday Micro headings, Footnote dates in 18-high rows (gap 6), selected = gold pill 36 high. */
@Composable
private fun MonthGrid(y: Int, m: Int, selected: String, onPick: (String) -> Unit) {
    val wd = shortWeekdays()
    val lead = weekdayIndex(iso(y, m, 1))
    val days = daysInMonth(y, m)
    val cells = 7 * ((lead + days + 6) / 7)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            wd.forEach { Text(it, color = C.Muted, style = T.Micro, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        for (row in 0 until cells / 7) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                for (col in 0 until 7) {
                    val d = row * 7 + col - lead + 1
                    if (d in 1..days) {
                        val date = iso(y, m, d)
                        val sel = date == selected
                        Box(
                            Modifier.weight(1f).height(if (sel) 36.dp else 18.dp).clip(R.Pill).then(if (sel) Modifier.background(C.Gold) else Modifier).clickable { onPick(date) },
                            contentAlignment = Alignment.Center,
                        ) { Text(d.toString(), color = if (sel) C.OnAccent else C.Ink, style = T.Footnote, textAlign = TextAlign.Center) }
                    } else Spacer(Modifier.weight(1f).height(18.dp))
                }
            }
        }
    }
}

/** Year overview: 4 quarters (gap 20) of 3 mini months (gap 12) — Micro month name, 16 pt day cells. */
@Composable
private fun YearGrid(y: Int, today: String, onMonth: (Int) -> Unit) {
    val s = LocalStrings.current
    val letters = listOf(tr("M", "M", "ن"), tr("D", "T", "ث"), tr("M", "W", "ر"), tr("D", "T", "خ"), tr("F", "F", "ج"), tr("S", "S", "س"), tr("S", "S", "ح"))
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        for (q in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (c in 0 until 3) {
                    val mm = q * 3 + c + 1
                    Column(Modifier.weight(1f).clickable { onMonth(mm) }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(s.months[mm - 1], color = if (today.take(7) == iso(y, mm, 1).take(7)) C.GoldLabel else C.Ink, style = T.Micro, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        val lead = weekdayIndex(iso(y, mm, 1)); val days = daysInMonth(y, mm); val cells = 7 * ((lead + days + 6) / 7)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(Modifier.fillMaxWidth()) { letters.forEach { Text(it, color = C.Muted, style = T.Micro, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).height(16.dp)) } }
                            for (row in 0 until cells / 7) Row(Modifier.fillMaxWidth()) {
                                for (col in 0 until 7) {
                                    val d = row * 7 + col - lead + 1
                                    val date = iso(y, mm, d.coerceAtLeast(1))
                                    Box(Modifier.weight(1f).height(16.dp), contentAlignment = Alignment.Center) {
                                        if (d in 1..days) Text(d.toString(), color = if (date == today) C.GoldLabel else C.Ink, style = T.Micro,
                                            fontWeight = if (date == today) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center)
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

/** Figma "Event": bg/secondary radius 20, padding 12/14, gap 12 — time column 60 · 3 pt status bar · title / place / status chip. */
@Composable
private fun AppointmentRow(a: Appointment, view: CalView) {
    val s = LocalStrings.current
    val wd = shortWeekdays()
    val requested = a.status == "requested"
    val accent = if (requested) C.Gold else C.Green
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(R.CardShape).background(C.Surface)
            .clickable(enabled = !a.videoLink.isNullOrBlank()) { a.videoLink?.let(::openUrl) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.width(60.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val time = if (a.allDay || a.time == null) s.allDay else a.time
            when (view) {
                CalView.DAY -> {
                    Text(time, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    a.endTime?.let { Text(it, color = C.Muted, style = T.Caption, maxLines = 1) }
                }
                CalView.WEEK -> {
                    Text("${wd[weekdayIndex(a.date)]} ${a.date.substring(8, 10).toInt()}.", color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(time, color = C.Muted, style = T.Caption, maxLines = 1)
                }
                else -> {
                    Text("${a.date.substring(8, 10).toInt()}. ${s.months[a.date.substring(5, 7).toInt() - 1].take(3)}.", color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(time, color = C.Muted, style = T.Caption, maxLines = 1)
                }
            }
        }
        Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(accent))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(a.title?.ifBlank { null } ?: "—", color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val where = a.location?.takeIf { it.isNotBlank() } ?: if (!a.videoLink.isNullOrBlank()) tr("Online · Video-Call", "Online · video call", "عبر الإنترنت · مكالمة فيديو") else null
            where?.let { w ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.clickable(enabled = !a.location.isNullOrBlank()) { openUrl("https://maps.google.com/?q=" + (a.location ?: "").encodeForUrl()) }) {
                    Icon(if (a.location.isNullOrBlank()) Ic.Video else Ic.Pin, null, tint = C.Muted, modifier = Modifier.size(13.dp))
                    Text(w, color = C.Muted, style = T.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(
                Modifier.height(22.dp).clip(R.Pill).background(if (requested) C.GoldTint else C.Green.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(if (requested) Ic.Clock else Ic.CheckBold, null, tint = if (requested) C.GoldLabel else C.Green, modifier = Modifier.size(11.dp))
                Text(if (requested) tr("Ausstehend", "Pending", "بانتظار التأكيد") else tr("Bestätigt", "Confirmed", "مؤكد"),
                    color = if (requested) C.GoldLabel else C.Green, style = T.Caption, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/* ───────────── 19 · Terminanfrage ───────────── */

private data class Kind(val key: String, val minutes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun AppointmentRequestScreen(state: AppState) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val kinds = listOf(
        Kind("meeting", 60, Ic.Video) to tr("Review-Meeting", "Review meeting", "اجتماع مراجعة"),
        Kind("shoot", 120, Ic.Camera) to tr("Foto-/Videodreh", "Photo / video shoot", "جلسة تصوير"),
        Kind("call", 30, Ic.Phone) to tr("Kurzer Call", "Quick call", "مكالمة سريعة"),
        Kind("consult", 60, Ic.Chat) to tr("Beratung", "Consultation", "استشارة"),
    )
    var kind by remember { mutableStateOf(kinds.first().first) }
    val today = todayIso()
    val days = remember(today) {
        val out = mutableListOf<String>(); var d = nextDayIso(today)
        while (out.size < 12) { if (weekdayIndex(d) != 6) out += d; d = nextDayIso(d) }
        out
    }
    var day by remember { mutableStateOf(days.firstOrNull { weekdayIndex(it) < 5 } ?: days.first()) }
    var slots by remember { mutableStateOf<List<Slot>?>(null) }
    var time by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val sent = tr("Anfrage gesendet – das Team meldet sich", "Request sent – the team will confirm", "أُرسل الطلب وسيؤكده الفريق")

    LaunchedEffect(day, kind) {
        slots = null; time = null
        slots = runCatching { state.api.availability(day, kind.minutes).slots.filter { it.time.endsWith(":00") } }.getOrElse { emptyList() }
    }
    val wd = shortWeekdays()
    val firstM = days.first().substring(5, 7).toInt(); val lastM = days.last().substring(5, 7).toInt()
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopBar(tr("Termin anfragen", "Request appointment", "طلب موعد"), onBack = { state.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(tr("Art des Termins", "Type of appointment", "نوع الموعد"), color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                kinds.forEach { (k, label) ->
                    val on = kind == k
                    Row(
                        Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.dp, C.StrokeGlass, R.CardShape).clickable { kind = k }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(k.icon, null, tint = if (on) C.Ink else C.Muted, modifier = Modifier.size(22.dp))
                        Text(label, color = if (on) C.GoldLabel else C.Ink, style = T.Footnote, modifier = Modifier.weight(1f))
                        Icon(if (on) Ic.CircleCheck else Ic.Circle, null, tint = if (on) C.Ink else C.Muted, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(tr("Tag", "Day", "اليوم"), color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold)
                Text(if (firstM == lastM) "${s.months[firstM - 1]} ${days.first().take(4)}" else "${s.months[firstM - 1]} – ${s.months[lastM - 1]} ${days.last().take(4)}", color = C.Muted, style = T.Caption)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                days.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { d ->
                            val sat = weekdayIndex(d) == 5
                            DateCell(wd[weekdayIndex(d)], d.substring(8, 10), d == day, Modifier.weight(1f), enabled = !sat) { day = d }
                        }
                        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Text(tr("Uhrzeit · Berliner Zeit", "Time · Berlin time", "الوقت · توقيت برلين"), color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold)
            val list = slots
            if (list == null) Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = C.Gold) }
            else if (list.none { it.free }) Row(
                Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.dp, C.StrokeGlass, R.CardShape).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Ic.Clock, null, tint = C.Muted, modifier = Modifier.size(22.dp))
                Text(tr("An diesem Tag ist leider nichts frei.", "Nothing free on this day.", "لا توجد أوقات متاحة في هذا اليوم."), color = C.Muted, style = T.Footnote)
            }
            else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                list.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { sl -> ChoiceChip(sl.time, time == sl.time, { time = sl.time }, Modifier.weight(1f), enabled = sl.free, strike = !sl.free, height = 36.dp) }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Hinweis ans Team · optional", "Note to the team · optional", "ملاحظة للفريق · اختياري"), color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold)
                Box(Modifier.fillMaxWidth().height(112.dp).clip(R.FieldShape).background(C.GlassStrong).border(1.dp, C.StrokeGlass, R.FieldShape).padding(16.dp)) {
                    if (note.isEmpty()) Text(tr("z. B. Wir möchten das neue Reel vor der Veröffentlichung prüfen", "e.g. We'd like to review the new reel before it goes live", "مثال: نريد مراجعة الريلز الجديد قبل النشر"), color = C.Muted, style = T.BodyS)
                    BasicTextField(note, { note = it }, textStyle = T.BodyS.copy(color = C.Ink), cursorBrush = SolidColor(C.Gold), modifier = Modifier.fillMaxSize())
                }
            }
            error?.let { Text(it, color = C.Red, style = T.Footnote) }
            PrimaryButton(tr("Anfrage senden", "Send request", "إرسال الطلب"), onClick = {
                val t = time ?: return@PrimaryButton
                busy = true; error = null
                scope.launch {
                    try {
                        state.api.requestAppointment(kind.key, day, t, note.trim())
                        state.showToast(sent); state.refresh(); state.back()
                    } catch (e: ApiError) { error = if (e.status == 0) s.networkError else s.loadError } finally { busy = false }
                }
            }, enabled = time != null, loading = busy)
            Text(
                tr("Das Team bestätigt innerhalb von 2 Arbeitsstunden. Der Termin erscheint im Kalender als „Ausstehend“.",
                    "The team confirms within 2 working hours. Until then it shows in your calendar as “pending”.",
                    "يؤكد الفريق الطلب خلال ساعتي عمل، وسيظهر في التقويم بحالة «بانتظار التأكيد»."),
                color = C.Muted, style = T.Caption,
            )
        }
    }
}
