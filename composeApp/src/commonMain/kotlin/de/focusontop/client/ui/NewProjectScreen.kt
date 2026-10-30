@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.focusontop.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.focusontop.client.data.AppState
import de.focusontop.client.data.Screen
import de.focusontop.client.i18n.tr
import de.focusontop.client.platform.PickedFile
import de.focusontop.client.platform.rememberFilePicker
import kotlinx.coroutines.launch

/**
 * Figma 31 · Neues Projekt (Leistung → Details → Referenzen → Gesendet).
 * The request reaches the team as one structured message in the team chat (plus the reference files).
 */
@Composable
fun NewProjectScreen(state: AppState) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0) }
    val services = listOf(
        Triple(Ic.Video, tr("Video & Reels", "Video & reels", "فيديو وريلز"), tr("Werbefilme, Reels, Clips", "Ads, reels, clips", "إعلانات، ريلز، مقاطع")),
        Triple(Ic.Camera, tr("Fotografie", "Photography", "تصوير فوتوغرافي"), tr("Produkt, Portrait, Event", "Product, portrait, event", "منتجات، بورتريه، فعاليات")),
        Triple(Ic.Users, tr("Social Media", "Social media", "سوشيال ميديا"), tr("Planung, Content, Betreuung", "Planning, content, management", "تخطيط، محتوى، إدارة")),
        Triple(Ic.Calendar175, tr("Event", "Event", "فعالية"), tr("Hochzeit, Firmenevent", "Wedding, corporate event", "أعراس، فعاليات شركات")),
        Triple(Ic.Clapper175, tr("Imagefilm", "Brand film", "فيلم تعريفي"), tr("Unternehmen, Marke", "Company, brand", "شركة، علامة تجارية")),
        Triple(Ic.Music, tr("Musikproduktion", "Music production", "إنتاج موسيقي"), tr("Aufnahme, Mix, Sound", "Recording, mix, sound", "تسجيل، مكساج، صوت")),
    )
    val goals = listOf(tr("Bekanntheit", "Awareness", "الانتشار"), tr("Verkauf", "Sales", "المبيعات"), tr("Recruiting", "Recruiting", "التوظيف"), tr("Event-Doku", "Event coverage", "توثيق فعالية"))
    val budgets = listOf(tr("bis 1.000 €", "up to €1,000", "حتى 1.000 €"), "1.000–3.000 €", "3.000–5.000 €", tr("über 5.000 €", "over €5,000", "أكثر من 5.000 €"))
    val picked = remember { mutableStateListOf<Int>() }
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf<String?>(null) }
    var budget by remember { mutableStateOf<String?>(null) }
    var date by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val refs = remember { mutableStateListOf<PickedFile>() }
    var busy by remember { mutableStateOf(false) }
    val failed = tr("Senden fehlgeschlagen", "Could not send", "تعذّر الإرسال")
    val picker = rememberFilePicker { files -> refs.addAll(files.take(6 - refs.size)) }
    val lServices = tr("Leistungen", "Services", "الخدمات"); val lGoal = tr("Ziel", "Goal", "الهدف"); val lBudget = tr("Budget", "Budget", "الميزانية")
    val lDate = tr("Termin", "Date", "الموعد"); val lPlace = tr("Drehort", "Location", "مكان التصوير"); val lRef = tr("Referenz", "Reference", "مرجع")
    val lHead = tr("Neue Projektanfrage", "New project request", "طلب مشروع جديد"); val lNotes = tr("Hinweise", "Notes", "ملاحظات"); val lName = tr("Projektname", "Project name", "اسم المشروع")

    fun send() {
        busy = true
        val text = buildString {
            appendLine("📋 $lHead" + (if (name.isNotBlank()) " – ${name.trim()}" else ""))
            appendLine("$lServices: " + picked.sorted().joinToString(", ") { services[it].second })
            goal?.let { appendLine("$lGoal: $it") }
            budget?.let { appendLine("$lBudget: $it") }
            if (date.isNotBlank()) appendLine("$lDate: ${date.trim()}")
            if (place.isNotBlank()) appendLine("$lPlace: ${place.trim()}")
            if (link.isNotBlank()) appendLine("$lRef: ${link.trim()}")
            if (notes.isNotBlank()) appendLine("$lNotes: ${notes.trim()}")
        }.trim()
        scope.launch {
            try {
                state.api.sendChat(text)
                refs.forEach { f -> val blob = state.uploadBlob(f); state.api.sendChat("", blob.url, f.name, f.mimeType) }
                step = 3
            } catch (_: Exception) { state.showToast(failed) } finally { busy = false }
        }
    }

    if (step == 3) {
        Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
            Box(Modifier.size(88.dp).clip(R.Pill).background(C.GoldTint), contentAlignment = Alignment.Center) {
                Box(Modifier.size(60.dp).clip(R.Pill).background(C.Gold), contentAlignment = Alignment.Center) { Icon(Ic.CheckBold, null, tint = C.OnAccent, modifier = Modifier.size(30.dp)) }
            }
            Text(tr("Anfrage gesendet", "Request sent", "تم إرسال الطلب"), color = C.Ink, style = T.Title2, textAlign = TextAlign.Center)
            Text(tr("Danke! Wir melden uns innerhalb von 24 Stunden mit einem Angebot. Du findest die Anfrage im Team-Chat.", "Thanks! We'll get back to you with an offer within 24 hours. You'll find the request in the team chat.", "شكراً! سنرد عليك بعرض خلال 24 ساعة. تجد الطلب في محادثة الفريق."),
                color = C.Muted, style = T.BodyS, textAlign = TextAlign.Center)
            Box(Modifier.fillMaxWidth().height(50.dp).clip(R.Pill).background(C.Gold).clickable { state.back(); state.go(Screen.Chat()) }, contentAlignment = Alignment.Center) {
                Text(tr("Zum Chat", "Open chat", "فتح المحادثة"), color = C.OnAccent, style = T.Body, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.fillMaxWidth().height(50.dp).clip(R.Pill).background(C.GlassStrong).clickable { state.back() }, contentAlignment = Alignment.Center) {
                Text(tr("Fertig", "Done", "تم"), color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        // "Wizard header": close/back 24 · Headline title · step counter
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (step == 0) Ic.XSm else Ic.Back, null, tint = C.Ink, modifier = Modifier.size(24.dp).clickable { if (step == 0) state.back() else step-- })
            Text(tr("Neues Projekt", "New project", "مشروع جديد"), color = C.Ink, style = T.Headline, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text("${step + 1} / 3", color = C.Muted, style = T.Footnote, fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i -> Box(Modifier.weight(1f).height(4.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)).background(if (i <= step) C.Gold else C.Surface2)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            when (step) {
                0 -> {
                    Text(tr("Was dürfen wir für dich produzieren?", "What can we produce for you?", "ماذا يمكننا أن ننتج لك؟"), color = C.Ink, style = T.Title2)
                    Text(tr("Wähle eine oder mehrere Leistungen.", "Choose one or more services.", "اختر خدمة أو أكثر."), color = C.Muted, style = T.BodyS)
                    services.chunked(2).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEachIndexed { c, (icon, title, sub) ->
                                val idx = r * 2 + c; val on = idx in picked
                                ServiceTile(icon, title, sub, on, Modifier.weight(1f)) { if (on) picked.remove(idx) else picked.add(idx) }
                            }
                        }
                    }
                    WizardButton(tr("Weiter", "Next", "التالي"), picked.isNotEmpty()) { step = 1 }
                }
                1 -> {
                    Text(tr("Erzähl uns mehr", "Tell us more", "أخبرنا المزيد"), color = C.Ink, style = T.Title2)
                    Text(tr("Je genauer, desto schneller bekommst du ein passendes Angebot.", "The more precise, the faster you get a matching offer.", "كلما كانت التفاصيل أدق، وصلك العرض المناسب أسرع."), color = C.Muted, style = T.BodyS)
                    WizardInput(lName, name, tr("z. B. Herbstkampagne 2026", "e.g. Autumn campaign 2026", "مثلاً حملة الخريف 2026")) { name = it }
                    WizardChips(lGoal, goals, goal) { goal = it }
                    WizardChips(lBudget, budgets, budget) { budget = it }
                    WizardInput(tr("Wunschtermin", "Preferred date", "الموعد المفضل"), date, tr("z. B. 15. November 2026", "e.g. 15 November 2026", "مثلاً 15 نوفمبر 2026")) { date = it }
                    WizardInput(lPlace, place, tr("Studio Hagen oder bei dir vor Ort", "Studio Hagen or at your place", "استوديو هاغن أو في موقعك")) { place = it }
                    WizardButton(tr("Weiter", "Next", "التالي"), true) { step = 2 }
                }
                else -> {
                    Text(tr("Referenzen und Zusammenfassung", "References and summary", "المراجع والملخص"), color = C.Ink, style = T.Title2)
                    Text(tr("Zeig uns, was dir gefällt – Bilder, Videos oder Links.", "Show us what you like – images, videos or links.", "أرنا ما يعجبك — صور أو فيديوهات أو روابط."), color = C.Muted, style = T.BodyS)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        refs.take(2).forEach { f ->
                            Box(Modifier.weight(1f).height(96.dp).clip(R.ThumbShape).background(C.Surface2).clickable { refs.remove(f) }.padding(8.dp), contentAlignment = Alignment.BottomStart) {
                                Text(f.name, color = C.Ink, style = T.Caption, maxLines = 2)
                            }
                        }
                        Column(Modifier.weight(1f).height(96.dp).clip(R.ThumbShape).border(1.5.dp, C.Faint, R.ThumbShape).clickable { picker.pickMedia() },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                            Icon(Ic.PlusBold, null, tint = C.GoldLabel, modifier = Modifier.size(20.dp))
                            Text(if (refs.size > 2) "+${refs.size - 2}" else tr("Hinzufügen", "Add", "إضافة"), color = C.GoldLabel, style = T.Caption, fontWeight = FontWeight.SemiBold)
                        }
                        repeat((2 - refs.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                    }
                    Row(Modifier.fillMaxWidth().height(52.dp).clip(R.FieldShape).background(C.GlassStrong).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) {
                            if (link.isEmpty()) Text(tr("Instagram- oder YouTube-Link", "Instagram or YouTube link", "رابط إنستغرام أو يوتيوب"), color = C.Muted, style = T.Body)
                            BasicTextField(link, { link = it }, singleLine = true, textStyle = T.Body.copy(color = C.Ink), cursorBrush = SolidColor(C.Gold), modifier = Modifier.fillMaxWidth())
                        }
                        Icon(Ic.Link, null, tint = C.Muted, modifier = Modifier.size(18.dp))
                    }
                    Box(Modifier.fillMaxWidth().height(84.dp).clip(R.FieldShape).background(C.GlassStrong).padding(horizontal = 14.dp, vertical = 12.dp)) {
                        if (notes.isEmpty()) Text(tr("z. B. Warme Farben, ruhige Kamerafahrten, Fokus auf das Produkt.", "e.g. Warm colours, calm camera moves, focus on the product.", "مثلاً ألوان دافئة، حركة كاميرا هادئة، تركيز على المنتج."), color = C.Muted, style = T.BodyS)
                        BasicTextField(notes, { notes = it }, textStyle = T.BodyS.copy(color = C.Ink), cursorBrush = SolidColor(C.Gold), modifier = Modifier.fillMaxSize())
                    }
                    Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface)) {
                        listOf(lServices to picked.sorted().joinToString(", ") { services[it].second }, lGoal to (goal ?: "—"), lBudget to (budget ?: "—"), lDate to date.ifBlank { "—" })
                            .forEachIndexed { k, (l, v) ->
                                if (k > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
                                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(l, color = C.Muted, style = T.Footnote, modifier = Modifier.weight(1f))
                                    Text(v, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                }
                            }
                    }
                    if (busy) Box(Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = C.Gold) }
                    else WizardButton(tr("Anfrage senden", "Send request", "إرسال الطلب"), true) { send() }
                }
            }
        }
    }
}

