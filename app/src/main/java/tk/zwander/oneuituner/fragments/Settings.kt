package tk.zwander.oneuituner.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import tk.zwander.oneuituner.R

class Settings : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }
}