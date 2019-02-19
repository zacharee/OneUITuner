package tk.zwander.oneuituner

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import tk.zwander.oneuituner.util.Keys
import tk.zwander.oneuituner.util.install
import tk.zwander.oneuituner.util.isInstalled
import tk.zwander.oneuituner.util.navController
import java.io.File

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener, (File) -> Unit {
    private val currentFrag: NavDestination?
        get() = navController.currentDestination

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        updateFABs()

        navController.addOnDestinationChangedListener(this)

        apply.setOnClickListener {
            install(currentFrag?.label.toString(), this)
        }

        remove.setOnClickListener {

        }

//        val ldClass = Class.forName("libcore.icu.LocaleData")
//        val get = ldClass.getMethod("get", Locale::class.java)
//        val d = get.invoke(null, resources.configuration.locale)
//
//        val hms = ldClass.getDeclaredField("timeFormat_hms")
//            .apply { isAccessible = true }
//            .get(d)
//
//        Log.e("OneUITuner", hms.toString())
    }

    override fun invoke(apk: File) {
        val installIntent = Intent(Intent.ACTION_VIEW)
        installIntent.setDataAndType(
            FileProvider.getUriForFile(this,
                "$packageName.apkprovider", apk),
            "application/vnd.android.package-archive")
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(installIntent)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        updateFABs()
    }

    override fun onDestroy() {
        super.onDestroy()

        navController.removeOnDestinationChangedListener(this)
    }

    private fun updateFABs() {
        when (currentFrag?.id) {
            R.id.main -> {
                remove.visibility = View.GONE
                apply.visibility = View.GONE
            }

            R.id.clock -> {
                remove.visibility = if (isInstalled(Keys.clockPkg)) View.VISIBLE else View.GONE
                apply.visibility = View.VISIBLE
            }

            R.id.qs -> {
                remove.visibility = if (isInstalled(Keys.qsPkg)) View.VISIBLE else View.GONE
                apply.visibility = View.VISIBLE
            }
        }
    }
}
