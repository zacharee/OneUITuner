package tk.zwander.oneuituner.fragments

import android.os.Bundle
import tk.zwander.oneuituner.R
import tk.zwander.oneuituner.util.PrefManager

class Clock : Base() {
    override val title = R.string.clock

    init {
        keysToSync.add(PrefManager.CLOCK_FORMAT)
        keysToSync.add(PrefManager.QS_DATE_FORMAT)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.clock, rootKey)
    }
}