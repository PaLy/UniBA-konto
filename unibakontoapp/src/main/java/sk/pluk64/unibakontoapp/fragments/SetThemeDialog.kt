package sk.pluk64.unibakontoapp.fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.DialogFragment
import sk.pluk64.unibakontoapp.MainActivity
import sk.pluk64.unibakontoapp.PreferencesKeys
import sk.pluk64.unibakontoapp.R

data class NightModeItem(val nightMode: Int, val text: String) {
    override fun toString(): String {
        return text
    }
}

class SetThemeDialog : DialogFragment() {
    lateinit var activity: MainActivity
        private set

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            activity = context
        }
    }

    private val preferences: SharedPreferences by lazy {
        activity.getPreferences(Context.MODE_PRIVATE)
    }

    private var nightMode: Int = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        nightMode = preferences.getInt(PreferencesKeys.THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        val nightModeOptions = ArrayList<NightModeItem>()
        when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q -> {
                nightModeOptions.add(NightModeItem(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, getString(R.string.system_theme)))
                nightModeOptions.add(NightModeItem(AppCompatDelegate.MODE_NIGHT_NO, getString(R.string.light)))
                nightModeOptions.add(NightModeItem(AppCompatDelegate.MODE_NIGHT_YES, getString(R.string.dark)))
            }
            android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.P -> {
                nightModeOptions.add(NightModeItem(AppCompatDelegate.MODE_NIGHT_NO, getString(R.string.light)))
                nightModeOptions.add(NightModeItem(AppCompatDelegate.MODE_NIGHT_YES, getString(R.string.dark)))
                nightModeOptions.add(NightModeItem(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, getString(R.string.system_theme)))
            }
            else -> {
                nightModeOptions.add(NightModeItem(AppCompatDelegate.MODE_NIGHT_NO, getString(R.string.light)))
                nightModeOptions.add(NightModeItem(AppCompatDelegate.MODE_NIGHT_YES, getString(R.string.dark)))
            }
        }
        val checkedItem = nightModeOptions.indexOfFirst { it.nightMode == nightMode }

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.set_theme)
        builder.setSingleChoiceItems(
            nightModeOptions.map { it.text }.toTypedArray(),
            checkedItem
        ) { dialog, i ->
            nightMode = nightModeOptions[i].nightMode
            preferences.edit().putInt(PreferencesKeys.THEME, nightMode).apply()
            dialog.cancel()
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }

        return builder.create()
    }
}
