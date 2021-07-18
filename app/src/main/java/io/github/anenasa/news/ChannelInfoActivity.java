package io.github.anenasa.news;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ChannelInfoActivity extends AppCompatActivity {

    ChannelInfoFragment fragment = new ChannelInfoFragment();
    int channelIndex;
    String channelUrl;
    String channelName;
    String channelFormat;
    float channelVolume;
    boolean channelIsHidden;
    int channelWidth;
    int channelHeight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        channelIndex = getIntent().getExtras().getInt("index");
        channelUrl = getIntent().getExtras().getString("url");
        channelName = getIntent().getExtras().getString("name");
        channelFormat = getIntent().getExtras().getString("format");
        channelVolume = getIntent().getExtras().getFloat("volume");
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
        returnIntent.putExtra("isHidden", fragment.prefIsHidden.isChecked());
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

            Preference prefName = (Preference)findPreference("name");
            prefName.setSummary((CharSequence) activity.channelName);
            prefIsHidden = findPreference("isHidden");
            prefIsHidden.setChecked(activity.channelIsHidden);
            Preference prefNumber = (Preference)findPreference("number");
            prefNumber.setSummary(String.valueOf(activity.channelIndex));
            Preference prefUrl = (Preference)findPreference("url");
            prefUrl.setSummary((CharSequence) activity.channelUrl);
            Preference prefFormat = (Preference)findPreference("format");
            prefFormat.setSummary((CharSequence) activity.channelFormat);
            Preference prefVolume = (Preference)findPreference("volume");
            prefVolume.setSummary(String.valueOf(activity.channelVolume));
            Preference prefSize = (Preference)findPreference("size");
            prefSize.setSummary(activity.channelWidth + "×" + activity.channelHeight);
        }
    }
}
