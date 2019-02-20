package tk.zwander.oneuituner.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class PrefManager private constructor(private val context: Context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: PrefManager? = null

        fun getInstance(context: Context): PrefManager {
            if (instance == null) instance = PrefManager(context.applicationContext)

            return instance!!
        }

        const val STOCK_CLOCK = "stock_clock"
        const val AM_PM = "am_pm"

        const val HEADER_COUNT_PORTRAIT = "header_count_portrait"
        const val HEADER_COUNT_LANDSCAPE = "header_count_landscape"

        const val OLD_RECENTS = "old_recents"
        const val NAV_HEIGHT = "nav_height"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    val stockClock: Boolean
        get() = getBoolean(STOCK_CLOCK, false)

    val amPmStyle: String
        get() = getString(AM_PM, "gone")

    val headerCountPortrait: Int
        get() = getInt(HEADER_COUNT_PORTRAIT, 6)

    val headerCountLandscape: Int
        get() = getInt(HEADER_COUNT_LANDSCAPE, 10)

    val oldRecents: Boolean
        get() = getBoolean(OLD_RECENTS, false)

    val navHeight: Float
        get() = getInt(NAV_HEIGHT, 480) / 10f

    fun getInt(key: String, def: Int = 0) = prefs.getInt(key, def)
    fun getString(key: String, def: String): String = prefs.getString(key, def) ?: def
    fun getBoolean(key: String, def: Boolean = false) = prefs.getBoolean(key, def)

    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()
    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
            prefs.registerOnSharedPreferenceChangeListener(listener)

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
}