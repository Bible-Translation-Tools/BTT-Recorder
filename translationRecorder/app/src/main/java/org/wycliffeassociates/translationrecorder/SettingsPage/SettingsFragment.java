package org.wycliffeassociates.translationrecorder.SettingsPage;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync.ResyncLanguageNamesTask;
import org.wycliffeassociates.translationrecorder.TranslationRecorderApp;
import org.wycliffeassociates.translationrecorder.database.ProjectDatabaseHelper;
import org.wycliffeassociates.translationrecorder.utilities.TaskFragment;

import org.wycliffeassociates.translationrecorder.R;

/**
 * Created by leongv on 12/17/2015.
 */
public class SettingsFragment extends PreferenceFragment  implements SharedPreferences.OnSharedPreferenceChangeListener {

    private int FILE_GET_REQUEST_CODE = 45;

    LanguageSelector mParent;
    SharedPreferences mSharedPreferences;
    ProjectDatabaseHelper db;
    private TaskFragment mTaskFragment;
    private String TAG_TASK_FRAGMENT = "tag_task_fragment";

    interface LanguageSelector{
        void sourceLanguageSelected();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
        mSharedPreferences = getPreferenceScreen().getSharedPreferences();
        mParent = (LanguageSelector) getActivity();
        db = ((TranslationRecorderApp)getActivity().getApplication()).getDatabase();
        // Below is the code to clear the SharedPreferences. Use it wisely.
        // mSharedPreferences.edit().clear().commit();

        // Register listener(s)
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        FragmentManager fm = getFragmentManager();
        mTaskFragment = (TaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);
        if (mTaskFragment == null) {
            mTaskFragment = new TaskFragment();
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
            fm.executePendingTransactions();
        }

        Preference sourceLanguageButton = findPreference(Settings.KEY_PREF_GLOBAL_LANG_SRC);
        sourceLanguageButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mParent.sourceLanguageSelected();
                return true;
            }
        });

        Preference addTemporaryLanguageButton = findPreference(Settings.KEY_PREF_ADD_LANGUAGE);
        addTemporaryLanguageButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AddTargetLanguageDialog add = new AddTargetLanguageDialog();
                    add.show(getFragmentManager(), "add");
                    return false;
                }
            }
        );

        Preference updateLanguagesButton = findPreference(Settings.KEY_PREF_UPDATE_LANGUAGES);
        updateLanguagesButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Preference updateLanguagesUrlPref = findPreference(Settings.KEY_PREF_UPDATE_LANGUAGES_URL);
                String updateLanguagesUrl = updateLanguagesUrlPref.getSummary().toString();

                mTaskFragment.executeRunnable(
                        new ResyncLanguageNamesTask(1, getActivity(), db, updateLanguagesUrl),
                        "Updating Languages",
                        "Please wait...",
                        true
                );
                return true;
            }
        });

        Preference updateLanguagesFromFileButton = findPreference(Settings.KEY_PREF_UPDATE_LANGUAGES_FROM_FILE);
        updateLanguagesFromFileButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("application/octet-stream");
                startActivityForResult(intent, FILE_GET_REQUEST_CODE);
                return true;
            }
        });

        Preference languagesUrlButton = findPreference(Settings.KEY_PREF_LANGUAGES_URL);
        languagesUrlButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LanguagesUrlDialog add = new LanguagesUrlDialog();
                add.show(getFragmentManager(), "save");
                return false;
            }
        });

        Preference uploadServerButton = findPreference(Settings.KEY_PREF_UPLOAD_SERVER);
        uploadServerButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                UploadServerDialog add = new UploadServerDialog();
                add.show(getFragmentManager(), "save");
                return false;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == FILE_GET_REQUEST_CODE) {
                Uri uri = resultData.getData();
                mTaskFragment.executeRunnable(
                        new ResyncLanguageNamesTask(1, getActivity(), db, uri),
                        "Updating Languages",
                        "Please wait...",
                        true
                );
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get rid of the extra padding in the settings page body (where it loads this fragment)
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (v != null) {
            ListView lv = (ListView) v.findViewById(android.R.id.list);
            lv.setPadding(0, 0, 0, 0);
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        for (String k : mSharedPreferences.getAll().keySet()) {
            updateSummaryText(mSharedPreferences, k);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences mSharedPreferences, String key) {
        updateSummaryText(mSharedPreferences, key);
    }

    private void updateSummariesSetViaActivities(SharedPreferences mSharedPreferences){
        String uristring = mSharedPreferences.getString(Settings.KEY_PREF_GLOBAL_SOURCE_LOC, "");
        Uri dir = Uri.parse(uristring);
        if(dir != null) {
            uristring = dir.getLastPathSegment();
            //This removes "primary:", though maybe this is helpful in identifying between sd card and internal storage.
            //uristring = uristring.substring(uristring.indexOf(":")+1, uristring.length());
            findPreference(Settings.KEY_PREF_GLOBAL_SOURCE_LOC).setSummary(uristring);
        } else {
            findPreference(Settings.KEY_PREF_GLOBAL_SOURCE_LOC).setSummary(mSharedPreferences.getString(Settings.KEY_PREF_GLOBAL_SOURCE_LOC, ""));
        }
    }

    public void updateSummaryText(SharedPreferences mSharedPreferences, String key) {
        try {
            updateSummariesSetViaActivities(mSharedPreferences);
            String text  = mSharedPreferences.getString(key, "");
            if(findPreference(key) != null) {
                findPreference(key).setSummary(text);
            }
        } catch (ClassCastException err) {
            System.out.println("IGNORING SUMMARY UPDATE FOR " + key);
        }
    }
}