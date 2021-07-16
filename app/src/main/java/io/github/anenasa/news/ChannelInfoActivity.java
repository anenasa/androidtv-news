package io.github.anenasa.news;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.os.Bundle;

public class ChannelInfoActivity extends AppCompatActivity {

    int index;
    String url;
    String name;
    String format;
    float volume;
    int width;
    int height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        index = getIntent().getExtras().getInt("index");
        url = getIntent().getExtras().getString("url");
        name = getIntent().getExtras().getString("name");
        format = getIntent().getExtras().getString("format");
        volume = getIntent().getExtras().getFloat("volume");
        width = getIntent().getExtras().getInt("width");
        height = getIntent().getExtras().getInt("height");
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new ChannelInfoFragment())
                .commit();
    }

    public static class ChannelInfoFragment extends PreferenceFragmentCompat {
        ChannelInfoActivity activity;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            activity = (ChannelInfoActivity) getActivity();
            setPreferencesFromResource(R.xml.activity_channel_info, rootKey);

            Preference prefName = (Preference)findPreference("name");
            prefName.setSummary((CharSequence) activity.name);
            Preference prefNumber = (Preference)findPreference("number");
            prefNumber.setSummary(String.valueOf(activity.index));
            Preference prefUrl = (Preference)findPreference("url");
            prefUrl.setSummary((CharSequence) activity.url);
            Preference prefFormat = (Preference)findPreference("format");
            prefFormat.setSummary((CharSequence) activity.format);
            Preference prefVolume = (Preference)findPreference("volume");
            prefVolume.setSummary(String.valueOf(activity.volume));
            Preference prefSize = (Preference)findPreference("size");
            prefSize.setSummary(activity.width + "×" + activity.height);
        }
    }
}
