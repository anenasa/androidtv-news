package io.github.anenasa.news;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ChannelInfoActivity extends AppCompatActivity {

    ChannelInfoFragment fragment = new ChannelInfoFragment();
    int channelIndex;
    String defaultUrl;
    String defaultName;
    String defaultFormat;
    float defaultVolume;
    String customUrl;
    String customName;
    String customFormat;
    String customVolume;
    boolean channelIsHidden;
    int channelWidth;
    int channelHeight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        channelIndex = getIntent().getExtras().getInt("index");
        defaultUrl = getIntent().getExtras().getString("defaultUrl");
        defaultName = getIntent().getExtras().getString("defaultName");
        defaultFormat = getIntent().getExtras().getString("defaultFormat");
        defaultVolume = getIntent().getExtras().getFloat("defaultVolume");
        customUrl = getIntent().getExtras().getString("customUrl");
        customName = getIntent().getExtras().getString("customName");
        customFormat = getIntent().getExtras().getString("customFormat");
        customVolume = getIntent().getExtras().getString("customVolume");
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
            prefIsHidden.setChecked(activity.channelIsHidden);
            Preference prefNumber = findPreference("number");
            prefNumber.setSummary(String.valueOf(activity.channelIndex));
            EditTextPreference prefUrl = findPreference("url");
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
            prefSize.setSummary(activity.channelWidth + "×" + activity.channelHeight);
        }
    }
}
