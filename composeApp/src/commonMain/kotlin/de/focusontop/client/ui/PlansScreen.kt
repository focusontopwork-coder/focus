package de.focusontop.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.focusontop.client.data.AppState
import de.focusontop.client.data.Screen
import de.focusontop.client.i18n.tr

/** Plan catalogue as designed in Figma 29b / 29c (prices in € per month, net). */
private data class Plan(val key: String, val name: String, val price: Int, val reels: String, val photos: String, val storage: String, val rounds: String,
                        val social: Boolean, val strategy: Boolean, val ads: Boolean, val personal: Boolean)

private val PLANS = listOf(
    Plan("starter", "Starter", 299, "4", "20", "50 GB", "1", social = false, strategy = false, ads = false, personal = false),
    Plan("pro", "Pro", 699, "8", "40", "200 GB", "2", social = true, strategy = true, ads = false, personal = false),
    Plan("business", "Business", 1490, "16", "80", "1 TB", "∞", social = true, strategy = true, ads = true, personal = true),
)

private fun eur(v: Int): String = v.toString().reversed().chunked(3).joinToString(".").reversed() + " €"

/** Figma 29b · Tarife. */
@Composable
fun PlansScreen(state: AppState) {
    var yearly by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(tr("Tarife", "Plans", "الباقات"), onBack = { state.back() })
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tr("Wähle den Tarif, der zu deinem Content passt. Jederzeit monatlich kündbar.", "Choose the plan that fits your content. Cancel monthly at any time.", "اختر الباقة المناسبة لمحتواك. يمكن الإلغاء شهرياً في أي وقت."), color = C.Muted, style = T.BodyS)
            Row(Modifier.fillMaxWidth().height(44.dp).clip(R.Pill).background(C.GlassStrong).padding(4.dp)) {
                listOf(tr("Monatlich", "Monthly", "شهري"), tr("Jährlich · −15 %", "Yearly · −15 %", "سنوي · −15 %")).forEachIndexed { i, l ->
                    val on = (i == 1) == yearly
                    Box(Modifier.weight(1f).fillMaxHeight().clip(R.Pill).then(if (on) Modifier.background(C.Surface2) else Modifier).clickable { yearly = i == 1 }, contentAlignment = Alignment.Center) {
                        Text(l, color = if (on) C.Ink else C.Muted, style = T.Footnote, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            PLANS.forEach { p -> PlanCard(state, p, yearly) }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp).clickable { state.go(Screen.PlanCompare) }, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Alle Tarife vergleichen", "Compare all plans", "قارن كل الباقات"), color = C.GoldLabel, style = T.BodyS, fontWeight = FontWeight.SemiBold)
                Icon(Ic.ChevronRight175, null, tint = C.GoldLabel, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PlanCard(state: AppState, p: Plan, yearly: Boolean) {
    val popular = p.key == "pro"
    val price = if (yearly) kotlin.math.round(p.price * 0.85).toInt() else p.price
    val desc = when (p.key) {
        "starter" -> tr("Für den Einstieg mit regelmäßigem Content.", "For getting started with regular content.", "للبداية بمحتوى منتظم.")
        "pro" -> tr("Für Marken, die wöchentlich sichtbar sein wollen.", "For brands that want to be seen every week.", "للعلامات التي تريد الظهور أسبوعياً.")
        else -> tr("Komplette Content-Produktion mit Ads.", "Complete content production with ads.", "إنتاج محتوى متكامل مع الإعلانات.")
    }
    val features = buildList {
        add(tr("${p.reels} Reels pro Monat", "${p.reels} reels per month", "${p.reels} ريلز شهرياً"))
        add(tr("${p.photos} bearbeitete Fotos", "${p.photos} edited photos", "${p.photos} صورة معدّلة"))
        add(tr("${p.storage} Speicher", "${p.storage} storage", "مساحة ${p.storage}"))
        add(if (p.rounds == "∞") tr("Unbegrenzte Korrekturen", "Unlimited revisions", "تعديلات غير محدودة") else if (p.rounds == "1") tr("1 Korrekturrunde", "1 revision round", "جولة تعديل واحدة") else tr("${p.rounds} Korrekturrunden", "${p.rounds} revision rounds", "${p.rounds} جولات تعديل"))
        if (p.ads) add(tr("Social Media + Werbeanzeigen", "Social media + ads", "سوشيال ميديا + إعلانات")) else if (p.social) add(tr("Social-Media-Betreuung", "Social media management", "إدارة السوشيال ميديا"))
        if (p.personal) add(tr("Persönlicher Ansprechpartner", "Personal contact", "مسؤول تواصل شخصي"))
    }
    val draft = tr("Ich möchte den Tarif ${p.name} buchen.", "I'd like to book the ${p.name} plan.", "أرغب بالاشتراك في باقة ${p.name}.")
    Column(
        Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(if (popular) 1.5.dp else 1.dp, if (popular) C.Gold else C.Separator, R.CardShape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(p.name, color = C.Ink, style = T.Title3)
            if (popular) Text(tr("Beliebt", "Popular", "الأكثر طلباً"), color = C.OnAccent, style = T.Caption, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.height(22.dp).clip(R.Pill).background(C.Gold).padding(horizontal = 10.dp, vertical = 3.dp))
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(eur(price), color = C.Ink, style = T.Title1)
            Text(tr("/ Monat", "/ month", "/ شهرياً"), color = C.Muted, style = T.Footnote, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text(desc, color = C.Muted, style = T.Footnote)
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            features.forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Ic.CheckBold, null, tint = C.GoldLabel, modifier = Modifier.size(16.dp))
                    Text(f, color = C.Ink, style = T.BodyS)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(48.dp).clip(R.Pill).background(if (popular) C.Gold else C.GlassStrong).clickable { state.go(Screen.Chat(draft = draft)) }, contentAlignment = Alignment.Center) {
            Text(tr("${p.name} wählen", "Choose ${p.name}", "اختر ${p.name}"), color = if (popular) C.OnAccent else C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Figma 29c · Tarifvergleich. */
@Composable
fun PlanCompareScreen(state: AppState) {
    val rows: List<Pair<String, (Plan) -> Any>> = listOf(
        tr("Preis / Monat", "Price / month", "السعر / شهرياً") to { p: Plan -> eur(p.price) },
        tr("Reels pro Monat", "Reels per month", "ريلز شهرياً") to { p: Plan -> p.reels },
        tr("Fotos pro Monat", "Photos per month", "صور شهرياً") to { p: Plan -> p.photos },
        tr("Speicher", "Storage", "المساحة") to { p: Plan -> p.storage },
        tr("Korrekturrunden", "Revision rounds", "جولات التعديل") to { p: Plan -> p.rounds },
        tr("Social-Media-Betreuung", "Social media management", "إدارة السوشيال ميديا") to { p: Plan -> p.social },
        tr("Content-Strategie", "Content strategy", "استراتيجية المحتوى") to { p: Plan -> p.strategy },
        tr("Werbeanzeigen", "Ads", "الإعلانات") to { p: Plan -> p.ads },
        tr("Persönlicher Ansprechpartner", "Personal contact", "مسؤول تواصل شخصي") to { p: Plan -> p.personal },
        tr("Monatlich kündbar", "Cancel monthly", "إلغاء شهري") to { _: Plan -> true },
    )
    val draft = tr("Ich möchte den Tarif Pro buchen.", "I'd like to book the Pro plan.", "أرغب بالاشتراك في باقة Pro.")
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(tr("Tarifvergleich", "Plan comparison", "مقارنة الباقات"), onBack = { state.back() })
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.dp, C.Separator, R.CardShape)) {
                Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Leistung", "Feature", "الميزة"), color = C.Muted, style = T.Footnote, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(152f).padding(start = 14.dp, end = 8.dp))
                    PLANS.forEach { Text(it.name, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(68f)) }
                }
                rows.forEach { (label, value) ->
                    Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
                    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = C.Ink, style = T.Footnote, modifier = Modifier.weight(152f).padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp))
                        PLANS.forEach { p ->
                            Box(Modifier.weight(68f), contentAlignment = Alignment.Center) {
                                when (val v = value(p)) {
                                    true -> Icon(Ic.CheckBold, null, tint = C.GoldLabel, modifier = Modifier.size(16.dp))
                                    false -> Box(Modifier.width(6.dp).height(2.dp).background(C.Faint))
                                    else -> Text(v.toString(), color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(50.dp).clip(R.Pill).background(C.Gold).clickable { state.go(Screen.Chat(draft = draft)) }, contentAlignment = Alignment.Center) {
                Text(tr("Pro wählen", "Choose Pro", "اختر Pro"), color = C.OnAccent, style = T.Body, fontWeight = FontWeight.SemiBold)
            }
            Text(tr("Alle Preise zzgl. MwSt. Tarifwechsel jederzeit zum Monatsende möglich.", "All prices plus VAT. Plan changes possible at the end of any month.", "جميع الأسعار غير شاملة الضريبة. يمكن تغيير الباقة في نهاية أي شهر."),
                color = C.Muted, style = T.Caption, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
