package io.github.anenasa.news;

import androidx.activity.OnBackPressedCallback;
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
import android.widget.Toast;

import com.codekidlabs.storagechooser.StorageChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.stream.Collectors;

import okhttp3.Request;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity {

    String defaultFormat;
    String defaultVolume;
    boolean isShowErrorMessage;
    boolean enableBackgroundExtract;
    boolean invertChannelButtons;
    boolean hideNavigationBar;
    boolean hideStatusBar;
    boolean useExternalJS;
    boolean updateYtdlpOnStart;
    boolean ytdlpUpdated = false;
    final String TAG = "SettingsActivity";
    boolean remove_cache = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras == null) {
            Toast.makeText(this, "Error: intentExtras is null", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error: intentExtras is null");
            finish();
            return;
        }
        defaultFormat = intentExtras.getString("defaultFormat");
        defaultVolume = intentExtras.getString("defaultVolume");
        isShowErrorMessage = intentExtras.getBoolean("isShowErrorMessage");
        enableBackgroundExtract = intentExtras.getBoolean("enableBackgroundExtract");
        invertChannelButtons = intentExtras.getBoolean("invertChannelButtons");
        hideNavigationBar = intentExtras.getBoolean("hideNavigationBar");
        hideStatusBar = intentExtras.getBoolean("hideStatusBar");
        useExternalJS = intentExtras.getBoolean("useExternalJS");
        updateYtdlpOnStart = intentExtras.getBoolean("updateYtdlpOnStart");
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new SettingsFragment())
                .commit();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("defaultFormat", defaultFormat);
                returnIntent.putExtra("defaultVolume", defaultVolume);
                returnIntent.putExtra("isShowErrorMessage", isShowErrorMessage);
                returnIntent.putExtra("enableBackgroundExtract", enableBackgroundExtract);
                returnIntent.putExtra("invertChannelButtons", invertChannelButtons);
                returnIntent.putExtra("hideNavigationBar", hideNavigationBar);
                returnIntent.putExtra("hideStatusBar", hideStatusBar);
                returnIntent.putExtra("useExternalJS", useExternalJS);
                returnIntent.putExtra("updateYtdlpOnStart", updateYtdlpOnStart);
                returnIntent.putExtra("remove_cache", remove_cache);
                returnIntent.putExtra("ytdlpUpdated", ytdlpUpdated);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });
    }

    private void showFileChooser(String outputName) {
        StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(this)
                .withFragmentManager(getFragmentManager())
                .withMemoryBar(true)
                .allowCustomPath(true)
                .setType(StorageChooser.FILE_PICKER)
                .build();
        chooser.show();
        chooser.setOnSelectListener(path -> {
            try (InputStream inputStream = new FileInputStream(path)) {
                copyToExternal(inputStream, outputName);
            } catch (IOException e) {
                Toast.makeText(this, "檔案匯入失敗", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "showFileChooser error", e);
            }
        });
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SettingsActivity activity = (SettingsActivity) getActivity();
        String SAVE_FILE_NAME;

        private final ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        activity.showFileChooser(SAVE_FILE_NAME);
                    }
                });

        private final ActivityResultLauncher<String> selectFileLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri == null) return;
                    try (InputStream inputStream = activity.getContentResolver().openInputStream(uri)) {
                        if (inputStream == null) {
                            throw new IOException("inputStream is null");
                        }
                        activity.copyToExternal(inputStream, SAVE_FILE_NAME);
                    } catch (IOException e) {
                        Toast.makeText(activity, "檔案匯入失敗", Toast.LENGTH_SHORT).show();
                        Log.e(activity.TAG, "selectFileLauncher error", e);
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
                saveToFile("config.txt");
                return true;
            });

            EditTextPreference prefConfigurl = findPreference("configurl");
            assert prefConfigurl != null;
            prefConfigurl.setOnPreferenceChangeListener((preference, newValue) -> {
                File file = new File(requireActivity().getExternalFilesDir(null), "config.txt");
                // Without deleting first, when config.txt is already created with adb push,
                // writing will fail with "java.io.FileNotFoundException" "open failed: EACCES (Permission denied)"
                if (file.exists() && !file.delete()) {
                    Log.e(activity.TAG, "failed to delete file");
                }
                if (!newValue.toString().isEmpty()) {
                    try (FileOutputStream stream = new FileOutputStream(file)) {
                        stream.write(("{\"channelList\": [{\"list\": \"" + newValue + "\"}]}").getBytes());
                    } catch (IOException e) {
                        Log.e(activity.TAG, "prefConfigurl error", e);
                        preference.setSummary(e.getMessage());
                    }
                }
                return true;
            });

            Preference cookies = findPreference("cookies");
            assert cookies != null;
            cookies.setOnPreferenceClickListener(preference -> {
                saveToFile("cookies.txt");
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

            SwitchPreference prefUpdateYtdlpOnStart = findPreference("updateYtdlpOnStart");
            assert prefUpdateYtdlpOnStart != null;
            prefUpdateYtdlpOnStart.setChecked(activity.updateYtdlpOnStart);
            prefUpdateYtdlpOnStart.setOnPreferenceChangeListener((preference, newValue) -> {
                activity.updateYtdlpOnStart = (boolean) newValue;
                return true;
            });

            Preference update_ytdlp = findPreference("update_ytdlp");
            assert update_ytdlp != null;
            update_ytdlp.setSummary(YtDlp.version);
            update_ytdlp.setOnPreferenceClickListener(preference -> {
                update_ytdlp.setSummary("正在更新");
                new Thread(() -> {
                    try {
                        YtDlp.download(activity.getExternalFilesDir(null));
                        activity.runOnUiThread(() -> update_ytdlp.setSummary("已更新，將重新啟動應用程式"));
                        activity.ytdlpUpdated = true;
                    }
                    catch (IOException e) {
                        activity.runOnUiThread(() -> update_ytdlp.setSummary("更新時發生錯誤"));
                        Log.e(activity.TAG, "Preference update_ytdlp error", e);
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
                        Request request = new Request.Builder()
                                .url("https://raw.githubusercontent.com/anenasa/androidtv-news/main/VERSION")
                                .build();
                        String version_new;
                        try (Response response = MyApplication.okHttpClient.newCall(request).execute()) {
                            if (!response.isSuccessful()) {
                                throw new IOException("下載失敗：" + response);
                            }
                            version_new = response.body().string().trim();
                        }
                        if(!version_new.equals(BuildConfig.VERSION_NAME.split("-")[0])){
                            activity.runOnUiThread(() -> update.setSummary("有新版本"));
                            String apiString = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) ? "" : "-api21";
                            String link = String.format("https://github.com/anenasa/androidtv-news/releases/download/v%1$s/androidtv-news%2$s-%1$s.apk", version_new, apiString);
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                            startActivity(intent);
                        }
                        else{
                            activity.runOnUiThread(() -> update.setSummary("已經是最新版本"));
                        }
                    } catch (IOException e) {
                        activity.runOnUiThread(() -> update.setSummary("更新時發生錯誤"));
                        Log.e(activity.TAG, "Preference update error", e);
                    }
                }).start();
                return true;
            });

            Preference about = findPreference("about");
            assert about != null;
            about.setOnPreferenceClickListener(preference -> {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.format("新聞直播 %s 版", BuildConfig.VERSION_NAME)).append('\n')
                        .append("專案頁面：https://github.com/anenasa/androidtv-news").append('\n')
                        .append("授權：GNU General Public License v3.0").append('\n')
                        .append("函式庫：").append('\n')
                        .append("yt-dlp - The Unlicense").append('\n')
                        .append("Chaquopy - MIT License").append('\n')
                        .append("Node.js - MIT License").append('\n')
                        .append("QuickJS - MIT License").append('\n')
                        .append("ExoPlayer - Apache License 2.0").append('\n')
                        .append("OkHttp - Apache License 2.0").append('\n')
                        .append("Storage Chooser - Mozilla Public License Version 2.0").append('\n')
                        .append('\n');
                try{
                    try (InputStream stream = getResources().openRawResource(R.raw.gpl3)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        stringBuilder.append(reader.lines().collect(Collectors.joining("\n"))).append('\n');
                    }
                    stringBuilder.append("The Unlicense").append('\n');
                    try (InputStream stream = getResources().openRawResource(R.raw.unlicense)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        stringBuilder.append(reader.lines().collect(Collectors.joining("\n"))).append('\n');
                    }
                    stringBuilder.append("MIT License").append('\n');
                    try (InputStream stream = getResources().openRawResource(R.raw.chaquopy)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        stringBuilder.append(reader.lines().collect(Collectors.joining("\n"))).append('\n');
                    }
                    try (InputStream stream = getResources().openRawResource(R.raw.nodejs)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        stringBuilder.append(reader.lines().collect(Collectors.joining("\n"))).append('\n');
                    }
                    try (InputStream stream = getResources().openRawResource(R.raw.quickjs)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        stringBuilder.append(reader.lines().collect(Collectors.joining("\n"))).append('\n');
                    }
                    try (InputStream stream = getResources().openRawResource(R.raw.apache2)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        stringBuilder.append(reader.lines().collect(Collectors.joining("\n"))).append('\n');
                    }
                    try (InputStream stream = getResources().openRawResource(R.raw.mpl2)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        stringBuilder.append(reader.lines().collect(Collectors.joining("\n"))).append('\n');
                    }
                } catch (IOException e) {
                    Toast.makeText(activity, "讀取許可證失敗", Toast.LENGTH_SHORT).show();
                    Log.e(activity.TAG, "Read license error", e);
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

        void saveToFile(String outputName) {
            SAVE_FILE_NAME = outputName;

            // Check if file picker is available
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            PackageManager packageManager = requireActivity().getPackageManager();
            ComponentName componentName = intent.resolveActivity(packageManager);
            // Possible results of componentName if no third-party file picker is installed:
            // com.android.documentsui on non-Android TV
            // null on Android TV <= 10
            // com.google.android.tv.frameworkpackagestubs on Android TV 11
            // com.android.tv.frameworkpackagestubs on Android TV 12 and 13
            if(componentName != null && !componentName.getPackageName().endsWith("frameworkpackagestubs")){
                selectFileLauncher.launch("text/*");
            }
            else {
                // No file picker available
                // https://stackoverflow.com/a/38715569/20756028
                if(ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == -1) {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
                else{
                    activity.showFileChooser(outputName);
                }
            }
        }
    }

    void copyToExternal(InputStream inputStream, String outputName) throws IOException {
        File outputFile = new File(getExternalFilesDir(null), outputName);
        // Without deleting first, when output file is already created with adb push,
        // writing will fail with "java.io.FileNotFoundException" "open failed: EACCES (Permission denied)"
        if (outputFile.exists() && !outputFile.delete()) {
            Log.e(TAG, "failed to delete file");
        }
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
