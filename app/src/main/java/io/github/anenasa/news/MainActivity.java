package io.github.anenasa.news;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";

    int channelNum;
    Channel[] channel;
    int channelLength_config;
    String input = "";
    String defaultFormat;
    String defaultVolume;

    SimpleExoPlayer player = null;
    SurfaceView playerView = null;
    TextView textView;

    Timer timer;
    TimerTask timerTask;

    SharedPreferences preferences;

    Handler mHandler = new Handler(Looper.getMainLooper());
    boolean isStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences("io.github.anenasa.news", MODE_PRIVATE);
        channelNum = preferences.getInt("channelNum", 0);
        defaultFormat = preferences.getString("defaultFormat", "best");
        defaultVolume = preferences.getString("defaultVolume", "1.0");

        try {
            YoutubeDL.getInstance().init(getApplication());
        } catch (YoutubeDLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }
        player = new SimpleExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.e(TAG, Log.getStackTraceString(error));
                Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                if(channel[channelNum].needParse() != Channel.NEEDPARSE_NO) {
                    // Force parse by removing video url
                    channel[channelNum].setVideo("");
                }
                play(channelNum);
            }
        });
        playerView = findViewById(R.id.playerView);
        player.setVideoSurfaceView(playerView);
        textView = findViewById(R.id.textView);

        readChannelList();
        if(channel == null){
            return;
        }
        if(channelNum >= channel.length){
            channelNum = 0;
        }

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                for (Channel value : channel) {
                    if (!value.isHidden()) {
                        try {
                            value.parse();
                        } catch (IOException | YoutubeDLException | JSONException | InterruptedException e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                }
                saveSettings();
            }
        };
        // Delay timer for one second because if two requests are sent to
        // Hami Video at the same time, one of them will fail.
        timer.scheduleAtFixedRate(timerTask, 1000, 3600000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(channel == null){
            return;
        }
        isStarted = true;
        play(channelNum);
    }

    void readChannelList() {
        try {
            File customFile = new File(getExternalFilesDir(null), "config.txt");
            InputStream inputStream;
            if(customFile.exists()){
                inputStream = new FileInputStream(customFile);
            }
            else{
                File customUrl = new File(getExternalFilesDir(null), "url.txt");
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                URL url;
                if(customUrl.exists()){
                    InputStream urlStream = new FileInputStream(customUrl);
                    BufferedReader urlReader = new BufferedReader(new InputStreamReader(urlStream));
                    url = new URL(urlReader.readLine());
                }
                else{
                    url = new URL("https://anenasa.github.io/channel/config.txt");
                }
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setDoOutput(true);
                urlConnection.connect();
                inputStream = url.openStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                stringBuilder.append(line).append('\n');
            }
            JSONObject json = new JSONObject(stringBuilder.toString());
            JSONArray channelList = json.getJSONArray("channelList");
            channelLength_config = channelList.length();

            JSONObject customJsonObject = null;
            JSONObject customChannelList = null;
            if(!preferences.getString("jsonSettings", "").isEmpty()) {
                customJsonObject = new JSONObject(preferences.getString("jsonSettings", ""));
                customChannelList = customJsonObject.getJSONObject("customChannelList");
                JSONArray newChannelArray = customJsonObject.getJSONArray("newChannelArray");
                channel = new Channel[channelList.length() + newChannelArray.length()];
                for(int i = channelList.length(); i < channel.length; i++){
                    JSONObject newChannelObject = newChannelArray.getJSONObject(i - channelList.length());
                    channel[i] = new Channel(i, "", "", defaultFormat, Float.parseFloat(defaultVolume), "");
                    channel[i].setUrl(newChannelObject.getString("customUrl"));
                    channel[i].setName(newChannelObject.getString("customName"));
                    channel[i].setFormat(newChannelObject.getString("customFormat"));
                    channel[i].setVolume(newChannelObject.getString("customVolume"));
                    channel[i].setHeader(newChannelObject.getString("customHeader"));
                    channel[i].setHidden(newChannelObject.getBoolean("isHidden"));
                }
            }
            else {
                channel = new Channel[channelList.length()];
            }
            for(int i = 0; i < channelList.length(); i++){

                JSONObject channelObject = channelList.getJSONObject(i);
                String url = channelObject.getString("url");
                String name = channelObject.getString("name");
                String format;
                if(channelObject.isNull("ytdl-format")) {
                    format = defaultFormat;
                }
                else{
                    format = channelObject.getString("ytdl-format");
                }
                float volume;
                if(channelObject.isNull("volume")) {
                    volume = Float.parseFloat(defaultVolume);
                }
                else{
                    volume = (float) channelObject.getDouble("volume");
                }
                String header;
                if(channelObject.isNull("header")) {
                    header = "";
                }
                else{
                    header = channelObject.getString("header");
                }
                channel[i] = new Channel(i, url, name, format, volume, header);
                if(customJsonObject != null && customChannelList.has(name)) {
                    JSONObject customChannelObject = customChannelList.getJSONObject(name);
                    channel[i].setUrl(customChannelObject.getString("customUrl"));
                    channel[i].setName(customChannelObject.getString("customName"));
                    channel[i].setFormat(customChannelObject.getString("customFormat"));
                    channel[i].setVolume(customChannelObject.getString("customVolume"));
                    channel[i].setHeader(customChannelObject.getString("customHeader"));
                    channel[i].setHidden(customChannelObject.getBoolean("isHidden"));
                }
                channel[i].setVideo(preferences.getString(channel[i].getUrl() + channel[i].getFormat(), ""));
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            channel = null;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("頻道清單讀取失敗");
            builder.setMessage(e.toString());
            builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    void play(int num)
    {
        new Thread( new Runnable() {
            @Override
            public void run() {
                if(channel[num].needParse() == Channel.NEEDPARSE_YES) {
                    try {
                        channel[num].parse();
                    } catch (JSONException | IOException | YoutubeDLException | InterruptedException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
                MediaItem mediaItem = MediaItem.fromUri(channel[num].getVideo());
                Map<String, String> map = new HashMap<>();
                if(!channel[num].getHeader().isEmpty()) {
                    String[] header = channel[num].getHeader().split(":", 2);
                    if(header.length != 2){
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "header 格式錯誤", Toast.LENGTH_LONG).show();
                        });
                    }
                    else {
                        map.put(header[0], header[1]);
                    }
                }
                DataSource.Factory factory = new DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(map);
                MediaSource mediaSource = new DefaultMediaSourceFactory(factory)
                        .createMediaSource(mediaItem);
                // player needs to run on main thread
                mHandler.post(new Runnable() {
                    @Override
                    public void run () {
                        if(num != channelNum){
                            // Already switched to another channel, do not play this
                            return;
                        }
                        player.setMediaSource(mediaSource);
                        player.prepare();
                        player.setVolume(channel[num].getVolume());
                        if(isStarted){
                            player.play();
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            // ChannelListActivity
            if (resultCode == Activity.RESULT_OK) {
                channelNum = data.getIntExtra("channelNum", 0);
                player.stop();
                play(channelNum);
            }
        }
        else if(requestCode == 1){
            // ChannelInfoActivity - existing channel
            if (resultCode == Activity.RESULT_OK) {
                if(data.getBooleanExtra("delete", false)){
                    Channel[] array = new Channel[channel.length - 1];
                    for(int i = 0, k = 0; i < array.length; i++){
                        if(i != channelNum){
                            array[k] = channel[i];
                            k++;
                        }
                    }
                    channel = array;
                    channelNum = 0;
                    return;
                }

                String url_old = channel[channelNum].getUrl();
                String url_new;
                if(data.getStringExtra("customUrl").isEmpty()) {
                    url_new = channel[channelNum].defaultUrl;
                }
                else{
                    url_new = data.getStringExtra("customUrl");
                }
                String format_old = channel[channelNum].getFormat();
                String format_new;
                if(data.getStringExtra("customFormat").isEmpty()){
                    format_new = channel[channelNum].defaultFormat;
                }
                else{
                    format_new = data.getStringExtra("customFormat");
                }
                if(!url_old.equals(url_new) || !format_old.equals(format_new)){
                    channel[channelNum].setVideo("");
                }
                channel[channelNum].setName(data.getStringExtra("customName"));
                channel[channelNum].setHidden(data.getBooleanExtra("isHidden", false));
                channel[channelNum].setUrl(data.getStringExtra("customUrl"));
                channel[channelNum].setFormat(data.getStringExtra("customFormat"));
                channel[channelNum].setVolume(data.getStringExtra("customVolume"));
                channel[channelNum].setHeader(data.getStringExtra("customHeader"));
                saveSettings();
            }
        }
        else if (requestCode == 2) {
            // SettingsActivity
            if (resultCode == Activity.RESULT_OK) {
                String newFormat = data.getStringExtra("defaultFormat");
                String newVolume = data.getStringExtra("defaultVolume");
                if(!defaultFormat.equals(newFormat) || !defaultVolume.equals(newVolume)){
                    defaultFormat = newFormat;
                    defaultVolume = newVolume;
                    readChannelList();
                    play(channelNum);
                }
            }
        }
        else if(requestCode == 3){
            // ChannelInfoActivity - new channel
            if (resultCode == Activity.RESULT_OK) {
                if(data.getBooleanExtra("delete", false)){
                    return;
                }
                channel = Arrays.copyOf(channel, channel.length + 1);
                channelNum = channel.length - 1;
                channel[channelNum] = new Channel(channelNum, "", "", defaultFormat, Float.parseFloat(defaultVolume), "");
                channel[channelNum].setName(data.getStringExtra("customName"));
                channel[channelNum].setHidden(data.getBooleanExtra("isHidden", false));
                channel[channelNum].setUrl(data.getStringExtra("customUrl"));
                channel[channelNum].setFormat(data.getStringExtra("customFormat"));
                channel[channelNum].setVolume(data.getStringExtra("customVolume"));
                channel[channelNum].setHeader(data.getStringExtra("customHeader"));
                saveSettings();
                play(channelNum);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent (KeyEvent event){
        if(event.getAction() == KeyEvent.ACTION_DOWN && getSupportFragmentManager().getFragments().isEmpty()) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if(input.equals("")){
                        showMenu();
                    }
                    else if(Integer.parseInt(input) < channel.length){
                        channelNum = Integer.parseInt(input);
                        player.stop();
                        play(channelNum);
                        clearInput();
                    }
                    else{
                        clearInput();
                    }
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if(!input.equals("")){
                        clearInput();
                    }
                    else{
                        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                .setTitle("退出")
                                .setMessage("是否要退出新聞直播？")
                                .setPositiveButton("確定", (dialog, id) -> finish())
                                .setNegativeButton("取消", (dialogInterface, i) -> {});
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_CHANNEL_UP:
                    do {
                        if (channelNum == 0) {
                            channelNum = channel.length - 1;
                        } else {
                            channelNum -= 1;
                        }
                    } while (channel[channelNum].isHidden());
                    player.stop();
                    play(channelNum);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    do {
                        if (channelNum == channel.length - 1) {
                            channelNum = 0;
                        } else {
                            channelNum += 1;
                        }
                    } while (channel[channelNum].isHidden());
                    player.stop();
                    play(channelNum);
                    return true;
                case KeyEvent.KEYCODE_INFO:
                    showChannelInfo(findViewById(R.id.container));
                    return true;
                case KeyEvent.KEYCODE_MENU:
                    showSettings(findViewById(R.id.container));
                    return true;
                case KeyEvent.KEYCODE_0:
                    appendInput(0);
                    return true;
                case KeyEvent.KEYCODE_1:
                    appendInput(1);
                    return true;
                case KeyEvent.KEYCODE_2:
                    appendInput(2);
                    return true;
                case KeyEvent.KEYCODE_3:
                    appendInput(3);
                    return true;
                case KeyEvent.KEYCODE_4:
                    appendInput(4);
                    return true;
                case KeyEvent.KEYCODE_5:
                    appendInput(5);
                    return true;
                case KeyEvent.KEYCODE_6:
                    appendInput(6);
                    return true;
                case KeyEvent.KEYCODE_7:
                    appendInput(7);
                    return true;
                case KeyEvent.KEYCODE_8:
                    appendInput(8);
                    return true;
                case KeyEvent.KEYCODE_9:
                    appendInput(9);
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    void appendInput(int num) {
        input += num;
        textView.setText(input);
    }

    void clearInput() {
        input = "";
        textView.setText("");
    }

    public void showMenu(){
        MenuFragment fragment = new MenuFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(fragment.getClass().getName())
                .commit();
    }

    public void showChannelList(View view){
        Intent intent = new Intent(this, ChannelListActivity.class);
        String[] nameArray = new String[channel.length];
        boolean[] isHiddenArray = new boolean[channel.length];
        for(int i = 0; i < channel.length; i++){
            nameArray[i] = channel[i].getName();
            isHiddenArray[i] = channel[i].isHidden();
        }
        intent.putExtra("nameArray", nameArray);
        intent.putExtra("isHiddenArray", isHiddenArray);
        startActivityForResult(intent, 0);
        getSupportFragmentManager().popBackStack();
    }

    public void showChannelInfo(View view){
        Intent intent = new Intent(this, ChannelInfoActivity.class);
        intent.putExtra("isNewChannel", channelNum >= channelLength_config);
        intent.putExtra("index", channel[channelNum].getIndex());
        intent.putExtra("defaultUrl", channel[channelNum].defaultUrl);
        intent.putExtra("defaultName", channel[channelNum].defaultName);
        intent.putExtra("defaultFormat", channel[channelNum].defaultFormat);
        intent.putExtra("defaultVolume", channel[channelNum].defaultVolume);
        intent.putExtra("defaultHeader", channel[channelNum].defaultHeader);
        intent.putExtra("customUrl", channel[channelNum].customUrl);
        intent.putExtra("customName", channel[channelNum].customName);
        intent.putExtra("customFormat", channel[channelNum].customFormat);
        intent.putExtra("customVolume", channel[channelNum].customVolume);
        intent.putExtra("customHeader", channel[channelNum].customHeader);
        intent.putExtra("isHidden", channel[channelNum].isHidden());
        intent.putExtra("width", player.getVideoSize().width);
        intent.putExtra("height", player.getVideoSize().height);
        startActivityForResult(intent, 1);
        getSupportFragmentManager().popBackStack();
    }

    public void addNewChannel(View view) {
        Intent intent = new Intent(this, ChannelInfoActivity.class);
        intent.putExtra("isNewChannel", true);
        intent.putExtra("index", channel.length);
        intent.putExtra("defaultUrl", "");
        intent.putExtra("defaultName", "");
        intent.putExtra("defaultFormat", defaultFormat);
        intent.putExtra("defaultVolume", Float.parseFloat(defaultVolume));
        intent.putExtra("defaultHeader", "");
        intent.putExtra("customUrl", "");
        intent.putExtra("customName", "");
        intent.putExtra("customFormat", "");
        intent.putExtra("customVolume", "");
        intent.putExtra("customHeader", "");
        intent.putExtra("isHidden", false);
        intent.putExtra("width", 0);
        intent.putExtra("height", 0);
        startActivityForResult(intent, 3);
        getSupportFragmentManager().popBackStack();
    }

    public void showSettings(View view){
        Intent intentSettings = new Intent(this, SettingsActivity.class);
        intentSettings.putExtra("defaultFormat", defaultFormat);
        intentSettings.putExtra("defaultVolume", defaultVolume);
        startActivityForResult(intentSettings, 2);
        getSupportFragmentManager().popBackStack();
    }

    void saveSettings() {
        if(channel == null){
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("channelNum", channelNum);
        editor.putString("defaultFormat", defaultFormat);
        editor.putString("defaultVolume", defaultVolume);
        for (Channel value : channel) {
            editor.putString(value.getUrl() + value.getFormat(), value.getVideo());
        }
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject channelListObject = new JSONObject();
            JSONArray newChannelArray = new JSONArray();
            for(int i = 0; i < channel.length; i++){
                JSONObject channelObject = new JSONObject();
                channelObject.put("customUrl", channel[i].customUrl);
                channelObject.put("customName", channel[i].customName);
                channelObject.put("customFormat", channel[i].customFormat);
                channelObject.put("customVolume", channel[i].customVolume);
                channelObject.put("customHeader", channel[i].customHeader);
                channelObject.put("isHidden", channel[i].isHidden());
                if(i < channelLength_config) {
                    channelListObject.put(channel[i].defaultName, channelObject);
                }
                else{
                    newChannelArray.put(channelObject);
                }
            }
            jsonObject.put("customChannelList", channelListObject);
            jsonObject.put("newChannelArray", newChannelArray);
            editor.putString("jsonSettings", jsonObject.toString());
        } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStarted = false;
        player.stop();
        saveSettings();
    }
}
