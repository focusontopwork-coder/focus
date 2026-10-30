package de.focusontop.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.focusontop.client.platform.AndroidHost
import de.focusontop.client.platform.PushBridge

class MainActivity : ComponentActivity() {
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
