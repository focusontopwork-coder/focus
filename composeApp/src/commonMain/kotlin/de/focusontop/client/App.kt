package de.focusontop.client

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import de.focusontop.client.data.AppState
import de.focusontop.client.data.Screen
import de.focusontop.client.i18n.LocalLang
import de.focusontop.client.i18n.LocalStrings
import de.focusontop.client.i18n.stringsFor
import de.focusontop.client.platform.PlatformBackHandler
import de.focusontop.client.ui.C
import de.focusontop.client.ui.NewProjectScreen
import de.focusontop.client.ui.PlansScreen
import de.focusontop.client.ui.PlanCompareScreen
import de.focusontop.client.ui.OnboardingScreen
import de.focusontop.client.ui.PrivacyConsentScreen
import de.focusontop.client.ui.NotificationsPermissionScreen
import de.focusontop.client.ui.LegalScreen
import de.focusontop.client.ui.EditProfileScreen
import de.focusontop.client.ui.FilesPage
import de.focusontop.client.ui.ProjectDetailScreen
import de.focusontop.client.ui.DeliveryScreen
import de.focusontop.client.ui.VersionCompareScreen
import de.focusontop.client.ui.AppointmentRequestScreen
import de.focusontop.client.ui.ChatScreen
import de.focusontop.client.ui.InvoicesScreen
import de.focusontop.client.ui.MessagesScreen
import de.focusontop.client.ui.ProjectsScreen
import de.focusontop.client.ui.RegisterScreen
import de.focusontop.client.ui.SettingsScreen
import de.focusontop.client.ui.ViewerScreen
import de.focusontop.client.ui.StoriesScreen
import de.focusontop.client.ui.ComposeScreen
import de.focusontop.client.ui.PostScreen
import de.focusontop.client.ui.ClientProfileScreen
import de.focusontop.client.ui.DiscoverScreen
import de.focusontop.client.ui.StoryCameraScreen
import de.focusontop.client.ui.ForgotScreen
import de.focusontop.client.ui.FtTheme
import de.focusontop.client.ui.LoginScreen
import de.focusontop.client.ui.MainScreen
import de.focusontop.client.ui.NotificationsScreen
import de.focusontop.client.ui.SplashScreen
import de.focusontop.client.ui.WelcomeScreen
import kotlinx.coroutines.delay

/** [debugStartScreen] / [debugLang]: visual-QA only (desktop screenshot harness) —
 *  jumps straight past Splash/Login to a given screen in a given language.
 *  Both null (the Android/iOS entry points) behaves exactly as before. */
@Composable
fun App(debugStartScreen: Screen? = null, debugLang: de.focusontop.client.i18n.Lang? = null) {
    setSingletonImageLoaderFactory { ctx ->
        ImageLoader.Builder(ctx)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }

    val state = remember {
        AppState().apply {
            if (debugLang != null) setLanguage(debugLang)
            if (debugStartScreen != null) { stack.clear(); stack.add(debugStartScreen) }
        }
    }
    val strings = stringsFor(state.lang)

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLang provides state.lang,
        LocalLayoutDirection provides if (state.lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        FtTheme {
            PlatformBackHandler(enabled = state.stack.size > 1) { state.back() }

            Box(Modifier.fillMaxSize().background(C.Bg)) {
                AnimatedContent(
                    targetState = state.current,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    contentKey = { it::class.simpleName + ((it as? Screen.FileDetail)?.file?.id ?: "") + ((it as? Screen.Viewer)?.ids?.joinToString() ?: "") + ((it as? Screen.Post)?.id ?: "") + ((it as? Screen.ClientProfile)?.id ?: "") },
                    label = "screen",
                ) { screen ->
                    when (screen) {
                        Screen.Splash -> {
                            SplashScreen()
                            LaunchedEffect(Unit) { delay(900); state.start() }
                        }
                        Screen.Welcome -> WelcomeScreen(state)
                        Screen.Onboarding -> OnboardingScreen(state)
                        Screen.PrivacyConsent -> PrivacyConsentScreen(state)
                        Screen.NotificationsPermission -> NotificationsPermissionScreen(state)
                        Screen.Login -> LoginScreen(state)
                        Screen.Forgot -> ForgotScreen(state)
                        Screen.Register -> RegisterScreen(state)
                        Screen.Main -> MainScreen(state)
                        Screen.Notifications -> NotificationsScreen(state)
                        is Screen.FileDetail -> ViewerScreen(state, listOf(screen.file.id), 0, null, screen.file)
                        is Screen.CompareVersions -> VersionCompareScreen(state, screen.file)
                        is Screen.Viewer -> ViewerScreen(state, screen.ids, screen.index, screen.title)
                        Screen.Messages -> MessagesScreen(state)
                        is Screen.Chat -> ChatScreen(state, screen.draft)
                        Screen.AppointmentRequest -> AppointmentRequestScreen(state)
                        Screen.Settings -> SettingsScreen(state)
                        Screen.EditProfile -> EditProfileScreen(state)
                        Screen.Legal -> LegalScreen(state)
                        Screen.Plans -> PlansScreen(state)
                        Screen.NewProject -> NewProjectScreen(state)
                        Screen.PlanCompare -> PlanCompareScreen(state)
                        is Screen.ProjectDetail -> ProjectDetailScreen(state, screen.name)
                        is Screen.Delivery -> DeliveryScreen(state, screen.name)
                        Screen.Files -> FilesPage(state)
                        Screen.Invoices -> InvoicesScreen(state)
                        Screen.Projects -> ProjectsScreen(state)
                        is Screen.Stories -> StoriesScreen(state, screen.groups, screen.group)
                        is Screen.Compose -> ComposeScreen(state, screen.kind)
                        is Screen.Post -> PostScreen(state, screen.id, screen.post)
                        is Screen.ClientProfile -> ClientProfileScreen(state, screen.id)
                        Screen.Discover -> DiscoverScreen(state)
                        Screen.StoryCamera -> StoryCameraScreen(state)
                    }
                }
                // short confirmations ("Link kopiert")
                state.toast?.let { msg ->
                    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 110.dp), contentAlignment = Alignment.BottomCenter) {
                        Text(
                            msg, color = C.Bg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(C.Gold).padding(horizontal = 18.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
