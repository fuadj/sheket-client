package com.mukera.sheket.client;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.mukera.sheket.client.utils.PrefUtil;

/**
 * Created by fuad on 9/15/16.
 */
public class LanguageSelectionDialog {
    public static void displayLanguageConfigurationDialog(final Context context, boolean is_cancellable, final Runnable languageSelectedListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_first_time_configuration, null);
        builder.setView(view);
        final Button btnEnglish = (Button) view.findViewById(R.id.dialog_config_btn_english);
        final Button btnAmharic = (Button) view.findViewById(R.id.dialog_config_btn_amharic);

        builder.setTitle(R.string.placeholder_select_language);
        builder.setCancelable(is_cancellable);
        final AlertDialog dialog = builder.create();
        if (!is_cancellable)
            dialog.setCanceledOnTouchOutside(false);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selected_lang = -1;
                if (v.getId() == btnEnglish.getId()) {
                    selected_lang = PrefUtil.LANGUAGE_ENGLISH;
                } else if (v.getId() == btnAmharic.getId()) {
                    selected_lang = PrefUtil.LANGUAGE_AMHARIC;
                }

                dialog.dismiss();
                if (selected_lang == -1 ||
                        selected_lang == PrefUtil.getUserLanguageId(context)) {
                    return;
                }

                PrefUtil.setUserLanguage(context, selected_lang);

                if (languageSelectedListener != null)
                    languageSelectedListener.run();
            }
        };

        btnEnglish.setOnClickListener(listener);
        btnAmharic.setOnClickListener(listener);
        dialog.show();
    }
}
