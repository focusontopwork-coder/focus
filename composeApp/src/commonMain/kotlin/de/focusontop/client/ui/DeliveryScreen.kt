@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.focusontop.client.ui

import de.focusontop.client.i18n.tr
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.focusontop.client.data.ApiError
import de.focusontop.client.data.AppState
import de.focusontop.client.data.ClientFile
import de.focusontop.client.data.Media
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.platform.openUrl
import de.focusontop.client.platform.shareText
import kotlinx.coroutines.launch

/* ───────────── 13f · Freigabe · Lieferung  /  13g · Freigabe · Bewertung ───────────── */

private fun fileSizeLabel(f: ClientFile): String? {
    val parts = listOfNotNull(
        f.mimeType?.substringAfterLast('/')?.uppercase()?.takeIf { it.isNotBlank() && it.length <= 5 },
        f.size,
    )
    return parts.joinToString(" · ").ifBlank { null }
}

@Composable
fun DeliveryScreen(state: AppState, projectName: String) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val home = state.home
    val files = home?.files.orEmpty().filter { it.projectName == projectName && !it.mine && (it.status == "approved" || it.status == "delivered" || it.status == "published") }
    val approvedAt = files.maxByOrNull { it.createdAt ?: "" }?.createdAt
    var rate by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(55.dp).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Ic.Back, s.back, tint = C.Ink, modifier = Modifier.size(24.dp).clickable { state.back() })
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(tr("Lieferung", "Delivery", "التسليم"), color = C.Ink, style = T.Title3, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(projectName, color = C.Muted, style = T.Caption, maxLines = 1)
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Green.copy(alpha = 0.12f)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(C.Green), contentAlignment = Alignment.Center) {
                        Icon(Ic.CheckBold, null, tint = C.Bg, modifier = Modifier.size(12.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            approvedAt?.let { tr("Freigegeben am ${fmtDate(it)}", "Approved on ${fmtDate(it)}", "تمت الموافقة بتاريخ ${fmtDate(it)}") }
                                ?: tr("Freigegeben", "Approved", "تمت الموافقة"),
                            color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold,
                        )
                        Text(tr("Alle Dateien sind ohne Wasserzeichen verfügbar.", "All files are available without a watermark.", "كل الملفات متاحة بدون علامة مائية."),
                            color = C.Muted, style = T.Caption)
                    }
                }
            }
            if (files.isEmpty()) {
                item { EmptyState(tr("Noch keine gelieferten Dateien.", "No delivered files yet.", "لا توجد ملفات مُسلَّمة بعد."), icon = Ic.FolderCheck) }
            }
            items(files, key = { it.id }) { f ->
                Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(64.dp).clip(R.ThumbShape).background(C.Surface2)) { FileCover(f, Modifier.fillMaxSize(), 200) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(f.fileName ?: f.title(), color = C.Ink, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            fileSizeLabel(f)?.let { Text(it, color = C.Muted, style = T.Caption, maxLines = 1) }
                        }
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(C.GoldTint)
                                .clickable { Media.download(f.url, f.fileName ?: f.name)?.let(::openUrl) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Ic.Download, s.download, tint = C.GoldLabel, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.GlassStrong).padding(12.dp),
                    verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Ic.LockSm, null, tint = C.Muted, modifier = Modifier.size(16.dp))
                    Text(
                        tr("Vorschauen vor der Freigabe sind mit Wasserzeichen versehen und nicht herunterladbar.",
                            "Previews before approval carry a watermark and cannot be downloaded.",
                            "المعاينات قبل الموافقة تحمل علامة مائية ولا يمكن تنزيلها."),
                        color = C.Muted, style = T.Caption,
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(tr("Link teilen", "Share link", "مشاركة الرابط"), icon = Ic.Share, modifier = Modifier.weight(1f), onClick = {
                        val link = files.firstOrNull()?.let { Media.share(it) }
                        if (link != null) shareText(link)
                    })
                    PrimaryButton(tr("Alle laden", "Download all", "تحميل الكل"), icon = Ic.Download, modifier = Modifier.weight(1f), onClick = {
                        files.forEach { f -> Media.download(f.url, f.fileName ?: f.name)?.let(::openUrl) }
                    })
                }
            }
            if (files.isNotEmpty()) item {
                Text(
                    tr("Wie war die Zusammenarbeit? Bewerten →", "How was working together? Rate it →", "كيف كان التعاون؟ قيّم الآن ←"),
                    color = C.GoldLabel, style = T.Footnote, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { rate = true },
                )
            }
        }
    }

    val ratingLabel = tr("Bewertung", "Rating", "تقييم")
    if (rate) RatingSheet(projectName, onDismiss = { rate = false }) { stars, tags, note ->
        rate = false
        val summary = buildString {
            append("⭐ ").append(ratingLabel).append(": ").append(stars).append("/5 — ").append(projectName)
            if (tags.isNotEmpty()) append("\n").append(tags.joinToString(", "))
            if (note.isNotBlank()) append("\n").append(note)
        }
        scope.launch { runCatching { state.api.sendChat(summary) } }
    }
}

