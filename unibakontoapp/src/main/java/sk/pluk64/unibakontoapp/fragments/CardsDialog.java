package sk.pluk64.unibakontoapp.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;

import com.google.gson.Gson;

import java.util.List;

import sk.pluk64.unibakonto.CardInfo;
import sk.pluk64.unibakontoapp.R;

import static android.content.Context.MODE_PRIVATE;

public class CardsDialog extends DialogFragment {
    private Activity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            this.activity = (Activity) context;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.title_card);
        builder.setPositiveButton(R.string.ok, null);

        builder.setMessage(getFormattedCardsData());
        return builder.create();
    }

    public SpannableStringBuilder getFormattedCardsData() {
        SharedPreferences preferences = activity.getPreferences(MODE_PRIVATE);
        List<CardInfo> cardInfos = AccountFragment.loadCards(preferences, new Gson());
        SpannableStringBuilder resultBuilder = new SpannableStringBuilder();
        if (cardInfos != null) {
            for (CardInfo cardInfo : cardInfos) {
                SpannableString ss = new SpannableString(cardInfo.number);
                ss.setSpan(new RelativeSizeSpan(1.6f), 0, ss.length(), 0);

                resultBuilder.append(ss);
                resultBuilder.append(String.format("\n(%s: %s)\n\n", getString(R.string.valid_until), cardInfo.validUntil));
            }
        }
        if (resultBuilder.length() > 0) {
            resultBuilder.delete(resultBuilder.length() - 2, resultBuilder.length());
        } else {
            resultBuilder.append(getString(R.string.no_cards));
            resultBuilder.append("\n\n");
            resultBuilder.append(String.format("(%s)", getString(R.string.try_logout)));
        }
        return resultBuilder;
    }
}
