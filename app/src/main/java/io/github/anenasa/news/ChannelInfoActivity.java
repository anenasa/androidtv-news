package io.github.anenasa.news;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

public class ChannelInfoActivity extends AppCompatActivity {

    ChannelInfoFragment fragment = new ChannelInfoFragment();
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isNewChannel = getIntent().getExtras().getBoolean("isNewChannel");
        channelIndex = getIntent().getExtras().getInt("index");
        defaultUrl = getIntent().getExtras().getString("defaultUrl");
        defaultName = getIntent().getExtras().getString("defaultName");
        defaultFormat = getIntent().getExtras().getString("defaultFormat");
        defaultVolume = getIntent().getExtras().getFloat("defaultVolume");
        defaultHeader = getIntent().getExtras().getString("defaultHeader");
        customUrl = getIntent().getExtras().getString("customUrl");
        customName = getIntent().getExtras().getString("customName");
        customFormat = getIntent().getExtras().getString("customFormat");
        customVolume = getIntent().getExtras().getString("customVolume");
        customHeader = getIntent().getExtras().getString("customHeader");
        channelIsHidden = getIntent().getExtras().getBoolean("isHidden");
        channelWidth = getIntent().getExtras().getInt("width");
        channelHeight = getIntent().getExtras().getInt("height");
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("customName", fragment.activity.customName);
        returnIntent.putExtra("isHidden", fragment.prefIsHidden.isChecked());
        returnIntent.putExtra("customUrl", fragment.activity.customUrl);
        returnIntent.putExtra("customFormat", fragment.activity.customFormat);
        returnIntent.putExtra("customVolume", fragment.activity.customVolume);
        returnIntent.putExtra("customHeader", fragment.activity.customHeader);
        setResult(Activity.RESULT_OK, returnIntent);
        super.onBackPressed();
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
