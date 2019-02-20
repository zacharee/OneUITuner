package tk.zwander.oneuituner.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import tk.zwander.oneuituner.R

class Misc : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.misc, rootKey)
    }

    override fun onResume() {
        super.onResume()

        activity?.setTitle(R.string.misc)
    }
}