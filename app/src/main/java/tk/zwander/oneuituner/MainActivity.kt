package tk.zwander.oneuituner

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
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

    private val backButton by lazy { createBackButton() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            install(currentFrag?.label.toString(), this)
        }

        remove.setOnClickListener {

        }

        val animDuration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

        title_switcher.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in).apply { duration =  animDuration}
        title_switcher.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out).apply { duration = animDuration }
        title_switcher.setFactory {
            AppCompatTextView(this).apply {
                setTextAppearance(android.R.style.TextAppearance_Material_Widget_ActionBar_Title)
                setTextColor(Color.WHITE)
            }
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

    override fun setTitle(title: CharSequence?) {
        title_switcher.setText(title)
        super.setTitle(null)
    }

    override fun setTitle(titleId: Int) {
        title = getText(titleId)
    }

    override fun onDestroy() {
        super.onDestroy()

        navController.removeOnDestinationChangedListener(this)
    }

    private fun updateFABs() {
        val id = currentFrag?.id
        setBackClickable(id != R.id.main)

        when (id) {
            R.id.main -> {
                remove_wrapper.animatedVisibility = View.GONE
                apply_wrapper.animatedVisibility = View.GONE
            }

            R.id.clock -> {
                remove_wrapper.animatedVisibility = if (isInstalled(Keys.clockPkg)) View.VISIBLE else View.GONE
                apply_wrapper.animatedVisibility = View.VISIBLE
            }

            R.id.qs -> {
                remove_wrapper.animatedVisibility = if (isInstalled(Keys.qsPkg)) View.VISIBLE else View.GONE
                apply_wrapper.animatedVisibility = View.VISIBLE
            }
        }
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
}
