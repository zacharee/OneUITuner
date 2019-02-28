package tk.zwander.oneuituner.root

import android.annotation.SuppressLint
import android.content.Context
import android.os.IBinder
import com.topjohnwu.superuser.io.SuFile
import eu.chainfire.librootjava.RootIPC
import eu.chainfire.librootjava.RootJava
import eu.chainfire.libsuperuser.Shell
import tk.zwander.oneuituner.BuildConfig
import tk.zwander.oneuituner.RootBridge
import tk.zwander.oneuituner.util.WorkaroundInstaller
import tk.zwander.oneuituner.util.completionIntent
import tk.zwander.oneuituner.util.needsRoot
import java.nio.file.Files

@SuppressLint("StaticFieldLeak")
object RootStuff {
    private val context = RootJava.getSystemContext()
    private val workaroundInstaller = WorkaroundInstaller.getRootInstance(context)

    @JvmStatic
    fun main(args: Array<String>) {
        RootJava.restoreOriginalLdLibraryPath()

        val ipc = RootBridgeImpl()

        try {
            RootIPC(BuildConfig.APPLICATION_ID, ipc, 100, 30 * 1000, true)
        } catch (e: RootIPC.TimeoutException) {}
    }

    class RootBridgeImpl : RootBridge.Stub() {
        @SuppressLint("PrivateApi")
        val iPMClass = Class.forName("android.os.IPowerManager")

        @SuppressLint("PrivateApi")
        val pm = kotlin.run {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val iPMStubClass = Class.forName("android.os.IPowerManager\$Stub")

            val getServiceOrThrow = serviceManagerClass.getMethod("getServiceOrThrow", String::class.java)
            val asInterface = iPMStubClass.getMethod("asInterface", IBinder::class.java)

            val binder = getServiceOrThrow.invoke(null, Context.POWER_SERVICE) as IBinder
            asInterface.invoke(null, binder)
        }

        override fun reboot(reason: String) {
            iPMClass.getMethod("reboot", Boolean::class.java, String::class.java, Boolean::class.java)
                .invoke(pm, false, reason, true)
        }

        override fun installPkg(path: String, name: String) {
            if (needsRoot) {
                Shell.SU.run("mount -o rw,remount /system")

                val src = SuFile(path)
                val dstDir = SuFile("/system/app/$name")

                if (!dstDir.exists()) {
                    dstDir.mkdirs()
                }

                val dstFile = SuFile(dstDir, "$name.apk")

                Files.copy(src.toPath(), dstFile.toPath())

                Shell.SU.run("mount -o ro,remount /system")
            } else {
                workaroundInstaller.installPackage(path, name)
            }
        }

        override fun uninstallPkg(pkg: String) {
            if (needsRoot) {
                Shell.SU.run("mount -o rw,remount /system")

                val path = context.packageManager.getPackageInfo(pkg, 0)
                    .applicationInfo.sourceDir

                SuFile(path).parentFile.deleteRecursively()

                Shell.SU.run("mount -o ro,remount /system")

                context.startActivity(completionIntent)
            } else {
                workaroundInstaller.uninstallPackage(pkg)
            }
        }
    }
}