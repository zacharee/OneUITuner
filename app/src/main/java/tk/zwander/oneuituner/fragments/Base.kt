package tk.zwander.oneuituner.fragments

import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

abstract class Base : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    internal abstract val title: Int

    internal val keysToSync = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()

        activity?.setTitle(title)
        keysToSync.forEach { syncSummary(it) }
    }

    @CallSuper
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        syncSummary(key)
    }

    override fun onDestroy() {
        super.onDestroy()

        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    internal fun syncSummary(key: String?) {
        key?.let {
            if (keysToSync.contains(it)) {
                findPreference(it)?.apply {
                    when {
                        this is ListPreference -> {
                            summary = entry
                        }
                        else -> {
                            summary = preferenceManager.sharedPreferences.all[it]?.toString()
                        }
                    }
                }
            }
        }
    }
}