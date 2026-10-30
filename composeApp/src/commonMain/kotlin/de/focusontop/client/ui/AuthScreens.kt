package de.focusontop.client.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.focusontop.client.data.ApiError
import de.focusontop.client.data.AppState
import de.focusontop.client.data.ClientConfig
import de.focusontop.client.data.Media
import de.focusontop.client.data.Screen
import de.focusontop.client.i18n.Lang
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.i18n.Strings
import de.focusontop.client.i18n.tr
import de.focusontop.client.platform.appleSignInAvailable
import de.focusontop.client.platform.appleSignInToken
import de.focusontop.client.platform.googleIdToken
import de.focusontop.client.platform.googleSignInAvailable
import de.focusontop.client.platform.openUrl
import de.focusontop.client.platform.platformName
import de.focusontop.client.res.Res
import de.focusontop.client.res.logo_white
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/* ───────────────────────── shared pieces (Figma auth frames) ───────────────────────── */

/** Figma "Content": padding 24 / 16 / 40 / 16, gap 24, below the status bar. */
@Composable
private fun AuthContent(modifier: Modifier = Modifier, gap: Dp = 24.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

/** "FT Logo" wordmark, label/primary. Native size 164 × 48. */
@Composable
fun Wordmark(height: Dp) {
    Image(
        painterResource(Res.drawable.logo_white), "Focus On Top",
        Modifier.height(height).width(height * (164f / 48f)),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(C.Ink),
    )
}

/** "Focus On Top brand": wordmark 48 + descriptor Micro, gap 12. */
@Composable
private fun BrandBlock() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Wordmark(48.dp)
        Text(tr("FOTO · VIDEO · SOCIAL MEDIA", "PHOTO · VIDEO · SOCIAL MEDIA", "تصوير · فيديو · سوشيال ميديا"), color = C.Muted, style = T.Micro)
    }
}

/** Logo 22 + language filter — Figma "Action row", space-between, 30 high. */
@Composable
private fun BrandRow(state: AppState, strongChip: Boolean) {
    Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Wordmark(22.dp)
        LanguageSwitch(state, strong = strongChip)
    }
}

/** Language "Filter" (30 high, padding 7/10, Caption) → menu with the three languages. */
@Composable
fun LanguageSwitch(state: AppState, strong: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.height(30.dp).clip(R.Pill).background(if (strong) C.GlassStrong else C.Glass)
                .border(1.dp, C.StrokeGlass, R.Pill).clickable { open = true }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(state.lang.label, color = C.Ink, style = T.Caption, maxLines = 1)
            Icon(Ic.ChevronDownSm, null, tint = C.Ink, modifier = Modifier.size(12.dp))
        }
        // Figma A · "Language dropdown": 190 wide, bg/secondary + separator, radius 16, padding 16, gap 16, Body
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = C.Surface,
            shape = RoundedCornerShape(R.Menu), border = androidx.compose.foundation.BorderStroke(1.dp, C.Separator)) {
            Column(Modifier.width(190.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Lang.entries.forEach { l ->
                    val on = state.lang == l
                    Row(
                        Modifier.fillMaxWidth().clickable { state.setLanguage(l); open = false }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(l.label, color = if (on) C.GoldLabel else C.Ink, style = T.Body, modifier = Modifier.weight(1f))
                        if (on) Icon(Ic.Check, null, tint = C.Ink, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/** Figma "Action row" space-between: supporting text fills, accent label at the end. */
@Composable
private fun PromptRow(lead: String, accent: String, style: androidx.compose.ui.text.TextStyle, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(lead, color = C.Muted, style = style, modifier = Modifier.weight(1f))
        Text(accent, color = C.GoldLabel, style = style, modifier = Modifier.clickable(onClick = onClick))
    }
}

@Composable
private fun AuthError(text: String?) {
    if (text != null) Text(text, color = C.Red, style = T.Footnote)
}

/** Divider · "oder" · divider (gap 12, Caption). */
@Composable
private fun OrDivider(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(C.Separator))
        Text(text, color = C.Muted, style = T.Caption, textAlign = TextAlign.Center)
        Box(Modifier.weight(1f).height(1.dp).background(C.Separator))
    }
}

/** Two 3 pt step bars, gap 8, radius 1. */
@Composable
private fun StepBars(step: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(2) { i ->
            Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(1.dp)).background(if (i <= step) C.Gold else C.Surface2))
        }
    }
}

