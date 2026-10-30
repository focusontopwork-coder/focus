package de.focusontop.client.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** The current language, for the newer screens that keep their texts next to the UI. */
val LocalLang = staticCompositionLocalOf { Lang.DE }

/** One text in the three app languages (German first — the system's base language). */
@Composable
fun tr(de: String, en: String, ar: String): String = when (LocalLang.current) {
    Lang.DE -> de
    Lang.EN -> en
    Lang.AR -> ar
}

/** Same, outside composition. */
fun trL(lang: Lang, de: String, en: String, ar: String): String = when (lang) {
    Lang.DE -> de
    Lang.EN -> en
    Lang.AR -> ar
}
