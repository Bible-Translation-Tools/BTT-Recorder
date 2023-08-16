package org.wycliffeassociates.translationrecorder.SettingsPage;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import org.wycliffeassociates.translationrecorder.R;

/**
 * Created by mxaln on 08/16/2023.
 */

public class LanguagesUrlDialog extends DialogFragment {

    SharedPreferences pref;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_languages_url, null);

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String currentServerName = pref.getString(Settings.KEY_PREF_LANGUAGES_URL, getString(R.string.pref_languages_url));

        final EditText url = view.findViewById(R.id.url);
        final Button saveButton = view.findViewById(R.id.save_button);
        final Button restoreButton = view.findViewById(R.id.restore_default);
        final Button cancelButton = view.findViewById(R.id.close_button);

        url.setText(currentServerName);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = url.getText().toString();

                if(name.length() > 0) {
                    pref.edit().putString(Settings.KEY_PREF_LANGUAGES_URL, name).commit();
                    dismiss();
                }
            }
        });

        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pref.edit().putString(Settings.KEY_PREF_LANGUAGES_URL, getString(R.string.pref_languages_url)).commit();
                dismiss();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        builder.setView(view);
        return builder.create();
    }
}
