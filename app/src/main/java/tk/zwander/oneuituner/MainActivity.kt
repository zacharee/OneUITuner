package tk.zwander.oneuituner

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.samsungthemelib.IRootInterface
import com.samsungthemelib.ui.Installer
import com.samsungthemelib.ui.PermissionsActivity
import com.samsungthemelib.util.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.adb_alert.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import tk.zwander.oneuituner.util.*
import java.io.File

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener, (File) -> Unit, IPCConnectionListener {
    private val currentFrag: NavDestination?
        get() = navController.currentDestination
    private val overlayReceiver = OverlayReceiver()

    private val backButton by lazy { createBackButton() }
    private val resultListener: (ResultData) -> Unit = { data ->
        progress_apply.visibility = View.GONE
        progress_install.visibility = View.GONE
        progress_remove.visibility = View.GONE

        val status = data.status
        val success = status == PackageInstaller.STATUS_SUCCESS

        Toast.makeText(this@MainActivity, if (success) R.string.succeeded else R.string.failed, Toast.LENGTH_SHORT)
            .show()

        if (success && needsThemeCenter) {
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
    private val ipcDialog by lazy {
        AlertDialog.Builder(this)
            .setTitle(R.string.adb_needed)
            .setMessage(R.string.adb_needed_desc)
            .setCancelable(false)
            .setPositiveButton(R.string.show_me_command, null)
            .setNegativeButton(R.string.close) { _, _ ->
                finish()
            }
            .create()
            .apply {
                setOnShowListener {
                    val show = getButton(AlertDialog.BUTTON_POSITIVE)
                    show.setOnClickListener {
                        dismiss()
                        showADBDialog()
                    }
                }
            }
    }
    private val adbDialog by lazy {
        AlertDialog.Builder(this)
            .setView(R.layout.adb_alert)
            .setTitle(R.string.adb_needed)
            .setCancelable(false)
            .setPositiveButton(R.string.check, null)
            .setNegativeButton(R.string.close) { _, _ ->
                finish()
            }
            .create()
            .apply {
                setOnShowListener {
                    val check = getButton(AlertDialog.BUTTON_POSITIVE)
                    check.setOnClickListener {
                        if (!themeLibApp.ipcReceiver.connected) {
                            Toast.makeText(this@MainActivity, R.string.try_again, Toast.LENGTH_SHORT).show()
                        } else {
                            dismiss()
                        }
                    }
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionsActivity.requestForResult(
            this,
            PermissionsActivity.REQ_PERMISSIONS,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        createShellLauncher()

        overlayReceiver.register()

        root.layoutTransition = LayoutTransition()
            .apply {
                enableTransitionType(LayoutTransition.CHANGING)
            }

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        updateFABs()

        navController.addOnDestinationChangedListener(this)

        apply.setOnClickListener {
            progress_apply.visibility = View.VISIBLE
            compile(currentFrag?.label.toString(), this)
        }

        remove.setOnClickListener {
            progress_remove.visibility = View.VISIBLE
            uninstall(currentFrag?.label.toString())
        }

        val animDuration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

        title_switcher.inAnimation =
            AnimationUtils.loadAnimation(this, android.R.anim.fade_in).apply { duration = animDuration }
        title_switcher.outAnimation =
            AnimationUtils.loadAnimation(this, android.R.anim.fade_out).apply { duration = animDuration }
        title_switcher.setFactory {
            AppCompatTextView(this).apply {
                setTextAppearance(android.R.style.TextAppearance_Material_Widget_ActionBar_Title)
                setTextColor(Color.WHITE)
            }
        }

        themeLibApp.addResultListener(resultListener)
        themeLibApp.addConnectionListener(this)

//        if (needsThemeCenter) {
//            if (!themeLibApp.ipcReceiver.connected) {
//                isSuAsync {
//                    if (!it) showNoIPCDialog()
//                }
//            } else {
//                themeLibApp.ipcReceiver.postIPCAction {
//                    if (themeLibApp.libVersion > it.version()) {
//                        it.stop()
//
//                        showNoIPCDialog()
//                    }
//                }
//            }
//        }

        if (needsThemeCenter) {
            install_wrapper.visibility = View.VISIBLE
            install.setOnClickListener {
                progress_install.visibility = View.VISIBLE
                compileAndInstall()
            }
        }
    }

    override fun onIPCConnected(ipc: IRootInterface?) {
        mainExecutor.execute {
            if (ipcDialog.isShowing) {
                ipcDialog.dismiss()
            }

            if (adbDialog.isShowing) {
                adbDialog.dismiss()
            }
        }
    }

    override fun onIPCDisconnected(ipc: IRootInterface?) {
        isSuAsync {
            if (it) {
                themeLibApp.launchSu()
            } else {
                ipcDialog.show()
            }
        }
    }

    private fun showNoIPCDialog() {
        ipcDialog.show()
    }

    private fun showADBDialog() {
        adbDialog.show()

        adbDialog.message.setText(R.string.adb_command)
        adbDialog.command.text = resources.getString(R.string.command, packageName)
    }

    override fun invoke(apk: File) {
        if (needsThemeCenter) {
            moveToInputDir(arrayOf(apk))
            runOnUiThread {
                progress_apply.visibility = View.GONE
            }
        } else {
            Installer.install(this, arrayOf(apk))
        }
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

    override fun setTitle(title: CharSequence?) {
        title_switcher.setText(title)
        super.setTitle(null)
    }

    override fun setTitle(titleId: Int) {
        title = getText(titleId)
    }

    override fun onDestroy() {
        super.onDestroy()

        overlayReceiver.unregister()
        navController.removeOnDestinationChangedListener(this)
        themeLibApp.removeResultListener(resultListener)
    }

    override fun onResume() {
        super.onResume()

        updateFABs()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PermissionsActivity.REQ_PERMISSIONS -> {
                if (resultCode != Activity.RESULT_OK) finish()
            }
        }
    }

    private fun updateFABs() {
        val id = currentFrag?.id
        val enabled = id != R.id.main

        setBackClickable(enabled)

        apply_wrapper.animatedVisibility = if (enabled) View.VISIBLE else View.GONE

        remove_wrapper.animatedVisibility = if (
            when (id) {
                R.id.clock -> isInstalled(Keys.clockPkg)
                R.id.qs -> isInstalled(Keys.qsPkg)
                R.id.misc -> isInstalled(Keys.miscPkg)
                R.id.statusBar -> isInstalled(Keys.statusBarPkg)
                R.id.lockScreen -> isInstalled(Keys.lockScreenPkg)
                else -> false
            }
        ) View.VISIBLE else View.GONE
    }

    private var RelativeLayout.animatedVisibility: Int
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

    private fun setBackClickable(clickable: Boolean) {
        backButton.isClickable = clickable

        backButton.animate()
            .scaleX(if (clickable) 1f else 0f)
            .scaleY(if (clickable) 1f else 0f)
            .setInterpolator(if (clickable) OvershootInterpolator() else AnticipateInterpolator())
            .setDuration(resources.getInteger(android.R.integer.config_mediumAnimTime).toLong())
            .start()
    }

    private fun createBackButton(): ImageButton {
        val mNavButtonView = toolbar::class.java.getDeclaredField("mNavButtonView")
        mNavButtonView.isAccessible = true

        return mNavButtonView.get(toolbar) as ImageButton
    }

    inner class OverlayReceiver : BroadcastReceiver() {
        fun register() {
            val remFilter = IntentFilter()
            remFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
            remFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
            remFilter.addDataScheme("package")

            registerReceiver(this, remFilter)
        }

        fun unregister() {
            unregisterReceiver(this)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
//            when (intent?.action) {
//                Intent.ACTION_PACKAGE_ADDED,
//                    Intent.ACTION_PACKAGE_REMOVED -> updateFABs()
//            }
        }
    }
}
