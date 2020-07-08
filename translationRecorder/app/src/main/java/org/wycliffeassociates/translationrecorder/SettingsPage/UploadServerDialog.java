package org.wycliffeassociates.translationrecorder.SettingsPage;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import org.wycliffeassociates.translationrecorder.R;
import org.wycliffeassociates.translationrecorder.TranslationRecorderApp;
import org.wycliffeassociates.translationrecorder.database.ProjectDatabaseHelper;

/**
 * Created by sarabiaj on 12/14/2016.
 */

public class UploadServerDialog extends DialogFragment {

    SharedPreferences pref;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_upload_server, null);

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String currentServerName = pref.getString(Settings.KEY_PREF_UPLOAD_SERVER, "opentranslationtools.org");

        final EditText serverName = (EditText) view.findViewById(R.id.server_name);
        final Button saveButton = (Button) view.findViewById(R.id.save_button);
        final Button restoreButton = (Button) view.findViewById(R.id.restore_default);
        final Button cancelButton = (Button) view.findViewById(R.id.close_button);

        serverName.setText(currentServerName);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = serverName.getText().toString();

                if(name.length() > 0) {
                    pref.edit().putString(Settings.KEY_PREF_UPLOAD_SERVER, name).commit();
                    dismiss();
                }
            }
        });

        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pref.edit().putString(Settings.KEY_PREF_UPLOAD_SERVER, "opentranslationtools.org").commit();
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
