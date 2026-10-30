package de.focusontop.client

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import de.focusontop.client.platform.AndroidHost
import de.focusontop.client.platform.PushBridge

// FragmentActivity (not plain ComponentActivity) — androidx.biometric's BiometricPrompt
// needs one for the Face ID / fingerprint sheet (Figma "03b · Zugang · Face ID").
// It still extends ComponentActivity under the hood, so setContent/enableEdgeToEdge work unchanged.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        AndroidHost.activity = java.lang.ref.WeakReference(this)
        fetchPushToken()
        setContent { App() }
    }

    override fun onDestroy() {
        if (AndroidHost.activity?.get() === this) AndroidHost.activity = null
        super.onDestroy()
    }

    /** Firebase is optional: without google-services.json there is simply no push. */
    private fun fetchPushToken() {
        runCatching {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { t -> if (!t.isNullOrBlank()) PushBridge.onToken(t) }
        }
    }
}
