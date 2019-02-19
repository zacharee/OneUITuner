package tk.zwander.oneuituner

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import eu.chainfire.librootjava.RootIPCReceiver
import eu.chainfire.librootjava.RootJava
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tk.zwander.oneuituner.root.RootStuff

class App : Application() {
    val ipcReceiver by lazy { IPCReceiverImpl(this, 100) }
    val nm by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        super.onCreate()

        GlobalScope.launch {
            Shell.SU.run("pm grant $packageName ${android.Manifest.permission.WRITE_SECURE_SETTINGS}")
            Shell.SU.run("pm grant $packageName ${android.Manifest.permission.WRITE_EXTERNAL_STORAGE}")

            Shell.SU.run(
                RootJava.getLaunchScript(
                    this@App,
                    RootStuff::class.java,
                    null, null, null,
                    "${BuildConfig.APPLICATION_ID}:root"))
        }

        ipcReceiver.setContext(this)

        val channel = NotificationChannel(
            "opfp_main",
            resources.getText(R.string.app_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannel(channel)
    }

    class IPCReceiverImpl(context: Context, code: Int) : RootIPCReceiver<RootBridge>(context, code) {
        var ipc: RootBridge? = null
            set(value) {
                field = value

                if (value != null) {
                    queuedActions.forEach {
                        it.invoke(value)
                    }
                    queuedActions.clear()
                }
            }

        private var queuedActions = ArrayList<(RootBridge) -> Unit>()

        override fun onConnect(ipc: RootBridge?) {
            this.ipc = ipc
        }

        override fun onDisconnect(ipc: RootBridge?) {
            this.ipc = null
        }

        fun postIPCAction(action: (RootBridge) -> Unit) {
            if (ipc == null) {
                queuedActions.add(action)
            } else {
                action.invoke(ipc!!)
            }
        }
    }
}