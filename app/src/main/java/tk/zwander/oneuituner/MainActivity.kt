package tk.zwander.oneuituner

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.shape.MaterialShapeDrawable
import com.samsungthemelib.IRootInterface
import com.samsungthemelib.ui.Installer
import com.samsungthemelib.ui.PermissionsActivity
import com.samsungthemelib.util.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import tk.zwander.oneuituner.ui.MenuModal
import tk.zwander.oneuituner.util.*
import java.io.File
import java.net.URLConnection

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener, (File) -> Unit, () -> Unit, IPCConnectionListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private val currentFrag: NavDestination?
        get() = navController.currentDestination

    private val navButton by lazy {
        run {
            Toolbar::class.java
                .getDeclaredField("mNavButtonView")
                .apply {
                    isAccessible = true
                }
                .get(bottom_bar) as View
        }
    }
    private val resultListener: (ResultData) -> Unit = { data ->
        progress.animatedVisibility = View.GONE

        val status = data.status
        val success = status == PackageInstaller.STATUS_SUCCESS

        Toast.makeText(this@MainActivity, if (success) R.string.succeeded else R.string.failed, Toast.LENGTH_SHORT)
            .show()

        if (success && needsThemeCenter && !prefs.forceNormalInstall) {
            AlertDialog.Builder(this)
                .setTitle(R.string.launch_theme_center)
                .setMessage(resources.getString(R.string.launch_theme_center_desc, ThemeCompiler.PROJECT_TITLE))
                .setPositiveButton(R.string.go) { _, _ ->
                    val themeStore = Intent()

                    themeStore.data = Uri.parse("themestore://MainPage?contentsType=THEMES&appId=${ThemeCompiler.PROJECT_PACKAGE_NAME}")
                    startActivity(themeStore)
                }
                .setNegativeButton(R.string.close, null)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs.registerOnSharedPreferenceChangeListener(this)

        PermissionsActivity.requestForResult(
            this,
            PermissionsActivity.REQ_PERMISSIONS,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        bottom_bar.setNavigationOnClickListener {
            onBackPressed()
        }

        menuInflater.inflate(R.menu.menu, bottom_bar.menu)

        bottom_bar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.open_menu -> {
                    MenuModal()
                        .show(supportFragmentManager, null)
                    true
                }
                else -> false
            }
        }

        navButton.visibility = View.GONE

        with(bottom_bar.background as MaterialShapeDrawable) {
            val color = ElevationOverlayProvider(this@MainActivity)
                .getSurfaceColorWithOverlayIfNeeded(elevation)

            window.navigationBarColor = color
        }

        root.layoutTransition = LayoutTransition()
            .apply {
                enableTransitionType(LayoutTransition.CHANGING)
            }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        updateFABs()

        navController.addOnDestinationChangedListener(this)

        apply.setOnClickListener {
            progress.animatedVisibility = View.VISIBLE
            compile(currentFrag?.label.toString(), this)
        }

        remove.setOnClickListener {
            progress.animatedVisibility = View.VISIBLE
            uninstall(currentFrag?.label.toString(), this)
        }

        val animDuration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

        title_switcher.inAnimation =
            AnimationUtils.loadAnimation(this, android.R.anim.fade_in).apply { duration = animDuration }
        title_switcher.outAnimation =
            AnimationUtils.loadAnimation(this, android.R.anim.fade_out).apply { duration = animDuration }

        themeLibApp.addResultListener(resultListener)
        themeLibApp.addConnectionListener(this)
    }

    override fun onIPCConnected(ipc: IRootInterface?) {
    }

    override fun onIPCDisconnected(ipc: IRootInterface?) {
        isSuAsync {
            if (it) {
                themeLibApp.launchSu()
            }
        }
    }

    override fun invoke(apk: File) {
        if (needsThemeCenter && !prefs.forceNormalInstall) {
            if (prefs.useSynergy) {
                installForSynergy(apk)
            } else {
                moveToInputDir(arrayOf(apk))
            }

            runOnUiThread {
                progress.animatedVisibility = View.GONE
                updateFABs()
            }
        } else {
            if (prefs.useSynergy) {
                installForSynergy(apk)

                runOnUiThread {
                    progress.animatedVisibility = View.GONE
                    updateFABs()
                }
            } else {
                Installer.install(this, arrayOf(apk), themeLibApp.rootBinder)
            }
        }
    }

    override fun invoke() {
        runOnUiThread {
            progress.visibility = View.GONE
            updateFABs()
        }
    }

    private fun installForSynergy(file: File) {
        val fileUri = FileProvider.getUriForFile(this,
            "$packageName.apkprovider", file)
        Intent(Intent.ACTION_SEND).run {
            `package` = "projekt.samsung.theme.compiler"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = URLConnection.guessContentTypeFromName(file.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(this)
        }
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        updateFABs()
    }

    override fun setTitle(title: CharSequence?) {
        title_switcher.setText(title)
        super.setTitle(null)
    }

    override fun setTitle(titleId: Int) {
        title = getText(titleId)
    }

    override fun onDestroy() {
        super.onDestroy()

        prefs.unregisterOnSharedPreferenceChangeListener(this)
        navController.removeOnDestinationChangedListener(this)
        themeLibApp.removeResultListener(resultListener)
    }

    override fun onResume() {
        super.onResume()

        updateFABs()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            PrefManager.USE_SYNERGY,
            PrefManager.FORCE_NORMAL_INSTALL -> {
                updateFABs()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PermissionsActivity.REQ_PERMISSIONS -> {
                val perms = data?.getStringArrayExtra(PermissionsActivity.EXTRA_PERMISSIONS)!!
                val results = data.getIntArrayExtra(PermissionsActivity.EXTRA_PERMISSION_RESULTS)!!

                perms.forEachIndexed { index, s ->
                    if (s == android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        && results[index] != PackageManager.PERMISSION_GRANTED) finish()
                }
            }
        }
    }

    private fun updateFABs() {
        val id = currentFrag?.id
        val enabled = id != R.id.main

        navButton.animatedVisibility = if (enabled) View.VISIBLE else View.GONE
        apply.animatedVisibility = if (enabled) View.VISIBLE else View.INVISIBLE

        remove.animatedVisibility = when {
            when (id) {
                R.id.clock -> isInstalled(Keys.clockPkg)
                R.id.qs -> isInstalled(Keys.qsPkg)
                R.id.misc -> isInstalled(Keys.miscPkg)
                R.id.statusBar -> isInstalled(Keys.statusBarPkg)
                R.id.lockScreen -> isInstalled(Keys.lockScreenPkg)
                else -> false
            } -> View.VISIBLE
            prefs.useSynergy -> View.GONE
            else -> View.INVISIBLE
        }

        if (needsThemeCenterAndNoSynergy && !prefs.forceNormalInstall) {
            install.animatedVisibility = View.VISIBLE
            install.setOnClickListener {
                progress.animatedVisibility = View.VISIBLE
                compileAndInstall()
            }
        } else {
            install.animatedVisibility = if (prefs.useSynergy) View.GONE else View.INVISIBLE
        }
    }

    private var View.animatedVisibility: Int
        get() = visibility
        set(value) {
            val hide = value != View.VISIBLE

            if (!hide) visibility = value

            animate()
                .scaleX(if (hide) 0f else 1f)
                .scaleY(if (hide) 0f else 1f)
                .setDuration(resources.getInteger(android.R.integer.config_mediumAnimTime).toLong())
                .setInterpolator(if (hide) AnticipateInterpolator() else OvershootInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        if (hide) visibility = value
                    }
                })
        }
}
