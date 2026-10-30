package de.focusontop.client.ui

import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.res.Res
import de.focusontop.client.res.inter_bold
import de.focusontop.client.res.inter_regular
import de.focusontop.client.res.inter_semibold
import de.focusontop.client.res.arabic_bold
import de.focusontop.client.res.arabic_regular
import de.focusontop.client.res.arabic_semibold
import de.focusontop.client.i18n.Lang
import de.focusontop.client.i18n.LocalLang
import androidx.compose.ui.unit.em
import org.jetbrains.compose.resources.Font

/*
 * Focus On Top design tokens — mirrors the Figma variable collection "FOT · Tokens"
 * (modes Brand Dark / Brand Light). Names on the right are the Figma variable names.
 */
private data class Palette(
    val bg: Color,          // color/bg/primary
    val surface: Color,     // color/bg/secondary
    val surface2: Color,    // color/bg/tertiary
    val separator: Color,   // color/separator
    val gold: Color,        // color/accent/gold
    val goldPressed: Color, // color/accent/gold-pressed
    val goldLabel: Color,   // color/accent/gold-label
    val turquoise: Color,   // color/accent/turquoise
    val ink: Color,         // color/label/primary
    val muted: Color,       // color/label/secondary
    val faint: Color,       // color/label/tertiary
    val success: Color,     // color/status/success
    val danger: Color,      // color/status/danger
    val glass: Color,       // color/fill/glass
    val glassStrong: Color, // color/fill/glass-strong
    val strokeGlass: Color, // color/stroke/glass
    val navGlass: Color,    // color/nav/glass
    val scrim: Color,       // color/overlay/scrim
    val onAccent: Color,    // color/label/on-accent
    val goldTint: Color,    // color/accent/gold-tint
)

private val BrandDark = Palette(
    bg = Color(0xFF0D202C), surface = Color(0xFF112735), surface2 = Color(0xFF1B3445), separator = Color(0xFF203A4B),
    gold = Color(0xFFECB31F), goldPressed = Color(0xFFD49E12), goldLabel = Color(0xFFECB31F), turquoise = Color(0xFF4DBBCC),
    ink = Color(0xFFF7F8F8), muted = Color(0xFFA0A7AD), faint = Color(0xFF6E7A83),
    success = Color(0xFF4ADE80), danger = Color(0xFFF87171),
    glass = Color(0x0FF7F8F8), glassStrong = Color(0x1AF7F8F8), strokeGlass = Color(0x1AF7F8F8),
    navGlass = Color(0xC7112735), scrim = Color(0xB3030B12), onAccent = Color(0xFF112735), goldTint = Color(0x29ECB31F),
)

private val BrandLight = Palette(
    bg = Color(0xFFF7F8F8), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFECEEEF), separator = Color(0xFFE1E4E6),
    gold = Color(0xFFECB31F), goldPressed = Color(0xFFD49E12), goldLabel = Color(0xFFA87A05), turquoise = Color(0xFF2C9AAB),
    ink = Color(0xFF112735), muted = Color(0xFF6E7A83), faint = Color(0xFFA0A7AD),
    success = Color(0xFF1FA855), danger = Color(0xFFE5484D),
    glass = Color(0x0D112735), glassStrong = Color(0x14112735), strokeGlass = Color(0x14112735),
    navGlass = Color(0xC7FFFFFF), scrim = Color(0x73112735), onAccent = Color(0xFF112735), goldTint = Color(0x29ECB31F),
)

/** Current appearance. Brand Dark is the default; Settings → Darstellung switches it. */
object ThemeState { var dark by mutableStateOf(runCatching { de.focusontop.client.platform.Prefs.get("theme") != "light" }.getOrDefault(true)) }
private val p: Palette get() = if (ThemeState.dark) BrandDark else BrandLight

