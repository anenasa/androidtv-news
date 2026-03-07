package io.github.anenasa.news;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class ChannelInfoActivity extends AppCompatActivity {

    ChannelInfoFragment fragment = new ChannelInfoFragment();
    final String TAG = "ChannelInfoActivity";
    boolean isNewChannel;
    int channelIndex;
    String defaultUrl;
    String defaultName;
    String defaultFormat;
    float defaultVolume;
    String defaultHeader;
    String customUrl;
    String customName;
    String customFormat;
    String customVolume;
    String customHeader;
    boolean channelIsHidden;
    int channelWidth;
    int channelHeight;
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
        isNewChannel = intentExtras.getBoolean("isNewChannel");
        channelIndex = intentExtras.getInt("index");
        defaultUrl = intentExtras.getString("defaultUrl");
        defaultName = intentExtras.getString("defaultName");
        defaultFormat = intentExtras.getString("defaultFormat");
        defaultVolume = intentExtras.getFloat("defaultVolume");
        defaultHeader = intentExtras.getString("defaultHeader");
        customUrl = intentExtras.getString("customUrl");
        customName = intentExtras.getString("customName");
        customFormat = intentExtras.getString("customFormat");
        customVolume = intentExtras.getString("customVolume");
        customHeader = intentExtras.getString("customHeader");
        channelIsHidden = intentExtras.getBoolean("isHidden");
        channelWidth = intentExtras.getInt("width");
        channelHeight = intentExtras.getInt("height");
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("customName", fragment.activity.customName);
                returnIntent.putExtra("isHidden", fragment.prefIsHidden.isChecked());
                returnIntent.putExtra("customUrl", fragment.activity.customUrl);
                returnIntent.putExtra("customFormat", fragment.activity.customFormat);
                returnIntent.putExtra("customVolume", fragment.activity.customVolume);
                returnIntent.putExtra("customHeader", fragment.activity.customHeader);
                returnIntent.putExtra("remove_cache", fragment.activity.remove_cache);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });
    }

    public static class ChannelInfoFragment extends PreferenceFragmentCompat {
        ChannelInfoActivity activity;
        SwitchPreferenceCompat prefIsHidden;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            activity = (ChannelInfoActivity) getActivity();
            setPreferencesFromResource(R.xml.activity_channel_info, rootKey);

            EditTextPreference prefName = findPreference("name");
            assert prefName != null;
            if(activity.customName.isEmpty()){
                prefName.setSummary(activity.defaultName);
                prefName.setText(activity.defaultName);
            }
            else{
                prefName.setSummary(activity.customName);
                prefName.setText(activity.customName);
            }
            prefName.setOnPreferenceChangeListener((preference, newValue) -> {
                activity.customName = newValue.toString();
                if(newValue.toString().isEmpty()){
                    preference.setSummary(activity.defaultName);
                }
                else {
                    preference.setSummary(newValue.toString());
                }
                return true;
            });
            prefIsHidden = findPreference("isHidden");
            assert prefIsHidden != null;
            prefIsHidden.setChecked(activity.channelIsHidden);
            Preference prefNumber = findPreference("number");
            assert prefNumber != null;
            prefNumber.setSummary(String.valueOf(activity.channelIndex));
            EditTextPreference prefUrl = findPreference("url");
            assert prefUrl != null;
            if(activity.customUrl.isEmpty()){
                prefUrl.setSummary(activity.defaultUrl);
                prefUrl.setText(activity.defaultUrl);
            }
            else{
                prefUrl.setSummary(activity.customUrl);
                prefUrl.setText(activity.customUrl);
            }
            prefUrl.setOnPreferenceChangeListener((preference, newValue) -> {
                activity.customUrl = newValue.toString();
                if(newValue.toString().isEmpty()){
                    preference.setSummary(activity.defaultUrl);
                }
                else {
                    preference.setSummary(newValue.toString());
                }
                return true;
            });
            EditTextPreference prefFormat = findPreference("format");
            assert prefFormat != null;
            if(activity.customFormat.isEmpty()){
                prefFormat.setSummary(activity.defaultFormat);
                prefFormat.setText(activity.defaultFormat);
            }
            else{
                prefFormat.setSummary(activity.customFormat);
                prefFormat.setText(activity.customFormat);
            }
            prefFormat.setOnPreferenceChangeListener((preference, newValue) -> {
                activity.customFormat = newValue.toString();
                if(newValue.toString().isEmpty()){
                    preference.setSummary(activity.defaultFormat);
                }
                else {
                    preference.setSummary(newValue.toString());
                }
                return true;
            });
            EditTextPreference prefVolume = findPreference("volume");
            assert prefVolume != null;
            if(activity.customVolume.isEmpty()){
                prefVolume.setSummary(String.valueOf(activity.defaultVolume));
                prefVolume.setText(String.valueOf(activity.defaultVolume));
            }
            else{
                prefVolume.setSummary(activity.customVolume);
                prefVolume.setText(activity.customVolume);
            }
            prefVolume.setOnPreferenceChangeListener((preference, newValue) -> {
                activity.customVolume = newValue.toString();
                if(newValue.toString().isEmpty()){
                    preference.setSummary(String.valueOf(activity.defaultVolume));
                }
                else {
                    preference.setSummary(newValue.toString());
                }
                return true;
            });
            Preference prefSize = findPreference("size");
            assert prefSize != null;
            prefSize.setSummary(activity.channelWidth + "×" + activity.channelHeight);
            EditTextPreference prefHeader = findPreference("header");
            assert prefHeader != null;
            if(activity.customHeader.isEmpty()){
                prefHeader.setSummary(activity.defaultHeader);
                prefHeader.setText(activity.defaultHeader);
            }
            else{
                prefHeader.setSummary(activity.customHeader);
                prefHeader.setText(activity.customHeader);
            }
            prefHeader.setOnPreferenceChangeListener((preference, newValue) -> {
                activity.customHeader = newValue.toString();
                if(newValue.toString().isEmpty()){
                    preference.setSummary(activity.defaultHeader);
                }
                else {
                    preference.setSummary(newValue.toString());
                }
                return true;
            });
            Preference prefRemoveCache = findPreference("remove_cache");
            assert prefRemoveCache != null;
            prefRemoveCache.setOnPreferenceClickListener(preference -> {
                activity.remove_cache = true;
                preference.setSummary("將移除快取");
                return true;
            });
            Preference prefDelete = findPreference("delete");
            assert prefDelete != null;
            prefDelete.setVisible(activity.isNewChannel);
            prefDelete.setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                        .setTitle("刪除")
                        .setMessage("確定要刪除嗎？")
                        .setPositiveButton("確定", (dialog, id) -> {
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("delete", true);
                            activity.setResult(Activity.RESULT_OK, returnIntent);
                            activity.finish();
                        })
                        .setNegativeButton("取消", (dialogInterface, i) -> {});
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return true;
            });
        }
    }
}
