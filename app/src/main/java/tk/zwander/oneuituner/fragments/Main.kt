package tk.zwander.oneuituner.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import tk.zwander.oneuituner.R
import tk.zwander.oneuituner.util.Keys
import tk.zwander.oneuituner.util.navController
import tk.zwander.oneuituner.util.navOptions

class Main : PreferenceFragmentCompat() {
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
    }

    override fun onResume() {
        super.onResume()

        activity?.setTitle(R.string.app_name)
    }
}