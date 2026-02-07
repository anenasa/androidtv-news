package io.github.anenasa.news;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.codekidlabs.storagechooser.StorageChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SettingsActivity extends AppCompatActivity {

    String defaultFormat;
    String defaultVolume;
    boolean isShowErrorMessage;
    boolean enableBackgroundExtract;
    boolean invertChannelButtons;
    boolean hideNavigationBar;
    boolean hideStatusBar;
    boolean useExternalJS;
    boolean ytdlpUpdated = false;
    final String TAG = "SettingsActivity";
    boolean remove_cache = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        defaultFormat = getIntent().getExtras().getString("defaultFormat");
        defaultVolume = getIntent().getExtras().getString("defaultVolume");
        isShowErrorMessage = getIntent().getExtras().getBoolean("isShowErrorMessage");
        enableBackgroundExtract = getIntent().getExtras().getBoolean("enableBackgroundExtract");
        invertChannelButtons = getIntent().getExtras().getBoolean("invertChannelButtons");
        hideNavigationBar = getIntent().getExtras().getBoolean("hideNavigationBar");
        hideStatusBar = getIntent().getExtras().getBoolean("hideStatusBar");
        useExternalJS = getIntent().getExtras().getBoolean("useExternalJS");
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
        returnIntent.putExtra("isShowErrorMessage", isShowErrorMessage);
        returnIntent.putExtra("enableBackgroundExtract", enableBackgroundExtract);
        returnIntent.putExtra("invertChannelButtons", invertChannelButtons);
        returnIntent.putExtra("hideNavigationBar", hideNavigationBar);
        returnIntent.putExtra("hideStatusBar", hideStatusBar);
        returnIntent.putExtra("useExternalJS", useExternalJS);
        returnIntent.putExtra("remove_cache", remove_cache);
        returnIntent.putExtra("ytdlpUpdated", ytdlpUpdated);
        setResult(Activity.RESULT_OK, returnIntent);
        super.onBackPressed();
    }

    private void showConfigChooser() {
        StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(this)
                .withFragmentManager(getFragmentManager())
                .withMemoryBar(true)
                .allowCustomPath(true)
                .setType(StorageChooser.FILE_PICKER)
                .build();
        chooser.show();
        chooser.setOnSelectListener(path -> {
            try {
                InputStream inputStream = new FileInputStream(path);
                copyConfig(inputStream);
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        });
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SettingsActivity activity;

        private ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        activity.showConfigChooser();
                    }
                });

        @SuppressLint("ObsoleteSdkInt")
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            activity = (SettingsActivity)getActivity();
            setPreferencesFromResource(R.xml.activity_settings, rootKey);

            Preference config = findPreference("config");
            assert config != null;
            config.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/*");
                PackageManager packageManager = requireActivity().getPackageManager();
                ComponentName componentName = intent.resolveActivity(packageManager);
                // Possible results of componentName:
                // com.android.documentsui on non-Android TV
                // null on Android TV <= 10
                // com.google.android.tv.frameworkpackagestubs on Android TV 11
                // com.android.tv.frameworkpackagestubs on Android TV 12 and 13
                if(componentName != null && !componentName.getPackageName().endsWith("frameworkpackagestubs")){
                    startActivityForResult(intent, 0);
                }
                else {
                    // SAF does not work on Android TV
                    // https://stackoverflow.com/a/38715569/20756028
                    if(ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == -1) {
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                    else{
                        activity.showConfigChooser();
                    }
                }
                return true;
            });

            EditTextPreference prefConfigurl = findPreference("configurl");
            assert prefConfigurl != null;
            prefConfigurl.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!newValue.toString().isEmpty()) {
                    try {
                        File file = new File(requireActivity().getExternalFilesDir(null), "config.txt");
                        // Without deleting first, when config.txt is already created with adb push,
                        // writing will fail with "java.io.FileNotFoundException" "open failed: EACCES (Permission denied)"
                        file.delete();
                        FileOutputStream stream = new FileOutputStream(file);
                        stream.write(("{\"channelList\": [{\"list\": \"" + newValue + "\"}]}").getBytes());
                    } catch (IOException e) {
                        Log.e(activity.TAG, Log.getStackTraceString(e));
                        preference.setSummary(e.toString());
                    }
                }
                return true;
            });

            EditTextPreference prefFormat = findPreference("format");
            assert prefFormat != null;
            prefFormat.setSummary(activity.defaultFormat);
            prefFormat.setText(activity.defaultFormat);
            prefFormat.setOnPreferenceChangeListener((preference, newValue) -> {
                if(newValue.toString().isEmpty()){
                    activity.defaultFormat = "bv*+ba/b";
                    preference.setSummary("bv*+ba/b");
                }
                else {
                    activity.defaultFormat = newValue.toString();
                    preference.setSummary(newValue.toString());
                }
                return true;
            });

            EditTextPreference prefVolume = findPreference("volume");
            assert prefVolume != null;
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

            SwitchPreference prefShowErrorMessage = findPreference("isShowErrorMessage");
            assert prefShowErrorMessage != null;
            prefShowErrorMessage.setChecked(activity.isShowErrorMessage);
            prefShowErrorMessage.setOnPreferenceChangeListener((preference, newValue) -> {
                activity.isShowErrorMessage = (boolean) newValue;
                return true;
            });

            SwitchPreference prefEnableBackgroundExtract = findPreference("enableBackgroundExtract");
            assert prefEnableBackgroundExtract != null;
            prefEnableBackgroundExtract.setChecked(activity.enableBackgroundExtract);
            prefEnableBackgroundExtract.setOnPreferenceChangeListener((preference, newValue) -> {
                activity.enableBackgroundExtract = (boolean) newValue;
                return true;
            });

            SwitchPreference prefInvertChannelButtons = findPreference("invertChannelButtons");
            assert prefInvertChannelButtons != null;
            prefInvertChannelButtons.setChecked(activity.invertChannelButtons);
            prefInvertChannelButtons.setOnPreferenceChangeListener((preference, newValue) -> {
                activity.invertChannelButtons = (boolean) newValue;
                return true;
            });

            SwitchPreference prefHideNavigationBar = findPreference("hideNavigationBar");
            assert prefHideNavigationBar != null;
            prefHideNavigationBar.setChecked(activity.hideNavigationBar);
            prefHideNavigationBar.setOnPreferenceChangeListener((preference, newValue) -> {
                activity.hideNavigationBar = (boolean) newValue;
                return true;
            });

            SwitchPreference prefHideStatusBar = findPreference("hideStatusBar");
            assert prefHideStatusBar != null;
            prefHideStatusBar.setChecked(activity.hideStatusBar);
            prefHideStatusBar.setOnPreferenceChangeListener((preference, newValue) -> {
                activity.hideStatusBar = (boolean) newValue;
                return true;
            });

            SwitchPreference prefUseExternalJS = findPreference("useExternalJS");
            assert prefUseExternalJS != null;
            prefUseExternalJS.setChecked(activity.useExternalJS);
            prefUseExternalJS.setOnPreferenceChangeListener((preference, newValue) -> {
                activity.useExternalJS = (boolean) newValue;
                return true;
            });

            Preference update_ytdlp = findPreference("update_ytdlp");
            assert update_ytdlp != null;
            update_ytdlp.setSummary(YtDlp.version);
            update_ytdlp.setOnPreferenceClickListener(preference -> {
                update_ytdlp.setSummary("正在更新");
                new Thread(() -> {
                    try {
                        YtDlp.download(activity);
                        activity.runOnUiThread(() -> update_ytdlp.setSummary("已更新，將重新啟動應用程式"));
                        activity.ytdlpUpdated = true;
                    }
                    catch (IOException e) {
                        activity.runOnUiThread(() -> update_ytdlp.setSummary("更新時發生錯誤"));
                        Log.e(activity.TAG, Log.getStackTraceString(e));
                    }
                }).start();
                return true;
            });

            Preference update = findPreference("update");
            assert update != null;
            update.setOnPreferenceClickListener(preference -> {
                update.setSummary("正在檢查更新");
                new Thread(() -> {
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
                            activity.runOnUiThread(() -> update.setSummary("有新版本"));
                            String link;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                link = "https://github.com/anenasa/androidtv-news/releases/download/v" + version_new + "/androidtv-news-" + version_new + ".apk";
                            }
                            else {
                                link = "https://github.com/anenasa/androidtv-news/releases/download/v" + version_new + "/androidtv-news-api21-" + version_new + ".apk";
                            }
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                            startActivity(intent);
                        }
                        else{
                            activity.runOnUiThread(() -> update.setSummary("已經是最新版本"));
                        }
                    } catch (IOException e) {
                        activity.runOnUiThread(() -> update.setSummary("更新時發生錯誤"));
                        Log.e(activity.TAG, Log.getStackTraceString(e));
                    }
                }).start();
                return true;
            });

            Preference about = findPreference("about");
            assert about != null;
            about.setOnPreferenceClickListener(preference -> {
                InputStream streamUnlicense = getResources().openRawResource(R.raw.unlicense);
                InputStream streamChaquopy = getResources().openRawResource(R.raw.chaquopy);
                InputStream streamQuickjs = getResources().openRawResource(R.raw.quickjs);
                InputStream streamApache2 = getResources().openRawResource(R.raw.apache2);
                InputStream streamMPL2 = getResources().openRawResource(R.raw.mpl2);
                BufferedReader readerUnlicense = new BufferedReader(new InputStreamReader(streamUnlicense));
                BufferedReader readerChaquopy = new BufferedReader(new InputStreamReader(streamChaquopy));
                BufferedReader readerQuickjs = new BufferedReader(new InputStreamReader(streamQuickjs));
                BufferedReader readerApache2 = new BufferedReader(new InputStreamReader(streamApache2));
                BufferedReader readerMPL2 = new BufferedReader(new InputStreamReader(streamMPL2));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.format("新聞直播 %s 版", BuildConfig.VERSION_NAME)).append('\n')
                        .append("專案頁面：https://github.com/anenasa/androidtv-news").append('\n')
                        .append("授權：GNU General Public License v3.0").append('\n')
                        .append("函式庫：").append('\n')
                        .append("yt-dlp - The Unlicense").append('\n')
                        .append("Chaquopy - MIT License").append('\n')
                        .append("QuickJS - MIT License").append('\n')
                        .append("ExoPlayer - Apache License 2.0").append('\n')
                        .append("OkHttp - Apache License 2.0").append('\n')
                        .append("Storage Chooser - Mozilla Public License Version 2.0").append('\n')
                        .append('\n');
                try{
                    stringBuilder.append("The Unlicense").append('\n');
                    for (String line; (line = readerUnlicense.readLine()) != null; ) {
                        stringBuilder.append(line).append('\n');
                    }
                    stringBuilder.append("MIT License").append('\n');
                    for (String line; (line = readerChaquopy.readLine()) != null; ) {
                        stringBuilder.append(line).append('\n');
                    }
                    for (String line; (line = readerQuickjs.readLine()) != null; ) {
                        stringBuilder.append(line).append('\n');
                    }
                    for (String line; (line = readerApache2.readLine()) != null; ) {
                        stringBuilder.append(line).append('\n');
                    }
                    for (String line; (line = readerMPL2.readLine()) != null; ) {
                        stringBuilder.append(line).append('\n');
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(preference.getContext())
                        .setTitle("關於")
                        .setMessage(stringBuilder.toString())
                        .setPositiveButton("確定", (dialog, id) -> dialog.dismiss());
                        AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return true;
            });

            Preference prefRemoveCache = findPreference("remove_cache");
            assert prefRemoveCache != null;
            prefRemoveCache.setOnPreferenceClickListener(preference -> {
                activity.remove_cache = true;
                preference.setSummary("將移除快取");
                return true;
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            Uri uri = resultData.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                copyConfig(inputStream);
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    void copyConfig(InputStream inputStream) throws IOException {
        File outputFile = new File(getExternalFilesDir(null), "config.txt");
        // Without deleting first, when config.txt is already created with adb push,
        // writing will fail with "java.io.FileNotFoundException" "open failed: EACCES (Permission denied)"
        outputFile.delete();
        OutputStream outputStream = new FileOutputStream(outputFile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            outputStream.write(buf, 0, len);
        }
        inputStream.close();
        outputStream.close();
    }
}
