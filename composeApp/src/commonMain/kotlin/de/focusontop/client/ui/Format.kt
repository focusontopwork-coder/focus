package de.focusontop.client.ui

import androidx.compose.ui.graphics.Color
import de.focusontop.client.data.ClientFile
import de.focusontop.client.data.Invoice
import de.focusontop.client.data.asDouble
import de.focusontop.client.i18n.Strings

/** "2026-10-16T09:00:00Z" → "16.10.2026" (no time-zone maths needed for a date). */
fun fmtDate(iso: String?): String {
    if (iso.isNullOrBlank() || iso.length < 10) return "—"
    val y = iso.substring(0, 4); val m = iso.substring(5, 7); val d = iso.substring(8, 10)
    return "$d.$m.$y"
}

fun fmtMoney(v: Double?, currency: String?): String {
    if (v == null) return "—"
    val whole = v.toLong()
    val cents = ((v - whole) * 100).let { kotlin.math.round(it).toLong() }
    val grouped = whole.toString().reversed().chunked(3).joinToString(".").reversed()
    val c = when (currency?.uppercase()) { null, "", "EUR", "€" -> "€"; else -> currency }
    return if (cents == 0L) "$c $grouped" else "$c $grouped,${cents.toString().padStart(2, '0')}"
}

data class StatusLook(val label: String, val fg: Color)

fun taskStatus(s: Strings, status: String?): StatusLook = when (status) {
    "in_progress" -> StatusLook(s.stInProgress, C.GreenText)
    "review" -> StatusLook(s.stReview, C.Gold)
    "revision_requested" -> StatusLook(s.stRevision, C.Red)
    "completed" -> StatusLook(s.stCompleted, C.Blue)
    else -> StatusLook(s.stTodo, C.Muted)
}

fun fileStatus(s: Strings, status: String?): StatusLook = when (status) {
    "ready_for_review" -> StatusLook(s.fReady, C.Gold)
    "revision_requested" -> StatusLook(s.fRevision, C.Red)
    "approved" -> StatusLook(s.fApproved, C.GreenText)
    else -> StatusLook(s.fDelivered, C.Blue)
}

fun invoiceStatus(s: Strings, inv: Invoice, today: String): StatusLook = when {
    inv.status == "paid" || inv.status == "bezahlt" -> StatusLook(s.paid, C.GreenText)
    !inv.dueDate.isNullOrBlank() && inv.dueDate.take(10) < today -> StatusLook(s.overdue, C.Red)
    else -> StatusLook(s.open_, C.Gold)
}

fun invoiceTotal(inv: Invoice): Double? = inv.total.asDouble() ?: inv.amount.asDouble()

fun ClientFile.title(): String = name ?: fileName ?: "—"

/** Day of week for "YYYY-MM-DD": 0 = Monday … 6 = Sunday (no date library needed). */
fun weekdayIndex(iso: String): Int {
    var y = iso.substring(0, 4).toInt(); val m = iso.substring(5, 7).toInt(); val d = iso.substring(8, 10).toInt()
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    if (m < 3) y -= 1
    val sun0 = (y + y / 4 - y / 100 + y / 400 + t[m - 1] + d) % 7   // 0 = Sunday
    return (sun0 + 6) % 7
}

/** "YYYY-MM-DD" + 1 day. */
fun nextDayIso(iso: String): String {
    var y = iso.substring(0, 4).toInt(); var m = iso.substring(5, 7).toInt(); var d = iso.substring(8, 10).toInt() + 1
    val leap = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
    val days = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    if (d > days[m - 1]) { d = 1; m += 1; if (m > 12) { m = 1; y += 1 } }
    return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}

/** "Today" / "Tomorrow" / "Wednesday, 15 October". */
fun dayHeader(s: Strings, iso: String, today: String): String = when (iso) {
    today -> s.today
    nextDayIso(today) -> s.tomorrow
    else -> "${s.weekdays[weekdayIndex(iso)]}، ${iso.substring(8, 10).toInt()} ${s.months[iso.substring(5, 7).toInt() - 1]}"
        .let { if (s.today == "اليوم") it else it.replace("، ", ", ") }
}

fun daysInMonth(y: Int, m: Int): Int {
    val leap = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
    return intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)[m - 1]
}

fun iso(y: Int, m: Int, d: Int) = "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"

/** Days since 1970-01-01 for a civil date (UTC). */
private fun daysFromCivil(y0: Int, m: Int, d: Int): Long {
    val y = if (m <= 2) y0 - 1 else y0
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097L + doe - 719468L
}

