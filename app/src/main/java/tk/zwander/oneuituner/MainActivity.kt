package tk.zwander.oneuituner

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.graphics.Color
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
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import eu.chainfire.libsuperuser.Shell
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import tk.zwander.oneuituner.util.*
import java.io.File

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener, (File) -> Unit {
    private val currentFrag: NavDestination?
        get() = navController.currentDestination
    private val overlayReceiver = OverlayReceiver()

    private val backButton by lazy { createBackButton() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            install(currentFrag?.label.toString(), this)
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

        if (needsRoot && !Shell.SU.available()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.root_required)
                .setMessage(R.string.root_required_desc)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    finish()
                }
                .show()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        when (intent?.action) {
            WorkaroundInstaller.ACTION_FINISHED -> {
                val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -100)
                val message: String? = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

                progress_apply.visibility = View.GONE
                progress_remove.visibility = View.GONE

                when (status) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        val confirmIntent = intent.extras?.get(Intent.EXTRA_INTENT) as Intent?
                        startActivity(confirmIntent)
                    }

                    PackageInstaller.STATUS_SUCCESS -> {
                        Toast.makeText(this, R.string.succeeded, Toast.LENGTH_SHORT).show()
                        updateFABs()
                    }

                    PackageInstaller.STATUS_FAILURE -> {
                        Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show()
                        updateFABs()
                    }
                }
            }
        }
    }

    override fun invoke(apk: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.apkprovider",
            apk
        )

        if (!Shell.SU.available()) {
            workaroundInstaller.installPackage(uri, apk.name)
        } else {
            app.ipcReceiver.postIPCAction { it.installPkg(apk.absolutePath, apk.nameWithoutExtension) }
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
    }

    override fun onResume() {
        super.onResume()

        updateFABs()
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
