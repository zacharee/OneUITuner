package tk.zwander.oneuituner.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import tk.zwander.oneuituner.R

class Recents : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.recents, rootKey)
    }

    override fun onResume() {
        super.onResume()

        activity?.setTitle(R.string.recents)
    }
}