@Composable
private fun RatingSheet(projectName: String, onDismiss: () -> Unit, onSend: (stars: Int, tags: List<String>, note: String) -> Unit) {
    var stars by remember { mutableStateOf(5) }
    var note by remember { mutableStateOf("") }
    val allTags = listOf(
        tr("Bildqualität", "Image quality", "جودة الصورة"),
        tr("Kreativität", "Creativity", "الإبداع"),
        tr("Kommunikation", "Communication", "التواصل"),
        tr("Pünktlichkeit", "Punctuality", "الالتزام بالمواعيد"),
        tr("Preis-Leistung", "Value for money", "القيمة مقابل السعر"),
    )
    val tags = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val starLabels = listOf(
        tr("Sehr unzufrieden", "Very unhappy", "غير راضٍ إطلاقاً"),
        tr("Unzufrieden", "Unhappy", "غير راضٍ"),
        tr("Okay", "Okay", "مقبول"),
        tr("Gut", "Good", "جيد"),
        tr("Sehr gut", "Very good", "ممتاز"),
    )
    var consent by remember { mutableStateOf(true) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(C.Scrim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier.fillMaxWidth().clip(R.SheetShape).background(C.Surface)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.width(36.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(C.Separator))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr("Wie zufrieden bist du?", "How satisfied are you?", "ما مدى رضاك؟"), color = C.Ink, style = T.Title3, fontWeight = FontWeight.SemiBold)
                    Text(projectName, color = C.Muted, style = T.Caption)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in 1..5) {
                        Icon(
                            Ic.Star, null,
                            tint = if (i <= stars) C.Gold else C.Separator,
                            modifier = Modifier.size(36.dp).clickable { stars = i },
                        )
                    }
                }
                Text(starLabels[(stars - 1).coerceIn(0, 4)], color = C.GoldLabel, style = T.BodyS, fontWeight = FontWeight.SemiBold)
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr("Was hat dir gefallen?", "What did you like?", "ما الذي أعجبك؟"), color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allTags.forEach { t ->
                            val on = tags.contains(t)
                            Text(t, color = if (on) C.GoldLabel else C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, maxLines = 1,
                                modifier = Modifier.height(34.dp).clip(R.Pill).background(if (on) C.GoldTint else C.GlassStrong)
                                    .then(if (on) Modifier.border(1.dp, C.Gold, R.Pill) else Modifier)
                                    .clickable { if (on) tags.remove(t) else tags.add(t) }.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(80.dp).clip(R.FieldShape).background(C.GlassStrong).padding(horizontal = 14.dp, vertical = 12.dp)) {
                        if (note.isEmpty()) Text(tr("Erzähl uns mehr … (optional)", "Tell us more … (optional)", "أخبرنا أكثر … (اختياري)"), color = C.Muted, style = T.BodyS)
                        BasicTextField(note, { note = it }, textStyle = T.BodyS.copy(color = C.Ink), cursorBrush = SolidColor(C.Gold), modifier = Modifier.fillMaxSize())
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (consent) C.Gold else C.GlassStrong)
                                .clickable { consent = !consent },
                            contentAlignment = Alignment.Center,
                        ) { if (consent) Icon(Ic.CheckBold, null, tint = C.OnAccent, modifier = Modifier.size(13.dp)) }
                        Text(
                            tr("Focus On Top darf meine Bewertung auf der Website zeigen.",
                                "Focus On Top may show my rating on the website.",
                                "يحق لـ Focus On Top عرض تقييمي على الموقع."),
                            color = C.Muted, style = T.Caption,
                        )
                    }
                }
                val consentNote = tr("Darf auf der Website gezeigt werden", "May be shown on the website", "يمكن عرضه على الموقع")
                PrimaryButton(tr("Bewertung senden", "Send rating", "إرسال التقييم"), modifier = Modifier.fillMaxWidth(), onClick = {
                    val full = note + (if (consent) "\n($consentNote)" else "")
                    onSend(stars, tags.toList(), full)
                })
            }
        }
    }
}
