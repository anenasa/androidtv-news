package io.github.anenasa.news;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SettingsActivity extends AppCompatActivity {

    String defaultFormat;
    String defaultVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        defaultFormat = getIntent().getExtras().getString("defaultFormat");
        defaultVolume = getIntent().getExtras().getString("defaultVolume");
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new SettingsFragment())
                .commit();
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("defaultFormat", defaultFormat);
        returnIntent.putExtra("defaultVolume", defaultVolume);
        setResult(Activity.RESULT_OK, returnIntent);
        super.onBackPressed();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SettingsActivity activity;
        final String TAG = "SettingsFragment";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            activity = (SettingsActivity)getActivity();
            setPreferencesFromResource(R.xml.activity_settings, rootKey);

            EditTextPreference prefFormat = findPreference("format");
            prefFormat.setSummary(activity.defaultFormat);
            prefFormat.setText(activity.defaultFormat);
            prefFormat.setOnPreferenceChangeListener((preference, newValue) -> {
                if(newValue.toString().isEmpty()){
                    activity.defaultFormat = "best";
                    preference.setSummary("best");
                }
                else {
                    activity.defaultFormat = newValue.toString();
                    preference.setSummary(newValue.toString());
                }
                return true;
            });

            EditTextPreference prefVolume = findPreference("volume");
            prefVolume.setSummary(activity.defaultVolume);
            prefVolume.setText(activity.defaultVolume);
            prefVolume.setOnPreferenceChangeListener((preference, newValue) -> {
                if(newValue.toString().isEmpty()){
                    activity.defaultVolume = "1.0";
                    preference.setSummary("1.0");
                }
                else {
                    activity.defaultVolume = newValue.toString();
                    preference.setSummary(newValue.toString());
                }
                return true;
            });

            Preference update = (Preference)findPreference("update");
            update.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    update.setSummary("正在檢查更新");
                    new Thread( new Runnable() {
                        @Override
                        public void run() {
                            try {
                                URL url = new URL("https://raw.githubusercontent.com/anenasa/androidtv-news/main/VERSION");
                                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                                urlConnection.setRequestMethod("GET");
                                urlConnection.setReadTimeout(10000);
                                urlConnection.setConnectTimeout(15000);
                                urlConnection.setDoOutput(true);
                                urlConnection.connect();
                                InputStream inputStream = url.openStream();
                                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                                String version_new = reader.readLine();
                                if(!version_new.equals(BuildConfig.VERSION_NAME.split("-")[0])){
                                    activity.runOnUiThread(() -> {
                                        update.setSummary("有新版本");
                                    });
                                    String link = "https://github.com/anenasa/androidtv-news/releases/tag/v" + version_new;
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                                    startActivity(intent);
                                }
                                else{
                                    activity.runOnUiThread(() -> {
                                        update.setSummary("已經是最新版本");
                                    });
                                }
                            } catch (IOException e) {
                                activity.runOnUiThread(() -> {
                                    update.setSummary("更新時發生錯誤");
                                });
                                Log.e(TAG, Log.getStackTraceString(e));
                            }
                        }
                    }).start();
                    return true;
                }
            });

            Preference about = (Preference)findPreference("about");
            about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    InputStream streamGPL3 = getResources().openRawResource(R.raw.gpl3);
                    InputStream streamApache2 = getResources().openRawResource(R.raw.apache2);
                    BufferedReader readerGPL3 = new BufferedReader(new InputStreamReader(streamGPL3));
                    BufferedReader readerApache2 = new BufferedReader(new InputStreamReader(streamApache2));
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(String.format("新聞直播 %s 版", BuildConfig.VERSION_NAME)).append('\n')
                            .append("專案頁面：https://github.com/anenasa/androidtv-news").append('\n')
                            .append("授權：GNU General Public License v3.0").append('\n')
                            .append("函式庫：").append('\n')
                            .append("youtubedl-android - GNU General Public License v3.0").append('\n')
                            .append("ExoPlayer - Apache License 2.0").append('\n')
                            .append("OkHttp - Apache License 2.0").append('\n')
                            .append('\n');
                    try{
                        for (String line; (line = readerGPL3.readLine()) != null; ) {
                            stringBuilder.append(line).append('\n');
                        }
                        for (String line; (line = readerApache2.readLine()) != null; ) {
                            stringBuilder.append(line).append('\n');
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(preference.getContext())
                            .setTitle("關於")
                            .setMessage(stringBuilder.toString())
                            .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                            AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    return true;
                }
            });
        }
    }
}
