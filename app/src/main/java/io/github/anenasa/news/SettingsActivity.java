package io.github.anenasa.news;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new SettingsFragment())
                .commit();

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.activity_settings, rootKey);
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
