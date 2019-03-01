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

        const val CUSTOM_CLOCK = "custom_clock"
        const val CLOCK_FORMAT = "clock_format"
        const val CUSTOM_QS_DATE_FORMAT = "custom_qs_date_format"
        const val QS_DATE_FORMAT = "qs_date_format"

        const val HEADER_COUNT_PORTRAIT = "header_count_portrait"
        const val HEADER_COUNT_LANDSCAPE = "header_count_landscape"

        const val OLD_RECENTS = "old_recents"
        const val NAV_HEIGHT = "nav_height"
        const val STATUS_BAR_HEIGHT = "status_bar_height"

        const val LEFT_SYSTEM_ICONS = "left_system_icons"
        const val HIDE_STATUS_BAR_CARRIER = "hide_status_bar_carrier"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    val customClock: Boolean
        get() = getBoolean(CUSTOM_CLOCK, false)

    val clockFormat: String
        get() = getString(CLOCK_FORMAT, "h:mm a")

    val customQsDateFormat: Boolean
        get() = getBoolean(CUSTOM_QS_DATE_FORMAT, false)

    val qsDateFormat: String
        get() = getString(QS_DATE_FORMAT, "EEEMMMMdd")

    val headerCountPortrait: Int
        get() = getInt(HEADER_COUNT_PORTRAIT, 6)

    val headerCountLandscape: Int
        get() = getInt(HEADER_COUNT_LANDSCAPE, 10)

    val oldRecents: Boolean
        get() = getBoolean(OLD_RECENTS, false)

    val navHeight: Float
        get() = getInt(NAV_HEIGHT, 480) / 10f

    val statusBarHeight: Float
        get() = getInt(STATUS_BAR_HEIGHT, 240) / 10f

    val leftSystemIcons: Boolean
        get() = getBoolean(LEFT_SYSTEM_ICONS, false)

    val hideStatusBarCarrier: Boolean
        get() = getBoolean(HIDE_STATUS_BAR_CARRIER, false)

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