/** "2026-10-14 09:30:00" (server UTC) or ISO → epoch ms; null if unreadable. */
fun epochOf(s: String?): Long? {
    if (s.isNullOrBlank() || s.length < 10) return null
    return runCatching {
        val y = s.substring(0, 4).toInt(); val mo = s.substring(5, 7).toInt(); val d = s.substring(8, 10).toInt()
        val h = if (s.length >= 13) s.substring(11, 13).toInt() else 0
        val mi = if (s.length >= 16) s.substring(14, 16).toInt() else 0
        val se = if (s.length >= 19) s.substring(17, 19).toIntOrNull() ?: 0 else 0
        var ms = ((daysFromCivil(y, mo, d) * 24 + h) * 60 + mi) * 60_000L + se * 1000L
        // "+02:00" style offsets
        Regex("([+-])(\\d{2}):?(\\d{2})$").find(s)?.let { m ->
            val off = (m.groupValues[2].toInt() * 60 + m.groupValues[3].toInt()) * 60_000L
            ms -= if (m.groupValues[1] == "+") off else -off
        }
        ms
    }.getOrNull()
}

/** Arabic counted nouns: 1 / 2 / 3–10 / 11+. */
private fun arCount(n: Long, one: String, two: String, few: String, many: String) = when {
    n == 1L -> one
    n == 2L -> two
    n in 3..10 -> "$n $few"
    else -> "$n $many"
}

/** "vor 2 Std." / "2 h ago" / "منذ ساعتين". */
fun relativeTime(lang: de.focusontop.client.i18n.Lang, createdAt: String?, now: Long): String {
    val t = epochOf(createdAt) ?: return ""
    val min = ((now - t) / 60_000).coerceAtLeast(0)
    val h = min / 60; val d = h / 24; val w = d / 7
    return when (lang) {
        de.focusontop.client.i18n.Lang.DE -> when {
            min < 1 -> "gerade eben"; min < 60 -> "vor $min Min."; h < 24 -> "vor $h Std."
            d < 7 -> if (d == 1L) "vor 1 Tag" else "vor $d Tagen"; w < 5 -> if (w == 1L) "vor 1 Woche" else "vor $w Wochen"
            else -> fmtDate(createdAt)
        }
        de.focusontop.client.i18n.Lang.EN -> when {
            min < 1 -> "just now"; min < 60 -> "$min min ago"; h < 24 -> "$h h ago"
            d < 7 -> if (d == 1L) "1 day ago" else "$d days ago"; w < 5 -> if (w == 1L) "1 week ago" else "$w weeks ago"
            else -> fmtDate(createdAt)
        }
        de.focusontop.client.i18n.Lang.AR -> when {
            min < 1 -> "الآن"
            min < 60 -> "منذ " + arCount(min, "دقيقة", "دقيقتين", "دقائق", "دقيقة")
            h < 24 -> "منذ " + arCount(h, "ساعة", "ساعتين", "ساعات", "ساعة")
            d < 7 -> "منذ " + arCount(d, "يوم", "يومين", "أيام", "يومًا")
            w < 5 -> "منذ " + arCount(w, "أسبوع", "أسبوعين", "أسابيع", "أسبوعًا")
            else -> fmtDate(createdAt)
        }
    }
}

/** Bytes → "12.4" GB (one decimal). */
fun gb(bytes: Double): String {
    val v = bytes / 1_000_000_000.0
    val r = kotlin.math.round(v * 10) / 10.0
    return if (r == kotlin.math.floor(r)) r.toLong().toString() else r.toString()
}

fun num(v: Double?): String = when {
    v == null -> "—"
    v == kotlin.math.floor(v) -> v.toLong().toString()
    else -> (kotlin.math.round(v * 10) / 10.0).toString()
}

/** Arabic "n days" with the right noun form. */
fun arDays(n: Int): String = when {
    n == 1 -> "يوم واحد"
    n == 2 -> "يومين"
    n in 3..10 -> "$n أيام"
    else -> "$n يومًا"
}

/** "YYYY-MM-DD" ± n days. */
fun addDaysIso(isoDate: String, n: Int): String {
    val y0 = isoDate.substring(0, 4).toInt(); val m0 = isoDate.substring(5, 7).toInt(); val d0 = isoDate.substring(8, 10).toInt()
    val y1 = if (m0 <= 2) y0 - 1 else y0
    val era = (if (y1 >= 0) y1 else y1 - 399) / 400
    val yoe = y1 - era * 400
    val doy = (153 * (m0 + (if (m0 > 2) -3 else 9)) + 2) / 5 + d0 - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    var z = era * 146097L + doe - 719468L + n
    // civil from days
    z += 719468
    val era2 = (if (z >= 0) z else z - 146096) / 146097
    val doe2 = z - era2 * 146097
    val yoe2 = (doe2 - doe2 / 1460 + doe2 / 36524 - doe2 / 146096) / 365
    val doy2 = doe2 - (365 * yoe2 + yoe2 / 4 - yoe2 / 100)
    val mp = (5 * doy2 + 2) / 153
    val d = (doy2 - (153 * mp + 2) / 5 + 1).toInt()
    val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val y = (yoe2 + era2 * 400 + (if (m <= 2) 1 else 0)).toInt()
    return iso(y, m, d)
}

/** Monday of the week that contains [isoDate]. */
fun weekStart(isoDate: String): String = addDaysIso(isoDate, -weekdayIndex(isoDate))