/** Colour tokens. Reads are snapshot-aware, so switching the theme recomposes every screen. */
object C {
    val Bg get() = p.bg
    val BgTop get() = p.bg
    val Surface get() = p.surface
    val Surface2 get() = p.surface2
    val Border get() = p.separator
    val Separator get() = p.separator
    val Gold get() = p.gold
    val GoldDeep get() = p.goldPressed
    val GoldLabel get() = p.goldLabel
    val GoldTint get() = p.goldTint
    val Ink get() = p.ink
    val Muted get() = p.muted
    val Faint get() = p.faint
    val Green get() = p.success
    val GreenText get() = p.success
    val Red get() = p.danger
    val Blue get() = p.turquoise
    val Turquoise get() = p.turquoise
    val Glass get() = p.glass
    val GlassStrong get() = p.glassStrong
    val StrokeGlass get() = p.strokeGlass
    val NavGlass get() = p.navGlass
    val Scrim get() = p.scrim
    val OnAccent get() = p.onAccent
}

/** Radius tokens (Figma radius variables). */
object R {
    val Card = 20.dp; val Field = 14.dp; val Menu = 16.dp; val Thumb = 10.dp; val Sheet = 34.dp
    val ThumbShape = RoundedCornerShape(10.dp)
    val Pill = RoundedCornerShape(999.dp)
    val CardShape = RoundedCornerShape(Card)
    val FieldShape = RoundedCornerShape(Field)
    val SheetShape = RoundedCornerShape(topStart = Sheet, topEnd = Sheet)
}

/** Flat brand background (gradients were removed in the redesign). */
fun Modifier.screenBg(): Modifier = this.background(C.Bg)

/** Kept for source compatibility — the primary action is a flat gold fill now. */
val GoldBrush: Brush get() = SolidColor(C.Gold)

/**
 * Figma uses Inter for Latin text styles and Noto Sans Arabic for Arabic ones.
 * The Arabic files are Noto Sans Arabic merged with Inter's Latin glyphs, so names
 * and brand words inside Arabic text render in Inter exactly like the Figma frames.
 */
@Composable
fun brandFamily(): FontFamily = if (LocalLang.current == Lang.AR) FontFamily(
    Font(Res.font.arabic_regular, FontWeight.Normal),
    Font(Res.font.arabic_regular, FontWeight.Medium),
    Font(Res.font.arabic_semibold, FontWeight.SemiBold),
    Font(Res.font.arabic_bold, FontWeight.Bold),
    Font(Res.font.arabic_bold, FontWeight.ExtraBold),
    Font(Res.font.arabic_bold, FontWeight.Black),
) else FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_regular, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
    Font(Res.font.inter_bold, FontWeight.ExtraBold),
    Font(Res.font.inter_bold, FontWeight.Black),
)

/** Kept for source compatibility with v1 screens. */
@Composable
fun lamaFamily(): FontFamily = brandFamily()

/**
 * Type scale — Figma text styles Latin/<step> and Arabic/<step>.
 * Latin: size / line height / tracking as in Figma. Arabic: line height = size × 1.6, tracking 0.
 */
object T {
    @Composable private fun s(size: Int, lh: Int, trackPct: Double, w: FontWeight = FontWeight.Normal): TextStyle {
        val ar = LocalLang.current == Lang.AR
        return TextStyle(
            fontFamily = brandFamily(),
            fontSize = size.sp,
            lineHeight = (if (ar) kotlin.math.round(size * 1.6).toInt() else lh).sp,
            letterSpacing = (if (ar) 0.0 else trackPct / 100.0).em,
            fontWeight = w,
        )
    }
    val LargeTitle: TextStyle @Composable get() = s(34, 40, -2.2, FontWeight.Bold)
    val Title1: TextStyle @Composable get() = s(28, 34, -2.0, FontWeight.Bold)
    val Title2: TextStyle @Composable get() = s(22, 28, -1.5, FontWeight.SemiBold)
    val Title3: TextStyle @Composable get() = s(20, 26, -1.2, FontWeight.SemiBold)
    val Headline: TextStyle @Composable get() = s(17, 22, -0.8, FontWeight.SemiBold)
    val Body: TextStyle @Composable get() = s(15, 20, -0.5)
    val BodyS: TextStyle @Composable get() = s(14, 20, -0.3)
    val Footnote: TextStyle @Composable get() = s(13, 18, 0.0)
    val Caption: TextStyle @Composable get() = s(12, 16, 0.0)
    val Micro: TextStyle @Composable get() = s(11, 13, 0.5)
}

