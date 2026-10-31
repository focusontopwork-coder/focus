package de.focusontop.client

import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/**
 * Real entry point for the browser build (focusontop.de/mobile-web), unlike
 * desktopMain's Main.kt which only renders one screen for QA screenshots.
 * Mounts the same shared App() used on Android/iOS into index.html's
 * #ComposeTarget div.
 */
fun main() {
    ComposeViewport(document.getElementById("ComposeTarget")!!) {
        App()
    }
}
