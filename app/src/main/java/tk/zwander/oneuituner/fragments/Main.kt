package tk.zwander.oneuituner.fragments

import android.os.Bundle
import androidx.preference.Preference
import com.samsungthemelib.util.killTrial
import com.samsungthemelib.util.trialKillerActive
import tk.zwander.oneuituner.R
import tk.zwander.oneuituner.util.Keys
import tk.zwander.oneuituner.util.navController
import tk.zwander.oneuituner.util.navOptions

class Main : Base() {
    companion object {
        private const val ACTIVATE_TRIAL_KILLER = "activate_trial_killer"
    }

    override val title = R.string.app_name

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main, rootKey)

        findPreference(ACTIVATE_TRIAL_KILLER).setOnPreferenceChangeListener { _, newValue ->
            val active = newValue.toString().toBoolean()

            context?.trialKillerActive = active
            if (active) context?.killTrial()

            true
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return run {
            val (ret, action) = when(preference?.key) {
                Keys.clock -> true to R.id.action_main_to_clock
                Keys.qs -> true to R.id.action_main_to_qs
                Keys.misc -> true to R.id.action_main_to_misc
                Keys.statusBar -> true to R.id.action_main_to_statusBar
                Keys.lockScreen -> true to R.id.action_main_to_lockScreen
                else -> super.onPreferenceTreeClick(preference) to 0
            }

            if (action != 0) {
                navController.navigate(
                    action,
                    null,
                    navOptions
                )
            }

            ret
        }
    }
}