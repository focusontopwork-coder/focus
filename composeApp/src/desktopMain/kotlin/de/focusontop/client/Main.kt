package de.focusontop.client

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import de.focusontop.client.data.AppState
import de.focusontop.client.data.Screen
import de.focusontop.client.i18n.Lang

/**
 * Visual-QA only: renders one named screen in one language, full-screen,
 * so it can be screenshotted headlessly (Xvfb + ImageMagick) and compared
 * pixel-for-pixel against the matching Figma frame. Never shipped — this is
 * a separate "desktop" Kotlin target alongside Android/iOS (see build.gradle.kts).
 *
 * Usage: SCREEN=Login LANG=ar java -cp ... de.focusontop.client.MainKt
 */
private fun screenFor(name: String): Screen = when (name) {
    "Welcome" -> Screen.Welcome
    "Onboarding" -> Screen.Onboarding
    "PrivacyConsent" -> Screen.PrivacyConsent
    "NotificationsPermission" -> Screen.NotificationsPermission
    "Login" -> Screen.Login
    "Forgot" -> Screen.Forgot
    "Register" -> Screen.Register
    "Main" -> Screen.Main
    "Notifications" -> Screen.Notifications
    "Messages" -> Screen.Messages
    "Settings" -> Screen.Settings
    "EditProfile" -> Screen.EditProfile
    "Legal" -> Screen.Legal
    "Plans" -> Screen.Plans
    "NewProject" -> Screen.NewProject
    "PlanCompare" -> Screen.PlanCompare
    "Files" -> Screen.Files
    "Invoices" -> Screen.Invoices
    "Projects" -> Screen.Projects
    "Discover" -> Screen.Discover
    "StoryCamera" -> Screen.StoryCamera
    else -> Screen.Login
}

private fun langFor(name: String): Lang = when (name.lowercase()) {
    "ar" -> Lang.AR
    "en" -> Lang.EN
    else -> Lang.DE
}

fun main() {
    val screenName = System.getenv("SCREEN") ?: "Login"
    val langName = System.getenv("LANG_CODE") ?: "de"
    val widthDp = (System.getenv("WIDTH_DP") ?: "390").toInt()
    val heightDp = (System.getenv("HEIGHT_DP") ?: "900").toInt()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "QA",
            state = WindowState(position = WindowPosition(0.dp, 0.dp), size = androidx.compose.ui.unit.DpSize(widthDp.dp, heightDp.dp)),
            undecorated = true,
        ) {
            App(debugStartScreen = screenFor(screenName), debugLang = langFor(langName))
        }
    }
}