/** Figma "Card": bg/secondary, glass hairline, radius 20, padding 16, gap 16. */
@Composable
private fun AuthCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface).border(1.dp, C.StrokeGlass, R.CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

/* ───────────────────────── 01 · Splash ───────────────────────── */

@Composable
fun SplashScreen() {
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()
        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
        ) {
            BrandBlock()
            Text(LocalStrings.current.tagline, color = C.Muted, style = T.Body, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            CircularProgressIndicator(Modifier.size(24.dp), color = C.Gold, strokeWidth = 2.dp, trackColor = Color.Transparent)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
    }
}

/* ───────────────────────── 02 · Willkommen ───────────────────────── */

@Composable
private fun FilterLabel(text: String, selected: Boolean) {
    Text(
        text, color = if (selected) C.GoldLabel else C.Ink, style = T.Caption, maxLines = 1,
        modifier = Modifier.height(30.dp).clip(R.Pill).background(if (selected) C.GoldTint else C.Glass)
            .border(1.dp, if (selected) C.Gold else C.StrokeGlass, R.Pill).padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
fun WelcomeScreen(state: AppState) {
    val s = LocalStrings.current
    Box(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        AuthContent {
            BrandRow(state, strongChip = false)
            // "Welcome artwork": 232 high, padding 24, card centered
            Box(Modifier.fillMaxWidth().height(232.dp).padding(24.dp), contentAlignment = Alignment.Center) {
                AuthCard {
                    Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Ic.Clapper, null, tint = C.Ink, modifier = Modifier.size(30.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.width(144.dp).height(4.dp).background(C.Surface2))
                            Box(Modifier.width(88.dp).height(4.dp).background(C.Surface2))
                        }
                        Icon(Ic.CircleCheck, null, tint = C.Turquoise, modifier = Modifier.size(22.dp))
                    }
                    Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterLabel(s.approve, true)
                        FilterLabel(tr("Änderung", "Change", "تعديل"), false)
                    }
                }
            }
            Text(s.welcomeTitle, color = C.Ink, style = T.LargeTitle, modifier = Modifier.fillMaxWidth())
            Text(s.welcomeBody, color = C.Muted, style = T.Body, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Ic.BellRing, null, tint = C.Ink, modifier = Modifier.size(18.dp))
                Text(s.welcomePoint1, color = C.Ink, style = T.Footnote, modifier = Modifier.weight(1f))
            }
            PrimaryButton(s.signIn, onClick = { state.go(Screen.Onboarding) })
            PromptRow(s.noAccount, s.contactUs, T.Footnote) { openUrl("https://www.focusontop.de") }
        }
    }
}

private fun loginError(s: Strings, e: ApiError): String = when {
    e.status == 0 -> s.networkError
    e.status == 401 -> s.wrongLogin
    e.code == "client_only" -> s.staffNotAllowed
    e.code == "pending" -> s.accountPending
    e.status == 403 && (e.message ?: "").let { it.contains("مراجعة") || it.contains("pending", true) || it.contains("Prüfung", true) || it.contains("geprüft", true) } -> s.accountPending
    e.status == 403 -> s.accountBlocked
    else -> e.message ?: s.loadError
}

/* ───────────────────────── 03 · Anmelden  +  04 · Bestätigungscode ───────────────────────── */

