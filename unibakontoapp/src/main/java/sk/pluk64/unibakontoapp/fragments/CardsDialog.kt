package sk.pluk64.unibakontoapp.fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import sk.pluk64.unibakonto.CardInfo
import sk.pluk64.unibakontoapp.MainActivity
import sk.pluk64.unibakontoapp.PreferencesKeys
import sk.pluk64.unibakontoapp.R
import sk.pluk64.unibakontoapp.preferencesutils.getList

class CardsDialog : DialogFragment() {
    lateinit var activity: MainActivity
        private set

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is MainActivity) {
            activity = context
        }
    }

    private val preferences: SharedPreferences by lazy {
        activity.getPreferences(Context.MODE_PRIVATE)
    }

    private val formattedCardsData: SpannableStringBuilder
        get() {
            val cardInfos = preferences.getList(PreferencesKeys.CARDS, CardInfo.serializer())
            val resultBuilder = SpannableStringBuilder()
            for ((number, _, _, validUntil) in cardInfos) {
                val ss = SpannableString(number)
                ss.setSpan(RelativeSizeSpan(1.6f), 0, ss.length, 0)

                resultBuilder.append(ss)
                resultBuilder.append(String.format("\n(%s: %s)\n\n", getString(R.string.valid_until), validUntil))
            }
            if (resultBuilder.isNotEmpty()) {
                resultBuilder.delete(resultBuilder.length - 2, resultBuilder.length)
            } else {
                resultBuilder.append(getString(R.string.no_cards))
                resultBuilder.append("\n\n")
                resultBuilder.append(String.format("(%s)", getString(R.string.try_logout)))
            }
            return resultBuilder
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.title_card)
        builder.setPositiveButton(R.string.ok, null)

        builder.setMessage(formattedCardsData)
        return builder.create()
    }
}
