package tk.zwander.oneuituner.util

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceFragmentCompat
import eu.chainfire.libsuperuser.Shell
import tk.zwander.oneuituner.App
import tk.zwander.oneuituner.BuildConfig
import tk.zwander.oneuituner.R
import java.io.File


val Context.aapt: String?
    get() {
        val aapt = File(cacheDir, "aapt")
        if (aapt.exists()) return aapt.absolutePath

        if (!assets.extractAsset("aapt", aapt.absolutePath))
            return null

        Shell.SH.run("chmod 755 ${aapt.absolutePath}")
        return aapt.absolutePath
    }

val Context.zipalign: String?
    get() {
        val zipalign = File(cacheDir, "zipalign")
        if (zipalign.exists()) {
            Shell.SH.run("chmod 755 ${zipalign.absolutePath}")
            return zipalign.absolutePath
        }

        if (!assets.extractAsset("zipalign", zipalign.absolutePath))
            return null

        Shell.SH.run("chmod 755 ${zipalign.absolutePath}")
        return zipalign.absolutePath
    }

val Context.app: App
    get() = applicationContext as App

val Context.prefs: PrefManager
    get() = PrefManager.getInstance(this)

val Context.workaroundInstaller: WorkaroundInstaller
    get() = WorkaroundInstaller.getInstance(this)

fun Context.isInstalled(packageName: String) =
        try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            Log.e("OneUITuner", e.message)
            false
        }

val PreferenceFragmentCompat.navController: NavController
    get() = NavHostFragment.findNavController(this)

val navOptions: NavOptions
    get() = NavOptions.Builder()
        .setEnterAnim(android.R.anim.fade_in)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(android.R.anim.fade_out)
        .build()

val needsRoot: Boolean
    get() {
        val df = SimpleDateFormat("YYYY-MM-DD")
        val compDate = df.parse("2019-02-01")
        val secDate = df.parse(Build.VERSION.SECURITY_PATCH)

        return !(Build.MODEL.contains("960") || Build.MODEL.contains("965"))
                || secDate.after(compDate)
    }

val Activity.navController: NavController
    get() = findNavController(R.id.nav_host)

val String.isValidClockFormat: Boolean
    get() = try {
        SimpleDateFormat(this)
        true
    } catch (e: Exception) {
        false
    }

val completionIntent: Intent
    get() = Intent(WorkaroundInstaller.ACTION_FINISHED).apply {
        component = ComponentName(BuildConfig.APPLICATION_ID, "${BuildConfig.APPLICATION_ID}.MainActivity")
    }

fun Context.broadcastFinish(intent: Intent) {
    intent.component = ComponentName(BuildConfig.APPLICATION_ID, "${BuildConfig.APPLICATION_ID}.FinishReceiver")
    sendBroadcast(intent)
}

fun loggedSu(command: String) {
    Log.e("OneUITuner", command)
    Shell.run("su", arrayOf(command), null, true)
        .apply { Log.e("OneUITuner", this?.toString() ?: "null") }
}