@Composable
fun LoginScreen(state: AppState) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var twoFaUser by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var config by remember { mutableStateOf<ClientConfig?>(null) }
    LaunchedEffect(Unit) { if (googleSignInAvailable || appleSignInAvailable) config = runCatching { state.api.clientConfig() }.getOrNull() }

    fun google() {
        val cfg = config ?: return
        val clientId = if (platformName == "ios") cfg.googleIosClientId ?: return else cfg.googleWebClientId ?: return
        error = null; busy = true
        scope.launch {
            try {
                val googleToken = googleIdToken(clientId)
                if (googleToken != null) {
                    val resp = state.api.loginWithGoogle(googleToken)
                    if (resp.token == null || !state.onLoggedIn(resp)) error = s.staffNotAllowed
                }
            } catch (e: ApiError) {
                error = if (e.code == "google" || e.code == "google_not_configured" || e.status == 400) s.googleFailed else loginError(s, e)
            } catch (e: Exception) {
                error = s.googleFailed
            } finally { busy = false }
        }
    }

    fun apple() {
        error = null; busy = true
        scope.launch {
            try {
                val appleToken = appleSignInToken()
                if (appleToken != null) {
                    val resp = state.api.loginWithApple(appleToken)
                    if (resp.token == null || !state.onLoggedIn(resp)) error = s.staffNotAllowed
                }
            } catch (e: ApiError) { error = loginError(s, e) }
            catch (e: Exception) { error = s.loadError }
            finally { busy = false }
        }
    }

    fun submit() {
        error = null; busy = true
        scope.launch {
            try {
                val resp = if (twoFaUser != null) state.api.verify2fa(twoFaUser!!, code) else state.api.login(email, password)
                if (resp.requires2FA == true && resp.userId != null) {
                    twoFaUser = resp.userId
                } else if (resp.token != null) {
                    if (!state.onLoggedIn(resp)) error = s.staffNotAllowed
                } else error = s.wrongLogin
            } catch (e: ApiError) { error = loginError(s, e) }
            finally { busy = false }
        }
    }

    if (twoFaUser != null) {
        Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
            TopBar(s.twoFaTitle, onBack = { twoFaUser = null; code = ""; error = null })
            AuthContent {
                Box(Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                    Icon(Ic.ShieldCheck, null, tint = C.GoldLabel, modifier = Modifier.size(48.dp))
                }
                Text(s.twoFaTitle, color = C.Ink, style = T.Title1, modifier = Modifier.fillMaxWidth())
                Text(s.twoFaHint, color = C.Muted, style = T.Body, modifier = Modifier.fillMaxWidth())
                CodeEntry(code) { code = it.filter(Char::isDigit).take(6) }
                AuthError(error)
                PrimaryButton(s.verify, onClick = ::submit, loading = busy, enabled = code.length == 6)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        AuthContent {
            BrandRow(state, strongChip = true)
            Column(Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(s.hello, color = C.Ink, style = T.Title1)
                Text(s.loginHint, color = C.Muted, style = T.BodyS)
            }
            FtField(s.email, email, { email = it }, placeholder = "name@firma.de", keyboard = KeyboardType.Email, trailing = Ic.Mail)
            FtField(s.password, password, { password = it }, placeholder = "••••••••", password = true)
            Text(s.forgotPassword, color = C.GoldLabel, style = T.Footnote,
                modifier = Modifier.fillMaxWidth().clickable { state.go(Screen.Forgot) })
            AuthError(error)
            PrimaryButton(s.login, onClick = ::submit, loading = busy, enabled = email.isNotBlank() && password.isNotBlank())
            if (config?.googleSignIn == true) {
                OrDivider(s.orDivider)
                SecondaryButton(s.continueWithGoogle, onClick = { if (!busy) google() }, modifier = Modifier.fillMaxWidth(), icon = Ic.Chrome)
                if (appleSignInAvailable) SecondaryButton(s.continueWithApple, onClick = { if (!busy) apple() }, modifier = Modifier.fillMaxWidth())
            }
            PromptRow(tr("Noch kein Konto?", "No account yet?", "ليس لديك حساب؟"), tr("Konto erstellen", "Create account", "إنشاء حساب"), T.Caption) {
                state.go(Screen.Register)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
            Text(s.clientsOnly, color = C.Muted, style = T.Caption, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth()) {
                Text(s.privacy, color = C.Muted, style = T.Micro, modifier = Modifier.weight(1f).clickable { openUrl(Media.PRIVACY) })
                Text(tr("Mitarbeiter-Login", "Staff login", "دخول الموظفين"), color = C.Muted, style = T.Micro,
                    modifier = Modifier.weight(1f).clickable { openUrl("https://focusontop.de") })
            }
        }
    }
}

/** Six digit boxes 53 × 58, gap 8, radius 14; the active box has a gold 1 pt stroke. */
@Composable
private fun CodeEntry(code: String, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(Modifier.fillMaxWidth().height(58.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) { i ->
                val ch = code.getOrNull(i)
                val active = focused && (i == code.length || (code.length == 6 && i == 5))
                Box(
                    Modifier.weight(1f).height(58.dp).clip(R.FieldShape).background(C.Glass)
                        .border(1.dp, if (active) C.Gold else C.StrokeGlass, R.FieldShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        ch?.toString() ?: if (active) "│" else "–",
                        color = if (ch != null) C.Ink else C.Muted, style = T.Title2,
                        fontWeight = if (ch != null) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center,
                    )
                }
            }
        }
        BasicTextField(
            value = code, onValueChange = onChange,
            modifier = Modifier.matchParentSize().alpha(0f).focusRequester(focus).onFocusChanged { focused = it.isFocused },
            singleLine = true, cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
    }
}

/* ───────────────────────── 05 · Passwort zurücksetzen ───────────────────────── */

@Composable
fun ForgotScreen(state: AppState) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopBar(s.resetTitle, onBack = { state.back() })
        AuthContent {
            Text(s.resetTitle, color = C.Ink, style = T.Title1, modifier = Modifier.fillMaxWidth())
            Text(s.resetHint, color = C.Muted, style = T.Body, modifier = Modifier.fillMaxWidth())
            FtField(s.email, email, { email = it }, placeholder = "name@firma.de", keyboard = KeyboardType.Email, trailing = Ic.Mail)
            AuthError(error)
            PrimaryButton(s.sendLink, loading = busy, enabled = email.contains("@"), onClick = {
                busy = true; error = null
                scope.launch {
                    try { state.api.forgotPassword(email); done = true }
                    catch (e: ApiError) { error = if (e.status == 0) s.networkError else s.loadError }
                    finally { busy = false }
                }
            })
            if (done) AuthCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Ic.CircleCheck, null, tint = C.Green, modifier = Modifier.size(22.dp))
                    Text(s.resetSent, color = C.Muted, style = T.Footnote, modifier = Modifier.weight(1f))
                }
            }
            SecondaryButton(s.back, onClick = { state.back() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/* ───────────────────────── 06 · 07 · 08 · Registrierung ───────────────────────── */

private val COUNTRY_CODES = listOf("DE" to "+49", "AT" to "+43", "CH" to "+41", "NL" to "+31", "BE" to "+32", "FR" to "+33", "IT" to "+39", "PL" to "+48", "TR" to "+90", "SY" to "+963", "LB" to "+961", "AE" to "+971", "SA" to "+966", "GB" to "+44", "US" to "+1")

@Composable
private fun CountryCode(selected: Pair<String, String>, onPick: (Pair<String, String>) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.clickable { open = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${selected.first} ${selected.second}", color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Icon(Ic.ChevronDownSm, null, tint = C.Muted, modifier = Modifier.size(14.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(C.Surface)) {
            COUNTRY_CODES.forEach { cc ->
                Text(
                    "${cc.first}  ${cc.second}", color = if (cc == selected) C.GoldLabel else C.Ink, style = T.BodyS,
                    modifier = Modifier.width(160.dp).clickable { onPick(cc); open = false }.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(state: AppState) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(COUNTRY_CODES.first()) }
    var company by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf(0) }   // 0 = data, 1 = account type, 2 = pending
    var privateAccount by remember { mutableStateOf(true) }
    val errExists = tr("Diese E-Mail ist bereits registriert.", "This e-mail is already registered.", "هذا البريد مسجل بالفعل.")
    val errNet = tr("Keine Internetverbindung.", "No internet connection.", "لا يوجد اتصال بالإنترنت.")
    val errFail = tr("Registrierung fehlgeschlagen.", "Registration failed.", "تعذر إنشاء الحساب.")
    val createTitle = tr("Konto erstellen", "Create account", "إنشاء حساب")
    val typeTitle = tr("Kontotyp wählen", "Choose account type", "اختر نوع الحساب")

    fun submit() {
        error = null; busy = true
        scope.launch {
            try {
                val fullPhone = phone.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.let { "${country.second} $it" }
                state.api.register(name, email, password, company, privateAccount, fullPhone)
                step = 2
            } catch (e: ApiError) {
                error = when {
                    e.status == 400 && (e.message ?: "").contains("exists", true) -> errExists
                    e.status == 0 -> errNet
                    else -> e.message ?: errFail
                }
            } catch (e: Exception) { error = errFail }
            finally { busy = false }
        }
    }

    if (step == 2) {
        Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
            AuthContent {
                Column(
                    Modifier.fillMaxWidth().padding(top = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp),
                ) {
                    BrandBlock()
                    Box(
                        Modifier.size(96.dp).clip(R.CardShape).background(C.Surface).border(1.dp, C.Separator, R.CardShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Ic.Hourglass, null, tint = C.GoldLabel, modifier = Modifier.size(40.dp)) }
                    Text(
                        tr("Danke! Dein Konto wartet auf die Freigabe durch unser Team.", "Thank you! Your account is waiting for approval by our team.", "شكراً! حسابك بانتظار موافقة فريقنا."),
                        color = C.Ink, style = T.Title1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                    PrimaryButton(tr("Zurück zur Anmeldung", "Back to sign in", "العودة لتسجيل الدخول"), onClick = { state.back() })
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopBar(if (step == 0) createTitle else typeTitle, onBack = { if (step == 1) step = 0 else state.back() })
        AuthContent {
            StepBars(step)
            if (step == 0) {
                Text(createTitle, color = C.Ink, style = T.Title1, modifier = Modifier.fillMaxWidth())
                Text(tr("Erstelle dein Kundenkonto – unser Team prüft und aktiviert es.", "Create your client account – our team reviews and activates it.", "أنشئ حساب العميل — يراجعه فريقنا ويُفعّله."),
                    color = C.Muted, style = T.BodyS, modifier = Modifier.fillMaxWidth())
                FtField(tr("Name", "Name", "الاسم"), name, { name = it }, placeholder = tr("Name", "Name", "الاسم"), trailing = Ic.UserRound)
                FtField(tr("E-Mail", "E-mail", "البريد الإلكتروني"), email, { email = it }, placeholder = "name@firma.de", keyboard = KeyboardType.Email, trailing = Ic.Mail)
                FtField(
                    tr("Telefonnummer", "Phone number", "رقم الهاتف"), phone, { phone = it.filter { c -> c.isDigit() || c == ' ' }.take(18) },
                    placeholder = "176 12345678", keyboard = KeyboardType.Phone, trailing = Ic.Phone,
                    leading = {
                        CountryCode(country) { country = it }
                        Box(Modifier.width(1.dp).height(24.dp).background(C.Separator))
                    },
                )
                FtField(tr("Firma (optional)", "Company (optional)", "الشركة (اختياري)"), company, { company = it }, placeholder = tr("Firma", "Company", "الشركة"), trailing = Ic.Building)
                FtField(tr("Passwort", "Password", "كلمة المرور"), password, { password = it }, placeholder = "••••••••", password = true)
                AuthError(error)
                PrimaryButton(
                    tr("Weiter", "Continue", "متابعة"), onClick = { error = null; step = 1 },
                    enabled = name.isNotBlank() && email.contains("@") && password.length >= 6,
                )
                Text(
                    tr("Bereits ein Konto? Anmelden", "Already have an account? Sign in", "لديك حساب؟ تسجيل الدخول"),
                    color = C.GoldLabel, style = T.Footnote, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { state.back() },
                )
            } else {
                Text(typeTitle, color = C.Ink, style = T.Title1, modifier = Modifier.fillMaxWidth())
                Text(tr("Du kannst das später jederzeit in den Einstellungen ändern.", "You can change this anytime later in the settings.", "يمكنك تغيير ذلك لاحقاً من الإعدادات."),
                    color = C.Muted, style = T.BodyS, modifier = Modifier.fillMaxWidth())
                AccountTypeCard(
                    Ic.Globe, tr("Öffentliches Konto", "Public account", "حساب عام"),
                    tr("Dein Profil und deine Beiträge sind für alle Nutzer sichtbar — jeder kann dir folgen und deine Videos kommentieren.", "Your profile and posts are visible to all users — anyone can follow you and comment on your videos.", "ملفك ومنشوراتك تظهر لكل مستخدمي التطبيق — يمكن لأي شخص متابعتك والتعليق على فيديوهاتك."),
                    selected = !privateAccount,
                ) { privateAccount = false }
                AccountTypeCard(
                    Ic.LockKeyhole, tr("Privates Konto", "Private account", "حساب خاص"),
                    tr("Nur du und bestätigte Follower sehen deine Beiträge. Du entscheidest, wer dir folgen darf.", "Only you and approved followers see your posts. You decide who may follow you.", "أنت ومن توافق على متابعتهم فقط ترون منشوراتك. أنت تتحكم بمن يتابعك."),
                    selected = privateAccount,
                ) { privateAccount = true }
                AuthError(error)
                PrimaryButton(createTitle, onClick = ::submit, loading = busy)
            }
        }
    }
}

@Composable
private fun AccountTypeCard(icon: ImageVector, title: String, body: String, selected: Boolean, onClick: () -> Unit) {
    AuthCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = if (selected) C.Ink else C.Muted, modifier = Modifier.size(22.dp))
            Text(title, color = if (selected) C.GoldLabel else C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(if (selected) Ic.CircleCheck else Ic.Circle, null, tint = if (selected) C.Ink else C.Muted, modifier = Modifier.size(22.dp))
        }
        Text(body, color = C.Muted, style = T.BodyS)
    }
}


/* ───────────── 02b · Onboarding (3 pages) ───────────── */

@Composable
fun OnboardingScreen(state: AppState) {
    var page by remember { mutableStateOf(0) }
    val pages = listOf(
        Triple(Ic.FolderCheck, tr("Alle Projekte an einem Ort", "All projects in one place", "كل مشاريعك في مكان واحد"),
            tr("Verfolge jeden Schritt – vom Briefing bis zur fertigen Lieferung.", "Follow every step – from briefing to final delivery.", "تابع كل خطوة — من الموجز حتى التسليم النهائي.")),
        Triple(Ic.Clapper175, tr("Videos prüfen und freigeben", "Review and approve videos", "راجع الفيديوهات ووافق عليها"),
            tr("Setze Anmerkungen direkt an die richtige Sekunde im Video.", "Leave notes right at the exact second of the video.", "ضع ملاحظاتك عند الثانية المناسبة في الفيديو.")),
        Triple(Ic.Share, tr("Überall teilen", "Share everywhere", "شارك في كل مكان"),
            tr("Verbinde Instagram, TikTok und Facebook und poste mit einem Tipp.", "Connect Instagram, TikTok and Facebook and post with one tap.", "اربط إنستغرام وتيك توك وفيسبوك وانشر بلمسة واحدة.")),
    )
    fun finish() { state.markWelcomed(); state.go(Screen.PrivacyConsent) }
    val (icon, title, body) = pages[page]
    Box(Modifier.fillMaxSize().background(C.Bg)) {
        // hero 390 × 520 with a fade into bg/primary from 280
        Box(Modifier.fillMaxWidth().height(520.dp).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(C.Surface2, C.Surface, C.Bg))), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = C.GoldLabel, modifier = Modifier.size(96.dp))
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(240.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, C.Bg))))
        }
        Text(tr("Überspringen", "Skip", "تخطي"), color = Color.White, style = T.Footnote, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 10.dp, end = 20.dp).clickable { finish() })
        Column(Modifier.align(Alignment.TopStart).padding(start = 20.dp, end = 20.dp, top = 520.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i -> Box(Modifier.width(if (i == page) 22.dp else 8.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (i == page) C.Gold else C.Surface2)) }
            }
            Text(title, color = C.Ink, style = T.Title1)
            Text(body, color = C.Muted, style = T.Body)
        }
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 34.dp)) {
            Box(Modifier.fillMaxWidth().height(50.dp).clip(R.Pill).background(C.Gold).clickable { if (page < 2) page++ else finish() }, contentAlignment = Alignment.Center) {
                Text(if (page < 2) tr("Weiter", "Next", "التالي") else tr("Los geht’s", "Let’s go", "لنبدأ"), color = C.OnAccent, style = T.Body, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


/* ───────────── 02c · Einstieg · Datenschutz (Consent sheet) ───────────── */
@Composable
fun PrivacyConsentScreen(state: AppState) {
    var analytics by remember { mutableStateOf(false) }
    var personalization by remember { mutableStateOf(false) }
    fun proceed() {
        de.focusontop.client.platform.Prefs.set("consent_analytics", if (analytics) "1" else "0")
        de.focusontop.client.platform.Prefs.set("consent_personalization", if (personalization) "1" else "0")
        de.focusontop.client.platform.Prefs.set("consent_done", "1")
        state.go(Screen.NotificationsPermission)
    }
    Box(Modifier.fillMaxSize().background(C.Bg), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().clip(R.SheetShape).background(C.Surface).navigationBarsPadding().statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.width(36.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(C.Separator))
            Box(Modifier.align(Alignment.Start).padding(top = 10.dp).size(48.dp).clip(RoundedCornerShape(14.dp)).background(C.GoldTint), contentAlignment = Alignment.Center) {
                Icon(Ic.ShieldCheck, null, tint = C.GoldLabel, modifier = Modifier.size(24.dp))
            }
            Text(tr("Deine Privatsphäre", "Your privacy", "خصوصيتك"), color = C.Ink, style = T.Title2, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
            Text(
                tr("Wir nutzen nur, was nötig ist. Analyse und Personalisierung bleiben aus, bis du zustimmst. Du kannst das jederzeit in den Einstellungen ändern.",
                    "We only use what's necessary. Analytics and personalization stay off until you agree. You can change this anytime in Settings.",
                    "نستخدم الضروري فقط. التحليل والتخصيص متوقفان حتى توافق، ويمكنك تغيير ذلك في أي وقت من الإعدادات."),
                color = C.Muted, style = T.BodyS, modifier = Modifier.align(Alignment.Start),
            )
            Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                ConsentRow(tr("Notwendig", "Necessary", "ضروري"), tr("Anmeldung, Sicherheit, Uploads", "Sign-in, security, uploads", "تسجيل الدخول والأمان والرفع")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Ic.CheckCircle, null, tint = C.Green, modifier = Modifier.size(14.dp))
                        Text(tr("Immer an", "Always on", "دائمًا"), color = C.Muted, style = T.Caption)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
                ConsentRow(tr("Analyse", "Analytics", "التحليل"), tr("Hilft uns, die App zu verbessern", "Helps us improve the app", "يساعدنا على تحسين التطبيق")) {
                    FtToggle(analytics) { analytics = it }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
                ConsentRow(tr("Personalisierung", "Personalization", "التخصيص"), tr("Passende Vorschläge im Feed", "Relevant suggestions in the feed", "مقترحات مناسبة في الصفحة الرئيسية")) {
                    FtToggle(personalization) { personalization = it }
                }
            }
            Spacer(Modifier.height(2.dp))
            PrimaryButton(tr("Alle akzeptieren", "Accept all", "قبول الكل"), modifier = Modifier.fillMaxWidth(), onClick = {
                analytics = true; personalization = true; proceed()
            })
            SecondaryButton(tr("Auswahl speichern", "Save selection", "حفظ الاختيار"), modifier = Modifier.fillMaxWidth(), onClick = { proceed() })
            Text(tr("Datenschutzerklärung · Impressum", "Privacy policy · Imprint", "سياسة الخصوصية · بيانات الناشر"), color = C.Muted, style = T.Caption,
                modifier = Modifier.clickable { de.focusontop.client.platform.openUrl(de.focusontop.client.data.Media.PRIVACY) })
        }
    }
}

@Composable
private fun ConsentRow(title: String, sub: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = C.Ink, style = T.Body, fontWeight = FontWeight.SemiBold)
            Text(sub, color = C.Muted, style = T.Footnote)
        }
        trailing()
    }
}