@Composable
fun FtTheme(content: @Composable () -> Unit) {
    val lama = brandFamily()
    val type = Typography(
        displayLarge = T.LargeTitle.copy(fontFamily = lama),
        displayMedium = T.Title1.copy(fontFamily = lama),
        displaySmall = T.Title1.copy(fontFamily = lama),
        headlineLarge = T.Title1.copy(fontFamily = lama),
        headlineMedium = T.Title2.copy(fontFamily = lama),
        headlineSmall = T.Title3.copy(fontFamily = lama),
        titleLarge = T.Title3.copy(fontFamily = lama),
        titleMedium = T.Headline.copy(fontFamily = lama),
        titleSmall = T.Body.copy(fontFamily = lama, fontWeight = FontWeight.SemiBold),
        bodyLarge = T.Body.copy(fontFamily = lama),
        bodyMedium = T.BodyS.copy(fontFamily = lama),
        bodySmall = T.Footnote.copy(fontFamily = lama),
        labelLarge = T.Body.copy(fontFamily = lama, fontWeight = FontWeight.SemiBold),
        labelMedium = T.Footnote.copy(fontFamily = lama, fontWeight = FontWeight.SemiBold),
        labelSmall = T.Caption.copy(fontFamily = lama, fontWeight = FontWeight.SemiBold),
    )
    val scheme = if (ThemeState.dark) darkColorScheme(
        primary = C.Gold, onPrimary = C.OnAccent, background = C.Bg, onBackground = C.Ink,
        surface = C.Surface, onSurface = C.Ink, surfaceVariant = C.Surface2, onSurfaceVariant = C.Muted,
        outline = C.Separator, error = C.Red,
    ) else lightColorScheme(
        primary = C.Gold, onPrimary = C.OnAccent, background = C.Bg, onBackground = C.Ink,
        surface = C.Surface, onSurface = C.Ink, surfaceVariant = C.Surface2, onSurfaceVariant = C.Muted,
        outline = C.Separator, error = C.Red,
    )
    MaterialTheme(colorScheme = scheme, typography = type) {
        ProvideTextStyle(type.bodyLarge.copy(color = C.Ink), content)
    }
}

/** Primary action (Figma "Button"): gold capsule, 48 high, padding 12/16, label Body S semibold. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, icon: ImageVector? = null) {
    Row(
        modifier.fillMaxWidth().height(48.dp).clip(R.Pill).background(if (enabled) C.Gold else C.Surface2)
            .clickable(enabled = enabled && !loading, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (enabled) C.OnAccent else C.Faint
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = C.OnAccent, strokeWidth = 2.dp)
        else {
            if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Text(text, color = fg, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** Secondary action (Figma "Button" secondary): glass-strong capsule with glass hairline, 48 high. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = C.Ink, icon: ImageVector? = null) {
    Row(
        modifier.height(48.dp).clip(R.Pill).background(C.GlassStrong).border(1.dp, C.StrokeGlass, R.Pill)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, color = color, style = T.BodyS, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * Text field (Figma "Form field"): label Footnote semibold, gap 8; input 56 high, padding 16,
 * fill/glass + glass hairline, radius/field; text and placeholder Body S; trailing icon 19.
 */
