package org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.wycliffeassociates.translationrecorder.database.ProjectDatabaseHelper;
import org.wycliffeassociates.translationrecorder.project.components.Language;
import org.wycliffeassociates.translationrecorder.project.ParseJSON;
import org.wycliffeassociates.translationrecorder.utilities.Task;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by sarabiaj on 12/14/2016.
 */

public class ResyncLanguageNamesTask extends Task {
    Context mCtx;
    ProjectDatabaseHelper db;
    boolean isLocal = false;
    Uri localFile;
    Handler handler;

    public ResyncLanguageNamesTask(int taskTag, Context ctx, ProjectDatabaseHelper db) {
        super(taskTag);
        mCtx = ctx;
        this.db = db;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public ResyncLanguageNamesTask(int taskTag, Context ctx, ProjectDatabaseHelper db, Uri uri) {
        this(taskTag, ctx, db);
        this.isLocal = true;
        this.localFile = uri;
    }

    @Override
    public void run() {
        String json = isLocal ? loadJsonFromFile() : loadJsonFromUrl();
        try {
            JSONArray jsonObject = new JSONArray(json);
            ParseJSON parseJSON = new ParseJSON(mCtx);
            Language[] languages = parseJSON.pullLangNames(jsonObject);
            db.addLanguages(languages);
            onTaskCompleteDelegator();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mCtx, "Languages successfully updated!", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (final JSONException e) {
            e.printStackTrace();
            onTaskErrorDelegator();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mCtx, "Invalid json file", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String loadJsonFromUrl() {
        try {
            URL url = new URL("http://td.unfoldingword.org/exports/langnames.json");
            HttpURLConnection urlConnection = null;

            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
                reader.close();
                return json.toString();
            } catch (final IOException e) {
                e.printStackTrace();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mCtx, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                return "";
            } finally {
                urlConnection.disconnect();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String loadJsonFromFile() {
        try (InputStream is = mCtx.getContentResolver().openInputStream(localFile)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();
            return json.toString();
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mCtx, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            onTaskErrorDelegator();
            return "";
        }
    }
}
