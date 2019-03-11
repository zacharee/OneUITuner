package tk.zwander.oneuituner.root

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.IBinder
import eu.chainfire.librootjava.RootIPC
import eu.chainfire.librootjava.RootJava
import tk.zwander.oneuituner.BuildConfig
import tk.zwander.oneuituner.RootBridge
import tk.zwander.oneuituner.SuRunner
import tk.zwander.oneuituner.util.WorkaroundInstaller
import tk.zwander.oneuituner.util.broadcastFinish
import tk.zwander.oneuituner.util.log
import tk.zwander.oneuituner.util.needsRoot
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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

        var su: SuRunner? = null

        override fun setSuRunner(runner: SuRunner?) {
            su = runner
        }

        override fun reboot(reason: String) {
            iPMClass.getMethod("reboot", Boolean::class.java, String::class.java, Boolean::class.java)
                .invoke(pm, false, reason, true)
        }

        override fun installPkg(path: String, name: String) {
            if (needsRoot) {
                val intent = Intent(WorkaroundInstaller.ACTION_FINISHED)

                try {
                    su?.run("mount -o rw,remount /system")

                    val src = File(path)
                    val dstDir = File("/system/app/$name")

                    dstDir.mkdir()

                    val dstFile = File(dstDir, "$name.apk")

                    Files.copy(src.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

                    su?.run("mount -o ro,remount /system")

                    workaroundInstaller.installPackage(dstFile.absolutePath, name)
                } catch (e: Exception) {
                    e.log()
                    intent.putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                    intent.putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, e.message)

                    context.broadcastFinish(intent)
                }

            } else {
                workaroundInstaller.installPackage(path, name)
            }
        }

        override fun uninstallPkg(pkg: String) {
            if (needsRoot) {
                val intent = Intent(WorkaroundInstaller.ACTION_FINISHED)

                try {
                    su?.run("mount -o rw,remount /system")

                    val path = context.packageManager.getPackageInfo(pkg, 0)
                        .applicationInfo.sourceDir

                    su?.run("rm -rf ${File(path).parentFile.absolutePath}")

                    su?.run("mount -o ro,remount /system")

                    workaroundInstaller.uninstallPackage(pkg)
                } catch (e: Exception) {
                    intent.putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                    intent.putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, e.message)

                    context.broadcastFinish(intent)
                }
            } else {
                workaroundInstaller.uninstallPackage(pkg)
            }
        }
    }
}