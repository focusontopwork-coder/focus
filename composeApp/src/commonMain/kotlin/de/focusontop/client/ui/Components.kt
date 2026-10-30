package de.focusontop.client.ui

import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import de.focusontop.client.res.logo_white
import de.focusontop.client.res.Res
import org.jetbrains.compose.resources.painterResource
import de.focusontop.client.i18n.LocalStrings
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.focusontop.client.data.ClientFile
import de.focusontop.client.data.Media

/**
 * Header / rail icon button. Figma: bare Lucide icon 24 in a 44 hit area, no tile.
 * gold = filled gold circle (primary round action). Badge = gold count, dot = red unread dot.
 */
@Composable
fun IconTile(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gold: Boolean = false,
    badge: Int = 0,
    dot: Boolean = false,
    circle: Boolean = false,
    size: Dp = 44.dp,
    desc: String? = null,
    tint: Color? = null,
) {
    Box(modifier.size(size)) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape)
                .then(if (gold) Modifier.background(C.Gold) else if (circle) Modifier.background(C.GlassStrong) else Modifier)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, desc, tint = tint ?: if (gold) C.OnAccent else C.Ink, modifier = Modifier.size(if (gold || circle) size * 0.45f else 24.dp)) }
        if (badge > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 2.dp).size(18.dp).clip(CircleShape).background(C.Gold).border(2.dp, C.Bg, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(if (badge > 9) "9+" else badge.toString(), color = C.OnAccent, style = T.Micro, fontWeight = FontWeight.SemiBold) }
        } else if (dot) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp).size(8.dp).clip(CircleShape).background(C.Red).border(1.5.dp, C.Bg, CircleShape))
        }
    }
}

/** Primary action (Figma "Button"): flat gold capsule, 50 (40 small), Body semibold. */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    small: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Row(
        modifier.height(if (small) 36.dp else 48.dp).clip(CircleShape)
            .background(if (enabled) C.Gold else C.Surface2)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = if (small) 16.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val fg = if (enabled) C.OnAccent else C.Faint
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = C.OnAccent, strokeWidth = 2.5.dp)
        else {
            if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, color = fg, style = if (small) T.Footnote else T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** Secondary action: glass capsule (fill/glass-strong), no border. */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, small: Boolean = false, color: Color = C.Ink) {
    Row(
        modifier.height(if (small) 36.dp else 48.dp).clip(CircleShape).background(C.GlassStrong).border(1.dp, C.StrokeGlass, CircleShape)
            .clickable(onClick = onClick).padding(horizontal = if (small) 16.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = color, style = if (small) T.Footnote else T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Screen header (Figma "Page header"): plain back / close icon, Title 2 semibold, actions at the end. */
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, close: Boolean = false, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconTile(if (close) Ic.Close else Ic.Back, onBack, desc = if (close) null else LocalStrings.current.back)
        else Spacer(Modifier.width(8.dp))
        Text(title, color = C.Ink, style = T.Title2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Usage meter: 4 pt track (bg/tertiary) with a gold fill. */
@Composable
fun SegmentMeter(fraction: Float, modifier: Modifier = Modifier, segments: Int = 10, height: Dp = 4.dp) {
    val f = fraction.coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(C.Surface2)) {
        if (f > 0f) Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(CircleShape).background(C.Gold))
    }
}

/** "Month 3 of 6": done = gold bars, now = gold dot, rest = dim bars. */
@Composable
fun MonthDots(index: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..total.coerceAtMost(12)) {
            when {
                i < index -> Box(Modifier.width(14.dp).height(4.dp).clip(CircleShape).background(C.Gold))
                i == index -> Box(Modifier.size(6.dp).clip(CircleShape).background(C.Gold))
                else -> Box(Modifier.width(14.dp).height(4.dp).clip(CircleShape).background(C.Surface2))
            }
        }
    }
}

/** Focus On Top team avatar: circle on bg/tertiary with the white FT mark (as in Figma). */
@Composable
fun FtBadge(size: Dp = 44.dp, dashed: Boolean = false) {
    Box(
        Modifier.size(size).clip(CircleShape).background(C.Surface2).then(if (dashed) Modifier.border(2.dp, C.Gold, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painterResource(Res.drawable.logo_white), "Focus On Top",
            Modifier.width(size * 0.62f),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(C.Ink),
        )
    }
}

/** A person: circular avatar (photo or initial on bg/tertiary). */
@Composable
fun InitialTile(name: String?, size: Dp, avatar: String? = null, dashed: Boolean = false, warm: Boolean = false) {
    Box(
        Modifier.size(size).clip(CircleShape).background(C.Surface2).then(if (dashed) Modifier.border(2.dp, C.Gold, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val src = Media.image(avatar, 256)
        if (src != null) AsyncImage(model = src, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text((name ?: "?").trim().take(1).uppercase(), color = C.GoldLabel, fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.SemiBold)
    }
}

fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 2.dp): Modifier = this.drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
        size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 4, w * 2.5f))),
    )
}

/** Placeholder while media loads: flat bg/tertiary (no decorative gradients). */
fun placeholderBrush(seed: String): Brush = SolidColor(C.Surface2)