@Composable
fun FtField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    trailing: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label.isNotEmpty()) Text(label, color = C.Ink, style = T.Footnote, fontWeight = FontWeight.SemiBold)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(R.FieldShape).background(C.GlassStrong)
                .border(1.dp, if (focused) C.Gold else C.StrokeGlass, R.FieldShape).padding(16.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leading != null) leading()
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, color = C.Muted, style = T.BodyS, maxLines = if (singleLine) 1 else Int.MAX_VALUE)
                androidx.compose.foundation.text.BasicTextField(
                    value = value, onValueChange = onChange,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                    textStyle = T.BodyS.copy(color = C.Ink),
                    singleLine = singleLine, minLines = minLines,
                    cursorBrush = SolidColor(C.Gold),
                    keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
                    visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                )
            }
            if (password) Icon(if (visible) Ic.EyeOff else Ic.Eye, LocalStrings.current.showPassword, tint = C.Muted,
                modifier = Modifier.size(19.dp).clickable { visible = !visible })
            else if (trailing != null) Icon(trailing, null, tint = C.Muted, modifier = Modifier.size(19.dp))
        }
    }
}

/** Page header: plain back chevron, centred headline (Figma "Page header"). */
/** Figma "Page header": 72 high, padding 16, back arrow 22, Title 3 centered, 1 pt separator at the bottom. */
@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}, leading: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().height(72.dp)) {
        Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                if (leading != null) leading()
                else if (onBack != null) Icon(Ic.Back, LocalStrings.current.back, tint = C.Ink,
                    modifier = Modifier.size(32.dp).clip(R.Pill).clickable(onClick = onBack).padding(5.dp))
            }
            Text(title, color = C.Ink, style = T.Title3, maxLines = 1, textAlign = TextAlign.Center,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                Row(Modifier.wrapContentWidth(align = Alignment.End, unbounded = true), verticalAlignment = Alignment.CenterVertically) { actions() }
            }
        }
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp).background(C.Separator))
    }
}

/** Status chip: pill, Caption, tinted fill. */
@Composable
fun Chip(text: String, fg: Color, bg: Color = fg.copy(alpha = 0.16f)) {
    Text(
        text, color = fg, style = T.Caption,
        modifier = Modifier.clip(R.Pill).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Card surface (Figma "Card"): bg/secondary, radius/card, no border. */
@Composable
fun Panel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
    ) { content() }
}

/** Filter chip (Figma "Filter"): 30 high, padding 7/10, Caption; glass + hairline, selected = gold tint + gold 1 pt. */
@Composable
fun SelectPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) C.GoldLabel else C.Ink,
        style = T.Caption,
        maxLines = 1,
        modifier = Modifier.height(30.dp).clip(R.Pill)
            .background(if (selected) C.GoldTint else C.Glass)
            .border(1.dp, if (selected) C.Gold else C.StrokeGlass, R.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
fun EmptyState(title: String, body: String? = null, icon: ImageVector? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Box(Modifier.size(64.dp).clip(R.Pill).background(C.GlassStrong), contentAlignment = Alignment.Center) { Icon(icon, null, tint = C.Muted, modifier = Modifier.size(28.dp)) }
        Text(title, color = C.Ink, style = T.Headline, textAlign = TextAlign.Center)
        if (body != null) Text(body, color = C.Muted, style = T.BodyS, textAlign = TextAlign.Center)
    }
}

@Composable
fun ErrorBanner(text: String, onRetry: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(R.CardShape).background(C.Red.copy(alpha = 0.14f)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = C.Ink, style = T.Footnote, modifier = Modifier.weight(1f))
        Text(LocalStrings.current.retry, color = C.GoldLabel, style = T.Footnote, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onRetry).padding(6.dp))
    }
}

/** Hairline divider (color/separator). */
@Composable
fun Hairline(modifier: Modifier = Modifier) { Box(modifier.fillMaxWidth().height(1.dp).background(C.Separator)) }

/** Bottom-sheet container (Figma sheets): top radius 34, handle 36×5. */
@Composable
fun SheetSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().clip(R.SheetShape).background(C.Surface).padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 36.dp, height = 5.dp).clip(R.Pill).background(C.Separator))
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** Square-ish icon container used in lists (Figma "Icon tile"). */
@Composable
fun TintTile(icon: ImageVector, size: Dp = 40.dp, tint: Color = C.GoldLabel, bg: Color = C.GoldTint) {
    Box(Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}
