package sk.pluk64.unibakontoapp.anychart.themes

import com.anychart.APIlib

interface AnyChartTheme {
    val themeJs: String
    val themeObject: String

    companion object {
        fun set(theme: AnyChartTheme) {
            APIlib.getInstance().addJSLine(theme.themeJs)
            APIlib.getInstance().addJSLine("anychart.theme(${theme.themeObject});")
        }
    }
}