/* ───────────── 02d · Einstieg · Mitteilungen erlauben ───────────── */
@Composable
fun NotificationsPermissionScreen(state: AppState) {
    fun proceed() { de.focusontop.client.platform.Prefs.set("notif_prompted", "1"); state.go(Screen.Login) }
    val benefits = listOf(
        tr("Neue Version zur Freigabe", "New version ready to approve", "نسخة جديدة بانتظار موافقتك"),
        tr("Termin bestätigt oder verschoben", "Appointment confirmed or moved", "تأكيد أو تغيير موعد"),
        tr("Antwort vom Team", "Reply from the team", "رد من الفريق"),
    )
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(90.dp))
        Box(Modifier.align(Alignment.CenterHorizontally).size(104.dp).clip(RoundedCornerShape(28.dp)).background(C.GoldTint), contentAlignment = Alignment.Center) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(C.Gold.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(Ic.BellRing, null, tint = C.GoldLabel, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(tr("Nichts Wichtiges verpassen", "Never miss what matters", "لا تفوّت شيئًا مهمًا"), color = C.Ink, style = T.Title2, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(tr("Wir melden uns nur, wenn es um dein Projekt geht – kein Werbe-Spam.", "We only reach out about your project – no promotional spam.", "نرسل إشعارات تخص مشروعك فقط، بدون إعلانات مزعجة."),
            color = C.Muted, style = T.Body, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().clip(R.CardShape).background(C.Surface)) {
            benefits.forEachIndexed { i, b ->
                Row(Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Ic.CircleCheck, null, tint = C.Gold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(b, color = C.Ink, style = T.BodyS)
                }
                if (i < benefits.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(C.Separator))
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(tr("Mitteilungen erlauben", "Allow notifications", "السماح بالإشعارات"), modifier = Modifier.fillMaxWidth(), onClick = {
            de.focusontop.client.platform.requestNotificationPermission(); proceed()
        })
        Spacer(Modifier.height(10.dp))
        SecondaryButton(tr("Später", "Later", "لاحقًا"), modifier = Modifier.fillMaxWidth(), onClick = { proceed() })
        Spacer(Modifier.height(20.dp))
    }
}
