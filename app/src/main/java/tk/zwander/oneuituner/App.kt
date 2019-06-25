package tk.zwander.oneuituner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.samsungthemelib.ThemeLibApp

class App : ThemeLibApp() {
    val nm by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            "oneuituner_main",
            resources.getText(R.string.app_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannel(channel)
    }
}