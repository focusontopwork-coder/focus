package de.focusontop.client.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Lucide icons (ISC licence, lucide.dev) — the icon set used in the Figma file.
 * 24×24 viewport, round caps/joins, stroke 1.5 (Figma screens); post actions 1.75, tab bar 1.6. Icon(tint) colours them.
 * Directional icons auto-mirror in RTL.
 */
object Ic {
    private const val W = 1.5f

    private fun c(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"
    private fun rr(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

    /** Outline icon. */
    fun lucide(name: String, vararg d: String, mirror: Boolean = false, width: Float = W): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f, autoMirror = mirror).apply {
            d.forEach {
                addPath(pathData = addPathNodes(it), fill = null, stroke = SolidColor(Color.Black),
                    strokeLineWidth = width, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            }
        }.build()

    /** Solid icon (liked heart, play). */
    private fun solid(name: String, vararg d: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            d.forEach {
                addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black), stroke = SolidColor(Color.Black),
                    strokeLineWidth = W, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            }
        }.build()

    private const val HOUSE_A = "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"
    private const val HOUSE_B = "M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"
    private const val HEART = "M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"
    private const val STAR_PATH = "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z"
    private const val CLAP_TOP = "M20.2 6 3 11l-.9-2.4c-.3-1.1.3-2.2 1.3-2.5l13.5-4c1.1-.3 2.2.3 2.5 1.3Z"
    private const val CLAP_BODY = "M3 11h18v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z"

    // navigation
    val Home = lucide("house", HOUSE_A, HOUSE_B, width = 1.6f)
    val HomeFill = lucide("house-active", HOUSE_A, HOUSE_B, width = 2.1f)
    val Search = lucide("search", c(11f, 11f, 8f), "m21 21-4.3-4.3", width = 1.6f)
    val Plus = lucide("plus", "M5 12h14", "M12 5v14", width = 2f)
    val Clapper = lucide("clapperboard", CLAP_TOP, "m6.2 5.3 3.1 3.9", "m12.4 3.4 3.1 4", CLAP_BODY, width = 1.6f)
    val ClapperActive = lucide("clapperboard-active", CLAP_TOP, "m6.2 5.3 3.1 3.9", "m12.4 3.4 3.1 4", CLAP_BODY, width = 2.1f)
    val User = lucide("user-round", c(12f, 8f, 5f), "M20 21a8 8 0 0 0-16 0", width = 1.6f)
    val UserActive = lucide("user-round-active", c(12f, 8f, 5f), "M20 21a8 8 0 0 0-16 0", width = 2.1f)
    val SearchActive = lucide("search-active", c(11f, 11f, 8f), "m21 21-4.3-4.3", width = 2.1f)

    // content
    val Grid = lucide("grid-3x3", rr(3f, 3f, 18f, 18f, 2f), "M3 9h18", "M3 15h18", "M9 3v18", "M15 3v18")
    val GridFill = lucide("grid-3x3-active", rr(3f, 3f, 18f, 18f, 2f), "M3 9h18", "M3 15h18", "M9 3v18", "M15 3v18", width = 2.1f)
    val Calendar = lucide("calendar", "M8 2v4", "M16 2v4", rr(3f, 4f, 18f, 18f, 2f), "M3 10h18")
    val CalendarFill = lucide("calendar-active", "M8 2v4", "M16 2v4", rr(3f, 4f, 18f, 18f, 2f), "M3 10h18", width = 2.1f)
    val CalendarPlus = lucide("calendar-plus", "M8 2v4", "M16 2v4", "M21 13V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h8", "M3 10h18", "M16 19h6", "M19 16v6")
    val Bell = lucide("bell", "M10.268 21a2 2 0 0 0 3.464 0", "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326")
    val Chat = lucide("message-circle", "M7.9 20A9 9 0 1 0 4 16.1L2 22Z", width = 1.75f)
    val ChatLines = lucide("message-square-text", "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z", "M13 8H7", "M17 12H7")
    val Heart = lucide("heart", HEART, width = 1.75f)
    val Star = solid("star", STAR_PATH)
    val HeartFill = solid("heart-filled", HEART)
    val Send = lucide("send", "M14.536 21.686a.5.5 0 0 0 .937-.024l6.5-19a.496.496 0 0 0-.635-.635l-19 6.5a.5.5 0 0 0-.024.937l7.93 3.18a2 2 0 0 1 1.112 1.11z", "m21.854 2.147-10.94 10.939", width = 1.75f)
    val Bookmark = lucide("bookmark", "m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z", width = 1.75f)
    val More = lucide("ellipsis", c(12f, 12f, 1f), c(19f, 12f, 1f), c(5f, 12f, 1f))
    val Menu = More
    val Play = solid("play", "M6 3 20 12 6 21 6 3z")
    val Pause = solid("pause", rr(6f, 4f, 4f, 16f, 1f), rr(14f, 4f, 4f, 16f, 1f))
    val Video = lucide("video", "m16 13 5.223 3.482a.5.5 0 0 0 .777-.416V7.87a.5.5 0 0 0-.752-.432L16 10.5", rr(2f, 6f, 14f, 12f, 2f))
    val Image = lucide("image", rr(3f, 3f, 18f, 18f, 2f), c(9f, 9f, 2f), "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21")
    val Camera = lucide("camera", "M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z", c(12f, 13f, 3f))
    val Music = lucide("music", "M9 18V5l12-2v13", c(6f, 18f, 3f), c(18f, 16f, 3f))
    val Pencil = lucide("pencil-line", "M12 20h9", "M16.376 3.622a1 1 0 0 1 3.002 3.002L7.368 18.635a2 2 0 0 1-.855.506l-2.872.838a.5.5 0 0 1-.62-.62l.838-2.872a2 2 0 0 1 .506-.854z")
    val Paperclip = lucide("paperclip", "m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48")
    val Mic = lucide("mic", "M12 19v3", "M19 10v2a7 7 0 0 1-14 0v-2", rr(9f, 2f, 6f, 13f, 3f))
    val ScanFace = lucide(
        "scan-face",
        "M3 7V5a2 2 0 0 1 2-2h2", "M17 3h2a2 2 0 0 1 2 2v2", "M21 17v2a2 2 0 0 1-2 2h-2", "M7 21H5a2 2 0 0 1-2-2v-2",
        "M8 14s1.5 2 4 2 4-2 4-2", "M9 9h.01", "M15 9h.01",
    )
    val UserPlus = lucide("user-plus", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", c(9f, 7f, 4f), "M19 8v6", "M22 11h-6")
    val Users = lucide("users", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", c(9f, 7f, 4f), "M22 21v-2a4 4 0 0 0-3-3.87", "M16 3.13a4 4 0 0 1 0 7.75")
    val PlusBold = lucide("plus", "M5 12h14", "M12 5v14", width = 2f)
    val Instagram = lucide("instagram", rr(2f, 2f, 20f, 20f, 5f), "M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z", "M17.5 6.5h.01")
    val Facebook = lucide("facebook", "M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z")
    val PlusCircle = lucide("circle-plus", c(12f, 12f, 10f), "M8 12h8", "M12 8v8")
    val Share = lucide("share", "M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8", "m16 6-4-4-4 4", "M12 2v13")
    val CheckBold = lucide("check", "M20 6 9 17l-5-5", width = 3f)
    val Compare = lucide("git-compare", c(18f, 6f, 3f), c(6f, 18f, 3f), "M13 6h3a2 2 0 0 1 2 2v7", "M11 18H8a2 2 0 0 1 -2 -2V9")
    val HardDrive = lucide("hard-drive", "M22 12H2", "M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z", "M6 16h.01", "M10 16h.01")
    val CalendarDays = lucide("calendar-days", rr(3f, 4f, 18f, 18f, 2f), "M16 2v4", "M8 2v4", "M3 10h18", "M8 14h.01", "M12 14h.01", "M16 14h.01", "M8 18h.01", "M12 18h.01", "M16 18h.01")
    val FileText = lucide("file-text", "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z", "M14 2v4a2 2 0 0 0 2 2h4", "M10 9H8", "M16 13H8", "M16 17H8")
    val CloudAlert = lucide("cloud-alert", "M12 12v4", "M12 20h.01", "M17 18h.5a1 1 0 0 0 0-9h-1.79A7 7 0 1 0 7 17.708", width = 1.25f)
    val FolderCheck = lucide("folder-check", "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z", "m9 13 2 2 4-4")
    val Grid3 = lucide("grid-3x3", rr(3f, 3f, 18f, 18f, 2f), "M3 9h18", "M3 15h18", "M9 3v18", "M15 3v18")
    val Settings = lucide("settings", "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z", c(12f, 12f, 3f))
    val SquarePen = lucide("square-pen", "M12 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7", "M18.375 2.625a1 1 0 0 1 3 3l-9.013 9.014a2 2 0 0 1-.853.505l-2.873.84a.5.5 0 0 1-.62-.62l.84-2.873a2 2 0 0 1 .506-.852z")
    val ListChecks = lucide("list-checks", "m3 17 2 2 4-4", "m3 7 2 2 4-4", "M13 6h8", "M13 12h8", "M13 18h8")
    val X = lucide("x", "M18 6 6 18", "m6 6 12 12")
    val CameraBold = lucide("camera", "M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z", c(12f, 13f, 3f), width = 2f)
    val CheckAllBold = lucide("check-check", "M18 6 7 17l-5-5", "m22 10-7.5 7.5L13 16", width = 2f)
    val CheckCheck = lucide("check-check", "M18 6 7 17l-5-5", "m22 10-7.5 7.5L13 16")
    val Paperclip2 = lucide("paperclip", "m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48", width = 1.75f)
    val MoreH = lucide("ellipsis", c(12f, 12f, 1f), c(19f, 12f, 1f), c(5f, 12f, 1f))
    val Tag = lucide("tag", "M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z", c(7.5f, 7.5f, .5f), width = 1.75f)
    val GridL = lucide("grid", rr(3f, 3f, 18f, 18f, 2f), "M3 9h18", "M3 15h18", "M9 3v18", "M15 3v18", width = 1.75f)
    val ClapperL = lucide("clapperboard", "M20.2 6 3 11l-.9-2.4c-.3-1.1.3-2.2 1.3-2.5l13.5-4c1.1-.3 2.2.3 2.5 1.3Z", "m6.2 5.3 3.1 3.9", "m12.4 3.4 3.1 4", "M3 11h18v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z", width = 1.75f)
    val BadgeCheckT = lucide("badge-check", "M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z", "m9 12 2 2 4-4")
    val LockKeyholeThin = lucide("lock-keyhole", c(12f, 16f, 1f), rr(3f, 10f, 18f, 12f, 2f), "M7 10V7a5 5 0 0 1 10 0v3", width = 1.25f)
    val ImagePlus = lucide("image-plus", "M16 5h6", "M19 2v6", "M21 11.5V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7.5", "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21", c(9f, 9f, 2f), width = 1.25f)
    val CircleHelp = lucide("circle-help", c(12f, 12f, 10f), "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3", "M12 17h.01")
    val Trash = lucide("trash-2", "M3 6h18", "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6", "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2", "M10 11v6", "M14 11v6")
    val LockSm = lucide("lock", rr(3f, 11f, 18f, 11f, 2f), "M7 11V7a5 5 0 0 1 10 0v4", width = 1.75f)
    val XSm = lucide("x", "M18 6 6 18", "m6 6 12 12", width = 1.75f)
    val Link2 = lucide("link-2", "M9 17H7A5 5 0 0 1 7 7h2", "M15 7h2a5 5 0 1 1 0 10h-2", "M8 12h8", width = 1.75f)
    val Languages = lucide("languages", "m5 8 6 6", "m4 14 6-6 2-3", "M2 5h12", "M7 2h1", "m22 22-5-10-5 10", "M14 18h6")
    val Sun = lucide("sun", c(12f, 12f, 4f), "M12 2v2", "M12 20v2", "m4.93 4.93 1.41 1.41", "m17.66 17.66 1.41 1.41", "M2 12h2", "M20 12h2", "m6.34 17.66-1.41 1.41", "m19.07 4.93-1.41 1.41")
    val ShieldCheck15 = lucide("shield-check", "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z", "m9 12 2 2 4-4")
    val Wallet = lucide("wallet", "M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1", "M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4", width = 1.75f)
    val FileText175 = lucide("file-text", "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z", "M14 2v4a2 2 0 0 0 2 2h4", "M10 9H8", "M16 13H8", "M16 17H8", width = 1.75f)
    val Clapper175 = lucide("clapperboard", "M20.2 6 3 11l-.9-2.4c-.3-1.1.3-2.2 1.3-2.5l13.5-4c1.1-.3 2.2.3 2.5 1.3Z", "m6.2 5.3 3.1 3.9", "m12.4 3.4 3.1 4", "M3 11h18v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z", width = 1.75f)
    val Eye175 = lucide("eye", "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0", c(12f, 12f, 3f), width = 1.75f)
    val Calendar175 = lucide("calendar", rr(3f, 4f, 18f, 18f, 2f), "M16 2v4", "M8 2v4", "M3 10h18", width = 1.75f)
    val ChevronRight175 = lucide("chevron-right", "m9 18 6-6-6-6", mirror = true, width = 1.75f)
    val Plus175 = lucide("plus", "M5 12h14", "M12 5v14", width = 1.75f)
    val ArrowUpBold = lucide("arrow-up", "m5 12 7-7 7 7", "M12 19V5", width = 2.2f)
    val Mail = lucide("mail", rr(2f, 4f, 20f, 16f, 2f), "m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7")
    val Chrome = lucide("chrome", c(12f, 12f, 10f), c(12f, 12f, 4f), "M21.17 8H12", "M3.95 6.06 8.54 14", "M10.88 21.94 15.46 14")
    val ShieldCheck = lucide("shield-check", "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z", "m9 12 2 2 4-4", width = 1.25f)
    val Hourglass = lucide("hourglass", "M5 22h14", "M5 2h14", "M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22", "M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2", width = 1.25f)
    val BellRing = lucide("bell-ring", "M10.268 21a2 2 0 0 0 3.464 0", "M22 8c0-2.3-.8-4.3-2-6", "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326", "M4 2C2.8 3.7 2 5.7 2 8")
    val Building = lucide("building-2", "M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18Z", "M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2", "M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2", "M10 6h4", "M10 10h4", "M10 14h4", "M10 18h4")
    val Phone = lucide("phone", "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z", width = 1.75f)
    val Circle = lucide("circle", c(12f, 12f, 10f))
    val CircleCheck = lucide("circle-check", c(12f, 12f, 10f), "m9 12 2 2 4-4")
    val LockKeyhole = lucide("lock-keyhole", c(12f, 16f, 1f), rr(3f, 10f, 18f, 12f, 2f), "M7 10V7a5 5 0 0 1 10 0v3")
    val UserRound = lucide("user-round", c(12f, 8f, 5f), "M20 21a8 8 0 0 0-16 0")
    val ChevronDownSm = lucide("chevron-down", "m6 9 6 6 6-6", width = 1.75f)
    val EyeOff = lucide("eye-off", "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49", "M14.084 14.158a3 3 0 0 1-4.242-4.242", "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143", "m2 2 20 20")
    val Eye = lucide("eye", "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0", c(12f, 12f, 3f))
    val BadgeCheck = lucide("badge-check", "M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z", "m9 12 2 2 4-4")

    // actions & status
    val Upload = lucide("upload", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m17 8-5-5-5 5", "M12 3v12")
    val Download = lucide("download", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m7 10 5 5 5-5", "M12 15V3")
    val Link = lucide("link", "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71", "M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71")
    val Check = lucide("check", "M20 6 9 17l-5-5")
    val CheckAll = lucide("check-check", "M18 6 7 17l-5-5", "m22 10-7.5 7.5L13 16")
    val CheckCircle = lucide("circle-check", c(12f, 12f, 10f), "m9 12 2 2 4-4")
    val Alert = lucide("circle-alert", c(12f, 12f, 10f), "M12 8v4", "M12 16h.01")
    val Close = lucide("x", "M18 6 6 18", "m6 6 12 12")
    val Back = lucide("arrow-left", "m12 19-7-7 7-7", "M19 12H5", mirror = true)
    val Forward = lucide("arrow-right", "M5 12h14", "m12 5 7 7-7 7", mirror = true)
    val ChevronLeft = lucide("chevron-left", "m15 18-6-6 6-6", mirror = true)
    val ChevronRight = lucide("chevron-right", "m9 18 6-6-6-6", mirror = true)
    val ChevronDown = lucide("chevron-down", "m6 9 6 6 6-6")
    val ArrowUp = lucide("arrow-up", "m5 12 7-7 7 7", "M12 19V5")
    val External = lucide("arrow-up-right", "M7 7h10v10", "M7 17 17 7", mirror = true)
    val Flip = lucide("switch-camera", "M11 19H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h5", "M13 5h7a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-5", c(12f, 12f, 3f), "m18 22-3-3 3-3", "m6 2 3 3-3 3")
    val File = lucide("file-text", "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z", "M14 2v4a2 2 0 0 0 2 2h4", "M10 9H8", "M16 13H8", "M16 17H8")
    val FileUp = lucide("file-up", "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z", "M14 2v4a2 2 0 0 0 2 2h4", "M12 12v6", "m15 15-3-3-3 3")
    val Folder = lucide("folder", "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z")
    val FolderOpen = lucide("folder-open", "m6 14 1.5-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.54 6a2 2 0 0 1-1.95 1.5H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18a2 2 0 0 1 2 2v2")
    val Cloud = lucide("cloud-upload", "M12 13v8", "M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242", "m8 17 4-4 4 4")
    val Shield = lucide("shield-check", "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z", "m9 12 2 2 4-4")
    val Lock = lucide("lock", rr(3f, 11f, 18f, 11f, 2f), "M7 11V7a5 5 0 0 1 10 0v4")
    val Card = lucide("credit-card", rr(2f, 5f, 20f, 14f, 2f), "M2 10h20")
    val Sparkles = lucide("sparkles", "M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z", "M20 3v4", "M22 5h-4")
    val Globe = lucide("globe", c(12f, 12f, 10f), "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20", "M2 12h20")
    val Moon = lucide("moon", "M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z")
    val Receipt = lucide("receipt", "M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z", "M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8", "M12 17.5v-11")
    val Logout = lucide("log-out", "m16 17 5-5-5-5", "M21 12H9", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4", mirror = true)
    val Help = lucide("circle-help", c(12f, 12f, 10f), "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3", "M12 17h.01")
    val Tasks = lucide("list-checks", "m3 17 2 2 4-4", "m3 7 2 2 4-4", "M13 6h8", "M13 12h8", "M13 18h8")
    val Pin = lucide("map-pin", "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0", c(12f, 10f, 3f))
    val Clock = lucide("clock", c(12f, 12f, 10f), "M12 6v6l4 2")
    val Zap = lucide("zap", "M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z")
    val Layers = lucide("layers", "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83Z", "m22 17.65-9.17 4.16a2 2 0 0 1-1.66 0L2 17.65", "m22 12.65-9.17 4.16a2 2 0 0 1-1.66 0L2 12.65")
}
