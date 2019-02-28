package tk.zwander.oneuituner.util

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import tk.zwander.oneuituner.BuildConfig
import java.io.File
import java.io.FileInputStream

class WorkaroundInstaller(context: Context) : ContextWrapper(context) {
    companion object {
        const val flags = 16777346

        const val ACTION_FINISHED = "${BuildConfig.APPLICATION_ID}.intent.action.FINISHED"
    }

    val packageInstaller = packageManager.packageInstaller

    fun installPackage(path: String, name: String) {
        val input = FileInputStream(File(path))
        installInternal(input, name)
    }

    fun installPackage(source: Uri, name: String) {
        val input = FileInputStream(contentResolver.openFileDescriptor(source, "r")!!.fileDescriptor)
        installInternal(input, name)
    }

    private fun installInternal(input: FileInputStream, name: String) {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply {
                val installParamsField = this::class.java.getField("installFlags")
                installParamsField.set(this, flags)
            }
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        val output = session.openWrite(name, 0, -1)

        val buf = ByteArray(16384)

        try {
            var b: Int

            while (true) {
                b = input.read(buf)

                if (b < 0) break

                output.write(buf, 0, b)
            }
        } finally {
            session.fsync(output)
            input.close()
            output.close()
            session.commit(
                PendingIntent.getActivity(
                    this,
                    sessionId,
                    Intent(ACTION_FINISHED).apply {
                        component = ComponentName(BuildConfig.APPLICATION_ID, "${BuildConfig.APPLICATION_ID}.MainActivity")
                    },
                    0
                ).intentSender
            )
        }
    }
}