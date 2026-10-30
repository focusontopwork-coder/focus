package de.focusontop.client

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Called from Swift: MainViewControllerKt.MainViewController() */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