/** The picture behind a file: image itself, video thumbnail, or a neutral placeholder. */
@Composable
fun FileCover(f: ClientFile, modifier: Modifier = Modifier, width: Int = 480) {
    Box(modifier.background(C.Surface2), contentAlignment = Alignment.Center) {
        val src = when (f.type) {
            "Image" -> Media.image(f.url, width)
            else -> Media.image(f.thumbnail, width)
        }
        if (src != null) AsyncImage(model = src, contentDescription = f.title(), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else if (f.type == "Document" || f.type == "Link") Icon(Ic.File, null, tint = C.Muted, modifier = Modifier.size(28.dp))
    }
}

/** Grid tile (Figma "File · …"): radius/thumb 10, duration chip for videos, status badge. */
/** Figma "File" tile: radius 10; video pill bottom-start (black 55 %, radius 6); status badge top-end ("Neu" gold / "Frei" green). */
@Composable
fun MediaTile(f: ClientFile, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(R.Thumb)).background(C.Surface2).clickable(onClick = onClick)) {
        FileCover(f, Modifier.fillMaxSize())
        if (f.type == "Video") {
            Row(
                Modifier.align(Alignment.BottomStart).padding(6.dp).height(20.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) { Icon(Ic.Play, null, tint = Color.White, modifier = Modifier.size(9.dp)) }
        }
        when (f.status) {
            "ready_for_review" -> Text(
                de.focusontop.client.i18n.tr("Neu", "New", "جديد"), color = C.OnAccent, style = T.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).height(22.dp).clip(CircleShape).background(C.Gold).padding(horizontal = 7.dp, vertical = 3.dp),
            )
            "approved", "delivered", "published" -> Row(
                Modifier.align(Alignment.TopEnd).padding(6.dp).height(22.dp).clip(CircleShape).background(C.Green).padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(Ic.CheckBold, null, tint = C.OnAccent, modifier = Modifier.size(10.dp))
                Text(de.focusontop.client.i18n.tr("Frei", "Approved", "موافق"), color = C.OnAccent, style = T.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            "revision_requested" -> Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp).clip(CircleShape).background(C.Red))
        }
    }
}

/** Filled status chip (gold). */
@Composable
fun GoldPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = C.OnAccent, style = T.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1,
        modifier = modifier.clip(CircleShape).background(C.Gold).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Tinted status chip (Figma "Chip": tint fill, coloured label). */
@Composable
fun OutlinePill(text: String, color: Color = C.Gold) {
    val fg = if (color == C.Gold) C.GoldLabel else color
    Text(
        text, color = fg, style = T.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Section header (Headline semibold) with an optional trailing link in gold. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = C.Ink, style = T.Headline, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = C.Muted, style = T.Footnote)
    }
}

/** Form section label. */
@Composable
fun StepLabel(n: Int, text: String) {
    Text(text, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold)
}

/** Profile shortcut (Figma "Profile shortcut"): icon 22 above a caption label, no container. */
@Composable
fun CircleAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(R.Field)).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = C.Ink, modifier = Modifier.size(22.dp))
        Text(label, color = C.Ink, style = T.Caption, maxLines = 1)
    }
}

/** Selectable chip (Figma "Chip"/"Filter"): selected = gold tint + gold hairline, else glass. */
@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, strike: Boolean = false, height: Dp = 30.dp) {
    Box(
        modifier.height(height).clip(CircleShape)
            .background(if (selected) C.GoldTint else C.Glass)
            .border(1.dp, if (selected) C.Gold else C.StrokeGlass, CircleShape)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, color = when { selected -> C.GoldLabel; !enabled -> C.Faint; else -> C.Ink }, style = T.Caption, maxLines = 1,
            textDecoration = if (strike) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
        )
    }
}


/** Header icon (Figma page header): 22 pt Lucide icon, label/primary; optional red dot or count badge. */
@Composable
fun HeaderIcon(icon: ImageVector, desc: String?, dot: Boolean = false, badge: Int = 0, tint: Color = C.Ink, onClick: () -> Unit) {
    Box(
        Modifier.size(22.dp).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.fillMaxSize())
        if (badge > 0) Box(
            Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-7).dp).height(16.dp).widthIn(min = 16.dp).clip(CircleShape).background(C.Red).padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) { Text(if (badge > 99) "99+" else badge.toString(), color = Color.White, style = T.Micro, fontWeight = FontWeight.SemiBold) }
        else if (dot) Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp).size(8.dp).clip(CircleShape).background(C.Red).border(1.5.dp, C.Bg, CircleShape))
    }
}


/** Figma "Toggle": 42 × 26, radius 13, padding 4, thumb 18 — off = bg/tertiary + label/secondary thumb, on = gold + bg/primary thumb. */
@Composable
fun FtToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val x by androidx.compose.animation.core.animateDpAsState(if (checked) 16.dp else 0.dp, label = "toggle")
    Box(
        Modifier.size(42.dp, 26.dp).clip(RoundedCornerShape(13.dp)).background(if (checked) C.Gold else C.Surface2)
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onChange(!checked) }
            .padding(4.dp),
    ) { Box(Modifier.offset(x = x).size(18.dp).clip(CircleShape).background(if (checked) C.Bg else C.Muted)) }
}