/** "Service" tile: 127 high, radius 20, padding 14 — selected = gold tint + gold 1.5 stroke, gold icon tile + check. */
@Composable
private fun ServiceTile(icon: ImageVector, title: String, sub: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(127.dp).clip(R.CardShape).background(if (on) C.GoldTint else C.Surface).border(if (on) 1.5.dp else 1.dp, if (on) C.Gold else C.Separator, R.CardShape)
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(40.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(if (on) C.Gold else C.Surface2), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (on) C.OnAccent else C.GoldLabel, modifier = Modifier.size(20.dp))
            }
            if (on) Box(Modifier.size(22.dp).clip(R.Pill).background(C.Gold), contentAlignment = Alignment.Center) { Icon(Ic.CheckBold, null, tint = C.OnAccent, modifier = Modifier.size(12.dp)) }
        }
        Text(title, color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(sub, color = C.Muted, style = T.Caption, maxLines = 2)
    }
}

@Composable
private fun WizardInput(label: String, value: String, hint: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(label, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth().height(52.dp).clip(R.FieldShape).background(C.GlassStrong).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(hint, color = C.Muted, style = T.Body)
            BasicTextField(value, onChange, singleLine = true, textStyle = T.Body.copy(color = C.Ink), cursorBrush = SolidColor(C.Gold), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun WizardChips(label: String, options: List<String>, selected: String?, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(label, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { o ->
                val on = o == selected
                Text(o, color = if (on) C.GoldLabel else C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    modifier = Modifier.height(36.dp).clip(R.Pill).background(if (on) C.GoldTint else C.GlassStrong).then(if (on) Modifier.border(1.dp, C.Gold, R.Pill) else Modifier)
                        .clickable { onPick(o) }.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun WizardButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(50.dp).clip(R.Pill).background(if (enabled) C.Gold else C.Surface2).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) C.OnAccent else C.Faint, style = T.Body, fontWeight = FontWeight.SemiBold)
    }
}
