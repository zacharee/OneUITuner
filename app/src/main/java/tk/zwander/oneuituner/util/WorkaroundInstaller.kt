package tk.zwander.oneuituner.util

import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInstaller
import android.net.Uri
import tk.zwander.oneuituner.BuildConfig
import java.io.File
import java.io.FileInputStream

class WorkaroundInstaller private constructor(context: Context) : ContextWrapper(context) {
    companion object {
        const val flags = 16777346

        const val ACTION_FINISHED = "${BuildConfig.APPLICATION_ID}.intent.action.FINISHED"

        private var instance: WorkaroundInstaller? = null

        fun getInstance(context: Context): WorkaroundInstaller {
            if (instance == null) instance = WorkaroundInstaller(context.applicationContext)

            return instance!!
        }

        fun getRootInstance(context: Context): WorkaroundInstaller {
            return WorkaroundInstaller(context)
        }
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

    fun uninstallPackage(pkg: String) {
        packageInstaller.uninstall(pkg,
            PendingIntent.getActivity(
                this,
                100,
                completionIntent,
                0
            ).intentSender
        )
    }

    private fun installInternal(input: FileInputStream, name: String) {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply {
                val installParamsField = this::class.java.getField("installFlags")
                installParamsField.set(this, if (needsRoot) 0x10 else flags)

                if (needsRoot) {
                    val volumeUuidField = this::class.java.getField("volumeUuid")
                    volumeUuidField.set(this, "system")
                }
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
                    completionIntent,
                    0
                ).intentSender
            )
        }
    }
}