package tk.zwander.oneuituner.fragments

import android.os.Bundle
import tk.zwander.oneuituner.R
import tk.zwander.oneuituner.util.Keys
import tk.zwander.oneuituner.util.navController
import tk.zwander.oneuituner.util.navOptions

class Main : Base() {
    override val title = R.string.app_name

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main, rootKey)

        findPreference(Keys.clock).setOnPreferenceClickListener {
            navController.navigate(
                R.id.action_main_to_clock,
                null,
                navOptions
            )
            true
        }

        findPreference(Keys.qs).setOnPreferenceClickListener {
            navController.navigate(
                R.id.action_main_to_qs,
                null,
                navOptions
            )
            true
        }

        findPreference(Keys.misc).setOnPreferenceClickListener {
            navController.navigate(
                R.id.action_main_to_misc,
                null,
                navOptions
            )
            true
        }

        findPreference(Keys.statusBar).setOnPreferenceClickListener {
            navController.navigate(
                R.id.action_main_to_statusBar,
                null,
                navOptions
            )
            true
        }

        findPreference(Keys.lockScreen).setOnPreferenceClickListener {
            navController.navigate(
                R.id.action_main_to_lockScreen,
                null,
                navOptions
            )
            true
        }
    }
}