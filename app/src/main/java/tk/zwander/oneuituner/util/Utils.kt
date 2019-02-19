package tk.zwander.oneuituner.util

import android.app.Activity
import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceFragmentCompat
import tk.zwander.oneuituner.App
import tk.zwander.oneuituner.R


val Context.app: App
    get() = applicationContext as App

val Context.prefs: PrefManager
    get() = PrefManager.getInstance(this)

fun Context.isInstalled(packageName: String) =
        try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }

val PreferenceFragmentCompat.navController: NavController
    get() = NavHostFragment.findNavController(this)

val PreferenceFragmentCompat.navOptions: NavOptions
    get() = NavOptions.Builder()
        .setEnterAnim(android.R.anim.fade_in)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(android.R.anim.fade_out)
        .build()

val Activity.navController: NavController
    get() = findNavController(R.id.nav_host)