package de.focusontop.client

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import de.focusontop.client.platform.AndroidHost

class FocusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidHost.init(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("default", getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH)
        )
    }
}
