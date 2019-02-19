package tk.zwander.oneuituner.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import tk.zwander.oneuituner.R

class QS : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.qs, rootKey)
    }
}